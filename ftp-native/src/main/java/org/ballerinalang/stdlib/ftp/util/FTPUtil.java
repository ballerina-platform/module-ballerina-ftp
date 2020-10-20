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

package org.ballerinalang.stdlib.ftp.util;

import io.ballerina.runtime.api.ErrorCreator;
import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.StringUtils;
import io.ballerina.runtime.api.ValueCreator;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.types.BAnnotatableType;
import io.ballerina.runtime.types.BType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
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

/**
 * Utils class for FTP client operations.
 */
public class FTPUtil {

    private static final Logger log = LoggerFactory.getLogger(FTPUtil.class);
    private static final int MAX_PORT = 65535;

    private FTPUtil() {
        // private constructor
    }

    public static boolean notValidProtocol(String url) {
        return !url.startsWith("ftp") && !url.startsWith("sftp") && !url.startsWith("ftps");
    }

    public static String createUrl(BObject clientConnector, String filePath) throws BallerinaFTPException {

        String username = (String) clientConnector.getNativeData(FTPConstants.ENDPOINT_CONFIG_USERNAME);
        String password = (String) clientConnector.getNativeData(FTPConstants.ENDPOINT_CONFIG_PASS_KEY);
        String host = (String) clientConnector.getNativeData(FTPConstants.ENDPOINT_CONFIG_HOST);
        int port = (Integer) clientConnector.getNativeData(FTPConstants.ENDPOINT_CONFIG_PORT);
        String protocol = (String) clientConnector.getNativeData(FTPConstants.ENDPOINT_CONFIG_PROTOCOL);

        return createUrl(protocol, host, port, username, password, filePath);
    }

    public static String createUrl(BMap config) throws BallerinaFTPException {

        final String filePath = (config.getStringValue(StringUtils.fromString(FTPConstants.ENDPOINT_CONFIG_PATH)))
                .getValue();
        String protocol = (config.getStringValue(StringUtils.fromString(FTPConstants.ENDPOINT_CONFIG_PROTOCOL)))
                .getValue();
        final String host = (config.getStringValue(StringUtils.fromString(FTPConstants.ENDPOINT_CONFIG_HOST)))
                .getValue();
        int port = extractPortValue(config.getIntValue(StringUtils.fromString(FTPConstants.ENDPOINT_CONFIG_PORT)));

        final BMap secureSocket = config.getMapValue(StringUtils.fromString(
                FTPConstants.ENDPOINT_CONFIG_SECURE_SOCKET));
        String username = null;
        String password = null;
        if (secureSocket != null) {
            final BMap basicAuth = secureSocket.getMapValue(StringUtils.fromString(
                    FTPConstants.ENDPOINT_CONFIG_BASIC_AUTH));
            if (basicAuth != null) {
                username = (basicAuth.getStringValue(StringUtils.fromString(FTPConstants.ENDPOINT_CONFIG_USERNAME)))
                        .getValue();
                password = (basicAuth.getStringValue(StringUtils.fromString(FTPConstants.ENDPOINT_CONFIG_PASS_KEY)))
                        .getValue();
            }
        }
        return createUrl(protocol, host, port, username, password, filePath);
    }

    private static String createUrl(String protocol, String host, int port, String username, String password,
                                    String filePath) throws BallerinaFTPException {

        String userInfo = username + ":" + password;
        URI uri = null;
        try {
            uri = new URI(protocol, userInfo, host, port, filePath, null, null);
        } catch (URISyntaxException e) {
            throw new BallerinaFTPException("Error occurred while constructing a URI from host: " + host +
                    ", port: " + port + ", username: " + username + " and basePath: " + filePath + e.getMessage(), e);
        }
        return uri.toString();
    }

    public static Map<String, String> getAuthMap(BMap config) {

        final BMap secureSocket = config.getMapValue(StringUtils.fromString(
                FTPConstants.ENDPOINT_CONFIG_SECURE_SOCKET));
        String username = null;
        String password = null;
        if (secureSocket != null) {
            final BMap basicAuth = secureSocket.getMapValue(StringUtils.fromString(
                    FTPConstants.ENDPOINT_CONFIG_BASIC_AUTH));
            if (basicAuth != null) {
                username = (basicAuth.getStringValue(StringUtils.fromString(FTPConstants.ENDPOINT_CONFIG_USERNAME)))
                        .getValue();
                password = (basicAuth.getStringValue(StringUtils.fromString(FTPConstants.ENDPOINT_CONFIG_PASS_KEY)))
                        .getValue();
            }
        }
        Map<String, String> authMap = new HashMap<>();
        authMap.put(FTPConstants.ENDPOINT_CONFIG_USERNAME, username);
        authMap.put(FTPConstants.ENDPOINT_CONFIG_PASS_KEY, password);

        return authMap;
    }

    /**
     * Creates an error message.
     *
     * @param error   the cause for the error.
     * @param details the detailed message of the error.
     * @return an error which will be propagated to ballerina user.
     */
    public static BError createError(String error, String details) {
        return ErrorCreator.createError(StringUtils.fromString(error), StringUtils.fromString(details));
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
    public static BType getFileInfoType() {

        BMap<BString, Object> fileInfoStruct = ValueCreator.createRecordValue(new Module(
                FTPConstants.FTP_ORG_NAME, FTPConstants.FTP_MODULE_NAME, FTPConstants.FTP_MODULE_VERSION),
                FTPConstants.FTP_FILE_INFO);
        return (BAnnotatableType) fileInfoStruct.getType();
    }

    public static ByteArrayInputStream compress(InputStream inputStream, String targetFilePath) {

        String fileName = new File(targetFilePath).getName();
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);

        try {

            zipOutputStream.putNextEntry(new ZipEntry(fileName));
            byte[] buffer = new byte[1000];
            int len;
            while ((len = bufferedInputStream.read(buffer)) > 0) {
                zipOutputStream.write(buffer, 0, len);
            }
            return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

        } catch (IOException ex) {
            log.error("The file does not exist");
        } finally {
            try {
                bufferedInputStream.close();
                zipOutputStream.closeEntry();
                zipOutputStream.close();
            } catch (IOException e) {
                log.error("Error in closing stream");
            }
        }
        return null;
    }

    public static String getCompressedFileName(String fileName) {

        return fileName.substring(0, fileName.lastIndexOf('.')).concat(".zip");
    }
}
