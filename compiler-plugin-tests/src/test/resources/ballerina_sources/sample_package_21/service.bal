// Copyright (c) 2025 WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
//
// WSO2 LLC. licenses this file to you under the Apache License,
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

listener ftp:Listener secureEP = check new({
    protocol: ftp:SFTP,
    host: "127.0.0.1",
    port: 21213,
    auth: {
        credentials: {username: "wso2", password: "wso2123"},
        privateKey: {
            path: "tests/resources/sftp.private.key",
            password: "changeit"
        }
    },
    path: "/home/in"
});

// Invalid: onFileDeleted second parameter is not Caller
service on secureEP {
    remote function onFileDeleted(string[] deletedFiles, string invalidParam) returns error? {
    }
}
