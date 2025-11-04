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

import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.concurrent.StrandMetadata;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.types.ObjectType;
import io.ballerina.runtime.api.types.Parameter;
import io.ballerina.runtime.api.types.PredefinedTypes;
import io.ballerina.runtime.api.types.StreamType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
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

/**
 * Handles content-based callbacks for FTP listener.
 */
public class FtpContentCallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(FtpContentCallbackHandler.class);
    private final Runtime ballerinaRuntime;
    private final FileSystemManager fileSystemManager;
    private final FileSystemOptions fileSystemOptions;

    public FtpContentCallbackHandler(Runtime ballerinaRuntime, FileSystemManager fileSystemManager,
                                     FileSystemOptions fileSystemOptions) {
        this.ballerinaRuntime = ballerinaRuntime;
        this.fileSystemManager = fileSystemManager;
        this.fileSystemOptions = fileSystemOptions;
    }

    /**
     * Processes content callbacks for added files in the event.
     * Routes each file to the appropriate content handler based on file extension and annotations.
     */
    public void processContentCallbacks(BObject service, RemoteFileSystemEvent event,
                                        ContentMethodRouter router, BObject callerObject) {
        List<FileInfo> addedFiles = event.getAddedFiles();

        for (FileInfo fileInfo : addedFiles) {
            try {
                // Route file to appropriate method
                Optional<MethodType> methodTypeOpt = router.routeFile(fileInfo);

                if (methodTypeOpt.isEmpty()) {
                    log.warn("No content handler method found for file: {}. Skipping content processing.",
                            fileInfo.getPath());
                    continue;
                }

                MethodType methodType = methodTypeOpt.get();

                // Fetch file content
                byte[] fileContent = fetchFileContentFromRemote(fileInfo);

                // Convert content based on method signature
                Object convertedContent = convertFileContent(fileContent, methodType);

                // Prepare method arguments
                Object[] methodArguments = prepareContentMethodArguments(methodType, convertedContent,
                        fileInfo, callerObject);

                // Invoke method asynchronously
                invokeContentMethodAsync(service, methodType.getName(), methodArguments);

            } catch (Exception exception) {
                log.error("Failed to process file: " + fileInfo.getPath(), exception);
                // Continue processing other files even if one fails
            }
        }
    }

    /**
     * Fetches file content from the remote FTP/SFTP server.
     */
    private byte[] fetchFileContentFromRemote(FileInfo fileInfo) throws Exception {
        // Use the path from fileInfo which contains the full URI (e.g., ftp://host/path/file.txt)
        String fileUri = fileInfo.getPath();
        FileObject fileObject = null;
        InputStream inputStream = null;

        try {
            fileObject = fileSystemManager.resolveFile(fileUri, fileSystemOptions);
            inputStream = fileObject.getContent().getInputStream();
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

    /**
     * Converts file content to the appropriate Ballerina type based on the method signature.
     */
    private Object convertFileContent(byte[] fileContent, MethodType methodType) throws Exception {
        String methodName = methodType.getName();
        Parameter firstParameter = methodType.getParameters()[0];
        Type firstParamType = TypeUtils.getReferredType(firstParameter.type);
        int firstParamTypeTag = firstParamType.getTag();

        switch (methodName) {
            case ON_FILE_REMOTE_FUNCTION:
                return convertOnFileContent(fileContent, firstParamTypeTag);
            case ON_FILE_TEXT_REMOTE_FUNCTION:
                return FtpContentConverter.convertBytesToString(fileContent);
            case ON_FILE_JSON_REMOTE_FUNCTION:
                return FtpContentConverter.convertBytesToJson(fileContent, firstParamType);
            case ON_FILE_XML_REMOTE_FUNCTION:
                return FtpContentConverter.convertBytesToXml(fileContent, firstParamType);
            case ON_FILE_CSV_REMOTE_FUNCTION:
                return FtpContentConverter.convertBytesToCsv(fileContent, firstParamType);
            default:
                throw new IllegalArgumentException("Unknown content method: " + methodName);
        }
    }

    /**
     * Converts content for onFile method (byte[] or stream).
     */
    private Object convertOnFileContent(byte[] fileContent, int firstParamTypeTag) {
        if (firstParamTypeTag == ARRAY_TAG) {
            // Return as byte[]
            return FtpContentConverter.convertToBallerinaByteArray(fileContent);
        }
        // Return as stream<byte[], error?>
        // Create a byte stream from the content
        return createByteStreamFromContent(fileContent);
    }


    /**
     * Creates a Ballerina byte stream from byte array content.
     *
     * @param content The byte array content
     * @return Ballerina stream object of type stream<byte[], error?>
     */
    private Object createByteStreamFromContent(byte[] content) {
        try {
            // Create the native iterator that will provide chunks of bytes
            ContentByteStreamIterator iterator = new ContentByteStreamIterator(content);

            // Get the ContentByteStream type from the FTP module
            Module ftpModule = new Module(FtpConstants.FTP_ORG_NAME, FtpConstants.FTP_MODULE_NAME,
                    FtpUtil.getFtpPackage().getMajorVersion());

            // Create a ContentByteStream object with the iterator handle
            BObject contentByteStreamObject = ValueCreator.createObjectValue(
                    ftpModule,
                    "ContentByteStream",
                    iterator
            );

            // Create the stream type: stream<byte[], error?>
            ArrayType byteArrayType = TypeCreator.createArrayType(PredefinedTypes.TYPE_BYTE);
            StreamType streamType = TypeCreator.createStreamType(byteArrayType, PredefinedTypes.TYPE_ERROR);

            // Create and return the stream object
            return ValueCreator.createStreamValue(streamType, contentByteStreamObject);

        } catch (Exception e) {
            log.error("Failed to create byte stream from content", e);
            // Fallback to returning byte array if stream creation fails
            return FtpContentConverter.convertToBallerinaByteArray(content);
        }
    }

    /**
     * Prepares method arguments for content handler methods.
     */
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

    /**
     * Creates a Ballerina FileInfo record from Java FileInfo object.
     */
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

    /**
     * Invokes the content handler method asynchronously.
     */
    private void invokeContentMethodAsync(BObject service, String methodName, Object[] methodArguments) {
        Thread.startVirtualThread(() -> {
            try {
                ObjectType serviceType = (ObjectType) TypeUtils.getReferredType(TypeUtils.getType(service));
                boolean isConcurrentSafe = serviceType.isIsolated() && serviceType.isIsolated(methodName);
                StrandMetadata strandMetadata = new StrandMetadata(isConcurrentSafe, null);

                Object result = ballerinaRuntime.callMethod(service, methodName, strandMetadata, methodArguments);

                if (result instanceof BError) {
                    ((BError) result).printStackTrace();
                }
            } catch (BError error) {
                error.printStackTrace();
            } catch (Exception exception) {
                log.error("Error invoking content method: " + methodName, exception);
            }
        });
    }
}
