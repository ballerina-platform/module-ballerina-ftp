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

package org.ballerinalang.net.ftp.client.nativeimpl.clientendpoint;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.connector.api.BLangConnectorSPIUtil;
import org.ballerinalang.connector.api.Struct;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BConnector;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.natives.annotations.ReturnType;
import org.ballerinalang.net.ftp.client.nativeimpl.util.FTPConstants;

/**
 * Get the client endpoint.
 */

@BallerinaFunction(
        packageName = "ballerina.net.ftp",
        functionName = "getConnector",
        receiver = @Receiver(type = TypeKind.STRUCT, structType = "Client",
                             structPackage = "ballerina.net.ftp"),
        returnType = {@ReturnType(type = TypeKind.CONNECTOR)},
        isPublic = true
)
public class GetConnector extends BlockingNativeCallableUnit {

    @Override
    public void execute(Context context) {
        Struct clientEndpoint = BLangConnectorSPIUtil.getConnectorEndpointStruct(context);
        BConnector clientConnector;
        BStruct clientEndPoint = (BStruct) context.getRefArgument(0);
        if (clientEndPoint.getNativeData("BConnector") != null) {
            clientConnector = (BConnector) clientEndPoint.getNativeData("BConnector");
        } else {
            BStruct clientEndpointConfig = (BStruct) clientEndPoint.getRefField(0);
            clientConnector = BLangConnectorSPIUtil
                    .createBConnector(context.getProgramFile(), "ballerina.net.ftp", "ClientConnector",
                            clientEndpointConfig);
        }
        final String url = (String) clientEndpoint.getNativeData(FTPConstants.URL);
        clientConnector.setNativeData(FTPConstants.URL, url);
        context.setReturnValues(clientConnector);
    }
}
