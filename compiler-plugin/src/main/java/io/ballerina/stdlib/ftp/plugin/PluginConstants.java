/*
 * Copyright (c) 2022 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.ftp.plugin;

/**
 * Ftp compiler plugin constants.
 */
public class PluginConstants {

    public static final String PACKAGE_PREFIX = "ftp";
    public static final String ON_FILE_CHANGE_FUNC = "onFileChange";
    public static final String PACKAGE_ORG = "ballerina";

    // parameters
    public static final String WATCHEVENT = "WatchEvent";
    public static final String CALLER = "Caller";

    // return types error or nil
    public static final String ERROR = "error";

    // Code template related constants
    public static final String NODE_LOCATION = "node.location";
    public static final String LS = System.lineSeparator();
    public static final String CODE_TEMPLATE_NAME = "ADD_REMOTE_FUNCTION_CODE_SNIPPET";

    enum CompilationErrors {
        INVALID_ANNOTATION_NUMBER("No annotations are allowed for ftp services.", "FTP_101"),
        INVALID_REMOTE_FUNCTION("Only onFileChange remote method is allowed for ftp services.", "FTP_102"),
        METHOD_MUST_BE_REMOTE("onFileChange method must be remote.", "FTP_103"),
        RESOURCE_FUNCTION_NOT_ALLOWED("Resource functions are not allowed for ftp services.", "FTP_103"),
        NO_ON_FILE_CHANGE("onFileChange method not found.", "FTP_105"),
        MUST_HAVE_WATCHEVENT("Must have the required parameter ftp:WatchEvent & readonly or ftp:WatchEvent.",
                "FTP_106"),
        ONLY_PARAMS_ALLOWED("Invalid method parameter count. Only ftp:WatchEvent & readonly " +
                "or ftp:WatchEvent is allowed.", "FTP_107"),
        INVALID_PARAMETER("Invalid method parameter. Only ftp:WatchEvent & readonly or " +
                "ftp:WatchEvent is allowed.", "FTP_108"),
        INVALID_RETURN_TYPE_ERROR_OR_NIL("Invalid return type. Only error? or ftp:Error? is allowed.", "FTP_109"),
        TEMPLATE_CODE_GENERATION_HINT("Template generation for empty service", "FTP_110");
        private final String error;
        private final String errorCode;

        CompilationErrors(String error, String errorCode) {
            this.error = error;
            this.errorCode = errorCode;
        }

        String getError() {
            return error;
        }

        String getErrorCode() {
            return errorCode;
        }
    }
}
