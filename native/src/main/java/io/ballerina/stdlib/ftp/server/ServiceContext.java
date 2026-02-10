/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

package io.ballerina.stdlib.ftp.server;

import io.ballerina.runtime.api.values.BObject;

/**
 * Encapsulates per-service state required by the FTP listener.
 */
public class ServiceContext {

    private final BObject service;
    private final ServiceConfiguration configuration;
    private final FormatMethodsHolder formatMethodsHolder;
    private volatile BObject caller;

    public ServiceContext(BObject service, ServiceConfiguration configuration,
                          FormatMethodsHolder formatMethodsHolder, BObject caller) {
        this.service = service;
        this.configuration = configuration;
        this.formatMethodsHolder = formatMethodsHolder;
        this.caller = caller;
    }

    public BObject getService() {
        return service;
    }

    public ServiceConfiguration getConfiguration() {
        return configuration;
    }

    public FormatMethodsHolder getFormatMethodsHolder() {
        return formatMethodsHolder;
    }

    public BObject getCaller() {
        return caller;
    }

    public void setCaller(BObject caller) {
        this.caller = caller;
    }

    public String getPath() {
        return configuration == null ? null : configuration.getPath();
    }
}
