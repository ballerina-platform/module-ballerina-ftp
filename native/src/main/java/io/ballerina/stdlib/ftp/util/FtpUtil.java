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

package io.ballerina.stdlib.ftp.util;

import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.types.ObjectType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.ftp.exception.BallerinaFtpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static io.ballerina.stdlib.ftp.transport.server.util.FileTransportUtils.maskUrlPassword;
import static io.ballerina.stdlib.ftp.util.FtpConstants.ENDPOINT_CONFIG_PREFERRED_METHODS;
import static io.ballerina.stdlib.ftp.util.FtpConstants.FTP_ANONYMOUS_PASSWORD;
import static io.ballerina.stdlib.ftp.util.FtpConstants.FTP_ANONYMOUS_USERNAME;
import static io.ballerina.stdlib.ftp.util.FtpConstants.NO_AUTH_METHOD_ERROR;
import static io.ballerina.stdlib.ftp.util.FtpConstants.ON_FILE_CHANGE_REMOTE_FUNCTION;
import static io.ballerina.stdlib.ftp.util.FtpUtil.ErrorType.Error;
import static io.ballerina.stdlib.ftp.util.ModuleUtils.getModule;

/**
 * Utils class for FTP client operations.
 */
public class FtpUtil {

    private static final Logger log = LoggerFactory.getLogger(FtpUtil.class);
    private static final int MAX_PORT = 65535;

    private FtpUtil() {
        // private constructor
    }

    public static void extractTimeoutConfigurations(BMap<Object, Object> config, Map<String, Object> ftpProperties)
            throws BallerinaFtpException {
        // Extract connectTimeout
        Object connectTimeoutObj = config.get(StringUtils.fromString(FtpConstants.CONNECT_TIMEOUT));
        double connectTimeout = ((BDecimal) connectTimeoutObj).floatValue();
        validateTimeout(connectTimeout, "connectTimeout");
        ftpProperties.put(FtpConstants.CONNECT_TIMEOUT, String.valueOf(connectTimeout));

        // Extract socketConfig
        BMap socketConfig = config.getMapValue(StringUtils.fromString(FtpConstants.SOCKET_CONFIG));
        if (socketConfig == null) {
            return;
        }

        // Extract ftpDataTimeout
        Object ftpDataTimeoutObj = socketConfig.get(StringUtils.fromString(FtpConstants.FTP_DATA_TIMEOUT));
        double ftpDataTimeout = ((BDecimal) ftpDataTimeoutObj).floatValue();
        validateTimeout(ftpDataTimeout, "ftpDataTimeout");
        ftpProperties.put(FtpConstants.FTP_DATA_TIMEOUT, String.valueOf(ftpDataTimeout));

        // Extract ftpSocketTimeout
        Object ftpSocketTimeoutObj = socketConfig.get(StringUtils.fromString(FtpConstants.FTP_SOCKET_TIMEOUT));
        double ftpSocketTimeout = ((BDecimal) ftpSocketTimeoutObj).floatValue();
        validateTimeout(ftpSocketTimeout, "ftpSocketTimeout");
        ftpProperties.put(FtpConstants.FTP_SOCKET_TIMEOUT, String.valueOf(ftpSocketTimeout));

        // Extract sftpSessionTimeout
        Object sftpSessionTimeoutObj = socketConfig.get(StringUtils.fromString(FtpConstants.SFTP_SESSION_TIMEOUT));
        double sftpSessionTimeout = ((BDecimal) sftpSessionTimeoutObj).floatValue();
        validateTimeout(sftpSessionTimeout, "sftpSessionTimeout");
        ftpProperties.put(FtpConstants.SFTP_SESSION_TIMEOUT, String.valueOf(sftpSessionTimeout));
    }

    private static void validateTimeout(double timeout, String fieldName) throws BallerinaFtpException {
        if (timeout < 0) {
            throw new BallerinaFtpException(fieldName + " must be positive or zero (got: " + timeout + ")");
        }
        if (timeout > 600) {
            throw new BallerinaFtpException(fieldName + " must not exceed 600 seconds (got: " + timeout + ")");
        }
    }

    public static void extractFileTransferConfiguration(BMap<Object, Object> config,
                                                        Map<String, Object> ftpProperties) {
        // Update to the new constant
        BString fileTransferMode = config.getStringValue(StringUtils.fromString(FtpConstants.FILE_TRANSFER_MODE));
        if (fileTransferMode != null && !fileTransferMode.getValue().isEmpty()) {
            ftpProperties.put(FtpConstants.FILE_TRANSFER_MODE, fileTransferMode.getValue());
        }
    }

    public static void extractCompressionConfiguration(BMap<Object, Object> config, Map<String, Object> ftpProperties) {
        BArray sftpCompression = config.getArrayValue(StringUtils.fromString(FtpConstants.SFTP_COMPRESSION));
        if (sftpCompression != null && !sftpCompression.isEmpty()) {
            StringBuilder compressionValues = new StringBuilder();
            for (int i = 0; i < sftpCompression.size(); i++) {
                if (i > 0) {
                    compressionValues.append(",");
                }
                compressionValues.append(sftpCompression.getBString(i).getValue());
            }
            String sftpCompressionValue = compressionValues.toString();
            if (!"none".equals(sftpCompressionValue)) {
                ftpProperties.put(FtpConstants.SFTP_COMPRESSION, sftpCompressionValue);
            }
        }
    }

    public static void extractKnownHostsConfiguration(BMap<Object, Object> config, Map<String, Object> ftpProperties) {
        BString knownHosts = config.getStringValue(StringUtils.fromString(FtpConstants.SFTP_KNOWN_HOSTS));
        if (knownHosts != null && !knownHosts.getValue().isEmpty()) {
            ftpProperties.put(FtpConstants.SFTP_KNOWN_HOSTS, knownHosts.getValue());
        }
    }

    public static void extractProxyConfiguration(BMap<Object, Object> config, Map<String, Object> ftpProperties)
            throws BallerinaFtpException {
        BMap proxyConfig = config.getMapValue(StringUtils.fromString(FtpConstants.PROXY));
        if (proxyConfig == null) {
            return;
        }
        // Extract proxy host
        BString proxyHost = proxyConfig.getStringValue(StringUtils.fromString(FtpConstants.PROXY_HOST));
        if (proxyHost == null || proxyHost.getValue().isEmpty()) {
            throw new BallerinaFtpException("Proxy host cannot be empty");
        }
        ftpProperties.put(FtpConstants.PROXY_HOST, proxyHost.getValue());

        // Extract proxy port
        Object proxyPortObj = proxyConfig.get(StringUtils.fromString(FtpConstants.PROXY_PORT));
        if (proxyPortObj != null) {
            long proxyPort = ((Number) proxyPortObj).longValue();
            if (proxyPort < 1 || proxyPort > 65535) {
                throw new BallerinaFtpException("Proxy port must be between 1 and 65535 (got: " + proxyPort + ")");
            }
            ftpProperties.put(FtpConstants.PROXY_PORT, String.valueOf(proxyPort));
        }

        // Extract proxy type
        BString proxyType = proxyConfig.getStringValue(StringUtils.fromString(FtpConstants.PROXY_TYPE));
        if (proxyType != null && !proxyType.getValue().isEmpty()) {
            ftpProperties.put(FtpConstants.PROXY_TYPE, proxyType.getValue());
        }

        // Extract proxy auth
        BMap proxyAuth = proxyConfig.getMapValue(StringUtils.fromString(FtpConstants.PROXY_AUTH));
        if (proxyAuth != null) {
            BString proxyUsername = proxyAuth.getStringValue(StringUtils.fromString(
                    FtpConstants.PROXY_USERNAME));
            BString proxyPassword = proxyAuth.getStringValue(StringUtils.fromString(
                    FtpConstants.PROXY_PASSWORD));
            if (proxyUsername != null) {
                ftpProperties.put(FtpConstants.PROXY_USERNAME, proxyUsername.getValue());
            }
            if (proxyPassword != null) {
                ftpProperties.put(FtpConstants.PROXY_PASSWORD, proxyPassword.getValue());
            }
        }

        // Extract proxy command (for STREAM proxy)
        BString proxyCommand = proxyConfig.getStringValue(StringUtils.fromString(FtpConstants.PROXY_COMMAND));
        if (proxyCommand != null && !proxyCommand.getValue().isEmpty()) {
            ftpProperties.put(FtpConstants.PROXY_COMMAND, proxyCommand.getValue());
        }
    }

    /**
     * Configures FTPS mode (IMPLICIT or EXPLICIT).
     *
     * @param secureSocket The secure socket configuration map
     * @param config The configuration map to populate
     */
    public static void configureFtpsMode(BMap secureSocket, Map<String, Object> config) {
        // Ballerina record has default value 'EXPLICIT', so this is never null.
        String mode = secureSocket.getStringValue(StringUtils.fromString(
                FtpConstants.ENDPOINT_CONFIG_FTPS_MODE)).getValue();
        config.put(FtpConstants.ENDPOINT_CONFIG_FTPS_MODE, mode);
    }

    /**
     * Configures FTPS data channel protection level.
     *
     * @param secureSocket The secure socket configuration map
     * @param config The configuration map to populate
     */
    public static void configureFtpsDataChannelProtection(BMap secureSocket, Map<String, Object> config) {
        // Ballerina record has default value 'PRIVATE', so this is never null.
        String dataChannelProtection = secureSocket.getStringValue(StringUtils.fromString(
                FtpConstants.ENDPOINT_CONFIG_FTPS_DATA_CHANNEL_PROTECTION)).getValue();
        config.put(FtpConstants.ENDPOINT_CONFIG_FTPS_DATA_CHANNEL_PROTECTION, dataChannelProtection);
    }

    /**
     * Extracts path/password from the crypto:KeyStore/TrustStore record (BMap)
     * and stores them as strings in the configuration map.
     *
     * @param secureSocket The secure socket configuration map
     * @param storeKey The key identifying the store in secureSocket ("key" or "cert")
     * @param pathConfigKey The key to store the path in the config map
     * @param passwordConfigKey The key to store the password in the config map
     * @param config The configuration map to populate
     * @return The path of the store, or null if the store record is missing.
     */
    public static String extractAndConfigureStore(BMap secureSocket, String storeKey,
                                                  String pathConfigKey, String passwordConfigKey,
                                                  Map<String, Object> config) {
        // This CAN be null because 'crypto:KeyStore key?' is optional in SecureSocket
        BMap storeRecord = secureSocket.getMapValue(StringUtils.fromString(storeKey));

        if (storeRecord == null) {
            return null;
        }

        // 'path' and 'password' are mandatory in crypto:KeyStore/TrustStore, so they are guaranteed to exist.
        String path = storeRecord.getStringValue(StringUtils.fromString("path")).getValue();
        String password = storeRecord.getStringValue(StringUtils.fromString("password")).getValue();

        config.put(pathConfigKey, path);
        config.put(passwordConfigKey, password);

        return path;
    }

    public static String createUrl(BObject clientConnector, String filePath) throws BallerinaFtpException {
        String username = (String) clientConnector.getNativeData(FtpConstants.ENDPOINT_CONFIG_USERNAME);
        String password = null;
        Object storedPW = clientConnector.getNativeData(FtpConstants.ENDPOINT_CONFIG_PASS_KEY);
        if (storedPW != null) {
            password = (String) storedPW;
        }
        String host = (String) clientConnector.getNativeData(FtpConstants.ENDPOINT_CONFIG_HOST);
        int port = (Integer) clientConnector.getNativeData(FtpConstants.ENDPOINT_CONFIG_PORT);
        String protocol = (String) clientConnector.getNativeData(FtpConstants.ENDPOINT_CONFIG_PROTOCOL);
        return createUrl(protocol, host, port, username, password, filePath);
    }

    public static String createUrl(BMap config) throws BallerinaFtpException {
        final String filePath = (config.getStringValue(StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_PATH)))
                .getValue();
        String protocol = (config.getStringValue(StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_PROTOCOL)))
                .getValue();
        final String host = (config.getStringValue(StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_HOST)))
                .getValue();
        int port = extractPortValue(config.getIntValue(StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_PORT)));
        final BMap auth = config.getMapValue(StringUtils.fromString(
                FtpConstants.ENDPOINT_CONFIG_AUTH));
        

        String username = FTP_ANONYMOUS_USERNAME;
        String password = (protocol.equals(FtpConstants.SCHEME_FTP) || protocol.equals(FtpConstants.SCHEME_FTPS)) //
                ? FTP_ANONYMOUS_PASSWORD : null;
        
        if (auth != null) {
            final BMap credentials = auth.getMapValue(StringUtils.fromString(
                    FtpConstants.ENDPOINT_CONFIG_CREDENTIALS));
            if (credentials != null) {
                username = (credentials.getStringValue(StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_USERNAME)))
                        .getValue();
                if (username.isBlank()) {
                    throw new BallerinaFtpException("Username cannot be empty");
                }
                BString tempPassword = credentials.getStringValue(
                        StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_PASS_KEY));
                if (tempPassword != null) {
                    password = tempPassword.getValue();
                }
            }
        }
        
        return createUrl(protocol, host, port, username, password, filePath);
    }

    private static String createUrl(String protocol, String host, int port, String username, String password,
                                    String filePath) throws BallerinaFtpException {
        String userInfo = username;
        if (password != null) {
            userInfo = username + ":" + password;
        }
        final String normalizedPath = normalizeFtpPath(filePath);
        try {
            URI uri = new URI(protocol, userInfo, host, port, normalizedPath, null, null);
            return uri.toString();
        } catch (URISyntaxException e) {
            throw new BallerinaFtpException("Error occurred while constructing a URI from host: " + host +
                    ", port: " + port + ", username: " + username + " and basePath: " + filePath + e.getMessage(), e);
        }
    }

    private static String normalizeFtpPath(String rawPath) {
        if (rawPath == null || rawPath.isEmpty()) {
            return "/";
        }
        if (rawPath.startsWith("/")) {
            return rawPath;
        }
        return "/" + rawPath;
    }

    public static Map<String, String> getAuthMap(BMap config) {
        return getAuthMap(config, FtpConstants.SCHEME_FTP);
    }

    public static Map<String, String> getAuthMap(BMap config, String protocol) {
        final BMap auth = config.getMapValue(StringUtils.fromString(
                FtpConstants.ENDPOINT_CONFIG_AUTH));
        String username = FTP_ANONYMOUS_USERNAME;
        String password = (protocol.equals(FtpConstants.SCHEME_FTP) || protocol.equals(FtpConstants.SCHEME_FTPS)) //
                ? FTP_ANONYMOUS_PASSWORD : null;
        if (auth != null) {
            final BMap credentials = auth.getMapValue(StringUtils.fromString(
                    FtpConstants.ENDPOINT_CONFIG_CREDENTIALS));
            if (credentials != null) {
                username = (credentials.getStringValue(StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_USERNAME)))
                        .getValue();
                BString tempPassword = credentials.getStringValue(
                        StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_PASS_KEY));
                if (tempPassword != null) {
                    password = tempPassword.getValue();
                }
            }
        }
        Map<String, String> authMap = new HashMap<>();
        authMap.put(FtpConstants.ENDPOINT_CONFIG_USERNAME, username);
        authMap.put(FtpConstants.ENDPOINT_CONFIG_PASS_KEY, password);
        return authMap;
    }

    /**
     * Creates an error message.
     *
     * @param message the detailed message of the error.
     * @param errorTypeName error type.
     * @return an error which will be propagated to ballerina user.
     */
    public static BError createError(String message, String errorTypeName) {
        String safeMessage = maskUrlPassword(message);
        return ErrorCreator.createError(ModuleUtils.getModule(), errorTypeName, StringUtils.fromString(safeMessage),
                null, null);
    }

    public static BError createError(String message, Throwable cause, String errorTypeName) {
        String safeMessage = maskUrlPassword(message);
        return ErrorCreator.createError(ModuleUtils.getModule(), errorTypeName, StringUtils.fromString(safeMessage),
                cause == null ? null : cause instanceof BError ?
                        (BError) cause : ErrorCreator.createError(StringUtils
                        .fromString(maskUrlPassword(cause.getMessage()))), null);
    }

    public static Throwable findRootCause(Throwable throwable) {
        Objects.requireNonNull(throwable);
        Throwable rootCause = throwable;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }

    /**
     * Gives the port value from a given config input.
     *
     * @param longValue the input config value
     * @return the relevant int value from the config
     */
    public static int extractPortValue(long longValue) {
        if (longValue <= 0 || longValue > MAX_PORT) {
            log.error("Invalid port number given in configuration");
            return -1;
        }
        try {
            return Math.toIntExact(longValue);
        } catch (ArithmeticException e) {
            log.warn("The value set for port needs to be less than {}. The port value is set to {}",
                    Integer.MAX_VALUE, Integer.MAX_VALUE);
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Gives record type object for FileInfo record.
     *
     * @return FileInfo record type object
     */
    public static Type getFileInfoType() {
        BMap<BString, Object> fileInfoStruct = ValueCreator.createRecordValue(new Module(
                FtpConstants.FTP_ORG_NAME, FtpConstants.FTP_MODULE_NAME, FtpUtil.getFtpPackage().getMajorVersion()),
                FtpConstants.FTP_FILE_INFO);
        return fileInfoStruct.getType();
    }

    public static ByteArrayInputStream compress(InputStream inputStream, String targetFilePath) {
        String fileName = new File(targetFilePath).getName();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);
        try {
            zipOutputStream.putNextEntry(new ZipEntry(fileName));
            byte[] buffer = new byte[1000];
            int length;
            // This should ideally be wrapped from a BufferedInputStream. but currently
            // not working due to missing method implementations in the implemented custom ByteArrayInputStream.
            while ((length = inputStream.read(buffer)) > 0) {
                zipOutputStream.write(buffer, 0, length);
            }
        } catch (IOException ex) {
            log.error("The file does not exist");
        } finally {
            try {
                inputStream.close();
                zipOutputStream.closeEntry();
                zipOutputStream.close();
            } catch (IOException e) {
                log.error("Error in closing the stream");
            }
        }
        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }

    public static String getCompressedFileName(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf('.')).concat(".zip");
    }

    public static Optional<MethodType> getOnFileChangeMethod(BObject service) {
        MethodType[] methodTypes = ((ObjectType) TypeUtils.getReferredType(TypeUtils.getType(service))).getMethods();
        return Stream.of(methodTypes)
                .filter(methodType -> ON_FILE_CHANGE_REMOTE_FUNCTION.equals(methodType.getName()))
                .findFirst();
    }

    /**
     * Gets the content handler method from a service if it exists.
     * Checks for content methods in priority order: onFile, onFileText, onFileJson, onFileXml, onFileCsv.
     *
     * @param service The BObject service
     * @return Optional containing the MethodType if a content method exists
     */
    public static Optional<MethodType> getContentHandlerMethod(BObject service) {
        MethodType[] methodTypes = ((ObjectType) TypeUtils.getReferredType(TypeUtils.getType(service))).getMethods();
        return Stream.of(methodTypes)
                .filter(methodType -> isContentHandlerMethodName(methodType.getName()))
                .findFirst();
    }

    /**
      * Gets all content handler methods from a service.
     *
     * @param service The BObject service
     * @return Array of MethodType objects representing all content handler methods
     */
    public static MethodType[] getAllContentHandlerMethods(BObject service) {
        MethodType[] methodTypes = ((ObjectType) TypeUtils.getReferredType(TypeUtils.getType(service))).getMethods();
        return Stream.of(methodTypes)
                .filter(methodType -> isContentHandlerMethodName(methodType.getName()))
                .toArray(MethodType[]::new);
    }

    /**
     * Finds a specific content handler method by name from a service.
     *
     * @param service The BObject service
     * @param methodName The name of the method to find
     * @return Optional containing the MethodType if found
     */
    public static Optional<MethodType> findContentMethodByName(BObject service, String methodName) {
        if (!isContentHandlerMethodName(methodName)) {
            return Optional.empty();
        }

        MethodType[] methodTypes = ((ObjectType) TypeUtils.getReferredType(TypeUtils.getType(service))).getMethods();
        return Stream.of(methodTypes)
                .filter(methodType -> methodName.equals(methodType.getName()))
                .findFirst();
    }

    /**
     * Checks if the given method name is a content handler method.
     *
     * @param methodName The name of the method
     * @return true if it's a content handler method
     */
    public static boolean isContentHandlerMethodName(String methodName) {
        return FtpConstants.ON_FILE_REMOTE_FUNCTION.equals(methodName) ||
                FtpConstants.ON_FILE_TEXT_REMOTE_FUNCTION.equals(methodName) ||
                FtpConstants.ON_FILE_JSON_REMOTE_FUNCTION.equals(methodName) ||
                FtpConstants.ON_FILE_XML_REMOTE_FUNCTION.equals(methodName) ||
                FtpConstants.ON_FILE_CSV_REMOTE_FUNCTION.equals(methodName);
    }

    /**
     * Gets the onFileDeleted method from a service if it exists.
     *
     * @param service The BObject service
     * @return Optional containing the MethodType if onFileDeleted method exists
     */
    public static Optional<MethodType> getOnFileDeletedMethod(BObject service) {
        MethodType[] methodTypes = ((ObjectType) TypeUtils.getReferredType(TypeUtils.getType(service))).getMethods();
        return Stream.of(methodTypes)
                .filter(methodType ->
                        (FtpConstants.ON_FILE_DELETED_REMOTE_FUNCTION.equals(methodType.getName()) ||
                        FtpConstants.ON_FILE_DELETE_REMOTE_FUNCTION.equals(methodType.getName())))
                .findFirst();
    }

    public static String getAuthMethod(Object authMethodObj) {
        return authMethodObj.toString().toLowerCase().replace("_", "-");
    }

    public static String getPreferredMethodsFromAuthConfig(BMap authenticationConfig) {
        final BArray preferredMethods = authenticationConfig.getArrayValue((StringUtils.fromString(
                ENDPOINT_CONFIG_PREFERRED_METHODS)));
        String preferredAuthMethods = "";
        if (preferredMethods != null) {
            if (preferredMethods.isEmpty()) {
                throw FtpUtil.createError(NO_AUTH_METHOD_ERROR, Error.errorType());
            }
            preferredAuthMethods = Arrays.stream(preferredMethods.getValues()).limit(preferredMethods.size())
                    .map(FtpUtil::getAuthMethod)
                    .collect(Collectors.joining(","));
        }
        return preferredAuthMethods;
    }

    /**
     * Specifies the error type for ftp package.
     */
    public enum ErrorType {

        Error("Error");

        private String errorType;

        ErrorType(String errorType) {
            this.errorType = errorType;
        }

        public String errorType() {
            return errorType;
        }
    }

    /**
     * Gets ballerina ftp package.
     *
     * @return udp package.
     */
    public static Module getFtpPackage() {
        return getModule();
    }

    /**
     * Loads a Java KeyStore from a file path and password.
     * 
     * @param path The file path to the KeyStore
     * @param password The password for the KeyStore
     * @return The loaded java.security.KeyStore object
     * @throws BallerinaFtpException If loading fails
     */
    public static KeyStore loadKeyStore(String path, String password) throws BallerinaFtpException {
        if (path == null || path.isEmpty()) {
            return null;
        }
        try {
            // JKS extension checking
            String type = KeyStore.getDefaultType();
            if (path.toLowerCase().endsWith(".jks")) {
                type = "JKS";
            } else if (path.toLowerCase().endsWith(".p12") || path.toLowerCase().endsWith(".pfx")) {
                type = "PKCS12";
            }

            KeyStore keyStore = KeyStore.getInstance(type);
            char[] passChars = (password != null) ? password.toCharArray() : null;
            
            try (FileInputStream fis = new FileInputStream(new File(path))) {
                keyStore.load(fis, passChars);
            }
            return keyStore;
        } catch (Exception e) {
            throw new BallerinaFtpException("Failed to load KeyStore from path: " + path + ". " + e.getMessage(), e);
        }
    }
}

