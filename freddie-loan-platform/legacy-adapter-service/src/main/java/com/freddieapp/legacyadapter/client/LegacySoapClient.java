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
    public LoanEligibilityResult checkLoanEligibility(String customerId, java.math.BigDecimal loanAmount) {
        log.info("Calling legacy loan eligibility via OSB: customerId={} amount={}", customerId, loanAmount);
        try {
            com.freddieapp.legacyadapter.wsdl.loaneligibility.LoanEligibilityRequest request = 
                    new com.freddieapp.legacyadapter.wsdl.loaneligibility.LoanEligibilityRequest();
            request.setCustomerId(customerId);
            request.setLoanAmount(loanAmount);

            com.freddieapp.legacyadapter.wsdl.loaneligibility.LoanEligibilityResponse response = 
                    (com.freddieapp.legacyadapter.wsdl.loaneligibility.LoanEligibilityResponse) loanEligibilityTemplate
                            .marshalSendAndReceive(request);

            return LoanEligibilityResult.builder()
                    .eligible(response.isEligible())
                    .reason(response.getReason())
                    .bureauReference(response.getBureauReference())
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

    public LoanEligibilityResult loanEligibilityFallback(String customerId,
            java.math.BigDecimal loanAmount, Throwable ex) {
        log.warn("Loan eligibility circuit open for customerId={}. Returning default.", customerId);
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
