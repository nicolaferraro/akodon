package io.akodon.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

/**
 *
 */
@SpringBootApplication
public class ClientApp {

    public static void main(String[] args) throws Exception {
        ApplicationContext context = SpringApplication.run(ClientApp.class, args);

        // Deploy the application master
        AkodonMasterDeployer client = context.getBean(AkodonMasterDeployer.class);
        client.deployApplicationMaster();
    }

}
