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

package org.ballerinalang.net.ftp.server;

import org.ballerinalang.connector.api.ConnectorFuture;
import org.ballerinalang.connector.api.ConnectorFutureListener;
import org.ballerinalang.connector.api.ConnectorUtils;
import org.ballerinalang.connector.api.Executor;
import org.ballerinalang.connector.api.Resource;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.model.values.BValue;
import org.wso2.carbon.transport.remotefilesystem.listener.RemoteFileSystemListener;
import org.wso2.carbon.transport.remotefilesystem.message.RemoteFileSystemBaseMessage;
import org.wso2.carbon.transport.remotefilesystem.message.RemoteFileSystemMessage;

/**
 * File System connector listener for Ballerina.
 */
public class BallerinaFTPFileSystemListener implements RemoteFileSystemListener {

    @Override
    public void onMessage(RemoteFileSystemBaseMessage remoteFileSystemBaseMessage) {
        RemoteFileSystemMessage remoteFileSystemMessage = (RemoteFileSystemMessage) remoteFileSystemBaseMessage;
        Resource resource = FTPServerConnectorResourceDispatcher.findResource(remoteFileSystemMessage);
        BValue[] parameters = getSignatureParameters(resource, remoteFileSystemMessage);
        ConnectorFuture future = Executor.submit(resource, null, parameters);
        ConnectorFutureListener futureListener = new FTPConnectorFutureListener(remoteFileSystemMessage);
        future.setConnectorFutureListener(futureListener);
    }

    private BValue[] getSignatureParameters(Resource resource, RemoteFileSystemMessage fileSystemMessage) {
        BStruct request = ConnectorUtils.createStruct(resource, Constants.FTP_PACKAGE_NAME,
                Constants.FTP_SERVER_EVENT);
        request.setStringField(0, fileSystemMessage.getText());
        BValue[] bValues = new BValue[1];
        bValues[0] = request;
        return bValues;
    }

    @Override
    public void onError(Throwable throwable) {

    }
}
