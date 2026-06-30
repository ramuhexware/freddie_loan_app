package com.freddieapp.messaging.dto;

import lombok.*;

/**
 * Payload sent to the document.request.queue.
 * Carries the metadata for a document upload/verification request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentRequestMessage {

    private String loanId;
    private String customerId;
    private String documentType;   // W2, PAY_STUB, TAX_RETURN, APPRAISAL, ID_PROOF
    private String requestedBy;    // loan officer user ID
    private int    version;        // document version counter
    private boolean mandatory;     // true = loan cannot proceed without this doc
    private String dueDate;        // ISO-8601 date string
    private String notes;
}
