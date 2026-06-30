package com.freddieapp.customerservice.service;

import com.freddieapp.customerservice.dto.CustomerRequest;
import com.freddieapp.customerservice.dto.CustomerResponse;
import com.freddieapp.customerservice.entity.Customer;
import com.freddieapp.customerservice.entity.Customer.CustomerStatus;
import com.freddieapp.customerservice.entity.Customer.KycStatus;
import com.freddieapp.customerservice.event.CustomerEventPublisher;
import com.freddieapp.customerservice.exception.CustomerAlreadyExistsException;
import com.freddieapp.customerservice.exception.CustomerNotFoundException;
import com.freddieapp.customerservice.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerEventPublisher eventPublisher;
    private final SsnEncryptionService ssnEncryptionService;

    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new CustomerAlreadyExistsException("Customer already exists with email: " + request.getEmail());
        }

        Customer customer = Customer.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .ssnEncrypted(ssnEncryptionService.encrypt(request.getSsn()))
                .dateOfBirth(request.getDateOfBirth())
                .nationality(request.getNationality())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .zipCode(request.getZipCode())
                .customerStatus(CustomerStatus.ACTIVE)
                .kycStatus(KycStatus.PENDING)
                .build();

        Customer saved = customerRepository.save(customer);
        log.info("Customer created: id={} email={}", saved.getId(), saved.getEmail());

        eventPublisher.publishCustomerCreated(saved);
        return toResponse(saved);
    }

    public CustomerResponse getCustomerById(UUID customerId) {
        return toResponse(findCustomerOrThrow(customerId));
    }

    @Transactional
    public CustomerResponse updateCustomer(UUID customerId, CustomerRequest request) {
        Customer customer = findCustomerOrThrow(customerId);

        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setPhone(request.getPhone());
        customer.setAddressLine1(request.getAddressLine1());
        customer.setAddressLine2(request.getAddressLine2());
        customer.setCity(request.getCity());
        customer.setState(request.getState());
        customer.setZipCode(request.getZipCode());

        Customer updated = customerRepository.save(customer);
        log.info("Customer updated: id={}", customerId);
        eventPublisher.publishCustomerUpdated(updated);
        return toResponse(updated);
    }

    @Transactional
    public void deactivateCustomer(UUID customerId) {
        Customer customer = findCustomerOrThrow(customerId);
        customer.setCustomerStatus(CustomerStatus.INACTIVE);
        customerRepository.save(customer);
        log.info("Customer deactivated: id={}", customerId);
    }

    public Page<CustomerResponse> searchCustomers(String name, String email, String kycStatus, Pageable pageable) {
        return customerRepository.searchCustomers(name, email, kycStatus, pageable)
                .map(this::toResponse);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Customer findCustomerOrThrow(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + id));
    }

    private CustomerResponse toResponse(Customer c) {
        return CustomerResponse.builder()
                .id(c.getId())
                .firstName(c.getFirstName())
                .lastName(c.getLastName())
                .email(c.getEmail())
                .phone(c.getPhone())
                .dateOfBirth(c.getDateOfBirth())
                .nationality(c.getNationality())
                .addressLine1(c.getAddressLine1())
                .addressLine2(c.getAddressLine2())
                .city(c.getCity())
                .state(c.getState())
                .zipCode(c.getZipCode())
                .country(c.getCountry())
                .customerStatus(c.getCustomerStatus())
                .kycStatus(c.getKycStatus())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
