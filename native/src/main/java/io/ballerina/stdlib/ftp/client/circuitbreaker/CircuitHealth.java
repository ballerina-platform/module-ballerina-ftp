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

import java.time.Instant;

/**
 * Tracks the health metrics of the circuit breaker using a sliding window of buckets.
 * <p>
 * This class is NOT thread-safe. All methods must be called while holding the
 * write lock from the {@link CircuitBreaker}'s {@link java.util.concurrent.locks.StampedLock}.
 * </p>
 */
public class CircuitHealth {
    private final Bucket[] buckets;
    private final int numberOfBuckets;
    private final long bucketSizeMillis;
    private final long timeWindowMillis;

    private boolean lastRequestSuccess;
    private int lastUsedBucketId;
    private final Instant startTime;
    private Instant lastRequestTime;
    private Instant lastErrorTime;

    /**
     * Creates a new CircuitHealth with the specified configuration.
     *
     * @param numberOfBuckets  Number of buckets in the rolling window
     * @param bucketSizeMillis Size of each bucket in milliseconds
     * @param timeWindowMillis Total time window in milliseconds
     */
    public CircuitHealth(int numberOfBuckets, long bucketSizeMillis, long timeWindowMillis) {
        this.numberOfBuckets = numberOfBuckets;
        this.bucketSizeMillis = bucketSizeMillis;
        this.timeWindowMillis = timeWindowMillis;
        this.buckets = new Bucket[numberOfBuckets];
        for (int i = 0; i < numberOfBuckets; i++) {
            buckets[i] = new Bucket();
        }
        this.startTime = Instant.now();
        this.lastRequestTime = Instant.now();
        this.lastRequestSuccess = true;
        this.lastUsedBucketId = 0;
    }

    /**
     * Gets the current bucket ID based on elapsed time.
     *
     * @return The current bucket index
     */
    public int getCurrentBucketId() {
        long elapsedMillis = Instant.now().toEpochMilli() - startTime.toEpochMilli();
        long windowElapsed = elapsedMillis % timeWindowMillis;
        return (int) ((windowElapsed / bucketSizeMillis) % numberOfBuckets);
    }

    /**
     * Prepares the rolling window by resetting stale buckets.
     */
    public void prepareRollingWindow() {
        Instant currentTime = Instant.now();
        long idleTimeMillis = currentTime.toEpochMilli() - lastRequestTime.toEpochMilli();

        // If idle longer than entire window, reset all buckets
        if (idleTimeMillis > timeWindowMillis) {
            resetAllBuckets();
            return;
        }

        int currentBucketId = getCurrentBucketId();

        // Reset buckets that have become stale
        if (currentBucketId == lastUsedBucketId && idleTimeMillis > timeWindowMillis) {
            // Same bucket but idle exceeded the full time window - reset all
            resetAllBuckets();
        } else if (currentBucketId < lastUsedBucketId) {
            // Wrapped around - reset from 0 to current and from lastUsed+1 to end
            for (int i = 0; i <= currentBucketId; i++) {
                buckets[i].reset();
            }
            for (int i = lastUsedBucketId + 1; i < numberOfBuckets; i++) {
                buckets[i].reset();
            }
        } else if (currentBucketId > lastUsedBucketId) {
            // Reset buckets between last used and current
            for (int i = lastUsedBucketId + 1; i <= currentBucketId; i++) {
                buckets[i].reset();
            }
        }
    }

    /**
     * Resets all buckets to their initial state.
     */
    public void resetAllBuckets() {
        for (Bucket bucket : buckets) {
            bucket.reset();
        }
    }

    /**
     * Gets the total request count across all buckets.
     *
     * @return Total number of requests
     */
    public int getTotalRequestCount() {
        int total = 0;
        for (Bucket bucket : buckets) {
            total += bucket.getTotalCount();
        }
        return total;
    }

    /**
     * Gets the total failure count across all buckets.
     *
     * @return Total number of failures
     */
    public int getTotalFailureCount() {
        int total = 0;
        for (Bucket bucket : buckets) {
            total += bucket.getFailureCount();
        }
        return total;
    }

    /**
     * Calculates the current failure ratio across all buckets.
     *
     * @return Failure ratio between 0.0 and 1.0
     */
    public float getFailureRatio() {
        int totalCount = getTotalRequestCount();
        if (totalCount == 0) {
            return 0.0f;
        }
        return (float) getTotalFailureCount() / totalCount;
    }

    /**
     * Records a request in the current bucket.
     */
    public void recordRequest() {
        int bucketId = getCurrentBucketId();
        buckets[bucketId].incrementTotalCount();
        lastUsedBucketId = bucketId;
        lastRequestTime = Instant.now();
    }

    /**
     * Records a successful request.
     */
    public void recordSuccess() {
        lastRequestSuccess = true;
    }

    /**
     * Records a failed request in the current bucket.
     */
    public void recordFailure() {
        int bucketId = getCurrentBucketId();
        buckets[bucketId].incrementFailureCount();
        lastRequestSuccess = false;
        lastErrorTime = Instant.now();
    }

    public boolean isLastRequestSuccess() {
        return lastRequestSuccess;
    }

    public Instant getLastErrorTime() {
        return lastErrorTime;
    }

    public void setLastUsedBucketId(int bucketId) {
        this.lastUsedBucketId = bucketId;
    }
}
