package org.ballerinalang.net.fs.server;

import org.ballerinalang.connector.api.Service;
import org.wso2.transport.localfilesystem.server.connector.contract.LocalFileSystemServerConnector;

/**
 * To hold service and server connection information.
 */
public class ConnectorInfo {

    private Service service;
    private LocalFileSystemServerConnector serverConnector;
    private boolean start;

    public ConnectorInfo(Service service, LocalFileSystemServerConnector serverConnector) {
        this.service = service;
        this.serverConnector = serverConnector;
    }

    public boolean isStart() {
        return start;
    }

    public void setStart(boolean start) {
        this.start = start;
    }

    public LocalFileSystemServerConnector getServerConnector() {
        return serverConnector;
    }

    public Service getService() {
        return service;
    }
}
