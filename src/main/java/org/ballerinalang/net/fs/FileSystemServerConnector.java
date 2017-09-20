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

package org.ballerinalang.net.fs;

import org.ballerinalang.connector.api.AnnAttrValue;
import org.ballerinalang.connector.api.Annotation;
import org.ballerinalang.connector.api.BallerinaConnectorException;
import org.ballerinalang.connector.api.BallerinaServerConnector;
import org.ballerinalang.connector.api.ParamDetail;
import org.ballerinalang.connector.api.Resource;
import org.ballerinalang.connector.api.Service;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.wso2.carbon.messaging.exceptions.ServerConnectorException;
import org.wso2.carbon.transport.filesystem.connector.server.contract.FileSystemConnectorFactory;
import org.wso2.carbon.transport.filesystem.connector.server.contractimpl.FileSystemConnectorFactoryImpl;
import org.wso2.carbon.transport.filesystem.connector.server.exception.FileSystemServerConnectorException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code FileSystemServerConnector} This is the file system implementation for the
 * {@code BallerinaServerConnector} API.
 *
 * @since 0.94
 */
public class FileSystemServerConnector implements BallerinaServerConnector {

    private org.wso2.carbon.transport.filesystem.connector.server.contract.FileSystemServerConnector serverConnector;

    @Override
    public String getProtocolPackage() {
        return Constants.FILE_SYSTEM_PACKAGE_NAME;
    }

    @Override
    public void serviceRegistered(Service service) throws BallerinaConnectorException {
        Annotation configInfo = service.getAnnotation(Constants.FILE_SYSTEM_PACKAGE_NAME, Constants.ANNOTATION_CONFIG);
        if (configInfo != null) {
            Map<String, String> paramMap = getServerConnectorParamMap(configInfo);
            String dir = paramMap.get(Constants.ANNOTATION_DIR_URI);
            if (dir == null) {
                throw new BallerinaException("Cannot create file system server without dirURI");
            }
            // Validate resource signature
            for (Resource resource : service.getResources()) {
                validateResourceSignature(resource);
            }
            String serviceName = FileSystemServiceRegistry.getServiceKey(service);
            FileSystemConnectorFactory fileSystemConnectorFactory = new FileSystemConnectorFactoryImpl();
            try {
                serverConnector = fileSystemConnectorFactory.createServerConnector(serviceName, paramMap,
                        new BallerinaFileSystemListener());
                FileSystemServiceRegistry.getInstance().addService(service);
            } catch (FileSystemServerConnectorException e) {
                throw new BallerinaConnectorException("Unable to initialize FileSystemServerConnector instance", e);
            }
        }
    }

    @Override
    public void serviceUnregistered(Service service) throws BallerinaConnectorException {
        String serviceKeyName = FileSystemServiceRegistry.getServiceKey(service);
        if (FileSystemServiceRegistry.getInstance().getService(serviceKeyName) != null) {
            FileSystemServiceRegistry.getInstance().removeService(serviceKeyName);
            try {
                if (serverConnector != null) {
                    serverConnector.stop();
                }
            } catch (ServerConnectorException e) {
                throw new BallerinaException("Could not stop file server connector for " +
                        "service: " + serviceKeyName, e);
            }
        }
    }

    @Override
    public void deploymentComplete() throws BallerinaConnectorException {
        if (serverConnector != null) {
            try {
                serverConnector.start();
            } catch (FileSystemServerConnectorException e) {
                throw new BallerinaConnectorException("Unable to start FileSystemServerConnector task", e);
            }
        }
    }


    private Map<String, String> getServerConnectorParamMap(Annotation info) {
        Map<String, String> params = new HashMap<>();
        addAnnotationAttributeValue(info, Constants.ANNOTATION_DIR_URI, params);
        addAnnotationAttributeValue(info, Constants.ANNOTATION_FILE_PATTERN, params);
        addAnnotationAttributeValue(info, Constants.ANNOTATION_POLLING_INTERVAL, params);
        addAnnotationAttributeValue(info, Constants.ANNOTATION_CRON_EXPRESSION, params);
        addAnnotationAttributeValue(info, Constants.ANNOTATION_ACK_TIMEOUT, params);
        addAnnotationAttributeValue(info, Constants.ANNOTATION_FILE_COUNT, params);
        addAnnotationAttributeValue(info, Constants.ANNOTATION_SORT_ATTRIBUTE, params);
        addAnnotationAttributeValue(info, Constants.ANNOTATION_SORT_ASCENDING, params);
        addAnnotationAttributeValue(info, Constants.ANNOTATION_ACTION_AFTER_PROCESS, params);
        addAnnotationAttributeValue(info, Constants.ANNOTATION_ACTION_AFTER_FAILURE, params);
        addAnnotationAttributeValue(info, Constants.ANNOTATION_MOVE_AFTER_PROCESS, params);
        addAnnotationAttributeValue(info, Constants.ANNOTATION_MOVE_AFTER_FAILURE, params);
        addAnnotationAttributeValue(info, Constants.ANNOTATION_MOVE_TIMESTAMP_FORMAT, params);
        addAnnotationAttributeValue(info, Constants.ANNOTATION_CREATE_DIR, params);
        addAnnotationAttributeValue(info, Constants.ANNOTATION_PARALLEL, params);
        addAnnotationAttributeValue(info, Constants.ANNOTATION_THREAD_POOL_SIZE, params);
        return params;
    }

    private void addAnnotationAttributeValue(Annotation info, String attribute, Map<String, String> params) {
        AnnAttrValue attributeValue = info.getAnnAttrValue(attribute);
        if (attributeValue != null && !attributeValue.getStringValue().trim().isEmpty()) {
            params.put(attribute, attributeValue.getStringValue());
        }
    }

    private void validateResourceSignature(Resource resource) {
        List<ParamDetail> paramDetails = resource.getParamDetails();
        if (paramDetails.size() != 1) {
            throw new BallerinaConnectorException("Resource signature parameter count should be more than two");
        }
        ParamDetail reqParamDetail = paramDetails.get(0);
        if (reqParamDetail == null) {
            throw new BallerinaConnectorException("FileEvent parameter cannot be null");
        }
        if (reqParamDetail.getVarType().getPackagePath() == null
                || !reqParamDetail.getVarType().getPackagePath().equals(Constants.FILE_SYSTEM_PACKAGE_NAME)
                || !reqParamDetail.getVarType().getName().equals(Constants.FILE_SYSTEM_EVENT)) {
            throw new BallerinaConnectorException("Parameter should be of type - "
                    + Constants.FILE_SYSTEM_PACKAGE_NAME + ":" + Constants.FILE_SYSTEM_EVENT);
        }
    }
}
