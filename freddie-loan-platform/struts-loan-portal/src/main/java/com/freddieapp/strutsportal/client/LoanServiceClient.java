package com.freddieapp.strutsportal.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.freddieapp.strutsportal.config.ServiceConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP REST client for loan-origination-service.
 *
 * Calls:
 *  - GET  /api/v1/loans                       → list all loans (paginated)
 *  - GET  /api/v1/loans/{loanId}              → get loan detail
 *  - GET  /api/v1/loans/customer/{customerId} → loans by customer
 *  - POST /api/v1/loans                       → submit new loan application
 *  - POST /api/v1/loans/{loanId}/submit-for-underwriting → send to underwriting
 */
@Slf4j
@Component
public class LoanServiceClient {

    private final ServiceConfig serviceConfig;
    private final ObjectMapper  objectMapper;

    @Autowired
    public LoanServiceClient(ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
        this.objectMapper  = new ObjectMapper()
                .registerModule(new JavaTimeModule());
    }

    // ------------------------------------------------------------------ //
    //  GET /api/v1/loans — Fetch all loan applications from microservice  //
    // ------------------------------------------------------------------ //
    @SuppressWarnings("unchecked")
    public Map<String, Object> getAllLoans(int page, int size) {
        String url = serviceConfig.getLoanServiceBaseUrl()
                + "/loans?page=" + page + "&size=" + size;
        log.info("[REST→loan-origination] GET {}", url);
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            request.setHeader("Accept", "application/json");
            try (CloseableHttpResponse response = client.execute(request)) {
                String body = EntityUtils.toString(response.getEntity());
                return objectMapper.readValue(body, new TypeReference<>() {});
            }
        } catch (IOException e) {
            log.error("[REST→loan-origination] Error fetching loans: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    // ------------------------------------------------------------------ //
    //  GET /api/v1/loans/{loanId}                                        //
    // ------------------------------------------------------------------ //
    public Map<String, Object> getLoanById(String loanId) {
        String url = serviceConfig.getLoanServiceBaseUrl() + "/loans/" + loanId;
        log.info("[REST→loan-origination] GET {}", url);
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            request.setHeader("Accept", "application/json");
            try (CloseableHttpResponse response = client.execute(request)) {
                String body = EntityUtils.toString(response.getEntity());
                return objectMapper.readValue(body, new TypeReference<>() {});
            }
        } catch (IOException e) {
            log.error("[REST→loan-origination] Error fetching loan {}: {}", loanId, e.getMessage(), e);
            return new HashMap<>();
        }
    }

    // ------------------------------------------------------------------ //
    //  GET /api/v1/loans/customer/{customerId}                           //
    // ------------------------------------------------------------------ //
    public List<Map<String, Object>> getLoansByCustomer(String customerId) {
        String url = serviceConfig.getLoanServiceBaseUrl()
                + "/loans/customer/" + customerId;
        log.info("[REST→loan-origination] GET {}", url);
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            request.setHeader("Accept", "application/json");
            try (CloseableHttpResponse response = client.execute(request)) {
                String body = EntityUtils.toString(response.getEntity());
                return objectMapper.readValue(body, new TypeReference<>() {});
            }
        } catch (IOException e) {
            log.error("[REST→loan-origination] Error fetching loans for customer {}: {}", customerId, e.getMessage(), e);
            return List.of();
        }
    }

    // ------------------------------------------------------------------ //
    //  POST /api/v1/loans — Submit a new loan application                 //
    // ------------------------------------------------------------------ //
    public Map<String, Object> submitLoanApplication(Map<String, Object> loanRequest) {
        String url = serviceConfig.getLoanServiceBaseUrl() + "/loans";
        log.info("[REST→loan-origination] POST {} payload={}", url, loanRequest);
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(url);
            request.setHeader("Accept", "application/json");
            String json = objectMapper.writeValueAsString(loanRequest);
            request.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            try (CloseableHttpResponse response = client.execute(request)) {
                int statusCode = response.getCode();
                String body    = EntityUtils.toString(response.getEntity());
                log.info("[REST→loan-origination] POST /loans responded HTTP {}", statusCode);
                if (statusCode == 201 || statusCode == 200) {
                    return objectMapper.readValue(body, new TypeReference<>() {});
                }
                log.warn("[REST→loan-origination] Non-success HTTP {}: {}", statusCode, body);
                return Map.of("error", "Service returned HTTP " + statusCode, "body", body);
            }
        } catch (IOException e) {
            log.error("[REST→loan-origination] Error submitting loan: {}", e.getMessage(), e);
            return Map.of("error", e.getMessage());
        }
    }

    // ------------------------------------------------------------------ //
    //  POST /api/v1/loans/{loanId}/submit-for-underwriting               //
    // ------------------------------------------------------------------ //
    public Map<String, Object> submitForUnderwriting(String loanId) {
        String url = serviceConfig.getLoanServiceBaseUrl()
                + "/loans/" + loanId + "/submit-for-underwriting";
        log.info("[REST→loan-origination] POST {}", url);
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(url);
            request.setHeader("Accept", "application/json");
            try (CloseableHttpResponse response = client.execute(request)) {
                int    statusCode = response.getCode();
                String body       = EntityUtils.toString(response.getEntity());
                log.info("[REST→loan-origination] submit-for-underwriting HTTP {}", statusCode);
                if (statusCode == 200) {
                    return objectMapper.readValue(body, new TypeReference<>() {});
                }
                return Map.of("error", "Service returned HTTP " + statusCode);
            }
        } catch (IOException e) {
            log.error("[REST→loan-origination] Error sending to underwriting: {}", e.getMessage(), e);
            return Map.of("error", e.getMessage());
        }
    }
}
