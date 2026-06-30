package com.freddieapp.messaging.service;

import com.freddieapp.messaging.dto.DocumentRequestMessage;
import com.freddieapp.messaging.dto.EmailNotificationMessage;
import com.freddieapp.messaging.dto.LoanProcessingMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.stereotype.Service;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import java.util.UUID;

/**
 * Loan Messaging Service — ActiveMQ JMS
 *
 * Demonstrates all three key forms of JmsTemplate.convertAndSend():
 *
 *   1. convertAndSend(destination, message)
 *      Simple fire-and-forget. Converts POJO → TextMessage using the configured
 *      MappingJackson2MessageConverter and sends to the named queue.
 *
 *   2. convertAndSend(destination, message, MessagePostProcessor)
 *      Attaches JMS headers (JMSCorrelationID, JMSPriority, JMSType, custom
 *      properties) to the message after conversion but before dispatch.
 *
 *   3. convertAndSend(destination, message, postProcessor) — business variants
 *      Used to set priority, TTL, and selector-compatible properties for
 *      selective consumers and request-reply patterns.
 *
 * The JmsTemplate is configured in JmsConfig with:
 *   - sessionTransacted = true  (each send participates in the local JMS tx)
 *   - deliveryPersistent = true (messages survive broker restarts)
 *   - MappingJackson2MessageConverter (POJO ↔ JSON TextMessage)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoanMessagingService {

    // ─── Queue Destination Names ──────────────────────────────────────────────
    private static final String QUEUE_LOAN_PROCESSING   = "loan.processing.queue";
    private static final String QUEUE_DOCUMENT_REQUEST  = "document.request.queue";
    private static final String QUEUE_EMAIL_NOTIFY      = "notification.email.queue";
    private static final String QUEUE_AUDIT_TRAIL       = "audit.trail.queue";
    private static final String QUEUE_UNDERWRITING      = "underwriting.decision.queue";

    private final JmsTemplate jmsTemplate;

    // ─── Form 1: convertAndSend(destination, message) ────────────────────────
    // Simplest usage — converts the POJO to a JSON TextMessage and dispatches.
    // No headers are set; the converter adds the _type header automatically.

    /**
     * Sends a loan processing request to the loan processing queue.
     * The message payload is serialized to JSON by the Jackson converter.
     *
     * @param message the loan processing payload
     */
    public void sendLoanProcessingRequest(LoanProcessingMessage message) {
        log.info("[JMS] convertAndSend → {} | loanId={} priority={}",
                QUEUE_LOAN_PROCESSING, message.getLoanId(), message.getPriority());

        jmsTemplate.convertAndSend(QUEUE_LOAN_PROCESSING, message);

        log.info("[JMS] Loan processing request dispatched for loanId={}", message.getLoanId());
    }

    // ─── Form 2: convertAndSend(destination, message, MessagePostProcessor) ──
    // Converts the POJO, then hands the Message to the MessagePostProcessor
    // so JMS headers and custom properties can be set before the broker receives it.

    /**
     * Sends a loan processing request with full JMS header enrichment.
     *
     * Headers set via MessagePostProcessor:
     *   - JMSCorrelationID  → correlates request with async reply
     *   - JMSPriority       → message broker delivery priority (0–9)
     *   - JMSType           → logical message type for consumers
     *   - loanId property   → used by JMS message selectors on consumers
     *   - priorityLevel     → business-level priority label
     */
    public void sendLoanProcessingRequestWithHeaders(LoanProcessingMessage message) {
        String correlationId = UUID.randomUUID().toString();

        log.info("[JMS] convertAndSend (with PostProcessor) → {} | loanId={} correlationId={}",
                QUEUE_LOAN_PROCESSING, message.getLoanId(), correlationId);

        jmsTemplate.convertAndSend(
                QUEUE_LOAN_PROCESSING,
                message,
                (MessagePostProcessor) jmsMessage -> {
                    jmsMessage.setJMSCorrelationID(correlationId);
                    jmsMessage.setJMSPriority(message.getPriority());
                    jmsMessage.setJMSType("LoanProcessingRequest");
                    jmsMessage.setStringProperty("loanId", message.getLoanId());
                    jmsMessage.setStringProperty("customerId", message.getCustomerId());
                    jmsMessage.setStringProperty("priorityLevel", resolvePriorityLabel(message.getPriority()));
                    jmsMessage.setLongProperty("requestedAt", System.currentTimeMillis());
                    return jmsMessage;
                }
        );

        log.info("[JMS] Loan processing request dispatched with correlationId={}", correlationId);
    }

    /**
     * Sends a document processing request to the document service queue.
     *
     * MessagePostProcessor sets:
     *   - JMSCorrelationID → ties this request to the originating loan event
     *   - documentType     → allows document-service to use message selectors
     *   - loanId           → for selector-based routing per loan
     *   - JMSExpiration    → 30-minute TTL (via JMSTemplate TTL override inside postProcessor)
     *
     * @param request the document request payload
     */
    public void sendDocumentRequest(DocumentRequestMessage request) {
        String correlationId = UUID.randomUUID().toString();

        log.info("[JMS] convertAndSend → {} | loanId={} docType={}",
                QUEUE_DOCUMENT_REQUEST, request.getLoanId(), request.getDocumentType());

        jmsTemplate.convertAndSend(
                QUEUE_DOCUMENT_REQUEST,
                request,
                jmsMessage -> {
                    jmsMessage.setJMSCorrelationID(correlationId);
                    jmsMessage.setJMSType("DocumentRequest");
                    jmsMessage.setStringProperty("loanId", request.getLoanId());
                    jmsMessage.setStringProperty("documentType", request.getDocumentType());
                    jmsMessage.setStringProperty("requestedBy", request.getRequestedBy());
                    jmsMessage.setIntProperty("documentVersion", request.getVersion());
                    // Set 30-minute expiry: current time + 1,800,000 ms
                    jmsMessage.setJMSExpiration(System.currentTimeMillis() + 1_800_000L);
                    return jmsMessage;
                }
        );

        log.info("[JMS] Document request sent. correlationId={} loanId={} docType={}",
                correlationId, request.getLoanId(), request.getDocumentType());
    }

    /**
     * Sends an email notification request to the notification queue.
     *
     * MessagePostProcessor sets:
     *   - JMSType           → "EmailNotification"
     *   - notificationType  → allows topic-level routing (APPROVAL, REJECTION etc.)
     *   - recipientEmail    → consumed by email notification handler
     *   - JMSPriority       → HIGH priority (8) for rejection/approval, normal (4) otherwise
     */
    public void sendEmailNotificationRequest(EmailNotificationMessage notification) {
        int priority = isHighPriorityNotification(notification.getNotificationType()) ? 8 : 4;

        log.info("[JMS] convertAndSend → {} | type={} recipient={}",
                QUEUE_EMAIL_NOTIFY, notification.getNotificationType(), notification.getRecipientEmail());

        jmsTemplate.convertAndSend(
                QUEUE_EMAIL_NOTIFY,
                notification,
                jmsMessage -> {
                    jmsMessage.setJMSType("EmailNotification");
                    jmsMessage.setJMSPriority(priority);
                    jmsMessage.setStringProperty("notificationType", notification.getNotificationType());
                    jmsMessage.setStringProperty("recipientEmail", notification.getRecipientEmail());
                    jmsMessage.setStringProperty("loanId", notification.getLoanId());
                    jmsMessage.setBooleanProperty("htmlContent", notification.isHtml());
                    return jmsMessage;
                }
        );
    }

    /**
     * Sends an underwriting decision notification to the underwriting queue.
     *
     * MessagePostProcessor sets:
     *   - decision          → APPROVED / REFERRED / DECLINED (for message selectors)
     *   - assessmentId      → underwriting assessment reference
     *   - riskLevel         → LOW / MEDIUM / HIGH / CRITICAL
     */
    public void sendUnderwritingDecision(String loanId, String assessmentId,
                                         String decision, String riskLevel) {
        var payload = java.util.Map.of(
                "loanId", loanId,
                "assessmentId", assessmentId,
                "decision", decision,
                "riskLevel", riskLevel,
                "decidedAt", java.time.Instant.now().toString()
        );

        log.info("[JMS] convertAndSend → {} | loanId={} decision={} risk={}",
                QUEUE_UNDERWRITING, loanId, decision, riskLevel);

        jmsTemplate.convertAndSend(
                QUEUE_UNDERWRITING,
                payload,
                jmsMessage -> {
                    jmsMessage.setJMSType("UnderwritingDecision");
                    jmsMessage.setStringProperty("loanId", loanId);
                    jmsMessage.setStringProperty("decision", decision);
                    jmsMessage.setStringProperty("riskLevel", riskLevel);
                    jmsMessage.setStringProperty("assessmentId", assessmentId);
                    return jmsMessage;
                }
        );
    }

    /**
     * Sends an audit trail event using the simplest convertAndSend() form.
     * Audit messages are fire-and-forget — no custom headers needed.
     * The _type header added by the converter is sufficient for the audit consumer.
     */
    public void sendAuditEvent(String entityType, String entityId, String action, String performedBy) {
        var auditPayload = java.util.Map.of(
                "entityType", entityType,
                "entityId", entityId,
                "action", action,
                "performedBy", performedBy,
                "timestamp", java.time.Instant.now().toString()
        );

        log.info("[JMS] Audit convertAndSend → {} | entity={} id={} action={}",
                QUEUE_AUDIT_TRAIL, entityType, entityId, action);

        // Form 1 — plain convertAndSend, no post-processor needed
        jmsTemplate.convertAndSend(QUEUE_AUDIT_TRAIL, auditPayload);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String resolvePriorityLabel(int priority) {
        return switch (priority) {
            case 8, 9 -> "HIGH";
            case 5, 6, 7 -> "MEDIUM";
            default -> "NORMAL";
        };
    }

    private boolean isHighPriorityNotification(String type) {
        return type != null && (type.contains("APPROVED") || type.contains("REJECTED")
                || type.contains("DISBURSED"));
    }
}
