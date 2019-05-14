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

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.connector.api.BLangConnectorSPIUtil;
import org.ballerinalang.connector.api.Struct;
import org.ballerinalang.connector.api.Value;
import org.ballerinalang.ftp.util.FTPUtil;
import org.ballerinalang.ftp.util.FtpConstants;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.ballerinalang.ftp.util.FtpConstants.FTP_PACKAGE_NAME;

/**
 * Initialization of client endpoint.
 */

@BallerinaFunction(
        orgName = "wso2",
        packageName = "ftp:0.0.0",
        functionName = "initEndpoint",
        receiver = @Receiver(type = TypeKind.OBJECT, structType = "Client", structPackage = FTP_PACKAGE_NAME)
)
public class InitEndpoint extends BlockingNativeCallableUnit {
    private static final Logger log = LoggerFactory.getLogger(InitEndpoint.class);
    @Override
    public void execute(Context context) {
        Struct clientEndpoint = BLangConnectorSPIUtil.getConnectorEndpointStruct(context);
        Struct clientEndpointConfig = clientEndpoint.getStructField("config");
        Value protocol = clientEndpointConfig.getRefField(FtpConstants.ENDPOINT_CONFIG_PROTOCOL);
        if (FTPUtil.notValidProtocol(protocol.getStringValue())) {
            throw new BallerinaException("Only FTP, SFTP and FTPS protocols are supported by FTP client.");
        }
        String host = clientEndpointConfig.getStringField(FtpConstants.ENDPOINT_CONFIG_HOST);
        int port = (int) clientEndpointConfig.getIntField(FtpConstants.ENDPOINT_CONFIG_PORT);

        final Struct secureSocket = clientEndpointConfig.getStructField(FtpConstants.ENDPOINT_CONFIG_SECURE_SOCKET);
        String username = null;
        String password = null;
        if (secureSocket != null) {
            final Struct basicAuth = secureSocket.getStructField(FtpConstants.ENDPOINT_CONFIG_BASIC_AUTH);
            if (basicAuth != null) {
                username = basicAuth.getStringField(FtpConstants.ENDPOINT_CONFIG_USERNAME);
                password = basicAuth.getStringField(FtpConstants.ENDPOINT_CONFIG_PASSWORD);
            }
        }

        clientEndpoint.addNativeData(FtpConstants.ENDPOINT_CONFIG_USERNAME, username);
        clientEndpoint.addNativeData(FtpConstants.ENDPOINT_CONFIG_PASSWORD, password);
        clientEndpoint.addNativeData(FtpConstants.ENDPOINT_CONFIG_HOST, host);
        clientEndpoint.addNativeData(FtpConstants.ENDPOINT_CONFIG_PORT, port);
        clientEndpoint.addNativeData(FtpConstants.ENDPOINT_CONFIG_PROTOCOL, protocol.getStringValue());
        Map<String, String> config = new HashMap<>(3);
        config.put(FtpConstants.FTP_PASSIVE_MODE, String.valueOf(true));
        config.put(FtpConstants.USER_DIR_IS_ROOT, String.valueOf(false));
        config.put(FtpConstants.AVOID_PERMISSION_CHECK, String.valueOf(true));
        clientEndpoint.addNativeData(FtpConstants.PROPERTY_MAP, config);
        context.setReturnValues();
    }
}
