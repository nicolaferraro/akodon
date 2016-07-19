package io.wallace.client;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.wallace.core.WallaceConstants;

/**
 *
 */
@Component
public class YarnResourceFinder {

    private Logger LOG = LoggerFactory.getLogger(YarnResourceFinder.class);

    public InputStream getApplicationMasterJar() throws IOException {
        return getJar(WallaceConstants.ZIP_MASTER_JAR_PATH);
    }

    public InputStream getApplicationJar() throws IOException {
        return getJar(WallaceConstants.ZIP_APPLICATION_JAR_PATH);
    }

    private InputStream getJar(final String module) throws IOException {
        return YarnResourceFinder.class.getResourceAsStream(module);
    }

}
