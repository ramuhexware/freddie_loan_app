package com.freddieapp.loanorigination.dto;

import com.freddieapp.loanorigination.entity.LoanApplication.LoanStatus;
import com.freddieapp.loanorigination.entity.LoanApplication.LoanType;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationResponse {
    private String loanId;
    private String customerId;
    private LoanType loanType;
    private BigDecimal loanAmount;
    private BigDecimal propertyValue;
    private String propertyAddress;
    private BigDecimal interestRate;
    private Integer loanTermMonths;
    private LoanStatus loanStatus;
    private OffsetDateTime applicationDate;
    private OffsetDateTime decisionDate;
    private OffsetDateTime disbursementDate;
    private BigDecimal approvedAmount;
    private String rejectionReason;
}
