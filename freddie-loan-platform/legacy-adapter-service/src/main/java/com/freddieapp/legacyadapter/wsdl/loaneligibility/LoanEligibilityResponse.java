package com.freddieapp.legacyadapter.wsdl.loaneligibility;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "eligible",
    "reason",
    "bureauReference"
})
@XmlRootElement(name = "LoanEligibilityResponse")
public class LoanEligibilityResponse {

    protected boolean eligible;
    @XmlElement(required = true)
    protected String reason;
    @XmlElement(required = true)
    protected String bureauReference;

    public boolean isEligible() {
        return eligible;
    }

    public void setEligible(boolean value) {
        this.eligible = value;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String value) {
        this.reason = value;
    }

    public String getBureauReference() {
        return bureauReference;
    }

    public void setBureauReference(String value) {
        this.bureauReference = value;
    }
}
