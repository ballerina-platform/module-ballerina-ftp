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

package org.ballerinalang.stdlib.ftp.server;

import org.ballerinalang.jvm.api.BRuntime;
import org.ballerinalang.jvm.api.BValueCreator;
import org.ballerinalang.jvm.api.connector.CallableUnitCallback;
import org.ballerinalang.jvm.api.values.BArray;
import org.ballerinalang.jvm.api.values.BError;
import org.ballerinalang.jvm.api.values.BMap;
import org.ballerinalang.jvm.api.values.BObject;
import org.ballerinalang.jvm.api.values.BString;
import org.ballerinalang.jvm.types.BArrayType;
import org.ballerinalang.jvm.types.BPackage;
import org.ballerinalang.jvm.types.BTypes;
import org.ballerinalang.stdlib.ftp.util.FTPConstants;
import org.ballerinalang.stdlib.ftp.util.FTPUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.transport.remotefilesystem.listener.RemoteFileSystemListener;
import org.wso2.transport.remotefilesystem.message.FileInfo;
import org.wso2.transport.remotefilesystem.message.RemoteFileSystemBaseMessage;
import org.wso2.transport.remotefilesystem.message.RemoteFileSystemEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FTP File System connector listener for Ballerina.
 */
public class FTPListener implements RemoteFileSystemListener {

    private static final Logger log = LoggerFactory.getLogger(FTPListener.class);
    private final BRuntime runtime;
    private final BObject service;

    FTPListener(BRuntime runtime, BObject service) {

        this.runtime = runtime;
        this.service = service;
    }

    @Override
    public boolean onMessage(RemoteFileSystemBaseMessage remoteFileSystemBaseMessage) {

        if (remoteFileSystemBaseMessage instanceof RemoteFileSystemEvent) {
            RemoteFileSystemEvent event = (RemoteFileSystemEvent) remoteFileSystemBaseMessage;
            BMap<BString, Object> parameters = getSignatureParameters(event);
            runtime.invokeMethodAsync(service, service.getType().getAttachedFunctions()[0].getName(), null,
                    null, new CallableUnitCallback() {
                @Override
                public void notifySuccess() {}

                @Override
                public void notifyFailure(BError error) {
                    log.error("Error while invoking FTP onMessage method.");
                }
            }, parameters, true);
        }
        return true;
    }

    private BMap<BString, Object> getSignatureParameters(RemoteFileSystemEvent fileSystemEvent) {

        BMap<BString, Object> watchEventStruct = BValueCreator.createRecordValue(
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

            final BMap<BString, Object> fileInfo = BValueCreator.createRecordValue(
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
        return BValueCreator.createRecordValue(watchEventStruct, addedFiles, deletedFiles);
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
