package io.wallace.master;

import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.records.ApplicationAttemptId;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync;
import org.apache.hadoop.yarn.client.api.async.impl.NMClientAsyncImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.wallace.master.callback.NMCallbackHandler;
import io.wallace.master.callback.RMCallbackHandler;

/**
 *
 */
@Component
public class MasterController {

    private Configuration conf;

    private RMCallbackHandler rmCallbackHandler;

    private NMCallbackHandler nmCallbackHandler;

    @Autowired
    public MasterController(Configuration conf, RMCallbackHandler rmCallbackHandler, NMCallbackHandler nmCallbackHandler) {
        this.conf = conf;
        this.rmCallbackHandler = rmCallbackHandler;
        this.nmCallbackHandler = nmCallbackHandler;
    }

    public void startup() {

        Map<String, String> envs = System.getenv();

        String containerIdString = envs.get(ApplicationConstants.Environment.CONTAINER_ID.name());

        if (containerIdString == null) {
            // container id should always be set in the env by the framework
            throw new IllegalStateException("ContainerId not set in the environment");
        }

        ContainerId containerId = ContainerId.fromString(containerIdString);
        ApplicationAttemptId appAttemptID = containerId.getApplicationAttemptId();


        AMRMClientAsync amRMClient = AMRMClientAsync.createAMRMClientAsync(1000, rmCallbackHandler);
        amRMClient.init(conf);
        amRMClient.start();

        NMClientAsyncImpl nmClientAsync = new NMClientAsyncImpl(nmCallbackHandler);
        nmClientAsync.init(conf);
        nmClientAsync.start();

    }

}
