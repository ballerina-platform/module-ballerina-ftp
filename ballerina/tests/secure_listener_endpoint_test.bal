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

import ballerina/lang.runtime as runtime;
import ballerina/log;
import ballerina/test;

int secureAddedFileCount = 0;
int secureDeletedFileCount = 0;
boolean secureWatchEventReceived = false;

listener Listener secureRemoteServer = check new ({
    protocol: SFTP,
    host: "127.0.0.1",
    auth: {
        credentials: {
            username: "wso2",
            password: "wso2123"
        },
        privateKey: {
            path: "tests/resources/sftp.private.key",
            password: "changeit"
        }
    },
    port: 21213,
    pollingInterval: 2,
    fileNamePattern: "(.*).txt"
});

service "ftpServerConnector" on secureRemoteServer {
    function onFileChange(WatchEvent event) {
        secureAddedFileCount = event.addedFiles.length();
        secureDeletedFileCount = event.deletedFiles.length();
        secureWatchEventReceived = true;

        foreach FileInfo addedFile in event.addedFiles {
            log:printInfo("Added file path: " + addedFile.path);
        }
        foreach string deletedFile in event.deletedFiles {
            log:printInfo("Deleted file path: " + deletedFile);
        }
    }
}

@test:Config {
}
public function testSecureAddedFileCount() {
    int timeoutInSeconds = 300;
    // Test fails in 5 minutes if failed to receive watchEvent
    while timeoutInSeconds > 0 {
        if secureWatchEventReceived {
            log:printInfo("Securely added file count: " + secureAddedFileCount.toString());
            test:assertEquals(secureAddedFileCount, 2);
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive WatchEvent for 5 minuetes.");
    }
}

@test:Config {}
public function testConnectWithInvalidKey() returns error? {
    Listener|Error sftpServer = new ({
        protocol: SFTP,
        host: "localhost",
        port: 21213,
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            privateKey: {
                path: "tests/resources/sftp.wrong.private.key",
                password: "changeit"
            }
        },
        path: "/home/in",
        pollingInterval: 2,
        fileNamePattern: "(.*).txt"
    });

    if sftpServer is Error {
        test:assertEquals(sftpServer.message(), "Failed to initialize File server connector.");
    } else {
        test:assertFail("Non-error result when invalid key is used for creating a Listener.");
    }
}

@test:Config {}
public function testConnectWithInvalidKeyPath() returns error? {
    Listener|Error sftpServer = new ({
        protocol: SFTP,
        host: "localhost",
        port: 21213,
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            privateKey: {
                path: "tests/invalid_resources/sftp.private.key",
                password: "changeit"
            }
        },
        path: "/home/in",
        pollingInterval: 2,
        fileNamePattern: "(.*).txt"
    });

    if sftpServer is Error {
        test:assertEquals(sftpServer.message(), "Failed to initialize File server connector.");
    } else {
        test:assertFail("Non-error result when invalid key path is used for creating a Listener.");
    }
}

@test:Config {}
public function testConnectToSFTPServerWithFTPProtocol() returns error? {
    Listener|Error sftpServer = new ({
        protocol: FTP,
        host: "localhost",
        port: 21213,
        auth: {
            credentials: {username: "wso2", password: "wso2123"}
        },
        path: "/home/in",
        pollingInterval: 2,
        fileNamePattern: "(.*).txt"
    });

    if sftpServer is Error {
        test:assertEquals(sftpServer.message(), "Failed to initialize File server connector.");
    } else {
        test:assertFail("Non-error result when connecting to SFTP server via FTP is used for creating a Listener.");
    }
}

@test:Config {}
public function testConnectWithEmptyKey() returns error? {
    Listener|Error sftpServer = new ({
        protocol: SFTP,
        host: "localhost",
        port: 21213,
        auth: {
            credentials: {username: "wso2", password: "wso2123"}
        },
        path: "/home/in",
        pollingInterval: 2,
        fileNamePattern: "(.*).txt"
    });

    if sftpServer is Error {
        test:assertEquals(sftpServer.message(), "Failed to initialize File server connector.");
    } else {
        test:assertFail("Non-error result when no key config is provided when creating a Listener.");
    }
}

@test:Config {}
public function testConnectWithEmptyCredentials() returns error? {
    Listener|Error sftpServer = new ({
        protocol: SFTP,
        host: "localhost",
        port: 21213,
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            privateKey: {
                path: "tests/invalid_resources/sftp.private.key",
                password: "changeit"
            }
        },
        path: "/home/in",
        pollingInterval: 2,
        fileNamePattern: "(.*).txt"
    });

    if sftpServer is Error {
        test:assertEquals(sftpServer.message(), "Failed to initialize File server connector.");
    } else {
        test:assertFail("Non-error result when no credentials were provided when creating a Listener.");
    }
}

@test:Config {}
public function testConnectWithEmptyCredentialsAndKey() returns error? {
    Listener|Error sftpServer = new ({
        protocol: SFTP,
        host: "localhost",
        port: 21213,
        path: "/home/in",
        pollingInterval: 2,
        fileNamePattern: "(.*).txt"
    });

    if sftpServer is Error {
        test:assertEquals(sftpServer.message(), "Failed to initialize File server connector.");
    } else {
        test:assertFail("Non-error result when no auth config is provided when creating a Listener.");
    }
}
