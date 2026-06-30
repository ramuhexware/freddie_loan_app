package com.freddieapp.underwriting.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "UNDERWRITING_ASSESSMENTS", schema = "FREDDIE_UW")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnderwritingAssessment {

    @Id
    @Column(name = "ASSESSMENT_ID", length = 36, updatable = false, nullable = false)
    private String assessmentId;

    @Column(name = "LOAN_ID", nullable = false, length = 36)
    private String loanId;

    @Column(name = "CUSTOMER_ID", nullable = false, length = 36)
    private String customerId;

    @Column(name = "CREDIT_SCORE")
    private Integer creditScore;

    @Column(name = "DTI_RATIO", precision = 5, scale = 2)
    private BigDecimal dtiRatio;           // Debt-to-Income %

    @Column(name = "LTV_RATIO", precision = 5, scale = 2)
    private BigDecimal ltvRatio;           // Loan-to-Value %

    @Column(name = "ANNUAL_INCOME", precision = 18, scale = 2)
    private BigDecimal annualIncome;

    @Column(name = "MONTHLY_DEBT", precision = 12, scale = 2)
    private BigDecimal monthlyDebt;

    @Enumerated(EnumType.STRING)
    @Column(name = "RISK_LEVEL", length = 20)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "DECISION", length = 20)
    private Decision decision;

    @Column(name = "DECISION_REASON", length = 2000)
    private String decisionReason;

    @CreationTimestamp
    @Column(name = "ASSESSED_AT", updatable = false)
    private OffsetDateTime assessedAt;

    @Column(name = "ASSESSED_BY", length = 100)
    private String assessedBy;

    @Column(name = "BUREAU_REF", length = 255)
    private String bureauReference;        // Credit bureau reference ID

    public enum RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }
    public enum Decision  { APPROVED, REFERRED, DECLINED }
}
