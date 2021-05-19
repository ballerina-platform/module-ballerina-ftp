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

package org.ballerinalang.stdlib.ftp.client;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Future;
import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.async.Callback;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BStream;
import io.ballerina.runtime.api.values.BString;
import org.apache.commons.vfs2.FileSystemException;
import org.ballerinalang.stdlib.ftp.util.BallerinaFTPException;
import org.ballerinalang.stdlib.ftp.util.BufferHolder;
import org.ballerinalang.stdlib.ftp.util.FTPConstants;
import org.ballerinalang.stdlib.ftp.util.FTPUtil;
import org.ballerinalang.stdlib.ftp.util.FileByteArrayInputStream;
import org.ballerinalang.stdlib.io.channels.base.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.transport.remotefilesystem.message.FileInfo;
import org.wso2.transport.remotefilesystem.message.RemoteFileSystemBaseMessage;
import org.wso2.transport.remotefilesystem.message.RemoteFileSystemMessage;

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
import java.util.concurrent.CountDownLatch;

import static org.ballerinalang.stdlib.ftp.util.FTPConstants.BYTE_STREAM_NEXT_FUNC;
import static org.ballerinalang.stdlib.ftp.util.FTPConstants.ENTITY_BYTE_STREAM;
import static org.ballerinalang.stdlib.ftp.util.FTPConstants.FIELD_VALUE;
import static org.ballerinalang.stdlib.ftp.util.FTPConstants.STREAM_ENTRY_RECORD;
import static org.ballerinalang.stdlib.ftp.util.FTPUtil.getFtpPackage;


/**
 * Contains helper methods to invoke FTP actions.
 */
public class FTPClientHelper {

    private static final String READABLE_BYTE_CHANNEL = "ReadableByteChannel";
    private static final Logger log = LoggerFactory.getLogger(FTPClientHelper.class);

    private FTPClientHelper() {
        // private constructor
    }

    static boolean executeGenericAction(Future balFuture) {

        balFuture.complete(null);
        return true;
    }

    static boolean executeGetAction(RemoteFileSystemBaseMessage remoteFileSystemBaseMessage,
                                    Future balFuture, BObject clientConnector) {
        try {
            if (remoteFileSystemBaseMessage instanceof RemoteFileSystemMessage) {
                final InputStream in = ((RemoteFileSystemMessage) remoteFileSystemBaseMessage).getInputStream();
                ByteChannel byteChannel = new FTPByteChannel(in);
                Channel channel = new FTPChannel(byteChannel);
                InputStream inputStream = channel.getInputStream();
                clientConnector.addNativeData("readInputStream", inputStream);
                long arraySize = (long) clientConnector.getNativeData("arraySize");
                BMap<BString, Object> streamEntry = generateInputStreamEntry(inputStream, arraySize);
                clientConnector.addNativeData(ENTITY_BYTE_STREAM, streamEntry);
                balFuture.complete(streamEntry);
            }
        } catch (IOException e) {
            log.error("Error occurred while reading stream: ", e);
        }
        return true;
    }

    public static BMap<BString, Object> generateInputStreamEntry(InputStream inputStream, long arraySize) {
        BMap<BString, Object> streamEntry = ValueCreator.createRecordValue(getFtpPackage(), STREAM_ENTRY_RECORD);
        int arraySizeInt = (int) arraySize;
        try {
            byte[] buffer = new byte[arraySizeInt];
            int readNumber = inputStream.read(buffer);
            if (readNumber == -1) {
                inputStream.close();
                streamEntry.addNativeData("readInputStream", null);
                return null;
            }
            byte[] returnArray;
            if (readNumber < arraySizeInt) {
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
                                            Future balFuture) {

        if (remoteFileSystemBaseMessage instanceof RemoteFileSystemMessage) {
            balFuture.complete(((RemoteFileSystemMessage) remoteFileSystemBaseMessage).isDirectory());
        }
        return true;
    }

    static boolean executeListAction(RemoteFileSystemBaseMessage remoteFileSystemBaseMessage,
                                     Future balFuture) {

        if (remoteFileSystemBaseMessage instanceof RemoteFileSystemMessage) {
            RemoteFileSystemMessage message = (RemoteFileSystemMessage) remoteFileSystemBaseMessage;
            Map<String, FileInfo> childrenInfo = message.getChildrenInfo();
            BArray arrayValue = ValueCreator.createArrayValue(TypeCreator.createArrayType(FTPUtil.getFileInfoType()));

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
                        new Module(FTPConstants.FTP_ORG_NAME, FTPConstants.FTP_MODULE_NAME,
                                FTPUtil.getFtpPackage().getVersion()), FTPConstants.FTP_FILE_INFO, fileInfoParams);
                arrayValue.add(i++, ballerinaFileInfo);
            }
            balFuture.complete(arrayValue);
        }
        return true;
    }

    static boolean executeSizeAction(RemoteFileSystemBaseMessage remoteFileSystemBaseMessage,
                                     Future balFuture) {

        if (remoteFileSystemBaseMessage instanceof RemoteFileSystemMessage) {
            RemoteFileSystemMessage message = (RemoteFileSystemMessage) remoteFileSystemBaseMessage;
            balFuture.complete((int) message.getSize());
        }
        return true;
    }

    static InputStream getUploadStream(Environment env, BObject clientConnector, BMap<Object, Object> inputContent,
                                       boolean isFile) {
        if (isFile) {
            BStream fileByteStream = (BStream) inputContent.get(
                    StringUtils.fromString(FTPConstants.INPUT_CONTENT_FILE_CONTENT_KEY));
            if (fileByteStream != null) {
                BObject iteratorObj = fileByteStream.getIteratorObj();
                CountDownLatch latch = new CountDownLatch(1);
                return new FileByteArrayInputStream(new byte[0], env, clientConnector, new BufferHolder(),
                        iteratorObj, latch);
            }
            return null;
        } else {
            String textContent = (inputContent.getStringValue(StringUtils.fromString(
                    FTPConstants.INPUT_CONTENT_TEXT_CONTENT_KEY))).getValue();
            return new ByteArrayInputStream(textContent.getBytes());
        }
    }

    public static void callStreamNext(Environment env, BObject entity, BufferHolder bufferHolder,
                                       BObject iteratorObj, CountDownLatch latch) {
        env.getRuntime().invokeMethodAsync(iteratorObj, BYTE_STREAM_NEXT_FUNC, null, null, new Callback() {
            @Override
            public void notifySuccess(Object result) {
                if (result == null) {
                    entity.addNativeData(ENTITY_BYTE_STREAM, null);
                    latch.countDown();
                    return;
                }
                BArray arrayValue = ((BMap) result).getArrayValue(FIELD_VALUE);
                byte[] bytes = arrayValue.getBytes();
                bufferHolder.setBuffer(bytes);
            }

            @Override
            public void notifyFailure(BError bError) {
                latch.countDown();
                throw ErrorCreator.createError(StringUtils.fromString(
                        "Error occurred while streaming content: " + bError.getMessage()));
            }
        });
    }

    static RemoteFileSystemMessage getUncompressedMessage(BObject clientConnector, String filePath,
                                                          Map<String, String> propertyMap, InputStream stream) {

        try {
            String url = FTPUtil.createUrl(clientConnector, filePath);
            propertyMap.put(FTPConstants.PROPERTY_URI, url);
            return new RemoteFileSystemMessage(stream);
        } catch (BallerinaFTPException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    static RemoteFileSystemMessage getCompressedMessage(BObject clientConnector, String filePath,
                                                        Map<String, String> propertyMap,
                                                        ByteArrayInputStream compressedStream) {

        try {
            String compressedFilePath = FTPUtil.getCompressedFileName(filePath);
            String url = FTPUtil.createUrl(clientConnector, compressedFilePath);
            propertyMap.put(FTPConstants.PROPERTY_URI, url);
            return new RemoteFileSystemMessage(compressedStream);
        } catch (BallerinaFTPException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    /**
     * Concrete implementation of the {@link Channel}.
     */
    private static class FTPChannel extends Channel {

        FTPChannel(ByteChannel channel) {

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
    private static class FTPByteChannel implements ByteChannel {

        private InputStream inputStream;
        private ReadableByteChannel inputChannel;

        FTPByteChannel(InputStream inputStream) {

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
