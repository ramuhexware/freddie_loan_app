package com.freddieapp.appian.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "appian_integration_logs")
public class AppianIntegrationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "loan_id")
    private String loanId;

    @Column(name = "integration_type", nullable = false)
    private String integrationType; // e.g. "OUTBOUND_START_PROCESS", "INBOUND_LOAN_SUBMIT", "INBOUND_ACTION"

    @Lob
    @Column(name = "request_payload", length = 5000)
    private String requestPayload;

    @Lob
    @Column(name = "response_payload", length = 5000)
    private String responsePayload;

    @Column(name = "status", nullable = false)
    private String status; // "SUCCESS", "FAILED"

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "log_timestamp", nullable = false)
    private OffsetDateTime timestamp;
}
