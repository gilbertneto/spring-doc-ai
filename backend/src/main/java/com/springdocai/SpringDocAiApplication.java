package com.springdocai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SpringDocAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringDocAiApplication.class, args);
    }
}
