package io.akodon.client;

import org.apache.hadoop.conf.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.akodon.core.AkodonProperties;

/**
 *
 */
@org.springframework.context.annotation.Configuration
@EnableConfigurationProperties(AkodonProperties.class)
public class ClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Configuration getYarnConfiguration(AkodonProperties properties) {
        Configuration conf = new Configuration();
        conf.set("yarn.resourcemanager.hostname", properties.getYarnHost());
        conf.set("fs.defaultFS", "hdfs://" + properties.getYarnHost());

        return conf;
    }

}
