/*
 * Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.ballerinalang.stdlib.ftp.client;

import io.ballerina.runtime.api.Future;
import org.ballerinalang.stdlib.ftp.transport.listener.RemoteFileSystemListener;
import org.ballerinalang.stdlib.ftp.transport.message.RemoteFileSystemBaseMessage;
import org.ballerinalang.stdlib.ftp.util.FtpConstants;
import org.ballerinalang.stdlib.ftp.util.FtpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.ballerinalang.stdlib.ftp.util.FtpConstants.FTP_ERROR;

/**
 * Contains implementation of RemoteFileSystemListener.
 */
public class FtpClientListener implements RemoteFileSystemListener {

    private static final Logger log = LoggerFactory.getLogger(FtpClientListener.class);
    private CompletableFuture<Object> future;
    private Function<RemoteFileSystemBaseMessage, Boolean> function;

    private Future balFuture;

    FtpClientListener(Future listenerFuture,
                      Function<RemoteFileSystemBaseMessage, Boolean> function) {

        this.balFuture = listenerFuture;
        this.function = function;
    }

    @Override
    public boolean onMessage(RemoteFileSystemBaseMessage remoteFileSystemBaseMessage) {

        return function.apply(remoteFileSystemBaseMessage);
    }

    @Override
    public void onError(Throwable throwable) {

        log.error(throwable.getMessage(), throwable);
        balFuture.complete(FtpUtil.createError(throwable.getMessage(), FTP_ERROR));
    }

    @Override
    public void done() {
        log.debug(FtpConstants.SUCCESSFULLY_FINISHED_THE_ACTION);
    }
}
