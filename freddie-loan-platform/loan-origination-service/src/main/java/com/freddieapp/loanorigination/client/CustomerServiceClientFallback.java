package com.freddieapp.loanorigination.client;

import com.freddieapp.loanorigination.dto.CustomerDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class CustomerServiceClientFallback implements CustomerServiceClient {

    @Override
    public CustomerDto getCustomerById(UUID customerId) {
        log.warn("Customer service is unavailable. Falling back for customerId={}", customerId);
        return CustomerDto.builder()
                .id(customerId.toString())
                .firstName("Fallback")
                .lastName("Customer")
                .customerStatus("UNKNOWN")
                .kycStatus("UNKNOWN")
                .build();
    }
}
