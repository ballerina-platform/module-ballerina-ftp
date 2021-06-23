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
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.stdlib.ftp.util.BallerinaFtpException;
import org.ballerinalang.stdlib.ftp.util.FtpConstants;
import org.ballerinalang.stdlib.ftp.util.FtpUtil;
import org.ballerinalang.stdlib.io.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import static org.ballerinalang.stdlib.ftp.util.FtpConstants.ARRAY_SIZE;
import static org.ballerinalang.stdlib.ftp.util.FtpConstants.ENTITY_BYTE_STREAM;
import static org.ballerinalang.stdlib.ftp.util.FtpConstants.READ_INPUT_STREAM;
import static org.ballerinalang.stdlib.ftp.util.FtpUtil.ErrorType.Error;

/**
 * Contains functionality of FTP client.
 */
public class FtpClient {

    private static final Logger log = LoggerFactory.getLogger(FtpClient.class);

    private FtpClient() {
        // private constructor
    }

    public static Object initClientEndpoint(BObject clientEndpoint, BMap<Object, Object> config)
            throws BallerinaFtpException {
        String protocol = (config.getStringValue(StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_PROTOCOL)))
                .getValue();
        Map<String, String> authMap = FtpUtil.getAuthMap(config);
        clientEndpoint.addNativeData(FtpConstants.ENDPOINT_CONFIG_USERNAME,
                authMap.get(FtpConstants.ENDPOINT_CONFIG_USERNAME));
        clientEndpoint.addNativeData(FtpConstants.ENDPOINT_CONFIG_PASS_KEY,
                authMap.get(FtpConstants.ENDPOINT_CONFIG_PASS_KEY));
        clientEndpoint.addNativeData(FtpConstants.ENDPOINT_CONFIG_HOST,
                (config.getStringValue(StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_HOST))).getValue());
        clientEndpoint.addNativeData(FtpConstants.ENDPOINT_CONFIG_PORT,
                FtpUtil.extractPortValue(config.getIntValue(StringUtils.fromString(
                        FtpConstants.ENDPOINT_CONFIG_PORT))));
        clientEndpoint.addNativeData(FtpConstants.ENDPOINT_CONFIG_PROTOCOL, protocol);
        Map<String, String> ftpConfig = new HashMap<>(5);
        BMap auth = config.getMapValue(StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_AUTH));
        if (auth != null) {
            final BMap privateKey = auth.getMapValue(StringUtils.fromString(
                    FtpConstants.ENDPOINT_CONFIG_PRIVATE_KEY));
            if (privateKey != null) {
                final BString privateKeyPath = privateKey.getStringValue(StringUtils.fromString(
                        FtpConstants.ENDPOINT_CONFIG_PATH));
                ftpConfig.put(Constants.IDENTITY, privateKeyPath.getValue());
                final BString privateKeyPassword = privateKey.getStringValue(StringUtils.fromString(
                        FtpConstants.ENDPOINT_CONFIG_PASS_KEY));
                if (privateKeyPassword != null && !privateKeyPassword.getValue().isEmpty()) {
                    ftpConfig.put(Constants.IDENTITY_PASS_PHRASE, privateKeyPassword.getValue());
                }
            }
        }
        ftpConfig.put(FtpConstants.FTP_PASSIVE_MODE, String.valueOf(true));
        ftpConfig.put(FtpConstants.USER_DIR_IS_ROOT, String.valueOf(false));
        ftpConfig.put(FtpConstants.AVOID_PERMISSION_CHECK, String.valueOf(true));
        clientEndpoint.addNativeData(FtpConstants.PROPERTY_MAP, ftpConfig);
        return null;
    }

    public static Object get(Environment env, BObject clientConnector, BString filePath, long arraySize) {
        InputStream readInputStream = (InputStream) clientConnector.getNativeData(READ_INPUT_STREAM);
        if (readInputStream != null) {
            return FtpClientHelper.generateInputStreamEntry(readInputStream, arraySize);
        }
        clientConnector.addNativeData(ENTITY_BYTE_STREAM, null);
        clientConnector.addNativeData(ARRAY_SIZE, arraySize);
        String url;
        try {
            url = FtpUtil.createUrl(clientConnector, filePath.getValue());
        } catch (BallerinaFtpException e) {
            return FtpUtil.createError(e.getMessage(), e.getCause().getMessage(), Error.errorType());
        }
        Map<String, String> propertyMap = new HashMap<>(
                (Map<String, String>) clientConnector.getNativeData(FtpConstants.PROPERTY_MAP));
        propertyMap.put(FtpConstants.PROPERTY_URI, url);
        Future balFuture = env.markAsync();
        FtpClientListener connectorListener = new FtpClientListener(balFuture,
                remoteFileSystemBaseMessage -> FtpClientHelper.executeGetAction(remoteFileSystemBaseMessage,
                        balFuture, clientConnector));
        RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        VFSClientConnector connector;
        try {
            connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap, connectorListener);
        } catch (RemoteFileSystemConnectorException e) {
            return FtpUtil.createError(e.getMessage(), e.getCause().getMessage(), Error.errorType());
        }
        connector.send(null, FtpAction.GET);
        return null;
    }

    public static Object closeInputByteStream(BObject clientObject) {
        InputStream readInputStream = (InputStream) clientObject.getNativeData(READ_INPUT_STREAM);
        if (readInputStream != null) {
            try {
                readInputStream.close();
                clientObject.addNativeData(READ_INPUT_STREAM, null);
                clientObject.addNativeData(ENTITY_BYTE_STREAM, null);
                return null;
            } catch (IOException e) {
                return IOUtils.createError(e);
            }
        } else {
            return null;
        }
    }

    public static Object append(Environment env, BObject clientConnector, BMap<Object, Object> inputContent)
            throws BallerinaFtpException {
        try {
            String url = FtpUtil.createUrl(clientConnector, (inputContent.getStringValue(StringUtils.fromString(
                    FtpConstants.INPUT_CONTENT_FILE_PATH_KEY))).getValue());
            Map<String, String> propertyMap = new HashMap<>(
                    (Map<String, String>) clientConnector.getNativeData(FtpConstants.PROPERTY_MAP));
            propertyMap.put(FtpConstants.PROPERTY_URI, url);

            boolean isFile = inputContent.getBooleanValue(StringUtils.fromString(
                    FtpConstants.INPUT_CONTENT_IS_FILE_KEY));
            RemoteFileSystemMessage message;
            if (isFile) {
                InputStream stream = FtpClientHelper.getUploadStream(env, clientConnector, inputContent, true);
                message = new RemoteFileSystemMessage(stream);
            } else {
                String textContent = (inputContent.getStringValue(StringUtils.fromString(
                        FtpConstants.INPUT_CONTENT_TEXT_CONTENT_KEY))).getValue();
                InputStream stream = new ByteArrayInputStream(textContent.getBytes());
                message = new RemoteFileSystemMessage(stream);
            }
            Future balFuture = env.markAsync();
            FtpClientListener connectorListener = new FtpClientListener(balFuture,
                    remoteFileSystemBaseMessage -> FtpClientHelper.executeGenericAction(balFuture));
            RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
            VFSClientConnector connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap,
                    connectorListener);
            connector.send(message, FtpAction.APPEND);
            balFuture.complete(null);
        } catch (RemoteFileSystemConnectorException e) {
            throw new BallerinaFtpException(e.getMessage());
        }
        return null;
    }

    public static Object put(Environment env, BObject clientConnector, BMap<Object, Object> inputContent)
            throws BallerinaFtpException {
        Map<String, String> propertyMap = new HashMap<>(
                (Map<String, String>) clientConnector.getNativeData(FtpConstants.PROPERTY_MAP));
        boolean isFile = inputContent.getBooleanValue(StringUtils.fromString(FtpConstants.INPUT_CONTENT_IS_FILE_KEY));
        boolean compressInput = inputContent.getBooleanValue(StringUtils.fromString(
                FtpConstants.INPUT_CONTENT_COMPRESS_INPUT_KEY));
        InputStream stream = FtpClientHelper.getUploadStream(env, clientConnector, inputContent, isFile);
        RemoteFileSystemMessage message;
        ByteArrayInputStream compressedStream = null;

        try {
            if (stream != null) {
                if (compressInput) {
                    compressedStream = FtpUtil.compress(stream, (inputContent.getStringValue(StringUtils.fromString(
                            FtpConstants.INPUT_CONTENT_FILE_PATH_KEY))).getValue());
                    if (compressedStream != null) {
                        message = FtpClientHelper.getCompressedMessage(clientConnector, (inputContent.getStringValue(
                                StringUtils.fromString(FtpConstants.INPUT_CONTENT_FILE_PATH_KEY))).getValue(),
                                propertyMap, compressedStream);
                    } else {
                        throw new BallerinaFtpException("Error in compressing file");
                    }
                } else {
                    message = FtpClientHelper.getUncompressedMessage(clientConnector, (inputContent.getStringValue(
                            StringUtils.fromString(FtpConstants.INPUT_CONTENT_FILE_PATH_KEY))).getValue(), propertyMap,
                            stream);
                }
            } else {
                throw new BallerinaFtpException("Error in reading file");
            }
            Future balFuture = env.markAsync();
            FtpClientListener connectorListener = new FtpClientListener(balFuture, remoteFileSystemBaseMessage ->
                    FtpClientHelper.executeGenericAction(balFuture));
            RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();

            VFSClientConnector connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap,
                    connectorListener);
            connector.send(message, FtpAction.PUT);
            balFuture.complete(null);
        } catch (RemoteFileSystemConnectorException e) {
            throw new BallerinaFtpException(e.getMessage());
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

    public static Object delete(Environment env, BObject clientConnector, BString filePath)
            throws BallerinaFtpException {
        String url = FtpUtil.createUrl(clientConnector, filePath.getValue());
        Map<String, String> propertyMap = new HashMap<>(
                (Map<String, String>) clientConnector.getNativeData(FtpConstants.PROPERTY_MAP));
        propertyMap.put(FtpConstants.PROPERTY_URI, url);
        Future balFuture = env.markAsync();
        FtpClientListener connectorListener = new FtpClientListener(balFuture,
                remoteFileSystemBaseMessage -> FtpClientHelper.executeGenericAction(balFuture));
        RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        VFSClientConnector connector;
        try {
            connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap, connectorListener);
        } catch (RemoteFileSystemConnectorException e) {
            throw new BallerinaFtpException(e.getMessage());
        }
        connector.send(null, FtpAction.DELETE);
        balFuture.complete(null);
        return null;
    }

    public static Object isDirectory(Environment env, BObject clientConnector, BString filePath)
            throws BallerinaFtpException {
        String url = FtpUtil.createUrl(clientConnector, filePath.getValue());
        Map<String, String> propertyMap = new HashMap<>(
                (Map<String, String>) clientConnector.getNativeData(FtpConstants.PROPERTY_MAP));
        propertyMap.put(FtpConstants.PROPERTY_URI, url);
        Future balFuture = env.markAsync();
        FtpClientListener connectorListener = new FtpClientListener(balFuture, remoteFileSystemBaseMessage ->
                FtpClientHelper.executeIsDirectoryAction(remoteFileSystemBaseMessage, balFuture));
        RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        VFSClientConnector connector;
        try {
            connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap, connectorListener);
        } catch (RemoteFileSystemConnectorException e) {
            throw new BallerinaFtpException(e.getMessage());
        }
        connector.send(null, FtpAction.ISDIR);
        return false;
    }

    public static Object list(Environment env, BObject clientConnector, BString filePath) throws BallerinaFtpException {
        String url = FtpUtil.createUrl(clientConnector, filePath.getValue());
        Map<String, String> propertyMap = new HashMap<>(
                (Map<String, String>) clientConnector.getNativeData(FtpConstants.PROPERTY_MAP));
        propertyMap.put(FtpConstants.PROPERTY_URI, url);
        Future balFuture = env.markAsync();
        FtpClientListener connectorListener = new FtpClientListener(balFuture, remoteFileSystemBaseMessage ->
                FtpClientHelper.executeListAction(remoteFileSystemBaseMessage, balFuture));
        RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        VFSClientConnector connector;
        try {
            connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap, connectorListener);
        } catch (RemoteFileSystemConnectorException e) {
            throw new BallerinaFtpException(e.getMessage());
        }
        connector.send(null, FtpAction.LIST);
        return null;
    }

    public static Object mkdir(Environment env, BObject clientConnector, BString path) throws BallerinaFtpException {
        String url = FtpUtil.createUrl(clientConnector, path.getValue());
        Map<String, String> propertyMap = new HashMap<>(
                (Map<String, String>) clientConnector.getNativeData(FtpConstants.PROPERTY_MAP));
        propertyMap.put(FtpConstants.PROPERTY_URI, url);
        Future balFuture = env.markAsync();
        FtpClientListener connectorListener = new FtpClientListener(balFuture,
                remoteFileSystemBaseMessage -> FtpClientHelper.executeGenericAction(balFuture));
        RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        VFSClientConnector connector;
        try {
            connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap, connectorListener);
        } catch (RemoteFileSystemConnectorException e) {
            throw new BallerinaFtpException(e.getMessage());
        }
        connector.send(null, FtpAction.MKDIR);
        balFuture.complete(null);
        return null;
    }

    public static Object rename(Environment env, BObject clientConnector, BString origin, BString destination)
            throws BallerinaFtpException {
        Map<String, String> propertyMap = new HashMap<>(
                (Map<String, String>) clientConnector.getNativeData(FtpConstants.PROPERTY_MAP));
        propertyMap.put(FtpConstants.PROPERTY_URI, FtpUtil.createUrl(clientConnector, origin.getValue()));
        propertyMap.put(FtpConstants.PROPERTY_DESTINATION, FtpUtil.createUrl(clientConnector, destination.getValue()));
        Future balFuture = env.markAsync();
        FtpClientListener connectorListener = new FtpClientListener(balFuture,
                remoteFileSystemBaseMessage -> FtpClientHelper.executeGenericAction(balFuture));
        RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        VFSClientConnector connector;
        try {
            connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap, connectorListener);
        } catch (RemoteFileSystemConnectorException e) {
            throw new BallerinaFtpException(e.getMessage());
        }
        connector.send(null, FtpAction.RENAME);
        balFuture.complete(null);
        return null;
    }

    public static Object rmdir(Environment env, BObject clientConnector, BString filePath)
            throws BallerinaFtpException {
        String url = FtpUtil.createUrl(clientConnector, filePath.getValue());
        Map<String, String> propertyMap = new HashMap<>(
                (Map<String, String>) clientConnector.getNativeData(FtpConstants.PROPERTY_MAP));
        propertyMap.put(FtpConstants.PROPERTY_URI, url);
        Future balFuture = env.markAsync();
        FtpClientListener connectorListener = new FtpClientListener(balFuture,
                remoteFileSystemBaseMessage -> FtpClientHelper.executeGenericAction(balFuture));
        RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        VFSClientConnector connector;
        try {
            connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap, connectorListener);
        } catch (RemoteFileSystemConnectorException e) {
            throw new BallerinaFtpException(e.getMessage());
        }
        connector.send(null, FtpAction.RMDIR);
        balFuture.complete(null);
        return null;
    }

    public static Object size(Environment env, BObject clientConnector, BString filePath) throws BallerinaFtpException {
        String url = FtpUtil.createUrl(clientConnector, filePath.getValue());
        Map<String, String> propertyMap = new HashMap<>(
                (Map<String, String>) clientConnector.getNativeData(FtpConstants.PROPERTY_MAP));
        propertyMap.put(FtpConstants.PROPERTY_URI, url);
        propertyMap.put(FtpConstants.FTP_PASSIVE_MODE, Boolean.TRUE.toString());
        Future balFuture = env.markAsync();
        FtpClientListener connectorListener = new FtpClientListener(balFuture, remoteFileSystemBaseMessage ->
                FtpClientHelper.executeSizeAction(remoteFileSystemBaseMessage, balFuture));
        RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        VFSClientConnector connector;
        try {
            connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap, connectorListener);
        } catch (RemoteFileSystemConnectorException e) {
            throw new BallerinaFtpException(e.getMessage());
        }
        connector.send(null, FtpAction.SIZE);
        return 0;
    }
}
