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

package org.ballerinalang.ftp.util;

import org.ballerinalang.jvm.values.ErrorValue;
import org.ballerinalang.jvm.values.MapValue;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Utils class for FTP client operations.
 */
public class FTPUtil {

    private static final String FTP_ERROR_CODE = "{wso2/ftp}FTPError";
    private static final String FTP_ERROR = "FTPError";

    public static boolean notValidProtocol(String url) {

        return !url.startsWith("ftp") && !url.startsWith("sftp") && !url.startsWith("ftps");
    }

    public static boolean validProtocol(String url) {

        return url.startsWith("ftp://") || url.startsWith("sftp://") || url.startsWith("ftps://");
    }

    public static String createUrl(String protocol, String host, int port, String username, String passPhrase,
                                   String basePath) throws BallerinaFTPException {

        String userInfo = username + ":" + passPhrase;
        URI uri = null;
        try {
            uri = new URI(protocol, userInfo, host, port, basePath, null, null);
        } catch (URISyntaxException e) {
            throw new BallerinaFTPException("Error occurred while constructing a URI from host: " + host +
                    ", port: " + port + ", username: " + username + " and basePath: " + basePath + e.getMessage(), e);
        }
        return uri.toString();
    }

    /**
     * Creates an error message.
     *
     * @param errMsg the cause for the error.
     * @return an error which will be propagated to ballerina user.
     */
    public static ErrorValue createError(String errMsg) {

        return new ErrorValue(FTP_ERROR_CODE, errMsg);
    }

    /**
     * Gets an int from the {@link MapValue} config.
     *
     * @param config the config
     * @param key    the key that has an integer value
     * @param logger the logger to log errors
     * @return the relevant int value from the config
     */
    public static int getIntFromConfig(MapValue config, String key, Logger logger) {

        return getIntFromLong(config.getIntValue(key), key, logger);
    }

    /**
     * Gets an integer from a long value. Handles errors appropriately.
     *
     * @param longVal the long value.
     * @param name    the name of the long value: useful for logging the error.
     * @param logger  the logger to log errors
     * @return the int value from the given long value
     */
    public static int getIntFromLong(long longVal, String name, Logger logger) {

        if (longVal <= 0) {
            return -1;
        }
        try {
            return Math.toIntExact(longVal);
        } catch (ArithmeticException e) {
            logger.warn("The value set for {} needs to be less than {}. The {} value is set to {}", name,
                    Integer.MAX_VALUE, name, Integer.MAX_VALUE);
            return Integer.MAX_VALUE;
        }
    }
}
