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

/**
 * Categories of errors that can trip the circuit breaker.
 * Maps to the Ballerina FailureCategory enum.
 */
public enum FailureCategory {
    /**
     * Connection-level failures (timeout, refused, reset, DNS resolution).
     * Maps to ConnectionError type.
     */
    CONNECTION_ERROR,

    /**
     * Authentication failures (invalid credentials, key rejected).
     * Detected via FTP 530 response code.
     */
    AUTHENTICATION_ERROR,

    /**
     * Transient server errors that may succeed on retry.
     * Maps to ServiceUnavailableError type (FTP codes 421, 425, 426, 450, 451, 452).
     */
    TRANSIENT_ERROR,

    /**
     * All errors regardless of type. When configured, any error will trip the circuit.
     */
    ALL_ERRORS;

    /**
     * Converts a Ballerina enum string value to the corresponding Java enum.
     *
     * @param value The string value from Ballerina (e.g., "CONNECTION_ERROR")
     * @return The corresponding FailureCategory, or null if not found
     */
    public static FailureCategory fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return FailureCategory.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
