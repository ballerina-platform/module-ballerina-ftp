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

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.ftp.exception.BallerinaFtpException;

import java.util.HashSet;
import java.util.Set;

/**
 * Configuration for circuit breaker behavior.
 * Parsed from Ballerina CircuitBreakerConfig record.
 */
public class CircuitBreakerConfig {
    // Rolling window configuration
    private final int requestVolumeThreshold;
    private final long timeWindowMillis;
    private final long bucketSizeMillis;
    private final int numberOfBuckets;

    // Circuit breaker thresholds
    private final float failureThreshold;
    private final long resetTimeMillis;

    // Failure categories
    private final Set<FailureCategory> failureCategories;

    // Ballerina field names
    private static final BString ROLLING_WINDOW = StringUtils.fromString("rollingWindow");
    private static final BString REQUEST_VOLUME_THRESHOLD = StringUtils.fromString("requestVolumeThreshold");
    private static final BString TIME_WINDOW = StringUtils.fromString("timeWindow");
    private static final BString BUCKET_SIZE = StringUtils.fromString("bucketSize");
    private static final BString FAILURE_THRESHOLD = StringUtils.fromString("failureThreshold");
    private static final BString RESET_TIME = StringUtils.fromString("resetTime");
    private static final BString FAILURE_CATEGORIES = StringUtils.fromString("failureCategories");

    private CircuitBreakerConfig(int requestVolumeThreshold, long timeWindowMillis, long bucketSizeMillis,
                                  float failureThreshold, long resetTimeMillis,
                                  Set<FailureCategory> failureCategories) {
        this.requestVolumeThreshold = requestVolumeThreshold;
        this.timeWindowMillis = timeWindowMillis;
        this.bucketSizeMillis = bucketSizeMillis;
        this.numberOfBuckets = (int) (timeWindowMillis / bucketSizeMillis);
        this.failureThreshold = failureThreshold;
        this.resetTimeMillis = resetTimeMillis;
        this.failureCategories = failureCategories;
    }

    /**
     * Creates a CircuitBreakerConfig from a Ballerina BMap.
     *
     * @param config The Ballerina CircuitBreakerConfig record
     * @return A new CircuitBreakerConfig instance
     * @throws BallerinaFtpException if the configuration is invalid
     */
    @SuppressWarnings("unchecked")
    public static CircuitBreakerConfig fromBMap(BMap<BString, Object> config) throws BallerinaFtpException {
        // Extract rolling window configuration
        BMap<BString, Object> rollingWindow = (BMap<BString, Object>) config.getMapValue(ROLLING_WINDOW);
        int requestVolumeThreshold = rollingWindow.getIntValue(REQUEST_VOLUME_THRESHOLD).intValue();
        long timeWindowMillis = decimalToMillis(rollingWindow.get(TIME_WINDOW));
        long bucketSizeMillis = decimalToMillis(rollingWindow.get(BUCKET_SIZE));

        // Extract thresholds
        float failureThreshold = ((Number) config.get(FAILURE_THRESHOLD)).floatValue();
        long resetTimeMillis = decimalToMillis(config.get(RESET_TIME));

        // Extract failure categories
        BArray categoriesArray = config.getArrayValue(FAILURE_CATEGORIES);
        Set<FailureCategory> failureCategories = new HashSet<>();
        for (int i = 0; i < categoriesArray.size(); i++) {
            String categoryStr = categoriesArray.getBString(i).getValue();
            FailureCategory category = FailureCategory.fromString(categoryStr);
            if (category != null) {
                failureCategories.add(category);
            }
        }

        // Validate configuration
        validate(failureThreshold, timeWindowMillis, bucketSizeMillis, requestVolumeThreshold, resetTimeMillis);

        return new CircuitBreakerConfig(requestVolumeThreshold, timeWindowMillis, bucketSizeMillis,
                failureThreshold, resetTimeMillis, failureCategories);
    }

    private static long decimalToMillis(Object value) {
        if (value instanceof BDecimal) {
            return (long) (((BDecimal) value).floatValue() * 1000);
        }
        return (long) (((Number) value).doubleValue() * 1000);
    }

    private static void validate(float failureThreshold, long timeWindowMillis, long bucketSizeMillis,
                                 int requestVolumeThreshold, long resetTimeMillis)
            throws BallerinaFtpException {
        if (failureThreshold < 0.0f || failureThreshold > 1.0f) {
            throw new BallerinaFtpException("Circuit breaker failureThreshold must be between 0.0 and 1.0");
        }
        if (requestVolumeThreshold <= 0) {
            throw new BallerinaFtpException("Circuit breaker requestVolumeThreshold must be greater than 0");
        }
        if (resetTimeMillis <= 0) {
            throw new BallerinaFtpException("Circuit breaker resetTime must be greater than 0");
        }
        if (bucketSizeMillis <= 0) {
            throw new BallerinaFtpException("Circuit breaker bucketSize must be greater than 0");
        }
        if (timeWindowMillis < bucketSizeMillis) {
            throw new BallerinaFtpException("Circuit breaker timeWindow must be greater than or equal to bucketSize");
        }
        if (timeWindowMillis % bucketSizeMillis != 0) {
            throw new BallerinaFtpException("Circuit breaker timeWindow must be evenly divisible by bucketSize");
        }
    }

    public int getRequestVolumeThreshold() {
        return requestVolumeThreshold;
    }

    public long getTimeWindowMillis() {
        return timeWindowMillis;
    }

    public long getBucketSizeMillis() {
        return bucketSizeMillis;
    }

    public int getNumberOfBuckets() {
        return numberOfBuckets;
    }

    public float getFailureThreshold() {
        return failureThreshold;
    }

    public long getResetTimeMillis() {
        return resetTimeMillis;
    }

    public Set<FailureCategory> getFailureCategories() {
        return failureCategories;
    }
}
