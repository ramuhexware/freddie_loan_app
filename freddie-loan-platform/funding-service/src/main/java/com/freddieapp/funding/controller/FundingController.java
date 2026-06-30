package com.freddieapp.funding.controller;

import com.freddieapp.funding.entity.Disbursement;
import com.freddieapp.funding.repository.DisbursementRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/funding")
@RequiredArgsConstructor
public class FundingController {

    private final DisbursementRepository disbursementRepository;

    @PostMapping("/disburse")
    public ResponseEntity<Disbursement> disburseFunds(@RequestBody FundingRequest request) {
        log.info("Initiating loan disbursement for loanId={} amount={}", request.getLoanId(), request.getDisbursementAmount());
        
        Disbursement disbursement = Disbursement.builder()
                .disbursementId(UUID.randomUUID().toString())
                .loanId(request.getLoanId())
                .borrowerId(request.getBorrowerId())
                .disbursementAmount(request.getDisbursementAmount())
                .bankName(request.getBankName())
                .accountNumber(request.getAccountNumber())
                .routingNumber(request.getRoutingNumber())
                .disbursementDate(LocalDate.now())
                .status("PENDING")
                .build();
                
        Disbursement saved = disbursementRepository.save(disbursement);
        
        // Simulate settlement/processing background event
        log.info("Disbursement transaction registered with status: {}", saved.getStatus());
        
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/loan/{loanId}")
    public ResponseEntity<List<Disbursement>> getDisbursementsByLoan(@PathVariable String loanId) {
        log.info("Fetching disbursement history for loanId={}", loanId);
        return ResponseEntity.ok(disbursementRepository.findByLoanId(loanId));
    }

    @PostMapping("/{id}/settle")
    public ResponseEntity<Disbursement> settleDisbursement(@PathVariable String id) {
        log.info("Settling disbursementId={}", id);
        
        Disbursement disbursement = disbursementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Disbursement transaction not found: " + id));
                
        disbursement.setStatus("COMPLETED");
        
        Disbursement updated = disbursementRepository.save(disbursement);
        return ResponseEntity.ok(updated);
    }

    @Data
    public static class FundingRequest {
        private String loanId;
        private String borrowerId;
        private BigDecimal disbursementAmount;
        private String bankName;
        private String accountNumber;
        private String routingNumber;
    }
}
