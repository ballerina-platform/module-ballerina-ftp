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

package org.ballerinalang.net.fs.server;

import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.connector.api.AnnAttrValue;
import org.ballerinalang.connector.api.Annotation;
import org.ballerinalang.connector.api.BallerinaConnectorException;
import org.ballerinalang.connector.api.BallerinaServerConnector;
import org.ballerinalang.connector.api.ParamDetail;
import org.ballerinalang.connector.api.Resource;
import org.ballerinalang.connector.api.Service;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.transport.localfilesystem.server.connector.contract.LocalFileSystemConnectorFactory;
import org.wso2.transport.localfilesystem.server.connector.contractimpl.LocalFileSystemConnectorFactoryImpl;
import org.wso2.transport.localfilesystem.server.exception.LocalFileSystemServerConnectorException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code LocalFileSystemServerConnector} is the file system implementation for the
 * {@code BallerinaServerConnector} API.
 *
 * @since 0.94
 */
@JavaSPIService("org.ballerinalang.connector.api.BallerinaServerConnector")
public class LocalFileSystemServerConnector implements BallerinaServerConnector {

    private static final Logger log = LoggerFactory.getLogger(LocalFileSystemServerConnector.class);

    private final Map<String, ConnectorInfo> connectorMap = new HashMap<>();

    @Override
    public List<String> getProtocolPackages() {
        ArrayList<String> protocolPackageList = new ArrayList<>(1);
        protocolPackageList.add(Constants.FILE_SYSTEM_PACKAGE_NAME);
        return protocolPackageList;
    }

    @Override
    public void serviceRegistered(Service service) throws BallerinaConnectorException {
        List<Annotation> annotationList = service.getAnnotationList(Constants.FILE_SYSTEM_PACKAGE_NAME,
                Constants.ANNOTATION_CONFIG);
        String serviceName = getServiceKey(service);
        if (annotationList == null) {
            throw new BallerinaConnectorException("Unable to find the associated configuration " +
                    "annotation for given service: " + serviceName);
        }
        if (annotationList.size() > 1) {
            throw new BallerinaException(
                    "multiple service configuration annotations found in service: " + service.getName());
        }
        Annotation configInfo = annotationList.get(0);
        if (configInfo == null) {
            throw new BallerinaConnectorException("Unable to find the associated configuration " +
                    "annotation for given service: " + serviceName);
        }
        if (service.getResources() == null || service.getResources().length == 0) {
            throw new BallerinaConnectorException("No resources define for given service: " + serviceName);
        } else if (service.getResources().length >= 2) {
            throw new BallerinaConnectorException("More than one resource define for given service: "
                    + serviceName);
        }
        Map<String, String> paramMap = getServerConnectorParamMap(configInfo);
        String dir = paramMap.get(Constants.ANNOTATION_DIR_URI);
        if (dir == null) {
            throw new BallerinaException("Cannot create file system server without dirURI");
        }
        // Validate resource signature
        for (Resource resource : service.getResources()) {
            validateResourceSignature(resource);
        }
        LocalFileSystemConnectorFactory fileSystemConnectorFactory = new LocalFileSystemConnectorFactoryImpl();
        try {
            org.wso2.transport.localfilesystem.server.connector.contract.LocalFileSystemServerConnector
                    serverConnector = fileSystemConnectorFactory.createServerConnector(serviceName, paramMap,
                    new BallerinaLocalFileSystemListener(service));
            connectorMap.put(serviceName, new ConnectorInfo(service, serverConnector));
        } catch (LocalFileSystemServerConnectorException e) {
            throw new BallerinaConnectorException("Unable to initialize LocalFileSystemServerConnector instance",
                    e);
        }
        if (log.isDebugEnabled()) {
            log.debug("LocalFileSystemServerConnector registered successfully: " + serviceName);
        }
    }


    @Override
    public void deploymentComplete() throws BallerinaConnectorException {
        try {
            for (Map.Entry<String, ConnectorInfo> entry : connectorMap.entrySet()) {
                if (!entry.getValue().isStart()) {
                    entry.getValue().getServerConnector().start();
                    connectorMap.get(entry.getKey()).setStart(true);
                }
            }
        } catch (LocalFileSystemServerConnectorException e) {
            throw new BallerinaConnectorException("Unable to start LocalFileSystemServerConnector", e);
        }
    }

    private Map<String, String> getServerConnectorParamMap(Annotation info) {
        Map<String, String> params = new HashMap<>();
        copyAnnotationAttributeValue(info, Constants.ANNOTATION_DIR_URI, params);
        copyAnnotationAttributeValue(info, Constants.ANNOTATION_EVENTS, params);
        params.put(Constants.ANNOTATION_DIRECTORY_RECURSIVE,
                String.valueOf(info.getAnnAttrValue(Constants.ANNOTATION_DIRECTORY_RECURSIVE).getBooleanValue()));
        return params;
    }

    private void copyAnnotationAttributeValue(Annotation info, String attribute, Map<String, String> params) {
        AnnAttrValue attributeValue = info.getAnnAttrValue(attribute);
        if (attributeValue != null && !attributeValue.getStringValue().trim().isEmpty()) {
            params.put(attribute, attributeValue.getStringValue());
        }
    }

    private void validateResourceSignature(Resource resource) {
        List<ParamDetail> paramDetails = resource.getParamDetails();
        if (paramDetails.size() != 1) {
            throw new BallerinaConnectorException("Single fs:FileSystemEvent parameter allow in resource signature.");
        }
        ParamDetail reqParamDetail = paramDetails.get(0);
        if (reqParamDetail == null) {
            throw new BallerinaConnectorException("FileSystemEvent parameter cannot be null");
        }
        if (reqParamDetail.getVarType().getPackagePath() == null
                || !reqParamDetail.getVarType().getPackagePath().equals(Constants.FILE_SYSTEM_PACKAGE_NAME)
                || !reqParamDetail.getVarType().getName().equals(Constants.FILE_SYSTEM_EVENT)) {
            throw new BallerinaConnectorException("Parameter should be of type - "
                    + Constants.FILE_SYSTEM_PACKAGE_NAME + ":" + Constants.FILE_SYSTEM_EVENT);
        }
    }

    private String getServiceKey(Service service) {
        return service.getPackage() != null ? (service.getPackage() + "_" + service.getName()) : service.getName();
    }
}
