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

package io.ballerina.stdlib.ftp.server;

import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.stdlib.ftp.exception.BallerinaFtpException;
import io.ballerina.stdlib.ftp.exception.RemoteFileSystemConnectorException;
import io.ballerina.stdlib.ftp.transport.RemoteFileSystemConnectorFactory;
import io.ballerina.stdlib.ftp.transport.impl.RemoteFileSystemConnectorFactoryImpl;
import io.ballerina.stdlib.ftp.transport.server.connector.contract.RemoteFileSystemServerConnector;
import io.ballerina.stdlib.ftp.util.FtpConstants;
import io.ballerina.stdlib.ftp.util.FtpUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static io.ballerina.stdlib.ftp.util.FtpUtil.ErrorType.Error;

/**
 * Helper class for listener functions.
 */
public class FtpListenerHelper {

    private FtpListenerHelper() {
        // private constructor
    }

    public static Object register(BObject ftpListener, BMap<Object, Object> serviceEndpointConfig, BObject service,
            String name) {
        try {
            Map<String, String> paramMap = getServerConnectorParamMap(serviceEndpointConfig);
            RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
            final FtpListener listener = new FtpListener(Runtime.getCurrentRuntime(), service);
            if (name == null || name.isEmpty()) {
                name = service.getType().getName();
            }
            RemoteFileSystemServerConnector serverConnector = fileSystemConnectorFactory
                    .createServerConnector(name, paramMap, listener);
            ftpListener.addNativeData(FtpConstants.FTP_SERVER_CONNECTOR, serverConnector);
            // This is a temporary solution
            serviceEndpointConfig.addNativeData(FtpConstants.FTP_SERVER_CONNECTOR, serverConnector);
            return serverConnector;
        } catch (RemoteFileSystemConnectorException | BallerinaFtpException e) {
            Throwable rootCause = findRootCause(e);
            String detail = (rootCause != null) ? rootCause.getMessage() : null;
            return FtpUtil.createError(e.getMessage(), detail, Error.errorType());
        }
    }

    private static Throwable findRootCause(Throwable throwable) {
        Objects.requireNonNull(throwable);
        Throwable rootCause = throwable;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }

    private static Map<String, String> getServerConnectorParamMap(BMap serviceEndpointConfig)
            throws BallerinaFtpException {
        Map<String, String> params = new HashMap<>(12);
        BMap auth = serviceEndpointConfig.getMapValue(StringUtils.fromString(
                FtpConstants.ENDPOINT_CONFIG_AUTH));
        String url = FtpUtil.createUrl(serviceEndpointConfig);
        params.put(FtpConstants.URI, url);
        addStringProperty(serviceEndpointConfig, params);
        if (auth != null) {
            final BMap privateKey = auth.getMapValue(StringUtils.fromString(
                    FtpConstants.ENDPOINT_CONFIG_PRIVATE_KEY));
            if (privateKey != null) {
                final String privateKeyPath = (privateKey.getStringValue(StringUtils.fromString(
                        FtpConstants.ENDPOINT_CONFIG_PATH))).getValue();
                params.put(FtpConstants.IDENTITY, privateKeyPath);
                final String privateKeyPassword = (privateKey.getStringValue(StringUtils.fromString(
                        FtpConstants.ENDPOINT_CONFIG_PASS_KEY))).getValue();
                if (privateKeyPassword != null && !privateKeyPassword.isEmpty()) {
                    params.put(FtpConstants.IDENTITY_PASS_PHRASE, privateKeyPassword);
                }
            }
        }
        params.put(FtpConstants.USER_DIR_IS_ROOT, String.valueOf(false));
        params.put(FtpConstants.AVOID_PERMISSION_CHECK, String.valueOf(true));
        params.put(FtpConstants.PASSIVE_MODE, String.valueOf(true));
        return params;
    }

    private static void addStringProperty(BMap config, Map<String, String> params) {
        final String value = (config.getStringValue(StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_FILE_PATTERN)))
                .getValue();
        if (value != null && !value.isEmpty()) {
            params.put(FtpConstants.FILE_NAME_PATTERN, value);
        }
    }

    public static void poll(BMap<Object, Object> config) throws BallerinaFtpException {
        RemoteFileSystemServerConnector connector = (RemoteFileSystemServerConnector) config.
                getNativeData(FtpConstants.FTP_SERVER_CONNECTOR);
        try {
            connector.poll();
        } catch (RemoteFileSystemConnectorException e) {
            throw new BallerinaFtpException(e.getMessage());
        }
    }

    public static Object deregister(BObject ftpListener, BObject service) {
        try {
            Object serverConnectorObject = ftpListener.getNativeData(FtpConstants.FTP_SERVER_CONNECTOR);
            if (serverConnectorObject instanceof RemoteFileSystemServerConnector) {
                RemoteFileSystemServerConnector serverConnector
                        = (RemoteFileSystemServerConnector) serverConnectorObject;
                serverConnector.stop();
            }
        } catch (RemoteFileSystemConnectorException e) {
            Throwable rootCause = findRootCause(e);
            String detail = (rootCause != null) ? rootCause.getMessage() : null;
            return FtpUtil.createError(e.getMessage(), detail, Error.errorType());
        } finally {
            ftpListener.addNativeData(FtpConstants.FTP_SERVER_CONNECTOR, null);
        }
        return null;
    }

}
