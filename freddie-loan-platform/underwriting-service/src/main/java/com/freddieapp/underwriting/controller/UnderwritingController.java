package com.freddieapp.underwriting.controller;

import com.freddieapp.underwriting.dto.UnderwritingRequest;
import com.freddieapp.underwriting.dto.UnderwritingResponse;
import com.freddieapp.underwriting.entity.UnderwritingAssessment.Decision;
import com.freddieapp.underwriting.repository.UnderwritingAssessmentRepository;
import com.freddieapp.underwriting.service.UnderwritingEngine;
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
@RequestMapping("/api/v1/underwriting")
@RequiredArgsConstructor
@Tag(name = "Underwriting & Risk", description = "Risk assessment engines and compliance evaluation APIs")
public class UnderwritingController {

    private final UnderwritingEngine underwritingEngine;
    private final UnderwritingAssessmentRepository assessmentRepository;

    @PostMapping("/assess")
    @Operation(summary = "Execute automated underwriting assessment rules")
    public ResponseEntity<UnderwritingResponse> assessLoan(@Valid @RequestBody UnderwritingRequest request) {
        log.info("REST request to evaluate underwriting rules for loanId={}", request.getLoanId());
        return ResponseEntity.status(HttpStatus.CREATED).body(underwritingEngine.assessLoan(request));
    }

    @PostMapping("/override/{assessmentId}")
    @PreAuthorize("hasRole('UNDERWRITER')")
    @Operation(summary = "Manual underwriting decision override (Audited)")
    public ResponseEntity<UnderwritingResponse> overrideDecision(
            @PathVariable String assessmentId,
            @RequestParam Decision decision,
            @RequestParam String reason,
            @RequestParam String underwriter) {
        log.info("REST request to manually override assessmentId={} to decision={}", assessmentId, decision);
        return ResponseEntity.ok(underwritingEngine.overrideDecision(assessmentId, decision, reason, underwriter));
    }

    @GetMapping("/loan/{loanId}")
    @Operation(summary = "Get latest risk assessment by loan ID")
    public ResponseEntity<UnderwritingResponse> getLatestAssessment(@PathVariable String loanId) {
        return assessmentRepository.findLatestByLoanId(loanId)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get all historical assessments for a customer")
    public ResponseEntity<Page<UnderwritingResponse>> getCustomerAssessments(
            @PathVariable String customerId, Pageable pageable) {
        Page<UnderwritingResponse> page = assessmentRepository.findByCustomerId(customerId, pageable)
                .map(this::toResponse);
        return ResponseEntity.ok(page);
    }

    @GetMapping
    @Operation(summary = "Get all underwriting assessments (paginated)")
    public ResponseEntity<Page<UnderwritingResponse>> getAllAssessments(Pageable pageable) {
        Page<UnderwritingResponse> page = assessmentRepository.findAll(pageable)
                .map(this::toResponse);
        return ResponseEntity.ok(page);
    }

    private UnderwritingResponse toResponse(com.freddieapp.underwriting.entity.UnderwritingAssessment assessment) {
        return UnderwritingResponse.builder()
                .assessmentId(assessment.getAssessmentId())
                .loanId(assessment.getLoanId())
                .customerId(assessment.getCustomerId())
                .creditScore(assessment.getCreditScore())
                .dtiRatio(assessment.getDtiRatio())
                .ltvRatio(assessment.getLtvRatio())
                .riskLevel(assessment.getRiskLevel())
                .decision(assessment.getDecision())
                .decisionReason(assessment.getDecisionReason())
                .assessedAt(assessment.getAssessedAt())
                .assessedBy(assessment.getAssessedBy())
                .bureauReference(assessment.getBureauReference())
                .build();
    }
}
