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

package io.ballerina.stdlib.ftp.client;

import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.stdlib.ftp.util.FtpConstants;
import io.ballerina.stdlib.ftp.util.FtpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

import static io.ballerina.stdlib.ftp.util.FtpUtil.ErrorType.AllRetryAttemptsFailedError;
import static io.ballerina.stdlib.ftp.util.FtpUtil.ErrorType.Error;

/**
 * Helper class for FTP retry functionality.
 * Implements retry logic with exponential backoff directly in Java.
 */
public final class FtpRetryHelper {

    private static final Logger log = LoggerFactory.getLogger(FtpRetryHelper.class);

    private FtpRetryHelper() {
        // private constructor
    }

    protected static Object executeWithRetry(BObject clientConnector, Supplier<Object> operation,
                                          String operationName, String filePath) {
        // First attempt
        Object result = operation.get();

        // If success, return immediately
        if (!(result instanceof BError)) {
            return result;
        }

        // Check if retry is configured
        boolean isRetryEnabled = (boolean) clientConnector.getNativeData(FtpConstants.NATIVE_RETRY_ENABLED);
        if (!isRetryEnabled) {
            return result;
        }

        // Get retry configuration
        long count = (long) clientConnector.getNativeData(FtpConstants.NATIVE_RETRY_COUNT);
        double interval = (double) clientConnector.getNativeData(FtpConstants.NATIVE_RETRY_INTERVAL);
        double backOffFactor = (double) clientConnector.getNativeData(FtpConstants.NATIVE_RETRY_BACKOFF);
        double maxWaitInterval = (double) clientConnector.getNativeData(FtpConstants.NATIVE_RETRY_MAX_WAIT);

        BError lastError = (BError) result;
        double currentInterval = interval;

        log.debug("Operation '{}' failed for path '{}', starting retry with count={}, interval={}, " +
                "backOffFactor={}, maxWaitInterval={}", operationName, filePath, count, interval,
                backOffFactor, maxWaitInterval);

        // Retry loop
        for (int attempt = 1; attempt <= count; attempt++) {
            // Calculate wait time - apply backoff only after first retry
            if (attempt > 1) {
                currentInterval = getWaitTime(backOffFactor, maxWaitInterval, currentInterval);
            }

            log.debug("FTP retry attempt {}/{} for operation '{}' on path '{}', waiting {}s",
                    attempt, count, operationName, filePath, currentInterval);

            // Sleep before retry (convert seconds to milliseconds)
            try {
                long sleepMs = (long) (currentInterval * 1000);
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

    private static double getWaitTime(double backOffFactor, double maxWaitTime, double interval) {
        double waitTime = interval * backOffFactor;
        return Math.min(waitTime, maxWaitTime);
    }
}
