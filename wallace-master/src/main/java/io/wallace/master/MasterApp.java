package io.wallace.master;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

/**
 *
 */
@SpringBootApplication
public class MasterApp {

    public static void main(String[] args) throws Exception {
        ApplicationContext ctx = SpringApplication.run(MasterApp.class, args);

        MasterController ctrl = ctx.getBean(MasterController.class);
        ctrl.startup();
    }

}
