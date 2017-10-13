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

import org.ballerinalang.connector.api.AbstractNativeAction;
import org.ballerinalang.connector.api.BallerinaConnectorException;
import org.ballerinalang.nativeimpl.actions.ClientConnectorFuture;
import org.wso2.carbon.transport.remotefilesystem.listener.RemoteFileSystemListener;
import org.wso2.carbon.transport.remotefilesystem.message.RemoteFileSystemBaseMessage;

/**
 * {@code AbstractFtpAction} is the base class for all FTP Connector Actions.
 */
abstract class AbstractFtpAction extends AbstractNativeAction {

    boolean validateProtocol(String url) {
        return url.startsWith("ftp://") || url.startsWith("sftp://") || url.startsWith("ftps://");
    }

    /**
     * {@link RemoteFileSystemListener} implementation for receive notification from transport.
     */
    protected static class FTPClientConnectorListener implements RemoteFileSystemListener {
        private ClientConnectorFuture ballerinaFuture;

        FTPClientConnectorListener(ClientConnectorFuture ballerinaFuture) {
            this.ballerinaFuture = ballerinaFuture;
        }

        @Override
        public void onMessage(RemoteFileSystemBaseMessage remoteFileSystemBaseMessage) {
            // This default implementation handle situation where no response return from the transport side.
            // If there are any response coming from transport then specifically need to handle from relevant action
            // class by overriding this method.
        }

        @Override
        public void onError(Throwable throwable) {
            BallerinaConnectorException ex = new BallerinaConnectorException(throwable);
            ballerinaFuture.notifyFailure(ex);
        }

        @Override
        public void done() {
            ballerinaFuture.notifySuccess();
        }

        ClientConnectorFuture getBallerinaFuture() {
            return ballerinaFuture;
        }
    }
}
