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

import org.ballerinalang.jvm.api.BalEnv;
import org.ballerinalang.jvm.api.BalFuture;
import org.ballerinalang.jvm.api.BStringUtils;
import org.ballerinalang.jvm.api.values.BMap;
import org.ballerinalang.jvm.api.values.BObject;
import org.ballerinalang.stdlib.io.channels.base.Channel;
import org.ballerinalang.stdlib.io.utils.IOConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ballerinalang.stdlib.ftp.util.BallerinaFTPException;
import org.ballerinalang.stdlib.ftp.util.FTPConstants;
import org.ballerinalang.stdlib.ftp.util.FTPUtil;
import org.wso2.transport.remotefilesystem.Constants;
import org.wso2.transport.remotefilesystem.RemoteFileSystemConnectorFactory;
import org.wso2.transport.remotefilesystem.client.connector.contract.FtpAction;
import org.wso2.transport.remotefilesystem.client.connector.contract.VFSClientConnector;
import org.wso2.transport.remotefilesystem.exception.RemoteFileSystemConnectorException;
import org.wso2.transport.remotefilesystem.impl.RemoteFileSystemConnectorFactoryImpl;
import org.wso2.transport.remotefilesystem.message.RemoteFileSystemMessage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains functionality of FTP client
 */
public class FTPClient {

    private static final Logger log = LoggerFactory.getLogger(FTPClient.class);

    private FTPClient() {
        // private constructor
    }

    public static Object initClientEndpoint(BObject clientEndpoint, BMap<Object, Object> config)
            throws BallerinaFTPException {
        String protocol = (config.getStringValue(BStringUtils.fromString(FTPConstants.ENDPOINT_CONFIG_PROTOCOL)))
                .getValue();
        if (FTPUtil.notValidProtocol(protocol)) {
            throw new BallerinaFTPException("Only FTP, SFTP and FTPS protocols are supported by FTP client.");
        }

        Map<String, String> authMap = FTPUtil.getAuthMap(config);
        clientEndpoint.addNativeData(FTPConstants.ENDPOINT_CONFIG_USERNAME,
                authMap.get(FTPConstants.ENDPOINT_CONFIG_USERNAME));
        clientEndpoint.addNativeData(FTPConstants.ENDPOINT_CONFIG_PASS_KEY,
                authMap.get(FTPConstants.ENDPOINT_CONFIG_PASS_KEY));
        clientEndpoint.addNativeData(FTPConstants.ENDPOINT_CONFIG_HOST,
                (config.getStringValue(BStringUtils.fromString(FTPConstants.ENDPOINT_CONFIG_HOST))).getValue());
        clientEndpoint.addNativeData(FTPConstants.ENDPOINT_CONFIG_PORT,
                FTPUtil.extractPortValue(config.getIntValue(BStringUtils.fromString(
                        FTPConstants.ENDPOINT_CONFIG_PORT))));
        clientEndpoint.addNativeData(FTPConstants.ENDPOINT_CONFIG_PROTOCOL, protocol);
        Map<String, String> ftpConfig = new HashMap<>(5);
        BMap secureSocket = config.getMapValue(BStringUtils.fromString(FTPConstants.ENDPOINT_CONFIG_SECURE_SOCKET));
        if (secureSocket != null) {
            final BMap privateKey = secureSocket.getMapValue(BStringUtils.fromString(
                    FTPConstants.ENDPOINT_CONFIG_PRIVATE_KEY));
            if (privateKey != null) {
                final String privateKeyPath = (privateKey.getStringValue(BStringUtils.fromString(
                        FTPConstants.ENDPOINT_CONFIG_PATH))).getValue();
                if (privateKeyPath != null && !privateKeyPath.isEmpty()) {
                    ftpConfig.put(Constants.IDENTITY, privateKeyPath);
                    final String privateKeyPassword = (privateKey.getStringValue(BStringUtils.fromString(
                            FTPConstants.ENDPOINT_CONFIG_PASS_KEY))).getValue();
                    if (privateKeyPassword != null && !privateKeyPassword.isEmpty()) {
                        ftpConfig.put(Constants.IDENTITY_PASS_PHRASE, privateKeyPassword);
                    }
                }
            }
        }
        ftpConfig.put(FTPConstants.FTP_PASSIVE_MODE, String.valueOf(true));
        ftpConfig.put(FTPConstants.USER_DIR_IS_ROOT, String.valueOf(false));
        ftpConfig.put(FTPConstants.AVOID_PERMISSION_CHECK, String.valueOf(true));
        clientEndpoint.addNativeData(FTPConstants.PROPERTY_MAP, ftpConfig);
        return null;
    }

    public static Object get(BalEnv env, BObject clientConnector, String filePath) throws BallerinaFTPException {
        String url = FTPUtil.createUrl(clientConnector, filePath);
        Map<String, String> propertyMap = new HashMap<>(
                (Map<String, String>) clientConnector.getNativeData(FTPConstants.PROPERTY_MAP));
        propertyMap.put(FTPConstants.PROPERTY_URI, url);
        BalFuture balFuture = env.markAsync();
        FTPClientListener connectorListener = new FTPClientListener(balFuture,
                remoteFileSystemBaseMessage -> FTPClientHelper.executeGetAction(remoteFileSystemBaseMessage, balFuture));
        RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        VFSClientConnector connector;
        try {
            connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap, connectorListener);
        } catch (RemoteFileSystemConnectorException e) {
            throw new BallerinaFTPException(e.getMessage());
        }
        connector.send(null, FtpAction.GET);
        return null;
    }

    public static Object append(BalEnv env, BObject clientConnector, BMap<Object, Object> inputContent)
            throws BallerinaFTPException {
        try {
            String url = FTPUtil.createUrl(clientConnector, (inputContent.getStringValue(BStringUtils.fromString(
                    FTPConstants.INPUT_CONTENT_FILE_PATH_KEY))).getValue());
            Map<String, String> propertyMap = new HashMap<>(
                    (Map<String, String>) clientConnector.getNativeData(FTPConstants.PROPERTY_MAP));
            propertyMap.put(FTPConstants.PROPERTY_URI, url);

            boolean isFile = inputContent.getBooleanValue(BStringUtils.fromString(
                    FTPConstants.INPUT_CONTENT_IS_FILE_KEY));
            RemoteFileSystemMessage message;
            if (isFile) {
                BObject fileContent = inputContent.getObjectValue(BStringUtils.fromString(
                        FTPConstants.INPUT_CONTENT_FILE_CONTENT_KEY));
                Channel byteChannel = (Channel) fileContent.getNativeData(IOConstants.BYTE_CHANNEL_NAME);
                message = new RemoteFileSystemMessage(byteChannel.getInputStream());
            } else {
                String textContent = (inputContent.getStringValue(BStringUtils.fromString(
                        FTPConstants.INPUT_CONTENT_TEXT_CONTENT_KEY))).getValue();
                InputStream stream = new ByteArrayInputStream(textContent.getBytes());
                message = new RemoteFileSystemMessage(stream);
            }
            BalFuture balFuture = env.markAsync();
            FTPClientListener connectorListener = new FTPClientListener(balFuture,
                    remoteFileSystemBaseMessage -> FTPClientHelper.executeGenericAction(balFuture));
            RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
            VFSClientConnector connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap,
                    connectorListener);
            connector.send(message, FtpAction.APPEND);
            balFuture.complete(null);
        } catch (RemoteFileSystemConnectorException | IOException e) {
            throw new BallerinaFTPException(e.getMessage());
        }
        return null;
    }

    public static Object put(BalEnv env, BObject clientConnector, BMap<Object, Object> inputContent)
            throws BallerinaFTPException {
        Map<String, String> propertyMap = new HashMap<>(
                (Map<String, String>) clientConnector.getNativeData(FTPConstants.PROPERTY_MAP));
        boolean isFile = inputContent.getBooleanValue(BStringUtils.fromString(FTPConstants.INPUT_CONTENT_IS_FILE_KEY));
        boolean compressInput = inputContent.getBooleanValue(BStringUtils.fromString(
                FTPConstants.INPUT_CONTENT_COMPRESS_INPUT_KEY));
        InputStream stream = FTPClientHelper.getUploadStream(inputContent, isFile);
        RemoteFileSystemMessage message;
        ByteArrayInputStream compressedStream = null;

        try {
            if (stream != null) {
                if (compressInput) {
                    compressedStream = FTPUtil.compress(stream, (inputContent.getStringValue(BStringUtils.fromString(
                            FTPConstants.INPUT_CONTENT_FILE_PATH_KEY))).getValue());
                    if (compressedStream != null) {
                        message = FTPClientHelper.getCompressedMessage(clientConnector, (inputContent.getStringValue(
                                BStringUtils.fromString(FTPConstants.INPUT_CONTENT_FILE_PATH_KEY))).getValue(),
                                propertyMap, compressedStream);
                    } else {
                        throw new BallerinaFTPException("Error in compressing file");
                    }
                } else {
                    message = FTPClientHelper.getUncompressedMessage(clientConnector, (inputContent.getStringValue(
                            BStringUtils.fromString(FTPConstants.INPUT_CONTENT_FILE_PATH_KEY))).getValue(), propertyMap,
                            stream);
                }
            } else {
                throw new BallerinaFTPException("Error in reading file");
            }
            BalFuture balFuture = env.markAsync();
            FTPClientListener connectorListener = new FTPClientListener(balFuture, remoteFileSystemBaseMessage ->
                    FTPClientHelper.executeGenericAction(balFuture));
            RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();

            VFSClientConnector connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap,
                    connectorListener);
            connector.send(message, FtpAction.PUT);
            balFuture.complete(null);
        } catch (RemoteFileSystemConnectorException e) {
            throw new BallerinaFTPException(e.getMessage());
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
                if (compressedStream != null) {
                    compressedStream.close();
                }
            } catch (IOException e) {
                log.error("Error in closing stream");
            }
        }
        return null;
    }

    public static Object delete(BalEnv env, BObject clientConnector, String filePath) throws BallerinaFTPException {
        String url = FTPUtil.createUrl(clientConnector, filePath);
        Map<String, String> propertyMap = new HashMap<>(
                (Map<String, String>) clientConnector.getNativeData(FTPConstants.PROPERTY_MAP));
        propertyMap.put(FTPConstants.PROPERTY_URI, url);
        BalFuture balFuture = env.markAsync();
        FTPClientListener connectorListener = new FTPClientListener(balFuture,
                remoteFileSystemBaseMessage -> FTPClientHelper.executeGenericAction(balFuture));
        RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        VFSClientConnector connector;
        try {
            connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap, connectorListener);
        } catch (RemoteFileSystemConnectorException e) {
            throw new BallerinaFTPException(e.getMessage());
        }
        connector.send(null, FtpAction.DELETE);
        balFuture.complete(null);
        return null;
    }

    public static Object isDirectory(BalEnv env, BObject clientConnector, String filePath) throws BallerinaFTPException {
        String url = FTPUtil.createUrl(clientConnector, filePath);
        Map<String, String> propertyMap = new HashMap<>(
                (Map<String, String>) clientConnector.getNativeData(FTPConstants.PROPERTY_MAP));
        propertyMap.put(FTPConstants.PROPERTY_URI, url);
        BalFuture balFuture = env.markAsync();
        FTPClientListener connectorListener = new FTPClientListener(balFuture, remoteFileSystemBaseMessage ->
                FTPClientHelper.executeIsDirectoryAction(remoteFileSystemBaseMessage, balFuture));
        RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        VFSClientConnector connector;
        try {
            connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap, connectorListener);
        } catch (RemoteFileSystemConnectorException e) {
            throw new BallerinaFTPException(e.getMessage());
        }
        connector.send(null, FtpAction.ISDIR);
        return false;
    }

    public static Object list(BalEnv env, BObject clientConnector, String filePath) throws BallerinaFTPException {
        String url = FTPUtil.createUrl(clientConnector, filePath);
        Map<String, String> propertyMap = new HashMap<>(
                (Map<String, String>) clientConnector.getNativeData(FTPConstants.PROPERTY_MAP));
        propertyMap.put(FTPConstants.PROPERTY_URI, url);
        BalFuture balFuture = env.markAsync();
        FTPClientListener connectorListener = new FTPClientListener(balFuture, remoteFileSystemBaseMessage ->
                FTPClientHelper.executeListAction(remoteFileSystemBaseMessage, balFuture));
        RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        VFSClientConnector connector;
        try {
            connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap, connectorListener);
        } catch (RemoteFileSystemConnectorException e) {
            throw new BallerinaFTPException(e.getMessage());
        }
        connector.send(null, FtpAction.LIST);
        return null;
    }

    public static Object mkdir(BalEnv env, BObject clientConnector, String path) throws BallerinaFTPException {
        String url = FTPUtil.createUrl(clientConnector, path);
        Map<String, String> propertyMap = new HashMap<>(
                (Map<String, String>) clientConnector.getNativeData(FTPConstants.PROPERTY_MAP));
        propertyMap.put(FTPConstants.PROPERTY_URI, url);
        BalFuture balFuture = env.markAsync();
        FTPClientListener connectorListener = new FTPClientListener(balFuture,
                remoteFileSystemBaseMessage -> FTPClientHelper.executeGenericAction(balFuture));
        RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        VFSClientConnector connector;
        try {
            connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap, connectorListener);
        } catch (RemoteFileSystemConnectorException e) {
            throw new BallerinaFTPException(e.getMessage());
        }
        connector.send(null, FtpAction.MKDIR);
        balFuture.complete(null);
        return null;
    }

    public static Object rename(BalEnv env, BObject clientConnector, String origin, String destination)
            throws BallerinaFTPException {
        Map<String, String> propertyMap = new HashMap<>(
                (Map<String, String>) clientConnector.getNativeData(FTPConstants.PROPERTY_MAP));
        propertyMap.put(FTPConstants.PROPERTY_URI, FTPUtil.createUrl(clientConnector, origin));
        propertyMap.put(FTPConstants.PROPERTY_DESTINATION, FTPUtil.createUrl(clientConnector, destination));
        BalFuture balFuture = env.markAsync();
        FTPClientListener connectorListener = new FTPClientListener(balFuture,
                remoteFileSystemBaseMessage -> FTPClientHelper.executeGenericAction(balFuture));
        RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        VFSClientConnector connector;
        try {
            connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap, connectorListener);
        } catch (RemoteFileSystemConnectorException e) {
            throw new BallerinaFTPException(e.getMessage());
        }
        connector.send(null, FtpAction.RENAME);
        balFuture.complete(null);
        return null;
    }

    public static Object rmdir(BalEnv env, BObject clientConnector, String filePath) throws BallerinaFTPException {
        String url = FTPUtil.createUrl(clientConnector, filePath);
        Map<String, String> propertyMap = new HashMap<>(
                (Map<String, String>) clientConnector.getNativeData(FTPConstants.PROPERTY_MAP));
        propertyMap.put(FTPConstants.PROPERTY_URI, url);
        BalFuture balFuture = env.markAsync();
        FTPClientListener connectorListener = new FTPClientListener(balFuture,
                remoteFileSystemBaseMessage -> FTPClientHelper.executeGenericAction(balFuture));
        RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        VFSClientConnector connector;
        try {
            connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap, connectorListener);
        } catch (RemoteFileSystemConnectorException e) {
            throw new BallerinaFTPException(e.getMessage());
        }
        connector.send(null, FtpAction.RMDIR);
        balFuture.complete(null);
        return null;
    }

    public static Object size(BalEnv env, BObject clientConnector, String filePath) throws BallerinaFTPException {
        String url = FTPUtil.createUrl(clientConnector, filePath);
        Map<String, String> propertyMap = new HashMap<>(
                (Map<String, String>) clientConnector.getNativeData(FTPConstants.PROPERTY_MAP));
        propertyMap.put(FTPConstants.PROPERTY_URI, url);
        propertyMap.put(FTPConstants.FTP_PASSIVE_MODE, Boolean.TRUE.toString());
        BalFuture balFuture = env.markAsync();
        FTPClientListener connectorListener = new FTPClientListener(balFuture, remoteFileSystemBaseMessage ->
                FTPClientHelper.executeSizeAction(remoteFileSystemBaseMessage, balFuture));
        RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        VFSClientConnector connector;
        try {
            connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap, connectorListener);
        } catch (RemoteFileSystemConnectorException e) {
            throw new BallerinaFTPException(e.getMessage());
        }
        connector.send(null, FtpAction.SIZE);
        return 0;
    }

}
