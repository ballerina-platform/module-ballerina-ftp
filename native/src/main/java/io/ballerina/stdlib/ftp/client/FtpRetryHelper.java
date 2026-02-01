/*
 * Copyright (c) 2025 WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package io.ballerina.stdlib.ftp.client;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.stdlib.ftp.util.FtpConstants;
import io.ballerina.stdlib.ftp.util.FtpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.function.Supplier;

import static io.ballerina.stdlib.ftp.util.FtpUtil.ErrorType.AllRetryAttemptsFailedError;
import static io.ballerina.stdlib.ftp.util.FtpUtil.ErrorType.Error;

/**
 * Helper class for FTP retry functionality.
 * Implements retry logic with exponential backoff directly in Java.
 */
public final class FtpRetryHelper {

    private static final Logger log = LoggerFactory.getLogger(FtpRetryHelper.class);
    private static final BigDecimal DEFAULT_MAX_WAIT_INTERVAL = BigDecimal.valueOf(60);
    private static final BigDecimal MILLIS_MULTIPLIER = BigDecimal.valueOf(1000);

    private FtpRetryHelper() {
        // private constructor
    }

    /**
     * Checks if retry is configured for the client.
     *
     * @param clientConnector The FTP client connector object
     * @return true if retry config exists, false otherwise
     */
    public static boolean isRetryConfigured(BObject clientConnector) {
        return clientConnector.getNativeData(FtpConstants.NATIVE_RETRY_CONFIG) != null;
    }

    /**
     * Gets the retry configuration from the client.
     *
     * @param clientConnector The FTP client connector object
     * @return The retry config BMap or null if not configured
     */
    public static BMap<?, ?> getRetryConfig(BObject clientConnector) {
        return (BMap<?, ?>) clientConnector.getNativeData(FtpConstants.NATIVE_RETRY_CONFIG);
    }

    /**
     * Executes an operation with retry logic using exponential backoff.
     * If the operation fails and retry is configured, it will retry up to the configured count.
     *
     * @param clientConnector The FTP client connector object
     * @param operation       The operation to execute (returns Object which may be result or BError)
     * @param operationName   The name of the operation for logging
     * @param filePath        The file path for logging
     * @return The operation result or error after exhausting retries
     */
    public static Object executeWithRetry(BObject clientConnector, Supplier<Object> operation,
                                          String operationName, String filePath) {
        // First attempt
        Object result = operation.get();

        // If success, return immediately
        if (!(result instanceof BError)) {
            return result;
        }

        // Check if retry is configured
        if (!isRetryConfigured(clientConnector)) {
            return result;
        }

        // Get retry configuration
        BMap<?, ?> retryConfig = getRetryConfig(clientConnector);
        long count = retryConfig.getIntValue(StringUtils.fromString(FtpConstants.RETRY_COUNT));
        Object intervalObject = retryConfig.get(StringUtils.fromString(FtpConstants.RETRY_INTERVAL));
        double intervalDecimal = ((BDecimal) intervalObject).floatValue();
        double inputBackOffFactor = retryConfig.getFloatValue(
                StringUtils.fromString(FtpConstants.RETRY_BACKOFF_FACTOR));
        Object inputMaxWaitIntervalObj = retryConfig.get(
                StringUtils.fromString(FtpConstants.RETRY_MAX_WAIT_INTERVAL));
        double inputMaxWaitInterval = ((BDecimal) inputMaxWaitIntervalObj).floatValue();

        // Validate and normalize configuration values
        BigDecimal interval = BigDecimal.valueOf(intervalDecimal);
        BigDecimal backOffFactor = inputBackOffFactor <= 0.0
                ? BigDecimal.ONE
                : BigDecimal.valueOf(inputBackOffFactor);
        BigDecimal maxWaitInterval = BigDecimal.valueOf(inputMaxWaitInterval).compareTo(BigDecimal.ZERO) == 0
                ? DEFAULT_MAX_WAIT_INTERVAL
                : BigDecimal.valueOf(inputMaxWaitInterval);

        BError lastError = (BError) result;
        BigDecimal currentInterval = interval;

        log.debug("Operation '{}' failed for path '{}', starting retry with count={}, interval={}, " +
                "backOffFactor={}, maxWaitInterval={}", operationName, filePath, count, interval,
                backOffFactor, maxWaitInterval);

        // Retry loop
        for (int attempt = 1; attempt <= count; attempt++) {
            // Calculate wait time - apply backoff only after first retry
            if (attempt > 1) {
                currentInterval = getWaitTime(backOffFactor, maxWaitInterval, currentInterval);
            }
            BigDecimal waitTime = currentInterval.min(maxWaitInterval);

            log.debug("FTP retry attempt {}/{} for operation '{}' on path '{}', waiting {}s",
                    attempt, count, operationName, filePath, waitTime);

            // Sleep before retry (convert seconds to milliseconds)
            try {
                long sleepMs = waitTime.multiply(MILLIS_MULTIPLIER).longValue();
                Thread.sleep(sleepMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return FtpUtil.createError("Retry interrupted for operation '" + operationName + "'",
                        e, Error.errorType());
            }

            // Execute the operation
            result = operation.get();

            if (!(result instanceof BError)) {
                log.debug("Operation '{}' succeeded on retry attempt {} for path '{}'",
                        operationName, attempt, filePath);
                return result;
            }

            lastError = (BError) result;
        }

        // All retries exhausted
        log.debug("Operation '{}' failed after {} retry attempts for path '{}'",
                operationName, count, filePath);
        return FtpUtil.createError("Operation '" + operationName + "' failed after " + count +
                " retry attempts: " + lastError.getMessage(), lastError, AllRetryAttemptsFailedError.errorType());
    }

    /**
     * Calculates the wait time with exponential backoff, capped at maxWaitTime.
     *
     * @param backOffFactor The multiplier for exponential backoff
     * @param maxWaitTime   The maximum wait time cap
     * @param interval      The current interval
     * @return The calculated wait time, capped at maxWaitTime
     */
    private static BigDecimal getWaitTime(BigDecimal backOffFactor, BigDecimal maxWaitTime, BigDecimal interval) {
        BigDecimal waitTime = interval.multiply(backOffFactor);
        return waitTime.compareTo(maxWaitTime) > 0 ? maxWaitTime : waitTime;
    }
}
