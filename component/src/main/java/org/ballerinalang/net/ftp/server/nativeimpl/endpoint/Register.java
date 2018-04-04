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

package org.ballerinalang.net.ftp.server.nativeimpl.endpoint;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.connector.api.BLangConnectorSPIUtil;
import org.ballerinalang.connector.api.BallerinaConnectorException;
import org.ballerinalang.connector.api.Resource;
import org.ballerinalang.connector.api.Service;
import org.ballerinalang.connector.api.Struct;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.net.ftp.server.Constants;
import org.ballerinalang.net.ftp.server.FTPFileSystemListener;
import org.ballerinalang.util.codegen.PackageInfo;
import org.ballerinalang.util.codegen.StructInfo;
import org.wso2.transport.remotefilesystem.RemoteFileSystemConnectorFactory;
import org.wso2.transport.remotefilesystem.exception.RemoteFileSystemConnectorException;
import org.wso2.transport.remotefilesystem.impl.RemoteFileSystemConnectorFactoryImpl;
import org.wso2.transport.remotefilesystem.server.connector.contract.RemoteFileSystemServerConnector;

import java.util.HashMap;
import java.util.Map;

/**
 * Register remote FTP server listener service.
 */

@BallerinaFunction(
        orgName = "ballerina",
        packageName = "net.ftp",
        functionName = "register",
        receiver = @Receiver(type = TypeKind.STRUCT, structType = "ServiceEndpoint",
                             structPackage = "ballerina.net.ftp"),
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
            serviceEndpoint.addNativeData(Constants.FTP_SERVER_CONNECTOR, serverConnector);
        } catch (RemoteFileSystemConnectorException e) {
            throw new BallerinaConnectorException("Unable to initialize server connector", e);
        }
        context.setReturnValues();
    }

    private StructInfo getStrcutIno(Context context) {
        PackageInfo httpPackageInfo = context.getProgramFile().getPackageInfo(Constants.FTP_PACKAGE_NAME);
        return httpPackageInfo.getStructInfo(Constants.FTP_SERVER_EVENT);
    }

    private Map<String, String> getServerConnectorParamMap(Struct serviceEndpointConfig) {
        Map<String, String> params = new HashMap<>(19);
        addProperty(serviceEndpointConfig, params, Constants.ANNOTATION_DIR_URI, null);
        addProperty(serviceEndpointConfig, params, Constants.ANNOTATION_FILE_PATTERN, null);
        addProperty(serviceEndpointConfig, params, Constants.ANNOTATION_POLLING_INTERVAL, null);
        addProperty(serviceEndpointConfig, params, Constants.ANNOTATION_CRON_EXPRESSION, null);
        addProperty(serviceEndpointConfig, params, Constants.ANNOTATION_FILE_COUNT, null);
        addProperty(serviceEndpointConfig, params, Constants.ANNOTATION_SORT_ATTRIBUTE, null);
        addProperty(serviceEndpointConfig, params, Constants.ANNOTATION_SORT_ASCENDING, null);
        addProperty(serviceEndpointConfig, params, Constants.ANNOTATION_ACTION_AFTER_PROCESS, null);
        addProperty(serviceEndpointConfig, params, Constants.ANNOTATION_ACTION_AFTER_FAILURE, null);
        addProperty(serviceEndpointConfig, params, Constants.ANNOTATION_MOVE_AFTER_PROCESS, null);
        addProperty(serviceEndpointConfig, params, Constants.ANNOTATION_MOVE_AFTER_FAILURE, null);
        addProperty(serviceEndpointConfig, params, Constants.ANNOTATION_MOVE_TIMESTAMP_FORMAT, null);
        addProperty(serviceEndpointConfig, params, Constants.ANNOTATION_CREATE_DIR, null);
        addProperty(serviceEndpointConfig, params, Constants.ANNOTATION_PARALLEL, null);
        addProperty(serviceEndpointConfig, params, Constants.ANNOTATION_THREAD_POOL_SIZE, null);
        addProperty(serviceEndpointConfig, params, Constants.ANNOTATION_SFTP_IDENTITIES, null);
        addProperty(serviceEndpointConfig, params, Constants.ANNOTATION_SFTP_IDENTITY_PASS_PHRASE, null);
        addProperty(serviceEndpointConfig, params, Constants.ANNOTATION_SFTP_USER_DIR_IS_ROOT, String.valueOf(false));
        addProperty(serviceEndpointConfig, params, Constants.ANNOTATION_AVOID_PERMISSION_CHECK, String.valueOf(true));
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
