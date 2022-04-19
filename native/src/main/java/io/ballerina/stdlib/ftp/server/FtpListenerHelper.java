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
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.ftp.exception.BallerinaFtpException;
import io.ballerina.stdlib.ftp.exception.RemoteFileSystemConnectorException;
import io.ballerina.stdlib.ftp.transport.RemoteFileSystemConnectorFactory;
import io.ballerina.stdlib.ftp.transport.impl.RemoteFileSystemConnectorFactoryImpl;
import io.ballerina.stdlib.ftp.transport.server.connector.contract.RemoteFileSystemServerConnector;
import io.ballerina.stdlib.ftp.util.FtpConstants;
import io.ballerina.stdlib.ftp.util.FtpUtil;
import io.ballerina.stdlib.ftp.util.ModuleUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static io.ballerina.stdlib.ftp.util.FtpConstants.FTP_CALLER;
import static io.ballerina.stdlib.ftp.util.FtpConstants.FTP_CLIENT;
import static io.ballerina.stdlib.ftp.util.FtpConstants.FTP_SERVICE_ENDPOINT_CONFIG;
import static io.ballerina.stdlib.ftp.util.FtpUtil.ErrorType.Error;
import static io.ballerina.stdlib.ftp.util.FtpUtil.getOnFileChangeMethod;

/**
 * Helper class for listener functions.
 */
public class FtpListenerHelper {

    private FtpListenerHelper() {
        // private constructor
    }

    /**
     * Initialize a new FTP Connector for the listener.
     * @param ftpListener Listener that places `ftp:WatchEvent` by Ballerina runtime
     * @param serviceEndpointConfig FTP server endpoint configuration
     */
    public static Object init(BObject ftpListener, BMap<BString, Object> serviceEndpointConfig) {
        try {
            Map<String, String> paramMap = getServerConnectorParamMap(serviceEndpointConfig);
            RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
            final FtpListener listener = new FtpListener(Runtime.getCurrentRuntime());
            RemoteFileSystemServerConnector serverConnector = fileSystemConnectorFactory
                    .createServerConnector(paramMap, listener);
            ftpListener.addNativeData(FtpConstants.FTP_SERVER_CONNECTOR, serverConnector);
            // This is a temporary solution

            ftpListener.addNativeData(FTP_SERVICE_ENDPOINT_CONFIG, serviceEndpointConfig);
            return null;
        } catch (RemoteFileSystemConnectorException | BallerinaFtpException | BError e) {
            Throwable rootCause = findRootCause(e);
            String detail = (rootCause != null) ? rootCause.getMessage() : null;
            return FtpUtil.createError(e.getMessage(), detail, Error.errorType());
        }
    }

    public static Object register(BObject ftpListener, BObject service) {
        RemoteFileSystemServerConnector ftpConnector = (RemoteFileSystemServerConnector) ftpListener.getNativeData(
                FtpConstants.FTP_SERVER_CONNECTOR);
        FtpListener listener = ftpConnector.getFtpListener();
        listener.addService(service);

        Optional<MethodType> methodType = getOnFileChangeMethod(service);
        if (methodType.isEmpty() || methodType.get().getParameters().length != 2) {
            return null;
        }
        if (listener.getCaller() != null) {
            return null;
        }
        BMap serviceEndpointConfig  = (BMap) ftpListener.getNativeData(FTP_SERVICE_ENDPOINT_CONFIG);;
        BObject caller = createCaller(serviceEndpointConfig);
        if (caller instanceof BError) {
            BError endpointError = (BError) caller;
            return FtpUtil.createError(endpointError.getMessage(), endpointError.getDetails().toString(),
                    Error.errorType());
        } else {
            listener.setCaller(caller);
        }
        return null;
    }

    protected static Throwable findRootCause(Throwable throwable) {
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
                        FtpConstants.ENDPOINT_CONFIG_KEY_PATH))).getValue();
                if (privateKeyPath.isEmpty()) {
                    throw FtpUtil.createError("Private key path cannot be empty.", null, Error.errorType());
                }
                params.put(FtpConstants.IDENTITY, privateKeyPath);
                String privateKeyPassword = null;
                if (privateKey.containsKey(StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_PASS_KEY))) {
                    privateKeyPassword = (privateKey.getStringValue(StringUtils.fromString(
                            FtpConstants.ENDPOINT_CONFIG_PASS_KEY))).getValue();
                }
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
        BString namePatternString = config.getStringValue(StringUtils.fromString(
                FtpConstants.ENDPOINT_CONFIG_FILE_PATTERN));
        String fileNamePattern = (namePatternString != null && !namePatternString.getValue().isEmpty()) ?
                namePatternString.getValue() : "";
        params.put(FtpConstants.FILE_NAME_PATTERN, fileNamePattern);
    }

    public static Object poll(BObject ftpListener) {
        RemoteFileSystemServerConnector connector = (RemoteFileSystemServerConnector) ftpListener.
                getNativeData(FtpConstants.FTP_SERVER_CONNECTOR);
        try {
            connector.poll();
        } catch (RemoteFileSystemConnectorException e) {
            return FtpUtil.createError("Error during the poll operation: " + e.getMessage(), null, Error.errorType());
        }
        return null;
    }

    public static Object deregister(BObject ftpListener, BObject service) {
        try {
            Object serverConnectorObject = ftpListener.getNativeData(FtpConstants.FTP_SERVER_CONNECTOR);
            if (serverConnectorObject instanceof RemoteFileSystemServerConnector) {
                RemoteFileSystemServerConnector serverConnector
                        = (RemoteFileSystemServerConnector) serverConnectorObject;
                Object stopError = serverConnector.stop();
                if (stopError instanceof BError) {
                    return stopError;
                }
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

    private static BObject createCaller(BMap<BString, Object> serviceEndpointConfig) {
        BObject client = ValueCreator.createObjectValue(ModuleUtils.getModule(), FTP_CLIENT, serviceEndpointConfig);
        return ValueCreator.createObjectValue(ModuleUtils.getModule(), FTP_CALLER, client);
    }
}
