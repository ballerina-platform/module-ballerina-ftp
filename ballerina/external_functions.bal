// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/jballerina.java;

isolated function initEndpoint(Client clientEndpoint, map<anydata> config) returns Error? = @java:Method {
    name: "initClientEndpoint",
    'class: "io.ballerina.stdlib.ftp.client.FtpClient"
} external;

isolated function getBytes(Client clientEndpoint, string path) returns byte[]|Error = @java:Method {
    name: "getBytes",
    'class: "io.ballerina.stdlib.ftp.client.FtpClient"
} external;

isolated function getText(Client clientEndpoint, string path) returns string|Error = @java:Method {
    name: "getText",
    'class: "io.ballerina.stdlib.ftp.client.FtpClient"
} external;

isolated function delete(Client clientEndpoint, string path) returns Error? = @java:Method {
    name: "delete",
    'class: "io.ballerina.stdlib.ftp.client.FtpClient"
} external;

isolated function append(Client clientEndpoint, InputContent inputContent) returns Error? = @java:Method {
    name: "append",
    'class: "io.ballerina.stdlib.ftp.client.FtpClient"
} external;

isolated function put(Client clientEndpoint, InputContent inputContent) returns Error? = @java:Method {
    name: "put",
    'class: "io.ballerina.stdlib.ftp.client.FtpClient"
} external;

isolated function putJson(Client clientEndpoint, string path, string content, FileWriteOption option) returns Error? = @java:Method {
    name: "putJson",
    'class: "io.ballerina.stdlib.ftp.client.FtpClient"  
} external;

isolated function putXml(Client clientEndpoint, string path, xml content, FileWriteOption option) returns Error? = @java:Method {
    name: "putXml",
    'class: "io.ballerina.stdlib.ftp.client.FtpClient"  
} external;

isolated function putCsv(Client clientEndpoint, string path, string[][]|record {}[] content, FileWriteOption option) returns Error? = @java:Method {
    name: "putCsv",
    'class: "io.ballerina.stdlib.ftp.client.FtpClient"  
} external;

isolated function mkdir(Client clientEndpoint, string path) returns Error? = @java:Method {
    name: "mkdir",
    'class: "io.ballerina.stdlib.ftp.client.FtpClient"
} external;

isolated function rmdir(Client clientEndpoint, string path) returns Error? = @java:Method {
    name: "rmdir",
    'class: "io.ballerina.stdlib.ftp.client.FtpClient"
} external;

isolated function rename(Client clientEndpoint, string origin, string destination) returns Error? = @java:Method {
    name: "rename",
    'class: "io.ballerina.stdlib.ftp.client.FtpClient"
} external;

isolated function move(Client clientEndpoint, string sourcePath, string destinationPath) returns Error? = @java:Method {
    name: "move",
    'class: "io.ballerina.stdlib.ftp.client.FtpClient"
} external;

isolated function copy(Client clientEndpoint, string sourcePath, string destinationPath) returns Error? = @java:Method {
    name: "copy",
    'class: "io.ballerina.stdlib.ftp.client.FtpClient"
} external;

isolated function exists(Client clientEndpoint, string path) returns boolean|Error = @java:Method {
    name: "exists",
    'class: "io.ballerina.stdlib.ftp.client.FtpClient"
} external;

isolated function size(Client clientEndpoint, string path) returns int|Error = @java:Method {
    name: "size",
    'class: "io.ballerina.stdlib.ftp.client.FtpClient"
} external;

isolated function list(Client clientEndpoint, string path) returns FileInfo[]|Error = @java:Method {
    name: "list",
    'class: "io.ballerina.stdlib.ftp.client.FtpClient"
} external;

isolated function isDirectory(Client clientEndpoint, string path) returns boolean|Error = @java:Method {
    name: "isDirectory",
    'class: "io.ballerina.stdlib.ftp.client.FtpClient"
} external;

isolated function poll(Listener listenerEndpoint) returns Error? = @java:Method {
    name: "poll",
    'class: "io.ballerina.stdlib.ftp.server.FtpListenerHelper"
} external;

isolated function initListener(Listener listenerEndpoint, ListenerConfiguration config)
        returns Error? = @java:Method {
    name: "init",
    'class: "io.ballerina.stdlib.ftp.server.FtpListenerHelper"
} external;

isolated function register(Listener listenerEndpoint, Service ftpService) returns Error? = @java:Method {
    name: "register",
    'class: "io.ballerina.stdlib.ftp.server.FtpListenerHelper"
} external;

isolated function deregister(Listener listenerEndpoint, Service ftpService) returns Error? = @java:Method {
    name: "deregister",
    'class: "io.ballerina.stdlib.ftp.server.FtpListenerHelper"
} external;
