/*
 * Copyright (c) 2026 WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.ftp.client.circuitbreaker;

import io.ballerina.runtime.api.values.BError;
import io.ballerina.stdlib.ftp.util.FtpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Set;

/**
 * Circuit breaker implementation for FTP client operations.
 * Prevents cascade failures by temporarily blocking requests when the server is experiencing issues.
 */
public class CircuitBreaker {
    private static final Logger log = LoggerFactory.getLogger(CircuitBreaker.class);

    private final CircuitBreakerConfig config;
    private final CircuitHealth health;
    private volatile CircuitState state;
    private boolean trialRequestInProgress;
    private final Object lock = new Object();

    /**
     * Creates a new CircuitBreaker with the specified configuration.
     *
     * @param config The circuit breaker configuration
     */
    public CircuitBreaker(CircuitBreakerConfig config) {
        this.config = config;
        this.health = new CircuitHealth(
                config.getNumberOfBuckets(),
                config.getBucketSizeMillis(),
                config.getTimeWindowMillis()
        );
        this.state = CircuitState.CLOSED;
        this.trialRequestInProgress = false;
        log.debug("Circuit breaker initialized with {} buckets, {}ms window, {}% failure threshold",
                config.getNumberOfBuckets(), config.getTimeWindowMillis(),
                config.getFailureThreshold() * 100);
    }

    /**
     * Checks if the circuit is currently open (blocking requests).
     *
     * @return true if the circuit is open
     */
    public boolean isOpen() {
        synchronized (lock) {
            updateState();
            if (state == CircuitState.HALF_OPEN) {
                return trialRequestInProgress;
            }
            return state == CircuitState.OPEN;
        }
    }

    /**
     * Records that a request is about to be made.
     * Call this BEFORE starting the operation.
     */
    public void recordRequestStart() {
        synchronized (lock) {
            updateState();
            if (state == CircuitState.OPEN || (state == CircuitState.HALF_OPEN && trialRequestInProgress)) {
                return;
            }
            if (state == CircuitState.HALF_OPEN) {
                trialRequestInProgress = true;
            }
            health.prepareRollingWindow();
            health.recordRequest();
        }
    }

    /**
     * Records the outcome of an operation.
     * Call this AFTER the operation completes.
     *
     * @param error The throwable if the operation failed, null if successful
     */
    public void recordOutcome(Throwable error) {
        synchronized (lock) {
            if (state == CircuitState.HALF_OPEN) {
                if (error == null) {
                    recordSuccess();
                    state = CircuitState.CLOSED;
                    health.resetAllBuckets();
                    log.info("Circuit breaker transitioning from HALF_OPEN to CLOSED (trial succeeded)");
                } else if (shouldCountAsFailure(error)) {
                    recordFailure();
                    log.debug("Circuit breaker recorded failure: {}", error.getMessage());
                    state = CircuitState.OPEN;
                    log.info("Circuit breaker transitioning from HALF_OPEN to OPEN (trial failed)");
                } else {
                    recordSuccess();
                    state = CircuitState.CLOSED;
                    health.resetAllBuckets();
                    log.info("Circuit breaker transitioning from HALF_OPEN to CLOSED (trial succeeded)");
                }
                trialRequestInProgress = false;
                return;
            }

            if (error == null) {
                recordSuccess();
            } else if (shouldCountAsFailure(error)) {
                recordFailure();
                log.debug("Circuit breaker recorded failure: {}", error.getMessage());
            }
            // Update state after recording the outcome
            updateState();
        }
    }

    /**
     * Creates an error to throw when the circuit is open.
     *
     * @return A BError representing the service unavailable state
     */
    public BError createServiceUnavailableError() {
        Instant lastError = health.getLastErrorTime();
        long remainingMillis = 0;
        if (lastError != null) {
            long elapsedMillis = Instant.now().toEpochMilli() - lastError.toEpochMilli();
            remainingMillis = Math.max(0, config.getResetTimeMillis() - elapsedMillis);
        }

        String message = String.format(
                "FTP server unavailable. Circuit breaker is OPEN. Retry in %.0f seconds.",
                remainingMillis / 1000.0
        );

        return FtpUtil.createError(message, FtpUtil.ErrorType.CircuitBreakerOpenError.errorType());
    }

    /**
     * Updates the circuit state based on current metrics.
     * Must be called within synchronized block.
     */
    private void updateState() {
        health.prepareRollingWindow();

        switch (state) {
            case OPEN:
                if (resetTimeElapsed()) {
                    state = CircuitState.HALF_OPEN;
                    trialRequestInProgress = false;
                    log.info("Circuit breaker transitioning from OPEN to HALF_OPEN");
                }
                break;

            case HALF_OPEN:
                // HALF_OPEN transition is evaluated after the trial request completes.
                break;

            case CLOSED:
                if (health.getTotalRequestCount() >= config.getRequestVolumeThreshold()) {
                    float failureRatio = health.getFailureRatio();
                    if (failureRatio > config.getFailureThreshold()) {
                        state = CircuitState.OPEN;
                        log.info("Circuit breaker transitioning from CLOSED to OPEN " +
                                "(failure ratio {} > threshold {})",
                                failureRatio, config.getFailureThreshold());
                    }
                }
                break;
        }
    }

    /**
     * Checks if the reset time has elapsed since the circuit opened.
     *
     * @return true if reset time has elapsed
     */
    private boolean resetTimeElapsed() {
        Instant lastError = health.getLastErrorTime();
        if (lastError == null) {
            return true;
        }
        long elapsedMillis = Instant.now().toEpochMilli() - lastError.toEpochMilli();
        return elapsedMillis >= config.getResetTimeMillis();
    }

    /**
     * Records a successful operation.
     * Must be called within synchronized block.
     */
    private void recordSuccess() {
        health.recordSuccess();
    }

    /**
     * Records a failed operation.
     * Must be called within synchronized block.
     */
    private void recordFailure() {
        health.recordFailure();
    }

    /**
     * Determines if an error should count as a failure for circuit breaker purposes.
     *
     * @param e The throwable to evaluate
     * @return true if the error should count as a failure
     */
    private boolean shouldCountAsFailure(Throwable e) {
        Set<FailureCategory> configuredCategories = config.getFailureCategories();

        // If ALL_ERRORS is configured, any exception trips the circuit
        if (configuredCategories.contains(FailureCategory.ALL_ERRORS)) {
            return true;
        }

        // Categorize the exception
        FailureCategory category = FailureCategorizer.categorize(e);
        if (category == null) {
            // Exception doesn't match any specific category
            return false;
        }

        // Check if the category is in the configured list
        return configuredCategories.contains(category);
    }

    /**
     * Gets the current circuit state.
     *
     * @return The current state
     */
    public CircuitState getState() {
        synchronized (lock) {
            return state;
        }
    }
}
