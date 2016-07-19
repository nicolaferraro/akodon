package io.wallace.core;

import org.apache.hadoop.conf.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 *
 */
@org.springframework.context.annotation.Configuration
@EnableConfigurationProperties(WallaceProperties.class)
@ConditionalOnClass(Configuration.class)
public class WallaceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Configuration getYarnConfiguration(WallaceProperties properties) {
        Configuration conf = new Configuration();
        conf.set("yarn.resourcemanager.hostname", properties.getYarnHost());
        conf.set("fs.defaultFS", "hdfs://" + properties.getYarnHost());

        return conf;
    }

}
