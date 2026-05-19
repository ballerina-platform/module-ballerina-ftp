/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.ftp.observability;

import io.ballerina.runtime.observability.ObserverContext;

/**
 * Extension of ObserverContext for the FTP connector.
 * Automatically attaches {@code context}, {@code remote_url}, and {@code protocol} tags.
 */
public class FtpObserverContext extends ObserverContext {

    private static final String TAG_CONTEXT = "context";
    private static final String TAG_REMOTE_URL = "remote_url";
    private static final String TAG_PROTOCOL = "protocol";

    static final String TAG_FILE_PATH = "file.path";
    static final String TAG_DESTINATION_PATH = "destination.path";
    static final String TAG_EVENT_TYPE = "event.type";
    static final String TAG_OPERATION_TYPE = "operation.type";
    static final String TAG_ERROR_TYPE = "error_type";

    FtpObserverContext(String context) {
        addTag(TAG_CONTEXT, context);
    }

    public FtpObserverContext(String context, String remoteUrl, String protocol) {
        this(context);
        addTag(TAG_REMOTE_URL, remoteUrl != null ? remoteUrl : FtpMetricsUtil.UNKNOWN);
        addTag(TAG_PROTOCOL, protocol != null ? protocol : FtpMetricsUtil.UNKNOWN);
    }
}
