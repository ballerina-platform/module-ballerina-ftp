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

package org.ballerinalang.stdlib.ftp.server;

import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import org.ballerinalang.stdlib.ftp.util.BallerinaFTPException;
import org.ballerinalang.stdlib.ftp.util.FTPConstants;
import org.ballerinalang.stdlib.ftp.util.FTPUtil;
import org.wso2.transport.remotefilesystem.Constants;
import org.wso2.transport.remotefilesystem.RemoteFileSystemConnectorFactory;
import org.wso2.transport.remotefilesystem.exception.RemoteFileSystemConnectorException;
import org.wso2.transport.remotefilesystem.impl.RemoteFileSystemConnectorFactoryImpl;
import org.wso2.transport.remotefilesystem.server.connector.contract.RemoteFileSystemServerConnector;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for listener functions.
 */
public class FTPListenerHelper {

    private FTPListenerHelper() {
        // private constructor
    }

    public static RemoteFileSystemServerConnector register(BObject ftpListener,
                                                           BMap<Object, Object> serviceEndpointConfig, BObject service,
                                                           String name)
            throws BallerinaFTPException {

        try {
            Map<String, String> paramMap = getServerConnectorParamMap(serviceEndpointConfig);
            RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
            final FTPListener listener = new FTPListener(Runtime.getCurrentRuntime(), service);
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

    private static Map<String, String> getServerConnectorParamMap(BMap serviceEndpointConfig)
            throws BallerinaFTPException {

        Map<String, String> params = new HashMap<>(12);

        BMap auth = serviceEndpointConfig.getMapValue(StringUtils.fromString(
                FTPConstants.ENDPOINT_CONFIG_AUTH));
        String url = FTPUtil.createUrl(serviceEndpointConfig);
        params.put(Constants.URI, url);
        addStringProperty(serviceEndpointConfig, params);
        if (auth != null) {
            final BMap privateKey = auth.getMapValue(StringUtils.fromString(
                    FTPConstants.ENDPOINT_CONFIG_PRIVATE_KEY));
            if (privateKey != null) {
                final String privateKeyPath = (privateKey.getStringValue(StringUtils.fromString(
                        FTPConstants.ENDPOINT_CONFIG_PATH))).getValue();
                params.put(Constants.IDENTITY, privateKeyPath);
                final String privateKeyPassword = (privateKey.getStringValue(StringUtils.fromString(
                        FTPConstants.ENDPOINT_CONFIG_PASS_KEY))).getValue();
                if (privateKeyPassword != null && !privateKeyPassword.isEmpty()) {
                    params.put(Constants.IDENTITY_PASS_PHRASE, privateKeyPassword);
                }
            }
        }
        params.put(Constants.USER_DIR_IS_ROOT, String.valueOf(false));
        params.put(Constants.AVOID_PERMISSION_CHECK, String.valueOf(true));
        params.put(Constants.PASSIVE_MODE, String.valueOf(true));
        return params;
    }

    private static void addStringProperty(BMap config, Map<String, String> params) {

        final String value = (config.getStringValue(StringUtils.fromString(FTPConstants.ENDPOINT_CONFIG_FILE_PATTERN)))
                .getValue();
        if (value != null && !value.isEmpty()) {
            params.put(Constants.FILE_NAME_PATTERN, value);
        }
    }

    public static void poll(BMap<Object, Object> config) throws BallerinaFTPException {

        RemoteFileSystemServerConnector connector = (RemoteFileSystemServerConnector) config.
                getNativeData(FTPConstants.FTP_SERVER_CONNECTOR);
        try {
            connector.poll();
        } catch (RemoteFileSystemConnectorException e) {
            throw new BallerinaFTPException(e.getMessage());

        }
    }
}
