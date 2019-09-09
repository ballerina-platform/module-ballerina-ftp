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

import org.ballerinalang.jvm.BRuntime;
import org.ballerinalang.jvm.values.ArrayValue;
import org.ballerinalang.jvm.values.MapValue;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.stdlib.io.channels.base.Channel;
import org.ballerinalang.stdlib.io.utils.IOConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.ei.ftp.util.BallerinaFTPException;
import org.wso2.ei.ftp.util.FTPUtil;
import org.wso2.ei.ftp.util.FtpConstants;
import org.wso2.transport.remotefilesystem.RemoteFileSystemConnectorFactory;
import org.wso2.transport.remotefilesystem.client.connector.contract.FtpAction;
import org.wso2.transport.remotefilesystem.client.connector.contract.VFSClientConnector;
import org.wso2.transport.remotefilesystem.exception.RemoteFileSystemConnectorException;
import org.wso2.transport.remotefilesystem.impl.RemoteFileSystemConnectorFactoryImpl;
import org.wso2.transport.remotefilesystem.message.RemoteFileSystemMessage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Contains functionality of FTP client
 */
public class FTPClient {

    private static final Logger logger = LoggerFactory.getLogger("ballerina");

    private FTPClient() {
        // private constructor
    }

    public static void initClientEndpoint(ObjectValue clientEndpoint, MapValue<Object, Object> config)
            throws BallerinaFTPException {

        String protocol = config.getStringValue(FtpConstants.ENDPOINT_CONFIG_PROTOCOL);
        if (FTPUtil.notValidProtocol(protocol)) {
            throw new BallerinaFTPException("Only FTP, SFTP and FTPS protocols are supported by FTP client.");
        }
        String host = config.getStringValue(FtpConstants.ENDPOINT_CONFIG_HOST);
        int port = FTPUtil.getIntFromConfig(config, FtpConstants.ENDPOINT_CONFIG_PORT, logger);

        final MapValue secureSocket = config.getMapValue(FtpConstants.ENDPOINT_CONFIG_SECURE_SOCKET);
        String username = null;
        String password = null;
        if (secureSocket != null) {
            final MapValue basicAuth = secureSocket.getMapValue(FtpConstants.ENDPOINT_CONFIG_BASIC_AUTH);
            if (basicAuth != null) {
                username = basicAuth.getStringValue(FtpConstants.ENDPOINT_CONFIG_USERNAME);
                password = basicAuth.getStringValue(FtpConstants.ENDPOINT_CONFIG_PASSWORD);
            }
        }

        clientEndpoint.addNativeData(FtpConstants.ENDPOINT_CONFIG_USERNAME, username);
        clientEndpoint.addNativeData(FtpConstants.ENDPOINT_CONFIG_PASSWORD, password);
        clientEndpoint.addNativeData(FtpConstants.ENDPOINT_CONFIG_HOST, host);
        clientEndpoint.addNativeData(FtpConstants.ENDPOINT_CONFIG_PORT, port);
        clientEndpoint.addNativeData(FtpConstants.ENDPOINT_CONFIG_PROTOCOL, protocol);
        Map<String, String> ftpConfig = new HashMap<>(3);
        ftpConfig.put(FtpConstants.FTP_PASSIVE_MODE, String.valueOf(true));
        ftpConfig.put(FtpConstants.USER_DIR_IS_ROOT, String.valueOf(false));
        ftpConfig.put(FtpConstants.AVOID_PERMISSION_CHECK, String.valueOf(true));
        clientEndpoint.addNativeData(FtpConstants.PROPERTY_MAP, ftpConfig);
    }

    public static ObjectValue get(ObjectValue clientConnector, String filePath) throws BallerinaFTPException {

        String url = FTPUtil.createUrl(clientConnector, filePath);
        Map<String, String> propertyMap = new HashMap<>(
                (Map<String, String>) clientConnector.getNativeData(FtpConstants.PROPERTY_MAP));
        propertyMap.put(FtpConstants.PROPERTY_URI, url);

        CompletableFuture<Object> future = BRuntime.markAsync();
        FTPClientListener connectorListener = new FTPClientListener(future,
                remoteFileSystemBaseMessage -> FTPClientHelper.executeGetAction(remoteFileSystemBaseMessage, future));
        RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        VFSClientConnector connector;
        try {
            connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap, connectorListener);
        } catch (RemoteFileSystemConnectorException e) {
            logger.error(e.getMessage(), e);
            throw new BallerinaFTPException(e.getMessage());
        }
        connector.send(null, FtpAction.GET);
        return null;
    }

    public static void append(ObjectValue clientConnector, String filePath, ObjectValue sourceChannel)
            throws BallerinaFTPException {

        try {
            String url = FTPUtil.createUrl(clientConnector, filePath);
            Channel byteChannel = (Channel) sourceChannel.getNativeData(IOConstants.BYTE_CHANNEL_NAME);
            RemoteFileSystemMessage message = new RemoteFileSystemMessage(byteChannel.getInputStream());

            Map<String, String> propertyMap = new HashMap<>(
                    (Map<String, String>) clientConnector.getNativeData(FtpConstants.PROPERTY_MAP));
            propertyMap.put(FtpConstants.PROPERTY_URI, url);

            CompletableFuture<Object> future = BRuntime.markAsync();
            FTPClientListener connectorListener = new FTPClientListener(future,
                    remoteFileSystemBaseMessage -> FTPClientHelper.executeGenericAction(future));
            RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();

            VFSClientConnector connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap,
                    connectorListener);
            connector.send(message, FtpAction.APPEND);
            future.complete(null);
        } catch (RemoteFileSystemConnectorException | IOException e) {
            logger.error(e.getMessage(), e);
            throw new BallerinaFTPException(e.getMessage());
        }
    }

    public static void delete(ObjectValue clientConnector, String filePath) throws BallerinaFTPException {

        String url = FTPUtil.createUrl(clientConnector, filePath);
        Map<String, String> propertyMap = new HashMap<>(
                (Map<String, String>) clientConnector.getNativeData(FtpConstants.PROPERTY_MAP));
        propertyMap.put(FtpConstants.PROPERTY_URI, url);

        CompletableFuture<Object> future = BRuntime.markAsync();
        FTPClientListener connectorListener = new FTPClientListener(future,
                remoteFileSystemBaseMessage -> FTPClientHelper.executeGenericAction(future));
        RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        VFSClientConnector connector;
        try {
            connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap, connectorListener);
        } catch (RemoteFileSystemConnectorException e) {
            logger.error(e.getMessage(), e);
            throw new BallerinaFTPException(e.getMessage());
        }
        connector.send(null, FtpAction.DELETE);
        future.complete(null);
    }

    public static boolean isDirectory(ObjectValue clientConnector, String filePath) throws BallerinaFTPException {

        String url = FTPUtil.createUrl(clientConnector, filePath);
        Map<String, String> propertyMap = new HashMap<>(
                (Map<String, String>) clientConnector.getNativeData(FtpConstants.PROPERTY_MAP));
        propertyMap.put(FtpConstants.PROPERTY_URI, url);

        CompletableFuture<Object> future = BRuntime.markAsync();
        FTPClientListener connectorListener = new FTPClientListener(future, remoteFileSystemBaseMessage ->
                FTPClientHelper.executeIsDirectoryAction(remoteFileSystemBaseMessage, future));
        RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        VFSClientConnector connector;
        try {
            connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap, connectorListener);
        } catch (RemoteFileSystemConnectorException e) {
            logger.error(e.getMessage(), e);
            throw new BallerinaFTPException(e.getMessage());
        }
        connector.send(null, FtpAction.ISDIR);
        return false;
    }

    public static ArrayValue list(ObjectValue clientConnector, String filePath) throws BallerinaFTPException {

        String url = FTPUtil.createUrl(clientConnector, filePath);
        Map<String, String> propertyMap = new HashMap<>(
                (Map<String, String>) clientConnector.getNativeData(FtpConstants.PROPERTY_MAP));
        propertyMap.put(FtpConstants.PROPERTY_URI, url);

        CompletableFuture<Object> future = BRuntime.markAsync();
        FTPClientListener connectorListener = new FTPClientListener(future, remoteFileSystemBaseMessage ->
                FTPClientHelper.executeListAction(remoteFileSystemBaseMessage, future));
        RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        VFSClientConnector connector;
        try {
            connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap, connectorListener);
        } catch (RemoteFileSystemConnectorException e) {
            logger.error(e.getMessage(), e);
            throw new BallerinaFTPException(e.getMessage());
        }
        connector.send(null, FtpAction.LIST);
        return null;
    }

    public static void mkdir(ObjectValue clientConnector, String path) throws BallerinaFTPException {

        String url = FTPUtil.createUrl(clientConnector, path);
        Map<String, String> propertyMap = new HashMap<>(
                (Map<String, String>) clientConnector.getNativeData(FtpConstants.PROPERTY_MAP));
        propertyMap.put(FtpConstants.PROPERTY_URI, url);

        CompletableFuture<Object> future = BRuntime.markAsync();
        FTPClientListener connectorListener = new FTPClientListener(future,
                remoteFileSystemBaseMessage -> FTPClientHelper.executeGenericAction(future));
        RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        VFSClientConnector connector;
        try {
            connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap, connectorListener);
        } catch (RemoteFileSystemConnectorException e) {
            logger.error(e.getMessage(), e);
            throw new BallerinaFTPException(e.getMessage());
        }
        connector.send(null, FtpAction.MKDIR);
        future.complete(null);
    }

    public static void put(ObjectValue clientConnector, String filePath, ObjectValue sourceChannel)
            throws BallerinaFTPException {

        try {
            String url = FTPUtil.createUrl(clientConnector, filePath);

            Map<String, String> propertyMap = new HashMap<>(
                    (Map<String, String>) clientConnector.getNativeData(FtpConstants.PROPERTY_MAP));
            propertyMap.put(FtpConstants.PROPERTY_URI, url);

            Channel byteChannel = (Channel) sourceChannel.getNativeData(IOConstants.BYTE_CHANNEL_NAME);
            RemoteFileSystemMessage message = new RemoteFileSystemMessage(byteChannel.getInputStream());

            CompletableFuture<Object> future = BRuntime.markAsync();
            FTPClientListener connectorListener = new FTPClientListener(future, remoteFileSystemBaseMessage ->
                    FTPClientHelper.executeGenericAction(future));
            RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();

            VFSClientConnector connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap,
                    connectorListener);
            connector.send(message, FtpAction.PUT);
            future.complete(null);
        } catch (RemoteFileSystemConnectorException | IOException e) {
            logger.error(e.getMessage(), e);
            throw new BallerinaFTPException(e.getMessage());
        }
    }

    public static void rename(ObjectValue clientConnector, String origin, String destination)
            throws BallerinaFTPException {

        Map<String, String> propertyMap = new HashMap<>(
                (Map<String, String>) clientConnector.getNativeData(FtpConstants.PROPERTY_MAP));
        propertyMap.put(FtpConstants.PROPERTY_URI, FTPUtil.createUrl(clientConnector, origin));
        propertyMap.put(FtpConstants.PROPERTY_DESTINATION, FTPUtil.createUrl(clientConnector, destination));

        CompletableFuture<Object> future = BRuntime.markAsync();
        FTPClientListener connectorListener = new FTPClientListener(future,
                remoteFileSystemBaseMessage -> FTPClientHelper.executeGenericAction(future));
        RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        VFSClientConnector connector;
        try {
            connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap, connectorListener);
        } catch (RemoteFileSystemConnectorException e) {
            logger.error(e.getMessage(), e);
            throw new BallerinaFTPException(e.getMessage());
        }
        connector.send(null, FtpAction.RENAME);
        future.complete(null);
    }

    public static void rmdir(ObjectValue clientConnector, String filePath) throws BallerinaFTPException {

        String url = FTPUtil.createUrl(clientConnector, filePath);
        Map<String, String> propertyMap = new HashMap<>(
                (Map<String, String>) clientConnector.getNativeData(FtpConstants.PROPERTY_MAP));
        propertyMap.put(FtpConstants.PROPERTY_URI, url);

        CompletableFuture<Object> future = BRuntime.markAsync();
        FTPClientListener connectorListener = new FTPClientListener(future,
                remoteFileSystemBaseMessage -> FTPClientHelper.executeGenericAction(future));
        RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        VFSClientConnector connector;
        try {
            connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap, connectorListener);
        } catch (RemoteFileSystemConnectorException e) {
            logger.error(e.getMessage(), e);
            throw new BallerinaFTPException(e.getMessage());
        }
        connector.send(null, FtpAction.RMDIR);
        future.complete(null);
    }

    public static int size(ObjectValue clientConnector, String filePath) throws BallerinaFTPException {

        String url = FTPUtil.createUrl(clientConnector, filePath);
        Map<String, String> propertyMap = new HashMap<>(
                (Map<String, String>) clientConnector.getNativeData(FtpConstants.PROPERTY_MAP));
        propertyMap.put(FtpConstants.PROPERTY_URI, url);
        propertyMap.put(FtpConstants.FTP_PASSIVE_MODE, Boolean.TRUE.toString());

        CompletableFuture<Object> future = BRuntime.markAsync();
        FTPClientListener connectorListener = new FTPClientListener(future, remoteFileSystemBaseMessage ->
                FTPClientHelper.executeSizeAction(remoteFileSystemBaseMessage, future));
        RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        VFSClientConnector connector;
        try {
            connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap, connectorListener);
        } catch (RemoteFileSystemConnectorException e) {
            logger.error(e.getMessage(), e);
            throw new BallerinaFTPException(e.getMessage());
        }
        connector.send(null, FtpAction.SIZE);
        return 0;
    }
}
