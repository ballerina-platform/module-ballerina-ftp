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
import io.ballerina.runtime.api.types.Parameter;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.ftp.exception.BallerinaFtpException;
import io.ballerina.stdlib.ftp.exception.RemoteFileSystemConnectorException;
import io.ballerina.stdlib.ftp.transport.RemoteFileSystemConnectorFactory;
import io.ballerina.stdlib.ftp.transport.impl.RemoteFileSystemConnectorFactoryImpl;
import io.ballerina.stdlib.ftp.transport.server.FileDependencyCondition;
import io.ballerina.stdlib.ftp.transport.server.connector.contract.RemoteFileSystemServerConnector;
import io.ballerina.stdlib.ftp.transport.server.connector.contractimpl.RemoteFileSystemServerConnectorImpl;
import io.ballerina.stdlib.ftp.util.FtpConstants;
import io.ballerina.stdlib.ftp.util.FtpUtil;
import io.ballerina.stdlib.ftp.util.ModuleUtils;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.ballerina.stdlib.ftp.util.FtpConstants.ENDPOINT_CONFIG_PREFERRED_METHODS;
import static io.ballerina.stdlib.ftp.util.FtpConstants.FTP_CALLER;
import static io.ballerina.stdlib.ftp.util.FtpConstants.FTP_CLIENT;
import static io.ballerina.stdlib.ftp.util.FtpConstants.FTP_SERVICE_ENDPOINT_CONFIG;
import static io.ballerina.stdlib.ftp.util.FtpUtil.ErrorType.Error;
import static io.ballerina.stdlib.ftp.util.FtpUtil.extractCompressionConfiguration;
import static io.ballerina.stdlib.ftp.util.FtpUtil.extractFileTransferConfiguration;
import static io.ballerina.stdlib.ftp.util.FtpUtil.extractKnownHostsConfiguration;
import static io.ballerina.stdlib.ftp.util.FtpUtil.extractProxyConfiguration;
import static io.ballerina.stdlib.ftp.util.FtpUtil.extractTimeoutConfigurations;
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
     *
     * @param ftpListener           Listener that places `ftp:WatchEvent` by Ballerina runtime
     * @param serviceEndpointConfig FTP server endpoint configuration
     */
    public static Object init(Environment env, BObject ftpListener, BMap<BString, Object> serviceEndpointConfig) {
        try {
            Map<String, Object> paramMap = getServerConnectorParamMap(serviceEndpointConfig);
            RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
            final FtpListener listener = new FtpListener(env.getRuntime());

            RemoteFileSystemServerConnector serverConnector;
            List<FileDependencyCondition> dependencyConditions = parseFileDependencyConditions(serviceEndpointConfig);
            if (dependencyConditions.isEmpty()) {
                serverConnector = fileSystemConnectorFactory.createServerConnector(paramMap, listener);
            } else {
                serverConnector = fileSystemConnectorFactory.createServerConnector(paramMap, dependencyConditions,
                        listener);
            }

            // Pass FileSystemManager and options to listener for content fetching
            if (serverConnector instanceof RemoteFileSystemServerConnectorImpl) {
                RemoteFileSystemServerConnectorImpl connectorImpl =
                        (RemoteFileSystemServerConnectorImpl) serverConnector;
                listener.setFileSystemManager(connectorImpl.getFileSystemManager());
                listener.setFileSystemOptions(connectorImpl.getFileSystemOptions());
            }

            boolean laxDataBinding = serviceEndpointConfig.getBooleanValue(
                    StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_LAX_DATABINDING));
            listener.setLaxDataBinding(laxDataBinding);

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

        // Check if caller is needed (for onFileChange with 2 params or content methods with caller param)
        boolean needsCaller = false;

        Optional<MethodType> onFileChangeMethod = getOnFileChangeMethod(service);
        if (onFileChangeMethod.isPresent() && onFileChangeMethod.get().getParameters().length == 2) {
            needsCaller = true;
        }

        // Also check for content methods that might need caller
        Optional<MethodType> contentMethod = FtpUtil.getContentHandlerMethod(service);
        if (contentMethod.isPresent()) {
            // Content methods can have caller as 2nd or 3rd parameter
            Parameter[] params = contentMethod.get().getParameters();
            if (params.length >= 2) {
                needsCaller = true;
            }
        }

        // Also check for onFileDeleted method that might need caller
        Optional<MethodType> onFileDeletedMethod = FtpUtil.getOnFileDeletedMethod(service);
        if (onFileDeletedMethod.isPresent()) {
            // onFileDeleted can have caller as 2nd parameter
            Parameter[] params = onFileDeletedMethod.get().getParameters();
            if (params.length >= 2) {
                needsCaller = true;
            }
        }

        if (!needsCaller) {
            return null;
        }
        if (listener.getCaller() != null) {
            return null;
        }
        BMap serviceEndpointConfig = (BMap) ftpListener.getNativeData(FTP_SERVICE_ENDPOINT_CONFIG);
        BObject caller = createCaller(serviceEndpointConfig);
        if (caller instanceof BError) {
            return caller;
        } else {
            listener.setCaller(caller);
        }
        return null;
    }

    private static Map<String, Object> getServerConnectorParamMap(BMap serviceEndpointConfig)
            throws BallerinaFtpException {
        Map<String, Object> params = new HashMap<>(25);
        
        String protocol = extractProtocol(serviceEndpointConfig);
        configureBasicServerParams(serviceEndpointConfig, params);
        
        configureServerAuthentication(serviceEndpointConfig, protocol, params);
        applyDefaultServerParams(serviceEndpointConfig, params);
        addFileAgeFilterParams(serviceEndpointConfig, params);
        extractServerVfsConfigurations(serviceEndpointConfig, params);
        
        return params;
    }

    /**
     * Extracts the protocol from the service endpoint configuration.
     * 
     * @param serviceEndpointConfig The service endpoint configuration map
     * @return The protocol string
     */
    private static String extractProtocol(BMap serviceEndpointConfig) {
        return (serviceEndpointConfig.getStringValue(StringUtils.fromString(
                FtpConstants.ENDPOINT_CONFIG_PROTOCOL))).getValue();
    }

    /**
     * Configures basic server parameters (URL, file pattern).
     * 
     * @param serviceEndpointConfig The service endpoint configuration map
     * @param params The parameters map to populate
     * @throws BallerinaFtpException If URL creation fails
     */
    private static void configureBasicServerParams(BMap serviceEndpointConfig, Map<String, Object> params)
            throws BallerinaFtpException {
        String url = FtpUtil.createUrl(serviceEndpointConfig);
        params.put(FtpConstants.URI, url);
        addStringProperty(serviceEndpointConfig, params);
    }

    /**
     * Configures server authentication settings including validation and protocol-specific setup.
     * 
     * @param serviceEndpointConfig The service endpoint configuration map
     * @param protocol The protocol being used
     * @param params The parameters map to populate
     * @throws BallerinaFtpException If authentication configuration fails
     */
    private static void configureServerAuthentication(BMap serviceEndpointConfig, String protocol,
                                                       Map<String, Object> params) throws BallerinaFtpException {
        BMap auth = serviceEndpointConfig.getMapValue(StringUtils.fromString(
                FtpConstants.ENDPOINT_CONFIG_AUTH));
        if (auth == null) {
            return;
        }
        
        validateServerAuthProtocolCombination(auth, protocol);
        configureServerPrivateKey(auth, params);
        
        BMap secureSocket = auth.getMapValue(StringUtils.fromString(
                FtpConstants.ENDPOINT_CONFIG_SECURE_SOCKET));
        if (secureSocket != null && protocol.equals(FtpConstants.SCHEME_FTPS)) {
            configureServerFtpsSecureSocket(secureSocket, params);
        }
        
        if (protocol.equals(FtpConstants.SCHEME_SFTP)) {
            params.put(ENDPOINT_CONFIG_PREFERRED_METHODS, 
                    FtpUtil.getPreferredMethodsFromAuthConfig(auth));
        }
    }

    /**
     * Validates that protocol and authentication method combinations are correct for server.
     * 
     * @param auth The authentication configuration map
     * @param protocol The protocol being used (SFTP or FTPS)
     * @throws BallerinaFtpException If validation fails
     */
    private static void validateServerAuthProtocolCombination(BMap auth, String protocol)
            throws BallerinaFtpException {
        final BMap privateKey = auth.getMapValue(StringUtils.fromString(
                FtpConstants.ENDPOINT_CONFIG_PRIVATE_KEY));
        final BMap secureSocket = auth.getMapValue(StringUtils.fromString(
                FtpConstants.ENDPOINT_CONFIG_SECURE_SOCKET));
        
        if (privateKey != null && protocol.equals(FtpConstants.SCHEME_FTPS)) {
            throw FtpUtil.createError("privateKey can only be used with SFTP protocol. " +
                    "For FTPS, use secureSocket configuration.", Error.errorType());
        }
        
        if (secureSocket != null && protocol.equals(FtpConstants.SCHEME_SFTP)) {
            throw FtpUtil.createError("secureSocket can only be used with FTPS protocol. " +
                    "For SFTP, use privateKey configuration.", Error.errorType());
        }
        
        if (secureSocket != null && protocol.equals(FtpConstants.SCHEME_FTP)) {
            throw FtpUtil.createError("secureSocket can only be used with FTPS protocol. " +
                    "For FTP, do not use secureSocket configuration.", Error.errorType());
        }
    }

    /**
     * Configures private key authentication for SFTP server.
     * 
     * @param auth The authentication configuration map
     * @param params The parameters map to populate
     * @throws BallerinaFtpException If private key configuration is invalid
     */
    private static void configureServerPrivateKey(BMap auth, Map<String, Object> params)
            throws BallerinaFtpException {
        final BMap privateKey = auth.getMapValue(StringUtils.fromString(
                FtpConstants.ENDPOINT_CONFIG_PRIVATE_KEY));
        
        if (privateKey == null) {
            return;
        }
        
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

    /**
     * Configures secure socket settings for FTPS protocol on server.
     * 
     * @param secureSocket The secure socket configuration map
     * @param params The parameters map to populate
     * @throws BallerinaFtpException If keystore loading fails
     */
    private static void configureServerFtpsSecureSocket(BMap secureSocket, Map<String, Object> params) 
            throws BallerinaFtpException {
        configureServerFtpsMode(secureSocket, params);
        configureServerFtpsDataChannelProtection(secureSocket, params);
        // For Keystore
        extractAndConfigureServerStore(secureSocket, FtpConstants.SECURE_SOCKET_KEY, 
            FtpConstants.ENDPOINT_CONFIG_KEYSTORE_PATH, 
            FtpConstants.ENDPOINT_CONFIG_KEYSTORE_PASSWORD, 
            params, "Keystore");

        // For Truststore
        extractAndConfigureServerStore(secureSocket, FtpConstants.SECURE_SOCKET_TRUSTSTORE, 
            FtpConstants.ENDPOINT_CONFIG_TRUSTSTORE_PATH, 
            FtpConstants.ENDPOINT_CONFIG_TRUSTSTORE_PASSWORD, 
            params, "Truststore");
    }

    /**
     * Configures FTPS mode (IMPLICIT or EXPLICIT) for server.
     * 
     * @param secureSocket The secure socket configuration map
     * @param params The parameters map to populate
     */
    private static void configureServerFtpsMode(BMap secureSocket, Map<String, Object> params) {
        final BString mode = secureSocket.getStringValue(StringUtils.fromString(
                FtpConstants.ENDPOINT_CONFIG_FTPS_MODE));
        
        if (mode != null && !mode.getValue().isEmpty()) {
            params.put(FtpConstants.ENDPOINT_CONFIG_FTPS_MODE, mode.getValue());
        } else {
            params.put(FtpConstants.ENDPOINT_CONFIG_FTPS_MODE, FtpConstants.FTPS_MODE_EXPLICIT);
        }
    }

    /**
     * Configures FTPS data channel protection level for server.
     * 
     * @param secureSocket The secure socket configuration map
     * @param params The parameters map to populate
     */
    private static void configureServerFtpsDataChannelProtection(BMap secureSocket, Map<String, Object> params) {
        final BString dataChannelProtection = secureSocket.getStringValue(StringUtils.fromString(
                FtpConstants.ENDPOINT_CONFIG_FTPS_DATA_CHANNEL_PROTECTION));
        
        if (dataChannelProtection != null && !dataChannelProtection.getValue().isEmpty()) {
            params.put(FtpConstants.ENDPOINT_CONFIG_FTPS_DATA_CHANNEL_PROTECTION, 
                    dataChannelProtection.getValue());
        } else {
            params.put(FtpConstants.ENDPOINT_CONFIG_FTPS_DATA_CHANNEL_PROTECTION, 
                    FtpConstants.FTPS_DATA_CHANNEL_PROTECTION_PRIVATE);
        }
    }

    /**
     * Extracts a store (KeyStore or TrustStore) from secureSocket configuration and adds it to params.
     * Handles both BMap and BObject representations. Extracts path/password strings and loads the Java KeyStore.
     * 
     * @param secureSocket The secure socket configuration map
     * @param storeKey The key name (SECURE_SOCKET_KEY for KeyStore, SECURE_SOCKET_TRUSTSTORE for TrustStore)
     * @param pathConfigKey The configuration key for the store path
     * @param passwordConfigKey The configuration key for the store password
     * @param params The parameters map to populate
     * @param storeType The type of store ("Keystore" or "Truststore") for error messaging
     * @throws BallerinaFtpException 
     */
    private static void extractAndConfigureServerStore(BMap secureSocket, String storeKey, 
                                                       String pathConfigKey, String passwordConfigKey,
                                                       Map<String, Object> params, String storeType) 
            throws BallerinaFtpException {
        Object storeObj = getServerStoreObject(secureSocket, storeKey);
        if (storeObj == null) {
            return;
        }
        
        String path = null;
        String password = null;
        
        if (storeObj instanceof BMap) {
            BMap storeRecord = (BMap) storeObj;
            BString pathBStr = storeRecord.getStringValue(StringUtils.fromString(FtpConstants.KEYSTORE_PATH_KEY));
            BString passBStr = storeRecord.getStringValue(StringUtils.fromString(FtpConstants.KEYSTORE_PASSWORD_KEY));
            
            if (pathBStr != null) {
                path = pathBStr.getValue();
            }
            if (passBStr != null) {
                password = passBStr.getValue();
            }
        }
        
        // Validate empty path for keystore (Mandatory for Key, Optional for Trust)
        if (storeKey.equals(FtpConstants.SECURE_SOCKET_KEY) && (path == null || path.isEmpty())) {
            throw new BallerinaFtpException("Failed to load FTPS Server " + storeType + ": Path cannot be empty");
        }
        
        // Load the Java KeyStore Object
        if (path != null && !path.isEmpty()) {
            try {
                KeyStore javaKeyStore = FtpUtil.loadKeyStore(path, password);
                
                if (javaKeyStore != null) {
                    if (storeKey.equals(FtpConstants.SECURE_SOCKET_KEY)) {
                        params.put(FtpConstants.KEYSTORE_INSTANCE, javaKeyStore);
                    } else {
                        params.put(FtpConstants.TRUSTSTORE_INSTANCE, javaKeyStore);
                    }
                }
            } catch (BallerinaFtpException e) {
                // Uses the storeType ("Keystore" or "Truststore") in the error message
                throw new BallerinaFtpException("Failed to load FTPS Server " + storeType + ": " + e.getMessage(), e);
            }
        }
        
        if (password != null) {
            params.put(passwordConfigKey, password);
        }
    }

    /**
     * Attempts to retrieve a store object from secureSocket using multiple methods.
     * Handles both BMap and BObject representations.
     * 
     * @param secureSocket The secure socket configuration map
     * @param storeKey The key name (SECURE_SOCKET_KEY or SECURE_SOCKET_TRUSTSTORE)
     * @return The store object, or null if not found
     */
    private static Object getServerStoreObject(BMap secureSocket, String storeKey) {
        BString keyString = StringUtils.fromString(storeKey);
        Object storeObj = secureSocket.get(keyString);
        if (storeObj != null) {
            return storeObj;
        }
        
        storeObj = secureSocket.getMapValue(keyString);
        if (storeObj != null) {
            return storeObj;
        }
        
        return secureSocket.getObjectValue(keyString);
    }

    /**
     * Applies default server configuration values.
     * 
     * @param serviceEndpointConfig The service endpoint configuration map
     * @param params The parameters map to populate
     */
    private static void applyDefaultServerParams(BMap serviceEndpointConfig, Map<String, Object> params) {
        boolean userDirIsRoot = serviceEndpointConfig.getBooleanValue(FtpConstants.USER_DIR_IS_ROOT_FIELD);
        params.put(FtpConstants.USER_DIR_IS_ROOT, String.valueOf(userDirIsRoot));
        params.put(FtpConstants.AVOID_PERMISSION_CHECK, String.valueOf(true));
        params.put(FtpConstants.PASSIVE_MODE, String.valueOf(true));
    }

    /**
     * Extracts VFS-related configurations for server (timeout, file transfer, compression, known hosts, proxy).
     * 
     * @param serviceEndpointConfig The service endpoint configuration map
     * @param params The parameters map to populate
     * @throws BallerinaFtpException If extraction fails
     */
    private static void extractServerVfsConfigurations(BMap serviceEndpointConfig, Map<String, Object> params)
            throws BallerinaFtpException {
        extractTimeoutConfigurations(serviceEndpointConfig, params);
        extractFileTransferConfiguration(serviceEndpointConfig, params);
        extractCompressionConfiguration(serviceEndpointConfig, params);
        extractKnownHostsConfiguration(serviceEndpointConfig, params);
        extractProxyConfiguration(serviceEndpointConfig, params);
    }

    private static void addFileAgeFilterParams(BMap serviceEndpointConfig, Map<String, Object> params) {
        BMap fileAgeFilter = serviceEndpointConfig.getMapValue(
                StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_FILE_AGE_FILTER));
        if (fileAgeFilter != null) {
            // Min age
            Object minAgeObj = fileAgeFilter.get(StringUtils.fromString(FtpConstants.FILE_AGE_FILTER_MIN_AGE));
            if (minAgeObj != null) {
                params.put(FtpConstants.FILE_AGE_FILTER_MIN_AGE, String.valueOf(minAgeObj));
            }

            // Max age
            Object maxAgeObj = fileAgeFilter.get(StringUtils.fromString(FtpConstants.FILE_AGE_FILTER_MAX_AGE));
            if (maxAgeObj != null) {
                params.put(FtpConstants.FILE_AGE_FILTER_MAX_AGE, String.valueOf(maxAgeObj));
            }

            // Age calculation mode
            BString modeStr = fileAgeFilter.getStringValue(
                    StringUtils.fromString(FtpConstants.FILE_AGE_FILTER_AGE_CALCULATION_MODE));
            if (modeStr != null && !modeStr.getValue().isEmpty()) {
                params.put(FtpConstants.FILE_AGE_FILTER_AGE_CALCULATION_MODE, modeStr.getValue());
            }
        }
    }

    private static List<FileDependencyCondition> parseFileDependencyConditions(
            BMap<BString, Object> serviceEndpointConfig) {
        List<FileDependencyCondition> conditions = new ArrayList<>();

        BArray conditionsArray = serviceEndpointConfig.getArrayValue(
                StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_FILE_DEPENDENCY_CONDITIONS));

        if (conditionsArray == null || conditionsArray.isEmpty()) {
            return conditions;
        }

        for (int i = 0; i < conditionsArray.size(); i++) {
            BMap conditionMap = (BMap) conditionsArray.get(i);

            // Target pattern
            String targetPattern = conditionMap.getStringValue(
                    StringUtils.fromString(FtpConstants.DEPENDENCY_TARGET_PATTERN)).getValue();

            // Required files
            BArray requiredFilesArray = conditionMap.getArrayValue(
                    StringUtils.fromString(FtpConstants.DEPENDENCY_REQUIRED_FILES));
            List<String> requiredFiles = new ArrayList<>();
            for (int j = 0; j < requiredFilesArray.size(); j++) {
                requiredFiles.add(((BString) requiredFilesArray.get(j)).getValue());
            }

            // Matching mode
            String matchingMode = conditionMap.getStringValue(
                    StringUtils.fromString(FtpConstants.DEPENDENCY_MATCHING_MODE)).getValue();

            // Required file count
            long requiredFileCount = conditionMap.getIntValue(
                    StringUtils.fromString(FtpConstants.DEPENDENCY_REQUIRED_FILE_COUNT));

            conditions.add(new FileDependencyCondition(
                    targetPattern, requiredFiles, matchingMode, (int) requiredFileCount));
        }

        return conditions;
    }

    private static void addStringProperty(BMap config, Map<String, Object> params) {
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
