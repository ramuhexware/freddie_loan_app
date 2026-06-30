package com.freddieapp.legacyadapter.wsdl.loaneligibility;

import jakarta.xml.bind.annotation.XmlRegistry;

@XmlRegistry
public class ObjectFactory {

    public ObjectFactory() {}

    public LoanEligibilityRequest createLoanEligibilityRequest() {
        return new LoanEligibilityRequest();
    }

    public LoanEligibilityResponse createLoanEligibilityResponse() {
        return new LoanEligibilityResponse();
    }
}
