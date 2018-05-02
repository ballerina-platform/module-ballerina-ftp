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

import org.ballerinalang.bre.bvm.BLangVMStructs;
import org.ballerinalang.connector.api.Executor;
import org.ballerinalang.connector.api.Resource;
import org.ballerinalang.ftp.util.FtpConstants;
import org.ballerinalang.model.values.BRefValueArray;
import org.ballerinalang.model.values.BStringArray;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.services.ErrorHandlerUtils;
import org.ballerinalang.util.codegen.PackageInfo;
import org.ballerinalang.util.codegen.StructInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.transport.remotefilesystem.listener.RemoteFileSystemListener;
import org.wso2.transport.remotefilesystem.message.FileInfo;
import org.wso2.transport.remotefilesystem.message.RemoteFileSystemBaseMessage;
import org.wso2.transport.remotefilesystem.message.RemoteFileSystemEvent;

/**
 * FTP File System connector listener for Ballerina.
 */
public class FTPFileSystemListener implements RemoteFileSystemListener {

    private static final Logger log = LoggerFactory.getLogger(FTPFileSystemListener.class);
    private Resource resource;
    private PackageInfo packageInfo;

    public FTPFileSystemListener(Resource resource, PackageInfo packageInfo) {
        this.resource = resource;
        this.packageInfo = packageInfo;
    }

    @Override
    public boolean onMessage(RemoteFileSystemBaseMessage remoteFileSystemBaseMessage) {
        if (remoteFileSystemBaseMessage instanceof RemoteFileSystemEvent) {
            RemoteFileSystemEvent event = (RemoteFileSystemEvent) remoteFileSystemBaseMessage;
            BValue parameters = getSignatureParameters(event);
            Executor.submit(resource, new FTPCallableUnitCallback(), null, null, parameters);
        }
        return true;
    }

    private BValue getSignatureParameters(RemoteFileSystemEvent fileSystemEvent) {
        // For newly added files.
        final StructInfo fileInfoStructInfo = getFileInfoStructInfo(packageInfo);
        BRefValueArray fileInfoArray = new BRefValueArray(fileInfoStructInfo.getType());
        int i = 0;
        for (FileInfo info : fileSystemEvent.getAddedFiles()) {
            final BStruct fileInfoStruct = BLangVMStructs
                    .createBStruct(fileInfoStructInfo, info.getPath(), info.getFileSize(), info.getLastModifiedTime());
            fileInfoArray.add(i++, fileInfoStruct);
        }
        // For deleted files.
        BStringArray deletedFilesArray = new BStringArray();
        i = 0;
        for (String fileName : fileSystemEvent.getDeletedFiles()) {
            deletedFilesArray.add(i++, fileName);
        }
        // WatchEvent
        return BLangVMStructs
                .createBStruct(getWatchEventStructInfo(packageInfo), fileInfoArray, deletedFilesArray);
    }

    @Override
    public void onError(Throwable throwable) {
        ErrorHandlerUtils.printError(throwable);
    }

    @Override
    public void done() {
        log.debug("Successfully finished the action.");
    }

    private StructInfo getWatchEventStructInfo(PackageInfo packageInfo) {
        return packageInfo.getStructInfo(FtpConstants.FTP_SERVER_EVENT);
    }

    private StructInfo getFileInfoStructInfo(PackageInfo packageInfo) {
        return packageInfo.getStructInfo(FtpConstants.FTP_FILE_INFO);
    }
}
