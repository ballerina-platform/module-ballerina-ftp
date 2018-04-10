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
import org.ballerinalang.ftp.server.FTPFileSystemListener;
import org.ballerinalang.ftp.util.FTPUtil;
import org.ballerinalang.ftp.util.ServerConstants;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.util.codegen.PackageInfo;
import org.ballerinalang.util.codegen.StructInfo;
import org.wso2.transport.remotefilesystem.RemoteFileSystemConnectorFactory;
import org.wso2.transport.remotefilesystem.exception.RemoteFileSystemConnectorException;
import org.wso2.transport.remotefilesystem.impl.RemoteFileSystemConnectorFactoryImpl;
import org.wso2.transport.remotefilesystem.server.connector.contract.RemoteFileSystemServerConnector;

import java.util.HashMap;
import java.util.Map;

import static org.ballerinalang.ftp.util.ServerConstants.FTP_PACKAGE_NAME;
import static org.wso2.transport.remotefilesystem.Constants.ACTION_NONE;

/**
 * Register remote FTP server listener service.
 */

@BallerinaFunction(
        orgName = "wso2",
        packageName = "ftp",
        functionName = "register",
        receiver = @Receiver(type = TypeKind.STRUCT, structType = "Listener", structPackage = FTP_PACKAGE_NAME),
        args = {@Argument(name = "serviceType", type = TypeKind.TYPEDESC)},
        isPublic = true
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
            StructInfo structInfo = getStrcutIno(context);
            RemoteFileSystemServerConnector serverConnector = fileSystemConnectorFactory
                    .createServerConnector(service.getName(), paramMap,
                            new FTPFileSystemListener(resource, structInfo));
            serviceEndpoint.addNativeData(ServerConstants.FTP_SERVER_CONNECTOR, serverConnector);
        } catch (RemoteFileSystemConnectorException e) {
            throw new BallerinaConnectorException("Unable to initialize server connector", e);
        }
        context.setReturnValues();
    }

    private StructInfo getStrcutIno(Context context) {
        PackageInfo httpPackageInfo = context.getProgramFile().getPackageInfo(FTP_PACKAGE_NAME);
        return httpPackageInfo.getStructInfo(ServerConstants.FTP_SERVER_EVENT);
    }

    private Map<String, String> getServerConnectorParamMap(Struct serviceEndpointConfig) {
        Map<String, String> params = new HashMap<>(15);
        final String path = serviceEndpointConfig.getStringField(ServerConstants.ANNOTATION_PATH);
        String protocol = serviceEndpointConfig.getStringField(ServerConstants.ANNOTATION_PROTOCOL);
        final String host = serviceEndpointConfig.getStringField(ServerConstants.ANNOTATION_HOST);
        final long port = serviceEndpointConfig.getIntField(ServerConstants.ANNOTATION_PORT);
        final String username = serviceEndpointConfig.getStringField(ServerConstants.ANNOTATION_USERNAME);
        final String passPhrase = serviceEndpointConfig.getStringField(ServerConstants.ANNOTATION_PASSPHRASE);
        if (protocol != null && protocol.isEmpty()) {
            protocol = "tcp";
        }
        String url = FTPUtil.createUrl(protocol, host, port, username, passPhrase, path);
        params.put(ServerConstants.ANNOTATION_DIR_URI, url);
        addProperty(serviceEndpointConfig, params, ServerConstants.ANNOTATION_FILE_PATTERN, null);
        addProperty(serviceEndpointConfig, params, ServerConstants.ANNOTATION_POLLING_INTERVAL, null);
        addProperty(serviceEndpointConfig, params, ServerConstants.ANNOTATION_CRON_EXPRESSION, null);
        addProperty(serviceEndpointConfig, params, ServerConstants.ANNOTATION_FILE_COUNT, null);
        addProperty(serviceEndpointConfig, params, ServerConstants.ANNOTATION_PARALLEL, null);
        addProperty(serviceEndpointConfig, params, ServerConstants.ANNOTATION_THREAD_POOL_SIZE, null);
        addProperty(serviceEndpointConfig, params, ServerConstants.ANNOTATION_SFTP_IDENTITIES, null);
        addProperty(serviceEndpointConfig, params, ServerConstants.ANNOTATION_SFTP_IDENTITY_PASS_PHRASE, null);
        addProperty(serviceEndpointConfig, params, ServerConstants.ANNOTATION_SFTP_USER_DIR_IS_ROOT,
                String.valueOf(false));
        addProperty(serviceEndpointConfig, params, ServerConstants.ANNOTATION_AVOID_PERMISSION_CHECK,
                String.valueOf(true));
        params.put(ServerConstants.ANNOTATION_ACTION_AFTER_FAILURE, ACTION_NONE);
        params.put(ServerConstants.ANNOTATION_MOVE_AFTER_FAILURE, ACTION_NONE);
        params.put(ServerConstants.ANNOTATION_MOVE_AFTER_PROCESS, ACTION_NONE);
        params.put(ServerConstants.ANNOTATION_ACTION_AFTER_PROCESS, ACTION_NONE);
        return params;
    }

    private void addProperty(Struct config, Map<String, String> params, String key, String defaultValue) {
        final String value = config.getStringField(key);
        if (value != null && !value.isEmpty()) {
            params.put(key, value);
        } else if (defaultValue != null) {
            params.put(key, defaultValue);
        }
    }
}
