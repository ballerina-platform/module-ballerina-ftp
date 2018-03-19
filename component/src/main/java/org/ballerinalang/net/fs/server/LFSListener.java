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

import org.ballerinalang.connector.api.Executor;
import org.ballerinalang.connector.api.Resource;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.util.codegen.StructInfo;
import org.wso2.transport.localfilesystem.server.connector.contract.LocalFileSystemEvent;
import org.wso2.transport.localfilesystem.server.connector.contract.LocalFileSystemListener;

/**
 * File System connector listener for Ballerina.
 */
public class LFSListener implements LocalFileSystemListener {

    private Resource resource;
    private StructInfo structInfo;

    public LFSListener(Resource resource, StructInfo structInfo) {
        this.resource = resource;
        this.structInfo = structInfo;
    }

    @Override
    public void onMessage(LocalFileSystemEvent fileSystemEvent) {
        BValue[] parameters = getSignatureParameters(fileSystemEvent);
        Executor.submit(resource, new FSCallableUnitCallback(), null, parameters);
    }

    private BValue[] getSignatureParameters(LocalFileSystemEvent fileSystemEvent) {
        BStruct eventStruct = new BStruct(this.structInfo.getType());
        eventStruct.setStringField(0, fileSystemEvent.getFileName());
        eventStruct.setStringField(1, fileSystemEvent.getEvent());
        return new BValue[] { eventStruct };
    }
}
