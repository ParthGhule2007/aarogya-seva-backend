package com.ruralhealthcare.sos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Main application bootstrap for Aarogya Seva SOS Emergency Alert Microservice.
 * Independently buildable, runnable, and deployable.
 */
@SpringBootApplication
@EnableConfigurationProperties
public class SosApplication {

    public static void main(String[] args) {
        SpringApplication.run(SosApplication.class, args);
    }
}
