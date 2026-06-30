package com.freddieapp.customerservice.client;

import com.freddieapp.customerservice.exception.UcsApiException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CardSyncClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CardSyncClient.class);

    private final WebClient webClient;
    private final OAuthTokenCache oAuthTokenCache;

    // Retry configuration properties
    private final int apiRetriesMax = 3;
    private final Duration apiRetriesDelay = Duration.ofSeconds(2);

    /**
     * Fetch customer cards from card-service via reactive WebClient using the requested pattern.
     *
     * @param customerId the customer identifier (passed as object)
     */
    public void syncCustomerCards(UUID customerId) {
        String url = "http://localhost:8085/api/v1/cards/customer/" + customerId;
        Object object = customerId;
        String operation = "GET_CUSTOMER_CARDS";
        Class<String> responseType = String.class; // Receive payload as raw JSON string representation

        LOGGER.info("ORG API: OIM Sync Get service call started... for URL-" + url + " Primary Key-" + object + " Opreation-" + operation + " Thread name " + Thread.currentThread().getName());

        webClient.get()
                .uri(url)
                .headers(h -> h.add("Authorization", oAuthTokenCache.getOAuthAccessToken()))
                .exchangeToMono(clientResponse -> {
                    LOGGER.info("Recived response from OIM Sync application with status code " + clientResponse.statusCode().value());
                    if (clientResponse.statusCode().is4xxClientError()) {
                        throw new UcsApiException(HttpStatus.valueOf(clientResponse.statusCode().value()), "Org API :: URL is wrong ");
                    } else if (clientResponse.statusCode().is5xxServerError()) {
                        throw new UcsApiException(HttpStatus.valueOf(clientResponse.statusCode().value()), " Org API :: Error occured in OIM Sync :: for more details check OIM sync logs");
                    } else {
                        return clientResponse.bodyToMono(responseType);
                    }
                })
                .retryWhen(Retry.backoff(apiRetriesMax, apiRetriesDelay).jitter(0d).doAfterRetry(retrySignal -> {
                    LOGGER.info("Retried " + retrySignal.totalRetries());
                })
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> new UcsApiException(HttpStatus.valueOf(500),
                        " Retries exhausted ", retrySignal.failure())))
                .doOnSuccess(clientResponse -> {
                    LOGGER.info("Received reply from " + url + " Primary Key-" + object + " Opreation-" + operation + " Thread name " + Thread.currentThread().getName() + " with response " + clientResponse);
                })
                .doOnError(Throwable.class, (msg) -> {
                    saveToErrorTable(url, object.toString(), operation, msg);
                    LOGGER.error("Exception while calling get for " + url + " for entity " + object.toString() + " while " + operation + " Exception msg " + msg + " Thread name " + Thread.currentThread().getName());
                })
                .subscribe();
    }

    /**
     * Simulated method to log/save connection errors to database error table.
     */
    private void saveToErrorTable(String url, String objectKey, String operation, Throwable error) {
        LOGGER.warn("[DB-LOG] Logging service call failure to ERROR_LOG table: url={}, key={}, op={}, error={}",
                url, objectKey, operation, error.getMessage());
    }
}
