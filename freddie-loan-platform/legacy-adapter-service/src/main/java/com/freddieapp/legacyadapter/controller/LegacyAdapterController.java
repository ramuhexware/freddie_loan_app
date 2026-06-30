package com.freddieapp.legacyadapter.controller;

import com.freddieapp.legacyadapter.client.LegacySoapClient;
import com.freddieapp.legacyadapter.client.LegacySoapClient.LoanEligibilityResult;
import com.freddieapp.legacyadapter.client.LegacySoapClient.CustomerVerificationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/api/v1/legacy")
@RequiredArgsConstructor
public class LegacyAdapterController {

    private final LegacySoapClient legacySoapClient;

    @GetMapping("/eligibility")
    public ResponseEntity<LoanEligibilityResult> checkLoanEligibility(
            @RequestParam String loanId,
            @RequestParam String customerId,
            @RequestParam String customerName,
            @RequestParam BigDecimal loanAmount,
            @RequestParam BigDecimal propertyValue,
            @RequestParam int creditScore,
            @RequestParam BigDecimal annualIncome,
            @RequestParam BigDecimal monthlyDebt) {
        log.info("REST request to check legacy eligibility: loanId={}, customerId={}, amount={}", loanId, customerId, loanAmount);
        return ResponseEntity.ok(legacySoapClient.checkLoanEligibility(
                loanId, customerId, customerName, loanAmount, propertyValue, creditScore, annualIncome, monthlyDebt));
    }

    @GetMapping("/verification")
    public ResponseEntity<CustomerVerificationResult> verifyCustomer(
            @RequestParam String customerId) {
        log.info("REST request to verify legacy customer: customerId={}", customerId);
        return ResponseEntity.ok(legacySoapClient.verifyCustomer(customerId));
    }
}
