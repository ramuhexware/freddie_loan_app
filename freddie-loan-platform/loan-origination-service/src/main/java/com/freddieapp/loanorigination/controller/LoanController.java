package com.freddieapp.loanorigination.controller;

import com.freddieapp.loanorigination.dto.LoanApplicationRequest;
import com.freddieapp.loanorigination.dto.LoanApplicationResponse;
import com.freddieapp.loanorigination.service.LoanOriginationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@Tag(name = "Loan Origination", description = "Loan application lifecycle management APIs")
public class LoanController {

    private final LoanOriginationService loanOriginationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'ADMIN')")
    @Operation(summary = "Submit a new loan application")
    public ResponseEntity<LoanApplicationResponse> submitLoanApplication(
            @Valid @RequestBody LoanApplicationRequest request) {
        log.info("Submitting loan application for customerId={} type={} amount={}",
                request.getCustomerId(), request.getLoanType(), request.getLoanAmount());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(loanOriginationService.submitLoanApplication(request));
    }

    @GetMapping("/{loanId}")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'UNDERWRITER', 'ADMIN')")
    @Operation(summary = "Get loan application by ID")
    public ResponseEntity<LoanApplicationResponse> getLoanById(@PathVariable String loanId) {
        return ResponseEntity.ok(loanOriginationService.getLoanById(loanId));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'ADMIN')")
    @Operation(summary = "Get all loans for a customer")
    public ResponseEntity<Page<LoanApplicationResponse>> getLoansByCustomer(
            @PathVariable String customerId, Pageable pageable) {
        return ResponseEntity.ok(loanOriginationService.getLoansByCustomer(customerId, pageable));
    }

    @PostMapping("/{loanId}/submit-for-underwriting")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'ADMIN')")
    @Operation(summary = "Submit loan to underwriting service")
    public ResponseEntity<LoanApplicationResponse> submitForUnderwriting(@PathVariable String loanId) {
        log.info("Submitting loanId={} to underwriting", loanId);
        return ResponseEntity.ok(loanOriginationService.submitForUnderwriting(loanId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'UNDERWRITER', 'ADMIN')")
    @Operation(summary = "Get all loan applications (paginated)")
    public ResponseEntity<Page<LoanApplicationResponse>> getAllLoans(Pageable pageable) {
        return ResponseEntity.ok(loanOriginationService.getAllLoans(pageable));
    }
}
