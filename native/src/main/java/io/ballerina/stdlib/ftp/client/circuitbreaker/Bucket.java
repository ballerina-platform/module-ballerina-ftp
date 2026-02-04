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

package io.ballerina.stdlib.ftp.client.circuitbreaker;

import java.time.Instant;

/**
 * Represents a discrete time segment in the rolling window.
 * Each bucket tracks the number of total requests and failures within its time period.
 */
public class Bucket {
    private int totalCount;
    private int failureCount;
    private Instant lastUpdatedTime;

    /**
     * Creates a new empty bucket.
     */
    public Bucket() {
        this.totalCount = 0;
        this.failureCount = 0;
        this.lastUpdatedTime = null;
    }

    /**
     * Increments the total request count.
     */
    public void incrementTotalCount() {
        this.totalCount++;
        this.lastUpdatedTime = Instant.now();
    }

    /**
     * Increments the failure count.
     */
    public void incrementFailureCount() {
        this.failureCount++;
        this.lastUpdatedTime = Instant.now();
    }

    /**
     * Resets the bucket to its initial state.
     */
    public void reset() {
        this.totalCount = 0;
        this.failureCount = 0;
        this.lastUpdatedTime = null;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public Instant getLastUpdatedTime() {
        return lastUpdatedTime;
    }
}
