/*
 * Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.ei.ftp.server;

import org.ballerinalang.jvm.BRuntime;
import org.ballerinalang.jvm.values.MapValue;
import org.ballerinalang.jvm.values.ObjectValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.ei.ftp.util.BallerinaFTPException;
import org.wso2.ei.ftp.util.FTPConstants;
import org.wso2.ei.ftp.util.FTPUtil;
import org.wso2.transport.remotefilesystem.Constants;
import org.wso2.transport.remotefilesystem.RemoteFileSystemConnectorFactory;
import org.wso2.transport.remotefilesystem.exception.RemoteFileSystemConnectorException;
import org.wso2.transport.remotefilesystem.impl.RemoteFileSystemConnectorFactoryImpl;
import org.wso2.transport.remotefilesystem.server.connector.contract.RemoteFileSystemServerConnector;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for listener functions
 */
public class FTPListenerHelper {

    private static final Logger log = LoggerFactory.getLogger(FTPListenerHelper.class);

    private FTPListenerHelper() {
        // private constructor
    }

    public static RemoteFileSystemServerConnector register(ObjectValue ftpListener,
            MapValue<Object, Object> serviceEndpointConfig, ObjectValue service, String name)
            throws BallerinaFTPException {

        try {
            Map<String, String> paramMap = getServerConnectorParamMap(serviceEndpointConfig);
            RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
            final FTPListener listener = new FTPListener(BRuntime.getCurrentRuntime(), service);
            if (name == null || name.isEmpty()) {
                name = service.getType().getName();
            }
            RemoteFileSystemServerConnector serverConnector = fileSystemConnectorFactory
                    .createServerConnector(name, paramMap, listener);
            ftpListener.addNativeData(FTPConstants.FTP_SERVER_CONNECTOR, serverConnector);
            // This is a temporary solution
            serviceEndpointConfig.addNativeData(FTPConstants.FTP_SERVER_CONNECTOR, serverConnector);
            return serverConnector;
        } catch (RemoteFileSystemConnectorException e) {
            throw new BallerinaFTPException("Unable to initialize the FTP listener: " + e.getMessage(), e);
        }
    }

    private static Map<String, String> getServerConnectorParamMap(MapValue serviceEndpointConfig)
            throws BallerinaFTPException {

        Map<String, String> params = new HashMap<>(12);

        MapValue secureSocket = serviceEndpointConfig.getMapValue(FTPConstants.ENDPOINT_CONFIG_SECURE_SOCKET);
        String url = FTPUtil.createUrl(serviceEndpointConfig, log);
        params.put(Constants.URI, url);
        addStringProperty(serviceEndpointConfig, params);
        if (secureSocket != null) {
            final MapValue privateKey = secureSocket.getMapValue(FTPConstants.ENDPOINT_CONFIG_PRIVATE_KEY);
            if (privateKey != null) {
                final String privateKeyPath = privateKey.getStringValue(FTPConstants.ENDPOINT_CONFIG_FILE_PATH);
                if (privateKeyPath != null && !privateKeyPath.isEmpty()) {
                    params.put(Constants.IDENTITY, privateKeyPath);
                    final String privateKeyPassword = privateKey.getStringValue(FTPConstants.ENDPOINT_CONFIG_PASS_KEY);
                    if (privateKeyPassword != null && !privateKeyPassword.isEmpty()) {
                        params.put(Constants.IDENTITY_PASS_PHRASE, privateKeyPassword);
                    }
                }
            }
        }
        params.put(Constants.USER_DIR_IS_ROOT, String.valueOf(false));
        params.put(Constants.AVOID_PERMISSION_CHECK, String.valueOf(true));
        params.put(Constants.PASSIVE_MODE, String.valueOf(true));
        return params;
    }

    private static void addStringProperty(MapValue config, Map<String, String> params) {

        final String value = config.getStringValue(FTPConstants.ENDPOINT_CONFIG_FILE_PATTERN);
        if (value != null && !value.isEmpty()) {
            params.put(Constants.FILE_NAME_PATTERN, value);
        }
    }

    public static void poll(MapValue<Object, Object> config) throws BallerinaFTPException {

        RemoteFileSystemServerConnector connector = (RemoteFileSystemServerConnector) config.
                getNativeData(FTPConstants.FTP_SERVER_CONNECTOR);
        try {
            connector.poll();
        } catch (RemoteFileSystemConnectorException e) {
            throw new BallerinaFTPException(e.getMessage());

        }
    }
}
