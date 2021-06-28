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

import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.PredefinedTypes;
import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.async.Callback;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.stdlib.ftp.transport.listener.RemoteFileSystemListener;
import org.ballerinalang.stdlib.ftp.transport.message.FileInfo;
import org.ballerinalang.stdlib.ftp.transport.message.RemoteFileSystemBaseMessage;
import org.ballerinalang.stdlib.ftp.transport.message.RemoteFileSystemEvent;
import org.ballerinalang.stdlib.ftp.util.FtpConstants;
import org.ballerinalang.stdlib.ftp.util.FtpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FTP File System connector listener for Ballerina.
 */
public class FtpListener implements RemoteFileSystemListener {

    private static final Logger log = LoggerFactory.getLogger(FtpListener.class);
    private final Runtime runtime;
    private final BObject service;

    FtpListener(Runtime runtime, BObject service) {

        this.runtime = runtime;
        this.service = service;
    }

    @Override
    public boolean onMessage(RemoteFileSystemBaseMessage remoteFileSystemBaseMessage) {

        if (remoteFileSystemBaseMessage instanceof RemoteFileSystemEvent) {
            RemoteFileSystemEvent event = (RemoteFileSystemEvent) remoteFileSystemBaseMessage;
            BMap<BString, Object> parameters = getSignatureParameters(event);
            runtime.invokeMethodAsync(service, service.getType().getMethods()[0].getName(), null,
                    null, new Callback() {
                @Override
                public void notifySuccess(Object o) {}

                @Override
                public void notifyFailure(BError error) {
                    log.error("Error while invoking FTP onMessage method.");
                }
            }, parameters, true);
        }
        return true;
    }

    private BMap<BString, Object> getSignatureParameters(RemoteFileSystemEvent fileSystemEvent) {

        BMap<BString, Object> watchEventStruct = ValueCreator.createRecordValue(
                new Module(FtpConstants.FTP_ORG_NAME, FtpConstants.FTP_MODULE_NAME,
                        FtpUtil.getFtpPackage().getVersion()), FtpConstants.FTP_SERVER_EVENT);
        List<FileInfo> addedFileList = fileSystemEvent.getAddedFiles();
        List<String> deletedFileList = fileSystemEvent.getDeletedFiles();

        // For newly added files
        BArray addedFiles = ValueCreator.createArrayValue(TypeCreator.createArrayType(FtpUtil.getFileInfoType()));

        for (int i = 0; i < addedFileList.size(); i++) {
            FileInfo info = addedFileList.get(i);
            Map<String, Object> fileInfoParams = new HashMap<>();
            fileInfoParams.put("path", info.getPath());
            fileInfoParams.put("size", info.getFileSize());
            fileInfoParams.put("lastModifiedTimestamp", info.getLastModifiedTime());

            final BMap<BString, Object> fileInfo = ValueCreator.createRecordValue(
                    new Module(FtpConstants.FTP_ORG_NAME, FtpConstants.FTP_MODULE_NAME,
                            FtpUtil.getFtpPackage().getVersion()), FtpConstants.FTP_FILE_INFO, fileInfoParams);
            addedFiles.add(i, fileInfo);
        }

        // For deleted files
        BArray deletedFiles = ValueCreator.createArrayValue(TypeCreator.createArrayType(PredefinedTypes.TYPE_STRING));
        for (int i = 0; i < deletedFileList.size(); i++) {
            deletedFiles.add(i, deletedFileList.get(i));
        }
        // WatchEvent
        return ValueCreator.createRecordValue(watchEventStruct, addedFiles, deletedFiles);
    }

    @Override
    public void onError(Throwable throwable) {
        log.error(throwable.getMessage(), throwable);
    }

    @Override
    public void done() {
        log.debug(FtpConstants.SUCCESSFULLY_FINISHED_THE_ACTION);
    }
}
