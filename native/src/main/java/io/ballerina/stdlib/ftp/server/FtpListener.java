/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.concurrent.StrandMetadata;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.IntersectionType;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.types.ObjectType;
import io.ballerina.runtime.api.types.Parameter;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.types.TypeTags;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.ftp.exception.FtpInvalidConfigException;
import io.ballerina.stdlib.ftp.exception.RemoteFileSystemConnectorException;
import io.ballerina.stdlib.ftp.transport.listener.RemoteFileSystemListener;
import io.ballerina.stdlib.ftp.transport.message.FileInfo;
import io.ballerina.stdlib.ftp.transport.message.RemoteFileSystemBaseMessage;
import io.ballerina.stdlib.ftp.transport.message.RemoteFileSystemEvent;
import io.ballerina.stdlib.ftp.transport.server.connector.contract.RemoteFileSystemServerConnector;
import io.ballerina.stdlib.ftp.util.FtpConstants;
import io.ballerina.stdlib.ftp.util.FtpUtil;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.ballerina.runtime.api.types.TypeTags.OBJECT_TYPE_TAG;
import static io.ballerina.runtime.api.types.TypeTags.RECORD_TYPE_TAG;
import static io.ballerina.stdlib.ftp.util.FtpConstants.FTP_SERVER_EVENT;
import static io.ballerina.stdlib.ftp.util.FtpConstants.FTP_WATCHEVENT_ADDED_FILES;
import static io.ballerina.stdlib.ftp.util.FtpConstants.FTP_WATCHEVENT_DELETED_FILES;
import static io.ballerina.stdlib.ftp.util.FtpConstants.ON_FILE_CHANGE_REMOTE_FUNCTION;
import static io.ballerina.stdlib.ftp.util.FtpConstants.ON_FILE_DELETE_REMOTE_FUNCTION;
import static io.ballerina.stdlib.ftp.util.FtpConstants.ON_FILE_DELETED_REMOTE_FUNCTION;
import static io.ballerina.stdlib.ftp.util.FtpUtil.ErrorType.Error;
import static io.ballerina.stdlib.ftp.util.FtpUtil.findRootCause;
import static io.ballerina.stdlib.ftp.util.FtpUtil.getOnFileChangeMethod;
import static io.ballerina.stdlib.ftp.util.FtpUtil.getOnFileDeletedMethod;

/**
 * FTP File System connector listener for Ballerina.
 */
public class FtpListener implements RemoteFileSystemListener {

    private static final Logger log = LoggerFactory.getLogger(FtpListener.class);
    private final Runtime runtime;
    private Environment environment = null;
    private Map<String, ServiceContext> serviceContexts = new ConcurrentHashMap<>();
    private Map<String, ServiceContext> pathToServiceContext = new ConcurrentHashMap<>();
    private AtomicBoolean usesServiceLevelConfig = new AtomicBoolean(false);
    private BObject caller;
    private FileSystemManager fileSystemManager;
    private FileSystemOptions fileSystemOptions;
    private boolean laxDataBinding;
    private String legacyListenerPath;
    private BMap<?, ?> csvFailSafe = ValueCreator.createMapValue();

    FtpListener(Runtime runtime) {
        this.runtime = runtime;
    }

    FtpListener(Environment environment) {
        this.environment = environment;
        this.runtime = environment.getRuntime();
    }

    public void setFileSystemManager(FileSystemManager fileSystemManager) {
        this.fileSystemManager = fileSystemManager;
    }

    public void setFileSystemOptions(FileSystemOptions fileSystemOptions) {
        this.fileSystemOptions = fileSystemOptions;
    }

    public void setLaxDataBinding(boolean laxDataBinding) {
        this.laxDataBinding = laxDataBinding;
    }

    public void setCsvFailSafeConfigs(BMap<?, ?> csvFailSafe) {
        this.csvFailSafe = csvFailSafe;
    }

    @Override
    public boolean onMessage(RemoteFileSystemBaseMessage remoteFileSystemBaseMessage) {
        if (remoteFileSystemBaseMessage instanceof RemoteFileSystemEvent) {
            RemoteFileSystemEvent event = (RemoteFileSystemEvent) remoteFileSystemBaseMessage;

            if (runtime != null) {
                if (usesServiceLevelConfig.get() && event.getSourcePath() != null) {
                    // Path-keyed routing: dispatch only to the service monitoring this path
                    ServiceContext context = pathToServiceContext.get(event.getSourcePath());
                    if (context != null) {
                        dispatchFileEventToService(this.environment, context, event);
                    }
                } else {
                    if (event.getSourcePath() == null && legacyListenerPath != null) {
                        event.setSourcePath(legacyListenerPath);
                    }
                    // Legacy single-path mode: dispatch to all registered services
                    for (ServiceContext context : serviceContexts.values()) {
                        dispatchFileEventToService(this.environment, context, event);
                    }
                }
            } else {
                log.error("Runtime should not be null.");
            }
        }
        return true;
    }

    private void dispatchFileEventToService(Environment env, ServiceContext context, RemoteFileSystemEvent event) {
        BObject service = context.getService();
        BObject caller = context.getCaller();
        FormatMethodsHolder formatMethodHolder = context.getFormatMethodsHolder();
        if (formatMethodHolder == null) {
            try {
                formatMethodHolder = new FormatMethodsHolder(service);
            } catch (FtpInvalidConfigException e) {
                // This should not happen as validation occurs during attach
                FtpUtil.createError("Invalid post-process action configuration: " + e.getMessage(),
                        e, FtpConstants.FTP_ERROR).printStackTrace();
                return;
            }
        }
        Optional<MethodType> onFileDeletedMethodType = getOnFileDeletedMethod(service);

        // Dispatch Strategy: Check handler availability in order
        if (formatMethodHolder.hasContentMethods()) {
            processContentBasedCallbacks(env, service, event, formatMethodHolder, caller);
        } else if (onFileDeletedMethodType.isPresent()) {
            if (!event.getDeletedFiles().isEmpty()) {
                processDeletionCallback(service, event, onFileDeletedMethodType.get(), caller);
            }
        } else {
            // Strategy 3: Fall back to legacy onFileChange handler
            Optional<MethodType> onFileChangeMethodType = getOnFileChangeMethod(service);
            processMetadataOnlyCallbacks(service, event, onFileChangeMethodType.get(), caller);
        }
    }

    /**
     * Processes content-based callbacks for the new content listener methods.
     * Uses ContentMethodRouter to dispatch files to appropriate handlers.
     * Also handles file deletion events via onFileDeleted method if available.
     */
    private void processContentBasedCallbacks(Environment env, BObject service, RemoteFileSystemEvent event,
                                              FormatMethodsHolder holder, BObject caller) {
        // Process added files with content methods
        if (!event.getAddedFiles().isEmpty()) {
            if (fileSystemManager == null || fileSystemOptions == null) {
                log.error("FileSystemManager or FileSystemOptions not initialized for content callbacks. " +
                        "Content methods require proper FileSystem initialization. Skipping added files processing.");
            } else {
                try {
                    FtpContentCallbackHandler contentHandler = new FtpContentCallbackHandler(
                            runtime, fileSystemManager, fileSystemOptions, laxDataBinding, csvFailSafe);
                    contentHandler.processContentCallbacks(env, service, event, holder, caller);
                } catch (Exception e) {
                    FtpUtil.createError("Error in content callback processing for added files: " + e.getMessage(),
                            e, FtpConstants.FTP_ERROR).printStackTrace();
                }
            }
        }

        // Process deleted files with onFileDeleted method if available
        if (!event.getDeletedFiles().isEmpty()) {
            Optional<MethodType> onFileDeletedMethodType = getOnFileDeletedMethod(service);
            if (onFileDeletedMethodType.isPresent()) {
                processDeletionCallback(service, event, onFileDeletedMethodType.get(), caller);
            } else {
                log.debug("No onFileDeleted method found. Skipping deletion event processing for {} deleted files.",
                        event.getDeletedFiles().size());
            }
        }
    }

    private void processDeletionCallback(BObject service, RemoteFileSystemEvent event,
                                        MethodType methodType, BObject caller) {
        Parameter[] params = methodType.getParameters();

        // Check first parameter type to determine method variant
        Type firstParamType = TypeUtils.getReferredType(params[0].type);
        boolean isArrayType = firstParamType.getTag() == TypeTags.ARRAY_TAG;

        if (isArrayType) {
            // onFileDeleted(string[] deletedFiles) - call once with all files
            processFileDeletedCallback(service, event, methodType, caller);
        } else {
            // onFileDelete(string deletedFile) - call once per file
            processFileDeleteCallback(service, event, methodType, caller);
        }
    }

    /**
     * Processes file deletion callback for onFileDelete method.
     * Calls the method once per deleted file with a single string parameter.
     */
    private void processFileDeleteCallback(BObject service, RemoteFileSystemEvent event,
                                          MethodType methodType, BObject caller) {
        List<String> deletedFilesList = event.getDeletedFiles();
        Parameter[] params = methodType.getParameters();

        // Call onFileDelete once per deleted file
        for (String deletedFile : deletedFilesList) {
            BString deletedFileBString = StringUtils.fromString(deletedFile);
            Object[] args = getOnFileDeleteMethodArguments(params, deletedFileBString, caller);
            if (args != null) {
                invokeOnFileDeleteAsync(service, args);
            }
        }
    }

    /**
     * Processes file deletion callback for onFileDeleted method (deprecated).
     */
    private void processFileDeletedCallback(BObject service, RemoteFileSystemEvent event,
                                           MethodType methodType, BObject caller) {
        List<String> deletedFilesList = event.getDeletedFiles();
        BString[] deletedFilesBStringArray = new BString[deletedFilesList.size()];
        for (int i = 0; i < deletedFilesList.size(); i++) {
            deletedFilesBStringArray[i] = StringUtils.fromString(deletedFilesList.get(i));
        }

        // Create string array for deleted files
        BArray deletedFilesArray = ValueCreator.createArrayValue(deletedFilesBStringArray);

        Parameter[] params = methodType.getParameters();
        Object[] args = getOnFileDeletedMethodArguments(params, deletedFilesArray, caller);
        if (args != null) {
            invokeOnFileDeletedAsync(service, args);
        }
    }

    /**
     * Processes metadata-only callbacks for the traditional onFileChange method.
     */
    private void processMetadataOnlyCallbacks(BObject service, RemoteFileSystemEvent event,
                                              MethodType methodType, BObject caller) {
        Map<String, Object> watchEventParamValues = processWatchEventParamValues(event);
        Parameter[] params = methodType.getParameters();
        Object[] args = getMethodArguments(params, watchEventParamValues, caller);
        if (args != null) {
            invokeMethodAsync(service, args);
        }
    }

    private Object[] getMethodArguments(Parameter[] params, Map<String, Object> watchEventParamValues,
                                        BObject caller) {
        if (params.length == 1) {
            return new Object[] {getWatchEvent(params[0], watchEventParamValues)};
        } else if (params.length == 2) {
            if ((params[0].type.isReadOnly() || TypeUtils.getReferredType(params[0].type).getTag() == RECORD_TYPE_TAG)
                    && TypeUtils.getReferredType(params[1].type).getTag() == OBJECT_TYPE_TAG) {
                return new Object[] {getWatchEvent(params[0], watchEventParamValues), caller};
            } else if ((params[1].type.isReadOnly() || TypeUtils.getReferredType(params[1].type).getTag() ==
                    RECORD_TYPE_TAG) && TypeUtils.getReferredType(params[0].type).getTag() == OBJECT_TYPE_TAG) {
                return new Object[] {caller, getWatchEvent(params[1], watchEventParamValues)};
            }
        }
        return null;
    }

    private Object[] getOnFileDeleteMethodArguments(Parameter[] params, BString deletedFile, BObject caller) {
        if (params.length == 1) {
            // Only deletedFile parameter
            return new Object[] {deletedFile};
        } else if (params.length == 2) {
            // deletedFile and caller parameters
            return new Object[] {deletedFile, caller};
        }
        return null;
    }

    private Object[] getOnFileDeletedMethodArguments(Parameter[] params, BArray deletedFiles, BObject caller) {
        if (params.length == 1) {
            // Only deletedFiles parameter
            return new Object[] {deletedFiles};
        } else if (params.length == 2) {
            // deletedFiles and caller parameters
            return new Object[] {deletedFiles, caller};
        }
        return null;
    }

    private void invokeOnFileDeleteAsync(BObject service, Object ...args) {
        Thread.startVirtualThread(() -> {
            try {
                ObjectType serviceType = (ObjectType) TypeUtils.getReferredType(TypeUtils.getType(service));
                boolean isConcurrentSafe = serviceType.isIsolated() &&
                        serviceType.isIsolated(ON_FILE_DELETE_REMOTE_FUNCTION);
                StrandMetadata strandMetadata = new StrandMetadata(isConcurrentSafe, null);
                Object result = runtime.callMethod(service, ON_FILE_DELETE_REMOTE_FUNCTION, strandMetadata, args);
                if (result instanceof BError) {
                    ((BError) result).printStackTrace();
                }
            } catch (BError error) {
                error.printStackTrace();
            }
        });
    }

    private void invokeOnFileDeletedAsync(BObject service, Object ...args) {
        Thread.startVirtualThread(() -> {
            try {
                ObjectType serviceType = (ObjectType) TypeUtils.getReferredType(TypeUtils.getType(service));
                boolean isConcurrentSafe = serviceType.isIsolated() &&
                        serviceType.isIsolated(ON_FILE_DELETED_REMOTE_FUNCTION);
                StrandMetadata strandMetadata = new StrandMetadata(isConcurrentSafe, null);
                Object result = runtime.callMethod(service, ON_FILE_DELETED_REMOTE_FUNCTION, strandMetadata, args);
                if (result instanceof BError) {
                    ((BError) result).printStackTrace();
                }
            } catch (BError error) {
                error.printStackTrace();
            }
        });
    }

    private void invokeMethodAsync(BObject service, Object ...args) {
        Thread.startVirtualThread(() -> {
            try {
                ObjectType serviceType = (ObjectType) TypeUtils.getReferredType(TypeUtils.getType(service));
                boolean isConcurrentSafe = serviceType.isIsolated() &&
                        serviceType.isIsolated(ON_FILE_CHANGE_REMOTE_FUNCTION);
                StrandMetadata strandMetadata = new StrandMetadata(isConcurrentSafe, null);
                Object result = runtime.callMethod(service, ON_FILE_CHANGE_REMOTE_FUNCTION, strandMetadata, args);
                if (result instanceof BError) {
                    ((BError) result).printStackTrace();
                }
            } catch (BError error) {
                error.printStackTrace();
            }
        });

    }

    private BMap<BString, Object> getWatchEvent(Parameter parameter, Map<String, Object> parameters) {
        List<Map<String, Object>> addedFileParamList = (List<Map<String, Object>>)
                parameters.get(FTP_WATCHEVENT_ADDED_FILES);
        BString[] deletedFileBStringArray = (BString[]) parameters.get(FTP_WATCHEVENT_DELETED_FILES);

        Object[] addedFileInfoArray = new Object[addedFileParamList.size()];
        if (readonlyWatchEventExists(parameter)) {
            for (int i = 0; i < addedFileParamList.size(); i++) {
                final BMap<BString, Object> fileInfo = ValueCreator.createReadonlyRecordValue(
                        new Module(FtpConstants.FTP_ORG_NAME,
                                FtpConstants.FTP_MODULE_NAME, FtpUtil.getFtpPackage().getMajorVersion()),
                                        FtpConstants.FTP_FILE_INFO, addedFileParamList.get(i));
                addedFileInfoArray[i] = fileInfo;
            }
            BArray addedFileInfoBArray = ValueCreator.createArrayValue(addedFileInfoArray,
                    TypeCreator.createArrayType(FtpUtil.getFileInfoType(), true));

            BArray deletedFileBArray = ValueCreator.createReadonlyArrayValue(deletedFileBStringArray);

            Map<String, Object> watchEventMap = new HashMap<>();
            watchEventMap.put(FTP_WATCHEVENT_ADDED_FILES, addedFileInfoBArray);
            watchEventMap.put(FTP_WATCHEVENT_DELETED_FILES, deletedFileBArray);
            return ValueCreator.createReadonlyRecordValue(
                new Module(FtpConstants.FTP_ORG_NAME, FtpConstants.FTP_MODULE_NAME,
                    FtpUtil.getFtpPackage().getMajorVersion()), FTP_SERVER_EVENT, watchEventMap);
        } else {
            for (int i = 0; i < addedFileParamList.size(); i++) {
                final BMap<BString, Object> fileInfo = ValueCreator.createRecordValue(
                        new Module(FtpConstants.FTP_ORG_NAME,
                                FtpConstants.FTP_MODULE_NAME, FtpUtil.getFtpPackage().getMajorVersion()),
                                        FtpConstants.FTP_FILE_INFO, addedFileParamList.get(i));
                addedFileInfoArray[i] = fileInfo;
            }
            BArray addedFileInfoBArray = ValueCreator.createArrayValue(addedFileInfoArray,
                TypeCreator.createArrayType(FtpUtil.getFileInfoType(), false));

            BArray deletedFileBArray = ValueCreator.createArrayValue(deletedFileBStringArray);

            Map<String, Object> watchEventMap = new HashMap<>();
            watchEventMap.put(FTP_WATCHEVENT_ADDED_FILES, addedFileInfoBArray);
            watchEventMap.put(FTP_WATCHEVENT_DELETED_FILES, deletedFileBArray);
            return ValueCreator.createRecordValue(new Module(FtpConstants.FTP_ORG_NAME, FtpConstants.FTP_MODULE_NAME,
                    FtpUtil.getFtpPackage().getMajorVersion()), FTP_SERVER_EVENT, watchEventMap);
        }
    }

    private boolean readonlyWatchEventExists(Parameter parameter) {
        if (parameter.type.isReadOnly() && ((IntersectionType) parameter.type)
                .getEffectiveType().getTag() == RECORD_TYPE_TAG) {
            return true;
        }
        return false;
    }

    private Map<String, Object> processWatchEventParamValues(RemoteFileSystemEvent fileSystemEvent) {
        List<FileInfo> addedFileList = fileSystemEvent.getAddedFiles();
        List<String> deletedFileList = fileSystemEvent.getDeletedFiles();

        List<Map<String, Object>> addedFilesParamsList = new ArrayList<>();
        for (int i = 0; i < addedFileList.size(); i++) {
            FileInfo info = addedFileList.get(i);
            Map<String, Object> fileInfoParams = new HashMap<>();
            fileInfoParams.put("path", info.getPath());
            fileInfoParams.put("size", info.getFileSize());
            fileInfoParams.put("lastModifiedTimestamp", info.getLastModifiedTime());
            fileInfoParams.put("name", info.getFileName().getBaseName());
            fileInfoParams.put("isFolder", info.isFolder());
            fileInfoParams.put("isFile", info.isFile());
            try {
                fileInfoParams.put("pathDecoded", info.getFileName().getPathDecoded());
            } catch (FileSystemException e) {
                fileInfoParams.put("pathDecoded", info.getPath());
            }
            fileInfoParams.put("extension", info.getFileName().getExtension());
            fileInfoParams.put("publicURIString", info.getPublicURIString());
            fileInfoParams.put("fileType", info.getFileType().getName());
            fileInfoParams.put("isAttached", info.isAttached());
            fileInfoParams.put("isContentOpen", info.isContentOpen());
            fileInfoParams.put("isExecutable", info.isExecutable());
            fileInfoParams.put("isHidden", info.isHidden());
            fileInfoParams.put("isReadable", info.isReadable());
            fileInfoParams.put("isWritable", info.isWritable());
            fileInfoParams.put("depth", info.getFileName().getDepth());
            fileInfoParams.put("scheme", info.getFileName().getScheme());
            fileInfoParams.put("uri", info.getUrl().getPath());
            fileInfoParams.put("rootURI", info.getFileName().getRootURI());
            fileInfoParams.put("friendlyURI", info.getFileName().getFriendlyURI());

            addedFilesParamsList.add(fileInfoParams);
        }

        BString[] deletedFilesBstringArray = new BString[deletedFileList.size()];
        for (int i = 0; i < deletedFileList.size(); i++) {
            deletedFilesBstringArray[i] = StringUtils.fromString(deletedFileList.get(i));
        }

        Map<String, Object> processedParamMap = new HashMap<>();
        processedParamMap.put(FTP_WATCHEVENT_ADDED_FILES, addedFilesParamsList);
        processedParamMap.put(FTP_WATCHEVENT_DELETED_FILES, deletedFilesBstringArray);
        return processedParamMap;
    }

    @Override
    public void onError(Throwable throwable) {
        log.error(throwable.getMessage(), throwable);
    }

    @Override
    public BError done() {
        Set<Map.Entry<String, ServiceContext>> serviceEntries = serviceContexts.entrySet();
        for (Map.Entry<String, ServiceContext> serviceEntry : serviceEntries) {
            BObject service = serviceEntry.getValue().getService();
            try {
                Object serverConnectorObject = service.getNativeData(FtpConstants.FTP_SERVER_CONNECTOR);
                if (serverConnectorObject instanceof RemoteFileSystemServerConnector) {
                    RemoteFileSystemServerConnector serverConnector
                            = (RemoteFileSystemServerConnector) serverConnectorObject;
                    Object stopError = serverConnector.stop();
                    if (stopError instanceof BError) {
                        return (BError) stopError;
                    }
                }
            } catch (RemoteFileSystemConnectorException e) {
                return FtpUtil.createError(e.getMessage(), findRootCause(e), Error.errorType());
            } finally {
                service.addNativeData(FtpConstants.FTP_SERVER_CONNECTOR, null);
            }
        }
        log.debug(FtpConstants.SUCCESSFULLY_FINISHED_THE_ACTION);
        return null;
    }

    /**
     * Registers a service context.
     *
     * @param context The service context
     * @return true if added successfully, false if path is already registered or service type is invalid
     */
    public boolean addServiceContext(ServiceContext context) {
        BObject service = context.getService();
        Type serviceType = TypeUtils.getType(service);
        if (service == null || serviceType == null || serviceType.getName() == null) {
            return false;
        }
        ServiceConfiguration config = context.getConfiguration();
        if (config != null) {
            String path = config.getPath();
            if (pathToServiceContext.putIfAbsent(path, context) != null) {
                return false;
            }
            usesServiceLevelConfig.set(true);
        }
        if (serviceContexts.putIfAbsent(serviceType.getName(), context) != null) {
            if (config != null) {
                pathToServiceContext.remove(config.getPath(), context);
                if (pathToServiceContext.isEmpty()) {
                    usesServiceLevelConfig.set(false);
                }
            }
            return false;
        }
        return true;
    }

    public void removeServiceContext(ServiceContext context) {
        BObject service = context.getService();
        Type serviceType = TypeUtils.getType(service);
        if (serviceType == null || serviceType.getName() == null) {
            return;
        }
        serviceContexts.remove(serviceType.getName(), context);
        ServiceConfiguration config = context.getConfiguration();
        if (config != null) {
            pathToServiceContext.remove(config.getPath(), context);
            if (pathToServiceContext.isEmpty()) {
                usesServiceLevelConfig.set(false);
            }
        }
    }

    /**
     * Gets the service configuration for a given path.
     *
     * @param path The monitored path
     * @return The service configuration, or null if not configured
     */
    public ServiceConfiguration getServiceConfiguration(String path) {
        ServiceContext context = pathToServiceContext.get(path);
        return context == null ? null : context.getConfiguration();
    }

    /**
     * Checks if any service uses service-level configuration.
     *
     * @return true if at least one service uses @ftp:ServiceConfig annotation
     */
    public boolean usesServiceLevelConfig() {
        return usesServiceLevelConfig.get();
    }

    /**
     * Gets the number of registered services.
     *
     * @return The count of registered services
     */
    public int getServiceCount() {
        return serviceContexts.size();
    }

    public Iterable<ServiceContext> getServiceContexts() {
        return serviceContexts.values();
    }

    /**
     * Gets all service configurations.
     *
     * @return Map of path to configuration
     */
    public Map<String, ServiceConfiguration> getServiceConfigurations() {
        Map<String, ServiceConfiguration> configs = new HashMap<>();
        for (Map.Entry<String, ServiceContext> entry : pathToServiceContext.entrySet()) {
            configs.put(entry.getKey(), entry.getValue().getConfiguration());
        }
        return configs;
    }

    public void setCaller(BObject caller) {
        this.caller = caller;
    }

    public BObject getCaller() {
        return caller;
    }

    public void setLegacyListenerPath(String legacyListenerPath) {
        this.legacyListenerPath = legacyListenerPath;
    }

    void cleanup() {
        serviceContexts.clear();
        usesServiceLevelConfig.set(false);
        pathToServiceContext.clear();
        caller = null;
        fileSystemManager = null;
        fileSystemOptions = null;
        legacyListenerPath = null;
    }
}
