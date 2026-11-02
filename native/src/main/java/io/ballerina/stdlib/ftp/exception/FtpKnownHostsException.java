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
 * Exception thrown when the configured SFTP known_hosts file cannot be used for host key verification.
 */
public class FtpKnownHostsException extends RemoteFileSystemConnectorException implements ErrorTypeProvider {

    public FtpKnownHostsException(String message) {
        super(message);
    }

    public FtpKnownHostsException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String errorType() {
        return FtpUtil.ErrorType.InvalidConfigError.errorType();
    }
}
