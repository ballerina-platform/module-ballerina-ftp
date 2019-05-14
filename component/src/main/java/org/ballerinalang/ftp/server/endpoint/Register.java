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

package org.ballerinalang.ftp.server.endpoint;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.connector.api.BLangConnectorSPIUtil;
import org.ballerinalang.connector.api.BallerinaConnectorException;
import org.ballerinalang.connector.api.Resource;
import org.ballerinalang.connector.api.Service;
import org.ballerinalang.connector.api.Struct;
import org.ballerinalang.connector.api.Value;
import org.ballerinalang.ftp.server.FTPFileSystemListener;
import org.ballerinalang.ftp.util.FTPUtil;
import org.ballerinalang.ftp.util.FtpConstants;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.wso2.transport.remotefilesystem.Constants;
import org.wso2.transport.remotefilesystem.RemoteFileSystemConnectorFactory;
import org.wso2.transport.remotefilesystem.exception.RemoteFileSystemConnectorException;
import org.wso2.transport.remotefilesystem.impl.RemoteFileSystemConnectorFactoryImpl;
import org.wso2.transport.remotefilesystem.server.connector.contract.RemoteFileSystemServerConnector;

import java.util.HashMap;
import java.util.Map;

import static org.ballerinalang.ftp.util.FtpConstants.FTP_PACKAGE_NAME;

/**
 * Register remote FTP server listener service.
 */

@BallerinaFunction(
        orgName = "wso2",
        packageName = "ftp:0.0.0",
        functionName = "register",
        receiver = @Receiver(type = TypeKind.OBJECT, structType = "Listener", structPackage = FTP_PACKAGE_NAME)
)
public class Register extends BlockingNativeCallableUnit {
    @Override
    public void execute(Context context) {
        Service service = BLangConnectorSPIUtil.getServiceRegistered(context);
        Struct serviceEndpoint = BLangConnectorSPIUtil.getConnectorEndpointStruct(context);
        Struct serviceEndpointConfig = serviceEndpoint.getStructField("config");
        try {
            Map<String, String> paramMap = getServerConnectorParamMap(serviceEndpointConfig);
            RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
            final Resource resource = service.getResources()[0];
            final FTPFileSystemListener listener = new FTPFileSystemListener(resource,
                    context.getProgramFile().getPackageInfo(FTP_PACKAGE_NAME));
            RemoteFileSystemServerConnector serverConnector = fileSystemConnectorFactory
                    .createServerConnector(service.getName(), paramMap, listener);
            serviceEndpoint.addNativeData(FtpConstants.FTP_SERVER_CONNECTOR, serverConnector);
            // This is a temporary solution.
            serviceEndpointConfig.addNativeData(FtpConstants.FTP_SERVER_CONNECTOR, serverConnector);
        } catch (RemoteFileSystemConnectorException e) {
            throw new BallerinaConnectorException("Unable to initialize the FTP listener: " + e.getMessage(), e);
        }
        context.setReturnValues();
    }

    private Map<String, String> getServerConnectorParamMap(Struct serviceEndpointConfig) {
        Map<String, String> params = new HashMap<>(12);
        final String path = serviceEndpointConfig.getStringField(FtpConstants.ENDPOINT_CONFIG_PATH);
        Value protocol = serviceEndpointConfig.getRefField(FtpConstants.ENDPOINT_CONFIG_PROTOCOL);
        final String host = serviceEndpointConfig.getStringField(FtpConstants.ENDPOINT_CONFIG_HOST);
        int port = (int) serviceEndpointConfig.getIntField(FtpConstants.ENDPOINT_CONFIG_PORT);

        final Struct secureSocket = serviceEndpointConfig.getStructField(FtpConstants.ENDPOINT_CONFIG_SECURE_SOCKET);
        String username = null;
        String password = null;
        if (secureSocket != null) {
            final Struct basicAuth = secureSocket.getStructField(FtpConstants.ENDPOINT_CONFIG_BASIC_AUTH);
            if (basicAuth != null) {
                username = basicAuth.getStringField(FtpConstants.ENDPOINT_CONFIG_USERNAME);
                password = basicAuth.getStringField(FtpConstants.ENDPOINT_CONFIG_PASSWORD);
            }
        }
        String url = FTPUtil.createUrl(protocol.getStringValue(), host, port, username, password, path);
        params.put(Constants.URI, url);
        addStringProperty(serviceEndpointConfig, params, Constants.FILE_NAME_PATTERN,
                FtpConstants.ENDPOINT_CONFIG_FILE_PATTERN);
        if (secureSocket != null) {
            final Struct privateKey = secureSocket.getStructField(FtpConstants.ENDPOINT_CONFIG_PRIVATE_KEY);
            if (privateKey != null) {
                final String privateKeyPath = privateKey.getStringField(FtpConstants.ENDPOINT_CONFIG_FILE_PATH);
                if (privateKeyPath != null && !privateKeyPath.isEmpty()) {
                    params.put(Constants.IDENTITY, privateKeyPath);
                    final String privateKeyPassword = privateKey.getStringField(FtpConstants.ENDPOINT_CONFIG_PASSWORD);
                    if (privateKeyPassword != null && !privateKeyPassword.isEmpty()) {
                        params.put(Constants.IDENTITY_PASS_PHRASE, privateKeyPassword);
                    }
                }
            }
        }
        params.put(Constants.USER_DIR_IS_ROOT, String.valueOf(false));
        params.put(Constants.AVOID_PERMISSION_CHECK, String.valueOf(true));
        params.put(Constants.PASSIVE_MODE, String.valueOf(true));
        return params;
    }

    private void addStringProperty(Struct config, Map<String, String> params, String transportKey, String endpointKey) {
        final String value = config.getStringField(endpointKey);
        if (value != null && !value.isEmpty()) {
            params.put(transportKey, value);
        }
    }
}
