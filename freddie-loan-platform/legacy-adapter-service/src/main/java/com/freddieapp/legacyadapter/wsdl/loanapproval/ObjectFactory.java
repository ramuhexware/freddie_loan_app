package com.freddieapp.legacyadapter.wsdl.loanapproval;

import jakarta.xml.bind.annotation.XmlRegistry;

@XmlRegistry
public class ObjectFactory {

    public ObjectFactory() {}

    public LoanApprovalRequest createLoanApprovalRequest() {
        return new LoanApprovalRequest();
    }

    public LoanApprovalResponse createLoanApprovalResponse() {
        return new LoanApprovalResponse();
    }
}
