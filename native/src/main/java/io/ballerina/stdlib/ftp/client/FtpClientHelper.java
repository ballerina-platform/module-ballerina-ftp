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

package io.ballerina.stdlib.ftp.client;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BStream;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.ftp.exception.BallerinaFtpException;
import io.ballerina.stdlib.ftp.transport.message.FileInfo;
import io.ballerina.stdlib.ftp.transport.message.RemoteFileSystemBaseMessage;
import io.ballerina.stdlib.ftp.transport.message.RemoteFileSystemMessage;
import io.ballerina.stdlib.ftp.util.BufferHolder;
import io.ballerina.stdlib.ftp.util.FtpConstants;
import io.ballerina.stdlib.ftp.util.FtpUtil;
import io.ballerina.stdlib.io.channels.base.Channel;
import org.apache.commons.vfs2.FileSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static io.ballerina.stdlib.ftp.util.FtpConstants.ARRAY_SIZE;
import static io.ballerina.stdlib.ftp.util.FtpConstants.BYTE_STREAM_CLOSE_FUNC;
import static io.ballerina.stdlib.ftp.util.FtpConstants.BYTE_STREAM_NEXT_FUNC;
import static io.ballerina.stdlib.ftp.util.FtpConstants.ENTITY_BYTE_STREAM;
import static io.ballerina.stdlib.ftp.util.FtpConstants.FIELD_VALUE;
import static io.ballerina.stdlib.ftp.util.FtpConstants.READ_INPUT_STREAM;
import static io.ballerina.stdlib.ftp.util.FtpConstants.STREAM_ENTRY_RECORD;
import static io.ballerina.stdlib.ftp.util.FtpUtil.getFtpPackage;


/**
 * Contains helper methods to invoke FTP actions.
 */
class FtpClientHelper {

    private static final Logger log = LoggerFactory.getLogger(FtpClientHelper.class);

    private FtpClientHelper() {
        // private constructor
    }

    static boolean executeGenericAction() {
        return true;
    }

    static boolean executeGetAction(RemoteFileSystemBaseMessage remoteFileSystemBaseMessage,
                                    CompletableFuture<Object> balFuture, BObject clientConnector) {
        try {
            if (remoteFileSystemBaseMessage instanceof RemoteFileSystemMessage) {
                final InputStream in = ((RemoteFileSystemMessage) remoteFileSystemBaseMessage).getInputStream();
                ByteChannel byteChannel = new FtpByteChannel(in);
                Channel channel = new FtpChannel(byteChannel);
                InputStream inputStream = channel.getInputStream();
                clientConnector.addNativeData(READ_INPUT_STREAM, inputStream);
                BMap<BString, Object> streamEntry = generateInputStreamEntry(inputStream);
                clientConnector.addNativeData(ENTITY_BYTE_STREAM, streamEntry);
                balFuture.complete(streamEntry);
            }
        } catch (IOException e) {
            log.error("Error occurred while reading stream: ", e);
        }
        return true;
    }

    public static BMap<BString, Object> generateInputStreamEntry(InputStream inputStream) {
        BMap<BString, Object> streamEntry = ValueCreator.createRecordValue(getFtpPackage(), STREAM_ENTRY_RECORD);
        try {
            byte[] buffer = new byte[ARRAY_SIZE];
            int readNumber = inputStream.read(buffer);
            if (readNumber == -1) {
                inputStream.close();
                streamEntry.addNativeData(READ_INPUT_STREAM, null);
                return null;
            }
            byte[] returnArray;
            if (readNumber < ARRAY_SIZE) {
                returnArray = Arrays.copyOfRange(buffer, 0, readNumber);
            } else {
                returnArray = buffer;
            }
            streamEntry.put(FIELD_VALUE, ValueCreator.createArrayValue(returnArray));
        } catch (IOException e) {
            log.error("Error occurred while reading stream: ", e);
        }
        return streamEntry;
    }

    static boolean executeIsDirectoryAction(RemoteFileSystemBaseMessage remoteFileSystemBaseMessage,
                                            CompletableFuture<Object> balFuture) {
        if (remoteFileSystemBaseMessage instanceof RemoteFileSystemMessage) {
            balFuture.complete(((RemoteFileSystemMessage) remoteFileSystemBaseMessage).isDirectory());
        }
        return true;
    }

    static boolean executeListAction(RemoteFileSystemBaseMessage remoteFileSystemBaseMessage,
                                     CompletableFuture<Object> balFuture) {
        if (remoteFileSystemBaseMessage instanceof RemoteFileSystemMessage) {
            RemoteFileSystemMessage message = (RemoteFileSystemMessage) remoteFileSystemBaseMessage;
            Map<String, FileInfo> childrenInfo = message.getChildrenInfo();
            BArray arrayValue = ValueCreator.createArrayValue(TypeCreator.createArrayType(FtpUtil.getFileInfoType()));

            int i = 0;
            for (Map.Entry<String, FileInfo> entry : childrenInfo.entrySet()) {
                Map<String, Object> fileInfoParams = new HashMap<>();
                FileInfo fileInfo = entry.getValue();
                fileInfoParams.put("path", fileInfo.getPath());
                fileInfoParams.put("size", fileInfo.getFileSize());
                fileInfoParams.put("lastModifiedTimestamp", fileInfo.getLastModifiedTime());
                fileInfoParams.put("name", fileInfo.getBaseName());
                fileInfoParams.put("isFolder", fileInfo.isFolder());
                fileInfoParams.put("isFile", fileInfo.isFile());
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
                fileInfoParams.put("uri", fileInfo.getFileName().getURI());
                fileInfoParams.put("rootURI", fileInfo.getFileName().getRootURI());
                fileInfoParams.put("friendlyURI", fileInfo.getFileName().getFriendlyURI());
                try {
                    fileInfoParams.put("pathDecoded", fileInfo.getFileName().getPathDecoded());
                } catch (FileSystemException e) {
                    log.error("Error while evaluating the pathDecoded value.", e);
                }

                final BMap<BString, Object> ballerinaFileInfo = ValueCreator.createRecordValue(
                        new Module(FtpConstants.FTP_ORG_NAME, FtpConstants.FTP_MODULE_NAME,
                                FtpUtil.getFtpPackage().getMajorVersion()), FtpConstants.FTP_FILE_INFO, fileInfoParams);
                arrayValue.add(i++, ballerinaFileInfo);
            }
            balFuture.complete(arrayValue);
        }
        return true;
    }

    static boolean executeSizeAction(RemoteFileSystemBaseMessage remoteFileSystemBaseMessage,
                                     CompletableFuture<Object> balFuture) {
        if (remoteFileSystemBaseMessage instanceof RemoteFileSystemMessage) {
            RemoteFileSystemMessage message = (RemoteFileSystemMessage) remoteFileSystemBaseMessage;
            balFuture.complete((int) message.getSize());
        }
        return true;
    }

    static InputStream getUploadStream(Environment env, BObject clientConnector, BMap<Object, Object> inputContent,
                                       boolean isFile) {
        InputStream fileInputStream;
        if (isFile) {
            BStream fileByteStream = (BStream) inputContent.get(
                    StringUtils.fromString(FtpConstants.INPUT_CONTENT_FILE_CONTENT_KEY));
            if (fileByteStream != null) {
                BObject iteratorObj = fileByteStream.getIteratorObj();
                fileInputStream = new ByteArrayInputStream(new byte[0]) {
                    private final BufferHolder bufferHolder = new BufferHolder();

                    @Override
                    public int read(byte[] b) {
                        if (bufferHolder.getBuffer().length == 0) {
                            callStreamNext(env, clientConnector, bufferHolder, iteratorObj);
                        }
                        int bLength = b.length;
                        int buffLength = bufferHolder.getBuffer().length;
                        if (bufferHolder.isTerminal()) {
                            return -1;
                        }
                        if (bLength > buffLength) {
                            for (int i = 0; i < buffLength; i++) {
                                b[i] = bufferHolder.getBuffer()[i];
                            }
                            bufferHolder.setBuffer(new byte[0]);
                            return buffLength;
                        } else {
                            for (int i = 0; i < bLength; i++) {
                                b[i] = bufferHolder.getBuffer()[i];
                            }
                            int remainCount = buffLength % bLength;
                            byte[] remainBytes = new byte[remainCount];
                            for (int i = 0; i < remainCount; i++) {
                                remainBytes[i] = bufferHolder.getBuffer()[buffLength - remainCount + i];
                            }
                            bufferHolder.setBuffer(remainBytes);
                            return bLength;
                        }
                    }

                    @Override
                    public void close() {
                        callStreamClose(env, clientConnector, bufferHolder, iteratorObj);
                    }
                };
                return fileInputStream;
            }
            return null;
        } else {
            String textContent = (inputContent.getStringValue(StringUtils.fromString(
                    FtpConstants.INPUT_CONTENT_TEXT_CONTENT_KEY))).getValue();
            return new ByteArrayInputStream(textContent.getBytes());
        }
    }

    private static void callStreamNext(Environment env, BObject entity, BufferHolder bufferHolder,
                                       BObject iteratorObj) {
        env.yieldAndRun(() -> {
            Object result = env.getRuntime().callMethod(iteratorObj, BYTE_STREAM_NEXT_FUNC, null);
            if (result == bufferHolder.getTerminalType()) {
                handleStreamEnd(entity, bufferHolder);
                return null;
            }
            BArray arrayValue = ((BMap) result).getArrayValue(FIELD_VALUE);
            if (arrayValue == null) {
                handleStreamEnd(entity, bufferHolder);
                return null;
            }
            byte[] bytes = arrayValue.getBytes();
            bufferHolder.setBuffer(bytes);
            bufferHolder.setTerminal(false);
            return null;
        });
    }

    private static void callStreamClose(Environment env, BObject entity, BufferHolder bufferHolder,
                                       BObject iteratorObj) {
        env.yieldAndRun(() -> {
            try {
                env.getRuntime().callMethod(iteratorObj, BYTE_STREAM_CLOSE_FUNC, null);
                handleStreamEnd(entity, bufferHolder);
            } catch (Throwable t) {
                handleStreamEnd(entity, bufferHolder);
            }
            return null;
        });
    }

    public static void handleStreamEnd(BObject entity, BufferHolder bufferHolder) {
        entity.addNativeData(ENTITY_BYTE_STREAM, null);
        bufferHolder.setTerminal(true);
    }

    static RemoteFileSystemMessage getUncompressedMessage(BObject clientConnector, String filePath,
            Map<String, String> propertyMap, InputStream stream) throws BallerinaFtpException {
        String url = FtpUtil.createUrl(clientConnector, filePath);
        propertyMap.put(FtpConstants.URI, url);
        return new RemoteFileSystemMessage(stream);
    }

    static RemoteFileSystemMessage getCompressedMessage(BObject clientConnector, String filePath,
                                                        Map<String, String> propertyMap,
                                                        ByteArrayInputStream compressedStream) {
        try {
            String compressedFilePath = FtpUtil.getCompressedFileName(filePath);
            String url = FtpUtil.createUrl(clientConnector, compressedFilePath);
            propertyMap.put(FtpConstants.URI, url);
            return new RemoteFileSystemMessage(compressedStream);
        } catch (BallerinaFtpException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    /**
     * Concrete implementation of the {@link Channel}.
     */
    private static class FtpChannel extends Channel {

        FtpChannel(ByteChannel channel) {
            super(channel);
        }

        @Override
        public void transfer(int i, int i1, WritableByteChannel writableByteChannel) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Channel getChannel() {
            return this;
        }

        @Override
        public boolean remaining() {
            return false;
        }
    }

    /**
     * Create ByteChannel by encapsulating InputStream which comes from transport layer.
     */
    private static class FtpByteChannel implements ByteChannel {

        private InputStream inputStream;
        private ReadableByteChannel inputChannel;

        FtpByteChannel(InputStream inputStream) {
            this.inputStream = inputStream;
            this.inputChannel = Channels.newChannel(inputStream);
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            return inputChannel.read(dst);
        }

        @Override
        public int write(ByteBuffer src) {
            return 0;
        }

        @Override
        public boolean isOpen() {
            return inputChannel.isOpen();
        }

        @Override
        public void close() throws IOException {
            inputChannel.close();
            inputStream.close();
        }
    }
}
