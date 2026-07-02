package com.freddieapp.legacyadapter.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class LegacySoapClient {

    private final WebServiceTemplate loanEligibilityTemplate;
    private final WebServiceTemplate customerVerificationTemplate;

    @CircuitBreaker(name = "legacy-loan-eligibility", fallbackMethod = "loanEligibilityFallback")
    @Retry(name = "legacy-loan-eligibility")
    public LoanEligibilityResult checkLoanEligibility(
            String loanId,
            String customerId,
            String customerName,
            java.math.BigDecimal loanAmount,
            java.math.BigDecimal propertyValue,
            int creditScore,
            java.math.BigDecimal annualIncome,
            java.math.BigDecimal monthlyDebt) {
        log.info("Calling legacy loan eligibility: loanId={} customerId={} amount={}", loanId, customerId, loanAmount);
        
        // 1. Credit Score Pre-qualification Gate
        if (creditScore < 600) {
            log.warn("Credit score below 600 pre-qual gate. Auto-declining loanId={}", loanId);
            return LoanEligibilityResult.builder()
                    .eligible(false)
                    .reason("FICO credit score below minimum pre-qualification gate (600).")
                    .bureauReference("Auto-Declined by Pre-Qual Gate")
                    .build();
        }

        // 2. DTI Calculation
        java.math.BigDecimal monthlyIncome = annualIncome.divide(java.math.BigDecimal.valueOf(12), 4, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal monthlyDti = java.math.BigDecimal.ZERO;
        if (monthlyIncome.compareTo(java.math.BigDecimal.ZERO) > 0) {
            monthlyDti = monthlyDebt.multiply(java.math.BigDecimal.valueOf(100)).divide(monthlyIncome, 2, java.math.RoundingMode.HALF_UP);
        }

        try {
            com.freddieapp.legacyadapter.wsdl.loaneligibility.EligibilityRequest request = 
                    new com.freddieapp.legacyadapter.wsdl.loaneligibility.EligibilityRequest();
            request.setLoanId(loanId);
            request.setCustomerId(customerId);
            request.setLoanAmount(loanAmount);
            request.setCreditScore(creditScore);
            request.setMonthlyDti(monthlyDti);

            com.freddieapp.legacyadapter.wsdl.loaneligibility.EligibilityResponse response = 
                    (com.freddieapp.legacyadapter.wsdl.loaneligibility.EligibilityResponse) loanEligibilityTemplate
                            .marshalSendAndReceive(request);

            return LoanEligibilityResult.builder()
                    .eligible(response.isEligible())
                    .reason(response.getComments())
                    .bureauReference("Orchestrated Successfully via Legacy SOAP")
                    .build();
        } catch (SoapFaultClientException ex) {
            log.error("SOAP fault from legacy loan eligibility: {}", ex.getFaultStringOrReason());
            throw new LegacySoapException("Loan eligibility SOAP fault: " + ex.getFaultStringOrReason(), ex);
        }
    }

    @CircuitBreaker(name = "legacy-customer-verification", fallbackMethod = "customerVerificationFallback")
    @Retry(name = "legacy-customer-verification")
    public CustomerVerificationResult verifyCustomer(String customerId) {
        log.info("Calling legacy customer verification via OSB: customerId={}", customerId);
        try {
            com.freddieapp.legacyadapter.wsdl.customerverification.CustomerVerificationRequest request = 
                    new com.freddieapp.legacyadapter.wsdl.customerverification.CustomerVerificationRequest();
            request.setCustomerId(customerId);

            com.freddieapp.legacyadapter.wsdl.customerverification.CustomerVerificationResponse response = 
                    (com.freddieapp.legacyadapter.wsdl.customerverification.CustomerVerificationResponse) customerVerificationTemplate
                            .marshalSendAndReceive(request);

            return CustomerVerificationResult.builder()
                    .verified(response.isVerified())
                    .riskLevel(response.getRiskLevel())
                    .build();
        } catch (SoapFaultClientException ex) {
            log.error("SOAP fault from legacy customer verification: {}", ex.getFaultStringOrReason());
            throw new LegacySoapException("Customer verification SOAP fault: " + ex.getFaultStringOrReason(), ex);
        }
    }

    // ─── Fallback Methods ─────────────────────────────────────────────────────

    public LoanEligibilityResult loanEligibilityFallback(
            String loanId,
            String customerId,
            String customerName,
            java.math.BigDecimal loanAmount,
            java.math.BigDecimal propertyValue,
            int creditScore,
            java.math.BigDecimal annualIncome,
            java.math.BigDecimal monthlyDebt,
            Throwable ex) {
        log.warn("Loan eligibility circuit open for customerId={}. Returning default. Error: {}", customerId, ex.getMessage());
        return LoanEligibilityResult.builder().eligible(false).reason("Legacy system temporarily unavailable").build();
    }

    public CustomerVerificationResult customerVerificationFallback(String customerId, Throwable ex) {
        log.warn("Customer verification circuit open for customerId={}. Returning default.", customerId);
        return CustomerVerificationResult.builder().verified(false).build();
    }

    // ─── Result DTOs ──────────────────────────────────────────────────────────

    @lombok.Builder @lombok.Data
    public static class LoanEligibilityResult {
        private boolean eligible;
        private String reason;
        private String bureauReference;
    }

    @lombok.Builder @lombok.Data
    public static class CustomerVerificationResult {
        private boolean verified;
        private String riskLevel;
    }
}
