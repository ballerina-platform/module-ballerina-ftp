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
import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.async.Callback;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.utils.StringUtils;
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
import org.apache.commons.vfs2.FileSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.ballerina.stdlib.ftp.server.FtpListenerHelper.findRootCause;
import static io.ballerina.stdlib.ftp.util.FtpConstants.ON_FILE_CHANGE_REMOTE_FUNCTION;
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
                for (BObject service : registeredServices.values()) {
                    MethodType[] methodTypes = service.getType().getMethods();
                    for (MethodType remoteFunc : methodTypes) {
                        if (remoteFunc.getName().equals(ON_FILE_CHANGE_REMOTE_FUNCTION)) {
                            runtime.invokeMethodAsyncConcurrently(service, ON_FILE_CHANGE_REMOTE_FUNCTION, null,
                                    null, new Callback() {
                                        @Override
                                        public void notifySuccess(Object o) {
                                        }

                                        @Override
                                        public void notifyFailure(BError error) {
                                            log.error("Error while invoking FTP onMessage method.");
                                        }
                                    }, null, null, parameters, true);
                        }
                    }
                }
            } else {
                log.error("Runtime should not be null.");
            }
        }
        return true;
    }

    private BMap<BString, Object> getSignatureParameters(RemoteFileSystemEvent fileSystemEvent) {
        List<FileInfo> addedFileList = fileSystemEvent.getAddedFiles();
        List<String> deletedFileList = fileSystemEvent.getDeletedFiles();

        // For newly added files
        Object[] fileInfoRecord = new Object[addedFileList.size()];
        for (int i = 0; i < addedFileList.size(); i++) {
            FileInfo info = addedFileList.get(i);
            Map<String, Object> fileInfoParams = new HashMap<>();
            fileInfoParams.put("path", info.getPath());
            fileInfoParams.put("size", info.getFileSize());
            fileInfoParams.put("lastModifiedTimestamp", info.getLastModifiedTime());
            fileInfoParams.put("name", info.getFileName().getBaseName());
            fileInfoParams.put("isFolder", info.isFolder());
            fileInfoParams.put("isFile", info.isFile());
            try {
                fileInfoParams.put("pathDecoded", info.getFileName().getPathDecoded());
            } catch (FileSystemException e) {
                fileInfoParams.put("pathDecoded", info.getPath());
            }
            fileInfoParams.put("extension", info.getFileName().getExtension());
            fileInfoParams.put("publicURIString", info.getPublicURIString());
            fileInfoParams.put("fileType", info.getFileType().getName());
            fileInfoParams.put("isAttached", info.isAttached());
            fileInfoParams.put("isContentOpen", info.isContentOpen());
            fileInfoParams.put("isExecutable", info.isExecutable());
            fileInfoParams.put("isHidden", info.isHidden());
            fileInfoParams.put("isReadable", info.isReadable());
            fileInfoParams.put("isWritable", info.isWritable());
            fileInfoParams.put("depth", info.getFileName().getDepth());
            fileInfoParams.put("scheme", info.getFileName().getScheme());
            fileInfoParams.put("uri", info.getUrl().getPath());
            fileInfoParams.put("rootURI", info.getFileName().getRootURI());
            fileInfoParams.put("friendlyURI", info.getFileName().getFriendlyURI());

            final BMap<BString, Object> fileInfo = ValueCreator.createReadonlyRecordValue(
                    new Module(FtpConstants.FTP_ORG_NAME, FtpConstants.FTP_MODULE_NAME,
                            FtpUtil.getFtpPackage().getVersion()), FtpConstants.FTP_FILE_INFO, fileInfoParams);
            fileInfoRecord[i] = fileInfo;
        }
        BArray addedFiles = ValueCreator.createArrayValue(fileInfoRecord,
                TypeCreator.createArrayType(FtpUtil.getFileInfoType(), true));

        // For deleted files
        BString[] deletedFileBstringArray = new BString[deletedFileList.size()];
        for (int i = 0; i < deletedFileList.size(); i++) {
            deletedFileBstringArray[i] = StringUtils.fromString(deletedFileList.get(i));
        }
        BArray deletedFiles = ValueCreator.createReadonlyArrayValue(deletedFileBstringArray);

        // WatchEvent
        Map<String, Object> watchEventMap = new HashMap<>();
        watchEventMap.put("addedFiles", addedFiles);
        watchEventMap.put("deletedFiles", deletedFiles);
        return ValueCreator.createReadonlyRecordValue(
                new Module(FtpConstants.FTP_ORG_NAME, FtpConstants.FTP_MODULE_NAME,
                        FtpUtil.getFtpPackage().getVersion()), FtpConstants.FTP_SERVER_EVENT, watchEventMap);
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
