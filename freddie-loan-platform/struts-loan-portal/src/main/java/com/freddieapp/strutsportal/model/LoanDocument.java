package com.freddieapp.strutsportal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Struts-layer model for a Loan Document.
 * Maps to FREDDIE_LOANS.LOAN_DOCUMENTS in DB2.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanDocument {

    private String        documentId;
    private String        loanId;
    private String        customerId;
    private String        documentType;       // PAY_STUB | APPRAISAL | TAX_RETURN | BANK_STATEMENT | OTHER
    private String        originalFileName;
    private String        storedFileName;     // UUID-based name on disk
    private String        filePath;           // absolute path on server
    private Long          fileSizeBytes;
    private String        mimeType;
    private String        uploadedBy;
    private String        status;             // PENDING_REVIEW | APPROVED | REJECTED
    private String        reviewNotes;
    private LocalDateTime uploadedAt;
    private LocalDateTime reviewedAt;
}
