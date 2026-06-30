package com.freddieapp.legacyadapter.wsdl.loaneligibility;

import jakarta.xml.bind.annotation.XmlRegistry;

@XmlRegistry
public class ObjectFactory {

    public ObjectFactory() {}

    public EligibilityRequest createEligibilityRequest() {
        return new EligibilityRequest();
    }

    public EligibilityResponse createEligibilityResponse() {
        return new EligibilityResponse();
    }
}
