package com.freddieapp.appian.controller;

import com.freddieapp.appian.dto.ClientDtos.*;
import com.freddieapp.appian.entity.AppianIntegrationLog;
import com.freddieapp.appian.service.AppianIntegrationService;
import com.freddieapp.appian.client.LoanServiceClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/appian")
@RequiredArgsConstructor
@Tag(name = "Appian Integration", description = "Endpoints exposed specifically for Appian BPM Platform Integration")
public class AppianController {

    private final AppianIntegrationService appianIntegrationService;
    private final LoanServiceClient loanServiceClient;

    @GetMapping("/loans")
    @Operation(summary = "Retrieve loan applications (paginated) for Appian record list grids")
    public ResponseEntity<CustomPageResponse<LoanResponse>> getLoansForAppian(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        log.info("Appian Request -> Retrieve paginated loans list. page={}, size={}", page, size);
        return ResponseEntity.ok(loanServiceClient.getAllLoans(page, size));
    }

    @GetMapping("/loans/{loanId}")
    @Operation(summary = "Get detailed loan application details for Appian Record Views")
    public ResponseEntity<LoanResponse> getLoanByIdForAppian(@PathVariable String loanId) {
        log.info("Appian Request -> Retrieve loan details for loanId={}", loanId);
        return ResponseEntity.ok(loanServiceClient.getLoanById(loanId));
    }

    @PostMapping("/loans")
    @Operation(summary = "Submit a new loan application from an Appian intake process or form")
    public ResponseEntity<LoanResponse> createLoanFromAppian(@RequestBody AppianLoanSubmitRequest request) {
        log.info("Appian Request -> Submitting new loan application for customerId={}", request.getCustomerId());
        LoanResponse response = appianIntegrationService.submitLoanFromAppian(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/loans/{loanId}/action")
    @Operation(summary = "Submit underwriting actions, manual overrides, or approvals from Appian tasks")
    public ResponseEntity<AppianActionResponse> executeActionFromAppian(
            @PathVariable String loanId,
            @RequestBody AppianLoanActionRequest request) {
        log.info("Appian Request -> Executing action={} on loanId={}", request.getAction(), loanId);
        AppianActionResponse response = appianIntegrationService.executeActionFromAppian(loanId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/integration-logs")
    @Operation(summary = "Fetch audit logs of Appian transactions (paginated)")
    public ResponseEntity<Page<AppianIntegrationLog>> getIntegrationLogs(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        log.info("Retrieving Appian integration logs. page={}, size={}", page, size);
        return ResponseEntity.ok(appianIntegrationService.getAllLogs(PageRequest.of(page, size)));
    }

    @GetMapping("/loans/{loanId}/integration-logs")
    @Operation(summary = "Fetch integration logs specifically for a single loan application")
    public ResponseEntity<List<AppianIntegrationLog>> getIntegrationLogsForLoan(@PathVariable String loanId) {
        log.info("Retrieving Appian integration logs for loanId={}", loanId);
        return ResponseEntity.ok(appianIntegrationService.getLogsByLoanId(loanId));
    }
}
