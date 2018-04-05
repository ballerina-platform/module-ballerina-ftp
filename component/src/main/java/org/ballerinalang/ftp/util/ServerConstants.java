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

package org.ballerinalang.ftp.util;

/**
 * Constants for FTP Server connector.
 */
public class ServerConstants {

    public static final String URL = "URL";

    public static final String FTP_PACKAGE_NAME = "ballerina.ftp";
    public static final String FTP_SERVER_EVENT = "FTPServerEvent";
    public static final String FTP_SERVER_CONNECTOR = "serverConnector";
    public static final String CONFIG_ANNOTATION_NAME = "serviceConfig";

    public static final String ANNOTATION_DIR_URI = "dirURI";
    public static final String ANNOTATION_PROTOCOL = "protocol";
    public static final String ANNOTATION_HOST = "host";
    public static final String ANNOTATION_PORT = "port";
    public static final String ANNOTATION_USERNAME = "username";
    public static final String ANNOTATION_PASSPHRASE = "passPhrase";
    public static final String ANNOTATION_PATH = "path";
    public static final String ANNOTATION_FILE_PATTERN = "fileNamePattern";
    public static final String ANNOTATION_POLLING_INTERVAL = "pollingInterval";
    public static final String ANNOTATION_CRON_EXPRESSION = "cronExpression";
    public static final String ANNOTATION_FILE_COUNT = "perPollFileCount";
    public static final String ANNOTATION_PARALLEL = "parallel";
    public static final String ANNOTATION_THREAD_POOL_SIZE = "threadPoolSize";

    public static final String ANNOTATION_SFTP_IDENTITIES = "sftpIdentities";
    public static final String ANNOTATION_SFTP_IDENTITY_PASS_PHRASE = "sftpIdentityPassPhrase";
    public static final String ANNOTATION_SFTP_USER_DIR_IS_ROOT = "sftpUserDirIsRoot";
    public static final String ANNOTATION_AVOID_PERMISSION_CHECK = "sftpAvoidPermissionCheck";

    public static final String ANNOTATION_ACTION_AFTER_PROCESS = "actionAfterProcess";
    public static final String ANNOTATION_ACTION_AFTER_FAILURE = "actionAfterFailure";
    public static final String ANNOTATION_MOVE_AFTER_PROCESS = "moveAfterProcess";
    public static final String ANNOTATION_MOVE_AFTER_FAILURE = "moveAfterFailure";
}
