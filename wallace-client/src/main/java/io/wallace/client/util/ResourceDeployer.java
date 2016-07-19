package io.wallace.client.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;
import org.apache.hadoop.yarn.util.ConverterUtils;

/**
 * Manages the deployment of resources to the application folder on HDFS.
 */
public class ResourceDeployer {

    private FileSystem fs;

    private ApplicationSubmissionContext applicationContext;

    private Map<String, LocalResource> deployedResources;

    public ResourceDeployer(FileSystem fs, ApplicationSubmissionContext applicationContext) {
        this.fs = fs;
        this.applicationContext = applicationContext;
        this.deployedResources = new TreeMap<>();
    }

    /**
     * Copies a local file into the application folder.
     */
    public void add(String localName, File localFile) throws IOException {
        this.add(localName, new FileInputStream(localFile));
    }

    /**
     * Adds a resource into the application folder.
     */
    public void add(String localName, InputStream content) throws IOException {
        String suffix = applicationContext.getApplicationName() + "/" + applicationContext.getApplicationId() + "/" + localName;
        Path dst = new Path(fs.getHomeDirectory(), suffix);

        try (FSDataOutputStream out = fs.create(dst)) {
            IOUtils.copy(content, out);
        }

        FileStatus scFileStatus = fs.getFileStatus(dst);
        LocalResource scRsrc =
                LocalResource.newInstance(
                        ConverterUtils.getYarnUrlFromURI(dst.toUri()),
                        LocalResourceType.FILE, LocalResourceVisibility.APPLICATION,
                        scFileStatus.getLen(), scFileStatus.getModificationTime());

        deployedResources.put(localName, scRsrc);
    }

    /**
     * Returns all resources deployed by this resource deployer.
     */
    public Map<String, LocalResource> getDeployedResources() {
        return deployedResources;
    }


}
