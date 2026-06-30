package com.freddieapp.underwriting.dto;

import com.freddieapp.underwriting.entity.UnderwritingAssessment.Decision;
import com.freddieapp.underwriting.entity.UnderwritingAssessment.RiskLevel;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnderwritingResponse {
    private String assessmentId;
    private String loanId;
    private String customerId;
    private Integer creditScore;
    private BigDecimal dtiRatio;
    private BigDecimal ltvRatio;
    private RiskLevel riskLevel;
    private Decision decision;
    private String decisionReason;
    private OffsetDateTime assessedAt;
    private String assessedBy;
    private String bureauReference;

    // Premium LLPA & Amortization Analytics
    private BigDecimal llpaSurcharge;
    private BigDecimal pmiMonthlyPremium;
    private BigDecimal baseInterestRate;
    private BigDecimal adjustedInterestRate;
    private List<AmortizationPayment> amortizationSchedule;
}
