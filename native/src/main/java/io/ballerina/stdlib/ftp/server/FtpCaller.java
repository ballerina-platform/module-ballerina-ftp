/*
 * Copyright (c) 2025 WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.stdlib.ftp.server;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.concurrent.StrandMetadata;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BTypedesc;
import io.ballerina.stdlib.ftp.util.FtpConstants;
import io.ballerina.stdlib.ftp.util.FtpUtil;

public class FtpCaller {

    public static Object getJson(Environment env, BObject clientConnector, BString filePath, BTypedesc typeDesc) {
        return invokeClientMethod(env, clientConnector, "getJson", filePath, typeDesc);
    }

    public static Object getXml(Environment env, BObject clientConnector, BString filePath, BTypedesc typeDesc) {
        return invokeClientMethod(env, clientConnector, "getXml", filePath, typeDesc);
    }

    public static Object getCsv(Environment env, BObject clientConnector, BString filePath, BTypedesc typeDesc) {
        return invokeClientMethod(env, clientConnector, "getCsv", filePath, typeDesc);
    }

    public static Object getBytesAsStream(Environment env, BObject clientConnector, BString filePath) {
        return invokeClientMethod(env, clientConnector, "getBytesAsStream", filePath);
    }

    public static Object getCsvAsStream(Environment env, BObject clientConnector, BString filePath,
                                        BTypedesc typeDesc) {
        return invokeClientMethod(env, clientConnector, "getCsvAsStream", filePath, typeDesc);
    }

    private static Object invokeClientMethod(Environment env, BObject clientConnector, String methodName,
                                             Object... args) {
        return env.yieldAndRun(() -> {
            try {
                BObject clientObj = clientConnector.getObjectValue(StringUtils.fromString("client"));
                StrandMetadata strandMetadata = new StrandMetadata(true, null);
                return env.getRuntime().callMethod(clientObj, methodName, strandMetadata, args);
            } catch (BError bError) {
                return FtpUtil.createError("client method invocation failed: " + bError.getErrorMessage(),
                        bError, FtpConstants.FTP_ERROR);
            }
        });
    }
}
