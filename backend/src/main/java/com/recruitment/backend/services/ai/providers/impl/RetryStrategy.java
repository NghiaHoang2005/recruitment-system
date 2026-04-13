package com.recruitment.backend.services.ai.providers.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@Slf4j
public class RetryStrategy {

    // Configuration
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_DELAY_MS = 2000;  // 2 seconds
    private static final long MAX_DELAY_MS = 16000;     // 16 seconds
    private static final double BACKOFF_MULTIPLIER = 2.0;

    public <T> T executeWithRetry(
            String operationName,
            Supplier<T> operation,
            Supplier<T> fallbackOperation
    ) {
        long delayMs = INITIAL_DELAY_MS;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                log.debug("Attempt {}/{}: {}", attempt, MAX_RETRIES, operationName);
                T result = operation.get();
                
                if (attempt > 1) {
                    log.info("{} succeeded after {} attempts", operationName, attempt);
                }
                
                return result;
            } catch (Exception e) {
                // Check if it's a server error (5xx)
                if (isServerError(e)) {
                    log.warn("Attempt {}/{} failed with server error: {}", attempt, MAX_RETRIES, e.getMessage());

                    if (attempt < MAX_RETRIES) {
                        // Wait before retrying
                        log.info("Waiting {}ms before retry...", delayMs);
                        sleep(delayMs);
                        
                        // Exponential backoff
                        delayMs = calculateNextDelay(delayMs);
                    } else {
                        // Last attempt failed, will use fallback
                        log.error("{} failed after {} attempts, switching to fallback", operationName, MAX_RETRIES);
                    }
                } else {
                    // Non-server error, don't retry
                    log.error("{} failed with non-recoverable error: {}", operationName, e.getMessage());
                    throw e;
                }
            }
        }

        // All retries exhausted, use fallback
        try {
            log.info("Using fallback operation for: {}", operationName);
            T fallbackResult = fallbackOperation.get();
            log.info("Fallback succeeded for: {}", operationName);
            return fallbackResult;
        } catch (Exception fallbackError) {
            log.error("Fallback also failed: {}", fallbackError.getMessage());
            throw new RuntimeException(
                String.format("Both primary and fallback operations failed for: %s", operationName),
                fallbackError
            );
        }
    }

    private boolean isServerError(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }

        // Check for HTTP 5xx error indicators
        return message.contains("500") ||
               message.contains("501") ||
               message.contains("502") ||
               message.contains("503") ||
               message.contains("504") ||
               message.contains("505") ||
               message.contains("506") ||
               message.contains("507") ||
               message.contains("508") ||
               message.contains("509") ||
               message.contains("510") ||
               message.contains("HttpServerErrorException") ||
               message.contains("Internal Server Error") ||
               message.contains("Bad Gateway") ||
               message.contains("Service Unavailable") ||
               message.contains("Gateway Timeout") ||
               message.contains("HttpClientErrorException.ServiceUnavailable");
    }

    private long calculateNextDelay(long currentDelay) {
        long nextDelay = (long)(currentDelay * BACKOFF_MULTIPLIER);
        return Math.min(nextDelay, MAX_DELAY_MS);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Retry delay interrupted: {}", e.getMessage());
        }
    }

    public String getRetryConfig() {
        return String.format(
            "RetryConfig(maxRetries=%d, initialDelay=%dms, backoff=%.1fx, maxDelay=%dms)",
            MAX_RETRIES, INITIAL_DELAY_MS, BACKOFF_MULTIPLIER, MAX_DELAY_MS
        );
    }
}
