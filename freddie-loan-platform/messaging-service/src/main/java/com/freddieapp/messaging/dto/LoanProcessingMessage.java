package com.freddieapp.messaging.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Payload sent to the loan.processing.queue.
 * Serialized to a JSON TextMessage by MappingJackson2MessageConverter.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanProcessingMessage {

    private String loanId;
    private String customerId;
    private String loanType;           // PURCHASE, REFINANCE, HELOC, HOME_EQUITY
    private BigDecimal loanAmount;
    private BigDecimal propertyValue;
    private String propertyAddress;
    private String requestedBy;        // loan officer user ID
    private int    priority;           // JMS priority 0–9 (default 4)
    private String correlationId;      // optional upstream correlation
    private String eventTimestamp;     // ISO-8601
}
