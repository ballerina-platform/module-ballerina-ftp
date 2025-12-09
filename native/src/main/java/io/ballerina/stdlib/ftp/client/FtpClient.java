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
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.types.PredefinedTypes;
import io.ballerina.runtime.api.types.TypeTags;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BStream;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BTypedesc;
import io.ballerina.runtime.api.values.BXml;
import io.ballerina.stdlib.ftp.exception.BallerinaFtpException;
import io.ballerina.stdlib.ftp.exception.RemoteFileSystemConnectorException;
import io.ballerina.stdlib.ftp.transport.RemoteFileSystemConnectorFactory;
import io.ballerina.stdlib.ftp.transport.client.connector.contract.FtpAction;
import io.ballerina.stdlib.ftp.transport.client.connector.contract.VfsClientConnector;
import io.ballerina.stdlib.ftp.transport.client.connector.contractimpl.VfsClientConnectorImpl;
import io.ballerina.stdlib.ftp.transport.impl.RemoteFileSystemConnectorFactoryImpl;
import io.ballerina.stdlib.ftp.transport.message.RemoteFileSystemBaseMessage;
import io.ballerina.stdlib.ftp.transport.message.RemoteFileSystemMessage;
import io.ballerina.stdlib.ftp.util.BufferHolder;
import io.ballerina.stdlib.ftp.util.CSVUtils;
import io.ballerina.stdlib.ftp.util.FtpConstants;
import io.ballerina.stdlib.ftp.util.FtpUtil;
import io.ballerina.stdlib.io.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static io.ballerina.stdlib.ftp.util.FtpConstants.ENDPOINT_CONFIG_PREFERRED_METHODS;
import static io.ballerina.stdlib.ftp.util.FtpConstants.ENTITY_BYTE_STREAM;
import static io.ballerina.stdlib.ftp.util.FtpConstants.FTP_ERROR;
import static io.ballerina.stdlib.ftp.util.FtpConstants.READ_INPUT_STREAM;
import static io.ballerina.stdlib.ftp.util.FtpConstants.VFS_CLIENT_CONNECTOR;
import static io.ballerina.stdlib.ftp.util.FtpContentConverter.convertBytesToCsv;
import static io.ballerina.stdlib.ftp.util.FtpContentConverter.convertBytesToJson;
import static io.ballerina.stdlib.ftp.util.FtpContentConverter.convertBytesToString;
import static io.ballerina.stdlib.ftp.util.FtpContentConverter.convertBytesToXml;
import static io.ballerina.stdlib.ftp.util.FtpContentConverter.convertToBallerinaByteArray;
import static io.ballerina.stdlib.ftp.util.FtpUtil.ErrorType.Error;
import static io.ballerina.stdlib.ftp.util.FtpUtil.extractCompressionConfiguration;
import static io.ballerina.stdlib.ftp.util.FtpUtil.extractFileTransferConfiguration;
import static io.ballerina.stdlib.ftp.util.FtpUtil.extractKnownHostsConfiguration;
import static io.ballerina.stdlib.ftp.util.FtpUtil.extractProxyConfiguration;
import static io.ballerina.stdlib.ftp.util.FtpUtil.extractTimeoutConfigurations;
import static io.ballerina.stdlib.ftp.util.FtpUtil.findRootCause;

/**
 * Contains functionality of FTP client.
 */
public class FtpClient {

    private static final Logger log = LoggerFactory.getLogger(FtpClient.class);

    private FtpClient() {
        // private constructor
    }

    public static Object initClientEndpoint(BObject clientEndpoint, BMap<Object, Object> config) {
        String protocol = extractProtocol(config);
        configureClientEndpointBasic(clientEndpoint, config, protocol);
        
        Map<String, String> ftpConfig = new HashMap<>(20);
        Object authError = configureAuthentication(config, protocol, ftpConfig);
        if (authError != null) {
            return authError;
        }
        
        applyDefaultFtpConfig(config, ftpConfig);
        Object vfsError = extractVfsConfigurations(config, ftpConfig);
        if (vfsError != null) {
            return vfsError;
        }
        
        return createAndStoreConnector(clientEndpoint, ftpConfig);
    }

    /**
     * Extracts the protocol from the configuration.
     * 
     * @param config The client configuration map
     * @return The protocol string
     */
    private static String extractProtocol(BMap<Object, Object> config) {
        return (config.getStringValue(StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_PROTOCOL))).getValue();
    }

    /**
     * Configures basic client endpoint settings (host, port, auth, etc.).
     * 
     * @param clientEndpoint The client endpoint object
     * @param config The client configuration map
     * @param protocol The protocol being used
     */
    private static void configureClientEndpointBasic(BObject clientEndpoint, BMap<Object, Object> config, 
                                                     String protocol) {
        clientEndpoint.addNativeData(FtpConstants.ENDPOINT_CONFIG_LAX_DATABINDING,
                config.getBooleanValue(StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_LAX_DATABINDING)));

        Map<String, String> authMap = FtpUtil.getAuthMap(config, protocol);
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
    }

    /**
     * Configures authentication settings including validation and protocol-specific setup.
     * 
     * @param config The client configuration map
     * @param protocol The protocol being used
     * @param ftpConfig The FTP configuration map to populate
     * @return Error object if configuration fails, null otherwise
     */
    private static Object configureAuthentication(BMap<Object, Object> config, String protocol, 
                                                   Map<String, String> ftpConfig) {
        BMap auth = config.getMapValue(StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_AUTH));
        if (auth == null) {
            return null;
        }
        
        Object validationError = validateAuthProtocolCombination(auth, protocol);
        if (validationError != null) {
            return validationError;
        }
        
        configurePrivateKey(auth, ftpConfig);
        
        if (auth.getMapValue(StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_SECURE_SOCKET)) != null 
                && protocol.equals(FtpConstants.SCHEME_FTPS)) {
            Object ftpsError = configureFtpsSecureSocket(
                    auth.getMapValue(StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_SECURE_SOCKET)), 
                    ftpConfig);
            if (ftpsError != null) {
                return ftpsError;
            }
        }
        
        if (protocol.equals(FtpConstants.SCHEME_SFTP)) {
            ftpConfig.put(ENDPOINT_CONFIG_PREFERRED_METHODS, 
                    FtpUtil.getPreferredMethodsFromAuthConfig(auth));
        }
        
        return null;
    }

    /**
     * Validates that protocol and authentication method combinations are correct.
     * 
     * @param auth The authentication configuration map
     * @param protocol The protocol being used (SFTP or FTPS)
     * @return Error object if validation fails, null otherwise
     */
    private static Object validateAuthProtocolCombination(BMap auth, String protocol) {
        final BMap privateKey = auth.getMapValue(StringUtils.fromString(
                FtpConstants.ENDPOINT_CONFIG_PRIVATE_KEY));
        final BMap secureSocket = auth.getMapValue(StringUtils.fromString(
                FtpConstants.ENDPOINT_CONFIG_SECURE_SOCKET));
        
        if (privateKey != null && protocol.equals(FtpConstants.SCHEME_FTPS)) {
            return FtpUtil.createError("privateKey can only be used with SFTP protocol. " +
                    "For FTPS, use secureSocket configuration.", Error.errorType());
        }
        
        if (secureSocket != null && protocol.equals(FtpConstants.SCHEME_SFTP)) {
            return FtpUtil.createError("secureSocket can only be used with FTPS protocol. " +
                    "For SFTP, use privateKey configuration.", Error.errorType());
        }
        
        return null;
    }

    /**
     * Configures private key authentication for SFTP.
     * 
     * @param auth The authentication configuration map
     * @param ftpConfig The FTP configuration map to populate
     */
    private static void configurePrivateKey(BMap auth, Map<String, String> ftpConfig) {
        final BMap privateKey = auth.getMapValue(StringUtils.fromString(
                FtpConstants.ENDPOINT_CONFIG_PRIVATE_KEY));
        
        if (privateKey == null) {
            return;
        }
        
        final BString privateKeyPath = privateKey.getStringValue(StringUtils.fromString(
                FtpConstants.ENDPOINT_CONFIG_KEY_PATH));
        ftpConfig.put(FtpConstants.IDENTITY, privateKeyPath.getValue());
        
        final BString privateKeyPassword = privateKey.getStringValue(StringUtils.fromString(
                FtpConstants.ENDPOINT_CONFIG_PASS_KEY));
        if (privateKeyPassword != null && !privateKeyPassword.getValue().isEmpty()) {
            ftpConfig.put(FtpConstants.IDENTITY_PASS_PHRASE, privateKeyPassword.getValue());
        }
    }

    /**
     * Applies default FTP configuration values.
     * 
     * @param config The client configuration map
     * @param ftpConfig The FTP configuration map to populate
     */
    private static void applyDefaultFtpConfig(BMap<Object, Object> config, Map<String, String> ftpConfig) {
        ftpConfig.put(FtpConstants.PASSIVE_MODE, String.valueOf(true));
        boolean userDirIsRoot = config.getBooleanValue(FtpConstants.USER_DIR_IS_ROOT_FIELD);
        ftpConfig.put(FtpConstants.USER_DIR_IS_ROOT, String.valueOf(userDirIsRoot));
        ftpConfig.put(FtpConstants.AVOID_PERMISSION_CHECK, String.valueOf(true));
    }

    /**
     * Extracts VFS-related configurations (timeout, file transfer, compression, known hosts, proxy).
     * 
     * @param config The client configuration map
     * @param ftpConfig The FTP configuration map to populate
     * @return Error object if extraction fails, null otherwise
     */
    private static Object extractVfsConfigurations(BMap<Object, Object> config, Map<String, String> ftpConfig) {
        try {
            extractTimeoutConfigurations(config, ftpConfig);
            extractFileTransferConfiguration(config, ftpConfig);
            extractCompressionConfiguration(config, ftpConfig);
            extractKnownHostsConfiguration(config, ftpConfig);
            extractProxyConfiguration(config, ftpConfig);
            return null;
        } catch (BallerinaFtpException e) {
            return FtpUtil.createError(e.getMessage(), Error.errorType());
        }
    }

    /**
     * Creates the FTP URL, stores configuration, and creates the VFS client connector.
     * 
     * @param clientEndpoint The client endpoint object
     * @param ftpConfig The FTP configuration map
     * @return Error object if creation fails, null otherwise
     */
    private static Object createAndStoreConnector(BObject clientEndpoint, Map<String, String> ftpConfig) {
        String url;
        try {
            url = FtpUtil.createUrl(clientEndpoint, "");
        } catch (BallerinaFtpException e) {
            return FtpUtil.createError(e.getMessage(), Error.errorType());
        }
        
        ftpConfig.put(FtpConstants.URI, url);
        clientEndpoint.addNativeData(FtpConstants.PROPERTY_MAP, ftpConfig);
        
        RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        try {
            VfsClientConnector connector = fileSystemConnectorFactory.createVfsClientConnector(ftpConfig);
            clientEndpoint.addNativeData(VFS_CLIENT_CONNECTOR, connector);
            return null;
        } catch (RemoteFileSystemConnectorException e) {
            return FtpUtil.createError(e.getMessage(), findRootCause(e), Error.errorType());
        }
    }

    /**
     * Configures secure socket settings for FTPS protocol.
     * 
     * @param secureSocket The secure socket configuration map
     * @param ftpConfig The FTP configuration map to populate
     * @return Error object if configuration fails, null otherwise
     */
    private static Object configureFtpsSecureSocket(BMap secureSocket, Map<String, String> ftpConfig) {
        configureFtpsMode(secureSocket, ftpConfig);
        configureFtpsDataChannelProtection(secureSocket, ftpConfig);
        configureFtpsHostnameVerification(secureSocket, ftpConfig);
        extractAndConfigureStore(secureSocket, FtpConstants.SECURE_SOCKET_KEY, 
                FtpConstants.ENDPOINT_CONFIG_KEYSTORE_PATH, 
                FtpConstants.ENDPOINT_CONFIG_KEYSTORE_PASSWORD, 
                ftpConfig);
        extractAndConfigureStore(secureSocket, FtpConstants.SECURE_SOCKET_TRUSTSTORE, 
                FtpConstants.ENDPOINT_CONFIG_TRUSTSTORE_PATH, 
                FtpConstants.ENDPOINT_CONFIG_TRUSTSTORE_PASSWORD, 
                ftpConfig);
        return null;
    }

    /**
     * Configures FTPS mode (IMPLICIT or EXPLICIT).
     * 
     * @param secureSocket The secure socket configuration map
     * @param ftpConfig The FTP configuration map to populate
     */
    private static void configureFtpsMode(BMap secureSocket, Map<String, String> ftpConfig) {
        final BString mode = secureSocket.getStringValue(StringUtils.fromString(
                FtpConstants.ENDPOINT_CONFIG_FTPS_MODE));
        
        if (mode != null && !mode.getValue().isEmpty()) {
            ftpConfig.put(FtpConstants.ENDPOINT_CONFIG_FTPS_MODE, mode.getValue());
        } else {
            // Default to EXPLICIT if not specified
            ftpConfig.put(FtpConstants.ENDPOINT_CONFIG_FTPS_MODE, FtpConstants.FTPS_MODE_EXPLICIT);
        }
    }

    /**
     * Configures FTPS data channel protection level.
     * 
     * @param secureSocket The secure socket configuration map
     * @param ftpConfig The FTP configuration map to populate
     */
    private static void configureFtpsDataChannelProtection(BMap secureSocket, Map<String, String> ftpConfig) {
        final BString dataChannelProtection = secureSocket.getStringValue(StringUtils.fromString(
                FtpConstants.ENDPOINT_CONFIG_FTPS_DATA_CHANNEL_PROTECTION));
        
        if (dataChannelProtection != null && !dataChannelProtection.getValue().isEmpty()) {
            ftpConfig.put(FtpConstants.ENDPOINT_CONFIG_FTPS_DATA_CHANNEL_PROTECTION, 
                    dataChannelProtection.getValue());
        } else {
            // Default to PRIVATE (secure) if not specified
            ftpConfig.put(FtpConstants.ENDPOINT_CONFIG_FTPS_DATA_CHANNEL_PROTECTION, 
                    FtpConstants.FTPS_DATA_CHANNEL_PROTECTION_PRIVATE);
        }
    }

    /**
     * Configures FTPS hostname verification.
     * 
     * @param secureSocket The secure socket configuration map
     * @param ftpConfig The FTP configuration map to populate
     */
    private static void configureFtpsHostnameVerification(BMap secureSocket, Map<String, String> ftpConfig) {
        Object verifyHostnameObj = secureSocket.get(StringUtils.fromString(
                FtpConstants.ENDPOINT_CONFIG_FTPS_VERIFY_HOSTNAME));
        
        boolean verifyHostname = true; // Default to secure
        if (verifyHostnameObj != null) {
            if (verifyHostnameObj instanceof Boolean) {
                verifyHostname = (Boolean) verifyHostnameObj;
            } else if (verifyHostnameObj instanceof BString) {
                verifyHostname = Boolean.parseBoolean(((BString) verifyHostnameObj).getValue());
            }
        }
        
        ftpConfig.put(FtpConstants.ENDPOINT_CONFIG_FTPS_VERIFY_HOSTNAME, String.valueOf(verifyHostname));
    }

    /**
     * Extracts a store (KeyStore or TrustStore) from secureSocket configuration and adds it to ftpConfig.
     * Handles both BMap and BObject representations.
     * 
     * @param secureSocket The secure socket configuration map
     * @param storeKey The key name ("key" for KeyStore, "trustStore" for TrustStore)
     * @param pathConfigKey The configuration key for the store path
     * @param passwordConfigKey The configuration key for the store password
     * @param ftpConfig The FTP configuration map to populate
     */
    private static void extractAndConfigureStore(BMap secureSocket, String storeKey, 
                                                  String pathConfigKey, String passwordConfigKey,
                                                  Map<String, String> ftpConfig) {
        Object storeObj = getStoreObject(secureSocket, storeKey);
        if (storeObj == null) {
            return;
        }
        
        Map<String, String> storeInfo = FtpUtil.extractKeyStoreInfo(storeObj);
        if (storeInfo.isEmpty()) {
            return;
        }
        
        String storePath = storeInfo.get(FtpConstants.KEYSTORE_PATH_KEY);
        String storePassword = storeInfo.get(FtpConstants.KEYSTORE_PASSWORD_KEY);
        
        if (storePath != null && !storePath.isEmpty()) {
            ftpConfig.put(pathConfigKey, storePath);
        }
        
        if (storePassword != null && !storePassword.isEmpty()) {
            ftpConfig.put(passwordConfigKey, storePassword);
        }
    }

    /**
     * Attempts to retrieve a store object from secureSocket using multiple methods.
     * Handles both BMap and BObject representations.
     * 
     * @param secureSocket The secure socket configuration map
     * @param storeKey The key name ("key" or "trustStore")
     * @return The store object, or null if not found
     */
    private static Object getStoreObject(BMap secureSocket, String storeKey) {
        BString keyString = StringUtils.fromString(storeKey);
        Object storeObj = secureSocket.get(keyString);
        if (storeObj != null) {
            return storeObj;
        }
        
        storeObj = secureSocket.getMapValue(keyString);
        if (storeObj != null) {
            return storeObj;
        }
        
        return secureSocket.getObjectValue(keyString);
    }

    /**
     * @deprecated : use typed getters like getBytes/getText/getJson/getXml/getCsv or their streaming variants.
     */
    @Deprecated
    public static Object getFirst(Environment env, BObject clientConnector, BString filePath) {
        clientConnector.addNativeData(ENTITY_BYTE_STREAM, null);
        return env.yieldAndRun(() -> {
            CompletableFuture<Object> balFuture = new CompletableFuture<>();
            FtpClientListener connectorListener = new FtpClientListener(balFuture, false,
                    remoteFileSystemBaseMessage -> FtpClientHelper.executeGetAction(remoteFileSystemBaseMessage,
                            balFuture, clientConnector));
            VfsClientConnectorImpl connector = (VfsClientConnectorImpl) clientConnector.
                    getNativeData(VFS_CLIENT_CONNECTOR);
            connector.addListener(connectorListener);
            connector.send(null, FtpAction.GET, filePath.getValue(), null);
            return getResult(balFuture);
        });
    }

    /**
     * @deprecated : use getBytesAsStream or getCsvAsStream instead of this legacy accessor.
     */
    @Deprecated
    public static Object get(BObject clientConnector) {
        return FtpClientHelper.generateInputStreamEntry((InputStream) clientConnector.getNativeData(READ_INPUT_STREAM));
    }

    public static Object getBytes(Environment env, BObject clientConnector, BString filePath) {
        Object content = getAllContent(env, clientConnector, filePath);
        if (!(content instanceof byte[])) {
            return content;
        }
        return convertToBallerinaByteArray((byte[]) content);
    }

    public static Object getText(Environment env, BObject clientConnector, BString filePath) {
        Object content = getAllContent(env, clientConnector, filePath);
        if (!(content instanceof byte[])) {
            return content;
        }
        return convertBytesToString((byte[]) content);
    }

    public static Object getJson(Environment env, BObject clientConnector, BString filePath, BTypedesc typeDesc) {
        Object content = getAllContent(env, clientConnector, filePath);
        if (!(content instanceof byte[])) {
            return content;
        }

        boolean laxDataBinding = (boolean) clientConnector.getNativeData(FtpConstants.ENDPOINT_CONFIG_LAX_DATABINDING);
        return convertBytesToJson((byte[]) content, typeDesc.getDescribingType(), laxDataBinding);
    }

    public static Object getXml(Environment env, BObject clientConnector, BString filePath, BTypedesc typeDesc) {
        Object content = getAllContent(env, clientConnector, filePath);
        if (!(content instanceof byte[])) {
            return content;
        }
        boolean laxDataBinding = (boolean) clientConnector.getNativeData(FtpConstants.ENDPOINT_CONFIG_LAX_DATABINDING);
        return convertBytesToXml((byte[]) content, typeDesc.getDescribingType(), laxDataBinding);
    }

    public static Object getCsv(Environment env, BObject clientConnector, BString filePath, BTypedesc typeDesc) {
        Object content = getAllContent(env, clientConnector, filePath);
        if (!(content instanceof byte[])) {
            return content;
        }

        boolean laxDataBinding = (boolean) clientConnector.getNativeData(FtpConstants.ENDPOINT_CONFIG_LAX_DATABINDING);
        return convertBytesToCsv((byte[]) content, typeDesc.getDescribingType(), laxDataBinding);
    }

    public static Object getBytesAsStream(Environment env, BObject clientConnector, BString filePath) {
        boolean laxDataBinding = (boolean) clientConnector.getNativeData(FtpConstants.ENDPOINT_CONFIG_LAX_DATABINDING);
        return env.yieldAndRun(() -> {
            CompletableFuture<Object> balFuture = new CompletableFuture<>();
            FtpClientListener connectorListener = new FtpClientListener(balFuture, false,
                    remoteFileSystemBaseMessage ->
                            FtpClientHelper.executeStreamingAction(remoteFileSystemBaseMessage,
                                    balFuture, TypeCreator.createArrayType(PredefinedTypes.TYPE_BYTE), laxDataBinding));
            VfsClientConnectorImpl connector = (VfsClientConnectorImpl) clientConnector.
                    getNativeData(VFS_CLIENT_CONNECTOR);
            connector.addListener(connectorListener);
            connector.send(null, FtpAction.GET, filePath.getValue(), null);
            return getResult(balFuture);
        });
    }

    public static Object getCsvAsStream(Environment env, BObject clientConnector, BString filePath,
                                        BTypedesc typeDesc) {
        boolean laxDataBinding = (boolean) clientConnector.getNativeData(FtpConstants.ENDPOINT_CONFIG_LAX_DATABINDING);
        return env.yieldAndRun(() -> {
            CompletableFuture<Object> balFuture = new CompletableFuture<>();
            FtpClientListener connectorListener = new FtpClientListener(balFuture, false,
                    remoteFileSystemBaseMessage ->
                            FtpClientHelper.executeStreamingAction(remoteFileSystemBaseMessage,
                                    balFuture, typeDesc.getDescribingType(), laxDataBinding));
            VfsClientConnectorImpl connector = (VfsClientConnectorImpl) clientConnector.
                    getNativeData(VFS_CLIENT_CONNECTOR);
            connector.addListener(connectorListener);
            connector.send(null, FtpAction.GET, filePath.getValue(), null);
            return getResult(balFuture);
        });
    }

    private static Object getAllContent(Environment env, BObject clientConnector, BString filePath) {
        return env.yieldAndRun(() -> {
            CompletableFuture<Object> balFuture = new CompletableFuture<>();
            FtpClientListener connectorListener = new FtpClientListener(balFuture, false,
                    remoteFileSystemBaseMessage -> FtpClientHelper.executeGetAllAction(remoteFileSystemBaseMessage,
                            balFuture));
            VfsClientConnectorImpl connector = (VfsClientConnectorImpl) clientConnector.
                    getNativeData(VFS_CLIENT_CONNECTOR);
            connector.addListener(connectorListener);
            connector.send(null, FtpAction.GET_ALL, filePath.getValue(), null);
            return getResult(balFuture);
        });
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

    /**
     * @deprecated : use putBytes/putText/putJson/putXml/putCsv or the streaming variants with APPEND option.
     */
    @Deprecated
    public static Object append(Environment env, BObject clientConnector, BMap<Object, Object> inputContent) {
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
        return env.yieldAndRun(() -> {
            CompletableFuture<Object> balFuture = new CompletableFuture<>();
            FtpClientListener connectorListener = new FtpClientListener(balFuture, true,
                    remoteFileSystemBaseMessage -> FtpClientHelper.executeGenericAction());
            VfsClientConnectorImpl connector
                    = (VfsClientConnectorImpl) clientConnector.getNativeData(VFS_CLIENT_CONNECTOR);
            connector.addListener(connectorListener);
            connector.send(message, FtpAction.APPEND, (inputContent.getStringValue(StringUtils.fromString(
                    FtpConstants.INPUT_CONTENT_FILE_PATH_KEY))).getValue(), null);
            return getResult(balFuture);
        });
    }

    /**
     * @deprecated : use typed put methods (putBytes/putText/putJson/putXml/putCsv) instead.
     */
    @Deprecated
    public static Object put(Environment env, BObject clientConnector, BMap<Object, Object> inputContent) {
        Map<String, String> propertyMap = new HashMap<>(
                (Map<String, String>) clientConnector.getNativeData(FtpConstants.PROPERTY_MAP));
        boolean isFile = inputContent.getBooleanValue(StringUtils.fromString(FtpConstants.INPUT_CONTENT_IS_FILE_KEY));
        boolean compressInput = inputContent.getBooleanValue(StringUtils.fromString(
                FtpConstants.INPUT_CONTENT_COMPRESS_INPUT_KEY));
        InputStream stream = FtpClientHelper.getUploadStream(env, clientConnector, inputContent, isFile);
        RemoteFileSystemMessage message;
        ByteArrayInputStream compressedStream = null;

        if (stream != null) {
            if (compressInput) {
                compressedStream = FtpUtil.compress(stream, (inputContent.getStringValue(StringUtils.fromString(
                        FtpConstants.INPUT_CONTENT_FILE_PATH_KEY))).getValue());
                message = FtpClientHelper.getCompressedMessage(clientConnector, (inputContent.getStringValue(
                        StringUtils.fromString(FtpConstants.INPUT_CONTENT_FILE_PATH_KEY))).getValue(),
                        propertyMap, compressedStream);
            } else {
                try {
                    message = FtpClientHelper.getUncompressedMessage(clientConnector, (inputContent.getStringValue(
                            StringUtils.fromString(FtpConstants.INPUT_CONTENT_FILE_PATH_KEY))).getValue(),
                            propertyMap, stream);
                } catch (BallerinaFtpException e) {
                    return FtpUtil.createError(e.getMessage(), Error.errorType());
                }
            }
        } else {
            return FtpUtil.createError("Error while reading a file", Error.errorType());
        }
        Object result = env.yieldAndRun(() -> {
            CompletableFuture<Object> balFuture = new CompletableFuture<>();
            FtpClientListener connectorListener = new FtpClientListener(balFuture, true,
                    remoteFileSystemBaseMessage -> FtpClientHelper.executeGenericAction());
            VfsClientConnectorImpl connector
                    = (VfsClientConnectorImpl) clientConnector.getNativeData(VFS_CLIENT_CONNECTOR);
            connector.addListener(connectorListener);
            String filePath = (inputContent.getStringValue(
                    StringUtils.fromString(FtpConstants.INPUT_CONTENT_FILE_PATH_KEY))).getValue();
            if (compressInput) {
                filePath = FtpUtil.getCompressedFileName(filePath);
            }
            connector.send(message, FtpAction.PUT, filePath, null);
            return getResult(balFuture);
        });
        try {
            stream.close();
            if (compressedStream != null) {
                compressedStream.close();
            }
        } catch (IOException e) {
            log.error("Error in closing stream");
        }
        return result;
    }

    public static Object putBytes(Environment env, BObject clientConnector, BString path, BArray inputContent,
                                  BString options) {
        InputStream stream = new ByteArrayInputStream(inputContent.getBytes());
        RemoteFileSystemMessage message = new RemoteFileSystemMessage(stream);
        return putGenericAction(env, clientConnector, path, options, message);
    }

    public static Object putText(Environment env, BObject clientConnector, BString path, BString inputContent,
                                 BString options) {
        InputStream stream = new ByteArrayInputStream(inputContent.getValue().getBytes(StandardCharsets.UTF_8));
        RemoteFileSystemMessage message = new RemoteFileSystemMessage(stream);
        return putGenericAction(env, clientConnector, path, options, message);
    }

    public static Object putJson(Environment env, BObject clientConnector, BString path, BString inputContent,
                                 BString options) {
        InputStream stream = new ByteArrayInputStream(inputContent.getValue().getBytes(StandardCharsets.UTF_8));
        RemoteFileSystemMessage message = new RemoteFileSystemMessage(stream);
        return putGenericAction(env, clientConnector, path, options, message);
    }

    public static Object putXml(Environment env, BObject clientConnector, BString path, BXml inputContent,
                                BString options) {
        InputStream stream = new ByteArrayInputStream(inputContent.toString().getBytes(StandardCharsets.UTF_8));
        RemoteFileSystemMessage message = new RemoteFileSystemMessage(stream);
        return putGenericAction(env, clientConnector, path, options, message);
    }

    public static Object putCsv(Environment env, BObject clientConnector, BString path, BArray inputContent,
                                BString options) {
        boolean addHeader = !options.getValue().equals(FtpConstants.WRITE_OPTION_APPEND);
        String convertToCsv = CSVUtils.convertToCsv(inputContent, addHeader);
        InputStream stream = new ByteArrayInputStream(convertToCsv.getBytes(StandardCharsets.UTF_8));
        RemoteFileSystemMessage message = new RemoteFileSystemMessage(stream);
        return putGenericAction(env, clientConnector, path, options, message);
    }

    public static Object putBytesAsStream(Environment env, BObject clientConnector, BString path, BStream inputContent,
                                          BString options) {
        try {
            InputStream stream = createInputStreamFromIterator(env, inputContent.getIteratorObj());
            RemoteFileSystemMessage message = new RemoteFileSystemMessage(stream);
            return putGenericAction(env, clientConnector, path, options, message);
        } catch (Exception e) {
            return FtpUtil.createError(e.getMessage(), FTP_ERROR);
        }
    }

    public static Object putCsvAsStream(Environment env, BObject clientConnector, BString path, BStream inputContent,
                                        BString options) {
        try {
            InputStream stream = createInputStreamFromIterator(env, inputContent.getIteratorObj());
            RemoteFileSystemMessage message = new RemoteFileSystemMessage(stream);
            return putGenericAction(env, clientConnector, path, options, message);
        } catch (Exception e) {
            return FtpUtil.createError(e.getMessage(), FTP_ERROR);
        }
    }

    /**
     * Creates an InputStream from a Ballerina iterator using SequenceInputStream.
     */
    private static InputStream createInputStreamFromIterator(Environment environment, BObject iterator) {
        IteratorToInputStream streamIterator = new IteratorToInputStream(environment, iterator);
        return new SequenceInputStream(asEnumeration(streamIterator));
    }

    /**
     * Converts an Iterator to an Enumeration for compatibility with SequenceInputStream.
     */
    private static <T> Enumeration<T> asEnumeration(Iterator<T> iterator) {
        return new Enumeration<T>() {
            @Override
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @Override
            public T nextElement() {
                return iterator.next();
            }
        };
    }

    /**
     * Lightweight adapter that turns a Ballerina iterator into a sequence of InputStreams.
     * Keeps state minimal to reduce nesting and cognitive complexity in the outer method.
     */
    private static final class IteratorToInputStream implements Iterator<InputStream> {
        private final Environment env;
        private final BObject iterator;
        private InputStream nextStream;
        private boolean hasChecked;
        private boolean isFirstRow = true;

        IteratorToInputStream(Environment env, BObject iterator) {
            this.env = env;
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            if (hasChecked) {
                return nextStream != null;
            }
            nextStream = fetchNextStream();
            hasChecked = true;
            return nextStream != null;
        }

        @Override
        public InputStream next() {
            if (!hasChecked && !hasNext()) {
                throw new NoSuchElementException();
            }
            hasChecked = false;
            InputStream result = nextStream;
            nextStream = null;
            return result;
        }

        private InputStream fetchNextStream() {
            final Object next;
            try {
                next = env.getRuntime().callMethod(iterator, "next", null);
            } catch (Exception e) {
                throw FtpUtil.createError("Failed to read iterator", e, FTP_ERROR);
            }
            if (next == null) {
                return null;
            }
            if (next instanceof BError err) {
                throw FtpUtil.createError("Iterator error: " + err.getMessage(), FTP_ERROR);
            }

            byte[] bytes = toBytes(next);
            if (bytes.length == 0) {
                return null;
            }
            isFirstRow = false;
            return new ByteArrayInputStream(bytes);
        }

        private byte[] toBytes(Object value) {
            // Each element is a record with a 'value' field.
            @SuppressWarnings("unchecked")
            BMap<BString, Object> streamRecord = (BMap<BString, Object>) value;
            Object val = streamRecord.get(FtpConstants.FIELD_VALUE);

            if (val instanceof BArray array) {
                return bytesFromArray(array);
            }
            @SuppressWarnings("unchecked")
            BMap<BString, Object> recordValue = (BMap<BString, Object>) val;
            return bytesFromRecord(recordValue, isFirstRow);
        }

        private static byte[] bytesFromArray(BArray array) {
            // If it's a byte[] just return it; else it's CSV row from string[]
            if (array.getElementType().getTag() == TypeTags.BYTE_TAG) {
                return array.getBytes();
            }
            String csvRow = CSVUtils.convertArrayToCsvRow(array) + System.lineSeparator();
            return csvRow.getBytes(StandardCharsets.UTF_8);
        }

        private static byte[] bytesFromRecord(BMap<BString, Object> balRecord, boolean includeHeader) {
            String csvRow = CSVUtils.convertRecordToCsvRow(balRecord, includeHeader) + System.lineSeparator();
            return csvRow.getBytes(StandardCharsets.UTF_8);
        }
    }

    private static Object putGenericAction(Environment env, BObject clientConnector, BString path, BString options,
                                           RemoteFileSystemMessage message) {
        return env.yieldAndRun(() -> {
            CompletableFuture<Object> balFuture = new CompletableFuture<>();
            FtpClientListener connectorListener = new FtpClientListener(balFuture, true,
                    remoteFileSystemBaseMessage -> FtpClientHelper.executeGenericAction());
            VfsClientConnectorImpl connector
                    = (VfsClientConnectorImpl) clientConnector.getNativeData(VFS_CLIENT_CONNECTOR);
            connector.addListener(connectorListener);
            String filePath = path.getValue();
            if (options.getValue().equals(FtpConstants.WRITE_OPTION_OVERWRITE)) {
                connector.send(message, FtpAction.PUT, filePath, null);
            } else {
                connector.send(message, FtpAction.APPEND, filePath, null);
            }
            return getResult(balFuture);
        });
    }

    public static Object delete(Environment env, BObject clientConnector, BString filePath) {
        return executeSinglePathAction(env, clientConnector, filePath, FtpAction.DELETE, true,
                balFuture -> remoteFileSystemBaseMessage -> {
                    FtpClientHelper.executeGenericAction();
                    return true;
                });
    }

    public static Object isDirectory(Environment env, BObject clientConnector, BString filePath) {
        return executeSinglePathAction(env, clientConnector, filePath, FtpAction.ISDIR, false,
                balFuture -> remoteFileSystemBaseMessage -> {
                    FtpClientHelper.executeIsDirectoryAction(remoteFileSystemBaseMessage, balFuture);
                    return true;
                });
    }

    public static Object list(Environment env, BObject clientConnector, BString filePath) {
        return executeSinglePathAction(env, clientConnector, filePath, FtpAction.LIST, false,
                balFuture -> remoteFileSystemBaseMessage -> {
                    FtpClientHelper.executeListAction(remoteFileSystemBaseMessage, balFuture);
                    return true;
                });
    }

    public static Object mkdir(Environment env, BObject clientConnector, BString path) {
        return executeSinglePathAction(env, clientConnector, path, FtpAction.MKDIR, true,
                balFuture -> remoteFileSystemBaseMessage -> {
                    FtpClientHelper.executeGenericAction();
                    return true;
                });
    }

    public static Object rename(Environment env, BObject clientConnector, BString origin, BString destination) {
        return executeTwoPathAction(env, clientConnector, origin, destination, FtpAction.RENAME);
    }

    public static Object move(Environment env, BObject clientConnector, BString sourcePath, BString destinationPath) {
        return executeTwoPathAction(env, clientConnector, sourcePath, destinationPath, FtpAction.RENAME);
    }

    public static Object copy(Environment env, BObject clientConnector, BString sourcePath, BString destinationPath) {
        return executeTwoPathAction(env, clientConnector, sourcePath, destinationPath, FtpAction.COPY);
    }

    public static Object exists(Environment env, BObject clientConnector, BString filePath) {
        return executeSinglePathAction(env, clientConnector, filePath, FtpAction.EXISTS, false,
                balFuture -> remoteFileSystemBaseMessage -> {
                    FtpClientHelper.executeExistsAction(remoteFileSystemBaseMessage, balFuture);
                    return true;
                });
    }

    public static Object rmdir(Environment env, BObject clientConnector, BString filePath) {
        return executeSinglePathAction(env, clientConnector, filePath, FtpAction.RMDIR, true,
                balFuture -> remoteFileSystemBaseMessage -> {
                    FtpClientHelper.executeGenericAction();
                    return true;
                });
    }

    public static Object size(Environment env, BObject clientConnector, BString filePath) {
        return executeSinglePathAction(env, clientConnector, filePath, FtpAction.SIZE, false,
                balFuture -> remoteFileSystemBaseMessage -> {
                    FtpClientHelper.executeSizeAction(remoteFileSystemBaseMessage, balFuture);
                    return true;
                });
    }

    /**
     * Helper method to execute single-path FTP actions.
     *
     * @param env The Ballerina runtime environment
     * @param clientConnector The FTP client connector object
     * @param filePath The file path to operate on
     * @param action The FTP action to execute
     * @param closeInput Whether to close the input stream after the action
     * @param messageHandlerFactory A function that creates a message handler given a CompletableFuture
     * @return The result of the operation or an error
     */
    private static Object executeSinglePathAction(Environment env, BObject clientConnector, BString filePath,
                                                   FtpAction action, boolean closeInput,
                                                   Function<CompletableFuture<Object>,
                                                           Function<RemoteFileSystemBaseMessage, Boolean>>
                                                           messageHandlerFactory) {
        return env.yieldAndRun(() -> {
            CompletableFuture<Object> balFuture = new CompletableFuture<>();
            Function<RemoteFileSystemBaseMessage, Boolean> messageHandler =
                    messageHandlerFactory.apply(balFuture);
            FtpClientListener connectorListener = new FtpClientListener(balFuture, closeInput, messageHandler);
            VfsClientConnectorImpl connector = (VfsClientConnectorImpl) clientConnector.
                    getNativeData(VFS_CLIENT_CONNECTOR);
            connector.addListener(connectorListener);
            connector.send(null, action, filePath.getValue(), null);
            return getResult(balFuture);
        });
    }

    /**
     * Helper method to execute two-path FTP actions.
     *
     * @param env The Ballerina runtime environment
     * @param clientConnector The FTP client connector object
     * @param sourcePath The source file path
     * @param destinationPath The destination file path
     * @param action The FTP action to execute (RENAME for move, COPY for copy)
     * @return The result of the operation or an error
     */
    private static Object executeTwoPathAction(Environment env, BObject clientConnector, BString sourcePath,
                                                BString destinationPath, FtpAction action) {
        String destinationUrl;
        try {
            destinationUrl = FtpUtil.createUrl(clientConnector, destinationPath.getValue());
        } catch (BallerinaFtpException e) {
            return FtpUtil.createError(e.getMessage(), Error.errorType());
        }
        return env.yieldAndRun(() -> {
            CompletableFuture<Object> balFuture = new CompletableFuture<>();
            FtpClientListener connectorListener = new FtpClientListener(balFuture, true,
                    remoteFileSystemBaseMessage -> FtpClientHelper.executeGenericAction());
            VfsClientConnectorImpl connector = (VfsClientConnectorImpl) clientConnector.
                    getNativeData(VFS_CLIENT_CONNECTOR);
            connector.addListener(connectorListener);
            connector.send(null, action, sourcePath.getValue(), destinationUrl);
            return getResult(balFuture);
        });
    }

    public static Object getResult(CompletableFuture<Object> balFuture) {
        try {
            return balFuture.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ErrorCreator.createError(e);
        } catch (Throwable throwable) {
            return ErrorCreator.createError(throwable);
        }
    }

    public static void handleStreamEnd(BObject entity, BufferHolder bufferHolder) {
        entity.addNativeData(ENTITY_BYTE_STREAM, null);
        bufferHolder.setTerminal(true);
    }
}
