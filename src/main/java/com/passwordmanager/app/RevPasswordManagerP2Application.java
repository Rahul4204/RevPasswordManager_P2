package com.passwordmanager.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.transaction.jta.JtaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = { JtaAutoConfiguration.class })
@EnableAsync
public class RevPasswordManagerP2Application extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(RevPasswordManagerP2Application.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(RevPasswordManagerP2Application.class, args);
    }

}
