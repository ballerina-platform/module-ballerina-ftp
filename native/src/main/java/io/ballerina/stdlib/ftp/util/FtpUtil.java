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
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static io.ballerina.stdlib.ftp.util.FtpConstants.FTP_ANONYMOUS_PASSWORD;
import static io.ballerina.stdlib.ftp.util.FtpConstants.FTP_ANONYMOUS_USERNAME;
import static io.ballerina.stdlib.ftp.util.FtpConstants.ON_FILE_CHANGE_REMOTE_FUNCTION;
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

    public static String createUrl(BObject clientConnector, String filePath) throws BallerinaFtpException {
        String username = (String) clientConnector.getNativeData(FtpConstants.ENDPOINT_CONFIG_USERNAME);
        String password = (String) clientConnector.getNativeData(FtpConstants.ENDPOINT_CONFIG_PASS_KEY);
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
        String username = null;
        String password = null;
        if (auth != null) {
            final BMap credentials = auth.getMapValue(StringUtils.fromString(
                    FtpConstants.ENDPOINT_CONFIG_CREDENTIALS));
            if (credentials != null) {
                username = (credentials.getStringValue(StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_USERNAME)))
                        .getValue();
                if (username.isBlank()) {
                    throw new BallerinaFtpException("Username cannot be empty");
                }
                password = (credentials.getStringValue(StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_PASS_KEY)))
                        .getValue();
            }
        }
        return createUrl(protocol, host, port, username, password, filePath);
    }

    private static String createUrl(String protocol, String host, int port, String username, String password,
                                    String filePath) throws BallerinaFtpException {
        String userInfo = username + ":" + password;
        URI uri;
        try {
            uri = new URI(protocol, userInfo, host, port, filePath, null, null);
        } catch (URISyntaxException e) {
            throw new BallerinaFtpException("Error occurred while constructing a URI from host: " + host +
                    ", port: " + port + ", username: " + username + " and basePath: " + filePath + e.getMessage(), e);
        }
        return uri.toString();
    }

    public static Map<String, String> getAuthMap(BMap config) {
        final BMap auth = config.getMapValue(StringUtils.fromString(
                FtpConstants.ENDPOINT_CONFIG_AUTH));
        String username = FTP_ANONYMOUS_USERNAME;
        String password = FTP_ANONYMOUS_PASSWORD;
        if (auth != null) {
            final BMap credentials = auth.getMapValue(StringUtils.fromString(
                    FtpConstants.ENDPOINT_CONFIG_CREDENTIALS));
            if (credentials != null) {
                username = (credentials.getStringValue(StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_USERNAME)))
                        .getValue();
                password = (credentials.getStringValue(StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_PASS_KEY)))
                        .getValue();
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
        return ErrorCreator.createError(ModuleUtils.getModule(), errorTypeName, StringUtils.fromString(message),
                null, null);
    }

    public static BError createError(String message, String details, String errorTypeName) {
        return ErrorCreator.createError(ModuleUtils.getModule(), errorTypeName, StringUtils.fromString(message), null,
                null);
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
                FtpConstants.FTP_ORG_NAME, FtpConstants.FTP_MODULE_NAME, FtpUtil.getFtpPackage().getVersion()),
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

    public static MethodType getOnFileChangeMethod(BObject service) {
        MethodType[] methodTypes = service.getType().getMethods();
        for (MethodType method: methodTypes) {
            if (method.getName().equals(ON_FILE_CHANGE_REMOTE_FUNCTION)) {
                return method;
            }
        }
        return null;
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
}
