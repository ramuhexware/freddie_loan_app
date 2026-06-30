package com.freddieapp.customerservice.controller;

import com.freddieapp.customerservice.dto.CustomerRequest;
import com.freddieapp.customerservice.dto.CustomerResponse;
import com.freddieapp.customerservice.service.CustomerService;
import com.freddieapp.customerservice.client.CardSyncClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "Customer profile and KYC management APIs")
public class CustomerController {

    private final CustomerService customerService;
    private final CardSyncClient cardSyncClient;

    @PostMapping
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'ADMIN')")
    @Operation(summary = "Create new customer")
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest request) {
        log.info("Creating customer: email={}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(request));
    }

    @GetMapping("/{customerId}")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'UNDERWRITER', 'ADMIN')")
    @Operation(summary = "Get customer by ID")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable UUID customerId) {
        return ResponseEntity.ok(customerService.getCustomerById(customerId));
    }

    @PutMapping("/{customerId}")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'ADMIN')")
    @Operation(summary = "Update customer profile")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable UUID customerId,
            @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(customerService.updateCustomer(customerId, request));
    }

    @DeleteMapping("/{customerId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate customer")
    public ResponseEntity<Void> deactivateCustomer(@PathVariable UUID customerId) {
        customerService.deactivateCustomer(customerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'ADMIN')")
    @Operation(summary = "Search customers with pagination")
    public ResponseEntity<Page<CustomerResponse>> searchCustomers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String kycStatus,
            Pageable pageable) {
        return ResponseEntity.ok(customerService.searchCustomers(name, email, kycStatus, pageable));
    }

    @PostMapping("/{customerId}/sync-cards")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'ADMIN')")
    @Operation(summary = "Trigger card sync from card-service")
    public ResponseEntity<Void> syncCards(@PathVariable UUID customerId) {
        log.info("Triggering card sync for customer: {}", customerId);
        cardSyncClient.syncCustomerCards(customerId);
        return ResponseEntity.accepted().build();
    }
}

