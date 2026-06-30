package com.freddieapp.customerservice.exception;

import org.springframework.http.HttpStatus;

public class UcsApiException extends RuntimeException {
    
    private final HttpStatus status;

    public UcsApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public UcsApiException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
