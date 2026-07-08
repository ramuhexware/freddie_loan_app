package com.freddieapp.strutsportal.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.freddieapp.strutsportal.config.ServiceConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP REST client for customer-service.
 *
 * Calls:
 *  - GET  /api/v1/customers              → list all customers
 *  - GET  /api/v1/customers/{customerId} → customer by ID
 *  - GET  /api/v1/customers/search?q=   → search customers
 */
@Slf4j
@Component
public class CustomerServiceClient {

    private final ServiceConfig serviceConfig;
    private final ObjectMapper  objectMapper;

    @Autowired
    public CustomerServiceClient(ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
        this.objectMapper  = new ObjectMapper()
                .registerModule(new JavaTimeModule());
    }

    // ------------------------------------------------------------------ //
    //  GET /api/v1/customers                                              //
    // ------------------------------------------------------------------ //
    public List<Map<String, Object>> getAllCustomers() {
        String url = serviceConfig.getCustomerServiceBaseUrl() + "/customers";
        log.info("[REST→customer-service] GET {}", url);
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            request.setHeader("Accept", "application/json");
            try (CloseableHttpResponse response = client.execute(request)) {
                String body = EntityUtils.toString(response.getEntity());
                return objectMapper.readValue(body, new TypeReference<>() {});
            }
        } catch (IOException e) {
            log.error("[REST→customer-service] Error fetching customers: {}", e.getMessage(), e);
            return List.of();
        }
    }

    // ------------------------------------------------------------------ //
    //  GET /api/v1/customers/{customerId}                                 //
    // ------------------------------------------------------------------ //
    public Map<String, Object> getCustomerById(String customerId) {
        String url = serviceConfig.getCustomerServiceBaseUrl() + "/customers/" + customerId;
        log.info("[REST→customer-service] GET {}", url);
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            request.setHeader("Accept", "application/json");
            try (CloseableHttpResponse response = client.execute(request)) {
                int    statusCode = response.getCode();
                String body       = EntityUtils.toString(response.getEntity());
                if (statusCode == 200) {
                    return objectMapper.readValue(body, new TypeReference<>() {});
                }
                log.warn("[REST→customer-service] GET customer {} returned HTTP {}", customerId, statusCode);
                return new HashMap<>();
            }
        } catch (IOException e) {
            log.error("[REST→customer-service] Error fetching customer {}: {}", customerId, e.getMessage(), e);
            return new HashMap<>();
        }
    }

    // ------------------------------------------------------------------ //
    //  GET /api/v1/customers/search?q={term}                             //
    // ------------------------------------------------------------------ //
    public List<Map<String, Object>> searchCustomers(String searchTerm) {
        String url = serviceConfig.getCustomerServiceBaseUrl()
                + "/customers/search?q=" + searchTerm;
        log.info("[REST→customer-service] GET {}", url);
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            request.setHeader("Accept", "application/json");
            try (CloseableHttpResponse response = client.execute(request)) {
                String body = EntityUtils.toString(response.getEntity());
                return objectMapper.readValue(body, new TypeReference<>() {});
            }
        } catch (IOException e) {
            log.error("[REST→customer-service] Error searching customers: {}", e.getMessage(), e);
            return List.of();
        }
    }
}
