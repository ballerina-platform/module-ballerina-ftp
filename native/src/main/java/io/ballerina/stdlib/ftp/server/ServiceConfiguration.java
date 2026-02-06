/*
 * Copyright (c) 2026 WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.ftp.server;

import io.ballerina.stdlib.ftp.transport.server.FileDependencyCondition;

import java.util.List;

/**
 * Holds service-level configuration extracted from @ftp:ServiceConfig annotation.
 */
public class ServiceConfiguration {

    private final String path;
    private final String fileNamePattern;
    private final double minAge;
    private final double maxAge;
    private final String ageCalculationMode;
    private final List<FileDependencyCondition> dependencyConditions;

    private ServiceConfiguration(Builder builder) {
        this.path = builder.path;
        this.fileNamePattern = builder.fileNamePattern;
        this.minAge = builder.minAge;
        this.maxAge = builder.maxAge;
        this.ageCalculationMode = builder.ageCalculationMode;
        this.dependencyConditions = builder.dependencyConditions;
    }

    public String getPath() {
        return path;
    }

    public String getFileNamePattern() {
        return fileNamePattern;
    }

    public double getMinAge() {
        return minAge;
    }

    public double getMaxAge() {
        return maxAge;
    }

    public String getAgeCalculationMode() {
        return ageCalculationMode;
    }

    public List<FileDependencyCondition> getDependencyConditions() {
        return dependencyConditions;
    }

    /**
     * Builder for ServiceConfiguration.
     */
    public static class Builder {
        private String path;
        private String fileNamePattern;
        private double minAge = -1;
        private double maxAge = -1;
        private String ageCalculationMode = "LAST_MODIFIED";
        private List<FileDependencyCondition> dependencyConditions = List.of();

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder fileNamePattern(String pattern) {
            this.fileNamePattern = pattern;
            return this;
        }

        public Builder minAge(double minAge) {
            this.minAge = minAge;
            return this;
        }

        public Builder maxAge(double maxAge) {
            this.maxAge = maxAge;
            return this;
        }

        public Builder ageCalculationMode(String mode) {
            this.ageCalculationMode = mode;
            return this;
        }

        public Builder dependencyConditions(List<FileDependencyCondition> conditions) {
            this.dependencyConditions = conditions;
            return this;
        }

        public ServiceConfiguration build() {
            return new ServiceConfiguration(this);
        }
    }
}
