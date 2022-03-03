// Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/test;
import ballerina/lang.runtime;

// Disabling due to https://github.com/ballerina-platform/ballerina-standard-library/issues/2703
@test:Config {
    enable: true
}
public function testSFTPConnectionToFTPServer() returns error? {
    while !startedServers {
        runtime:sleep(2);
    }
    ClientConfiguration serverConfig = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21212,
        auth: {
            credentials: {username: "wso2", password: "wso2123"}
        }
    };
    Client|Error clientEp = new (serverConfig);
    if clientEp is Error {
        test:assertTrue(clientEp.message().startsWith("Error while connecting to the FTP server with URL: "),
            msg = "Unexpected error when tried to connect to a existing FTP server via SFTP. " + clientEp.message());
    } else {
        test:assertFail(msg = "Found a non-error response when tried to connect to a existing FTP server via SFTP.");
    }
}
