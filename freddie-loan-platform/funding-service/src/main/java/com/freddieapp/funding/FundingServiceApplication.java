package com.freddieapp.funding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class FundingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FundingServiceApplication.class, args);
    }
}
