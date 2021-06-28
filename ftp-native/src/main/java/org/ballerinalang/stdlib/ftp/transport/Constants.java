/*
 * Copyright (c) 2017 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.ballerinalang.stdlib.ftp.transport;

/**
 * This class contains the constants related to File transport.
 */
public final class Constants {

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

    private Constants() {
    }
}
