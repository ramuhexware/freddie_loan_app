package com.freddieapp.report;

import com.freddieapp.report.client.LoanServiceClient;
import com.freddieapp.report.client.UnderwritingServiceClient;
import com.freddieapp.report.dto.ClientDtos.CustomPageResponse;
import com.freddieapp.report.dto.ClientDtos.LoanResponse;
import com.freddieapp.report.dto.ClientDtos.UnderwritingResponse;
import com.freddieapp.report.entity.LoanReport;
import com.freddieapp.report.service.ReportGeneratorService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ReportServiceApplication.class, properties = {
    "eureka.client.enabled=false"
})
public class ReportGeneratorServiceTest {

    @Autowired
    private ReportGeneratorService reportGeneratorService;

    @MockBean
    private LoanServiceClient loanServiceClient;

    @MockBean
    private UnderwritingServiceClient underwritingServiceClient;

    @Test
    public void testGenerateReportCalculation() {
        // Mock Loan responses
        LoanResponse loan1 = LoanResponse.builder()
                .loanId("L-001")
                .customerId("C-001")
                .loanAmount(BigDecimal.valueOf(250000))
                .loanStatus("APPROVED")
                .build();

        LoanResponse loan2 = LoanResponse.builder()
                .loanId("L-002")
                .customerId("C-002")
                .loanAmount(BigDecimal.valueOf(350000))
                .loanStatus("REJECTED")
                .build();

        LoanResponse loan3 = LoanResponse.builder()
                .loanId("L-003")
                .customerId("C-003")
                .loanAmount(BigDecimal.valueOf(150000))
                .loanStatus("FUNDED")
                .build();

        CustomPageResponse<LoanResponse> loanPage = new CustomPageResponse<>(
                Arrays.asList(loan1, loan2, loan3), 0, 10, 3, 1, true, true
        );

        Mockito.when(loanServiceClient.getAllLoans(0, 1000)).thenReturn(loanPage);

        // Mock Underwriting responses
        UnderwritingResponse uw1 = UnderwritingResponse.builder()
                .loanId("L-001")
                .customerId("C-001")
                .dtiRatio(BigDecimal.valueOf(32.5))
                .ltvRatio(BigDecimal.valueOf(80.0))
                .riskLevel("LOW")
                .decision("APPROVED")
                .build();

        UnderwritingResponse uw2 = UnderwritingResponse.builder()
                .loanId("L-002")
                .customerId("C-002")
                .dtiRatio(BigDecimal.valueOf(48.2))
                .ltvRatio(BigDecimal.valueOf(95.0))
                .riskLevel("HIGH")
                .decision("DECLINED")
                .build();

        UnderwritingResponse uw3 = UnderwritingResponse.builder()
                .loanId("L-003")
                .customerId("C-003")
                .dtiRatio(BigDecimal.valueOf(28.0))
                .ltvRatio(BigDecimal.valueOf(75.0))
                .riskLevel("LOW")
                .decision("APPROVED")
                .build();

        CustomPageResponse<UnderwritingResponse> uwPage = new CustomPageResponse<>(
                Arrays.asList(uw1, uw2, uw3), 0, 10, 3, 1, true, true
        );

        Mockito.when(underwritingServiceClient.getAllAssessments(0, 1000)).thenReturn(uwPage);

        // Act
        LoanReport report = reportGeneratorService.generateLoanReport("SUMMARY");

        // Assert
        assertNotNull(report);
        assertEquals("SUMMARY", report.getReportType());
        assertEquals(3, report.getTotalLoansProcessed());
        assertEquals(2, report.getApprovedLoansCount()); // L-001 (APPROVED), L-003 (FUNDED)
        assertEquals(1, report.getRejectedLoansCount()); // L-002 (REJECTED)

        // Math checking
        // DTI avg: (32.5 + 48.2 + 28.0) / 3 = 108.7 / 3 = 36.233333333333334
        // LTV avg: (80.0 + 95.0 + 75.0) / 3 = 250.0 / 3 = 83.33333333333333
        assertEquals(36.233, report.getAverageDti(), 0.01);
        assertEquals(83.333, report.getAverageLtv(), 0.01);

        // Risk distribution checking
        String riskJson = report.getRiskDistributionJson();
        assertNotNull(riskJson);
        assertTrue(riskJson.contains("\"LOW\":2"));
        assertTrue(riskJson.contains("\"HIGH\":1"));
    }
}
