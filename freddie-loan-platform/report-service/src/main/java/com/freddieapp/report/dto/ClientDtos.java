package com.freddieapp.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public class ClientDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomPageResponse<T> {
        private List<T> content;
        private int number;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean last;
        private boolean first;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoanResponse {
        private String loanId;
        private String customerId;
        private String loanType;
        private BigDecimal loanAmount;
        private BigDecimal propertyValue;
        private String propertyAddress;
        private BigDecimal interestRate;
        private Integer loanTermMonths;
        private String loanStatus;
        private OffsetDateTime applicationDate;
        private OffsetDateTime decisionDate;
        private OffsetDateTime disbursementDate;
        private BigDecimal approvedAmount;
        private String rejectionReason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnderwritingResponse {
        private String assessmentId;
        private String loanId;
        private String customerId;
        private Integer creditScore;
        private BigDecimal dtiRatio;
        private BigDecimal ltvRatio;
        private String riskLevel;
        private String decision;
        private String decisionReason;
        private OffsetDateTime assessedAt;
        private String assessedBy;
        private String bureauReference;
    }
}
