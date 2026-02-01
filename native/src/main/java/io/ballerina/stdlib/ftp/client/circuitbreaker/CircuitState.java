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
 * Represents the possible states of a circuit breaker.
 *
 * <p>State transitions:
 * <ul>
 *   <li>CLOSED -> OPEN: When failure threshold is exceeded</li>
 *   <li>OPEN -> HALF_OPEN: After reset time has elapsed</li>
 *   <li>HALF_OPEN -> CLOSED: When trial request succeeds</li>
 *   <li>HALF_OPEN -> OPEN: When trial request fails</li>
 * </ul>
 */
public enum CircuitState {
    /**
     * Circuit is closed - normal operation, all requests pass through.
     * Failures are tracked and counted towards the threshold.
     */
    CLOSED,

    /**
     * Circuit is open - requests are blocked immediately.
     * Returns CircuitBreakerOpenError without attempting the operation.
     */
    OPEN,

    /**
     * Circuit is in recovery mode - allows one trial request.
     * If the trial succeeds, transitions to CLOSED.
     * If the trial fails, transitions back to OPEN.
     */
    HALF_OPEN
}
