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

import ballerina/ftp;
import ballerina/io;
import ballerina/lang.'string as strings;
import ballerina/test;

string appendFilePath = "tests/resources/datafiles/file1.txt";
string putFilePath = "tests/resources/datafiles/file2.txt";
string nonFittingFilePath = "/home/in/test4.txt";

ftp:ClientConfiguration sftpConfig = {
    protocol: ftp:SFTP,
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

ftp:ClientConfiguration sftpConfigUserDirRoot = {
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
    userDirIsRoot: true
};

ftp:Client? sftpClientEp = ();
ftp:Client? sftpClientUserDirRootEp = ();

ftp:Listener? callerListener = ();
ftp:Listener? secureRemoteServerListener = ();

@test:BeforeSuite
function initSftpTestEnvironment() returns error? {
    sftpClientEp = check new (sftpConfig);
    sftpClientUserDirRootEp = check new (sftpConfigUserDirRoot);

    callerListener = check new (callerListenerConfig);
    check (<ftp:Listener>callerListener).attach(callerService);
    check (<ftp:Listener>callerListener).'start();

    secureRemoteServerListener = check new (secureRemoteServerConfig);
    check (<ftp:Listener>secureRemoteServerListener).attach(secureRemoteServerService);
    check (<ftp:Listener>secureRemoteServerListener).'start();
}

@test:AfterSuite {}
function cleanSftpTestEnvironment() returns error? {
    check (<ftp:Listener>callerListener).gracefulStop();
    check (<ftp:Listener>secureRemoteServerListener).gracefulStop();
}
