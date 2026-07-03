package com.freddieapp.appian;

import com.freddieapp.appian.client.AppianApiClient;
import com.freddieapp.appian.client.LoanServiceClient;
import com.freddieapp.appian.client.UnderwritingServiceClient;
import com.freddieapp.appian.dto.ClientDtos.*;
import com.freddieapp.appian.entity.AppianIntegrationLog;
import com.freddieapp.appian.repository.AppianIntegrationLogRepository;
import com.freddieapp.appian.service.AppianIntegrationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AppianServiceApplication.class, properties = {
    "eureka.client.enabled=false",
    "spring.kafka.listener.auto-startup=false"
})
public class AppianIntegrationServiceTest {

    @Autowired
    private AppianIntegrationService appianIntegrationService;

    @Autowired
    private AppianIntegrationLogRepository logRepository;

    @MockBean
    private AppianApiClient appianApiClient;

    @MockBean
    private LoanServiceClient loanServiceClient;

    @MockBean
    private UnderwritingServiceClient underwritingServiceClient;

    @Test
    public void testSubmitLoanFromAppian() {
        // Arrange
        AppianLoanSubmitRequest request = AppianLoanSubmitRequest.builder()
                .customerId("CUST-100")
                .loanType("PURCHASE")
                .loanAmount(BigDecimal.valueOf(300000))
                .propertyValue(BigDecimal.valueOf(400000))
                .propertyAddress("123 Main St")
                .loanTermMonths(360)
                .build();

        LoanResponse expectedResponse = LoanResponse.builder()
                .loanId("LOAN-TEST-99")
                .customerId("CUST-100")
                .loanType("PURCHASE")
                .loanAmount(BigDecimal.valueOf(300000))
                .propertyValue(BigDecimal.valueOf(400000))
                .propertyAddress("123 Main St")
                .loanStatus("SUBMITTED")
                .build();

        Mockito.when(loanServiceClient.submitLoanApplication(Mockito.any(LoanApplicationRequest.class)))
                .thenReturn(expectedResponse);

        // Act
        LoanResponse actualResponse = appianIntegrationService.submitLoanFromAppian(request);

        // Assert
        assertNotNull(actualResponse);
        assertEquals("LOAN-TEST-99", actualResponse.getLoanId());
        assertEquals("SUBMITTED", actualResponse.getLoanStatus());

        // Verify log was saved to the H2 database
        List<AppianIntegrationLog> logs = logRepository.findByLoanIdOrderByTimestampDesc("LOAN-TEST-99");
        assertFalse(logs.isEmpty());
        AppianIntegrationLog auditLog = logs.get(0);
        assertEquals("INBOUND_LOAN_SUBMIT", auditLog.getIntegrationType());
        assertEquals("SUCCESS", auditLog.getStatus());
        assertEquals(201, auditLog.getStatusCode());
        assertTrue(auditLog.getRequestPayload().contains("CUST-100"));
        assertTrue(auditLog.getResponsePayload().contains("LOAN-TEST-99"));
    }

    @Test
    public void testProcessKafkaEventToAppian() {
        // Arrange
        String loanId = "LOAN-12345";
        String eventType = "SUBMITTED";

        Mockito.when(appianApiClient.startLoanApprovalProcess(Mockito.any(AppianWebhookEvent.class)))
                .thenReturn("PM-PROC-98765");

        // Act
        appianIntegrationService.processKafkaEventToAppian(loanId, eventType, null, "SUBMITTED");

        // Assert
        List<AppianIntegrationLog> logs = logRepository.findByLoanIdOrderByTimestampDesc(loanId);
        assertFalse(logs.isEmpty());
        AppianIntegrationLog auditLog = logs.get(0);
        assertEquals("OUTBOUND_START_PROCESS", auditLog.getIntegrationType());
        assertEquals("SUCCESS", auditLog.getStatus());
        assertEquals(200, auditLog.getStatusCode());
        assertEquals("PM-PROC-98765", auditLog.getResponsePayload());
    }
}
