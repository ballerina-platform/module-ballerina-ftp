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
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.ftp.util.FTPUtil;
import org.ballerinalang.model.values.BValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.transport.remotefilesystem.listener.RemoteFileSystemListener;
import org.wso2.transport.remotefilesystem.message.RemoteFileSystemBaseMessage;

/**
 * {@code AbstractFtpAction} is the base class for all FTP Connector Actions.
 */
abstract class AbstractFtpAction extends BlockingNativeCallableUnit {

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
            context.setReturnValues(FTPUtil.createError(context, throwable.getMessage()));
            log.error(throwable.getMessage(), throwable);
        }

        @Override
        public void done() {
        }
    }
}
