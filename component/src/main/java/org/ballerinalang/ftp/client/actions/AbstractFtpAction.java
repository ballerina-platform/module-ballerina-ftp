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
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BString;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.util.codegen.PackageInfo;
import org.ballerinalang.util.codegen.StructureTypeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.transport.remotefilesystem.listener.RemoteFileSystemListener;
import org.wso2.transport.remotefilesystem.message.RemoteFileSystemBaseMessage;

import static org.ballerinalang.ftp.util.FtpConstants.BALLERINA_BUILTIN;

/**
 * {@code AbstractFtpAction} is the base class for all FTP Connector Actions.
 */
abstract class AbstractFtpAction extends BlockingNativeCallableUnit {

    static BMap<String, BValue> getClientErrorStruct(Context context) {
        PackageInfo packageInfo = context.getProgramFile().getPackageInfo(BALLERINA_BUILTIN);
        final StructureTypeInfo structInfo = packageInfo.getStructInfo("error");
        return BLangVMStructs.createBStruct(structInfo);
    }

    /**
     * {@link RemoteFileSystemListener} implementation for receive notification from transport.
     */
    protected static class FTPClientConnectorListener implements RemoteFileSystemListener {

        private static final Logger log = LoggerFactory.getLogger(FTPClientConnectorListener.class);
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
            BMap<String, BValue> error = getClientErrorStruct(context);
            error.put("message", new BString(throwable.getMessage()));
            context.setReturnValues(error);
            log.error(throwable.getMessage(), throwable);
        }

        @Override
        public void done() {
        }
    }
}
