package com.freddieapp.loanorigination.dto;

import com.freddieapp.loanorigination.entity.LoanApplication.LoanType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationRequest {

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotNull(message = "Loan Type is required")
    private LoanType loanType;

    @NotNull(message = "Loan Amount is required")
    @DecimalMin(value = "0.01", message = "Loan amount must be positive")
    private BigDecimal loanAmount;

    @DecimalMin(value = "0.01", message = "Property value must be positive")
    private BigDecimal propertyValue;

    @Size(max = 500, message = "Property address must be less than 500 characters")
    private String propertyAddress;

    @Min(value = 1, message = "Loan term must be at least 1 month")
    private Integer loanTermMonths;
}
