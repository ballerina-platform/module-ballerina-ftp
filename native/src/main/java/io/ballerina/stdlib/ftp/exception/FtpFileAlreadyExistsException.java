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

package io.ballerina.stdlib.ftp.exception;

/**
 * Exception thrown when attempting to create a file or directory that already exists.
 */
public class FtpFileAlreadyExistsException extends RemoteFileSystemConnectorException {

    public FtpFileAlreadyExistsException(String message) {
        super(message);
    }

    public FtpFileAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
