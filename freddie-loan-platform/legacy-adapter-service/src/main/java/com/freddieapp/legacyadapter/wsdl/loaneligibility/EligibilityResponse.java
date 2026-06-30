package com.freddieapp.legacyadapter.wsdl.loaneligibility;

import jakarta.xml.bind.annotation.*;
import java.math.BigDecimal;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "eligible",
    "baseRate",
    "comments"
})
@XmlRootElement(name = "EligibilityResponse")
public class EligibilityResponse {

    protected boolean eligible;

    @XmlElement(required = true)
    protected BigDecimal baseRate;

    @XmlElement(required = true)
    protected String comments;

    public boolean isEligible() { return eligible; }
    public void setEligible(boolean value) { this.eligible = value; }

    public BigDecimal getBaseRate() { return baseRate; }
    public void setBaseRate(BigDecimal value) { this.baseRate = value; }

    public String getComments() { return comments; }
    public void setComments(String value) { this.comments = value; }
}
