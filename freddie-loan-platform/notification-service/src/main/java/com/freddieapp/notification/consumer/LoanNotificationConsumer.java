package com.freddieapp.notification.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.freddieapp.notification.service.NotificationDispatchService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanNotificationConsumer {

    private final NotificationDispatchService notificationDispatchService;

    @KafkaListener(
            topics         = "loan-lifecycle-events",
            groupId        = "notification-service-group",
            containerFactory = "manualAckListenerContainerFactory"
    )
    public void onLoanEvent(
            @Payload Map<String, Object> event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        String eventType  = (String) event.get("eventType");
        String loanId     = (String) event.get("loanId");
        String customerId = (String) event.get("customerId");

        log.info("Received loan event: type={} loanId={} topic={} partition={} offset={}",
                eventType, loanId, topic, partition, offset);

        try {
            notificationDispatchService.dispatch(eventType, customerId, loanId, event);
            ack.acknowledge();
            log.info("Notification dispatched and ACK'd for loanId={}", loanId);
        } catch (Exception ex) {
            log.error("Notification dispatch failed for loanId={}: {}", loanId, ex.getMessage(), ex);
            throw ex; // Re-throw to trigger DLT routing via Kafka error handler
        }
    }

    @KafkaListener(topics = "loan-lifecycle-events.DLT", groupId = "notification-dlt-group")
    @DltHandler
    public void onDlt(@Payload Map<String, Object> event,
                      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("DLT: Unprocessable loan notification event. topic={} event={}", topic, event);
        // TODO: Persist to dead-letter alert table for ops team review
    }
}
