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

package io.ballerina.stdlib.ftp.exception;

import io.ballerina.stdlib.ftp.util.FtpUtil;

/**
 * Exception thrown when all retry attempts have been exhausted.
 * This exception wraps the last failure encountered during retry attempts.
 */
public class FtpAllRetryAttemptsFailedException extends RemoteFileSystemConnectorException
        implements ErrorTypeProvider {

    private final int attemptCount;

    public FtpAllRetryAttemptsFailedException(String message) {
        super(message);
        this.attemptCount = 0;
    }

    public FtpAllRetryAttemptsFailedException(String message, int attemptCount) {
        super(message);
        this.attemptCount = attemptCount;
    }

    public FtpAllRetryAttemptsFailedException(String message, Throwable cause) {
        super(message, cause);
        this.attemptCount = 0;
    }

    public FtpAllRetryAttemptsFailedException(String message, int attemptCount, Throwable cause) {
        super(message, cause);
        this.attemptCount = attemptCount;
    }

    /**
     * Returns the number of retry attempts made before this exception was thrown.
     *
     * @return the number of retry attempts
     */
    public int getAttemptCount() {
        return attemptCount;
    }

    @Override
    public String errorType() {
        return FtpUtil.ErrorType.AllRetryAttemptsFailedError.errorType();
    }
}
