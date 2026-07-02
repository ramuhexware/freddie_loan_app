package com.freddieapp.legacyadapter.endpoints;


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


    private static final String LEGACY_NAMESPACE = "http://xmlns.oracle.com/FreddieApp/LegacySoapService";
    private static final String VERIFICATION_NAMESPACE = "http://xmlns.oracle.com/FreddieApp/CustomerVerificationService";

    @Autowired
    private WebServiceTemplate loanEligibilityTemplate;

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
