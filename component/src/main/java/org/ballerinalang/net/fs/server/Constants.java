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

package org.ballerinalang.net.fs.server;

/**
 * Constants for File System Server connector.
 */
public class Constants {

    //Annotation
    public static final String ANNOTATION_DIR_URI = "dirURI";
    public static final String ANNOTATION_EVENTS = "events";
    public static final String ANNOTATION_DIRECTORY_RECURSIVE = "recursive";
    public static final String CONFIG_ANNOTATION_NAME = "serviceConfig";

    public static final String FILE_SYSTEM_PACKAGE_NAME = "ballerina.net.fs";
    public static final String FILE_SYSTEM_EVENT = "FileSystemEvent";
    public static final String FILE_SYSTEM_ERROR = "FSError";
    public static final String FS_SERVER_CONNECTOR = "serverConnector";
}
