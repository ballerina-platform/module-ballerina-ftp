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
import io.ballerina.runtime.api.types.ObjectType;
import io.ballerina.runtime.api.types.Parameter;
import io.ballerina.runtime.api.types.TypeTags;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.ftp.exception.BallerinaFtpException;
import io.ballerina.stdlib.ftp.exception.FtpInvalidConfigException;
import io.ballerina.stdlib.ftp.exception.RemoteFileSystemConnectorException;
import io.ballerina.stdlib.ftp.transport.RemoteFileSystemConnectorFactory;
import io.ballerina.stdlib.ftp.transport.impl.RemoteFileSystemConnectorFactoryImpl;
import io.ballerina.stdlib.ftp.transport.server.FileDependencyCondition;
import io.ballerina.stdlib.ftp.transport.server.connector.contract.RemoteFileSystemServerConnector;
import io.ballerina.stdlib.ftp.transport.server.connector.contractimpl.MultiPathServerConnector;
import io.ballerina.stdlib.ftp.transport.server.connector.contractimpl.RemoteFileSystemServerConnectorImpl;
import io.ballerina.stdlib.ftp.util.FtpConstants;
import io.ballerina.stdlib.ftp.util.FtpUtil;
import io.ballerina.stdlib.ftp.util.ModuleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import static io.ballerina.stdlib.ftp.util.FtpUtil.ErrorType.InvalidConfigError;
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

    private static final Logger log = LoggerFactory.getLogger(FtpListenerHelper.class);

    private static final String CLOSE_METHOD = "close";
    private static final String CLOSE_CALLER_ERROR = "Error occurred while closing the caller: ";
    private static final BString CLIENT_INSTANCE = StringUtils.fromString("client");
    private static final String BASE_PARAMS_KEY = "BASE_CONNECTOR_PARAMS";
    private static final String FTP_LISTENER_KEY = "FTP_LISTENER_INSTANCE";

    public static final BString CSV_FAIL_SAFE = StringUtils.fromString("csvFailSafe");

    private FtpListenerHelper() {
        // private constructor
    }

    /**
     * Initialize the FTP Listener without creating a connector.
     * The connector is created lazily during the first service registration,
     * allowing us to choose between single-path (legacy) or multi-path mode
     * based on whether the first service uses @ftp:ServiceConfig.
     *
     * @param ftpListener           Listener that places `ftp:WatchEvent` by Ballerina runtime
     * @param serviceEndpointConfig FTP server endpoint configuration
     */
    public static Object init(Environment env, BObject ftpListener, BMap<BString, Object> serviceEndpointConfig) {
        try {
            Map<String, Object> paramMap = getServerConnectorParamMap(serviceEndpointConfig);
            final FtpListener listener = new FtpListener(env);

            boolean laxDataBinding = serviceEndpointConfig.getBooleanValue(
                    StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_LAX_DATABINDING));
            listener.setLaxDataBinding(laxDataBinding);

            BMap<?, ?> csvFailSafe = serviceEndpointConfig.getMapValue(CSV_FAIL_SAFE);
            listener.setCsvFailSafeConfigs(csvFailSafe);

            // Store all necessary data for connector creation during first service registration
            ftpListener.addNativeData(BASE_PARAMS_KEY, paramMap);
            ftpListener.addNativeData(FTP_LISTENER_KEY, listener);
            ftpListener.addNativeData(FTP_SERVICE_ENDPOINT_CONFIG, serviceEndpointConfig);
            // No connector created yet - deferred to register()
            return null;
        } catch (FtpInvalidConfigException e) {
            return FtpUtil.createError(e.getMessage(), findRootCause(e), InvalidConfigError.errorType());
        } catch (BallerinaFtpException e) {
            return FtpUtil.createError(e.getMessage(), findRootCause(e), Error.errorType());
        } catch (BError e) {
            return e;
        }
    }

    public static Object register(BObject ftpListener, BObject service) {
        RemoteFileSystemServerConnector ftpConnector = (RemoteFileSystemServerConnector) ftpListener.getNativeData(
                FtpConstants.FTP_SERVER_CONNECTOR);
        FtpListener listener = (FtpListener) ftpListener.getNativeData(FTP_LISTENER_KEY);

        // Check for @ftp:ServiceConfig annotation
        Optional<BMap<BString, Object>> serviceConfigAnnotation = getServiceConfigAnnotation(service);
        ServiceConfiguration serviceConfiguration = null;

        // First service registration - create the appropriate connector type
        if (ftpConnector == null) {
            if (serviceConfigAnnotation.isPresent()) {
                // First service has @ServiceConfig - create multi-path connector
                try {
                    ServiceConfiguration config = parseServiceConfiguration(serviceConfigAnnotation.get());
                    serviceConfiguration = config;
                    log.warn("Creating multi-path connector for service with @ftp:ServiceConfig annotation " +
                            "and path '{}'. The listener-level 'path' configuration will be ignored.",
                            config.getPath());

                    Object createResult = createMultiPathConnector(ftpListener, listener);
                    if (createResult != null) {
                        return createResult;
                    }
                    ftpConnector = (RemoteFileSystemServerConnector) ftpListener.getNativeData(
                            FtpConstants.FTP_SERVER_CONNECTOR);

                    // Add consumer for this service's path
                    MultiPathServerConnector multiPathConnector = (MultiPathServerConnector) ftpConnector;
                    log.debug("Adding path consumer for: {}", config.getPath());
                    multiPathConnector.addPathConsumer(
                            config.getPath(),
                            config.getFileNamePattern(),
                            config.getMinAge(),
                            config.getMaxAge(),
                            config.getAgeCalculationMode(),
                            config.getDependencyConditions()
                    );
                    listener.setFileSystemManager(multiPathConnector.getFileSystemManager());
                    listener.setFileSystemOptions(multiPathConnector.getFileSystemOptions());

                } catch (FtpInvalidConfigException e) {
                    return FtpUtil.createError(e.getMessage(), findRootCause(e), InvalidConfigError.errorType());
                } catch (RemoteFileSystemConnectorException e) {
                    return FtpUtil.createError(e.getMessage(), findRootCause(e), Error.errorType());
                }
            } else {
                // First service has no @ServiceConfig - create legacy single-path connector
                try {
                    BMap<BString, Object> serviceEndpointConfig =
                            (BMap<BString, Object>) ftpListener.getNativeData(FTP_SERVICE_ENDPOINT_CONFIG);
                    Object createResult = createLegacySinglePathConnector(ftpListener, listener,
                            serviceEndpointConfig);
                    if (createResult != null) {
                        return createResult;
                    }
                    ftpConnector = (RemoteFileSystemServerConnector) ftpListener.getNativeData(
                            FtpConstants.FTP_SERVER_CONNECTOR);
                } catch (Exception e) {
                    return FtpUtil.createError("Failed to create connector: " + e.getMessage(),
                            findRootCause(e), Error.errorType());
                }
            }
        } else {
            // Subsequent service registration - validate consistency
            boolean previousUsesConfig = listener.usesServiceLevelConfig();
            boolean currentUsesConfig = serviceConfigAnnotation.isPresent();

            if (previousUsesConfig != currentUsesConfig) {
                String errorMsg = currentUsesConfig
                        ? "All services attached to a listener must use @ftp:ServiceConfig annotation when any " +
                          "service uses it. Previous services did not use the annotation."
                        : "All services attached to a listener must use @ftp:ServiceConfig annotation when any " +
                          "service uses it. The new service is missing the annotation.";
                return FtpUtil.createError(errorMsg, null, InvalidConfigError.errorType());
            }

            // If service has @ftp:ServiceConfig, add its configuration and path consumer
            if (serviceConfigAnnotation.isPresent()) {
                try {
                    ServiceConfiguration config = parseServiceConfiguration(serviceConfigAnnotation.get());
                    serviceConfiguration = config;

                    // Path must be unique across services on the same listener
                    if (listener.getServiceConfiguration(config.getPath()) != null) {
                        return FtpUtil.createError(
                                "Duplicate path '" + config.getPath() + "' in @ftp:ServiceConfig. " +
                                "Each service must monitor a unique path.", null, InvalidConfigError.errorType());
                    }

                    // Add consumer for this service's path
                    if (!(ftpConnector instanceof MultiPathServerConnector)) {
                        return FtpUtil.createError(
                                "Invalid connector state: expected multi-path connector for services with " +
                                "@ftp:ServiceConfig annotation.", null, InvalidConfigError.errorType());
                    }

                    MultiPathServerConnector multiPathConnector = (MultiPathServerConnector) ftpConnector;
                    if (!multiPathConnector.hasConsumerForPath(config.getPath())) {
                        log.debug("Adding path consumer for: {}", config.getPath());
                        multiPathConnector.addPathConsumer(
                                config.getPath(),
                                config.getFileNamePattern(),
                                config.getMinAge(),
                                config.getMaxAge(),
                                config.getAgeCalculationMode(),
                                config.getDependencyConditions()
                        );
                    }

                    log.debug("Service registered with path: {}", config.getPath());
                } catch (FtpInvalidConfigException e) {
                    return FtpUtil.createError(e.getMessage(), findRootCause(e), InvalidConfigError.errorType());
                } catch (RemoteFileSystemConnectorException e) {
                    return FtpUtil.createError(e.getMessage(), findRootCause(e), Error.errorType());
                }
            }
        }

        FormatMethodsHolder formatMethodsHolder;
        try {
            formatMethodsHolder = new FormatMethodsHolder(service);
        } catch (FtpInvalidConfigException e) {
            return FtpUtil.createError(e.getMessage(), e, FtpUtil.ErrorType.InvalidConfigError.errorType());
        }

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

        // Post-processing actions require a caller even if the handler doesn't accept one
        if (!needsCaller && formatMethodsHolder.hasPostProcessingActions()) {
            needsCaller = true;
        }

        BObject caller = null;
        if (needsCaller) {
            BMap<BString, Object> serviceEndpointConfig =
                    (BMap<BString, Object>) ftpListener.getNativeData(FTP_SERVICE_ENDPOINT_CONFIG);
            if (serviceConfiguration != null) {
                BMap<BString, Object> callerConfig =
                        createCallerConfigWithPath(serviceEndpointConfig, serviceConfiguration.getPath());
                BObject createdCaller = createCaller(callerConfig);
                if (TypeUtils.getType(createdCaller).getTag() == TypeTags.ERROR_TAG) {
                    return createdCaller;
                }
                caller = createdCaller;
            } else {
                if (listener.getCaller() == null) {
                    BObject createdCaller = createCaller(serviceEndpointConfig);
                    if (TypeUtils.getType(createdCaller).getTag() == TypeTags.ERROR_TAG) {
                        return createdCaller;
                    }
                    listener.setCaller(createdCaller);
                }
                caller = listener.getCaller();
            }
        }

        ServiceContext context = new ServiceContext(service, serviceConfiguration, formatMethodsHolder, caller);
        if (!listener.addServiceContext(context)) {
            if (serviceConfiguration != null) {
                return FtpUtil.createError(
                        "Duplicate path '" + serviceConfiguration.getPath() + "' in @ftp:ServiceConfig. " +
                        "Each service must monitor a unique path.", null, InvalidConfigError.errorType());
            }
            return FtpUtil.createError("Failed to register service.", null, InvalidConfigError.errorType());
        }
        return null;
    }

    private static Object createMultiPathConnector(BObject ftpListener, FtpListener listener) {
        try {
            Map<String, Object> baseParams = (Map<String, Object>) ftpListener.getNativeData(BASE_PARAMS_KEY);
            if (baseParams == null) {
                return FtpUtil.createError("Failed to create multi-path connector: " +
                        "base parameters not found", null, Error.errorType());
            }

            // Listener-level monitoring configs must be ignored for service-level configuration.
            Map<String, Object> connectorParams = new HashMap<>(baseParams);
            connectorParams.remove(FtpConstants.FILE_NAME_PATTERN);
            connectorParams.remove(FtpConstants.FILE_AGE_FILTER_MIN_AGE);
            connectorParams.remove(FtpConstants.FILE_AGE_FILTER_MAX_AGE);
            connectorParams.remove(FtpConstants.FILE_AGE_FILTER_AGE_CALCULATION_MODE);

            RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
            RemoteFileSystemServerConnector serverConnector =
                    fileSystemConnectorFactory.createMultiPathServerConnector(connectorParams, listener);
            ftpListener.addNativeData(FtpConstants.FTP_SERVER_CONNECTOR, serverConnector);

            log.debug("Created multi-path connector for service-level configuration");
            return null;
        } catch (Exception e) {
            return FtpUtil.createError("Failed to create multi-path connector: " + e.getMessage(),
                    findRootCause(e), Error.errorType());
        }
    }

    private static Object createLegacySinglePathConnector(BObject ftpListener, FtpListener listener,
                                                          BMap<BString, Object> serviceEndpointConfig) {
        try {
            Map<String, Object> paramMap = (Map<String, Object>) ftpListener.getNativeData(BASE_PARAMS_KEY);
            RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();

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
                BString path = serviceEndpointConfig.getStringValue(
                        StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_PATH));
                if (path != null && !path.getValue().isEmpty()) {
                    listener.setLegacyListenerPath(path.getValue());
                }
            }

            ftpListener.addNativeData(FtpConstants.FTP_SERVER_CONNECTOR, serverConnector);
            log.debug("Created legacy single-path connector");
            return null;
        } catch (FtpInvalidConfigException e) {
            return FtpUtil.createError(e.getMessage(), findRootCause(e), InvalidConfigError.errorType());
        } catch (RemoteFileSystemConnectorException e) {
            return FtpUtil.createError(e);
        }
    }

    /**
     * Extracts the @ftp:ServiceConfig annotation from a service.
     *
     * @param service The service object
     * @return Optional containing the annotation map if present
     */
    private static Optional<BMap<BString, Object>> getServiceConfigAnnotation(BObject service) {
        ObjectType serviceType = (ObjectType) TypeUtils.getReferredType(TypeUtils.getType(service));

        BMap<BString, Object> annotations = serviceType.getAnnotations();
        if (annotations == null) {
            return Optional.empty();
        }

        for (BString key : annotations.getKeys()) {
            String keyStr = key.getValue();
            if (keyStr.endsWith(FtpConstants.SERVICE_CONFIG_ANNOTATION)) {
                Object annotationValue = annotations.get(key);
                if (annotationValue instanceof BMap) {
                    return Optional.of((BMap<BString, Object>) annotationValue);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Parses the @ftp:ServiceConfig annotation into a ServiceConfiguration object.
     *
     * @param annotation The annotation map
     * @return ServiceConfiguration object
     * @throws FtpInvalidConfigException if configuration is invalid
     */
    private static ServiceConfiguration parseServiceConfiguration(BMap<BString, Object> annotation)
            throws FtpInvalidConfigException {
        ServiceConfiguration.Builder builder = new ServiceConfiguration.Builder();

        // path is a required field in the ServiceConfiguration record; the Ballerina compiler
        // enforces its presence before the annotation reaches the native layer.
        String path = annotation.getStringValue(
                StringUtils.fromString(FtpConstants.SERVICE_CONFIG_PATH)).getValue();
        if (path.isEmpty()) {
            throw new FtpInvalidConfigException("The 'path' field cannot be empty in @ftp:ServiceConfig annotation.");
        }
        builder.path(path);

        // Extract fileNamePattern (optional)
        BString patternKey = StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_FILE_PATTERN);
        if (annotation.containsKey(patternKey)) {
            Object patternObj = annotation.get(patternKey);
            if (patternObj != null) {
                String pattern = patternObj.toString();
                // Validate regex pattern
                try {
                    java.util.regex.Pattern.compile(pattern);
                    builder.fileNamePattern(pattern);
                } catch (java.util.regex.PatternSyntaxException e) {
                    throw new FtpInvalidConfigException(
                            "Invalid regex pattern '" + pattern + "' in @ftp:ServiceConfig.fileNamePattern: " +
                            e.getMessage());
                }
            }
        }

        // Extract fileAgeFilter (optional) — uses the same extraction logic as listener-level config
        BString ageFilterKey = StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_FILE_AGE_FILTER);
        if (annotation.containsKey(ageFilterKey)) {
            Object ageFilterObj = annotation.get(ageFilterKey);
            if (ageFilterObj instanceof BMap) {
                BMap<BString, Object> ageFilter = (BMap<BString, Object>) ageFilterObj;
                parseFileAgeFilter(ageFilter, builder);
            }
        }

        // Extract fileDependencyConditions (optional)
        BString depKey = StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_FILE_DEPENDENCY_CONDITIONS);
        if (annotation.containsKey(depKey)) {
            Object depObj = annotation.get(depKey);
            if (depObj instanceof BArray) {
                BArray depArray = (BArray) depObj;
                List<FileDependencyCondition> conditions = parseFileDependencyConditionsFromArray(depArray);
                builder.dependencyConditions(conditions);
            }
        }

        return builder.build();
    }

    /**
     * Parses file age filter into ServiceConfiguration.Builder using the shared extraction logic.
     */
    private static void parseFileAgeFilter(BMap<BString, Object> ageFilter, ServiceConfiguration.Builder builder) {
        Map<String, Object> ageParams = new HashMap<>();
        addAgeFilterValues(ageFilter, ageParams);

        Object minAge = ageParams.get(FtpConstants.FILE_AGE_FILTER_MIN_AGE);
        if (minAge != null) {
            builder.minAge(Double.parseDouble((String) minAge));
        }

        Object maxAge = ageParams.get(FtpConstants.FILE_AGE_FILTER_MAX_AGE);
        if (maxAge != null) {
            builder.maxAge(Double.parseDouble((String) maxAge));
        }

        Object mode = ageParams.get(FtpConstants.FILE_AGE_FILTER_AGE_CALCULATION_MODE);
        if (mode != null) {
            builder.ageCalculationMode((String) mode);
        }
    }

    /**
     * Parses file dependency conditions from annotation array.
     */
    private static List<FileDependencyCondition> parseFileDependencyConditionsFromArray(BArray depArray)
            throws FtpInvalidConfigException {
        List<FileDependencyCondition> conditions = new ArrayList<>();

        for (int i = 0; i < depArray.size(); i++) {
            Object element = depArray.get(i);
            if (element instanceof BMap) {
                BMap<BString, Object> conditionMap = (BMap<BString, Object>) element;
                FileDependencyCondition condition = parseFileDependencyCondition(conditionMap);
                conditions.add(condition);
            }
        }

        return conditions;
    }

    /**
     * Parses a single file dependency condition.
     */
    private static FileDependencyCondition parseFileDependencyCondition(BMap<BString, Object> conditionMap)
            throws FtpInvalidConfigException {
        BString targetPatternKey = StringUtils.fromString(FtpConstants.DEPENDENCY_TARGET_PATTERN);
        BString requiredFilesKey = StringUtils.fromString(FtpConstants.DEPENDENCY_REQUIRED_FILES);
        BString matchingModeKey = StringUtils.fromString(FtpConstants.DEPENDENCY_MATCHING_MODE);
        BString requiredCountKey = StringUtils.fromString(FtpConstants.DEPENDENCY_REQUIRED_FILE_COUNT);

        String targetPattern = conditionMap.getStringValue(targetPatternKey).getValue();
        // Validate target pattern regex
        try {
            java.util.regex.Pattern.compile(targetPattern);
        } catch (java.util.regex.PatternSyntaxException e) {
            throw new FtpInvalidConfigException(
                    "Invalid regex pattern '" + targetPattern + "' in fileDependencyConditions.targetPattern: " +
                    e.getMessage());
        }

        List<String> requiredFiles = new ArrayList<>();
        if (conditionMap.containsKey(requiredFilesKey)) {
            BArray filesArray = conditionMap.getArrayValue(requiredFilesKey);
            for (int i = 0; i < filesArray.size(); i++) {
                String filePattern = filesArray.getBString(i).getValue();
                // Validate each required file pattern
                try {
                    java.util.regex.Pattern.compile(filePattern);
                } catch (java.util.regex.PatternSyntaxException e) {
                    throw new FtpInvalidConfigException(
                            "Invalid regex pattern '" + filePattern + "' in fileDependencyConditions.requiredFiles: " +
                            e.getMessage());
                }
                requiredFiles.add(filePattern);
            }
        }

        String matchingMode = FtpConstants.DEPENDENCY_MATCHING_MODE_ALL;
        if (conditionMap.containsKey(matchingModeKey)) {
            matchingMode = conditionMap.getStringValue(matchingModeKey).getValue();
        }

        int requiredFileCount = 1;
        if (conditionMap.containsKey(requiredCountKey)) {
            requiredFileCount = ((Number) conditionMap.get(requiredCountKey)).intValue();
        }

        return new FileDependencyCondition(targetPattern, requiredFiles, matchingMode, requiredFileCount);
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

    private static String extractProtocol(BMap serviceEndpointConfig) {
        return (serviceEndpointConfig.getStringValue(StringUtils.fromString(
                FtpConstants.ENDPOINT_CONFIG_PROTOCOL))).getValue();
    }

    private static void configureBasicServerParams(BMap serviceEndpointConfig, Map<String, Object> params)
            throws BallerinaFtpException {
        String url = FtpUtil.createUrl(serviceEndpointConfig);
        params.put(FtpConstants.URI, url);
        addStringProperty(serviceEndpointConfig, params);
    }

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

    private static void validateServerAuthProtocolCombination(BMap auth, String protocol)
            throws BallerinaFtpException {
        final BMap privateKey = auth.getMapValue(StringUtils.fromString(
                FtpConstants.ENDPOINT_CONFIG_PRIVATE_KEY));
        final BMap secureSocket = auth.getMapValue(StringUtils.fromString(
                FtpConstants.ENDPOINT_CONFIG_SECURE_SOCKET));
        
        if (privateKey != null && !protocol.equals(FtpConstants.SCHEME_SFTP)) {
            throw FtpUtil.createError("privateKey can only be used with SFTP protocol.", Error.errorType());
        }
        
        if (secureSocket != null && !protocol.equals(FtpConstants.SCHEME_FTPS)) {
            throw FtpUtil.createError("secureSocket can only be used with FTPS protocol.", Error.errorType());
        }
    }

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

    private static void configureServerFtpsSecureSocket(BMap secureSocket, Map<String, Object> params) 
            throws BallerinaFtpException {
        FtpUtil.configureFtpsMode(secureSocket, params);
        FtpUtil.configureFtpsDataChannelProtection(secureSocket, params);
        
        String keyStorePath = FtpUtil.extractAndConfigureStore(secureSocket, FtpConstants.SECURE_SOCKET_KEY, 
                FtpConstants.ENDPOINT_CONFIG_KEYSTORE_PATH, 
                FtpConstants.ENDPOINT_CONFIG_KEYSTORE_PASSWORD, 
                params);
        
        if (keyStorePath != null && keyStorePath.isEmpty()) {
            throw new BallerinaFtpException("Failed to load FTPS Server Keystore: Path cannot be empty");
        }

        String trustStorePath = FtpUtil.extractAndConfigureStore(secureSocket, FtpConstants.SECURE_SOCKET_TRUSTSTORE, 
                FtpConstants.ENDPOINT_CONFIG_TRUSTSTORE_PATH, 
                FtpConstants.ENDPOINT_CONFIG_TRUSTSTORE_PASSWORD, 
                params);
        
        if (trustStorePath != null && trustStorePath.isEmpty()) {
            params.remove(FtpConstants.ENDPOINT_CONFIG_TRUSTSTORE_PATH);
        }
    }

    private static void applyDefaultServerParams(BMap serviceEndpointConfig, Map<String, Object> params) {
        boolean userDirIsRoot = serviceEndpointConfig.getBooleanValue(FtpConstants.USER_DIR_IS_ROOT_FIELD);
        params.put(FtpConstants.USER_DIR_IS_ROOT, String.valueOf(userDirIsRoot));
        params.put(FtpConstants.AVOID_PERMISSION_CHECK, String.valueOf(true));
        params.put(FtpConstants.PASSIVE_MODE, String.valueOf(true));
    }

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
            addAgeFilterValues(fileAgeFilter, params);
        }
    }

    /**
     * Extracts minAge, maxAge and ageCalculationMode from a fileAgeFilter record into params.
     * Shared by both listener-level and service-level config processing.
     */
    private static void addAgeFilterValues(BMap ageFilter, Map<String, Object> params) {
        Object minAgeObj = ageFilter.get(StringUtils.fromString(FtpConstants.FILE_AGE_FILTER_MIN_AGE));
        if (minAgeObj != null) {
            params.put(FtpConstants.FILE_AGE_FILTER_MIN_AGE, String.valueOf(minAgeObj));
        }

        Object maxAgeObj = ageFilter.get(StringUtils.fromString(FtpConstants.FILE_AGE_FILTER_MAX_AGE));
        if (maxAgeObj != null) {
            params.put(FtpConstants.FILE_AGE_FILTER_MAX_AGE, String.valueOf(maxAgeObj));
        }

        BString modeStr = ageFilter.getStringValue(
                StringUtils.fromString(FtpConstants.FILE_AGE_FILTER_AGE_CALCULATION_MODE));
        if (modeStr != null && !modeStr.getValue().isEmpty()) {
            params.put(FtpConstants.FILE_AGE_FILTER_AGE_CALCULATION_MODE, modeStr.getValue());
        }
    }

    private static List<FileDependencyCondition> parseFileDependencyConditions(
            BMap<BString, Object> serviceEndpointConfig) throws FtpInvalidConfigException {
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
            FtpUtil.validateRegexPattern(targetPattern,
                    "fileDependencyConditions[" + i + "].targetPattern");

            // Required files (these are also regex patterns)
            BArray requiredFilesArray = conditionMap.getArrayValue(
                    StringUtils.fromString(FtpConstants.DEPENDENCY_REQUIRED_FILES));
            List<String> requiredFiles = new ArrayList<>();
            for (int j = 0; j < requiredFilesArray.size(); j++) {
                String requiredFilePattern = ((BString) requiredFilesArray.get(j)).getValue();
                FtpUtil.validateRegexPattern(requiredFilePattern,
                        "fileDependencyConditions[" + i + "].requiredFiles[" + j + "]");
                requiredFiles.add(requiredFilePattern);
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

    private static void addStringProperty(BMap config, Map<String, Object> params) 
    throws FtpInvalidConfigException {
        BString namePatternString = config.getStringValue(StringUtils.fromString(
                FtpConstants.ENDPOINT_CONFIG_FILE_PATTERN));
        String fileNamePattern = (namePatternString != null && !namePatternString.getValue().isEmpty()) ?
                namePatternString.getValue() : "";
        FtpUtil.validateRegexPattern(fileNamePattern, "fileNamePattern");
        params.put(FtpConstants.FILE_NAME_PATTERN, fileNamePattern);
    }

    public static Object poll(BObject ftpListener) {
        RemoteFileSystemServerConnector connector = (RemoteFileSystemServerConnector) ftpListener.
                getNativeData(FtpConstants.FTP_SERVER_CONNECTOR);
        if (connector == null) {
            return FtpUtil.createError("FTP listener is not initialized. Attach a service before polling.",
                    null, Error.errorType());
        }
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

    private static BMap<BString, Object> createCallerConfigWithPath(BMap<BString, Object> baseConfig, String path) {
        Map<String, Object> configCopy = new HashMap<>();
        for (BString key : baseConfig.getKeys()) {
            configCopy.put(key.getValue(), baseConfig.get(key));
        }
        configCopy.put(FtpConstants.ENDPOINT_CONFIG_PATH, StringUtils.fromString(path));
        return ValueCreator.createRecordValue(ModuleUtils.getModule(), "ListenerConfiguration", configCopy);
    }

    public static Object closeCaller(Environment env, BObject ftpListener) {
        RemoteFileSystemServerConnector ftpConnector = (RemoteFileSystemServerConnector) ftpListener
                .getNativeData(FtpConstants.FTP_SERVER_CONNECTOR);
        if (ftpConnector == null) {
            return null;
        }
        FtpListener listener = ftpConnector.getFtpListener();
        if (listener.usesServiceLevelConfig()) {
            for (ServiceContext context : listener.getServiceContexts()) {
                BObject caller = context.getCaller();
                if (caller == null) {
                    continue;
                }
                Object result = closeCallerClient(env, caller);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }

        BObject caller = listener.getCaller();
        if (caller == null) {
            return null;
        }
        return closeCallerClient(env, caller);
    }

    private static Object closeCallerClient(Environment env, BObject caller) {
        BObject ftpClient = caller.getObjectValue(CLIENT_INSTANCE);
        return env.yieldAndRun(() -> {
            StrandMetadata strandMetadata = new StrandMetadata(true,
                    ModuleUtils.getProperties(CLOSE_METHOD));
            Object result = env.getRuntime().callMethod(ftpClient, CLOSE_METHOD, strandMetadata);
            if (result != null) {
                BError error = (BError) result;
                return createError(CLOSE_CALLER_ERROR + error.getMessage(), findRootCause(error), FTP_ERROR);
            }
            return null;
        });
    }

    public static Object cleanup(Environment env, BObject ftpListener) {
        Object serverConnectorObject = ftpListener.getNativeData(FtpConstants.FTP_SERVER_CONNECTOR);
        RemoteFileSystemServerConnector ftpConnector =
                (serverConnectorObject instanceof RemoteFileSystemServerConnector)
                        ? (RemoteFileSystemServerConnector) serverConnectorObject
                        : null;
        FtpListener listener = ftpConnector != null
                ? ftpConnector.getFtpListener()
                : (FtpListener) ftpListener.getNativeData(FTP_LISTENER_KEY);
        try {
            closeCaller(env, ftpListener);
            if (ftpConnector != null) {
                ftpConnector.stop();
            }
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
