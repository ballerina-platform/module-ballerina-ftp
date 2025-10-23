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

import io.ballerina.runtime.api.Environment;
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
import java.util.Optional;

import static io.ballerina.stdlib.ftp.util.FtpConstants.ENDPOINT_CONFIG_PREFERRED_METHODS;
import static io.ballerina.stdlib.ftp.util.FtpConstants.FTP_CALLER;
import static io.ballerina.stdlib.ftp.util.FtpConstants.FTP_CLIENT;
import static io.ballerina.stdlib.ftp.util.FtpConstants.FTP_SERVICE_ENDPOINT_CONFIG;
import static io.ballerina.stdlib.ftp.util.FtpUtil.ErrorType.Error;
import static io.ballerina.stdlib.ftp.util.FtpUtil.findRootCause;
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
    public static Object init(Environment env, BObject ftpListener, BMap<BString, Object> serviceEndpointConfig) {
        try {
            Map<String, String> paramMap = getServerConnectorParamMap(serviceEndpointConfig);
            RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
            final FtpListener listener = new FtpListener(env.getRuntime());
            RemoteFileSystemServerConnector serverConnector = fileSystemConnectorFactory
                    .createServerConnector(paramMap, listener);
            ftpListener.addNativeData(FtpConstants.FTP_SERVER_CONNECTOR, serverConnector);
            // This is a temporary solution

            ftpListener.addNativeData(FTP_SERVICE_ENDPOINT_CONFIG, serviceEndpointConfig);
            return null;
        } catch (RemoteFileSystemConnectorException | BallerinaFtpException e) {
            return FtpUtil.createError(e.getMessage(), findRootCause(e), Error.errorType());
        } catch (BError e) {
            return e;
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
        BMap serviceEndpointConfig  = (BMap) ftpListener.getNativeData(FTP_SERVICE_ENDPOINT_CONFIG);
        BObject caller = createCaller(serviceEndpointConfig);
        if (caller instanceof BError) {
            return caller;
        } else {
            listener.setCaller(caller);
        }
        return null;
    }

    private static Map<String, String> getServerConnectorParamMap(BMap serviceEndpointConfig)
            throws BallerinaFtpException {
        Map<String, String> params = new HashMap<>(25);
        String protocol = (serviceEndpointConfig.getStringValue(StringUtils.fromString(
                FtpConstants.ENDPOINT_CONFIG_PROTOCOL))).getValue();
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
                    throw FtpUtil.createError("Private key path cannot be empty", null, Error.errorType());
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
            params.put(ENDPOINT_CONFIG_PREFERRED_METHODS, FtpUtil.getPreferredMethodsFromAuthConfig(auth));
        }
        boolean userDirIsRoot = serviceEndpointConfig.getBooleanValue(FtpConstants.USER_DIR_IS_ROOT_FIELD);
        params.put(FtpConstants.USER_DIR_IS_ROOT, String.valueOf(userDirIsRoot));
        params.put(FtpConstants.AVOID_PERMISSION_CHECK, String.valueOf(true));
        params.put(FtpConstants.PASSIVE_MODE, String.valueOf(true));
        extractListenerTimeoutConfigurations(serviceEndpointConfig, params, protocol);
        extractListenerFileTypeConfiguration(serviceEndpointConfig, params);
        extractListenerCompressionConfiguration(serviceEndpointConfig, params);
        extractListenerKnownHostsConfiguration(serviceEndpointConfig, params);
        extractListenerProxyConfiguration(serviceEndpointConfig, params);

        return params;
    }

    private static void extractListenerTimeoutConfigurations(BMap serviceEndpointConfig, Map<String, String> params,
                                                            String protocol) throws BallerinaFtpException {
        // Extract connectTimeout
        Object connectTimeoutObj = serviceEndpointConfig.get(StringUtils.fromString(FtpConstants.CONNECT_TIMEOUT));
        if (connectTimeoutObj != null) {
            double connectTimeout = ((Number) connectTimeoutObj).doubleValue();
            validateListenerTimeout(connectTimeout, "connectTimeout");
            params.put(FtpConstants.CONNECT_TIMEOUT, String.valueOf(connectTimeout));
        } else {
            // Default: 30.0 for FTP, 10.0 for SFTP
            String defaultTimeout = protocol.equalsIgnoreCase(FtpConstants.SCHEME_SFTP) ? "10.0" : "30.0";
            params.put(FtpConstants.CONNECT_TIMEOUT, defaultTimeout);
        }

        // Extract socketConfig
        BMap socketConfig = serviceEndpointConfig.getMapValue(StringUtils.fromString(FtpConstants.SOCKET_CONFIG));
        if (socketConfig != null) {
            // Extract ftpDataTimeout
            Object ftpDataTimeoutObj = socketConfig.get(StringUtils.fromString(FtpConstants.FTP_DATA_TIMEOUT));
            if (ftpDataTimeoutObj != null) {
                double ftpDataTimeout = ((Number) ftpDataTimeoutObj).doubleValue();
                validateListenerTimeout(ftpDataTimeout, "ftpDataTimeout");
                params.put(FtpConstants.FTP_DATA_TIMEOUT, String.valueOf(ftpDataTimeout));
            }

            // Extract ftpSocketTimeout
            Object ftpSocketTimeoutObj = socketConfig.get(StringUtils.fromString(FtpConstants.FTP_SOCKET_TIMEOUT));
            if (ftpSocketTimeoutObj != null) {
                double ftpSocketTimeout = ((Number) ftpSocketTimeoutObj).doubleValue();
                validateListenerTimeout(ftpSocketTimeout, "ftpSocketTimeout");
                params.put(FtpConstants.FTP_SOCKET_TIMEOUT, String.valueOf(ftpSocketTimeout));
            }

            // Extract sftpSessionTimeout
            Object sftpSessionTimeoutObj = socketConfig.get(StringUtils.fromString(FtpConstants.SFTP_SESSION_TIMEOUT));
            if (sftpSessionTimeoutObj != null) {
                double sftpSessionTimeout = ((Number) sftpSessionTimeoutObj).doubleValue();
                validateListenerTimeout(sftpSessionTimeout, "sftpSessionTimeout");
                params.put(FtpConstants.SFTP_SESSION_TIMEOUT, String.valueOf(sftpSessionTimeout));
            }
        }
    }

    private static void validateListenerTimeout(double timeout, String fieldName) throws BallerinaFtpException {
        if (timeout < 0) {
            throw new BallerinaFtpException(fieldName + " must be positive or zero (got: " + timeout + ")");
        }
        if (timeout > 600) {
            throw new BallerinaFtpException(fieldName + " must not exceed 600 seconds (got: " + timeout + ")");
        }
    }

    private static void extractListenerFileTypeConfiguration(BMap serviceEndpointConfig, Map<String, String> params) {
        BString ftpFileType = serviceEndpointConfig.getStringValue(StringUtils.fromString(FtpConstants.FTP_FILE_TYPE));
        if (ftpFileType != null && !ftpFileType.getValue().isEmpty()) {
            params.put(FtpConstants.FTP_FILE_TYPE, ftpFileType.getValue());
        }
    }

    private static void extractListenerCompressionConfiguration(BMap serviceEndpointConfig,
                                                               Map<String, String> params) {
        BString sftpCompression = serviceEndpointConfig.getStringValue(StringUtils.fromString(
                FtpConstants.SFTP_COMPRESSION));
        if (sftpCompression != null && !sftpCompression.getValue().isEmpty()) {
            params.put(FtpConstants.SFTP_COMPRESSION, sftpCompression.getValue());
        }
    }

    private static void extractListenerKnownHostsConfiguration(BMap serviceEndpointConfig,
                                                              Map<String, String> params) {
        BString knownHosts = serviceEndpointConfig.getStringValue(StringUtils.fromString(
                FtpConstants.SFTP_KNOWN_HOSTS));
        if (knownHosts != null && !knownHosts.getValue().isEmpty()) {
            params.put(FtpConstants.SFTP_KNOWN_HOSTS, knownHosts.getValue());
        }
    }

    private static void extractListenerProxyConfiguration(BMap serviceEndpointConfig, Map<String, String> params)
            throws BallerinaFtpException {
        BMap proxyConfig = serviceEndpointConfig.getMapValue(StringUtils.fromString(FtpConstants.PROXY));
        if (proxyConfig != null) {
            // Extract proxy host
            BString proxyHost = proxyConfig.getStringValue(StringUtils.fromString(FtpConstants.PROXY_HOST));
            if (proxyHost == null || proxyHost.getValue().isEmpty()) {
                throw new BallerinaFtpException("Proxy host cannot be empty");
            }
            params.put(FtpConstants.PROXY_HOST, proxyHost.getValue());

            // Extract proxy port
            Object proxyPortObj = proxyConfig.get(StringUtils.fromString(FtpConstants.PROXY_PORT));
            if (proxyPortObj != null) {
                long proxyPort = ((Number) proxyPortObj).longValue();
                if (proxyPort < 1 || proxyPort > 65535) {
                    throw new BallerinaFtpException("Proxy port must be between 1 and 65535 (got: " + proxyPort + ")");
                }
                params.put(FtpConstants.PROXY_PORT, String.valueOf(proxyPort));
            }

            // Extract proxy type
            BString proxyType = proxyConfig.getStringValue(StringUtils.fromString(FtpConstants.PROXY_TYPE));
            if (proxyType != null && !proxyType.getValue().isEmpty()) {
                params.put(FtpConstants.PROXY_TYPE, proxyType.getValue());
            }

            // Extract proxy auth
            BMap proxyAuth = proxyConfig.getMapValue(StringUtils.fromString(FtpConstants.PROXY_AUTH));
            if (proxyAuth != null) {
                BString proxyUsername = proxyAuth.getStringValue(StringUtils.fromString(
                        FtpConstants.PROXY_USERNAME));
                BString proxyPassword = proxyAuth.getStringValue(StringUtils.fromString(
                        FtpConstants.PROXY_PASSWORD));
                if (proxyUsername != null) {
                    params.put(FtpConstants.PROXY_USERNAME, proxyUsername.getValue());
                }
                if (proxyPassword != null) {
                    params.put(FtpConstants.PROXY_PASSWORD, proxyPassword.getValue());
                }
            }

            // Extract proxy command (for STREAM proxy)
            BString proxyCommand = proxyConfig.getStringValue(StringUtils.fromString(FtpConstants.PROXY_COMMAND));
            if (proxyCommand != null && !proxyCommand.getValue().isEmpty()) {
                params.put(FtpConstants.PROXY_COMMAND, proxyCommand.getValue());
            }
        }
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
            return FtpUtil.createError("Error during the poll operation: " + e.getMessage(),
                    findRootCause(e), Error.errorType());
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
            return FtpUtil.createError(e.getMessage(), findRootCause(e), Error.errorType());
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
