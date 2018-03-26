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

package org.ballerinalang.net.fs.navtiveimpl;

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
import org.ballerinalang.net.fs.server.Constants;
import org.ballerinalang.net.fs.server.LFSListener;
import org.ballerinalang.util.codegen.PackageInfo;
import org.ballerinalang.util.codegen.StructInfo;
import org.wso2.transport.localfilesystem.server.connector.contract.LocalFileSystemConnectorFactory;
import org.wso2.transport.localfilesystem.server.connector.contract.LocalFileSystemServerConnector;
import org.wso2.transport.localfilesystem.server.connector.contractimpl.LocalFileSystemConnectorFactoryImpl;
import org.wso2.transport.localfilesystem.server.exception.LocalFileSystemServerConnectorException;

import java.util.HashMap;
import java.util.Map;

/**
 * Register file listener service.
 */

@BallerinaFunction(
        orgName = "ballerina",
        packageName = "net.fs",
        functionName = "register",
        receiver = @Receiver(type = TypeKind.STRUCT, structType = "ServiceEndpoint",
                             structPackage = "ballerina.net.fs"),
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
            Map<String, String> paramMap = getParamMap(serviceEndpointConfig);
            LocalFileSystemConnectorFactory connectorFactory = new LocalFileSystemConnectorFactoryImpl();
            final Resource resource = service.getResources()[0];
            StructInfo structInfo = getStrcutIno(context);
            LocalFileSystemServerConnector serverConnector = connectorFactory
                    .createServerConnector(service.getName(), paramMap, new LFSListener(resource, structInfo));
            serviceEndpoint.addNativeData(Constants.FS_SERVER_CONNECTOR, serverConnector);
        } catch (LocalFileSystemServerConnectorException e) {
            throw new BallerinaConnectorException("Unable to initialize server connector", e);
        }
        context.setReturnValues();
    }

    private StructInfo getStrcutIno(Context context) {
        PackageInfo httpPackageInfo = context.getProgramFile().getPackageInfo(Constants.FILE_SYSTEM_PACKAGE_NAME);
        return httpPackageInfo.getStructInfo(Constants.FILE_SYSTEM_EVENT);
    }

    private Map<String, String> getParamMap(Struct serviceEndpointConfig) {
        final String dirURI = serviceEndpointConfig.getStringField(Constants.ANNOTATION_DIR_URI);
        final String events = serviceEndpointConfig.getStringField(Constants.ANNOTATION_EVENTS);
        final boolean recursive = serviceEndpointConfig.getBooleanField(Constants.ANNOTATION_DIRECTORY_RECURSIVE);
        Map<String, String> paramMap = new HashMap<>(3);
        if (dirURI != null && !dirURI.isEmpty()) {
            paramMap.put(Constants.ANNOTATION_DIR_URI, dirURI);
        }
        if (events != null && !events.isEmpty()) {
            paramMap.put(Constants.ANNOTATION_EVENTS, events);
        }
        paramMap.put(Constants.ANNOTATION_DIRECTORY_RECURSIVE, String.valueOf(recursive));
        return paramMap;
    }
}
