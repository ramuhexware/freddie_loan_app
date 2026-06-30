package com.freddieapp.appraisal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class AppraisalServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AppraisalServiceApplication.class, args);
    }
}
