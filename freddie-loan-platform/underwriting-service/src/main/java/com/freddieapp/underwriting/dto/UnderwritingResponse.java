package com.freddieapp.underwriting.dto;

import com.freddieapp.underwriting.entity.UnderwritingAssessment.Decision;
import com.freddieapp.underwriting.entity.UnderwritingAssessment.RiskLevel;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

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
}
