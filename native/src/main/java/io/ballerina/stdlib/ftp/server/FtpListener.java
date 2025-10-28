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
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
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

import static io.ballerina.runtime.api.types.TypeTags.OBJECT_TYPE_TAG;
import static io.ballerina.runtime.api.types.TypeTags.RECORD_TYPE_TAG;
import static io.ballerina.stdlib.ftp.util.FtpConstants.FTP_SERVER_EVENT;
import static io.ballerina.stdlib.ftp.util.FtpConstants.FTP_WATCHEVENT_ADDED_FILES;
import static io.ballerina.stdlib.ftp.util.FtpConstants.FTP_WATCHEVENT_DELETED_FILES;
import static io.ballerina.stdlib.ftp.util.FtpConstants.ON_FILE_CHANGE_REMOTE_FUNCTION;
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
    private Map<String, BObject> registeredServices = new HashMap<>();
    private BObject caller;
    private FileSystemManager fileSystemManager;
    private FileSystemOptions fileSystemOptions;

    FtpListener(Runtime runtime) {
        this.runtime = runtime;
    }

    public void setFileSystemManager(FileSystemManager fileSystemManager) {
        this.fileSystemManager = fileSystemManager;
    }

    public void setFileSystemOptions(FileSystemOptions fileSystemOptions) {
        this.fileSystemOptions = fileSystemOptions;
    }

    @Override
    public boolean onMessage(RemoteFileSystemBaseMessage remoteFileSystemBaseMessage) {
        if (remoteFileSystemBaseMessage instanceof RemoteFileSystemEvent) {
            RemoteFileSystemEvent event = (RemoteFileSystemEvent) remoteFileSystemBaseMessage;

            if (runtime != null) {
                for (BObject service : registeredServices.values()) {
                    // Create router to handle content method selection
                    ContentMethodRouter router = new ContentMethodRouter(service);

                    // Check if any content handler methods are available
                    if (router.hasContentMethods()) {
                        // Process content-based callbacks with routing
                        processContentBasedCallbacks(service, event, router);
                    } else {
                        // Fall back to traditional onFileChange
                        Optional<MethodType> onFileChangeMethodType = getOnFileChangeMethod(service);
                        if (onFileChangeMethodType.isPresent()) {
                            processMetadataOnlyCallbacks(service, event, onFileChangeMethodType.get());
                        } else {
                            log.error("No valid remote method found in service");
                        }
                    }
                }
            } else {
                log.error("Runtime should not be null.");
            }
        }
        return true;
    }

    /**
     * Processes content-based callbacks for the new content listener methods.
     * Uses ContentMethodRouter to dispatch files to appropriate handlers.
     * Also handles file deletion events via onFileDeleted method if available.
     */
    private void processContentBasedCallbacks(BObject service, RemoteFileSystemEvent event,
                                              ContentMethodRouter router) {
        // Process added files with content methods
        if (!event.getAddedFiles().isEmpty()) {
            if (fileSystemManager == null || fileSystemOptions == null) {
                log.error("FileSystemManager or FileSystemOptions not initialized for content callbacks. " +
                        "Content methods require proper FileSystem initialization. Skipping added files processing.");
            } else {
                try {
                    FtpContentCallbackHandler contentHandler = new FtpContentCallbackHandler(
                            runtime, fileSystemManager, fileSystemOptions);
                    contentHandler.processContentCallbacks(service, event, router, caller);
                } catch (Exception e) {
                    log.error("Error in content callback processing for added files", e);
                }
            }
        }

        // Process deleted files with onFileDeleted method if available
        if (!event.getDeletedFiles().isEmpty()) {
            Optional<MethodType> onFileDeletedMethodType = getOnFileDeletedMethod(service);
            if (onFileDeletedMethodType.isPresent()) {
                processFileDeletedCallback(service, event, onFileDeletedMethodType.get());
            } else {
                log.debug("No onFileDeleted method found. Skipping deletion event processing for {} deleted files.",
                        event.getDeletedFiles().size());
            }
        }
    }

    /**
     * Processes file deletion callback for onFileDeleted method.
     */
    private void processFileDeletedCallback(BObject service, RemoteFileSystemEvent event,
                                           MethodType methodType) {
        List<String> deletedFilesList = event.getDeletedFiles();
        BString[] deletedFilesBStringArray = new BString[deletedFilesList.size()];
        for (int i = 0; i < deletedFilesList.size(); i++) {
            deletedFilesBStringArray[i] = StringUtils.fromString(deletedFilesList.get(i));
        }

        // Create string array for deleted files
        BArray deletedFilesArray = ValueCreator.createArrayValue(deletedFilesBStringArray);

        Parameter[] params = methodType.getParameters();
        Object[] args = getOnFileDeletedMethodArguments(params, deletedFilesArray);
        if (args != null) {
            invokeOnFileDeletedAsync(service, args);
        }
    }

    /**
     * Processes metadata-only callbacks for the traditional onFileChange method.
     */
    private void processMetadataOnlyCallbacks(BObject service, RemoteFileSystemEvent event,
                                              MethodType methodType) {
        Map<String, Object> watchEventParamValues = processWatchEventParamValues(event);
        Parameter[] params = methodType.getParameters();
        Object[] args = getMethodArguments(params, watchEventParamValues);
        if (args != null) {
            invokeMethodAsync(service, args);
        }
    }

    private Object[] getMethodArguments(Parameter[] params, Map<String, Object> watchEventParamValues) {
        if (params.length == 1) {
            return new Object[] {getWatchEvent(params[0], watchEventParamValues)};
        } else if (params.length == 2) {
            if ((params[0].type.isReadOnly() || TypeUtils.getReferredType(params[0].type).getTag() == RECORD_TYPE_TAG)
                    && TypeUtils.getReferredType(params[1].type).getTag() == OBJECT_TYPE_TAG) {
                return new Object[] {getWatchEvent(params[0], watchEventParamValues), caller};
            } else if ((params[1].type.isReadOnly() || TypeUtils.getReferredType(params[1].type).getTag() ==
                    RECORD_TYPE_TAG) && TypeUtils.getReferredType(params[0].type).getTag() == OBJECT_TYPE_TAG) {
                return new Object[] {caller, getWatchEvent(params[1], watchEventParamValues)};
            } else {
                log.error("Invalid parameter types in onFileChange method");
            }
        } else {
            log.error("Invalid parameter count in onFileChange method");
        }
        return null;
    }

    private Object[] getOnFileDeletedMethodArguments(Parameter[] params, BArray deletedFiles) {
        if (params.length == 1) {
            // Only deletedFiles parameter
            return new Object[] {deletedFiles};
        } else if (params.length == 2) {
            // deletedFiles and caller parameters
            return new Object[] {deletedFiles, caller};
        } else {
            log.error("Invalid parameter count in onFileDeleted method");
        }
        return null;
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
        Set<Map.Entry<String, BObject>> serviceEntries = registeredServices.entrySet();
        for (Map.Entry<String, BObject> serviceEntry : serviceEntries) {
            BObject service = serviceEntry.getValue();
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

    protected void addService(BObject service) {
        Type serviceType = TypeUtils.getType(service);
        if (service != null && serviceType != null && serviceType.getName() != null) {
            registeredServices.put(serviceType.getName(), service);
        }
    }

    public void setCaller(BObject caller) {
        this.caller = caller;
    }

    public BObject getCaller() {
        return caller;
    }
}
