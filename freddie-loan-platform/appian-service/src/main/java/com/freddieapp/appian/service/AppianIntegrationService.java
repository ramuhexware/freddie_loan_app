package com.freddieapp.appian.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freddieapp.appian.client.AppianApiClient;
import com.freddieapp.appian.client.LoanServiceClient;
import com.freddieapp.appian.client.UnderwritingServiceClient;
import com.freddieapp.appian.dto.ClientDtos.*;
import com.freddieapp.appian.entity.AppianIntegrationLog;
import com.freddieapp.appian.repository.AppianIntegrationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppianIntegrationService {

    private final AppianApiClient appianApiClient;
    private final AppianIntegrationLogRepository logRepository;
    private final LoanServiceClient loanServiceClient;
    private final UnderwritingServiceClient underwritingServiceClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Outbound: Handle Kafka event to notify Appian.
     */
    @Transactional
    public void processKafkaEventToAppian(String loanId, String eventType, String customerId, Object originalPayload) {
        log.info("Processing Kafka event to Appian: loanId={}, type={}", loanId, eventType);

        AppianWebhookEvent event = AppianWebhookEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .loanId(loanId)
                .status(eventType)
                .timestamp(OffsetDateTime.now())
                .additionalData(originalPayload)
                .build();

        String reqJson = serialize(event);
        AppianIntegrationLog.AppianIntegrationLogBuilder logBuilder = AppianIntegrationLog.builder()
                .loanId(loanId)
                .requestPayload(reqJson)
                .timestamp(OffsetDateTime.now());

        try {
            String responseStr;
            if ("SUBMITTED".equalsIgnoreCase(eventType)) {
                logBuilder.integrationType("OUTBOUND_START_PROCESS");
                responseStr = appianApiClient.startLoanApprovalProcess(event);
            } else {
                logBuilder.integrationType("OUTBOUND_STATUS_UPDATE");
                appianApiClient.sendStatusUpdateToAppian(event);
                responseStr = "Status updated successfully in Appian";
            }

            logRepository.save(logBuilder
                    .responsePayload(responseStr)
                    .status("SUCCESS")
                    .statusCode(200)
                    .build());

        } catch (Exception ex) {
            log.error("Outbound Appian Integration failed for loanId={}: {}", loanId, ex.getMessage());
            logRepository.save(logBuilder
                    .responsePayload(ex.getMessage())
                    .status("FAILED")
                    .statusCode(500)
                    .build());
        }
    }

    /**
     * Inbound: Appian submits a new loan application.
     */
    @Transactional
    public LoanResponse submitLoanFromAppian(AppianLoanSubmitRequest request) {
        log.info("Inbound Appian request -> Submitting loan application for customerId={}", request.getCustomerId());

        String reqJson = serialize(request);
        AppianIntegrationLog.AppianIntegrationLogBuilder logBuilder = AppianIntegrationLog.builder()
                .integrationType("INBOUND_LOAN_SUBMIT")
                .requestPayload(reqJson)
                .timestamp(OffsetDateTime.now());

        try {
            LoanApplicationRequest serviceReq = LoanApplicationRequest.builder()
                    .customerId(request.getCustomerId())
                    .loanType(request.getLoanType())
                    .loanAmount(request.getLoanAmount())
                    .propertyValue(request.getPropertyValue())
                    .propertyAddress(request.getPropertyAddress())
                    .loanTermMonths(request.getLoanTermMonths())
                    .build();

            LoanResponse response = loanServiceClient.submitLoanApplication(serviceReq);
            
            logRepository.save(logBuilder
                    .loanId(response.getLoanId())
                    .responsePayload(serialize(response))
                    .status("SUCCESS")
                    .statusCode(201)
                    .build());

            return response;
        } catch (Exception ex) {
            log.error("Inbound Appian loan submission failed: {}", ex.getMessage());
            logRepository.save(logBuilder
                    .responsePayload(ex.getMessage())
                    .status("FAILED")
                    .statusCode(500)
                    .build());
            throw new RuntimeException("Appian Integration Submit Loan Failure: " + ex.getMessage(), ex);
        }
    }

    /**
     * Inbound: Appian performs manual override decision.
     */
    @Transactional
    public AppianActionResponse executeActionFromAppian(String loanId, AppianLoanActionRequest request) {
        log.info("Inbound Appian request -> Executing action={} for loanId={}", request.getAction(), loanId);

        String reqJson = serialize(request);
        AppianIntegrationLog.AppianIntegrationLogBuilder logBuilder = AppianIntegrationLog.builder()
                .loanId(loanId)
                .integrationType("INBOUND_ACTION")
                .requestPayload(reqJson)
                .timestamp(OffsetDateTime.now());

        try {
            // 1. Fetch Underwriting Assessment to get assessmentId
            UnderwritingResponse assessment = underwritingServiceClient.getLatestAssessment(loanId);
            if (assessment == null || assessment.getAssessmentId() == null) {
                throw new IllegalStateException("No active underwriting assessment found for loanId: " + loanId);
            }

            // 2. Perform override decision using Feign client
            underwritingServiceClient.overrideDecision(
                    assessment.getAssessmentId(),
                    request.getAction(), // "APPROVED" or "DECLINED"
                    request.getRemarks(),
                    request.getProcessedBy() != null ? request.getProcessedBy() : "APPIAN_USER"
            );

            // 3. Mark the loan status update in LoanOriginationService if approved/rejected.
            // Underwriting service overrides trigger kafka events which sync status, but let's return success.
            AppianActionResponse response = AppianActionResponse.builder()
                    .loanId(loanId)
                    .status(request.getAction())
                    .message("Underwriting override submitted successfully via Appian")
                    .timestamp(OffsetDateTime.now())
                    .build();

            logRepository.save(logBuilder
                    .responsePayload(serialize(response))
                    .status("SUCCESS")
                    .statusCode(200)
                    .build());

            return response;
        } catch (Exception ex) {
            log.error("Inbound Appian Action failed for loanId={}: {}", loanId, ex.getMessage());
            logRepository.save(logBuilder
                    .responsePayload(ex.getMessage())
                    .status("FAILED")
                    .statusCode(500)
                    .build());
            throw new RuntimeException("Appian Integration Action Failure: " + ex.getMessage(), ex);
        }
    }

    public List<AppianIntegrationLog> getLogsByLoanId(String loanId) {
        return logRepository.findByLoanIdOrderByTimestampDesc(loanId);
    }

    public Page<AppianIntegrationLog> getAllLogs(Pageable pageable) {
        return logRepository.findAll(pageable);
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception ex) {
            return "{\"error\":\"Serialization failed\"}";
        }
    }
}
