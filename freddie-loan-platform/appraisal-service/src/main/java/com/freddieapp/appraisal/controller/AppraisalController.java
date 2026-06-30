package com.freddieapp.appraisal.controller;

import com.freddieapp.appraisal.entity.Appraisal;
import com.freddieapp.appraisal.repository.AppraisalRepository;
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
@RequestMapping("/api/appraisals")
@RequiredArgsConstructor
public class AppraisalController {

    private final AppraisalRepository appraisalRepository;

    @PostMapping
    public ResponseEntity<Appraisal> scheduleAppraisal(@RequestBody AppraisalRequest request) {
        log.info("Scheduling appraisal for loanId={}", request.getLoanId());
        
        Appraisal appraisal = Appraisal.builder()
                .appraisalId(UUID.randomUUID().toString())
                .loanId(request.getLoanId())
                .propertyAddress(request.getPropertyAddress())
                .appraiserName(request.getAppraiserName())
                .appraisalDate(LocalDate.now().plusDays(5)) // schedule for 5 days out
                .status("SCHEDULED")
                .build();
                
        Appraisal saved = appraisalRepository.save(appraisal);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/loan/{loanId}")
    public ResponseEntity<List<Appraisal>> getAppraisalsByLoan(@PathVariable String loanId) {
        log.info("Fetching appraisals for loanId={}", loanId);
        return ResponseEntity.ok(appraisalRepository.findByLoanId(loanId));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<Appraisal> completeAppraisal(
            @PathVariable String id,
            @RequestParam BigDecimal appraisedValue) {
        log.info("Completing appraisalId={} with appraisedValue={}", id, appraisedValue);
        
        Appraisal appraisal = appraisalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Appraisal not found: " + id));
                
        appraisal.setAppraisedValue(appraisedValue);
        appraisal.setStatus("COMPLETED");
        
        Appraisal updated = appraisalRepository.save(appraisal);
        return ResponseEntity.ok(updated);
    }

    @Data
    public static class AppraisalRequest {
        private String loanId;
        private String propertyAddress;
        private String appraiserName;
    }
}
