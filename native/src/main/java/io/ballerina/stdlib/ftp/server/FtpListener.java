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

package io.ballerina.stdlib.ftp.server;

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
import io.ballerina.stdlib.ftp.exception.RemoteFileSystemConnectorException;
import io.ballerina.stdlib.ftp.transport.listener.RemoteFileSystemListener;
import io.ballerina.stdlib.ftp.transport.message.FileInfo;
import io.ballerina.stdlib.ftp.transport.message.RemoteFileSystemBaseMessage;
import io.ballerina.stdlib.ftp.transport.message.RemoteFileSystemEvent;
import io.ballerina.stdlib.ftp.transport.server.connector.contract.RemoteFileSystemServerConnector;
import io.ballerina.stdlib.ftp.util.FtpConstants;
import io.ballerina.stdlib.ftp.util.FtpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.ballerina.stdlib.ftp.server.FtpListenerHelper.findRootCause;
import static io.ballerina.stdlib.ftp.util.FtpUtil.ErrorType.Error;

/**
 * FTP File System connector listener for Ballerina.
 */
public class FtpListener implements RemoteFileSystemListener {

    private static final Logger log = LoggerFactory.getLogger(FtpListener.class);
    private final Runtime runtime;
    private Map<String, BObject> registeredServices = new HashMap<>();

    FtpListener(Runtime runtime) {
        this.runtime = runtime;
    }

    @Override
    public boolean onMessage(RemoteFileSystemBaseMessage remoteFileSystemBaseMessage) {
        if (remoteFileSystemBaseMessage instanceof RemoteFileSystemEvent) {
            RemoteFileSystemEvent event = (RemoteFileSystemEvent) remoteFileSystemBaseMessage;
            BMap<BString, Object> parameters = getSignatureParameters(event);
            if (runtime != null) {
                Set<Map.Entry<String, BObject>> serviceEntries = registeredServices.entrySet();
                for (Map.Entry<String, BObject> serviceEntry : serviceEntries) {
                    BObject service = serviceEntry.getValue();
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
            } else {
                log.error("Runtime should not be null.");
            }
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
    public BError done() {
        Set<Map.Entry<String, BObject>> serviceEntries = registeredServices.entrySet();
        for (Map.Entry<String, BObject> serviceEntry : serviceEntries) {
            BObject service = serviceEntry.getValue();
            try {
                Object serverConnectorObject = service.getNativeData(FtpConstants.FTP_SERVER_CONNECTOR);
                if (serverConnectorObject instanceof RemoteFileSystemServerConnector) {
                    RemoteFileSystemServerConnector serverConnector
                            = (RemoteFileSystemServerConnector) serverConnectorObject;
                    Object stopError = serverConnector.stop();
                    if (stopError instanceof BError) {
                        return (BError) stopError;
                    }
                }
            } catch (RemoteFileSystemConnectorException e) {
                Throwable rootCause = findRootCause(e);
                String detail = (rootCause != null) ? rootCause.getMessage() : null;
                return FtpUtil.createError(e.getMessage(), detail, Error.errorType());
            } finally {
                service.addNativeData(FtpConstants.FTP_SERVER_CONNECTOR, null);
            }
        }
        log.debug(FtpConstants.SUCCESSFULLY_FINISHED_THE_ACTION);
        return null;
    }

    protected void addService(BObject service) {
        if (service != null && service.getType() != null && service.getType().getName() != null) {
            registeredServices.put(service.getType().getName(), service);
        }
    }
}
