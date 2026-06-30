package com.freddieapp.legacyadapter.endpoints;

import com.freddieapp.legacyadapter.wsdl.loanapproval.LoanApprovalRequest;
import com.freddieapp.legacyadapter.wsdl.loanapproval.LoanApprovalResponse;
import com.freddieapp.legacyadapter.wsdl.loaneligibility.EligibilityRequest;
import com.freddieapp.legacyadapter.wsdl.loaneligibility.EligibilityResponse;
import com.freddieapp.legacyadapter.wsdl.customerverification.CustomerVerificationRequest;
import com.freddieapp.legacyadapter.wsdl.customerverification.CustomerVerificationResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

@Slf4j
@Endpoint
public class SoapEndpoints {

    private static final String BPEL_NAMESPACE = "http://xmlns.oracle.com/FreddieApp/LoanApprovalProcess";
    private static final String LEGACY_NAMESPACE = "http://xmlns.oracle.com/FreddieApp/LegacySoapService";
    private static final String VERIFICATION_NAMESPACE = "http://xmlns.oracle.com/FreddieApp/CustomerVerificationService";

    @Autowired
    private WebServiceTemplate loanEligibilityTemplate;

    @PayloadRoot(namespace = BPEL_NAMESPACE, localPart = "LoanApprovalRequest")
    @ResponsePayload
    public LoanApprovalResponse processLoanApproval(@RequestPayload LoanApprovalRequest request) {
        log.info("SOAP Endpoint - LoanApprovalRequest received: loanId={}, customerId={}, creditScore={}",
                request.getLoanId(), request.getCustomerId(), request.getCreditScore());

        LoanApprovalResponse response = new LoanApprovalResponse();
        response.setLoanId(request.getLoanId());
        response.setTimestamp(new Date());

        // Step 1: Credit Score Pre-qualification Gate (BPEL line 50)
        if (request.getCreditScore() < 600) {
            log.warn("SOAP Endpoint - Credit score below 600 pre-qual gate. Auto-declining loanId={}", request.getLoanId());
            response.setApproved(false);
            response.setInterestRate(BigDecimal.ZERO);
            response.setRiskLevel("HIGH");
            response.setDecisionReason("FICO credit score below minimum pre-qualification gate (600).");
            response.setOrchestrationStatus("Auto-Declined by BPEL Pre-Qual Gate");
            return response;
        }

        // Step 2: DTI Calculation (BPEL line 108)
        BigDecimal monthlyDebt = request.getMonthlyDebt();
        BigDecimal annualIncome = request.getAnnualIncome();
        BigDecimal monthlyIncome = annualIncome.divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);
        BigDecimal monthlyDti = BigDecimal.ZERO;
        if (monthlyIncome.compareTo(BigDecimal.ZERO) > 0) {
            monthlyDti = monthlyDebt.multiply(BigDecimal.valueOf(100)).divide(monthlyIncome, 2, RoundingMode.HALF_UP);
        }

        log.info("SOAP Endpoint - Calculated DTI for loanId={}: {}%", request.getLoanId(), monthlyDti);

        // Step 3: Invoke Legacy OSB Service (BPEL line 115)
        EligibilityRequest legacyReq = new EligibilityRequest();
        legacyReq.setLoanId(request.getLoanId());
        legacyReq.setCustomerId(request.getCustomerId());
        legacyReq.setLoanAmount(request.getLoanAmount());
        legacyReq.setCreditScore(request.getCreditScore());
        legacyReq.setMonthlyDti(monthlyDti);

        try {
            log.info("SOAP Endpoint - Invoking Legacy Soap Port with EligibilityRequest...");
            EligibilityResponse legacyResp = (EligibilityResponse) loanEligibilityTemplate.marshalSendAndReceive(legacyReq);

            // Step 4: Map Response based on Legacy evaluation (BPEL line 119)
            response.setApproved(legacyResp.isEligible());
            response.setInterestRate(legacyResp.getBaseRate());
            response.setRiskLevel(request.getCreditScore() >= 740 ? "LOW" : "MEDIUM");
            response.setDecisionReason(legacyResp.getComments());
            response.setOrchestrationStatus("Orchestrated Successfully via BPEL Process");
        } catch (Exception e) {
            log.error("SOAP Endpoint - Failed to invoke legacy soap eligibility service: {}", e.getMessage(), e);
            response.setApproved(false);
            response.setInterestRate(BigDecimal.ZERO);
            response.setRiskLevel("HIGH");
            response.setDecisionReason("Failed to invoke legacy soap eligibility service: " + e.getMessage());
            response.setOrchestrationStatus("Failed in BPEL Orchestration");
        }

        return response;
    }

    @PayloadRoot(namespace = LEGACY_NAMESPACE, localPart = "EligibilityRequest")
    @ResponsePayload
    public EligibilityResponse checkEligibility(@RequestPayload EligibilityRequest request) {
        log.info("SOAP Endpoint - EligibilityRequest received: customerId={}, amount={}, score={}, dti={}",
                request.getCustomerId(), request.getLoanAmount(), request.getCreditScore(), request.getMonthlyDti());

        EligibilityResponse response = new EligibilityResponse();

        // Business rules simulation
        if (request.getLoanAmount().compareTo(BigDecimal.valueOf(500000)) > 0) {
            response.setEligible(false);
            response.setBaseRate(BigDecimal.ZERO);
            response.setComments("Declined: Loan amount exceeds legacy maximum limit ($500,000).");
        } else if (request.getMonthlyDti().compareTo(BigDecimal.valueOf(45.0)) > 0) {
            response.setEligible(false);
            response.setBaseRate(BigDecimal.ZERO);
            response.setComments("Declined: Debt-to-Income ratio exceeds maximum limit (45.0%).");
        } else {
            response.setEligible(true);
            BigDecimal baseRate = BigDecimal.valueOf(6.25);
            if (request.getCreditScore() >= 740) {
                baseRate = BigDecimal.valueOf(5.75);
            } else if (request.getCreditScore() >= 680) {
                baseRate = BigDecimal.valueOf(6.00);
            }
            response.setBaseRate(baseRate);
            response.setComments("Legacy Approved: Loan satisfies underwriting criteria.");
        }

        log.info("SOAP Endpoint - Returning EligibilityResponse: eligible={}, rate={}, comments={}",
                response.isEligible(), response.getBaseRate(), response.getComments());
        return response;
    }

    @PayloadRoot(namespace = VERIFICATION_NAMESPACE, localPart = "CustomerVerificationRequest")
    @ResponsePayload
    public CustomerVerificationResponse verifyCustomer(@RequestPayload CustomerVerificationRequest request) {
        log.info("SOAP Endpoint - CustomerVerificationRequest received: customerId={}", request.getCustomerId());

        CustomerVerificationResponse response = new CustomerVerificationResponse();
        response.setVerified(true);
        response.setRiskLevel("LOW");

        return response;
    }
}
