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
public final class PluginConstants {

    private PluginConstants() {}

    public static final String PACKAGE_PREFIX = "ftp";
    public static final String ON_FILE_CHANGE_FUNC = "onFileChange";
    public static final String PACKAGE_ORG = "ballerina";

    // Content listener function names
    public static final String ON_FILE_FUNC = "onFile";
    public static final String ON_FILE_TEXT_FUNC = "onFileText";
    public static final String ON_FILE_JSON_FUNC = "onFileJson";
    public static final String ON_FILE_XML_FUNC = "onFileXml";
    public static final String ON_FILE_CSV_FUNC = "onFileCsv";
    public static final String ON_FILE_DELETED_FUNC = "onFileDeleted";

    // parameters
    public static final String WATCHEVENT = "WatchEvent";
    public static final String CALLER = "Caller";
    public static final String FILE_INFO = "FileInfo";

    // return types error or nil
    public static final String ERROR = "error";

    // Code template related constants
    public static final String NODE_LOCATION = "node.location";
    public static final String LS = System.lineSeparator();
    public static final String CODE_TEMPLATE_NAME_WITH_CALLER = "ADD_REMOTE_FUNCTION_CODE_SNIPPET_WITH_CALLER";
    public static final String CODE_TEMPLATE_NAME_WITHOUT_CALLER = "ADD_REMOTE_FUNCTION_CODE_SNIPPET_WITHOUT_CALLER";

    enum CompilationErrors {
        INVALID_REMOTE_FUNCTION("Invalid remote method. Only onFileChange (deprecated), content methods " +
                "(onFile, onFileText, onFileJson, onFileXml, onFileCsv), or onFileDeleted are allowed.", "FTP_101"),
        METHOD_MUST_BE_REMOTE("onFileChange method must be remote.", "FTP_102"),
        RESOURCE_FUNCTION_NOT_ALLOWED("Resource functions are not allowed for ftp services.", "FTP_103"),
        MUST_HAVE_WATCHEVENT("Must have the required parameter ftp:WatchEvent & readonly or ftp:WatchEvent.",
                "FTP_105"),
        ONLY_PARAMS_ALLOWED("Invalid method parameter count. Only ftp:WatchEvent & readonly " +
                "or ftp:WatchEvent is allowed.", "FTP_106"),
        INVALID_WATCHEVENT_PARAMETER("Invalid method parameter. Only ftp:WatchEvent & readonly or " +
                "ftp:WatchEvent is allowed.", "FTP_107"),
        INVALID_CALLER_PARAMETER("Invalid method parameter. Only ftp:Caller is allowed", "FTP_108"),
        INVALID_PARAMETERS("Invalid method parameters. Only ftp:WatchEvent & readonly or ftp:WatchEvent and " +
                "ftp:Caller is allowed.", "FTP_109"),
        INVALID_RETURN_TYPE_ERROR_OR_NIL("Invalid return type. Only error? or ftp:Error? is allowed.", "FTP_110"),
        TEMPLATE_CODE_GENERATION_HINT("Template generation for empty service", "FTP_111"),
        MULTIPLE_CONTENT_METHODS("Only one content handling strategy is allowed. Cannot mix onFileChange with " +
                "content methods (onFile, onFileText, onFileJson, onFileXml, onFileCsv) or onFileDeleted.", "FTP_112"),
        CONTENT_METHOD_MUST_BE_REMOTE("Content handler method '%s' must be remote.", "FTP_115"),
        INVALID_CONTENT_PARAMETER_TYPE("Invalid first parameter type for '%s'. Expected '%s', found '%s'.",
                "FTP_116"),
        INVALID_FILEINFO_PARAMETER("Invalid fileInfo parameter for '%s'. Only ftp:FileInfo is allowed.", "FTP_117"),
        TOO_MANY_PARAMETERS("Too many parameters for '%s'. Content methods accept at most 3 parameters: " +
                "(content, fileInfo?, caller?).", "FTP_118"),
        NO_VALID_REMOTE_METHOD("No valid remote method found. Service must have either onFileChange or content " +
                "handler methods (onFile, onFileText, onFileJson, onFileXml, onFileCsv) or onFileDeleted.", "FTP_119"),
        ON_FILE_DELETED_MUST_BE_REMOTE("onFileDeleted method must be remote.", "FTP_123"),
        INVALID_ON_FILE_DELETED_PARAMETER("Invalid parameter for onFileDeleted. First parameter must be string[].",
                "FTP_124"),
        INVALID_ON_FILE_DELETED_CALLER_PARAMETER("Invalid second parameter for onFileDeleted. " +
                "Only ftp:Caller is allowed.", "FTP_125"),
        TOO_MANY_PARAMETERS_ON_FILE_DELETED("Too many parameters. onFileDeleted accepts at most 2 parameters: " +
                "(deletedFiles, caller?).", "FTP_126"),
        ON_FILE_CHANGE_DEPRECATED("onFileChange method is deprecated. Use content handler methods or onFileDeleted.",
                                          "FTP_127");
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
