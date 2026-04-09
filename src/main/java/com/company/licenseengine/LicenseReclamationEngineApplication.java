package com.company.licenseengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;

@SpringBootApplication
@EnableScheduling
@EnableBatchProcessing
public class LicenseReclamationEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(LicenseReclamationEngineApplication.class, args);
    }
}