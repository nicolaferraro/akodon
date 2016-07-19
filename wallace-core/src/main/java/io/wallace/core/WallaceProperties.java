package io.wallace.core;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Wallace general configuration.
 */
@ConfigurationProperties("wallace")
public class WallaceProperties {

    /**
     * The name of the application.
     */
    private String applicationName = "Wallace-Quickstart";

    /**
     * The host where yarn is deployed.
     */
    private String yarnHost = "quickstart.cloudera";

    /**
     * Wallace application master properties.
     */
    private MasterProperties master = new MasterProperties();

    public static class MasterProperties {

        /**
         * Memory in MB to allocate for the application master.
         */
        private int memory = 1024;

        /**
         * Virtual cores to use for the application master.
         */
        private int virtualCores = 1;

        /**
         * Priority of the application master.
         */
        private int priority = 0;

        /**
         * Yarn queue of the application master.
         */
        private String yarnQueue = "default";

        public MasterProperties() {
        }

        public int getMemory() {
            return memory;
        }

        public void setMemory(int memory) {
            this.memory = memory;
        }

        public int getVirtualCores() {
            return virtualCores;
        }

        public void setVirtualCores(int virtualCores) {
            this.virtualCores = virtualCores;
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

        public String getYarnQueue() {
            return yarnQueue;
        }

        public void setYarnQueue(String yarnQueue) {
            this.yarnQueue = yarnQueue;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("MasterProperties{");
            sb.append("memory=").append(memory);
            sb.append(", virtualCores=").append(virtualCores);
            sb.append(", priority=").append(priority);
            sb.append(", yarnQueue='").append(yarnQueue).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getYarnHost() {
        return yarnHost;
    }

    public void setYarnHost(String yarnHost) {
        this.yarnHost = yarnHost;
    }

    public MasterProperties getMaster() {
        return master;
    }

    public void setMaster(MasterProperties master) {
        this.master = master;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WallaceProperties{");
        sb.append("applicationName='").append(applicationName).append('\'');
        sb.append(", yarnHost='").append(yarnHost).append('\'');
        sb.append(", master=").append(master);
        sb.append('}');
        return sb.toString();
    }
}
