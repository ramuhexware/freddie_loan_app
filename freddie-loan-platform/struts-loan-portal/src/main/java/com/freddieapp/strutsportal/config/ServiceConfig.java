package com.freddieapp.strutsportal.config;

import lombok.Data;

/**
 * Holds base URLs for all downstream microservices.
 * Values are injected from applicationContext.xml and can be overridden
 * via environment variables or Spring property placeholders.
 */
@Data
public class ServiceConfig {

    /** Base URL for loan-origination-service REST API */
    private String loanServiceBaseUrl;

    /** Base URL for customer-service REST API */
    private String customerServiceBaseUrl;

    /** Base URL for underwriting-service REST API */
    private String underwritingServiceBaseUrl;

    /** Base URL for report-service REST API */
    private String reportServiceBaseUrl;
}
