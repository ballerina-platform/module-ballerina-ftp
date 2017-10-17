package org.ballerinalang.net.ftp.server;

import org.ballerinalang.connector.api.BallerinaConnectorException;
import org.ballerinalang.connector.api.Resource;
import org.ballerinalang.connector.api.Service;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.transport.remotefilesystem.message.RemoteFileSystemEvent;

import java.util.Map;

/**
 * This class dispatch {@link RemoteFileSystemEvent} to the relevant resource.
 */
public class Dispatcher {

    private static final Logger log = LoggerFactory.getLogger(Dispatcher.class);

    /**
     * Get associate resource attached with given {@link RemoteFileSystemEvent}.
     *
     * @param fileSystemEvent {@link RemoteFileSystemEvent} generated from transport.
     * @return Associated resource for this event.
     * @throws BallerinaException
     */
    static Resource findResource(RemoteFileSystemEvent fileSystemEvent, Map<String, Service> serviceMap)
            throws BallerinaException {
        Service service = findService(fileSystemEvent, serviceMap);
        if (service == null) {
            throw new BallerinaConnectorException("No Service found to handle the service request.");
        }
        if (log.isDebugEnabled()) {
            log.debug("FileSystemMessage received for service: " + service.getName());
        }
        return service.getResources()[0];
    }

    private static Service findService(RemoteFileSystemEvent fileSystemEvent, Map<String, Service> serviceMap) {
        Object serviceNameProperty = fileSystemEvent.getProperty(Constants.TRANSPORT_PROPERTY_SERVICE_NAME);
        String serviceName = (serviceNameProperty != null) ? serviceNameProperty.toString() : null;
        if (serviceName == null) {
            throw new BallerinaException(
                    "Could not find a service to dispatch. " + Constants.TRANSPORT_PROPERTY_SERVICE_NAME +
                            " property not set.");
        }
        Service service = serviceMap.get(serviceName);
        if (service == null) {
            throw new BallerinaException("No service registered with the name: " + serviceName);
        }
        return service;
    }
}
