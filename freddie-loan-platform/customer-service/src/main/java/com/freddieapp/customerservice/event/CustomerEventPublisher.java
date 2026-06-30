package com.freddieapp.customerservice.event;

import com.freddieapp.customerservice.entity.Customer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerEventPublisher {

    private static final String TOPIC_KYC_EVENTS = "kyc-events";
    private static final String TOPIC_AUDIT       = "audit-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishCustomerCreated(Customer customer) {
        var event = CustomerCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("CUSTOMER_CREATED")
                .customerId(customer.getId().toString())
                .email(customer.getEmail())
                .kycStatus(customer.getKycStatus().name())
                .occurredAt(Instant.now())
                .build();

        kafkaTemplate.send(TOPIC_KYC_EVENTS, customer.getId().toString(), event)
                .whenComplete((r, ex) -> {
                    if (ex != null) log.error("Failed to publish CustomerCreatedEvent: {}", ex.getMessage());
                    else log.info("CustomerCreatedEvent published for customerId={}", customer.getId());
                });
    }

    public void publishCustomerUpdated(Customer customer) {
        var event = CustomerUpdatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("CUSTOMER_UPDATED")
                .customerId(customer.getId().toString())
                .occurredAt(Instant.now())
                .build();

        kafkaTemplate.send(TOPIC_AUDIT, customer.getId().toString(), event);
    }

    @lombok.Builder
    public record CustomerCreatedEvent(
            String eventId, String eventType, String customerId,
            String email, String kycStatus, Instant occurredAt) {}

    @lombok.Builder
    public record CustomerUpdatedEvent(
            String eventId, String eventType, String customerId, Instant occurredAt) {}
}
