package io.akodon;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;
import org.apache.hadoop.yarn.api.records.Priority;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.applications.distributedshell.DSConstants;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.YarnClientApplication;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
//@SpringBootApplication
public class HelloWorld {

    private static Logger LOG = LoggerFactory.getLogger(HelloWorld.class);

    private static String appName = "Example";

    private static String appMasterJar = "/home/nferraro/git/owned/akodon/akodon-example/target/akodon-example-1.0-SNAPSHOT.jar";

    private static String appMasterJarPath = "akodon-example-1.0-SNAPSHOT.jar";

    private static String shellScriptPath = "";// "/home/nferraro/git/owned/akodon/akodon-example/target/run.sh";

    public static final String SCRIPT_PATH = "ExecScript";

    // App master priority
    private static int amPriority = 0;
    // Queue for App master
    private static String amQueue = "default";

    public static final int amMemory = 1024;

    // Amt. of virtual core resource to request for to run the App Master
    private static int amVCores = 1;

    private static String appMasterMainClass= "io.akodon.Example";

    // Amt of memory to request for container in which shell script will be executed
    private static int containerMemory = 10;
    // Amt. of virtual cores to request for container in which shell script will be executed
    private static int containerVirtualCores = 1;
    // No. of containers in which the shell script needs to be executed
    private static int numContainers = 1;

    // Shell Command Container priority
    private static int shellCmdPriority = 0;

    // Env variables to be setup for the shell command
    private static Map<String, String> shellEnv = new HashMap<String, String>();


    public static void main(String[] args) throws Exception {
//        SpringApplication.run(HelloWorld.class, args);

        Configuration conf = new Configuration();
        conf.set("yarn.resourcemanager.hostname", "quickstart.cloudera");
        conf.set("fs.defaultFS", "hdfs://quickstart.cloudera");


        YarnClient yarnClient = YarnClient.createYarnClient();
        yarnClient.init(conf);
        yarnClient.start();

        YarnClientApplication app = yarnClient.createApplication();

        ApplicationSubmissionContext appContext = app.getApplicationSubmissionContext();
        ApplicationId appId = appContext.getApplicationId();

        System.out.println("MYIDDD " + appId);

        appContext.setKeepContainersAcrossApplicationAttempts(true);
        appContext.setApplicationName(appName);

        // set local resources for the application master
        // local files or archives as needed
        // In this scenario, the jar file for the application master is part of the local resources
        Map<String, LocalResource> localResources = new HashMap<String, LocalResource>();

        LOG.info("Copy App Master jar from local filesystem and add to local environment");
        // Copy the application master jar to the filesystem
        // Create a local resource to point to the destination jar path
        FileSystem fs = FileSystem.get(conf);
        addToLocalResources(fs, appMasterJar, appMasterJarPath, appId.toString(),
                localResources, null);

        // The shell script has to be made available on the final container(s)
        // where it will be executed.
        // To do this, we need to first copy into the filesystem that is visible
        // to the yarn framework.
        // We do not need to set this as a local resource for the application
        // master as the application master does not need it.
        String hdfsShellScriptLocation = "";
        long hdfsShellScriptLen = 0;
        long hdfsShellScriptTimestamp = 0;
        if (!shellScriptPath.isEmpty()) {
            Path shellSrc = new Path(shellScriptPath);
            String shellPathSuffix =
                    appName + "/" + appId.toString() + "/" + SCRIPT_PATH;
            Path shellDst =
                    new Path(fs.getHomeDirectory(), shellPathSuffix);
            fs.copyFromLocalFile(false, true, shellSrc, shellDst);
            hdfsShellScriptLocation = shellDst.toUri().toString();
            FileStatus shellFileStatus = fs.getFileStatus(shellDst);
            hdfsShellScriptLen = shellFileStatus.getLen();
            hdfsShellScriptTimestamp = shellFileStatus.getModificationTime();
        }

//        if (!shellCommand.isEmpty()) {
//            addToLocalResources(fs, null, shellCommandPath, appId.toString(),
//                    localResources, shellCommand);
//        }
//
//        if (shellArgs.length > 0) {
//            addToLocalResources(fs, null, shellArgsPath, appId.toString(),
//                    localResources, StringUtils.join(shellArgs, " "));
//        }

        // Set the env variables to be setup in the env where the application master will be run
        LOG.info("Set the environment for the application master");
        Map<String, String> env = new HashMap<String, String>();

        // put location of shell script into env
        // using the env info, the application master will create the correct local resource for the
        // eventual containers that will be launched to execute the shell scripts
        env.put(DSConstants.DISTRIBUTEDSHELLSCRIPTLOCATION, hdfsShellScriptLocation);
        env.put(DSConstants.DISTRIBUTEDSHELLSCRIPTTIMESTAMP, Long.toString(hdfsShellScriptTimestamp));
        env.put(DSConstants.DISTRIBUTEDSHELLSCRIPTLEN, Long.toString(hdfsShellScriptLen));

        // Add AppMaster.jar location to classpath
        // At some point we should not be required to add
        // the hadoop specific classpaths to the env.
        // It should be provided out of the box.
        // For now setting all required classpaths including
        // the classpath to "." for the application jar
        StringBuilder classPathEnv = new StringBuilder(ApplicationConstants.Environment.CLASSPATH.$$())
                .append(ApplicationConstants.CLASS_PATH_SEPARATOR).append("./*");
        for (String c : conf.getStrings(
                YarnConfiguration.YARN_APPLICATION_CLASSPATH,
                YarnConfiguration.DEFAULT_YARN_CROSS_PLATFORM_APPLICATION_CLASSPATH)) {
            classPathEnv.append(ApplicationConstants.CLASS_PATH_SEPARATOR);
            classPathEnv.append(c.trim());
        }

//        classPathEnv.append(ApplicationConstants.CLASS_PATH_SEPARATOR).append(
//                "./log4j.properties");

        classPathEnv.append(ApplicationConstants.CLASS_PATH_SEPARATOR).append(
                "./" + appMasterJarPath);

        env.put("CLASSPATH", classPathEnv.toString());

        // Set the necessary command to execute the application master
        Vector<CharSequence> vargs = new Vector<CharSequence>(30);

        // Set java executable command
        LOG.info("Setting up app master command");
        vargs.add(ApplicationConstants.Environment.JAVA_HOME.$$() + "/bin/java");
        // Set Xmx based on am memory size
        vargs.add("-Xmx" + amMemory + "m");
        // Set class name
        vargs.add(appMasterMainClass);
        // Set params for Application Master
        vargs.add("--container_memory " + String.valueOf(containerMemory));
        vargs.add("--container_vcores " + String.valueOf(containerVirtualCores));
        vargs.add("--num_containers " + String.valueOf(numContainers));
        vargs.add("--priority " + String.valueOf(shellCmdPriority));


        for (Map.Entry<String, String> entry : shellEnv.entrySet()) {
            vargs.add("--shell_env " + entry.getKey() + "=" + entry.getValue());
        }
        //if (debugFlag) {
            vargs.add("--debug");
        //}

        vargs.add("1>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/AppMaster.stdout");
        vargs.add("2>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/AppMaster.stderr");

        // Get final commmand
        StringBuilder command = new StringBuilder();
        for (CharSequence str : vargs) {
            command.append(str).append(" ");
        }

        LOG.info("Completed setting up app master command " + command.toString());
        List<String> commands = new ArrayList<String>();
        commands.add(command.toString());

        // Set up the container launch context for the application master
        ContainerLaunchContext amContainer = ContainerLaunchContext.newInstance(
                localResources, env, commands, null, null, null);

        // Set up resource type requirements
        // For now, both memory and vcores are supported, so we set memory and
        // vcores requirements
        Resource capability = Resource.newInstance(amMemory, amVCores);
        appContext.setResource(capability);

        // Service data is a binary blob that can be passed to the application
        // Not needed in this scenario
        // amContainer.setServiceData(serviceData);

        // Setup security tokens
        if (UserGroupInformation.isSecurityEnabled()) {
            // Note: Credentials class is marked as LimitedPrivate for HDFS and MapReduce
            Credentials credentials = new Credentials();
            String tokenRenewer = conf.get(YarnConfiguration.RM_PRINCIPAL);
            if (tokenRenewer == null || tokenRenewer.length() == 0) {
                throw new IOException(
                        "Can't get Master Kerberos principal for the RM to use as renewer");
            }

            // For now, only getting tokens for the default file-system.
            final Token<?> tokens[] =
                    fs.addDelegationTokens(tokenRenewer, credentials);
            if (tokens != null) {
                for (Token<?> token : tokens) {
                    LOG.info("Got dt for " + fs.getUri() + "; " + token);
                }
            }
            DataOutputBuffer dob = new DataOutputBuffer();
            credentials.writeTokenStorageToStream(dob);
            ByteBuffer fsTokens = ByteBuffer.wrap(dob.getData(), 0, dob.getLength());
            amContainer.setTokens(fsTokens);
        }

        appContext.setAMContainerSpec(amContainer);


        /////////

        // Set the priority for the application master
        Priority pri = Priority.newInstance(amPriority);
        appContext.setPriority(pri);

// Set the queue to which this application is to be submitted in the RM
        appContext.setQueue(amQueue);

// Submit the application to the applications manager
// SubmitApplicationResponse submitResp = applicationsManager.submitApplication(appRequest);

        yarnClient.submitApplication(appContext);
    }


    private static void addToLocalResources(FileSystem fs, String fileSrcPath,
                                            String fileDstPath, String appId, Map<String, LocalResource> localResources,
                                            String resources) throws IOException {
        String suffix =
                appName + "/" + appId + "/" + fileDstPath;
        Path dst =
                new Path(fs.getHomeDirectory(), suffix);
        if (fileSrcPath == null) {
            FSDataOutputStream ostream = null;
            try {
                ostream = FileSystem
                        .create(fs, dst, new FsPermission((short) 0710));
                ostream.writeUTF(resources);
            } finally {
                IOUtils.closeQuietly(ostream);
            }
        } else {
            fs.copyFromLocalFile(new Path(fileSrcPath), dst);
        }
        FileStatus scFileStatus = fs.getFileStatus(dst);
        LocalResource scRsrc =
                LocalResource.newInstance(
                        ConverterUtils.getYarnUrlFromURI(dst.toUri()),
                        LocalResourceType.FILE, LocalResourceVisibility.APPLICATION,
                        scFileStatus.getLen(), scFileStatus.getModificationTime());
        localResources.put(fileDstPath, scRsrc);
    }
}
