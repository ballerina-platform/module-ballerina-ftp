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
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.util.codegen.PackageInfo;
import org.ballerinalang.util.codegen.StructInfo;
import org.wso2.transport.remotefilesystem.exception.RemoteFileSystemConnectorException;
import org.wso2.transport.remotefilesystem.server.connector.contract.RemoteFileSystemServerConnector;

import static org.ballerinalang.ftp.util.FtpConstants.BALLERINA_BUILTIN;
import static org.ballerinalang.ftp.util.FtpConstants.FTP_PACKAGE_NAME;

/**
 * Native method for poll.
 */

@BallerinaFunction(
        orgName = "wso2",
        packageName = "ftp",
        functionName = "poll",
        args = {@Argument(name = "config", type = TypeKind.STRUCT, structType = "ListenerEndpointConfig",
                          structPackage = FTP_PACKAGE_NAME)},
        isPublic = true
)
public class Poll extends BlockingNativeCallableUnit {
    @Override
    public void execute(Context context) {
        final BStruct config = (BStruct) context.getRefArgument(0);
        RemoteFileSystemServerConnector connector = (RemoteFileSystemServerConnector) config
                .getNativeData(FtpConstants.FTP_SERVER_CONNECTOR);
        try {
            connector.poll();
        } catch (RemoteFileSystemConnectorException e) {
            context.setReturnValues(getErrorStruct(context));
            return;
        }
        context.setReturnValues();
    }

    private BStruct getErrorStruct(Context context) {
        PackageInfo packageInfo = context.getProgramFile().getPackageInfo(BALLERINA_BUILTIN);
        final StructInfo structInfo = packageInfo.getStructInfo("error");
        return BLangVMStructs.createBStruct(structInfo);
    }
}
