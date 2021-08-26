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

package io.ballerina.stdlib.ftp.transport.server.connector.contractimpl;

import io.ballerina.stdlib.ftp.exception.RemoteFileSystemConnectorException;
import io.ballerina.stdlib.ftp.server.FtpListener;
import io.ballerina.stdlib.ftp.transport.listener.RemoteFileSystemListener;
import io.ballerina.stdlib.ftp.transport.server.RemoteFileSystemConsumer;
import io.ballerina.stdlib.ftp.transport.server.connector.contract.RemoteFileSystemServerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of the {@link RemoteFileSystemServerConnector} interface.
 */
public class RemoteFileSystemServerConnectorImpl implements RemoteFileSystemServerConnector {

    private static final Logger log = LoggerFactory.getLogger(io.ballerina.stdlib.ftp.transport.server.connector
            .contractimpl.RemoteFileSystemServerConnectorImpl.class);

    private RemoteFileSystemConsumer consumer;
    private AtomicBoolean isPollOperationOccupied = new AtomicBoolean(false);

    public RemoteFileSystemServerConnectorImpl(Map<String, String> properties,
            RemoteFileSystemListener remoteFileSystemListener) throws RemoteFileSystemConnectorException {
        try {
            consumer = new RemoteFileSystemConsumer(properties, remoteFileSystemListener);
        } catch (RemoteFileSystemConnectorException e) {
            throw new RemoteFileSystemConnectorException(
                    "Failed to initialize File server connector.", e);
        }
    }

    public void poll() {
        if (isPollOperationOccupied.compareAndSet(false, true)) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Poll method invoked.");
                }
                consumer.consume();
            } catch (Exception e) {
                log.error("Error executing the polling cycle", e);
            } finally {
                isPollOperationOccupied.set(false);
            }
        } else {
            log.warn("A scheduled email polling job was skipped as the previous job was still processing.");
        }
    }

    public Object stop() {
        return consumer.close();
    }

    public FtpListener getFtpListener() {
        return consumer.getFtpListener();
    }
}
