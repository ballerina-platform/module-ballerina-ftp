/*
 * Copyright (c) 2017 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package io.ballerina.stdlib.ftp.transport.server.connector.contract;

import io.ballerina.stdlib.ftp.exception.RemoteFileSystemConnectorException;
import io.ballerina.stdlib.ftp.server.FtpListener;
import io.ballerina.stdlib.ftp.transport.server.RemoteFileSystemConsumer;

/**
 * RemoteFileSystemServer Connector interface to poll information from given the directory location.
 */
public interface RemoteFileSystemServerConnector {

    /**
     * This method will check the latest status of the given directory location.
     * Details will receive through {@link io.ballerina.stdlib.ftp.transport.listener.RemoteFileSystemListener}
     * once the execution finish.
     *
     * @throws RemoteFileSystemConnectorException if execution failed.
     */
    void poll() throws RemoteFileSystemConnectorException;

    /**
     * This method will stops the listeners to the given directory location.
     *
     * @throws RemoteFileSystemConnectorException if execution failed.
     */
    Object stop() throws RemoteFileSystemConnectorException;

    /**
     * This method will stops the listeners to the given directory location.
     *
     */
    FtpListener getFtpListener();

    /**
     * Get the consumer instance for advanced configuration.
     *
     * @return The RemoteFileSystemConsumer instance
     */
    RemoteFileSystemConsumer getConsumer();
}
