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

import ballerina/io;
import ballerina/jballerina.java;

isolated function initEndpoint(Client clientEndpoint, map<anydata> config) returns Error? = @java:Method {
    name: "initClientEndpoint",
    'class: "org.ballerinalang.stdlib.ftp.client.FTPClient"
} external;

isolated function get(Client clientEndpoint, handle path) returns io:ReadableByteChannel|Error = @java:Method{
    name: "get",
    'class: "org.ballerinalang.stdlib.ftp.client.FTPClient"
} external;

isolated function delete(Client clientEndpoint, handle path) returns Error? = @java:Method{
    name: "delete",
    'class: "org.ballerinalang.stdlib.ftp.client.FTPClient"
} external;

isolated function append(Client clientEndpoint, InputContent inputContent) returns Error? = @java:Method{
    name: "append",
    'class: "org.ballerinalang.stdlib.ftp.client.FTPClient"
} external;

isolated function put(Client clientEndpoint, InputContent inputContent) returns Error? = @java:Method{
    name: "put",
    'class: "org.ballerinalang.stdlib.ftp.client.FTPClient"
} external;

isolated function mkdir(Client clientEndpoint, handle path) returns Error? = @java:Method{
    name: "mkdir",
    'class: "org.ballerinalang.stdlib.ftp.client.FTPClient"
} external;

isolated function rmdir(Client clientEndpoint, handle path) returns Error? = @java:Method{
    name: "rmdir",
    'class: "org.ballerinalang.stdlib.ftp.client.FTPClient"
} external;

isolated function rename(Client clientEndpoint, handle origin, handle destination) returns Error? = @java:Method{
    name: "rename",
    'class: "org.ballerinalang.stdlib.ftp.client.FTPClient"
} external;

isolated function size(Client clientEndpoint, handle path) returns int|Error = @java:Method{
    name: "size",
    'class: "org.ballerinalang.stdlib.ftp.client.FTPClient"
} external;

isolated function list(Client clientEndpoint, handle path) returns FileInfo[]|Error = @java:Method{
    name: "list",
    'class: "org.ballerinalang.stdlib.ftp.client.FTPClient"
} external;

isolated function isDirectory(Client clientEndpoint, handle path) returns boolean|Error = @java:Method{
    name: "isDirectory",
    'class: "org.ballerinalang.stdlib.ftp.client.FTPClient"
} external;

isolated function poll(ListenerConfig config) returns Error? = @java:Method{
    name: "poll",
    'class: "org.ballerinalang.stdlib.ftp.server.FTPListenerHelper"
} external;

isolated function register(Listener listenerEndpoint, ListenerConfig config, service object {} ftpService, handle name)
    returns handle|Error = @java:Method{
    name: "register",
    'class: "org.ballerinalang.stdlib.ftp.server.FTPListenerHelper"
} external;
