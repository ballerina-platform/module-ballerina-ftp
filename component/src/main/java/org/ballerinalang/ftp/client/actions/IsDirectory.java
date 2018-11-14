/*
 * Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinalang.ftp.client.actions;

import org.ballerinalang.bre.Context;
import org.ballerinalang.ftp.util.FtpConstants;
import org.ballerinalang.model.values.BBoolean;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BString;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.natives.annotations.ReturnType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.transport.remotefilesystem.RemoteFileSystemConnectorFactory;
import org.wso2.transport.remotefilesystem.client.connector.contract.FtpAction;
import org.wso2.transport.remotefilesystem.client.connector.contract.VFSClientConnector;
import org.wso2.transport.remotefilesystem.exception.RemoteFileSystemConnectorException;
import org.wso2.transport.remotefilesystem.impl.RemoteFileSystemConnectorFactoryImpl;
import org.wso2.transport.remotefilesystem.message.RemoteFileSystemBaseMessage;
import org.wso2.transport.remotefilesystem.message.RemoteFileSystemMessage;

import java.util.HashMap;
import java.util.Map;

import static org.ballerinalang.ftp.util.FtpConstants.BALLERINA_BUILTIN;
import static org.ballerinalang.ftp.util.FtpConstants.FTP_PACKAGE_NAME;
import static org.ballerinalang.model.types.TypeKind.BOOLEAN;
import static org.ballerinalang.model.types.TypeKind.CONNECTOR;
import static org.ballerinalang.model.types.TypeKind.OBJECT;
import static org.ballerinalang.model.types.TypeKind.STRING;

/**
* FTP isDirectory operation.
*/
@BallerinaFunction(
        orgName = "wso2",
        packageName = "ftp:0.0.0",
        functionName = "isDirectory",
        receiver = @Receiver(type = OBJECT, structType = "ClientActions", structPackage = FTP_PACKAGE_NAME),
        args = {@Argument(name = "ftpClientConnector", type = CONNECTOR),
                @Argument(name = "path", type = STRING)},
        returnType = {
                @ReturnType(type = BOOLEAN),
                @ReturnType(type = OBJECT, structType = "error", structPackage = BALLERINA_BUILTIN)
        }
)
public class IsDirectory extends AbstractFtpAction {

    private static final Logger log = LoggerFactory.getLogger(IsDirectory.class);

    @Override
    public void execute(Context context) {
        BMap<String, BValue> clientConnector = (BMap<String, BValue>) context.getRefArgument(0);
        String pathString = context.getStringArgument(0);

        String url = (String) clientConnector.getNativeData(FtpConstants.URL);
        Map<String, String> prop = (Map<String, String>) clientConnector.getNativeData(FtpConstants.PROPERTY_MAP);
        Map<String, String> propertyMap = new HashMap<>(prop);
        propertyMap.put(FtpConstants.PROPERTY_URI, url + pathString);

        FTPIsDirectoryListener connectorListener = new FTPIsDirectoryListener(context);
        RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        VFSClientConnector connector;
        try {
            connector = fileSystemConnectorFactory.createVFSClientConnector(propertyMap, connectorListener);
        } catch (RemoteFileSystemConnectorException e) {
            BMap<String, BValue> error = getClientErrorStruct(context);
            error.put("message", new BString(e.getMessage()));
            context.setReturnValues(error);
            log.error(e.getMessage(), e);
            return;
        }
        connector.send(null, FtpAction.ISDIR);
    }

    private static class FTPIsDirectoryListener extends FTPClientConnectorListener {

        private static final Logger log = LoggerFactory.getLogger(FTPIsDirectoryListener.class);
        FTPIsDirectoryListener(Context context) {
            super(context);
        }

        @Override
        public boolean onMessage(RemoteFileSystemBaseMessage remoteFileSystemBaseMessage) {
            if (remoteFileSystemBaseMessage instanceof RemoteFileSystemMessage) {
                RemoteFileSystemMessage message = (RemoteFileSystemMessage) remoteFileSystemBaseMessage;
                getContext().setReturnValues(new BBoolean(message.isDirectory()));
            }
            return true;
        }

        @Override
        public void onError(Throwable throwable) {
            BMap<String, BValue> error = getClientErrorStruct(getContext());
            error.put("message", new BString(throwable.getMessage()));
            getContext().setReturnValues(error);
            log.error(throwable.getMessage(), throwable);
        }
    }
}
