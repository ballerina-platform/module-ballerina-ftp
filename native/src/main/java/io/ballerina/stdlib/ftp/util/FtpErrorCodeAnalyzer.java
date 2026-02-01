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

package io.ballerina.stdlib.ftp.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for analyzing FTP error messages and extracting FTP response codes.
 * This enables message-based error detection when the underlying VFS library
 * doesn't expose FTP codes directly.
 *
 * <p>FTP Response Code Categories:
 * <ul>
 *   <li>1xx - Positive Preliminary reply</li>
 *   <li>2xx - Positive Completion reply</li>
 *   <li>3xx - Positive Intermediate reply</li>
 *   <li>4xx - Transient Negative Completion reply (temporary failure, retry may succeed)</li>
 *   <li>5xx - Permanent Negative Completion reply (permanent failure)</li>
 * </ul>
 *
 * <p>Key codes for retry/circuit-breaker patterns:
 * <ul>
 *   <li>421 - Service not available, closing control connection</li>
 *   <li>425 - Cannot open data connection</li>
 *   <li>426 - Connection closed, transfer aborted</li>
 *   <li>450 - Requested file action not taken (temporary)</li>
 *   <li>451 - Requested action aborted: local error in processing</li>
 *   <li>452 - Requested action not taken: insufficient storage space</li>
 *   <li>530 - Not logged in (authentication failure)</li>
 *   <li>550 - Requested action not taken: file unavailable/not found/permission denied</li>
 * </ul>
 */
public final class FtpErrorCodeAnalyzer {

    // Pattern to match FTP response codes (3-digit numbers typically at start of message or after specific text)
    // Matches patterns like "421 Service not available", "response code: 550", etc.
    private static final Pattern FTP_CODE_PATTERN = Pattern.compile("\\b([1-5]\\d{2})\\b");

    // Service unavailable codes (transient - suitable for retry)
    public static final int SERVICE_NOT_AVAILABLE = 421;
    public static final int CANNOT_OPEN_DATA_CONNECTION = 425;
    public static final int CONNECTION_CLOSED = 426;
    public static final int FILE_ACTION_NOT_TAKEN = 450;
    public static final int ACTION_ABORTED_LOCAL_ERROR = 451;
    public static final int INSUFFICIENT_STORAGE = 452;

    // Permanent failure codes
    public static final int NOT_LOGGED_IN = 530;
    public static final int FILE_UNAVAILABLE = 550;
    public static final int PAGE_TYPE_UNKNOWN = 551;
    public static final int EXCEEDED_STORAGE = 552;
    public static final int FILENAME_NOT_ALLOWED = 553;

    private FtpErrorCodeAnalyzer() {
        // Utility class
    }

    /**
     * Extracts the first FTP response code found in an error message.
     *
     * @param errorMessage the error message to analyze
     * @return Optional containing the FTP code if found, empty otherwise
     */
    public static Optional<Integer> extractFtpCode(String errorMessage) {
        if (errorMessage == null || errorMessage.isEmpty()) {
            return Optional.empty();
        }

        Matcher matcher = FTP_CODE_PATTERN.matcher(errorMessage);
        while (matcher.find()) {
            int code = Integer.parseInt(matcher.group(1));
            // Validate it's a reasonable FTP code (100-599)
            if (code >= 100 && code <= 599) {
                return Optional.of(code);
            }
        }
        return Optional.empty();
    }

    /**
     * Checks if the error indicates a service unavailable condition (suitable for retry).
     * This includes codes 421, 425, 426, 450, 451, 452.
     *
     * @param errorMessage the error message to analyze
     * @return true if the error indicates service unavailable
     */
    public static boolean isServiceUnavailable(String errorMessage) {
        return extractFtpCode(errorMessage)
                .map(FtpErrorCodeAnalyzer::isServiceUnavailableCode)
                .orElse(false);
    }

    /**
     * Checks if an FTP code indicates a service unavailable condition.
     *
     * @param code the FTP response code
     * @return true if the code indicates service unavailable
     */
    public static boolean isServiceUnavailableCode(int code) {
        return code == SERVICE_NOT_AVAILABLE ||
                code == CANNOT_OPEN_DATA_CONNECTION ||
                code == CONNECTION_CLOSED ||
                code == FILE_ACTION_NOT_TAKEN ||
                code == ACTION_ABORTED_LOCAL_ERROR ||
                code == INSUFFICIENT_STORAGE;
    }

    /**
     * Checks if the error indicates a transient failure (4xx codes).
     * Transient failures may succeed on retry.
     *
     * @param errorMessage the error message to analyze
     * @return true if the error is transient
     */
    public static boolean isTransientError(String errorMessage) {
        return extractFtpCode(errorMessage)
                .map(code -> code >= 400 && code < 500)
                .orElse(false);
    }

    /**
     * Checks if the error indicates a permanent failure (5xx codes).
     * Permanent failures will not succeed on retry.
     *
     * @param errorMessage the error message to analyze
     * @return true if the error is permanent
     */
    public static boolean isPermanentError(String errorMessage) {
        return extractFtpCode(errorMessage)
                .map(code -> code >= 500 && code < 600)
                .orElse(false);
    }

    /**
     * Checks if the error indicates an authentication failure (code 530).
     *
     * @param errorMessage the error message to analyze
     * @return true if the error indicates authentication failure
     */
    public static boolean isAuthenticationError(String errorMessage) {
        return extractFtpCode(errorMessage)
                .map(code -> code == NOT_LOGGED_IN)
                .orElse(false);
    }

    /**
     * Checks if the error indicates a connection-related issue.
     * This includes authentication failures and service unavailable conditions.
     *
     * @param errorMessage the error message to analyze
     * @return true if the error is connection-related
     */
    public static boolean isConnectionError(String errorMessage) {
        return extractFtpCode(errorMessage)
                .map(code -> code == NOT_LOGGED_IN ||
                        code == SERVICE_NOT_AVAILABLE ||
                        code == CANNOT_OPEN_DATA_CONNECTION ||
                        code == CONNECTION_CLOSED)
                .orElse(false);
    }

    /**
     * Determines the recommended error type based on the FTP code in the message.
     *
     * @param errorMessage the error message to analyze
     * @return the recommended FtpUtil.ErrorType, or Error if no specific type matches
     */
    public static FtpUtil.ErrorType getRecommendedErrorType(String errorMessage) {
        Optional<Integer> codeOpt = extractFtpCode(errorMessage);
        if (codeOpt.isEmpty()) {
            return FtpUtil.ErrorType.Error;
        }

        int code = codeOpt.get();

        // Service unavailable - for retry/circuit breaker
        if (isServiceUnavailableCode(code)) {
            return FtpUtil.ErrorType.ServiceUnavailableError;
        }

        // Connection-related errors (authentication failure)
        if (code == NOT_LOGGED_IN) {
            return FtpUtil.ErrorType.ConnectionError;
        }

        // File not found (550 is ambiguous but commonly means file not found)
        if (code == FILE_UNAVAILABLE) {
            // Note: 550 can mean either "file not found" or "permission denied"
            // We default to FileNotFoundError; specific context may override this
            return FtpUtil.ErrorType.FileNotFoundError;
        }

        // Other permanent errors
        if (code >= 500 && code < 600) {
            return FtpUtil.ErrorType.Error;
        }

        return FtpUtil.ErrorType.Error;
    }

    /**
     * Analyzes an exception's full message chain for FTP codes.
     * This traverses the cause chain to find FTP codes that may be buried
     * in nested exceptions.
     *
     * @param throwable the exception to analyze
     * @return Optional containing the first FTP code found in the exception chain
     */
    public static Optional<Integer> extractFtpCodeFromException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            Optional<Integer> code = extractFtpCode(current.getMessage());
            if (code.isPresent()) {
                return code;
            }
            current = current.getCause();
        }
        return Optional.empty();
    }

    /**
     * Collects the full error message from an exception chain.
     *
     * @param throwable the exception to analyze
     * @return the concatenated error messages from the exception chain
     */
    public static String getFullErrorMessage(Throwable throwable) {
        StringBuilder message = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null) {
                if (message.length() > 0) {
                    message.append(" Caused by: ");
                }
                message.append(current.getMessage());
            }
            current = current.getCause();
        }
        return message.toString();
    }
}
