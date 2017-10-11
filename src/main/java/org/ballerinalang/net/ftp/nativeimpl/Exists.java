/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinalang.net.ftp.nativeimpl;

import org.ballerinalang.bre.Context;
import org.ballerinalang.connector.api.ConnectorFuture;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BBoolean;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.nativeimpl.actions.ClientConnectorFuture;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaAction;
import org.ballerinalang.natives.annotations.ReturnType;
import org.ballerinalang.net.ftp.nativeimpl.util.FTPConstants;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.wso2.carbon.transport.remotefilesystem.client.connector.contract.VFSClientConnector;
import org.wso2.carbon.transport.remotefilesystem.client.connector.contract.VFSClientConnectorFuture;
import org.wso2.carbon.transport.remotefilesystem.client.connector.contractimpl.VFSClientConnectorImpl;
import org.wso2.carbon.transport.remotefilesystem.message.RemoteFileSystemBaseMessage;
import org.wso2.carbon.transport.remotefilesystem.message.RemoteFileSystemMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * Checks the existence of a file.
 */
@BallerinaAction(
        packageName = "ballerina.net.ftp",
        actionName = "exists",
        connectorName = FTPConstants.CONNECTOR_NAME,
        args = {@Argument(name = "ftpClientConnector", type = TypeKind.CONNECTOR),
                @Argument(name = "file", type = TypeKind.STRUCT, structType = "File",
                         structPackage = "ballerina.lang.files") },
        returnType = {@ReturnType(type = TypeKind.BOOLEAN)})
public class Exists extends AbstractFtpAction {
    @Override
    public ConnectorFuture execute(Context context) {

        // Extracting Argument values
        BStruct file = (BStruct) getRefArgument(context, 1);
        if (!validateProtocol(file.getStringField(0))) {
            throw new BallerinaException("Only FTP, SFTP and FTPS protocols are supported by this connector");
        }
        //Create property map to send to transport.
        Map<String, String> propertyMap = new HashMap<>();
        String pathString = file.getStringField(0);
        propertyMap.put(FTPConstants.PROPERTY_URI, pathString);
        propertyMap.put(FTPConstants.PROPERTY_ACTION, FTPConstants.ACTION_EXISTS);
        propertyMap.put(FTPConstants.PROTOCOL, FTPConstants.PROTOCOL_FTP);
        propertyMap.put(FTPConstants.FTP_PASSIVE_MODE, Boolean.TRUE.toString());

        ClientConnectorFuture connectorFuture = new ClientConnectorFuture();
        FTPExistsClientConnectorListener connectorListener = new FTPExistsClientConnectorListener(connectorFuture);
        VFSClientConnector connector = new VFSClientConnectorImpl(propertyMap, connectorListener);
        VFSClientConnectorFuture future = connector.send(null);
        future.setFileSystemListener(connectorListener);
        return connectorFuture;
    }

    private static class FTPExistsClientConnectorListener extends FTPClientConnectorListener {

        FTPExistsClientConnectorListener(ClientConnectorFuture ballerinaFuture) {
            super(ballerinaFuture);
        }

        @Override
        public void onMessage(RemoteFileSystemBaseMessage remoteFileSystemBaseMessage) {
            BBoolean value = new BBoolean(Boolean.parseBoolean(
                    ((RemoteFileSystemMessage) remoteFileSystemBaseMessage).getText()));
            getBallerinaFuture().notifyReply(value);
        }
    }
}
