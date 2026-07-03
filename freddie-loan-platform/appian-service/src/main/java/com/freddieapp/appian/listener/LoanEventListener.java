package com.freddieapp.appian.listener;

import com.freddieapp.appian.service.AppianIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanEventListener {

    private final AppianIntegrationService appianIntegrationService;

    @KafkaListener(
            topics = {"loan-events", "loan-lifecycle-events"},
            groupId = "appian-service-group"
    )
    public void onLoanEvent(
            @Payload(required = false) Object payload,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        log.info("Received Kafka event on topic={} partition={} offset={} key={}", topic, partition, offset, key);

        String loanId = key;
        String eventType = "UNKNOWN";

        if (payload instanceof String) {
            eventType = (String) payload;
            log.info("Parsed event as plain String: eventType={}", eventType);
        } else if (payload instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) payload;
            if (map.containsKey("eventType")) {
                eventType = String.valueOf(map.get("eventType"));
            } else if (map.containsKey("status")) {
                eventType = String.valueOf(map.get("status"));
            } else if (map.containsKey("decision")) {
                eventType = String.valueOf(map.get("decision"));
            }

            if (map.containsKey("loanId")) {
                loanId = String.valueOf(map.get("loanId"));
            }
            log.info("Parsed event as JSON Map: loanId={}, eventType={}", loanId, eventType);
        } else if (payload != null) {
            eventType = payload.toString();
            log.info("Parsed event via toString(): eventType={}", eventType);
        }

        if (loanId == null || loanId.trim().isEmpty()) {
            log.warn("Skipping event processing because loanId is null or empty. topic={}, payload={}", topic, payload);
            if (ack != null) {
                ack.acknowledge();
            }
            return;
        }

        try {
            // Forward event to Appian integration service
            appianIntegrationService.processKafkaEventToAppian(loanId, eventType, null, payload);
            
            if (ack != null) {
                ack.acknowledge();
                log.info("Acknowledged Kafka event for loanId={}", loanId);
            }
        } catch (Exception ex) {
            log.error("Failed to process event for loanId={}: {}", loanId, ex.getMessage(), ex);
            // In a production setup, we would let it fail to trigger retry or DLQ,
            // but we'll acknowledge here to prevent blocking tests/simulation.
            if (ack != null) {
                ack.acknowledge();
            }
        }
    }
}
