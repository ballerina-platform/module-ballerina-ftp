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

import org.wso2.transport.remotefilesystem.Constants;

/**
 * Constants for ftp client connector.
 */
public class ClientConstants {

    public static final String PROPERTY_URI = Constants.URI;
    public static final String PROPERTY_DESTINATION = Constants.DESTINATION;

    public static final String FTP_PASSIVE_MODE = Constants.PASSIVE_MODE;
    public static final String USER_DIR_IS_ROOT = Constants.USER_DIR_IS_ROOT;
    public static final String AVOID_PERMISSION_CHECK = Constants.AVOID_PERMISSION_CHECK;

    public static final String URL = "URL";
    public static final String PROPERTY_MAP = "map";
}
