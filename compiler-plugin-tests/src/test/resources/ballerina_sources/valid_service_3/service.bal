// Copyright (c) 2022 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/ftp;

ftp:AuthConfiguration authConfig = {
    credentials: {username: "wso2", password: "wso2123"},
    privateKey: {
        path: "resources/sftp.private.key",
        password: "changeit"
    }
};

listener ftp:Listener secureRemoteServer = check new({
    protocol: ftp:SFTP,
    host: "localhost",
    auth: authConfig,
    port: 21213,
    pollingInterval: 2,
    path: "/upload/",
    fileNamePattern: "(.*).csv"
});

service "Test1" on secureRemoteServer {
    private final string var1 = "FTP Service";
    private final int var2 = 54;

    remote function onFileChange(ftp:WatchEvent event) {
    }
}

service "Test2" on secureRemoteServer {
    private final string var1 = "FTP Service";
    private final int var2 = 54;

    remote function onFileChange(ftp:WatchEvent event, ftp:Caller caller) {
    }
}

service "Test3" on secureRemoteServer {
    private final string var1 = "FTP Service";
    private final int var2 = 54;

    remote function onFileChange(ftp:WatchEvent & readonly event, ftp:Caller caller) {
    }
}

service "Test4" on secureRemoteServer {
    private final string var1 = "FTP Service";
    private final int var2 = 54;

    remote function onFileChange(ftp:Caller caller, ftp:WatchEvent & readonly event) {
    }
}

service "Test5" on secureRemoteServer {
    private final string var1 = "FTP Service";
    private final int var2 = 54;

    remote function onFileChange(ftp:Caller caller, ftp:WatchEvent event) {
    }
}
