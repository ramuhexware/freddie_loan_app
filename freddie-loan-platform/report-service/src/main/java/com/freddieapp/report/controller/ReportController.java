package com.freddieapp.report.controller;

import com.freddieapp.report.entity.LoanReport;
import com.freddieapp.report.service.ReportGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportGeneratorService reportGeneratorService;

    @PostMapping("/generate")
    public ResponseEntity<LoanReport> generateReport(@RequestParam(value = "type", defaultValue = "SUMMARY") String type) {
        log.info("REST request to generate loan report of type={}", type);
        LoanReport report = reportGeneratorService.generateLoanReport(type);
        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }

    @GetMapping
    public ResponseEntity<List<LoanReport>> getAllReports() {
        log.info("REST request to list all generated reports");
        return ResponseEntity.ok(reportGeneratorService.getAllReports());
    }

    @GetMapping("/latest")
    public ResponseEntity<LoanReport> getLatestReport() {
        log.info("REST request to get the latest generated report");
        return reportGeneratorService.getLatestReport()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
