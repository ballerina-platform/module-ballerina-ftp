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

import org.ballerinalang.ftp.util.BallerinaFTPException;
import org.ballerinalang.ftp.util.FTPUtil;
import org.ballerinalang.ftp.util.FtpConstants;
import org.ballerinalang.jvm.BRuntime;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.stdlib.io.channels.base.Channel;
import org.ballerinalang.stdlib.io.utils.IOConstants;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * FTP Append operation.
 */
public class Append extends AbstractFtpAction {

    private static final Logger log = LoggerFactory.getLogger("ballerina");

    public static void append(ObjectValue clientConnector, String path, ObjectValue sourceChannel)
            throws BallerinaFTPException {

        try {
            String username = (String) clientConnector.getNativeData(FtpConstants.ENDPOINT_CONFIG_USERNAME);
            String password = (String) clientConnector.getNativeData(FtpConstants.ENDPOINT_CONFIG_PASSWORD);
            String host = (String) clientConnector.getNativeData(FtpConstants.ENDPOINT_CONFIG_HOST);
            int port = (int) clientConnector.getNativeData(FtpConstants.ENDPOINT_CONFIG_PORT);
            String protocol = (String) clientConnector.getNativeData(FtpConstants.ENDPOINT_CONFIG_PROTOCOL);
            String url = FTPUtil.createUrl(protocol, host, port, username, password, path);
            Channel byteChannel = (Channel) sourceChannel.getNativeData(IOConstants.BYTE_CHANNEL_NAME);
            RemoteFileSystemMessage message = new RemoteFileSystemMessage(byteChannel.getInputStream());

            Map<String, String> prop = (Map<String, String>) clientConnector.getNativeData(FtpConstants.PROPERTY_MAP);
            //Create property map to send to transport.
            Map<String, String> propertyMap = new HashMap<>(prop);
            propertyMap.put(FtpConstants.PROPERTY_URI, url);

            CompletableFuture<Object> future = BRuntime.markAsync();
            FTPAppendClientConnectorListener connectorListener = new FTPAppendClientConnectorListener(future);
            RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();

            VFSClientConnector connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap,
                    connectorListener);
            connector.send(message, FtpAction.APPEND);
            future.complete(null);
        } catch (RemoteFileSystemConnectorException | IOException e) {
            log.error(e.getMessage(), e);
            throw new BallerinaFTPException(e.getMessage());
        }
    }

    private static class FTPAppendClientConnectorListener extends AbstractFtpAction.FTPClientConnectorListener {

        private static final Logger log = LoggerFactory.getLogger("ballerina");
        private CompletableFuture<Object> future;

        FTPAppendClientConnectorListener(CompletableFuture<Object> future) {

            super(future);
            this.future = future;
        }

        @Override
        public boolean onMessage(RemoteFileSystemBaseMessage remoteFileSystemBaseMessage) {

            future.complete(null);
            return true;
        }

        @Override
        public void onError(Throwable throwable) {

            log.error(throwable.getMessage(), throwable);
            future.complete(FTPUtil.createError(throwable.getMessage()));
        }
    }

}
