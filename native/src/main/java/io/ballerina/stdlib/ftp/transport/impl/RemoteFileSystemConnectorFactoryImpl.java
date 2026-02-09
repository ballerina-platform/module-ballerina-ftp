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
import io.ballerina.stdlib.ftp.server.FtpListener;
import io.ballerina.stdlib.ftp.transport.RemoteFileSystemConnectorFactory;
import io.ballerina.stdlib.ftp.transport.client.connector.contract.VfsClientConnector;
import io.ballerina.stdlib.ftp.transport.client.connector.contractimpl.VfsClientConnectorImpl;
import io.ballerina.stdlib.ftp.transport.listener.RemoteFileSystemListener;
import io.ballerina.stdlib.ftp.transport.server.FileDependencyCondition;
import io.ballerina.stdlib.ftp.transport.server.connector.contract.RemoteFileSystemServerConnector;
import io.ballerina.stdlib.ftp.transport.server.connector.contractimpl.MultiPathServerConnector;
import io.ballerina.stdlib.ftp.transport.server.connector.contractimpl.RemoteFileSystemServerConnectorImpl;

import java.util.List;
import java.util.Map;

/**
 * Implementation for {@link RemoteFileSystemConnectorFactory}.
 */
public class RemoteFileSystemConnectorFactoryImpl implements RemoteFileSystemConnectorFactory {

    @Override
    public RemoteFileSystemServerConnector createServerConnector(Map<String, Object> connectorConfig,
                                                                 RemoteFileSystemListener remoteFileSystemListener)
            throws RemoteFileSystemConnectorException {
        return new RemoteFileSystemServerConnectorImpl(connectorConfig, remoteFileSystemListener);
    }

    /**
     * Creates a server connector configured with the provided connector settings, dependency conditions, and listener.
     *
     * @param connectorConfig      map of connector configuration properties
     * @param dependencyConditions list of file dependency conditions that the connector should enforce
     * @param remoteFileSystemListener listener to receive remote filesystem events
     * @return a configured RemoteFileSystemServerConnector
     * @throws RemoteFileSystemConnectorException if the connector cannot be created with the given configuration
     */
    @Override
    public RemoteFileSystemServerConnector createServerConnector(Map<String, Object> connectorConfig,
                                                                 List<FileDependencyCondition> dependencyConditions,
                                                                 RemoteFileSystemListener remoteFileSystemListener)
            throws RemoteFileSystemConnectorException {
        return new RemoteFileSystemServerConnectorImpl(connectorConfig, dependencyConditions, remoteFileSystemListener);
    }

    /**
     * Create a server connector that supports multiple remote paths.
     *
     * @param connectorConfig configuration options for the connector
     * @param ftpListener     listener to receive FTP events from the connector
     * @return a RemoteFileSystemServerConnector configured for multi-path handling
     * @throws RemoteFileSystemConnectorException if the connector cannot be created
     */
    @Override
    public RemoteFileSystemServerConnector createMultiPathServerConnector(Map<String, Object> connectorConfig,
                                                                          FtpListener ftpListener)
            throws RemoteFileSystemConnectorException {
        return new MultiPathServerConnector(connectorConfig, ftpListener);
    }

    /**
     * Creates a VFS client connector configured with the provided connector settings.
     *
     * @param connectorConfig map of connector configuration options
     * @return a configured VfsClientConnector instance
     */
    @Override
    public VfsClientConnector createVfsClientConnector(Map<String, Object> connectorConfig)
            throws RemoteFileSystemConnectorException {
        return new VfsClientConnectorImpl(connectorConfig);
    }
}