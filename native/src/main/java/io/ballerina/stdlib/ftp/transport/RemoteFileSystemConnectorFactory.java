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

package io.ballerina.stdlib.ftp.transport;

//import io.ballerina.stdlib.ftp.transport.client.connector.contract.VFSClientConnector;
import io.ballerina.stdlib.ftp.transport.client.connector.contract.VFSClientConnector;
import io.ballerina.stdlib.ftp.transport.exception.RemoteFileSystemConnectorException;
import io.ballerina.stdlib.ftp.transport.listener.RemoteFileSystemListener;
import io.ballerina.stdlib.ftp.transport.server.connector.contract.RemoteFileSystemServerConnector;

import java.util.Map;

/**
 * Allow to create File system connectors.
 */
public interface RemoteFileSystemConnectorFactory {

    /**
     * @param serviceId          id used to identify the server connector instance.
     * @param connectorConfig    properties required for the {@link RemoteFileSystemServerConnector}.
     * @param remoteFileSystemListener listener which gets triggered when message comes.
     * @return RemoteFileSystemServerConnector RemoteFileSystemServerConnector instance.
     * @throws RemoteFileSystemConnectorException if any error occurred when creating the server connector.
     */
    RemoteFileSystemServerConnector createServerConnector(String serviceId, Map<String, String> connectorConfig,
                                                          RemoteFileSystemListener remoteFileSystemListener)
            throws RemoteFileSystemConnectorException;

    /**
     * @param connectorConfig          properties required for the {@link VFSClientConnector}.
     * @param remoteFileSystemListener listener which gets triggered when message comes.
     * @return VFSClientConnector instance
     * @throws RemoteFileSystemConnectorException if any error occurred when initializing the server connector.
     */
    VFSClientConnector createVFSClientConnector(Map<String, String> connectorConfig,
                                                RemoteFileSystemListener remoteFileSystemListener)
            throws RemoteFileSystemConnectorException;
}
