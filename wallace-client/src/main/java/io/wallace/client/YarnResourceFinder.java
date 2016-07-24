package io.wallace.client;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import io.wallace.core.WallaceConstants;

/**
 *
 */
@Component
public class YarnResourceFinder {

    private Logger LOG = LoggerFactory.getLogger(YarnResourceFinder.class);

    @Autowired
    private ResourceLoader resourceLoader;

    public InputStream getApplicationMasterJar() throws IOException {
        LOG.debug("Retrieving master jar in {}", WallaceConstants.LIB_MASTER_JAR_PATH);
        return getJar(WallaceConstants.LIB_MASTER_JAR_PATH);
    }

    public InputStream getApplicationJar() throws IOException {
        LOG.debug("Retrieving application jar in {}", WallaceConstants.LIB_APPLICATION_JAR_PATH);
        return getJar(WallaceConstants.LIB_APPLICATION_JAR_PATH);
    }

    private InputStream getJar(final String module) throws IOException {
        // Using Spring resource loader
        InputStream stream = resourceLoader.getResource("classpath:" + module).getURL().openStream();
        if (stream == null) {
            throw new IllegalStateException("Unable to find jar module " + module + " in the application package");
        }
        return stream;
    }

}
