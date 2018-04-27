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
package org.ballerinalang.ftp.client.actions;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BLangVMStructs;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.util.codegen.PackageInfo;
import org.ballerinalang.util.codegen.StructInfo;
import org.wso2.transport.remotefilesystem.listener.RemoteFileSystemListener;
import org.wso2.transport.remotefilesystem.message.RemoteFileSystemBaseMessage;

import static org.ballerinalang.ftp.util.FtpConstants.BALLERINA_BUILTIN;

/**
 * {@code AbstractFtpAction} is the base class for all FTP Connector Actions.
 */
abstract class AbstractFtpAction extends BlockingNativeCallableUnit {

    static BStruct getClientErrorStruct(Context context) {
        PackageInfo packageInfo = context.getProgramFile().getPackageInfo(BALLERINA_BUILTIN);
        final StructInfo structInfo = packageInfo.getStructInfo("error");
        return BLangVMStructs.createBStruct(structInfo);
    }

    /**
     * {@link RemoteFileSystemListener} implementation for receive notification from transport.
     */
    protected static class FTPClientConnectorListener implements RemoteFileSystemListener {
        private Context context;

        FTPClientConnectorListener(Context context) {
            this.context = context;
        }

        public Context getContext() {
            return context;
        }

        @Override
        public boolean onMessage(RemoteFileSystemBaseMessage remoteFileSystemBaseMessage) {
            // This default implementation handle situation where no response return from the transport side.
            // If there are any response coming from transport then specifically need to handle from relevant action
            // class by overriding this method.
            context.setReturnValues((BValue) null);
            return true;
        }

        @Override
        public void onError(Throwable throwable) {
            BStruct error = getClientErrorStruct(context);
            error.setStringField(0, throwable.getMessage());
            context.setReturnValues(error);
        }

        @Override
        public void done() {
        }
    }
}
