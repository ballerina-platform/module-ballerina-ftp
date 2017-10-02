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

import org.ballerinalang.connector.api.Service;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.transport.remotefilesystem.message.RemoteFileSystemMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * This services registry holds all the services of FileSystem.
 */
public class FTPServiceRegistry {

    private static final Logger logger = LoggerFactory.getLogger(FTPServiceRegistry.class);

    private final Map<String, Service> serviceMap = new HashMap<>();
    private static final FTPServiceRegistry INSTANCE = new FTPServiceRegistry();

    private FTPServiceRegistry() {
    }

    public static FTPServiceRegistry getInstance() {
        return INSTANCE;
    }

    public void addService(Service service) {
        String key = getServiceKey(service);
        serviceMap.put(key, service);
    }

    public Service getService(String key) {
        return serviceMap.get(key);
    }

    public void removeService(String key) {
        serviceMap.remove(key);
    }

    public static String getServiceKey(Service service) {
        return service.getPackage() != null ? (service.getPackage() + "_" + service.getName()) : service.getName();
    }

    public Service findService(RemoteFileSystemMessage fileSystemMessage) {
        Object serviceNameProperty = fileSystemMessage.getProperty(Constants.TRANSPORT_PROPERTY_SERVICE_NAME);
        String serviceName = (serviceNameProperty != null) ? serviceNameProperty.toString() : null;
        if (serviceName == null) {
            throw new BallerinaException(
                    "Could not find a service to dispatch. " + Constants.TRANSPORT_PROPERTY_SERVICE_NAME +
                            " property not set.");
        }
        Service service = INSTANCE.getService(serviceName);
        if (service == null) {
            throw new BallerinaException("No service registered with the name: " + serviceName);
        }
        return service;
    }
}
