package com.freddieapp.underwriting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmortizationPayment {
    private int monthNumber;
    private BigDecimal principalPaid;
    private BigDecimal interestPaid;
    private BigDecimal pmiPaid;
    private BigDecimal totalMonthlyPayment;
    private BigDecimal remainingPrincipal;
}
