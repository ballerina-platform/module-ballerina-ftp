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
package org.ballerinalang.net.ftp.client.nativeimpl.util;

import org.wso2.transport.remotefilesystem.Constants;

/**
 * Constants for ftp client connector.
 */
public class FTPConstants {
    public static final String PROPERTY_URI = Constants.URI;
    public static final String PROPERTY_DESTINATION = Constants.DESTINATION;
    public static final String PROPERTY_ACTION = Constants.ACTION;

    public static final String ACTION_GET = Constants.GET;
    public static final String ACTION_PUT = Constants.PUT;
    public static final String ACTION_APPEND = Constants.PUT;
    public static final String ACTION_DELETE = Constants.DELETE;
    public static final String ACTION_MKDIR = Constants.MKDIR;
    public static final String ACTION_RMDIR = Constants.RMDIR;
    public static final String ACTION_RENAME = Constants.RENAME;
    public static final String ACTION_SIZE = Constants.SIZE;
    public static final String ACTION_LIST = Constants.LIST;

    public static final String FTP_PASSIVE_MODE = Constants.FTP_PASSIVE_MODE;
    public static final String PROTOCOL = Constants.PROTOCOL;
    public static final String PROTOCOL_FTP = Constants.PROTOCOL_FTP;
    public static final String PROPERTY_APPEND = Constants.APPEND;

    public static final String URL = "URL";
}
