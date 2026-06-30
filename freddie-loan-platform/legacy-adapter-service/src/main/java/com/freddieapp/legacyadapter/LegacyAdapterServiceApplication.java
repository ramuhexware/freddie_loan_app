package com.freddieapp.legacyadapter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class LegacyAdapterServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LegacyAdapterServiceApplication.class, args);
    }
}
