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
import org.ballerinalang.ftp.util.FTPUtil;
import org.ballerinalang.ftp.util.FtpConstants;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
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

import static org.ballerinalang.ftp.util.FtpConstants.FTP_PACKAGE_NAME;

/**
* FTP Get operation.
*/
@BallerinaFunction(
        orgName = "wso2",
        packageName = "ftp:0.0.0",
        functionName = "get",
        receiver = @Receiver(type = TypeKind.OBJECT,
                             structType = "Client",
                             structPackage = FTP_PACKAGE_NAME)
)
public class Get extends AbstractFtpAction {

    private static final Logger log = LoggerFactory.getLogger(Get.class);

    @Override
    public void execute(Context context) {
        BMap<String, BValue> clientConnector = (BMap<String, BValue>) context.getRefArgument(0);
        String path = context.getStringArgument(0);
        String username = (String) clientConnector.getNativeData(FtpConstants.ENDPOINT_CONFIG_USERNAME);
        String password = (String) clientConnector.getNativeData(FtpConstants.ENDPOINT_CONFIG_PASSWORD);
        String host = (String) clientConnector.getNativeData(FtpConstants.ENDPOINT_CONFIG_HOST);
        int port = (int) clientConnector.getNativeData(FtpConstants.ENDPOINT_CONFIG_PORT);
        String protocol = (String) clientConnector.getNativeData(FtpConstants.ENDPOINT_CONFIG_PROTOCOL);
        String url = FTPUtil.createUrl(protocol, host, port, username, password, path);
        Map<String, String> prop = (Map<String, String>) clientConnector.getNativeData(FtpConstants.PROPERTY_MAP);
        //Create property map to send to transport.
        Map<String, String> propertyMap = new HashMap<>(prop);
        propertyMap.put(FtpConstants.PROPERTY_URI, url);

        FTPReadClientConnectorListener connectorListener = new FTPReadClientConnectorListener(context);
        RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        VFSClientConnector connector;
        try {
            connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap, connectorListener);
        } catch (RemoteFileSystemConnectorException e) {
            context.setReturnValues(FTPUtil.createError(context, e.getMessage()));
            log.error(e.getMessage(), e);
            return;
        }
        connector.send(null, FtpAction.GET);
    }

    private static class FTPReadClientConnectorListener extends FTPClientConnectorListener {

        private static final Logger log = LoggerFactory.getLogger(FTPReadClientConnectorListener.class);
        private Context context;

        FTPReadClientConnectorListener(Context context) {
            super(context);
            this.context = context;
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
            getContext().setReturnValues(FTPUtil.createError(context, throwable.getMessage()));
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
        public int write(ByteBuffer src) {
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
