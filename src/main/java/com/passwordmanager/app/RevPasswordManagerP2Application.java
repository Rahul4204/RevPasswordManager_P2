package com.passwordmanager.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class RevPasswordManagerP2Application {

    public static void main(String[] args) {
        SpringApplication.run(RevPasswordManagerP2Application.class, args);
    }

}
