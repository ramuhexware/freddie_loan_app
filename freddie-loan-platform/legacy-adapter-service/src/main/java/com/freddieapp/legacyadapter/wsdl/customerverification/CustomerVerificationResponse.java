package com.freddieapp.legacyadapter.wsdl.customerverification;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "verified",
    "riskLevel"
})
@XmlRootElement(name = "CustomerVerificationResponse")
public class CustomerVerificationResponse {

    protected boolean verified;
    @XmlElement(required = true)
    protected String riskLevel;

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean value) {
        this.verified = value;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String value) {
        this.riskLevel = value;
    }
}
