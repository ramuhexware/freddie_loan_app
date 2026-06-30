package com.freddieapp.legacyadapter.wsdl.loaneligibility;

import jakarta.xml.bind.annotation.*;
import java.math.BigDecimal;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "customerId",
    "loanAmount"
})
@XmlRootElement(name = "LoanEligibilityRequest")
public class LoanEligibilityRequest {

    @XmlElement(required = true)
    protected String customerId;

    @XmlElement(required = true)
    protected BigDecimal loanAmount;

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String value) {
        this.customerId = value;
    }

    public BigDecimal getLoanAmount() {
        return loanAmount;
    }

    public void setLoanAmount(BigDecimal value) {
        this.loanAmount = value;
    }
}
