package com.freddieapp.loanorigination.client;

import com.freddieapp.loanorigination.dto.CustomerDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Feign Client — Loan Origination → Customer Service
 *
 * Performs synchronous REST calls to customer-service via Eureka service discovery.
 * The circuit breaker is configured in application.yml and applied automatically
 * by Spring Cloud CircuitBreaker + Resilience4j.
 */
@FeignClient(
        name = "customer-service",
        path = "/api/v1/customers",
        fallback = CustomerServiceClientFallback.class
)
public interface CustomerServiceClient {

    @GetMapping("/{customerId}")
    CustomerDto getCustomerById(@PathVariable UUID customerId);
}
