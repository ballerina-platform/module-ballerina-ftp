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
import org.ballerinalang.bre.bvm.BLangVMStructs;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.ftp.util.FtpConstants;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.util.codegen.PackageInfo;
import org.ballerinalang.util.codegen.StructureTypeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.transport.remotefilesystem.exception.RemoteFileSystemConnectorException;
import org.wso2.transport.remotefilesystem.server.connector.contract.RemoteFileSystemServerConnector;

import static org.ballerinalang.ftp.util.FtpConstants.BALLERINA_BUILTIN;
import static org.ballerinalang.ftp.util.FtpConstants.FTP_PACKAGE_NAME;

/**
 * Native method for poll.
 */

@BallerinaFunction(
        orgName = "wso2",
        packageName = "ftp:0.0.0",
        functionName = "poll",
        args = {@Argument(name = "config", type = TypeKind.OBJECT, structType = "ListenerEndpointConfig",
                          structPackage = FTP_PACKAGE_NAME)},
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
            context.setReturnValues(getErrorStruct(context));
            log.error(e.getMessage(), e);
            return;
        }
        context.setReturnValues();
    }

    private BMap<String, BValue> getErrorStruct(Context context) {
        PackageInfo packageInfo = context.getProgramFile().getPackageInfo(BALLERINA_BUILTIN);
        final StructureTypeInfo structInfo = packageInfo.getStructInfo("error");
        return BLangVMStructs.createBStruct(structInfo);
    }
}
