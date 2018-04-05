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

package org.ballerinalang.ftp.server;

import org.ballerinalang.connector.api.Executor;
import org.ballerinalang.connector.api.Resource;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.services.ErrorHandlerUtils;
import org.ballerinalang.util.codegen.StructInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.transport.remotefilesystem.listener.RemoteFileSystemListener;
import org.wso2.transport.remotefilesystem.message.RemoteFileSystemBaseMessage;
import org.wso2.transport.remotefilesystem.message.RemoteFileSystemEvent;

/**
 * FTP File System connector listener for Ballerina.
 */
public class FTPFileSystemListener implements RemoteFileSystemListener {

    private static final Logger log = LoggerFactory.getLogger(FTPFileSystemListener.class);
    private Resource resource;
    private StructInfo structInfo;

    public FTPFileSystemListener(Resource resource, StructInfo structInfo) {
        this.resource = resource;
        this.structInfo = structInfo;
    }

    @Override
    public boolean onMessage(RemoteFileSystemBaseMessage remoteFileSystemBaseMessage) {
        if (remoteFileSystemBaseMessage instanceof RemoteFileSystemEvent) {
            RemoteFileSystemEvent event = (RemoteFileSystemEvent) remoteFileSystemBaseMessage;
            BValue[] parameters = getSignatureParameters(event);
            Executor.submit(resource, new FTPCallableUnitCallback(), null, null, parameters);
        }
        return true;
    }

    private BValue[] getSignatureParameters(RemoteFileSystemEvent fileSystemEvent) {
        BStruct eventStruct = new BStruct(structInfo.getType());
        eventStruct.setStringField(0, fileSystemEvent.getUri());
        eventStruct.setIntField(0, fileSystemEvent.getFileSize());
        eventStruct.setIntField(1, fileSystemEvent.getLastModifiedTime());
        return new BValue[] { eventStruct };
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
