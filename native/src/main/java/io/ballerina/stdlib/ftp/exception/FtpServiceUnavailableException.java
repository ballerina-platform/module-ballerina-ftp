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
 * Exception thrown when the FTP/SFTP service is temporarily unavailable.
 * This is a transient error indicating the operation may succeed on retry.
 * 
 * <p>Common FTP codes that trigger this exception:
 * <ul>
 *   <li>421 - Service not available, closing control connection</li>
 *   <li>425 - Cannot open data connection</li>
 *   <li>426 - Connection closed, transfer aborted</li>
 *   <li>450 - Requested file action not taken (temporary)</li>
 *   <li>451 - Requested action aborted: local error in processing</li>
 *   <li>452 - Requested action not taken: insufficient storage space</li>
 * </ul>
 *
 */
public class FtpServiceUnavailableException extends RemoteFileSystemConnectorException
        implements ErrorTypeProvider {

    private final int ftpCode;

    public FtpServiceUnavailableException(String message) {
        super(message);
        this.ftpCode = 0;
    }

    public FtpServiceUnavailableException(String message, int ftpCode) {
        super(message);
        this.ftpCode = ftpCode;
    }

    public FtpServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
        this.ftpCode = 0;
    }

    public FtpServiceUnavailableException(String message, int ftpCode, Throwable cause) {
        super(message, cause);
        this.ftpCode = ftpCode;
    }

    /**
     * Returns the FTP response code associated with this exception.
     *
     * @return the FTP code, or 0 if not available
     */
    public int getFtpCode() {
        return ftpCode;
    }

    @Override
    public String errorType() {
        return FtpUtil.ErrorType.ServiceUnavailableError.errorType();
    }
}
