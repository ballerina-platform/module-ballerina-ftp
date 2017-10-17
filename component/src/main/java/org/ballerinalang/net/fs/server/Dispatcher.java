package org.ballerinalang.net.fs.server;

import org.ballerinalang.connector.api.BallerinaConnectorException;
import org.ballerinalang.connector.api.Resource;
import org.ballerinalang.connector.api.Service;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.transport.localfilesystem.server.connector.contract.LocalFileSystemEvent;

import java.util.Map;

/**
 * This class dispatch {@link LocalFileSystemEvent} to the relevant resource.
 */
public class Dispatcher {

    private static final Logger log = LoggerFactory.getLogger(Dispatcher.class);

    /**
     * Get associate resource attached with given {@link LocalFileSystemEvent}.
     *
     * @param fileSystemEvent {@link LocalFileSystemEvent} generated from transport.
     * @return Associated resource for this event.
     * @throws BallerinaException
     */
    static Resource findResource(LocalFileSystemEvent fileSystemEvent, Map<String, Service> serviceMap)
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

    private static Service findService(LocalFileSystemEvent fileSystemEvent, Map<String, Service> serviceMap) {
        Object serviceNameProperty = fileSystemEvent.getProperty(Constants.TRANSPORT_PROPERTY_SERVICE_NAME);
        String serviceName = (serviceNameProperty != null) ? serviceNameProperty.toString() : null;
        if (serviceName == null) {
            throw new BallerinaConnectorException(
                    "Could not find a service to dispatch. " + Constants.TRANSPORT_PROPERTY_SERVICE_NAME +
                            " property not set or empty.");
        }
        return serviceMap.get(serviceName);
    }
}
