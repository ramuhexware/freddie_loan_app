package com.freddieapp.underwriting.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnderwritingRequest {

    @NotBlank(message = "Loan ID is required")
    private String loanId;

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotNull(message = "Loan Amount is required")
    private BigDecimal loanAmount;

    @NotNull(message = "Property Value is required")
    private BigDecimal propertyValue;

    @NotNull(message = "Annual Income is required")
    private BigDecimal annualIncome;

    @NotNull(message = "Monthly Debt is required")
    private BigDecimal monthlyDebt;
}
