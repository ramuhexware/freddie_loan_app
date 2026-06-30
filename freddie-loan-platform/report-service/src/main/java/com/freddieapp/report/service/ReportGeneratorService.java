package com.freddieapp.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freddieapp.report.client.LoanServiceClient;
import com.freddieapp.report.client.UnderwritingServiceClient;
import com.freddieapp.report.dto.ClientDtos.CustomPageResponse;
import com.freddieapp.report.dto.ClientDtos.LoanResponse;
import com.freddieapp.report.dto.ClientDtos.UnderwritingResponse;
import com.freddieapp.report.entity.LoanReport;
import com.freddieapp.report.repository.LoanReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGeneratorService {

    private final LoanServiceClient loanServiceClient;
    private final UnderwritingServiceClient underwritingServiceClient;
    private final LoanReportRepository reportRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public LoanReport generateLoanReport(String reportType) {
        log.info("Generating report of type={}", reportType);

        // Fetch up to 1000 loans and assessments
        List<LoanResponse> loans = new ArrayList<>();
        try {
            CustomPageResponse<LoanResponse> loanPage = loanServiceClient.getAllLoans(0, 1000);
            if (loanPage != null && loanPage.getContent() != null) {
                loans.addAll(loanPage.getContent());
            }
        } catch (Exception e) {
            log.error("Failed to fetch loans from loan-origination-service", e);
        }

        List<UnderwritingResponse> assessments = new ArrayList<>();
        try {
            CustomPageResponse<UnderwritingResponse> uwPage = underwritingServiceClient.getAllAssessments(0, 1000);
            if (uwPage != null && uwPage.getContent() != null) {
                assessments.addAll(uwPage.getContent());
            }
        } catch (Exception e) {
            log.error("Failed to fetch underwriting assessments from underwriting-service", e);
        }

        // Map assessments by loanId for fast lookup
        Map<String, UnderwritingResponse> assessmentMap = new HashMap<>();
        for (UnderwritingResponse uw : assessments) {
            if (uw.getLoanId() != null) {
                assessmentMap.put(uw.getLoanId(), uw);
            }
        }

        int totalLoans = loans.size();
        int approvedCount = 0;
        int rejectedCount = 0;

        double dtiSum = 0;
        int dtiCount = 0;
        double ltvSum = 0;
        int ltvCount = 0;

        Map<String, Integer> riskDistribution = new HashMap<>();
        riskDistribution.put("LOW", 0);
        riskDistribution.put("MEDIUM", 0);
        riskDistribution.put("HIGH", 0);

        for (LoanResponse loan : loans) {
            String status = loan.getLoanStatus();
            if (status != null) {
                if (status.equalsIgnoreCase("APPROVED") || status.equalsIgnoreCase("FUNDED")) {
                    approvedCount++;
                } else if (status.equalsIgnoreCase("REJECTED") || status.equalsIgnoreCase("DECLINED")) {
                    rejectedCount++;
                }
            }

            // Look up corresponding underwriting assessment
            UnderwritingResponse uw = assessmentMap.get(loan.getLoanId());
            if (uw != null) {
                if (uw.getDtiRatio() != null) {
                    dtiSum += uw.getDtiRatio().doubleValue();
                    dtiCount++;
                }
                if (uw.getLtvRatio() != null) {
                    ltvSum += uw.getLtvRatio().doubleValue();
                    ltvCount++;
                }
                String risk = uw.getRiskLevel();
                if (risk != null) {
                    String normRisk = risk.toUpperCase();
                    riskDistribution.put(normRisk, riskDistribution.getOrDefault(normRisk, 0) + 1);
                }
            }
        }

        double avgDti = dtiCount > 0 ? (dtiSum / dtiCount) : 0.0;
        double avgLtv = ltvCount > 0 ? (ltvSum / ltvCount) : 0.0;

        String riskJson = "{}";
        try {
            riskJson = objectMapper.writeValueAsString(riskDistribution);
        } catch (Exception e) {
            log.error("Failed to serialize risk distribution map", e);
        }

        LoanReport report = LoanReport.builder()
                .reportId(UUID.randomUUID().toString())
                .reportType(reportType != null ? reportType.toUpperCase() : "SUMMARY")
                .generatedAt(OffsetDateTime.now())
                .totalLoansProcessed(totalLoans)
                .approvedLoansCount(approvedCount)
                .rejectedLoansCount(rejectedCount)
                .averageDti(avgDti)
                .averageLtv(avgLtv)
                .riskDistributionJson(riskJson)
                .build();

        LoanReport savedReport = reportRepository.save(report);
        log.info("Report saved successfully with reportId={}", savedReport.getReportId());
        return savedReport;
    }

    public List<LoanReport> getAllReports() {
        return reportRepository.findAll();
    }

    public Optional<LoanReport> getLatestReport() {
        return reportRepository.findFirstByOrderByGeneratedAtDesc();
    }
}
