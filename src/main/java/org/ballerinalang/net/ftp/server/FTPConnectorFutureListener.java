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

package org.ballerinalang.net.ftp.server;

import org.ballerinalang.connector.api.BallerinaConnectorException;
import org.ballerinalang.connector.api.ConnectorFutureListener;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.services.ErrorHandlerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.transport.remotefilesystem.server.connector.contract.RemoteFileSystemMessage;

/**
 * {@code FTPConnectorFutureListener} is the responsible for acting on notifications
 * received from Ballerina side.
 */
public class FTPConnectorFutureListener implements ConnectorFutureListener {

    private static final Logger log = LoggerFactory.getLogger(FTPConnectorFutureListener.class);
    private RemoteFileSystemMessage fileSystemMessage;

    public FTPConnectorFutureListener(RemoteFileSystemMessage fileSystemMessage) {
        this.fileSystemMessage = fileSystemMessage;
    }

    @Override
    public void notifySuccess() {
        if (log.isDebugEnabled()) {
            Object serviceNameProperty = fileSystemMessage.getProperty(Constants.TRANSPORT_PROPERTY_SERVICE_NAME);
            String serviceName = (serviceNameProperty != null) ? serviceNameProperty.toString() : null;
            log.debug("Received success notify for FileSystemConnector service: " + serviceName);
        }
    }

    @Override
    public void notifyReply(BValue response) {
        if (log.isDebugEnabled() && response != null) {
            Object serviceNameProperty = fileSystemMessage.getProperty(Constants.TRANSPORT_PROPERTY_SERVICE_NAME);
            String serviceName = (serviceNameProperty != null) ? serviceNameProperty.toString() : null;
            log.debug("Received reply for FileSystemConnector service: " + serviceName + "; " + response.stringValue());
        }
    }

    @Override
    public void notifyFailure(BallerinaConnectorException ex) {
        Object serviceNameProperty = fileSystemMessage.getProperty(Constants.TRANSPORT_PROPERTY_SERVICE_NAME);
        String serviceName = (serviceNameProperty != null) ? serviceNameProperty.toString() : null;
        log.error("Error occurred for FileSystemConnector service: " + serviceName, ex);
        ErrorHandlerUtils.printError(ex);
    }
}
