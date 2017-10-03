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
package org.ballerinalang.net.ftp.nativeimpl.util;

import org.wso2.carbon.transport.remotefilesystem.Constants;

/**
 * Constants for ftp client connector.
 */
public class FTPConstants {
    public static final String FTP_CONNECTOR_NAME = "file";
    public static final String CONNECTOR_NAME = "ClientConnector";
    public static final String PROPERTY_URI = Constants.URI;
    public static final String PROPERTY_SOURCE = "source";
    public static final String PROPERTY_DESTINATION = Constants.DESTINATION;
    public static final String PROPERTY_ACTION = Constants.ACTION;
    public static final String PROPERTY_FOLDER = Constants.CREATE_FOLDER;
    public static final String ACTION_COPY = Constants.COPY;
    public static final String ACTION_CREATE = Constants.CREATE;
    public static final String ACTION_DELETE = Constants.DELETE;
    public static final String ACTION_EXISTS = Constants.EXISTS;
    public static final String ACTION_MOVE = Constants.MOVE;
    public static final String ACTION_READ = Constants.READ;
    public static final String ACTION_WRITE = Constants.WRITE;
    public static final String TYPE_FILE = "file";
    public static final String TYPE_FOLDER = "folder";
    public static final String FTP_PASSIVE_MODE = Constants.FTP_PASSIVE_MODE;
    public static final String PROTOCOL = Constants.PROTOCOL;
    public static final String PROTOCOL_FTP = Constants.PROTOCOL_FTP;
}
