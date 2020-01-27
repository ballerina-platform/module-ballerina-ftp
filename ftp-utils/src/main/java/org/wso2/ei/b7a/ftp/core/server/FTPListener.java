/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.ei.b7a.ftp.core.server;

import org.ballerinalang.jvm.BRuntime;
import org.ballerinalang.jvm.BallerinaValues;
import org.ballerinalang.jvm.types.BArrayType;
import org.ballerinalang.jvm.types.BPackage;
import org.ballerinalang.jvm.types.BTypes;
import org.ballerinalang.jvm.values.MapValue;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.jvm.values.api.BArray;
import org.ballerinalang.jvm.values.api.BValueCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.ei.b7a.ftp.core.util.FTPConstants;
import org.wso2.ei.b7a.ftp.core.util.FTPUtil;
import org.wso2.transport.remotefilesystem.listener.RemoteFileSystemListener;
import org.wso2.transport.remotefilesystem.message.FileInfo;
import org.wso2.transport.remotefilesystem.message.RemoteFileSystemBaseMessage;
import org.wso2.transport.remotefilesystem.message.RemoteFileSystemEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FTP File System connector listener for Ballerina
 */
public class FTPListener implements RemoteFileSystemListener {

    private static final Logger log = LoggerFactory.getLogger(FTPListener.class);
    private final BRuntime runtime;
    private final ObjectValue service;

    FTPListener(BRuntime runtime, ObjectValue service) {

        this.runtime = runtime;
        this.service = service;
    }

    @Override
    public boolean onMessage(RemoteFileSystemBaseMessage remoteFileSystemBaseMessage) {

        if (remoteFileSystemBaseMessage instanceof RemoteFileSystemEvent) {
            RemoteFileSystemEvent event = (RemoteFileSystemEvent) remoteFileSystemBaseMessage;
            MapValue<String, Object> parameters = getSignatureParameters(event);
            runtime.invokeMethodSync(service, service.getType().getAttachedFunctions()[0].getName(),
                    parameters, true);
        }
        return true;
    }

    private MapValue<String, Object> getSignatureParameters(RemoteFileSystemEvent fileSystemEvent) {

        MapValue<String, Object> watchEventStruct = BallerinaValues.createRecordValue(
                new BPackage(FTPConstants.FTP_ORG_NAME, FTPConstants.FTP_MODULE_NAME, FTPConstants.FTP_MODULE_VERSION),
                FTPConstants.FTP_SERVER_EVENT);
        List<FileInfo> addedFileList = fileSystemEvent.getAddedFiles();
        List<String> deletedFileList = fileSystemEvent.getDeletedFiles();

        // For newly added files
        BArray addedFiles = BValueCreator.createArrayValue(new BArrayType(FTPUtil.getFileInfoType()));

        for (int i = 0; i < addedFileList.size(); i++) {
            FileInfo info = addedFileList.get(i);
            Map<String, Object> fileInfoParams = new HashMap<>();
            fileInfoParams.put("path", info.getPath());
            fileInfoParams.put("size", info.getFileSize());
            fileInfoParams.put("lastModifiedTimestamp", info.getLastModifiedTime());

            final MapValue<String, Object> fileInfo = BallerinaValues.createRecordValue(
                    new BPackage(FTPConstants.FTP_ORG_NAME, FTPConstants.FTP_MODULE_NAME,
                    FTPConstants.FTP_MODULE_VERSION), FTPConstants.FTP_FILE_INFO, fileInfoParams);
            addedFiles.add(i, fileInfo);
        }

        // For deleted files
        BArray deletedFiles = BValueCreator.createArrayValue(new BArrayType(BTypes.typeString));
        for (int i = 0; i < deletedFileList.size(); i++) {
            deletedFiles.add(i, deletedFileList.get(i));
        }
        // WatchEvent
        return BallerinaValues
                .createRecord(watchEventStruct, addedFiles, deletedFiles);
    }

    @Override
    public void onError(Throwable throwable) {

        log.error(throwable.getMessage(), throwable);
    }

    @Override
    public void done() {

        log.debug(FTPConstants.SUCCESSFULLY_FINISHED_THE_ACTION);
    }
}
