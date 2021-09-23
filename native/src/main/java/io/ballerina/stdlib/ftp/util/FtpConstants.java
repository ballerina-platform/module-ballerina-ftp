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

package io.ballerina.stdlib.ftp.util;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BString;

/**
 * Constants for FTP operations.
 */
public class FtpConstants {

    private FtpConstants() {
        // private constructor
    }

    public static final String SUCCESSFULLY_FINISHED_THE_ACTION = "Successfully finished the action.";

    public static final String FILE_NAME_PATTERN = "fileNamePattern";
    public static final String SCHEME_SFTP = "sftp";
    public static final String SCHEME_FTP = "ftp";
    public static final String URI = "uri";
    public static final String PASSIVE_MODE = "PASSIVE_MODE";
    public static final String USER_DIR_IS_ROOT = "USER_DIR_IS_ROOT";
    public static final String DESTINATION = "destination";
    public static final String IDENTITY = "IDENTITY";
    public static final String IDENTITY_PASS_PHRASE = "IDENTITY_PASS_PHRASE";
    public static final String AVOID_PERMISSION_CHECK = "AVOID_PERMISSION_CHECK";
    public static final String PROPERTY_MAP = "map";
    public static final String VFS_CLIENT_CONNECTOR = "VfsClientConnector";
    public static final String FTP_ORG_NAME = "ballerina";
    public static final String FTP_MODULE_NAME = "ftp";
    public static final String ENTITY_BYTE_STREAM = "entity_byte_stream";
    public static final String READ_INPUT_STREAM = "readInputStream";
    public static final String ARRAY_SIZE = "arraySize";
    public static final String BYTE_STREAM_NEXT_FUNC = "next";
    public static final String BYTE_STREAM_CLOSE_FUNC = "close";
    public static final String STREAM_ENTRY_RECORD = "StreamEntry";
    public static final BString FIELD_VALUE = StringUtils.fromString("value");

    public static final String FTP_SERVER_EVENT = "WatchEvent";
    public static final String FTP_FILE_INFO = "FileInfo";
    public static final String FTP_SERVER_CONNECTOR = "serverConnector";
    public static final String FTP_ANONYMOUS_USERNAME = "anonymous";
    public static final String FTP_ANONYMOUS_PASSWORD = "";

    public static final String ENDPOINT_CONFIG_PROTOCOL = "protocol";
    public static final String ENDPOINT_CONFIG_HOST = "host";
    public static final String ENDPOINT_CONFIG_PORT = "port";
    public static final String ENDPOINT_CONFIG_USERNAME = "username";
    public static final String ENDPOINT_CONFIG_PASS_KEY = "password";
    public static final String ENDPOINT_CONFIG_PATH = "path";
    public static final String ENDPOINT_CONFIG_FILE_PATTERN = "fileNamePattern";
    public static final String ENDPOINT_CONFIG_AUTH = "auth";
    public static final String ENDPOINT_CONFIG_CREDENTIALS = "credentials";
    public static final String ENDPOINT_CONFIG_PRIVATE_KEY = "privateKey";

    public static final String INPUT_CONTENT_FILE_PATH_KEY = "filePath";
    public static final String INPUT_CONTENT_IS_FILE_KEY = "isFile";
    public static final String INPUT_CONTENT_FILE_CONTENT_KEY = "fileContent";
    public static final String INPUT_CONTENT_TEXT_CONTENT_KEY = "textContent";
    public static final String INPUT_CONTENT_COMPRESS_INPUT_KEY = "compressInput";

    public static final String ON_FILE_CHANGE_REMOTE_FUNCTION = "onFileChange";

}
