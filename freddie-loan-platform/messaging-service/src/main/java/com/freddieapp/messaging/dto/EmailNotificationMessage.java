package com.freddieapp.messaging.dto;

import lombok.*;

/**
 * Payload sent to the notification.email.queue.
 * Carries everything the notification-service needs to send an email.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailNotificationMessage {

    private String loanId;
    private String customerId;
    private String recipientEmail;
    private String recipientName;
    private String notificationType;   // LOAN_APPROVED, LOAN_REJECTED, DOCUMENT_REQUESTED, etc.
    private String subject;
    private String body;
    private boolean html;              // true = HTML email, false = plain text
    private String templateId;         // optional: email template identifier
    private String locale;             // en-US, es-US — for multilingual templates
}
