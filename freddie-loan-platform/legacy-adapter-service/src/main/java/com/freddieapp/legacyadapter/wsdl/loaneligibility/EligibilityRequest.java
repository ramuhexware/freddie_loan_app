package com.freddieapp.legacyadapter.wsdl.loaneligibility;

import jakarta.xml.bind.annotation.*;
import java.math.BigDecimal;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "loanId",
    "customerId",
    "loanAmount",
    "creditScore",
    "monthlyDti"
})
@XmlRootElement(name = "EligibilityRequest")
public class EligibilityRequest {

    @XmlElement(required = true)
    protected String loanId;

    @XmlElement(required = true)
    protected String customerId;

    @XmlElement(required = true)
    protected BigDecimal loanAmount;

    protected int creditScore;

    @XmlElement(required = true)
    protected BigDecimal monthlyDti;

    public String getLoanId() { return loanId; }
    public void setLoanId(String value) { this.loanId = value; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String value) { this.customerId = value; }

    public BigDecimal getLoanAmount() { return loanAmount; }
    public void setLoanAmount(BigDecimal value) { this.loanAmount = value; }

    public int getCreditScore() { return creditScore; }
    public void setCreditScore(int value) { this.creditScore = value; }

    public BigDecimal getMonthlyDti() { return monthlyDti; }
    public void setMonthlyDti(BigDecimal value) { this.monthlyDti = value; }
}
