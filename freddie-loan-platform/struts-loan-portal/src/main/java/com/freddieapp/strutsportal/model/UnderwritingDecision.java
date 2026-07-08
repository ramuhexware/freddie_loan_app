package com.freddieapp.strutsportal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Struts-layer model for an Underwriting Decision.
 * Maps to FREDDIE_LOANS.UNDERWRITING_DECISIONS in DB2.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnderwritingDecision {

    private String        decisionId;
    private String        loanId;
    private String        underwriterId;
    private String        underwriterName;
    private String        decision;           // APPROVED | REJECTED | CONDITIONAL
    private BigDecimal    approvedAmount;
    private BigDecimal    recommendedRate;
    private String        conditions;
    private String        rejectionReason;
    private Integer       debtToIncomeRatio;
    private Integer       loanToValueRatio;
    private Integer       creditScoreUsed;
    private String        riskCategory;       // LOW | MEDIUM | HIGH
    private String        notes;
    private LocalDateTime decisionDate;
    private LocalDateTime createdAt;
}
