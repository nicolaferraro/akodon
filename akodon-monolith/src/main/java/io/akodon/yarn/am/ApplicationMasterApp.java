package io.akodon.yarn.am;

/**
 *
 */
public class ApplicationMasterApp {

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 500; i++) {
            System.out.println("Hello from AM!");
            Thread.sleep(1000);
        }
    }

}
