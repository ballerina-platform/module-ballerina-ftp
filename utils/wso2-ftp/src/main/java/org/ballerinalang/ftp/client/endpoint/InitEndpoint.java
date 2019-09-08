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

package org.ballerinalang.ftp.client.endpoint;

import org.ballerinalang.ftp.util.BallerinaFTPException;
import org.ballerinalang.ftp.util.FTPUtil;
import org.ballerinalang.ftp.util.FtpConstants;
import org.ballerinalang.jvm.values.MapValue;
import org.ballerinalang.jvm.values.ObjectValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Initialization of client endpoint.
 */

public class InitEndpoint {

    private static final Logger logger = LoggerFactory.getLogger("ballerina");

    public static void initEndpoint(ObjectValue clientEndpoint, MapValue<Object, Object> config)
            throws BallerinaFTPException {

        String protocol = config.getStringValue(FtpConstants.ENDPOINT_CONFIG_PROTOCOL);
        if (FTPUtil.notValidProtocol(protocol)) {
            throw new BallerinaFTPException("Only FTP, SFTP and FTPS protocols are supported by FTP client.");
        }
        String host = config.getStringValue(FtpConstants.ENDPOINT_CONFIG_HOST);
        int port = FTPUtil.getIntFromConfig(config, FtpConstants.ENDPOINT_CONFIG_PORT, logger);
        logger.info("protocol: " + protocol + " host: " + host + " port: " + port);

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
        logger.info("user: {} -- {}", username, password);

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
        logger.info("clientEP: {}", clientEndpoint.getNativeData());
        //context.setReturnValues();
//        return new HandleValue("");
    }
}
