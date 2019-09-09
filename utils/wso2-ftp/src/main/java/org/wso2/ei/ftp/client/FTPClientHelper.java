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

package org.wso2.ei.ftp.client;

import org.apache.commons.vfs2.FileSystemException;
import org.ballerinalang.jvm.BallerinaValues;
import org.ballerinalang.jvm.types.BArrayType;
import org.ballerinalang.jvm.types.BPackage;
import org.ballerinalang.jvm.values.ArrayValue;
import org.ballerinalang.jvm.values.MapValue;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.stdlib.io.channels.base.Channel;
import org.ballerinalang.stdlib.io.utils.IOConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.ei.ftp.util.FTPUtil;
import org.wso2.ei.ftp.util.FtpConstants;
import org.wso2.transport.remotefilesystem.message.FileInfo;
import org.wso2.transport.remotefilesystem.message.RemoteFileSystemBaseMessage;
import org.wso2.transport.remotefilesystem.message.RemoteFileSystemMessage;

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
    private static final String PACKAGE_BALLERINA = "ballerina";
    private static final String PACKAGE_IO = "io";

    private static final Logger log = LoggerFactory.getLogger(PACKAGE_BALLERINA);

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
                    new BPackage(PACKAGE_BALLERINA, PACKAGE_IO), READABLE_BYTE_CHANNEL);
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
            ArrayValue arrayValue = new ArrayValue(new BArrayType(FTPUtil.getFileInfoType()));

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

                final MapValue<String, Object> ballerinaFileInfo = BallerinaValues.createRecordValue(
                        new BPackage(FtpConstants.FTP_ORG_NAME, FtpConstants.FTP_MODULE_NAME,
                                FtpConstants.FTP_MODULE_VERSION), FtpConstants.FTP_FILE_INFO, fileInfoParams);
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
