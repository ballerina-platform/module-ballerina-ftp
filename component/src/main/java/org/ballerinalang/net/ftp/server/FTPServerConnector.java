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
import org.wso2.transport.remotefilesystem.RemoteFileSystemConnectorFactory;
import org.wso2.transport.remotefilesystem.exception.RemoteFileSystemConnectorException;
import org.wso2.transport.remotefilesystem.impl.RemoteFileSystemConnectorFactoryImpl;
import org.wso2.transport.remotefilesystem.server.connector.contract.RemoteFileSystemServerConnector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code FTPServerConnector} This is the file system implementation for the
 * {@code BallerinaServerConnector} API.
 *
 * @since 0.94
 */
@JavaSPIService("org.ballerinalang.connector.api.BallerinaServerConnector")
public class FTPServerConnector implements BallerinaServerConnector {

    private static final Logger log = LoggerFactory.getLogger(FTPServerConnector.class);

    private final Map<String, ConnectorInfo> connectorMap = new HashMap<>();

    @Override
    public List<String> getProtocolPackages() {
        List<String> protocolPackageList = new ArrayList<>(1);
        protocolPackageList.add(Constants.FTP_PACKAGE_NAME);
        return protocolPackageList;
    }

    @Override
    public void serviceRegistered(Service service) throws BallerinaConnectorException {
        List<Annotation> annotationList = service.getAnnotationList(Constants.FTP_PACKAGE_NAME,
                Constants.ANNOTATION_CONFIG);
        String serviceName = getServiceName(service);
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
        Resource[] resources = service.getResources();
        if (resources == null) {
            throw new BallerinaConnectorException("No resources define for given service: " + service.getName());
        } else if (resources.length >= 2) {
            throw new BallerinaConnectorException("More than one resource define for given service: "
                    + service.getName());
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
        RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        try {
            RemoteFileSystemServerConnector serverConnector =
                    fileSystemConnectorFactory.createServerConnector(serviceName, paramMap,
                    new BallerinaFTPFileSystemListener(service));
            connectorMap.put(serviceName, new ConnectorInfo(service, serverConnector));
        } catch (RemoteFileSystemConnectorException e) {
            throw new BallerinaConnectorException("Unable to initialize FTPServerConnector instance", e);
        }
        if (log.isDebugEnabled()) {
            log.debug("FTPServerConnector registered successfully: " + serviceName);
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
        } catch (RemoteFileSystemConnectorException e) {
            throw new BallerinaConnectorException("Unable to start FTPServerConnector task", e);
        }

    }

    private Map<String, String> getServerConnectorParamMap(Annotation info) {
        Map<String, String> params = new HashMap<>(19);
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
        addAnnotationAttributeValue(info, Constants.ANNOTATION_SFTP_IDENTITIES, params);
        addAnnotationAttributeValue(info, Constants.ANNOTATION_SFTP_IDENTITY_PASS_PHRASE, params);
        addAnnotationAttributeValue(info, Constants.ANNOTATION_SFTP_USER_DIR_IS_ROOT, params);
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
            throw new BallerinaConnectorException("Single ftp:FTPServerEvent parameter allow in resource signature.");
        }
        ParamDetail reqParamDetail = paramDetails.get(0);
        if (reqParamDetail == null) {
            throw new BallerinaConnectorException("FTPServerEvent parameter cannot be null");
        }
        if (reqParamDetail.getVarType().getPackagePath() == null
                || !reqParamDetail.getVarType().getPackagePath().equals(Constants.FTP_PACKAGE_NAME)
                || !reqParamDetail.getVarType().getName().equals(Constants.FTP_SERVER_EVENT)) {
            throw new BallerinaConnectorException("Parameter should be of type - "
                    + Constants.FTP_PACKAGE_NAME + ":" + Constants.FTP_SERVER_EVENT);
        }
    }

    private static String getServiceName(Service service) {
        return service.getPackage() != null ? (service.getPackage() + "_" + service.getName()) : service.getName();
    }
}
