package io.wallace.master.callback;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.ContainerStatus;
import org.apache.hadoop.yarn.client.api.async.NMClientAsync;
import org.jboss.netty.util.internal.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component
public class NMCallbackHandler
        implements NMClientAsync.CallbackHandler {

    private static Logger LOG = LoggerFactory.getLogger(NMCallbackHandler.class);

    private ConcurrentMap<ContainerId, Container> containers =
            new ConcurrentHashMap<ContainerId, Container>();

    public NMCallbackHandler() {
    }

    public void addContainer(ContainerId containerId, Container container) {
        containers.putIfAbsent(containerId, container);
    }

    @Override
    public void onContainerStopped(ContainerId containerId) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Succeeded to stop Container " + containerId);
        }
        containers.remove(containerId);
    }

    @Override
    public void onContainerStatusReceived(ContainerId containerId,
                                          ContainerStatus containerStatus) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Container Status: id=" + containerId + ", status=" +
                    containerStatus);
        }
    }

    @Override
    public void onContainerStarted(ContainerId containerId,
                                   Map<String, ByteBuffer> allServiceResponse) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Succeeded to start Container " + containerId);
        }

    }

    @Override
    public void onStartContainerError(ContainerId containerId, Throwable t) {
        LOG.error("Failed to start Container " + containerId);
        containers.remove(containerId);
    }

    @Override
    public void onGetContainerStatusError(
            ContainerId containerId, Throwable t) {
        LOG.error("Failed to query the status of Container " + containerId);
    }

    @Override
    public void onStopContainerError(ContainerId containerId, Throwable t) {
        LOG.error("Failed to stop Container " + containerId);
        containers.remove(containerId);
    }
}
