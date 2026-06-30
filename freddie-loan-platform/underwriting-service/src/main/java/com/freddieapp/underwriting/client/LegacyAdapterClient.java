package com.freddieapp.underwriting.client;

import lombok.Builder;
import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "legacy-adapter-service", path = "/api/v1/legacy")
public interface LegacyAdapterClient {

    @GetMapping("/eligibility")
    LoanEligibilityResult checkEligibility(
            @RequestParam("loanId") String loanId,
            @RequestParam("customerId") String customerId,
            @RequestParam("customerName") String customerName,
            @RequestParam("loanAmount") BigDecimal loanAmount,
            @RequestParam("propertyValue") BigDecimal propertyValue,
            @RequestParam("creditScore") int creditScore,
            @RequestParam("annualIncome") BigDecimal annualIncome,
            @RequestParam("monthlyDebt") BigDecimal monthlyDebt);

    @GetMapping("/verification")
    CustomerVerificationResult verifyCustomer(@RequestParam("customerId") String customerId);

    @Data
    @Builder
    class LoanEligibilityResult {
        private boolean eligible;
        private String reason;
        private String bureauReference;
    }

    @Data
    @Builder
    class CustomerVerificationResult {
        private boolean verified;
        private String riskLevel;
    }
}
