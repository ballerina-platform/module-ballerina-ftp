/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.ftp.client.circuitbreaker;

import io.ballerina.stdlib.ftp.exception.ErrorTypeProvider;
import io.ballerina.stdlib.ftp.util.FtpErrorCodeAnalyzer;
import io.ballerina.stdlib.ftp.util.FtpUtil;

import java.io.EOFException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Locale;

/**
 * Categorizes exceptions into failure categories for circuit breaker evaluation.
 * Uses a three-tier approach:
 * 1. Check if exception implements ErrorTypeProvider (structured error types)
 * 2. Check exception type in cause chain
 * 3. Fall back to message-based categorization with FtpErrorCodeAnalyzer
 */
public final class FailureCategorizer {

    private FailureCategorizer() {
        // Utility class
    }

    /**
     * Categorizes an exception into a FailureCategory.
     * First tries ErrorTypeProvider, then exception types, then message patterns.
     *
     * @param throwable The exception to categorize
     * @return The FailureCategory, or null if the exception doesn't match any specific category
     */
    public static FailureCategory categorize(Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        // First, check if any exception in the chain implements ErrorTypeProvider
        Throwable current = throwable;
        while (current != null) {
            FailureCategory category = categorizeByErrorTypeProvider(current);
            if (category != null) {
                return category;
            }

            Throwable next = current.getCause();
            if (next == current) {
                break;
            }
            current = next;
        }

        // Second, try to categorize by walking the cause chain and checking exception types
        current = throwable;
        while (current != null) {
            FailureCategory category = categorizeByExceptionType(current);
            if (category != null) {
                return category;
            }

            // Also check message at each level using FtpErrorCodeAnalyzer
            category = categorizeByMessage(current.getMessage());
            if (category != null) {
                return category;
            }

            Throwable next = current.getCause();
            if (next == current) {
                break;
            }
            current = next;
        }

        return null;
    }

    private static FailureCategory categorizeByErrorTypeProvider(Throwable throwable) {
        if (!(throwable instanceof ErrorTypeProvider provider)) {
            return null;
        }

        String errorType = provider.errorType();
        if (errorType == null) {
            return null;
        }

        // Map error types to failure categories
        if (errorType.equals(FtpUtil.ErrorType.ConnectionError.errorType())) {
            return FailureCategory.CONNECTION_ERROR;
        }
        if (errorType.equals(FtpUtil.ErrorType.ServiceUnavailableError.errorType())) {
            return FailureCategory.TRANSIENT_ERROR;
        }

        return null;
    }

    private static FailureCategory categorizeByExceptionType(Throwable throwable) {
        // Connection errors
        if (throwable instanceof ConnectException ||
                throwable instanceof SocketTimeoutException ||
                throwable instanceof UnknownHostException ||
                throwable instanceof NoRouteToHostException) {
            return FailureCategory.CONNECTION_ERROR;
        }

        // Server disconnect (EOF indicates unexpected connection close) - now TRANSIENT_ERROR
        if (throwable instanceof EOFException) {
            return FailureCategory.TRANSIENT_ERROR;
        }

        // SocketException could be either connection or transient
        if (throwable instanceof SocketException) {
            String message = throwable.getMessage();
            if (message != null) {
                String lower = message.toLowerCase(Locale.ROOT);
                if (lower.contains("connection reset") || lower.contains("broken pipe")) {
                    return FailureCategory.TRANSIENT_ERROR;
                }
            }
            return FailureCategory.CONNECTION_ERROR;
        }

        return null;
    }

    private static FailureCategory categorizeByMessage(String message) {
        if (message == null) {
            return null;
        }

        // Use FtpErrorCodeAnalyzer for FTP response codes
        if (FtpErrorCodeAnalyzer.isServiceUnavailable(message)) {
            return FailureCategory.TRANSIENT_ERROR;
        }

        if (FtpErrorCodeAnalyzer.isAuthenticationError(message)) {
            return FailureCategory.AUTHENTICATION_ERROR;
        }

        String lower = message.toLowerCase(Locale.ROOT);

        // Connection errors - network/connectivity issues
        if (lower.contains("connection refused") ||
                lower.contains("connection timed out") ||
                lower.contains("connect timed out") ||
                lower.contains("network is unreachable") ||
                lower.contains("no route to host") ||
                lower.contains("host is unreachable") ||
                lower.contains("unknown host") ||
                lower.contains("connect failed") ||
                lower.contains("failed to connect") ||
                lower.contains("could not connect")) {
            return FailureCategory.CONNECTION_ERROR;
        }

        // Additional authentication error patterns (beyond FTP codes)
        if (lower.contains("authentication failed") ||
                lower.contains("invalid credentials") ||
                lower.contains("auth fail") ||
                lower.contains("login failed") ||
                lower.contains("access denied") ||
                lower.contains("not authorized") ||
                lower.contains("login incorrect")) {
            return FailureCategory.AUTHENTICATION_ERROR;
        }

        // Transient errors - connection lost during operation
        if (lower.contains("connection reset") ||
                lower.contains("broken pipe") ||
                lower.contains("connection closed") ||
                lower.contains("unexpected end") ||
                lower.contains("stream closed") ||
                lower.contains("socket closed") ||
                lower.contains("remote host closed")) {
            return FailureCategory.TRANSIENT_ERROR;
        }

        return null;
    }
}
