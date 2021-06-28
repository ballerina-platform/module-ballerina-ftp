/*
 * Copyright (c) 2017 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.ballerinalang.stdlib.ftp.transport.server.connector.contractimpl;

import org.ballerinalang.stdlib.ftp.transport.exception.RemoteFileSystemConnectorException;
import org.ballerinalang.stdlib.ftp.transport.listener.RemoteFileSystemListener;
import org.ballerinalang.stdlib.ftp.transport.server.RemoteFileSystemConsumer;
import org.ballerinalang.stdlib.ftp.transport.server.connector.contract.RemoteFileSystemServerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Implementation of the {@link RemoteFileSystemServerConnector} interface.
 */
public class RemoteFileSystemServerConnectorImpl implements RemoteFileSystemServerConnector {

    private static final Logger log = LoggerFactory.getLogger(org.ballerinalang.stdlib.ftp.transport.server.connector
            .contractimpl.RemoteFileSystemServerConnectorImpl.class);

    private RemoteFileSystemConsumer consumer;
    private String id;

    public RemoteFileSystemServerConnectorImpl(String id, Map<String, String> properties,
            RemoteFileSystemListener remoteFileSystemListener) throws RemoteFileSystemConnectorException {
        this.id = id;
        try {
            consumer = new RemoteFileSystemConsumer(id, properties, remoteFileSystemListener);
        } catch (RemoteFileSystemConnectorException e) {
            throw new RemoteFileSystemConnectorException(
                    "Failed to initialize File server connector " + "for Service: " + id, e);
        }
    }

    public void poll() {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Poll method invoke for " + id);
            }
            consumer.consume();
        } catch (RemoteFileSystemConnectorException e) {
            log.error("Error executing the polling cycle of RemoteFileSystemServer for service: " + id, e);
        }
    }
}
