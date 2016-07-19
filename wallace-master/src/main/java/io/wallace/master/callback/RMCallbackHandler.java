package io.wallace.master.callback;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerStatus;
import org.apache.hadoop.yarn.api.records.NodeReport;
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RMCallbackHandler implements AMRMClientAsync.CallbackHandler {

    private static Logger LOG = LoggerFactory.getLogger(RMCallbackHandler.class);

    private AtomicInteger containers;

    public RMCallbackHandler() {
        this.containers = new AtomicInteger(0);
    }

    public void setNumberOfContainers(int containers) {
        this.containers.set(containers);
    }

    public int getNumberOfContainers() {
        return this.containers.get();
    }

    @Override
    public void onContainersCompleted(List<ContainerStatus> list) {
        this.containers.addAndGet(-list.size());
    }

    @Override
    public void onContainersAllocated(List<Container> list) {
        this.containers.addAndGet(list.size());
    }

    @Override
    public void onShutdownRequest() {

    }

    @Override
    public void onNodesUpdated(List<NodeReport> list) {

    }

    @Override
    public float getProgress() {
        return 0.5f;
    }

    @Override
    public void onError(Throwable throwable) {
        // TODO recover
        LOG.error("Unrecoverable error", throwable);
    }

}