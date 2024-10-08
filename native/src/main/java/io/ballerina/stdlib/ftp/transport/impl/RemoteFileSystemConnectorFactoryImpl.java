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

package io.ballerina.stdlib.ftp.transport.impl;

import io.ballerina.stdlib.ftp.exception.RemoteFileSystemConnectorException;
import io.ballerina.stdlib.ftp.transport.RemoteFileSystemConnectorFactory;
import io.ballerina.stdlib.ftp.transport.client.connector.contract.VfsClientConnector;
import io.ballerina.stdlib.ftp.transport.client.connector.contractimpl.VfsClientConnectorImpl;
import io.ballerina.stdlib.ftp.transport.listener.RemoteFileSystemListener;
import io.ballerina.stdlib.ftp.transport.server.connector.contract.RemoteFileSystemServerConnector;
import io.ballerina.stdlib.ftp.transport.server.connector.contractimpl.RemoteFileSystemServerConnectorImpl;

import java.util.Map;

/**
 * Implementation for {@link RemoteFileSystemConnectorFactory}.
 */
public class RemoteFileSystemConnectorFactoryImpl implements RemoteFileSystemConnectorFactory {

    @Override
    public RemoteFileSystemServerConnector createServerConnector(Map<String, String> connectorConfig,
                                                                 RemoteFileSystemListener remoteFileSystemListener)
            throws RemoteFileSystemConnectorException {
        return new RemoteFileSystemServerConnectorImpl(connectorConfig, remoteFileSystemListener);
    }

    @Override
    public VfsClientConnector createVfsClientConnector(Map<String, String> connectorConfig)
            throws RemoteFileSystemConnectorException {
        return new VfsClientConnectorImpl(connectorConfig);
    }
}
