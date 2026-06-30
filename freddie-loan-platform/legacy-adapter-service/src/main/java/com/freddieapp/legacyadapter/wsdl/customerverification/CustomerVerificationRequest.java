package com.freddieapp.legacyadapter.wsdl.customerverification;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "customerId"
})
@XmlRootElement(name = "CustomerVerificationRequest")
public class CustomerVerificationRequest {

    @XmlElement(required = true)
    protected String customerId;

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String value) {
        this.customerId = value;
    }
}
