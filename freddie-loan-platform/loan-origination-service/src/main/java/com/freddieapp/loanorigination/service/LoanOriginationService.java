package com.freddieapp.loanorigination.service;

import com.freddieapp.loanorigination.client.CustomerServiceClient;
import com.freddieapp.loanorigination.dto.CustomerDto;
import com.freddieapp.loanorigination.dto.LoanApplicationRequest;
import com.freddieapp.loanorigination.dto.LoanApplicationResponse;
import com.freddieapp.loanorigination.entity.LoanApplication;
import com.freddieapp.loanorigination.entity.LoanApplication.LoanStatus;
import com.freddieapp.loanorigination.entity.LoanStatusHistory;
import com.freddieapp.loanorigination.repository.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanOriginationService {

    private final LoanApplicationRepository loanRepository;
    private final CustomerServiceClient customerServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public LoanApplicationResponse submitLoanApplication(LoanApplicationRequest request) {
        log.info("Verifying customer status with customer-service for customerId={}", request.getCustomerId());
        // Verify customer exists and is active using FeignClient
        CustomerDto customer = customerServiceClient.getCustomerById(UUID.fromString(request.getCustomerId()));
        if ("INACTIVE".equals(customer.getCustomerStatus())) {
            throw new IllegalArgumentException("Cannot submit loan application for an inactive customer");
        }

        String loanId = UUID.randomUUID().toString();
        LoanApplication loan = LoanApplication.builder()
                .loanId(loanId)
                .customerId(request.getCustomerId())
                .loanType(request.getLoanType())
                .loanAmount(request.getLoanAmount())
                .propertyValue(request.getPropertyValue())
                .propertyAddress(request.getPropertyAddress())
                .loanTermMonths(request.getLoanTermMonths())
                .loanStatus(LoanStatus.SUBMITTED)
                .createdBy("SYSTEM_USER")
                .build();

        // Add initial status history
        loan.getStatusHistory().add(LoanStatusHistory.builder()
                .historyId(UUID.randomUUID().toString())
                .loanApplication(loan)
                .fromStatus(null)
                .toStatus(LoanStatus.SUBMITTED.name())
                .notes("Loan application created and submitted successfully.")
                .changedBy("SYSTEM_USER")
                .build());

        LoanApplication saved = loanRepository.save(loan);
        log.info("Loan application submitted: loanId={}", saved.getLoanId());

        // Publish event to Kafka
        kafkaTemplate.send("loan-events", saved.getLoanId(), "SUBMITTED");

        return toResponse(saved);
    }

    public LoanApplicationResponse getLoanById(String loanId) {
        return loanRepository.findById(loanId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Loan application not found: " + loanId));
    }

    public Page<LoanApplicationResponse> getLoansByCustomer(String customerId, Pageable pageable) {
        return loanRepository.findByCustomerId(customerId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public LoanApplicationResponse submitForUnderwriting(String loanId) {
        LoanApplication loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan application not found: " + loanId));

        if (loan.getLoanStatus() != LoanStatus.SUBMITTED) {
            throw new IllegalStateException("Loan must be in SUBMITTED state to submit for underwriting");
        }

        // Transition status using native SQL query
        loanRepository.submitForUnderwriting(loanId);

        // Fetch fresh copy to return
        LoanApplication updatedLoan = loanRepository.findById(loanId).orElseThrow();
        
        // Record status change history
        LoanStatusHistory history = LoanStatusHistory.builder()
                .historyId(UUID.randomUUID().toString())
                .loanApplication(updatedLoan)
                .fromStatus(LoanStatus.SUBMITTED.name())
                .toStatus(LoanStatus.UNDER_REVIEW.name())
                .notes("Submitted for Underwriting Review")
                .changedBy("SYSTEM_USER")
                .build();
        updatedLoan.getStatusHistory().add(history);
        loanRepository.save(updatedLoan);

        kafkaTemplate.send("loan-events", loanId, "UNDER_REVIEW");
        log.info("Loan application transitioned to UNDER_REVIEW: loanId={}", loanId);

        return toResponse(updatedLoan);
    }

    public Page<LoanApplicationResponse> getAllLoans(Pageable pageable) {
        return loanRepository.findAll(pageable)
                .map(this::toResponse);
    }

    private LoanApplicationResponse toResponse(LoanApplication loan) {
        return LoanApplicationResponse.builder()
                .loanId(loan.getLoanId())
                .customerId(loan.getCustomerId())
                .loanType(loan.getLoanType())
                .loanAmount(loan.getLoanAmount())
                .propertyValue(loan.getPropertyValue())
                .propertyAddress(loan.getPropertyAddress())
                .interestRate(loan.getInterestRate())
                .loanTermMonths(loan.getLoanTermMonths())
                .loanStatus(loan.getLoanStatus())
                .applicationDate(loan.getApplicationDate())
                .decisionDate(loan.getDecisionDate())
                .disbursementDate(loan.getDisbursementDate())
                .approvedAmount(loan.getApprovedAmount())
                .rejectionReason(loan.getRejectionReason())
                .build();
    }
}
