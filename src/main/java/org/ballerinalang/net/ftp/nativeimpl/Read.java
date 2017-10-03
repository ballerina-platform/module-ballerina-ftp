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
import org.ballerinalang.connector.api.BallerinaConnectorException;
import org.ballerinalang.connector.api.ConnectorFuture;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BBlob;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.nativeimpl.actions.ClientConnectorFuture;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaAction;
import org.ballerinalang.natives.annotations.ReturnType;
import org.ballerinalang.net.ftp.nativeimpl.util.FileConstants;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.wso2.carbon.transport.remotefilesystem.client.connector.contract.VFSClientConnector;
import org.wso2.carbon.transport.remotefilesystem.client.connector.contract.VFSClientConnectorFuture;
import org.wso2.carbon.transport.remotefilesystem.client.connector.contractimpl.VFSClientConnectorImpl;
import org.wso2.carbon.transport.remotefilesystem.listener.RemoteFileSystemListener;
import org.wso2.carbon.transport.remotefilesystem.message.RemoteFileSystemBaseMessage;
import org.wso2.carbon.transport.remotefilesystem.message.RemoteFileSystemMessage;

import java.util.HashMap;
import java.util.Map;

/**
* Read.
*/
@BallerinaAction(
        packageName = "ballerina.net.ftp",
        actionName = "read",
        connectorName = FileConstants.CONNECTOR_NAME,
        args = {@Argument(name = "ftpClientConnector", type = TypeKind.CONNECTOR),
                @Argument(name = "file", type = TypeKind.STRUCT, structType = "File",
                         structPackage = "ballerina.lang.files")},
        returnType = {@ReturnType(type = TypeKind.BLOB)}
)
public class Read extends AbstractFtpAction {
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
        propertyMap.put(FileConstants.PROPERTY_URI, pathString);
        propertyMap.put(FileConstants.PROPERTY_ACTION, FileConstants.ACTION_READ);

        ClientConnectorFuture connectorFuture = new ClientConnectorFuture();
        FTPReadClientConnectorListener connectorListener = new FTPReadClientConnectorListener(connectorFuture);

        VFSClientConnector connector = new VFSClientConnectorImpl("", propertyMap, connectorListener);
        VFSClientConnectorFuture future = connector.send(null);
        future.setFileSystemListener(connectorListener);
        return connectorFuture;
    }

    private static class FTPReadClientConnectorListener implements RemoteFileSystemListener {

        private ClientConnectorFuture ballerinaFuture;

        FTPReadClientConnectorListener(ClientConnectorFuture ballerinaFuture) {
            this.ballerinaFuture = ballerinaFuture;
        }

        @Override
        public void onMessage(RemoteFileSystemBaseMessage remoteFileSystemBaseMessage) {
            BBlob blob = new BBlob(((RemoteFileSystemMessage) remoteFileSystemBaseMessage).getBytes().array());
            ballerinaFuture.notifyReply(blob);
        }

        @Override
        public void onError(Throwable throwable) {
            BallerinaConnectorException ex = new BallerinaConnectorException(throwable);
            ballerinaFuture.notifyFailure(ex);
        }
    }
}

