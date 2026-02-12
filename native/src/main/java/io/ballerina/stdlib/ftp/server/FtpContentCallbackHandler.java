/*
 * Copyright (c) 2025 WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.ftp.server;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.concurrent.StrandMetadata;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.types.ObjectType;
import io.ballerina.runtime.api.types.Parameter;
import io.ballerina.runtime.api.types.StreamType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.types.TypeTags;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.ftp.ContentByteStreamIteratorUtils;
import io.ballerina.stdlib.ftp.ContentCsvStreamIteratorUtils;
import io.ballerina.stdlib.ftp.transport.message.FileInfo;
import io.ballerina.stdlib.ftp.transport.message.RemoteFileSystemEvent;
import io.ballerina.stdlib.ftp.util.FtpConstants;
import io.ballerina.stdlib.ftp.util.FtpContentConverter;
import io.ballerina.stdlib.ftp.util.FtpUtil;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.ballerina.runtime.api.types.TypeTags.ARRAY_TAG;
import static io.ballerina.runtime.api.types.TypeTags.OBJECT_TYPE_TAG;
import static io.ballerina.runtime.api.types.TypeTags.RECORD_TYPE_TAG;
import static io.ballerina.stdlib.ftp.util.FtpConstants.ON_FILE_CSV_REMOTE_FUNCTION;
import static io.ballerina.stdlib.ftp.util.FtpConstants.ON_FILE_JSON_REMOTE_FUNCTION;
import static io.ballerina.stdlib.ftp.util.FtpConstants.ON_FILE_REMOTE_FUNCTION;
import static io.ballerina.stdlib.ftp.util.FtpConstants.ON_FILE_TEXT_REMOTE_FUNCTION;
import static io.ballerina.stdlib.ftp.util.FtpConstants.ON_FILE_XML_REMOTE_FUNCTION;
import static io.ballerina.stdlib.ftp.util.FtpContentConverter.convertBytesToCsv;
import static io.ballerina.stdlib.ftp.util.FtpContentConverter.convertBytesToJson;
import static io.ballerina.stdlib.ftp.util.FtpContentConverter.convertBytesToString;
import static io.ballerina.stdlib.ftp.util.FtpContentConverter.convertBytesToXml;
import static io.ballerina.stdlib.ftp.util.FtpContentConverter.convertToBallerinaByteArray;
import static io.ballerina.stdlib.ftp.util.FtpContentConverter.deriveFileNamePrefix;

/**
 * Handles content-based callbacks for FTP listener.
 */
public class FtpContentCallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(FtpContentCallbackHandler.class);
    private final Runtime ballerinaRuntime;
    private final FileSystemManager fileSystemManager;
    private final FileSystemOptions fileSystemOptions;
    private final boolean laxDataBinding;
    private final BMap<?, ?> csvFailSafe;

    public FtpContentCallbackHandler(Runtime ballerinaRuntime, FileSystemManager fileSystemManager,
                                     FileSystemOptions fileSystemOptions, boolean laxDataBinding,
                                     BMap<?, ?> csvFailSafe) {
        this.ballerinaRuntime = ballerinaRuntime;
        this.fileSystemManager = fileSystemManager;
        this.fileSystemOptions = fileSystemOptions;
        this.laxDataBinding = laxDataBinding;
        this.csvFailSafe = csvFailSafe;
    }

    /**
     * Processes content callbacks for added files in the event.
     * Routes each file to the appropriate content handler based on file extension and annotations.
     */
    public void processContentCallbacks(Environment env, BObject service, RemoteFileSystemEvent event,
                                        FormatMethodsHolder holder, BObject callerObject) {
        List<FileInfo> addedFiles = event.getAddedFiles();
        String listenerPath = event.getSourcePath();

        for (FileInfo fileInfo : addedFiles) {
            try {
                // Route file to appropriate method
                Optional<MethodType> methodTypeOpt = holder.getMethod(fileInfo);

                if (methodTypeOpt.isEmpty()) {
                    log.warn("No content handler method found for file: {}. Skipping content processing.",
                            fileInfo.getPath());
                    continue;
                }

                String fileUri = fileInfo.getPath();
                FileObject fileObject = fileSystemManager.resolveFile(fileUri, fileSystemOptions);
                InputStream inputStream = fileObject.getContent().getInputStream();

                // Convert content based on method signature
                MethodType methodType = methodTypeOpt.get();
                Object convertedContent = convertFileContent(env, fileObject, inputStream, methodType);

                // Check if content conversion returned an error (ContentBindingError)
                if (convertedContent instanceof BError bError) {
                    log.error("Content binding failed for file: {}", fileInfo.getPath());
                    routeToOnError(service, holder, bError, callerObject, fileInfo, listenerPath);
                    continue;
                }

                // Prepare method arguments
                Object[] methodArguments = prepareContentMethodArguments(methodType, convertedContent,
                        fileInfo, callerObject);

                // Get post-processing actions
                Optional<PostProcessAction> afterProcess = holder.getAfterProcessAction(methodType.getName());
                Optional<PostProcessAction> afterError = holder.getAfterErrorAction(methodType.getName());

                // Invoke method asynchronously with post-processing
                invokeContentMethodAsync(service, methodType.getName(), methodArguments,
                        fileInfo, callerObject, listenerPath, afterProcess, afterError);

            } catch (Exception exception) {
                log.error("Failed to process file: " + fileInfo.getPath(), exception);
                // Continue processing other files even if one fails
            }
        }
    }

    private byte[] fetchAllFileContentFromRemote(FileObject fileObject, InputStream inputStream) throws Exception {
        try {
            return FtpContentConverter.convertInputStreamToByteArray(inputStream);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    log.warn("Failed to close input stream", e);
                }
            }
            if (fileObject != null) {
                try {
                    fileObject.close();
                } catch (Exception e) {
                    log.warn("Failed to close file object", e);
                }
            }
        }
    }

    private Object convertFileContent(Environment environment, FileObject fileObject, InputStream inputStream,
                                      MethodType methodType)
            throws Exception {
        String methodName = methodType.getName();
        Parameter firstParameter = methodType.getParameters()[0];
        Type firstParamType = TypeUtils.getReferredType(firstParameter.type);

        if (firstParamType.getTag() == TypeTags.STREAM_TAG) {
            Type constrainedType = ((StreamType) firstParamType).getConstrainedType();
            switch (methodName) {
                case ON_FILE_REMOTE_FUNCTION -> {
                    return ContentByteStreamIteratorUtils.createStream(inputStream,
                            constrainedType, laxDataBinding, fileObject);
                }
                case ON_FILE_CSV_REMOTE_FUNCTION -> {
                    if (constrainedType.getTag() == ARRAY_TAG) {
                        return ContentCsvStreamIteratorUtils.createStringArrayStream(inputStream,
                                constrainedType, laxDataBinding, fileObject);
                    }
                    return ContentCsvStreamIteratorUtils.createRecordStream(inputStream,
                            constrainedType, laxDataBinding, fileObject);
                }
                default -> throw new IllegalArgumentException("Unknown content method: " + methodName);
            }
        } else {
            byte[] fileContent = fetchAllFileContentFromRemote(fileObject, inputStream);
            String fileNamePrefix = deriveFileNamePrefix(fileObject);
            String filePath = fileObject.getName().getURI();
            return switch (methodName) {
                case ON_FILE_REMOTE_FUNCTION -> convertToBallerinaByteArray(fileContent);
                case ON_FILE_TEXT_REMOTE_FUNCTION -> convertBytesToString(fileContent);
                case ON_FILE_JSON_REMOTE_FUNCTION -> convertBytesToJson(fileContent, firstParamType, laxDataBinding,
                        filePath);
                case ON_FILE_XML_REMOTE_FUNCTION -> convertBytesToXml(fileContent, firstParamType, laxDataBinding,
                        filePath);
                case ON_FILE_CSV_REMOTE_FUNCTION -> convertBytesToCsv(environment, fileContent, firstParamType,
                        laxDataBinding, csvFailSafe, fileNamePrefix, filePath);
                default -> throw new IllegalArgumentException("Unknown content method: " + methodName);
            };
        }
    }

    private Object[] prepareContentMethodArguments(MethodType methodType, Object convertedContent,
                                                   FileInfo fileInfo, BObject callerObject) {
        Parameter[] parameters = methodType.getParameters();

        if (parameters.length == 1) {
            return new Object[]{convertedContent};
        } else if (parameters.length == 2) {
            // Second parameter is either FileInfo or Caller
            int secondParamTypeTag = TypeUtils.getReferredType(parameters[1].type).getTag();

            if (secondParamTypeTag == RECORD_TYPE_TAG) {
                // FileInfo parameter
                return new Object[]{convertedContent, createFileInfoRecord(fileInfo)};
            } else if (secondParamTypeTag == OBJECT_TYPE_TAG) {
                // Caller parameter
                return new Object[]{convertedContent, callerObject};
            }
        } else if (parameters.length == 3) {
            // All three parameters: content, FileInfo, Caller
            return new Object[]{convertedContent, createFileInfoRecord(fileInfo), callerObject};
        }

        // Default: only content
        return new Object[]{convertedContent};
    }

    private BMap<BString, Object> createFileInfoRecord(FileInfo fileInfo) {
        Map<String, Object> fileInfoParams = new HashMap<>();
        fileInfoParams.put("path", fileInfo.getPath());
        fileInfoParams.put("size", fileInfo.getFileSize());
        fileInfoParams.put("lastModifiedTimestamp", fileInfo.getLastModifiedTime());
        fileInfoParams.put("name", fileInfo.getFileName().getBaseName());
        fileInfoParams.put("isFolder", fileInfo.isFolder());
        fileInfoParams.put("isFile", fileInfo.isFile());

        try {
            fileInfoParams.put("pathDecoded", fileInfo.getFileName().getPathDecoded());
        } catch (Exception e) {
            fileInfoParams.put("pathDecoded", fileInfo.getPath());
        }

        fileInfoParams.put("extension", fileInfo.getFileName().getExtension());
        fileInfoParams.put("publicURIString", fileInfo.getPublicURIString());
        fileInfoParams.put("fileType", fileInfo.getFileType().getName());
        fileInfoParams.put("isAttached", fileInfo.isAttached());
        fileInfoParams.put("isContentOpen", fileInfo.isContentOpen());
        fileInfoParams.put("isExecutable", fileInfo.isExecutable());
        fileInfoParams.put("isHidden", fileInfo.isHidden());
        fileInfoParams.put("isReadable", fileInfo.isReadable());
        fileInfoParams.put("isWritable", fileInfo.isWritable());
        fileInfoParams.put("depth", fileInfo.getFileName().getDepth());
        fileInfoParams.put("scheme", fileInfo.getFileName().getScheme());
        fileInfoParams.put("uri", fileInfo.getUrl().getPath());
        fileInfoParams.put("rootURI", fileInfo.getFileName().getRootURI());
        fileInfoParams.put("friendlyURI", fileInfo.getFileName().getFriendlyURI());

        return ValueCreator.createRecordValue(
                new Module(FtpConstants.FTP_ORG_NAME, FtpConstants.FTP_MODULE_NAME,
                        FtpUtil.getFtpPackage().getMajorVersion()),
                FtpConstants.FTP_FILE_INFO,
                fileInfoParams
        );
    }

    private void routeToOnError(BObject service, FormatMethodsHolder holder, BError error, BObject callerObject,
                                FileInfo fileInfo, String listenerPath) {
        if (!holder.hasOnErrorMethod()) {
            // No onError handler, error is already logged
            return;
        }

        Optional<MethodType> onErrorMethodOpt = holder.getOnErrorMethod();
        if (onErrorMethodOpt.isEmpty()) {
            return;
        }

        MethodType onErrorMethod = onErrorMethodOpt.get();

        Optional<PostProcessAction> onErrorAfterProcess = holder.getAfterProcessAction(onErrorMethod.getName());
        Optional<PostProcessAction> onErrorAfterError = holder.getAfterErrorAction(onErrorMethod.getName());
        boolean hasOnErrorActions = onErrorAfterProcess.isPresent() || onErrorAfterError.isPresent();

        // Prepare arguments for onError method
        Object[] methodArguments = prepareOnErrorMethodArguments(onErrorMethod, error, callerObject);

        // Invoke onError asynchronously and apply afterProcess/afterError actions
        invokeOnErrorMethodAsync(service, onErrorMethod.getName(), methodArguments, fileInfo, callerObject,
                listenerPath,
                hasOnErrorActions ? onErrorAfterProcess : Optional.empty(),
                hasOnErrorActions ? onErrorAfterError : Optional.empty());
    }

    private Object[] prepareOnErrorMethodArguments(MethodType methodType, BError error, BObject callerObject) {
        Parameter[] parameters = methodType.getParameters();

        if (parameters.length == 2) {
            // Two parameters: error, Caller
            return new Object[]{error, callerObject};
        }

        // Default: only error
        return new Object[]{error};
    }

    private void invokeOnErrorMethodAsync(BObject service, String methodName, Object[] methodArguments,
                                          FileInfo fileInfo, BObject callerObject, String listenerPath,
                                          Optional<PostProcessAction> afterProcess,
                                          Optional<PostProcessAction> afterError) {
        Thread.startVirtualThread(() -> {
            boolean isSuccess = false;
            try {
                ObjectType serviceType = (ObjectType) TypeUtils.getReferredType(TypeUtils.getType(service));
                boolean isConcurrentSafe = serviceType.isIsolated() && serviceType.isIsolated(methodName);
                StrandMetadata strandMetadata = new StrandMetadata(isConcurrentSafe, null);

                Object result = ballerinaRuntime.callMethod(service, methodName, strandMetadata, methodArguments);
                if (result instanceof BError) {
                    ((BError) result).printStackTrace();
                } else {
                    isSuccess = true;
                }
            } catch (BError error) {
                error.printStackTrace();
            } catch (Exception exception) {
                log.error("Error invoking onError method: " + methodName, exception);
            }

            if (isSuccess) {
                afterProcess.ifPresent(action -> executePostProcessAction(action, fileInfo, callerObject,
                        listenerPath, "afterProcess"));
            } else {
                afterError.ifPresent(action -> executePostProcessAction(action, fileInfo, callerObject,
                        listenerPath, "afterError"));
            }
        });
    }

    private void invokeContentMethodAsync(BObject service, String methodName, Object[] methodArguments,
                                          FileInfo fileInfo, BObject callerObject, String listenerPath,
                                          Optional<PostProcessAction> afterProcess,
                                          Optional<PostProcessAction> afterError) {
        Thread.startVirtualThread(() -> {
            boolean isSuccess = false;
            try {
                ObjectType serviceType = (ObjectType) TypeUtils.getReferredType(TypeUtils.getType(service));
                boolean isConcurrentSafe = serviceType.isIsolated() && serviceType.isIsolated(methodName);
                StrandMetadata strandMetadata = new StrandMetadata(isConcurrentSafe, null);

                Object result = ballerinaRuntime.callMethod(service, methodName, strandMetadata, methodArguments);

                if (result instanceof BError) {
                    ((BError) result).printStackTrace();
                    // Method returned an error - execute afterError action
                    afterError.ifPresent(action -> executePostProcessAction(action, fileInfo, callerObject,
                            listenerPath, "afterError"));
                } else {
                    isSuccess = true;
                }
            } catch (BError error) {
                error.printStackTrace();
                // Method threw an error - execute afterError action
                afterError.ifPresent(action -> executePostProcessAction(action, fileInfo, callerObject,
                        listenerPath, "afterError"));
            } catch (Exception exception) {
                log.error("Error invoking content method: " + methodName, exception);
                // Method threw an exception - execute afterError action
                afterError.ifPresent(action -> executePostProcessAction(action, fileInfo, callerObject,
                        listenerPath, "afterError"));
            }

            // Execute afterProcess action on success
            if (isSuccess) {
                afterProcess.ifPresent(action -> executePostProcessAction(action, fileInfo, callerObject,
                        listenerPath, "afterProcess"));
            }
        });
    }

    private void executePostProcessAction(PostProcessAction action, FileInfo fileInfo, BObject callerObject,
                                          String listenerPath, String actionContext) {

        String filePath;
        try {
            filePath = fileInfo.getFileName().getPathDecoded();
        } catch (Exception e) {
            log.warn("Cannot execute {} action: Failed to retrieve file path from FileInfo: {}", actionContext, 
            e.getMessage());
            return;
        }

        try {
            if (action.isDelete()) {
                executeDeleteAction(callerObject, filePath, actionContext);
            } else if (action.isMove()) {
                executeMoveAction(callerObject, filePath, listenerPath, action, actionContext);
            }
        } catch (Exception e) {
            log.error("Failed to execute {} action on file: {}", actionContext, filePath, e);
        }
    }

    private void executeDeleteAction(BObject callerObject, String filePath, String actionContext) {
        try {
            BObject clientObj = callerObject.getObjectValue(StringUtils.fromString("client"));
            StrandMetadata strandMetadata = new StrandMetadata(true, null);
            Object result = ballerinaRuntime.callMethod(clientObj, "delete", strandMetadata,
                    StringUtils.fromString(filePath));

            if (result instanceof BError) {
                log.error("Failed to delete file during {}: {} - {}", actionContext, filePath,
                        ((BError) result).getErrorMessage());
            } else {
                log.debug("Successfully deleted file during {}: {}", actionContext, filePath);
            }
        } catch (Exception e) {
            log.error("Exception during delete action ({}): {}", actionContext, filePath, e);
        }
    }

    private void executeMoveAction(BObject callerObject, String filePath, String listenerPath,
                                   PostProcessAction action, String actionContext) {
        try {
            String destinationPath = calculateMoveDestination(filePath, listenerPath, action);

            BObject clientObj = callerObject.getObjectValue(StringUtils.fromString("client"));
            StrandMetadata strandMetadata = new StrandMetadata(true, null);
            Object result = ballerinaRuntime.callMethod(clientObj, "move", strandMetadata,
                    StringUtils.fromString(filePath), StringUtils.fromString(destinationPath));

            if (result instanceof BError) {
                log.error("Failed to move file during {}: {} -> {} - {}", actionContext, filePath,
                        destinationPath, ((BError) result).getErrorMessage());
            } else {
                log.debug("Successfully moved file during {}: {} -> {}", actionContext, filePath, destinationPath);
            }
        } catch (Exception e) {
            log.error("Exception during move action ({}): {}", actionContext, filePath, e);
        }
    }

    private String calculateMoveDestination(String filePath, String listenerPath, PostProcessAction action) {
        String moveTo = action.getMoveTo();
        String normalizedFilePath = normalizeFilePath(filePath);
        String fileName = extractFileName(normalizedFilePath);
        if (fileName.isEmpty()) {
            return ensureTrailingSlash(moveTo);
        }

        if (!action.isPreserveSubDirs() || listenerPath == null || listenerPath.isEmpty()) {
            // Simple case: just append filename to moveTo directory
            return ensureTrailingSlash(moveTo) + fileName;
        }

        // Calculate relative path from listener root
        String normalizedListenerPath = ensureTrailingSlash(listenerPath);

        if (normalizedFilePath.startsWith(normalizedListenerPath)) {
            // Extract relative path including subdirectories
            String relativePath = normalizedFilePath.substring(normalizedListenerPath.length());
            if (relativePath.isEmpty()) {
                return ensureTrailingSlash(moveTo) + fileName;
            }
            return ensureTrailingSlash(moveTo) + relativePath;
        } else {
            // File path doesn't start with listener path, fall back to simple append
            return ensureTrailingSlash(moveTo) + fileName;
        }
    }

    private String ensureTrailingSlash(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        return path.endsWith("/") ? path : path + "/";
    }

    private String normalizeFilePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        int end = path.length();
        while (end > 1 && path.charAt(end - 1) == '/') {
            end--;
        }
        return path.substring(0, end);
    }

    private String extractFileName(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        int lastSlashIndex = path.lastIndexOf('/');
        return lastSlashIndex >= 0 ? path.substring(lastSlashIndex + 1) : path;
    }
}
