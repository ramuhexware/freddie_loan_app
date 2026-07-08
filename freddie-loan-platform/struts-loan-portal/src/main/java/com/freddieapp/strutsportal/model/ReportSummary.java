package com.freddieapp.strutsportal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO representing a single row in loan summary / report views.
 * Populated by aggregation queries from ReportDao.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSummary {

    private String     category;       // loan type or status label
    private Long       count;
    private BigDecimal totalAmount;
    private BigDecimal avgAmount;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private String     period;         // "2024-01" for monthly grouping
}
