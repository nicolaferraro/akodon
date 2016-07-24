package io.wallace.maven.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.activation.DataSource;
import javax.activation.FileDataSource;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.util.IOUtil;

import io.wallace.core.WallaceConstants;

/**
 *
 */
@Mojo(name = "repackage", defaultPhase = LifecyclePhase.PACKAGE)
public class RepackageMojo extends AbstractMojo {

    @Component
    private MavenProject project;

    @Component
    private RepositorySystem repo;

    @Parameter(defaultValue = ".wallace.original")
    private String backupExtension;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        File artifact = project.getArtifact().getFile();
        File backup = moveToBackup(artifact);

        File wallaceMaster = resolve("wallace-master");
        File wallaceClient = resolve("wallace-client");

        Map<String, DataSource> entries = new TreeMap<>();
        entries.put(WallaceConstants.LIB_MASTER_JAR_PATH, new FileDataSource(wallaceMaster));
        entries.put(WallaceConstants.LIB_APPLICATION_JAR_PATH, new FileDataSource(backup));
        for (File appProp : getApplicationPropertiesFiles()) {
            entries.put(appProp.getName(), new FileDataSource(appProp));
        }

        repackage(wallaceClient, artifact, entries);
    }

    private File resolve(String artifactId) throws MojoExecutionException {
        String groupId = project.getGroupId();
        String version = project.getVersion();
        String type = "jar";
        String canonicalString = groupId + ":" + artifactId + ":" + version + ":" + type;

        Artifact art = repo.createArtifact(groupId, artifactId, version, type);

        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
                .setArtifact(art)
                .setResolveRoot(true)
                .setResolveTransitively(false);

        ArtifactResolutionResult res = repo.resolve(request);

        if (!res.isSuccess()) {
            throw new MojoExecutionException("Cannot resolve artifact " + canonicalString);
        }

        Artifact resolved = null;
        for (Artifact artifact : res.getArtifacts()) {
            if (artifact.getGroupId().equals(groupId) && artifact.getArtifactId().equals(artifactId) && artifact.getVersion().equals(version) && artifact.getType().equals(type)) {
                resolved = artifact;
                break;
            }
        }

        if (resolved == null) {
            throw new MojoExecutionException("Cannot find artifact " + canonicalString + " within the resolved resources");
        }

        return resolved.getFile();
    }

    private File moveToBackup(File original) throws MojoExecutionException {
        File backup = new File(original.getParent(), original.getName() + backupExtension);
        boolean renamed = original.renameTo(backup);

        if (!renamed) {
            throw new MojoExecutionException("Could not rename file " + original);
        }

        return backup;
    }

    private void repackage(File sourceJar, File destinationJar, Map<String, DataSource> additionalEntries) throws MojoExecutionException {
        try (ZipInputStream in = new ZipInputStream(new FileInputStream(sourceJar)); ZipOutputStream out = new ZipOutputStream(new FileOutputStream(destinationJar))) {

            // Spring boot compatible jar
            out.setMethod(ZipEntry.STORED);
            out.setLevel(Deflater.NO_COMPRESSION);

            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                String name = entry.getName();
                if (!additionalEntries.containsKey(name)) {
                    writeZipEntry(name, in, out);
                }
            }

            for (String name : additionalEntries.keySet()) {
                DataSource additionalSource = additionalEntries.get(name);
                try (InputStream fin = additionalSource.getInputStream()) {
                    writeZipEntry(name, fin, out);
                }
            }

        } catch (IOException e) {
            throw new MojoExecutionException("Could not embed jar " + sourceJar, e);
        }
    }

    private void writeZipEntry(String name, InputStream source, ZipOutputStream out) throws IOException {
        byte[] content = IOUtil.toByteArray(source);
        long size = content.length;
        CRC32 crc = new CRC32();
        crc.update(content);
        long crc32Value = crc.getValue();

        ZipEntry ne = new ZipEntry(name);
        ne.setMethod(ZipEntry.STORED);
        ne.setSize(size);
        ne.setCrc(crc32Value);

        out.putNextEntry(ne);
        IOUtil.copy(content, out);
    }

    private List<File> getApplicationPropertiesFiles() {
        File build = new File(project.getBuild().getOutputDirectory());
        File[] files = build.listFiles(new FilenameFilter() {

            private Pattern pattern = Pattern.compile("^application(\\-[A-Za-z0-9]+)*\\.properties$");

            @Override
            public boolean accept(File file, String s) {
                return pattern.matcher(s).matches();
            }
        });

        return Arrays.asList(files);
    }

}
