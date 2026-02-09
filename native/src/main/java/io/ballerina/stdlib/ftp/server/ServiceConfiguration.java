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

    /**
     * Constructs a ServiceConfiguration using values from the given Builder.
     *
     * @param builder the Builder containing configuration values to initialize this instance
     */
    private ServiceConfiguration(Builder builder) {
        this.path = builder.path;
        this.fileNamePattern = builder.fileNamePattern;
        this.minAge = builder.minAge;
        this.maxAge = builder.maxAge;
        this.ageCalculationMode = builder.ageCalculationMode;
        this.dependencyConditions = builder.dependencyConditions;
    }

    /**
     * Gets the configured filesystem path for the FTP service.
     *
     * @return the configured path for the service
     */
    public String getPath() {
        return path;
    }

    /**
     * The configured file name pattern used to match incoming files for the service.
     *
     * @return the configured file name pattern, or {@code null} if not set
     */
    public String getFileNamePattern() {
        return fileNamePattern;
    }

    /**
     * Minimum file age configured for the service.
     *
     * @return the minimum file age as configured, or -1 if unset
     */
    public double getMinAge() {
        return minAge;
    }

    /**
     * Maximum permissible age for files matched by the service configuration, expressed in seconds.
     *
     * @return the maximum file age in seconds; -1 if not specified
     */
    public double getMaxAge() {
        return maxAge;
    }

    /**
     * Gets the mode used to calculate a file's age for the service.
     *
     * @return the age calculation mode (e.g., "LAST_MODIFIED")
     */
    public String getAgeCalculationMode() {
        return ageCalculationMode;
    }

    /**
     * The file dependency conditions configured for this service.
     *
     * @return the list of configured FileDependencyCondition objects, or an empty list if none
     */
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

        /**
         * Sets the directory path for the FTP service.
         *
         * @param path the directory path that the service will use
         * @return this Builder instance
         */
        public Builder path(String path) {
            this.path = path;
            return this;
        }

        /**
         * Sets the pattern used to match file names for the service.
         *
         * @param pattern the file name matching pattern
         * @return this Builder instance
         */
        public Builder fileNamePattern(String pattern) {
            this.fileNamePattern = pattern;
            return this;
        }

        /**
         * Sets the minimum file age used to include files when evaluating service conditions.
         *
         * Values less than 0 disable the minimum age check.
         *
         * @param minAge the minimum age (in seconds) for a file to be considered; use a value less than 0 to disable
         * @return this Builder instance
         */
        public Builder minAge(double minAge) {
            this.minAge = minAge;
            return this;
        }

        /**
         * Set the maximum file age used to filter which files the service processes.
         *
         * @param maxAge the maximum age value for files to be considered by the service
         * @return the Builder instance
         */
        public Builder maxAge(double maxAge) {
            this.maxAge = maxAge;
            return this;
        }

        /**
         * Set the age calculation mode used when evaluating file ages.
         *
         * @param mode the age calculation mode identifier (for example, "LAST_MODIFIED")
         * @return this Builder instance
         */
        public Builder ageCalculationMode(String mode) {
            this.ageCalculationMode = mode;
            return this;
        }

        /**
         * Sets the list of file dependency conditions to apply to the service configuration.
         *
         * @param conditions the file dependency conditions to attach to the service configuration
         * @return this Builder instance
         */
        public Builder dependencyConditions(List<FileDependencyCondition> conditions) {
            this.dependencyConditions = conditions;
            return this;
        }

        /**
         * Build a ServiceConfiguration from the current Builder state.
         *
         * @return a new ServiceConfiguration populated with the builder's values
         */
        public ServiceConfiguration build() {
            return new ServiceConfiguration(this);
        }
    }
}