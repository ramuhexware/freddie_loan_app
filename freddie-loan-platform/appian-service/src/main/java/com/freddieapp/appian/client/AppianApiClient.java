package com.freddieapp.appian.client;

import com.freddieapp.appian.dto.ClientDtos.AppianWebhookEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.UUID;

@Slf4j
@Component
public class AppianApiClient {

    private final RestTemplate restTemplate;

    @Value("${appian.api.url}")
    private String appianApiUrl;

    @Value("${appian.api.username}")
    private String username;

    @Value("${appian.api.password}")
    private String password;

    @Value("${appian.api.simulation-mode:true}")
    private boolean simulationMode;

    public AppianApiClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Start a process model in Appian for a specific loan application.
     */
    public String startLoanApprovalProcess(AppianWebhookEvent event) {
        log.info("Appian Client -> Initiating loan approval process flow for loanId={} (Simulation={})",
                event.getLoanId(), simulationMode);

        if (simulationMode) {
            log.info("Appian Client -> [SIMULATION] Started process successfully. AppianProcessInstanceId=PM-PROC-{}",
                    UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            return "SIM-PROC-" + UUID.randomUUID().toString();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth(username, password);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity<AppianWebhookEvent> requestEntity = new HttpEntity<>(event, headers);
            String startProcessUrl = appianApiUrl + "/process/start-loan-flow";

            ResponseEntity<String> response = restTemplate.exchange(
                    startProcessUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Appian Client -> Process started successfully in Appian: {}", response.getBody());
                return response.getBody();
            } else {
                log.error("Appian Client -> Error starting process in Appian. Status code: {}", response.getStatusCode());
                throw new RuntimeException("Appian process start failed. Status: " + response.getStatusCode());
            }
        } catch (Exception ex) {
            log.error("Appian Client -> Failed to trigger Appian Process endpoint: {}", ex.getMessage(), ex);
            throw new RuntimeException("Appian Integration Failure: " + ex.getMessage(), ex);
        }
    }

    /**
     * Send status update event to Appian to advance/complete a task.
     */
    public void sendStatusUpdateToAppian(AppianWebhookEvent event) {
        log.info("Appian Client -> Sending loan lifecycle update for loanId={}, status={} to Appian (Simulation={})",
                event.getLoanId(), event.getStatus(), simulationMode);

        if (simulationMode) {
            log.info("Appian Client -> [SIMULATION] Status update completed successfully.");
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth(username, password);

            HttpEntity<AppianWebhookEvent> requestEntity = new HttpEntity<>(event, headers);
            String updateUrl = appianApiUrl + "/task/update-status";

            ResponseEntity<String> response = restTemplate.exchange(
                    updateUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Appian Client -> Error updating status in Appian. Status code: {}", response.getStatusCode());
                throw new RuntimeException("Appian task update failed. Status: " + response.getStatusCode());
            }
        } catch (Exception ex) {
            log.error("Appian Client -> Failed to send update to Appian endpoint: {}", ex.getMessage(), ex);
            throw new RuntimeException("Appian Integration Failure: " + ex.getMessage(), ex);
        }
    }
}
