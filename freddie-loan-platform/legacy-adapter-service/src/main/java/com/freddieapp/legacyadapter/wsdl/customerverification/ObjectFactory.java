package com.freddieapp.legacyadapter.wsdl.customerverification;

import jakarta.xml.bind.annotation.XmlRegistry;

@XmlRegistry
public class ObjectFactory {

    public ObjectFactory() {}

    public CustomerVerificationRequest createCustomerVerificationRequest() {
        return new CustomerVerificationRequest();
    }

    public CustomerVerificationResponse createCustomerVerificationResponse() {
        return new CustomerVerificationResponse();
    }
}
