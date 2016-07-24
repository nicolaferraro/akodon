package io.wallace.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.Priority;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.YarnClientApplication;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.wallace.client.util.ResourceDeployer;
import io.wallace.core.WallaceConstants;
import io.wallace.core.WallaceProperties;

/**
 *
 */
@Component
public class WallaceMasterDeployer {

    private static Logger LOG = LoggerFactory.getLogger(WallaceMasterDeployer.class);

    private Configuration conf;

    private WallaceProperties wallaceConfig;

    private YarnResourceFinder jarFinder;

    @Autowired
    public WallaceMasterDeployer(Configuration conf, WallaceProperties wallaceConfig, YarnResourceFinder jarFinder) {
        this.conf = conf;
        this.wallaceConfig = wallaceConfig;
        this.jarFinder = jarFinder;
    }

    public void deployApplicationMaster() throws IOException, YarnException {


        YarnClient yarnClient = YarnClient.createYarnClient();
        yarnClient.init(conf);
        yarnClient.start();

        YarnClientApplication app = yarnClient.createApplication();

        ApplicationSubmissionContext appContext = app.getApplicationSubmissionContext();
        ApplicationId appId = appContext.getApplicationId();

        LOG.info("Got ApplicationId from YARN: {}", appId);

        appContext.setKeepContainersAcrossApplicationAttempts(true);
        appContext.setApplicationName(wallaceConfig.getApplicationName());

        LOG.info("Copy App Master jar from local filesystem and add to local environment");

        // Get a reference to the Hadoop file system
        FileSystem fs = FileSystem.get(conf);

        // Deploy the application master jar
        ResourceDeployer deployer = new ResourceDeployer(fs, appContext);
        deployer.add(WallaceConstants.LIB_MASTER_JAR, jarFinder.getApplicationMasterJar());
        deployer.add(WallaceConstants.LIB_APPLICATION_JAR, jarFinder.getApplicationJar());

        // Set the env variables to be setup in the env where the application master will be run
        LOG.info("Set the environment for the application master");
        Map<String, String> env = new HashMap<String, String>();

        // Build the classpath
        StringBuilder classPathEnv = new StringBuilder(ApplicationConstants.Environment.CLASSPATH.$$());

        for (String c : conf.getStrings(
                YarnConfiguration.YARN_APPLICATION_CLASSPATH,
                YarnConfiguration.DEFAULT_YARN_CROSS_PLATFORM_APPLICATION_CLASSPATH)) {
            classPathEnv.append(ApplicationConstants.CLASS_PATH_SEPARATOR);
            classPathEnv.append(c.trim());
        }

        env.put("CLASSPATH", classPathEnv.toString());

        // Set the necessary command to execute the application master
        Vector<CharSequence> vargs = new Vector<CharSequence>(30);

        // Set java executable command
        LOG.info("Setting up app master command");
        vargs.add(ApplicationConstants.Environment.JAVA_HOME.$$() + "/bin/java");
        vargs.add("-Xmx" + wallaceConfig.getMaster().getMemory() + "m");

        if(wallaceConfig.getMaster().isDebug()) {
            vargs.add("-agentlib:jdwp=transport=dt_socket,server=y,address=" + wallaceConfig.getMaster().getDebugPort() + ",suspend=y");
        }

        // Sets the deploy mode to yarn for the application master
        vargs.add("-Dwallace.master.deploy-mode=yarn");

        vargs.add("-jar " + WallaceConstants.LIB_MASTER_JAR);

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
                deployer.getDeployedResources(), env, commands, null, null, null);

        Resource capability = Resource.newInstance(wallaceConfig.getMaster().getMemory(), wallaceConfig.getMaster().getVirtualCores());
        appContext.setResource(capability);


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

        Priority pri = Priority.newInstance(wallaceConfig.getMaster().getPriority());
        appContext.setPriority(pri);

        appContext.setQueue(wallaceConfig.getMaster().getYarnQueue());
        yarnClient.submitApplication(appContext);
    }


}
