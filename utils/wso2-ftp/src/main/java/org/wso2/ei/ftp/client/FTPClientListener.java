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

package org.wso2.ei.ftp.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.ei.ftp.util.FTPUtil;
import org.wso2.transport.remotefilesystem.listener.RemoteFileSystemListener;
import org.wso2.transport.remotefilesystem.message.RemoteFileSystemBaseMessage;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Contains implementation of RemoteFileSystemListener
 */
public class FTPClientListener implements RemoteFileSystemListener {

    private static final Logger log = LoggerFactory.getLogger(FTPClientListener.class);
    private CompletableFuture<Object> future;
    private Function<RemoteFileSystemBaseMessage, Boolean> function;

    FTPClientListener(CompletableFuture<Object> future,
                      Function<RemoteFileSystemBaseMessage, Boolean> function) {

        this.future = future;
        this.function = function;
    }

    @Override
    public boolean onMessage(RemoteFileSystemBaseMessage remoteFileSystemBaseMessage) {

        return function.apply(remoteFileSystemBaseMessage);
    }

    @Override
    public void onError(Throwable throwable) {

        log.error(throwable.getMessage(), throwable);
        future.complete(FTPUtil.createError(throwable.getMessage()));
    }

    @Override
    public void done() {

    }
}
