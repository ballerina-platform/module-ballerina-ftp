package org.ballerinalang.net.ftp.server;

import org.ballerinalang.connector.api.Service;
import org.wso2.carbon.transport.remotefilesystem.server.connector.contract.RemoteFileSystemServerConnector;

/**
 * To hold service and server connection information.
 */
public class ConnectorInfo {

    private Service service;
    private RemoteFileSystemServerConnector serverConnector;
    private boolean start;

    public ConnectorInfo(Service service, RemoteFileSystemServerConnector serverConnector) {
        this.service = service;
        this.serverConnector = serverConnector;
    }

    public boolean isStart() {
        return start;
    }

    public void setStart(boolean start) {
        this.start = start;
    }

    public RemoteFileSystemServerConnector getServerConnector() {
        return serverConnector;
    }

    public Service getService() {
        return service;
    }
}
