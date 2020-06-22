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

package org.wso2.ei.b7a.ftp.core.client;

import org.apache.commons.vfs2.FileSystemException;
import org.ballerinalang.jvm.BallerinaValues;
import org.ballerinalang.jvm.StringUtils;
import org.ballerinalang.jvm.types.BArrayType;
import org.ballerinalang.jvm.types.BPackage;
import org.ballerinalang.jvm.values.MapValue;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.jvm.values.api.BArray;
import org.ballerinalang.jvm.values.api.BString;
import org.ballerinalang.jvm.values.api.BValueCreator;
import org.ballerinalang.stdlib.io.channels.base.Channel;
import org.ballerinalang.stdlib.io.utils.IOConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.ei.b7a.ftp.core.util.BallerinaFTPException;
import org.wso2.ei.b7a.ftp.core.util.FTPConstants;
import org.wso2.ei.b7a.ftp.core.util.FTPUtil;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Contains helper methods to invoke FTP actions
 */
class FTPClientHelper {

    private static final String READABLE_BYTE_CHANNEL = "ReadableByteChannel";
    private static final Logger log = LoggerFactory.getLogger(FTPClientHelper.class);

    private FTPClientHelper() {
        // private constructor
    }

    static boolean executeGenericAction(CompletableFuture<Object> future) {

        future.complete(null);
        return true;
    }

    static boolean executeGetAction(RemoteFileSystemBaseMessage remoteFileSystemBaseMessage,
                                    CompletableFuture<Object> future) {

        if (remoteFileSystemBaseMessage instanceof RemoteFileSystemMessage) {
            final InputStream in = ((RemoteFileSystemMessage) remoteFileSystemBaseMessage).getInputStream();
            ByteChannel byteChannel = new FTPByteChannel(in);
            Channel channel = new FTPChannel(byteChannel);

            ObjectValue channelStruct = BallerinaValues.createObjectValue(
                    new BPackage(FTPConstants.IO_ORG_NAME, FTPConstants.IO_MODULE_NAME, FTPConstants.IO_MODULE_VERSION),
                    READABLE_BYTE_CHANNEL);
            channelStruct.addNativeData(IOConstants.BYTE_CHANNEL_NAME, channel);
            future.complete(channelStruct);
        }
        return true;
    }

    static boolean executeIsDirectoryAction(RemoteFileSystemBaseMessage remoteFileSystemBaseMessage,
                                            CompletableFuture<Object> future) {

        if (remoteFileSystemBaseMessage instanceof RemoteFileSystemMessage) {
            future.complete(((RemoteFileSystemMessage) remoteFileSystemBaseMessage).isDirectory());
        }
        return true;
    }

    static boolean executeListAction(RemoteFileSystemBaseMessage remoteFileSystemBaseMessage,
                                     CompletableFuture<Object> future) {

        if (remoteFileSystemBaseMessage instanceof RemoteFileSystemMessage) {
            RemoteFileSystemMessage message = (RemoteFileSystemMessage) remoteFileSystemBaseMessage;
            Map<String, FileInfo> childrenInfo = message.getChildrenInfo();
            BArray arrayValue = BValueCreator.createArrayValue(new BArrayType(FTPUtil.getFileInfoType()));

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

                final MapValue<BString, Object> ballerinaFileInfo = BallerinaValues.createRecordValue(
                        new BPackage(FTPConstants.FTP_ORG_NAME, FTPConstants.FTP_MODULE_NAME,
                                FTPConstants.FTP_MODULE_VERSION), FTPConstants.FTP_FILE_INFO, fileInfoParams);
                arrayValue.add(i++, ballerinaFileInfo);
            }
            future.complete(arrayValue);
        }
        return true;
    }

    static boolean executeSizeAction(RemoteFileSystemBaseMessage remoteFileSystemBaseMessage,
                                     CompletableFuture<Object> future) {

        if (remoteFileSystemBaseMessage instanceof RemoteFileSystemMessage) {
            RemoteFileSystemMessage message = (RemoteFileSystemMessage) remoteFileSystemBaseMessage;
            future.complete((int) message.getSize());
        }
        return true;
    }

    static InputStream getUploadStream(MapValue<Object, Object> inputContent, boolean isFile) {

        if (isFile) {
            try {
                ObjectValue fileContent = inputContent.getObjectValue(StringUtils.fromString(
                        FTPConstants.INPUT_CONTENT_FILE_CONTENT_KEY));
                Channel byteChannel = (Channel) fileContent.getNativeData(IOConstants.BYTE_CHANNEL_NAME);
                return byteChannel.getInputStream();
            } catch (IOException e) {
                log.error("Error in reading input from file");
                return null;
            }
        } else {
            String textContent = (inputContent.getStringValue(StringUtils.fromString(
                    FTPConstants.INPUT_CONTENT_TEXT_CONTENT_KEY))).getValue();
            return new ByteArrayInputStream(textContent.getBytes());
        }
    }

    static RemoteFileSystemMessage getUncompressedMessage(ObjectValue clientConnector, String filePath,
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

    static RemoteFileSystemMessage getCompressedMessage(ObjectValue clientConnector, String filePath,
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
     * Concrete implementation of the {@link Channel}
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
     * Create ByteChannel by encapsulating InputStream which comes from transport layer
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
