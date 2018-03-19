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

package org.ballerinalang.net.ftp.client.nativeimpl.actions;

import org.ballerinalang.bre.Context;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BConnector;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.natives.annotations.ReturnType;
import org.ballerinalang.net.ftp.client.nativeimpl.util.ClientUtil;
import org.ballerinalang.net.ftp.client.nativeimpl.util.FTPConstants;
import org.wso2.transport.remotefilesystem.client.connector.contract.VFSClientConnector;
import org.wso2.transport.remotefilesystem.client.connector.contractimpl.VFSClientConnectorImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * Move a file or a folder from one place to another
 */
@BallerinaFunction(
        packageName = "ballerina.net.ftp",
        functionName = "move",
        receiver = @Receiver(type = TypeKind.STRUCT, structType = "ClientConnector",
                             structPackage = "ballerina.net.ftp"),
        args = {@Argument(name = "ftpClientConnector", type = TypeKind.CONNECTOR),
                @Argument(name = "source", type = TypeKind.STRUCT, structType = "File",
                        structPackage = "ballerina.lang.files"),
                @Argument(name = "destination", type = TypeKind.STRUCT, structType = "File",
                        structPackage = "ballerina.lang.files")},
        returnType = {@ReturnType(type = TypeKind.STRUCT, structType = "FTPClientError",
                                  structPackage = "ballerina.net.ftp")
        }
)
public class Move extends AbstractFtpAction {

    @Override
    public void execute(Context context) {
        BConnector clientConnector = (BConnector) context.getRefArgument(0);
        String url = (String) clientConnector.getNativeData(FTPConstants.URL);
        BStruct source = (BStruct) context.getRefArgument(1);
        BStruct destination = (BStruct) context.getRefArgument(2);

        //Create property map to send to transport.
        Map<String, String> propertyMap = new HashMap<>(5);
        propertyMap.put(FTPConstants.PROPERTY_URI, url + source.getStringField(0));
        String destinationUrl = destination.getStringField(0);
        if (!ClientUtil.validProtocol(destinationUrl)) {
            destinationUrl = url + destinationUrl;
        }
        propertyMap.put(FTPConstants.PROPERTY_DESTINATION, destinationUrl);
        propertyMap.put(FTPConstants.PROPERTY_ACTION, FTPConstants.ACTION_MOVE);
        propertyMap.put(FTPConstants.PROTOCOL, FTPConstants.PROTOCOL_FTP);
        propertyMap.put(FTPConstants.FTP_PASSIVE_MODE, Boolean.TRUE.toString());

        FTPClientConnectorListener connectorListener = new FTPClientConnectorListener(context);
        VFSClientConnector connector = new VFSClientConnectorImpl(propertyMap, connectorListener);
        connector.send(null);
    }
}
