package com.syscho.azure.obo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OboProfileApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(OboProfileApiApplication.class, args);
    }
}
