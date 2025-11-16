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

import java.util.Set;

/**
 * Ftp compiler plugin constants.
 */
public final class PluginConstants {

    private PluginConstants() {}

    public static final String PACKAGE_PREFIX = "ftp";
    public static final String ON_FILE_CHANGE_FUNC = "onFileChange";
    public static final String PACKAGE_ORG = "ballerina";

    // Format-specific handler function names
    public static final String ON_FILE_FUNC = "onFile";
    public static final String ON_FILE_TEXT_FUNC = "onFileText";
    public static final String ON_FILE_JSON_FUNC = "onFileJson";
    public static final String ON_FILE_XML_FUNC = "onFileXml";
    public static final String ON_FILE_CSV_FUNC = "onFileCsv";

    // Event-based handler function names
    public static final String ON_FILE_DELETED_FUNC = "onFileDeleted";

    /**
     * All format-specific handler names.
     * These handlers automatically convert file content to typed data.
     */
    public static final Set<String> ALL_FORMAT_HANDLERS = Set.of(
            ON_FILE_FUNC,
            ON_FILE_TEXT_FUNC,
            ON_FILE_JSON_FUNC,
            ON_FILE_XML_FUNC,
            ON_FILE_CSV_FUNC
    );

    // parameters
    public static final String WATCHEVENT = "WatchEvent";
    public static final String CALLER = "Caller";
    public static final String FILE_INFO = "FileInfo";

    // return types error or nil
    public static final String ERROR = "error";

    // Code template related constants (Format-specific handlers)
    public static final String NODE_LOCATION = "node.location";
    public static final String LS = System.lineSeparator();
    public static final String CODE_TEMPLATE_GENERIC = "ADD_ONFILE_HANDLER_TEMPLATE";
    public static final String CODE_TEMPLATE_JSON = "ADD_ONFILEJSON_HANDLER_TEMPLATE";
    public static final String CODE_TEMPLATE_XML = "ADD_ONFILEXML_HANDLER_TEMPLATE";
    public static final String CODE_TEMPLATE_CSV = "ADD_ONFILECSV_HANDLER_TEMPLATE";
    public static final String CODE_TEMPLATE_TEXT = "ADD_ONFILETEXT_HANDLER_TEMPLATE";

    public enum CompilationErrors {
        INVALID_REMOTE_FUNCTION("Invalid remote method. Allowed handlers: onFile, onFileText, onFileJson, " +
                "onFileXml, onFileCsv (format-specific), onFileDeleted, or onFileChange (deprecated).", "FTP_101"),
        METHOD_MUST_BE_REMOTE("onFileChange method must be remote.", "FTP_102"),
        RESOURCE_FUNCTION_NOT_ALLOWED("Resource functions are not allowed for ftp services.", "FTP_103"),
        MUST_HAVE_WATCHEVENT("Missing required parameter. Use either 'WatchEvent & readonly' or 'WatchEvent' " +
                "as the first parameter.", "FTP_105"),
        ONLY_PARAMS_ALLOWED("Too many parameters. onFileChange accepts at most 2 parameters: " +
                "(WatchEvent, Caller?).", "FTP_106"),
        INVALID_WATCHEVENT_PARAMETER("Invalid parameter type. Expected 'WatchEvent' or 'WatchEvent & readonly'.",
                "FTP_107"),
        INVALID_CALLER_PARAMETER("Invalid parameter type. Expected 'Caller'.", "FTP_108"),
        INVALID_PARAMETERS("Invalid parameter combination. Expected: (WatchEvent) or (WatchEvent & Caller).",
                "FTP_109"),
        INVALID_RETURN_TYPE_ERROR_OR_NIL("Invalid return type. Expected 'error?' or 'ftp:Error?'.", "FTP_110"),
        TEMPLATE_CODE_GENERATION_HINT("Empty FTP service. Click to generate handler method template.",
                "FTP_111"),
        MULTIPLE_CONTENT_METHODS("Cannot mix event-based handler (onFileChange) with " +
                "format-specific handlers (onFile, onFileText, onFileJson, onFileXml, onFileCsv, onFileDeleted).",
                "FTP_112"),
        CONTENT_METHOD_MUST_BE_REMOTE("'%s' handler must be declared as remote.", "FTP_115"),
        MANDATORY_PARAMETER_NOT_FOUND("Mandatory parameter missing for '%s'. Expected '%s'.", "FTP_120"),
        INVALID_CONTENT_PARAMETER_TYPE("Invalid parameter type for handler '%s'. " +
                "Expected '%s', found '%s'.", "FTP_116"),
        INVALID_FILEINFO_PARAMETER("Invalid parameter for '%s'. Optional second parameter must be 'FileInfo'.",
                "FTP_117"),
        TOO_MANY_PARAMETERS("Too many parameters for '%s'. Format-specific handlers accept at most 3 parameters: " +
                "(content, fileInfo?, caller?).", "FTP_118"),
        NO_VALID_REMOTE_METHOD("Service must define at least one handler method: onFile, onFileText, onFileJson, " +
                "onFileXml, onFileCsv (format-specific) or onFileDeleted.", "FTP_119"),
        ON_FILE_DELETED_MUST_BE_REMOTE("onFileDeleted method must be remote.", "FTP_123"),
        INVALID_ON_FILE_DELETED_PARAMETER("Invalid parameter for onFileDeleted. First parameter must be " +
                "'string[]' (list of deleted file paths).", "FTP_124"),
        INVALID_ON_FILE_DELETED_CALLER_PARAMETER("Invalid second parameter for onFileDeleted. " +
                "Optional second parameter must be 'Caller'.", "FTP_125"),
        TOO_MANY_PARAMETERS_ON_FILE_DELETED("Too many parameters for onFileDeleted. Accepts at most 2 parameters: " +
                "(deletedFiles, caller?).", "FTP_126"),
        ON_FILE_CHANGE_DEPRECATED("'onFileChange' is deprecated. Use format-specific handlers (onFileJson, " +
                "onFileXml, onFileCsv, onFileText) for automatic type conversion, or onFileDeleted for deletion " +
                "events.", "FTP_127");
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
