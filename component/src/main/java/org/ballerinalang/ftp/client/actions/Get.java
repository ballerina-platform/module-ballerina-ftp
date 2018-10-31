/*
 * Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.ballerinalang.ftp.client.actions;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BLangVMStructs;
import org.ballerinalang.ftp.util.FtpConstants;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BString;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.natives.annotations.ReturnType;
import org.ballerinalang.stdlib.io.channels.base.Channel;
import org.ballerinalang.stdlib.io.channels.base.readers.ChannelReader;
import org.ballerinalang.stdlib.io.channels.base.writers.ChannelWriter;
import org.ballerinalang.stdlib.io.utils.BallerinaIOException;
import org.ballerinalang.stdlib.io.utils.IOConstants;
import org.ballerinalang.util.codegen.PackageInfo;
import org.ballerinalang.util.codegen.StructureTypeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.transport.remotefilesystem.RemoteFileSystemConnectorFactory;
import org.wso2.transport.remotefilesystem.client.connector.contract.FtpAction;
import org.wso2.transport.remotefilesystem.client.connector.contract.VFSClientConnector;
import org.wso2.transport.remotefilesystem.exception.RemoteFileSystemConnectorException;
import org.wso2.transport.remotefilesystem.impl.RemoteFileSystemConnectorFactoryImpl;
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

import static org.ballerinalang.ftp.util.FtpConstants.BALLERINA_BUILTIN;
import static org.ballerinalang.ftp.util.FtpConstants.FTP_PACKAGE_NAME;
import static org.ballerinalang.model.types.TypeKind.CONNECTOR;
import static org.ballerinalang.model.types.TypeKind.OBJECT;
import static org.ballerinalang.model.types.TypeKind.STRING;

/**
* FTP Get operation.
*/
@BallerinaFunction(
        orgName = "wso2",
        packageName = "ftp:0.0.0",
        functionName = "get",
        receiver = @Receiver(type = OBJECT, structType = "ClientActions", structPackage = FTP_PACKAGE_NAME),
        args = {@Argument(name = "ftpClientConnector", type = CONNECTOR),
                @Argument(name = "path", type = STRING)},
        returnType = {@ReturnType(type = OBJECT, structType = "ReadableByteChannel", structPackage = "ballerina/io"),
                      @ReturnType(type = OBJECT, structType = "error", structPackage = BALLERINA_BUILTIN)
        }
)
public class Get extends AbstractFtpAction {

    private static final Logger log = LoggerFactory.getLogger(Get.class);

    @Override
    public void execute(Context context) {
        BMap<String, BValue> clientConnector = (BMap<String, BValue>) context.getRefArgument(0);
        String pathString = context.getStringArgument(0);

        String url = (String) clientConnector.getNativeData(FtpConstants.URL);
        Map<String, String> prop = (Map<String, String>) clientConnector.getNativeData(FtpConstants.PROPERTY_MAP);
        //Create property map to send to transport.
        Map<String, String> propertyMap = new HashMap<>(prop);
        propertyMap.put(FtpConstants.PROPERTY_URI, url + pathString);

        FTPReadClientConnectorListener connectorListener = new FTPReadClientConnectorListener(context);
        RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        VFSClientConnector connector;
        try {
            connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap, connectorListener);
        } catch (RemoteFileSystemConnectorException e) {
            BMap<String, BValue> error = getClientErrorStruct(context);
            error.put("message", new BString(e.getMessage()));
            context.setReturnValues(error);
            log.error(e.getMessage(), e);
            return;
        }
        connector.send(null, FtpAction.GET);
    }

    private static class FTPReadClientConnectorListener extends FTPClientConnectorListener {

        private static final Logger log = LoggerFactory.getLogger(FTPReadClientConnectorListener.class);

        FTPReadClientConnectorListener(Context context) {
            super(context);
        }

        @Override
        public boolean onMessage(RemoteFileSystemBaseMessage remoteFileSystemBaseMessage) {
            if (remoteFileSystemBaseMessage instanceof RemoteFileSystemMessage) {
                final InputStream in = ((RemoteFileSystemMessage) remoteFileSystemBaseMessage).getInputStream();
                ByteChannel byteChannel = new ReadByteChannel(in);
                Channel channel = new FTPGetAbstractChannel(byteChannel);
                BMap<String, BValue> channelStruct = getBStruct();
                channelStruct.addNativeData(IOConstants.BYTE_CHANNEL_NAME, channel);
                getContext().setReturnValues(channelStruct);
            }
            return true;
        }

        @Override
        public void onError(Throwable throwable) {
            BMap<String, BValue> error = getClientErrorStruct(getContext());
            error.put("message", new BString(throwable.getMessage()));
            getContext().setReturnValues(error);
            log.error(throwable.getMessage(), throwable);
        }

        private BMap<String, BValue> getBStruct() {
            PackageInfo timePackageInfo = getContext().getProgramFile().getPackageInfo("ballerina/io");
            final StructureTypeInfo structInfo = timePackageInfo.getStructInfo("ReadableByteChannel");
            return BLangVMStructs.createBStruct(structInfo);
        }
    }

    /**
     * This class will use to concrete implementation of the {@link Channel}.
     */
    private static class FTPGetAbstractChannel extends Channel {

        FTPGetAbstractChannel(ByteChannel channel) throws BallerinaIOException {
            super(channel, new ChannelReader(), new ChannelWriter());
        }

        @Override
        public void transfer(int i, int i1, WritableByteChannel writableByteChannel) throws BallerinaIOException {
            throw new BallerinaIOException("Unsupported operation.");
        }

        @Override
        public Channel getChannel() {
            return this;
        }

        @Override
        public boolean isSelectable() {
            return false;
        }

        @Override
        public boolean remaining() {
            return false;
        }
    }

    /**
     * This class will use to create ByteChannel by encapsulating InputStream that coming from transport layer.
     */
    private static class ReadByteChannel implements ByteChannel {
        private InputStream in;
        private ReadableByteChannel inChannel;

        ReadByteChannel(InputStream in) {
            this.in = in;
            this.inChannel = Channels.newChannel(in);
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            return inChannel.read(dst);
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            return 0;
        }

        @Override
        public boolean isOpen() {
            return inChannel.isOpen();
        }

        @Override
        public void close() throws IOException {
            inChannel.close();
            in.close();
        }
    }
}
