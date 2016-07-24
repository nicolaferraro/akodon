package io.wallace.master;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import io.wallace.core.WallaceProperties;

/**
 *
 */
@SpringBootApplication
public class MasterApp {

    public static void main(String[] args) throws Exception {
        ApplicationContext ctx = SpringApplication.run(MasterApp.class, args);

        WallaceProperties properties = ctx.getBean(WallaceProperties.class);

        if (properties.getMaster().getDeployMode() == WallaceProperties.MasterProperties.DeployMode.YARN) {
            MasterController ctrl = ctx.getBean(MasterController.class);
            ctrl.startup();
        }
    }

}
