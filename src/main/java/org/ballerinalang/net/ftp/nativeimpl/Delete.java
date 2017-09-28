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

package org.ballerinalang.net.ftp.nativeimpl;

import org.ballerinalang.bre.Context;
import org.ballerinalang.connector.api.ConnectorFuture;
import org.ballerinalang.model.types.TypeEnum;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.nativeimpl.actions.ClientConnectorFuture;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.Attribute;
import org.ballerinalang.natives.annotations.BallerinaAction;
import org.ballerinalang.natives.annotations.BallerinaAnnotation;
import org.ballerinalang.natives.connectors.BallerinaConnectorManager;
import org.ballerinalang.net.ftp.nativeimpl.util.FileConstants;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.wso2.carbon.messaging.exceptions.ClientConnectorException;

import java.util.HashMap;
import java.util.Map;

/**
 * Delete a file or a folder
 */
@BallerinaAction(
        packageName = "ballerina.net.ftp",
        actionName = "delete",
        connectorName = FileConstants.CONNECTOR_NAME,
        args = { @Argument(name = "ftpClientConnector", type = TypeEnum.CONNECTOR),
                 @Argument(name = "file", type = TypeEnum.STRUCT, structType = "File",
                         structPackage = "ballerina.lang.files") })
@BallerinaAnnotation(annotationName = "Description", attributes = { @Attribute(name = "value",
        value = "Delete a file or a folder") })
@BallerinaAnnotation(annotationName = "Param", attributes = { @Attribute(name = "connector",
        value = "ftp client connector") })
@BallerinaAnnotation(annotationName = "Param", attributes = { @Attribute(name = "file",
        value = "File struct containing path information") })
public class Delete extends AbstractFtpAction {
    @Override
    public ConnectorFuture execute(Context context) {

        // Extracting Argument values
        BStruct file = (BStruct) getRefArgument(context, 1);
        if (!validateProtocol(file.getStringField(0))) {
            throw new BallerinaException("Only FTP, SFTP and FTPS protocols are supported by this connector");
        }
        //Create property map to send to transport.
        Map<String, String> propertyMap = new HashMap<>();
        String pathString = file.getStringField(0);
        propertyMap.put(FileConstants.PROPERTY_URI, pathString);
        propertyMap.put(FileConstants.PROPERTY_ACTION, FileConstants.ACTION_DELETE);
        try {
            //Getting the sender instance and sending the message.
            BallerinaConnectorManager.getInstance().getClientConnector(FileConstants.FTP_CONNECTOR_NAME)
                                     .send(null, null, propertyMap);
        } catch (ClientConnectorException e) {
            throw new BallerinaException(e.getMessage(), e, context);
        }
        ClientConnectorFuture future = new ClientConnectorFuture();
        future.notifySuccess();
        return future;
    }
}
