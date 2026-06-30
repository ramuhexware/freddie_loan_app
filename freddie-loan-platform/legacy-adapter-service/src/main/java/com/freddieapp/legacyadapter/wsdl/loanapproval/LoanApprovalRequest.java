package com.freddieapp.legacyadapter.wsdl.loanapproval;

import jakarta.xml.bind.annotation.*;
import java.math.BigDecimal;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "loanId",
    "customerId",
    "customerName",
    "loanAmount",
    "propertyValue",
    "creditScore",
    "annualIncome",
    "monthlyDebt"
})
@XmlRootElement(name = "LoanApprovalRequest")
public class LoanApprovalRequest {

    @XmlElement(required = true)
    protected String loanId;

    @XmlElement(required = true)
    protected String customerId;

    @XmlElement(required = true)
    protected String customerName;

    @XmlElement(required = true)
    protected BigDecimal loanAmount;

    @XmlElement(required = true)
    protected BigDecimal propertyValue;

    protected int creditScore;

    @XmlElement(required = true)
    protected BigDecimal annualIncome;

    @XmlElement(required = true)
    protected BigDecimal monthlyDebt;

    public String getLoanId() { return loanId; }
    public void setLoanId(String value) { this.loanId = value; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String value) { this.customerId = value; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String value) { this.customerName = value; }

    public BigDecimal getLoanAmount() { return loanAmount; }
    public void setLoanAmount(BigDecimal value) { this.loanAmount = value; }

    public BigDecimal getPropertyValue() { return propertyValue; }
    public void setPropertyValue(BigDecimal value) { this.propertyValue = value; }

    public int getCreditScore() { return creditScore; }
    public void setCreditScore(int value) { this.creditScore = value; }

    public BigDecimal getAnnualIncome() { return annualIncome; }
    public void setAnnualIncome(BigDecimal value) { this.annualIncome = value; }

    public BigDecimal getMonthlyDebt() { return monthlyDebt; }
    public void setMonthlyDebt(BigDecimal value) { this.monthlyDebt = value; }
}
