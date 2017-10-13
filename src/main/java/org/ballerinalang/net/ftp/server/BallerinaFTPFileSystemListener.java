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
import org.ballerinalang.services.ErrorHandlerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.transport.remotefilesystem.listener.RemoteFileSystemListener;
import org.wso2.carbon.transport.remotefilesystem.message.RemoteFileSystemBaseMessage;
import org.wso2.carbon.transport.remotefilesystem.message.RemoteFileSystemEvent;

/**
 * File System connector listener for Ballerina.
 */
public class BallerinaFTPFileSystemListener implements RemoteFileSystemListener {

    private static final Logger log = LoggerFactory.getLogger(BallerinaFTPFileSystemListener.class);

    @Override
    public void onMessage(RemoteFileSystemBaseMessage remoteFileSystemBaseMessage) {
        if (remoteFileSystemBaseMessage instanceof RemoteFileSystemEvent) {
            RemoteFileSystemEvent event = (RemoteFileSystemEvent) remoteFileSystemBaseMessage;
            Resource resource = FTPServerConnectorResourceDispatcher.findResource(event);
            BValue[] parameters = getSignatureParameters(resource, event);
            ConnectorFuture future = Executor.submit(resource, null, parameters);
            ConnectorFutureListener futureListener = new FTPConnectorFutureListener(event);
            future.setConnectorFutureListener(futureListener);
        }
    }

    private BValue[] getSignatureParameters(Resource resource, RemoteFileSystemEvent fileSystemEvent) {
        BStruct request = ConnectorUtils.createStruct(resource, Constants.FTP_PACKAGE_NAME,
                Constants.FTP_SERVER_EVENT);
        request.setStringField(0, fileSystemEvent.getText());
        BValue[] bValues = new BValue[1];
        bValues[0] = request;
        return bValues;
    }

    @Override
    public void onError(Throwable throwable) {
        ErrorHandlerUtils.printError(throwable);
    }

    @Override
    public void done() {
        log.debug("Successfully finished the action.");
    }
}
