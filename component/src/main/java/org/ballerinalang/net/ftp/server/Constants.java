/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.net.ftp.server;

/**
 * Constants for FTP Server connector.
 */
public class Constants {

    //Annotation
    public static final String FTP_PACKAGE_NAME = "ballerina.net.ftp";
    public static final String FTP_SERVER_EVENT = "FTPServerEvent";
    public static final String FTP_SERVER_CONNECTOR = "serverConnector";
    public static final String CONFIG_ANNOTATION_NAME = "serviceConfig";

    public static final String ANNOTATION_DIR_URI = "dirURI";
    public static final String ANNOTATION_FILE_PATTERN = "fileNamePattern";
    public static final String ANNOTATION_POLLING_INTERVAL = "pollingInterval";
    public static final String ANNOTATION_CRON_EXPRESSION = "cronExpression";
    public static final String ANNOTATION_FILE_COUNT = "perPollFileCount";

    public static final String ANNOTATION_SORT_ATTRIBUTE = "fileSortAttribute";
    public static final String ANNOTATION_SORT_ASCENDING = "fileSortAscending";

    public static final String ANNOTATION_ACTION_AFTER_PROCESS = "actionAfterProcess";
    public static final String ANNOTATION_ACTION_AFTER_FAILURE = "actionAfterFailure";

    public static final String ANNOTATION_MOVE_AFTER_PROCESS = "moveAfterProcess";
    public static final String ANNOTATION_MOVE_AFTER_FAILURE = "moveAfterFailure";
    public static final String ANNOTATION_MOVE_TIMESTAMP_FORMAT = "moveTimestampFormat";
    public static final String ANNOTATION_CREATE_DIR = "createMoveDir";

    public static final String ANNOTATION_PARALLEL = "parallel";
    public static final String ANNOTATION_THREAD_POOL_SIZE = "threadPoolSize";

    public static final String ANNOTATION_SFTP_IDENTITIES = "sftpIdentities";
    public static final String ANNOTATION_SFTP_IDENTITY_PASS_PHRASE = "sftpIdentityPassPhrase";
    public static final String ANNOTATION_SFTP_USER_DIR_IS_ROOT = "sftpUserDirIsRoot";
    public static final String ANNOTATION_AVOID_PERMISSION_CHECK = "sftpAvoidpermissioncheck";
}
