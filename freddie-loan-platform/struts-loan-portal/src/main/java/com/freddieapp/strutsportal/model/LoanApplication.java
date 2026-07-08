package com.freddieapp.strutsportal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Struts-layer model for a Loan Application.
 * Maps to FREDDIE_LOANS.LOAN_APPLICATIONS in DB2.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplication {

    private String      loanId;
    private String      customerId;
    private String      customerName;       // joined from CUSTOMERS
    private String      loanType;           // PURCHASE | REFINANCE | HELOC | HOME_EQUITY
    private BigDecimal  loanAmount;
    private BigDecimal  propertyValue;
    private String      propertyAddress;
    private BigDecimal  interestRate;
    private Integer     loanTermMonths;
    private String      loanStatus;         // PENDING | SUBMITTED | UNDER_REVIEW | APPROVED | REJECTED | DISBURSED | CLOSED
    private LocalDateTime applicationDate;
    private LocalDateTime decisionDate;
    private LocalDateTime disbursementDate;
    private BigDecimal  approvedAmount;
    private String      rejectionReason;
    private String      createdBy;
    private LocalDateTime updatedAt;

    // -------  Form-binding fields (used in apply.jsp)  -------
    // These are populated from HTTP request parameters by Struts 2
}
