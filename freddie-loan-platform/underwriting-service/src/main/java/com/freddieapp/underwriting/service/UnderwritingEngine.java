package com.freddieapp.underwriting.service;

import com.freddieapp.underwriting.client.LegacyAdapterClient;
import com.freddieapp.underwriting.client.LegacyAdapterClient.CustomerVerificationResult;
import com.freddieapp.underwriting.client.LegacyAdapterClient.LoanEligibilityResult;
import com.freddieapp.underwriting.dto.AmortizationPayment;
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
import java.util.ArrayList;
import java.util.List;
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

        CustomerVerificationResult verification = legacyAdapterClient.verifyCustomer(request.getCustomerId());
        
        // Calculate credit score first so we can pass it to the orchestrator
        int creditScore = 700; // default FICO
        
        LoanEligibilityResult eligibility = legacyAdapterClient.checkEligibility(
                request.getLoanId(),
                request.getCustomerId(),
                "John Doe",
                request.getLoanAmount(),
                request.getPropertyValue(),
                creditScore,
                request.getAnnualIncome(),
                request.getMonthlyDebt()
        );

        if (eligibility.getBureauReference() != null && !eligibility.getBureauReference().isEmpty()) {
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

        // 4. Decisioning (Implementing Java 21 Switch Expression Pattern Matching & Record Patterns)
        record DecisionCriteria(RiskLevel riskLevel, boolean eligible, String verificationRisk) {}
        record DecisionInfo(Decision decision, String reason) {}

        DecisionCriteria criteria = new DecisionCriteria(riskLevel, eligibility.isEligible(), verification.getRiskLevel());

        DecisionInfo decisionInfo = switch (criteria) {
            case DecisionCriteria(var rl, var el, var vr) when !el -> 
                new DecisionInfo(Decision.DECLINED, "System Declined: " + eligibility.getReason());
            case DecisionCriteria(var rl, var el, var vr) when rl == RiskLevel.CRITICAL -> 
                new DecisionInfo(Decision.DECLINED, "System Declined: Exceeds risk thresholds.");
            case DecisionCriteria(var rl, var el, var vr) when rl == RiskLevel.HIGH || "HIGH".equals(vr) -> 
                new DecisionInfo(Decision.REFERRED, "System Referred: Requires manual underwriting review due to elevated risk indicators.");
            default -> 
                new DecisionInfo(Decision.APPROVED, "System Approved: Criteria meets Freddie Mac lending guidelines.");
        };

        Decision decision = decisionInfo.decision();
        String reason = decisionInfo.reason();

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

        // 7. Perform Advanced Business Calculations (LLPA, PMI, and Amortization Schedule)
        // LLPA Surcharge Calculation
        double llpaVal = 0.0;
        if (creditScore >= 740) {
            if (ltv.doubleValue() <= 60.0) llpaVal = 0.0;
            else if (ltv.doubleValue() <= 80.0) llpaVal = 0.25;
            else llpaVal = 0.50;
        } else if (creditScore >= 680) {
            if (ltv.doubleValue() <= 60.0) llpaVal = 0.50;
            else if (ltv.doubleValue() <= 80.0) llpaVal = 0.75;
            else llpaVal = 1.25;
        } else if (creditScore >= 620) {
            if (ltv.doubleValue() <= 60.0) llpaVal = 1.00;
            else if (ltv.doubleValue() <= 80.0) llpaVal = 1.75;
            else llpaVal = 2.25;
        } else {
            if (ltv.doubleValue() <= 60.0) llpaVal = 1.50;
            else if (ltv.doubleValue() <= 80.0) llpaVal = 2.50;
            else llpaVal = 3.25;
        }
        BigDecimal llpaSurcharge = BigDecimal.valueOf(llpaVal).setScale(2, RoundingMode.HALF_UP);

        // PMI Calculation
        BigDecimal pmiMonthlyPremium = BigDecimal.ZERO;
        if (ltv.doubleValue() > 80.0) {
            double pmiAnnualRate = 0.005; // 0.5% default
            if (creditScore < 680) pmiAnnualRate = 0.011; // 1.1%
            else if (creditScore < 740) pmiAnnualRate = 0.0075; // 0.75%
            pmiMonthlyPremium = request.getLoanAmount()
                    .multiply(BigDecimal.valueOf(pmiAnnualRate))
                    .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        }

        // Base & Adjusted Interest Rates
        BigDecimal baseInterestRate = BigDecimal.valueOf(6.50).setScale(2, RoundingMode.HALF_UP);
        BigDecimal adjustedInterestRate = baseInterestRate.add(llpaSurcharge).setScale(2, RoundingMode.HALF_UP);

        // Generate monthly amortization schedule (capping at 360 payments for 30-year fixed)
        List<AmortizationPayment> schedule = new ArrayList<>();
        double monthlyRateVal = adjustedInterestRate.doubleValue() / 100.0 / 12.0;
        double loanAmountVal = request.getLoanAmount().doubleValue();
        int totalMonths = 360;

        double monthlyBasePaymentVal = 0.0;
        if (monthlyRateVal > 0) {
            monthlyBasePaymentVal = loanAmountVal * (monthlyRateVal * Math.pow(1 + monthlyRateVal, totalMonths)) / (Math.pow(1 + monthlyRateVal, totalMonths) - 1);
        } else {
            monthlyBasePaymentVal = loanAmountVal / totalMonths;
        }
        BigDecimal monthlyBasePayment = BigDecimal.valueOf(monthlyBasePaymentVal).setScale(2, RoundingMode.HALF_UP);

        double remainingBalanceVal = loanAmountVal;
        double propertyValueVal = request.getPropertyValue().doubleValue();

        for (int m = 1; m <= totalMonths; m++) {
            double interestPaidVal = remainingBalanceVal * monthlyRateVal;
            double principalPaidVal = monthlyBasePaymentVal - interestPaidVal;

            if (remainingBalanceVal < principalPaidVal) {
                principalPaidVal = remainingBalanceVal;
                interestPaidVal = 0.0;
            }

            // Check if PMI is still required (LTV > 80%)
            double currentLtvVal = (remainingBalanceVal / propertyValueVal) * 100.0;
            BigDecimal currentPmi = BigDecimal.ZERO;
            if (currentLtvVal > 80.0) {
                currentPmi = pmiMonthlyPremium;
            }

            remainingBalanceVal -= principalPaidVal;
            if (remainingBalanceVal < 0) remainingBalanceVal = 0;

            BigDecimal totalPayment = BigDecimal.valueOf(principalPaidVal + interestPaidVal)
                    .add(currentPmi)
                    .setScale(2, RoundingMode.HALF_UP);

            schedule.add(AmortizationPayment.builder()
                    .monthNumber(m)
                    .principalPaid(BigDecimal.valueOf(principalPaidVal).setScale(2, RoundingMode.HALF_UP))
                    .interestPaid(BigDecimal.valueOf(interestPaidVal).setScale(2, RoundingMode.HALF_UP))
                    .pmiPaid(currentPmi)
                    .totalMonthlyPayment(totalPayment)
                    .remainingPrincipal(BigDecimal.valueOf(remainingBalanceVal).setScale(2, RoundingMode.HALF_UP))
                    .build());

            if (remainingBalanceVal <= 0) break;
        }

        // Using Java 21 Sequenced Collections to log first and last payment details
        if (!schedule.isEmpty()) {
            log.info("Amortization schedule calculated: firstPayment={}, lastPaymentRemainingPrincipal={}",
                    schedule.getFirst().getTotalMonthlyPayment(),
                    schedule.getLast().getRemainingPrincipal());
        }

        UnderwritingResponse response = toResponse(saved);
        response.setLlpaSurcharge(llpaSurcharge);
        response.setPmiMonthlyPremium(pmiMonthlyPremium);
        response.setBaseInterestRate(baseInterestRate);
        response.setAdjustedInterestRate(adjustedInterestRate);
        response.setAmortizationSchedule(schedule);

        return response;
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
