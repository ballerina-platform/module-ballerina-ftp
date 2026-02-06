/*
 * Copyright (c) 2026 WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.ftp.transport.server.connector.contractimpl;

import io.ballerina.runtime.api.values.BError;
import io.ballerina.stdlib.ftp.server.FtpListener;
import io.ballerina.stdlib.ftp.transport.listener.RemoteFileSystemListener;
import io.ballerina.stdlib.ftp.transport.message.RemoteFileSystemBaseMessage;
import io.ballerina.stdlib.ftp.transport.message.RemoteFileSystemEvent;

/**
 * A listener wrapper that adds path context to events before forwarding to FtpListener.
 * This enables path-based routing to the correct service when multiple paths are monitored.
 */
public class PathSpecificListener implements RemoteFileSystemListener {

    private final FtpListener ftpListener;
    private final String monitoredPath;

    public PathSpecificListener(FtpListener ftpListener, String monitoredPath) {
        this.ftpListener = ftpListener;
        this.monitoredPath = monitoredPath;
    }

    @Override
    public boolean onMessage(RemoteFileSystemBaseMessage message) {
        if (message instanceof RemoteFileSystemEvent) {
            RemoteFileSystemEvent event = (RemoteFileSystemEvent) message;
            // Set the source path on the event for routing
            event.setSourcePath(monitoredPath);
        }
        return ftpListener.onMessage(message);
    }

    @Override
    public void onError(Throwable throwable) {
        ftpListener.onError(throwable);
    }

    @Override
    public BError done() {
        // Don't call done() here - let the MultiPathServerConnector manage lifecycle
        return null;
    }

    /**
     * Gets the monitored path for this listener.
     *
     * @return The path being monitored
     */
    public String getMonitoredPath() {
        return monitoredPath;
    }
}
