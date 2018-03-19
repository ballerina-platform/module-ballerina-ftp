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
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.net.ftp.client.nativeimpl.util.ClientUtil;
import org.ballerinalang.net.ftp.client.nativeimpl.util.FTPConstants;
import org.ballerinalang.util.exceptions.BallerinaException;

/**
 * Initialization of client endpoint.
 */

@BallerinaFunction(
        packageName = "ballerina.net.ftp",
        functionName = "initEndpoint",
        receiver = @Receiver(type = TypeKind.STRUCT, structType = "ClientEndpoint",
                             structPackage = "ballerina.net.ftp"),
        args = {@Argument(name = "epName", type = TypeKind.STRING),
                @Argument(name = "config", type = TypeKind.STRUCT, structType = "ClientEndpointConfiguration")},
        isPublic = true
)
public class InitEndpoint extends BlockingNativeCallableUnit {

    @Override
    public void execute(Context context) {
        Struct clientEndpoint = BLangConnectorSPIUtil.getConnectorEndpointStruct(context);
        Struct clientEndpointConfig = clientEndpoint.getStructField("config");
        String protocol = clientEndpointConfig.getStringField("protocol");
        String host = clientEndpointConfig.getStringField("host");
        long port = clientEndpointConfig.getIntField("port");
        String username = clientEndpointConfig.getStringField("username");
        String passPhrase = clientEndpointConfig.getStringField("passPhrase");
        if (protocol != null) {
            if (protocol.isEmpty()) {
                protocol = "tcp";
            } else if (ClientUtil.notValidProtocol(protocol)) {
                throw new BallerinaException("Only FTP, SFTP and FTPS protocols are supported by this connector");
            }
        }
        String url = createUrl(protocol, host, port, username, passPhrase);
        clientEndpoint.addNativeData(FTPConstants.URL, url);
        context.setReturnValues();
    }

    private String createUrl(String protocol, String host, long port, String username, String passPhrase) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(protocol);
        urlBuilder.append(":");
        urlBuilder.append("//");
        if (username != null && !username.isEmpty()) {
            urlBuilder.append(username);
            if (passPhrase != null && !passPhrase.isEmpty()) {
                urlBuilder.append(":");
                urlBuilder.append(passPhrase);
            }
            urlBuilder.append("@");
        }
        urlBuilder.append(host);
        if (port > 0) {
            urlBuilder.append(":");
            urlBuilder.append(port);
        }
        return urlBuilder.toString();
    }
}
