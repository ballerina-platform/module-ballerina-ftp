/*
 * Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.ftp.server.endpoint;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.ftp.util.FTPUtil;
import org.ballerinalang.ftp.util.FtpConstants;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.transport.remotefilesystem.exception.RemoteFileSystemConnectorException;
import org.wso2.transport.remotefilesystem.server.connector.contract.RemoteFileSystemServerConnector;

import static org.ballerinalang.ftp.util.FtpConstants.FTP_PACKAGE_NAME;
import static org.ballerinalang.model.types.TypeKind.OBJECT;

/**
 * Native method for poll.
 */

@BallerinaFunction(
        orgName = "wso2",
        packageName = "ftp:0.0.0",
        functionName = "poll",
        args = {@Argument(name = "config", type = OBJECT, structType = "ListenerConfig",
                          structPackage = FTP_PACKAGE_NAME)},
        receiver = @Receiver(type = OBJECT, structType = "Listener", structPackage = FTP_PACKAGE_NAME),
        isPublic = true
)
public class Poll extends BlockingNativeCallableUnit {

    private static final Logger log = LoggerFactory.getLogger(Poll.class);

    @Override
    public void execute(Context context) {
        final BMap<String, BValue> config = (BMap<String, BValue>) context.getRefArgument(0);
        RemoteFileSystemServerConnector connector = (RemoteFileSystemServerConnector) config
                .getNativeData(FtpConstants.FTP_SERVER_CONNECTOR);
        try {
            connector.poll();
        } catch (RemoteFileSystemConnectorException e) {
            context.setReturnValues(FTPUtil.createError(context, e.getMessage()));
            log.error(e.getMessage(), e);
            return;
        }
        context.setReturnValues();
    }
}
