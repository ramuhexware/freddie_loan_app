package com.freddieapp.strutsportal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Struts-layer model for Audit Log entries.
 * Maps to FREDDIE_LOANS.AUDIT_LOG in DB2.
 * Written by AuditInterceptor on every Struts action.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    private Long          auditId;
    private String        actionNamespace;   // e.g. /loan
    private String        actionName;        // e.g. list, save, approve
    private String        actionMethod;
    private String        sessionUser;
    private String        remoteIpAddress;
    private String        requestParameters; // JSON string of params
    private String        resultCode;        // SUCCESS | ERROR | INPUT
    private Long          executionTimeMs;
    private String        errorMessage;
    private LocalDateTime timestamp;
}
