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

package org.ballerinalang.net.fs.server;

import org.ballerinalang.connector.api.ConnectorFuture;
import org.ballerinalang.connector.api.ConnectorFutureListener;
import org.ballerinalang.connector.api.ConnectorUtils;
import org.ballerinalang.connector.api.Executor;
import org.ballerinalang.connector.api.Resource;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.model.values.BValue;
import org.wso2.carbon.transport.localfilesystem.server.connector.contract.LocalFileSystemListener;
import org.wso2.carbon.transport.localfilesystem.server.connector.contract.LocalFileSystemMessage;

/**
 * File System connector listener for Ballerina.
 */
public class BallerinaLocalFileSystemListener implements LocalFileSystemListener {

    @Override
    public void onMessage(LocalFileSystemMessage fileSystemMessage) {
        Resource resource = LocalFileSystemResouceDispatcher.findResource(fileSystemMessage);
        BValue[] parameters = getSignatureParameters(resource, fileSystemMessage);
        ConnectorFuture future = Executor.submit(resource, null, parameters);
        ConnectorFutureListener futureListener = new LocalFileSystemServerConnectorFutureListener(fileSystemMessage);
        future.setConnectorFutureListener(futureListener);
    }

    private BValue[] getSignatureParameters(Resource resource, LocalFileSystemMessage fileSystemMessage) {
        BStruct request = ConnectorUtils.createStruct(resource, Constants.FILE_SYSTEM_PACKAGE_NAME,
                Constants.FILE_SYSTEM_EVENT);
        request.setStringField(0, fileSystemMessage.getFileName());
        request.setStringField(1, fileSystemMessage.getEvent());
        BValue[] bValues = new BValue[1];
        bValues[0] = request;
        return bValues;
    }
}
