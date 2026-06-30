package com.freddieapp.legacyadapter.wsdl.loanapproval;

import jakarta.xml.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Date;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "loanId",
    "approved",
    "interestRate",
    "riskLevel",
    "decisionReason",
    "orchestrationStatus",
    "timestamp"
})
@XmlRootElement(name = "LoanApprovalResponse")
public class LoanApprovalResponse {

    @XmlElement(required = true)
    protected String loanId;

    protected boolean approved;

    @XmlElement(required = true)
    protected BigDecimal interestRate;

    @XmlElement(required = true)
    protected String riskLevel;

    @XmlElement(required = true)
    protected String decisionReason;

    @XmlElement(required = true)
    protected String orchestrationStatus;

    @XmlElement(required = true)
    @XmlSchemaType(name = "dateTime")
    protected Date timestamp;

    public String getLoanId() { return loanId; }
    public void setLoanId(String value) { this.loanId = value; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean value) { this.approved = value; }

    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal value) { this.interestRate = value; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String value) { this.riskLevel = value; }

    public String getDecisionReason() { return decisionReason; }
    public void setDecisionReason(String value) { this.decisionReason = value; }

    public String getOrchestrationStatus() { return orchestrationStatus; }
    public void setOrchestrationStatus(String value) { this.orchestrationStatus = value; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date value) { this.timestamp = value; }
}
