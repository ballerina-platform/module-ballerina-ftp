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
import io.ballerina.runtime.api.concurrent.StrandMetadata;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.ballerina.stdlib.ftp.util.FtpConstants.ENDPOINT_CONFIG_PREFERRED_METHODS;
import static io.ballerina.stdlib.ftp.util.FtpConstants.FTP_CALLER;
import static io.ballerina.stdlib.ftp.util.FtpConstants.FTP_CLIENT;
import static io.ballerina.stdlib.ftp.util.FtpConstants.FTP_ERROR;
import static io.ballerina.stdlib.ftp.util.FtpConstants.FTP_SERVICE_ENDPOINT_CONFIG;
import static io.ballerina.stdlib.ftp.util.FtpUtil.ErrorType.Error;
import static io.ballerina.stdlib.ftp.util.FtpUtil.createError;
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

    private static final String CLOSE_METHOD = "close";
    private static final String CLOSE_CALLER_ERROR = "Error occurred while closing the caller: ";
    private static final BString CLIENT_INSTANCE = StringUtils.fromString("client");

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
            Map<String, String> paramMap = getServerConnectorParamMap(serviceEndpointConfig);
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

    private static Map<String, String> getServerConnectorParamMap(BMap serviceEndpointConfig)
            throws BallerinaFtpException {
        Map<String, String> params = new HashMap<>(25);

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

        // Add file age filter parameters
        addFileAgeFilterParams(serviceEndpointConfig, params);

        extractTimeoutConfigurations(serviceEndpointConfig, params);
        extractFileTransferConfiguration(serviceEndpointConfig, params);
        extractCompressionConfiguration(serviceEndpointConfig, params);
        extractKnownHostsConfiguration(serviceEndpointConfig, params);
        extractProxyConfiguration(serviceEndpointConfig, params);

        return params;
    }

    private static void addFileAgeFilterParams(BMap serviceEndpointConfig, Map<String, String> params) {
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

    public static Object closeCaller(Environment env, BObject ftpListener) {
        RemoteFileSystemServerConnector ftpConnector = (RemoteFileSystemServerConnector) ftpListener
                .getNativeData(FtpConstants.FTP_SERVER_CONNECTOR);
        FtpListener listener = ftpConnector.getFtpListener();
        BObject caller = listener.getCaller();
        if (caller == null) {
            return null;
        }
        BObject ftpClient = caller.getObjectValue(CLIENT_INSTANCE);
        return env.yieldAndRun(() -> {
            StrandMetadata strandMetadata = new StrandMetadata(true,
                    ModuleUtils.getProperties(CLOSE_METHOD));
            Object result = env.getRuntime().callMethod(ftpClient, CLOSE_METHOD, strandMetadata);
            if (result instanceof BError error) {
                return createError(CLOSE_CALLER_ERROR + error.getMessage(), findRootCause(error), FTP_ERROR);
            }
            return null;
        });
    }

    public static Object cleanup(Environment env, BObject ftpListener) {
        Object serverConnectorObject = ftpListener.getNativeData(FtpConstants.FTP_SERVER_CONNECTOR);
        RemoteFileSystemServerConnector ftpConnector = (RemoteFileSystemServerConnector) serverConnectorObject;
        FtpListener listener = ftpConnector.getFtpListener();
        try {
            closeCaller(env, ftpListener);
            ftpConnector.stop();
        } catch (Exception e) {
            return FtpUtil.createError(e.getMessage(), findRootCause(e), FTP_ERROR);
        } finally {
            if (listener != null) {
                listener.cleanup();
            }
        }
        return null;
    }
}
