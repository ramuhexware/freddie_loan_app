package com.freddieapp.underwriting.service;

import com.freddieapp.underwriting.client.LegacyAdapterClient;
import com.freddieapp.underwriting.client.LegacyAdapterClient.CustomerVerificationResult;
import com.freddieapp.underwriting.client.LegacyAdapterClient.LoanEligibilityResult;
import com.freddieapp.underwriting.dto.UnderwritingRequest;
import com.freddieapp.underwriting.dto.UnderwritingResponse;
import com.freddieapp.underwriting.entity.UnderwritingAssessment;
import com.freddieapp.underwriting.entity.UnderwritingAssessment.Decision;
import com.freddieapp.underwriting.entity.UnderwritingAssessment.RiskLevel;
import com.freddieapp.underwriting.repository.UnderwritingAssessmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnderwritingEngine {

    private final UnderwritingAssessmentRepository assessmentRepository;
    private final LegacyAdapterClient legacyAdapterClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public UnderwritingResponse assessLoan(UnderwritingRequest request) {
        log.info("Starting automated underwriting assessment for loanId={}", request.getLoanId());

        // 1. Invoke Legacy SOAP System via OSB to fetch Verification and Eligibility
        CustomerVerificationResult verification = legacyAdapterClient.verifyCustomer(request.getCustomerId());
        LoanEligibilityResult eligibility = legacyAdapterClient.checkEligibility(request.getCustomerId(), request.getLoanAmount());

        // Simulate credit score from bureau data or fallback
        int creditScore = 700; // default FICO
        if (eligibility.getBureauReference() != null && !eligibility.getBureauReference().isEmpty()) {
            // parse score or derive it from reference hashes
            creditScore = Math.abs(eligibility.getBureauReference().hashCode() % 250) + 550;
        }

        // 2. Compute Ratios: DTI and LTV
        BigDecimal monthlyIncome = request.getAnnualIncome().divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);
        BigDecimal dti = BigDecimal.ZERO;
        if (monthlyIncome.compareTo(BigDecimal.ZERO) > 0) {
            dti = request.getMonthlyDebt().multiply(BigDecimal.valueOf(100)).divide(monthlyIncome, 2, RoundingMode.HALF_UP);
        }

        BigDecimal ltv = BigDecimal.ZERO;
        if (request.getPropertyValue().compareTo(BigDecimal.ZERO) > 0) {
            ltv = request.getLoanAmount().multiply(BigDecimal.valueOf(100)).divide(request.getPropertyValue(), 2, RoundingMode.HALF_UP);
        }

        // 3. Automated Risk Scoring rules
        RiskLevel riskLevel = RiskLevel.LOW;
        if (creditScore < 600 || dti.compareTo(BigDecimal.valueOf(50)) > 0 || ltv.compareTo(BigDecimal.valueOf(95)) > 0) {
            riskLevel = RiskLevel.CRITICAL;
        } else if (creditScore < 660 || dti.compareTo(BigDecimal.valueOf(45)) > 0 || ltv.compareTo(BigDecimal.valueOf(90)) > 0) {
            riskLevel = RiskLevel.HIGH;
        } else if (creditScore < 740 || dti.compareTo(BigDecimal.valueOf(36)) > 0 || ltv.compareTo(BigDecimal.valueOf(80)) > 0) {
            riskLevel = RiskLevel.MEDIUM;
        }

        // 4. Decisioning
        Decision decision = Decision.APPROVED;
        String reason = "System Approved: Criteria meets Freddie Mac lending guidelines.";

        if (riskLevel == RiskLevel.CRITICAL || !eligibility.isEligible()) {
            decision = Decision.DECLINED;
            reason = "System Declined: " + (eligibility.isEligible() ? "Exceeds risk thresholds." : eligibility.getReason());
        } else if (riskLevel == RiskLevel.HIGH || "HIGH".equals(verification.getRiskLevel())) {
            decision = Decision.REFERRED;
            reason = "System Referred: Requires manual underwriting review due to elevated risk indicators.";
        }

        String assessmentId = UUID.randomUUID().toString();
        UnderwritingAssessment assessment = UnderwritingAssessment.builder()
                .assessmentId(assessmentId)
                .loanId(request.getLoanId())
                .customerId(request.getCustomerId())
                .creditScore(creditScore)
                .dtiRatio(dti)
                .ltvRatio(ltv)
                .annualIncome(request.getAnnualIncome())
                .monthlyDebt(request.getMonthlyDebt())
                .riskLevel(riskLevel)
                .decision(decision)
                .decisionReason(reason)
                .assessedBy("AUTOMATED_ENGINE")
                .bureauReference(eligibility.getBureauReference())
                .build();

        // 5. Persist to IBM DB2 Database
        UnderwritingAssessment saved = assessmentRepository.save(assessment);
        log.info("Underwriting assessment recorded in DB2: assessmentId={}, decision={}", saved.getAssessmentId(), saved.getDecision());

        // 6. Emit Decision Event to Kafka
        kafkaTemplate.send("loan-lifecycle-events", saved.getLoanId(), saved.getDecision().name());

        return toResponse(saved);
    }

    @Transactional
    public UnderwritingResponse overrideDecision(String assessmentId, Decision decision, String reason, String underwriter) {
        log.info("Recording manual underwriting override for assessmentId={} by underwriter={}", assessmentId, underwriter);

        UnderwritingAssessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found: " + assessmentId));

        // Use DB2 Native Update query
        assessmentRepository.recordDecision(
                assessmentId,
                decision,
                assessment.getRiskLevel(),
                "Override by " + underwriter + ": " + reason,
                underwriter
        );

        // Fetch refreshed record
        UnderwritingAssessment updated = assessmentRepository.findById(assessmentId).orElseThrow();

        // Emit manual override to Kafka
        kafkaTemplate.send("loan-lifecycle-events", updated.getLoanId(), "MANUAL_OVERRIDE_" + updated.getDecision().name());

        return toResponse(updated);
    }

    private UnderwritingResponse toResponse(UnderwritingAssessment assessment) {
        return UnderwritingResponse.builder()
                .assessmentId(assessment.getAssessmentId())
                .loanId(assessment.getLoanId())
                .customerId(assessment.getCustomerId())
                .creditScore(assessment.getCreditScore())
                .dtiRatio(assessment.getDtiRatio())
                .ltvRatio(assessment.getLtvRatio())
                .riskLevel(assessment.getRiskLevel())
                .decision(assessment.getDecision())
                .decisionReason(assessment.getDecisionReason())
                .assessedAt(assessment.getAssessedAt())
                .assessedBy(assessment.getAssessedBy())
                .bureauReference(assessment.getBureauReference())
                .build();
    }
}
