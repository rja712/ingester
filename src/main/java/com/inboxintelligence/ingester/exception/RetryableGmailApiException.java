package com.inboxintelligence.ingester.exception;

/**
 * Thrown for transient Gmail API errors that should be retried by resilience4j.
 */
public class RetryableGmailApiException extends RuntimeException {

    public RetryableGmailApiException(String message) {
        super(message);
    }

    public RetryableGmailApiException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
