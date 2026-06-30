package com.freddieapp.customerservice.client;

import org.springframework.stereotype.Component;

@Component
public class OAuthTokenCache {

    /**
     * Retrieve cached OAuth token for authorization headers.
     */
    public String getOAuthAccessToken() {
        return "Bearer freddie-internal-oauth-token-sample";
    }
}
