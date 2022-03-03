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

import ballerina/test;
import ballerina/jballerina.java;

string filePath = "/home/in/test1.txt";
string nonFittingFilePath = "/home/in/test4.txt";
string newFilePath = "/home/in/test2.txt";
string appendFilePath = "tests/resources/datafiles/file1.txt";
string putFilePath = "tests/resources/datafiles/file2.txt";

// Create the config to access anonymous mock FTP server
ClientConfiguration anonConfig = {
    protocol: FTP,
    host: "127.0.0.1",
    port: 21210
};

Client anonClientEp = checkpanic new (anonConfig);

// Create the config to access mock FTP server
ClientConfiguration config = {
    protocol: FTP,
    host: "127.0.0.1",
    port: 21212,
    auth: {credentials: {username: "wso2", password: "wso2123"}}
};

Client clientEp = checkpanic new (config);

// Create the config to access mock SFTP server
ClientConfiguration sftpConfig = {
    protocol: SFTP,
    host: "127.0.0.1",
    port: 21213,
    auth: {
        credentials: {username: "wso2", password: "wso2123"},
        privateKey: {
            path: "tests/resources/sftp.private.key",
            password: "changeit"
        }
    }
};

Client sftpClientEp = checkpanic new (sftpConfig);

// Start mock FTP servers
boolean startedServers = initServers();

function initServers() returns boolean {
    Error? response0 = initAnonymousFtpServer(anonConfig);
    Error? response1 = initFtpServer(config);
    Error? response2 = initSftpServer(sftpConfig);
    return !(response0 is Error || response1 is Error || response2 is Error);
}

@test:AfterSuite {}
public function stopServer() returns error? {
    _ = stopAnonymousFtpServer();
    _ = stopFtpServer();
    check stopSftpServer();
}

function initAnonymousFtpServer(map<anydata> config) returns Error? = @java:Method {
    name: "initAnonymousFtpServer",
    'class: "io.ballerina.stdlib.ftp.testutils.mockServerUtils.MockFtpServer"
} external;

function initFtpServer(map<anydata> config) returns Error? = @java:Method {
    name: "initFtpServer",
    'class: "io.ballerina.stdlib.ftp.testutils.mockServerUtils.MockFtpServer"
} external;

function initSftpServer(map<anydata> config) returns Error? = @java:Method {
    name: "initSftpServer",
    'class: "io.ballerina.stdlib.ftp.testutils.mockServerUtils.MockFtpServer"
} external;

function stopFtpServer() returns () = @java:Method {
    name: "stopFtpServer",
    'class: "io.ballerina.stdlib.ftp.testutils.mockServerUtils.MockFtpServer"
} external;

function stopAnonymousFtpServer() returns () = @java:Method {
    name: "stopAnonymousFtpServer",
    'class: "io.ballerina.stdlib.ftp.testutils.mockServerUtils.MockFtpServer"
} external;

function stopSftpServer() returns error? = @java:Method {
    name: "stopSftpServer",
    'class: "io.ballerina.stdlib.ftp.testutils.mockServerUtils.MockFtpServer"
} external;
