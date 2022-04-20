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

import ballerina/lang.runtime as runtime;
import ballerina/log;
import ballerina/test;
import ballerina/io;

int addedFileCount = 0;
FileInfo? anonServerAddedFileInfo = ();
int deletedFileCount = 0;
boolean watchEventReceived = false;
string deletedFilesNames = "";
FileInfo[] fileInfos = [];
isolated string addedFilename = "";

ListenerConfiguration remoteServerConfiguration = {
    protocol: FTP,
    host: "127.0.0.1",
    auth: {
        credentials: {
            username: "wso2",
            password: "wso2123"
        }
    },
    port: 21212,
    path: "/home/in",
    pollingInterval: 2,
    fileNamePattern: "(.*).txt"
};

Service remoteServerService = service object {
    remote function onFileChange(Caller caller, WatchEvent & readonly event) {
        addedFileCount = event.addedFiles.length();
        deletedFileCount = event.deletedFiles.length();
        watchEventReceived = true;

        foreach FileInfo addedFile in event.addedFiles {
            log:printInfo("Added file path: " + addedFile.path);
        }
        foreach string deletedFile in event.deletedFiles {
            log:printInfo("Deleted file path: " + deletedFile);
        }
    }
};

ListenerConfiguration anonymousRemoteServerConfig = {
    protocol: FTP,
    host: "127.0.0.1",
    auth: {
        credentials: {
            username: "anonymous",
            password: "anything"
        }
    },
    port: 21210,
    path: "/home/in",
    pollingInterval: 2,
    fileNamePattern: "(.*).txt"
};

Service anonymousRemoteServerService = service object {
    remote function onFileChange(WatchEvent & readonly event) {
        if event.addedFiles.length() == 1 && anonServerAddedFileInfo == () {
            anonServerAddedFileInfo = event.addedFiles[0];
        } else {
            anonServerAddedFileInfo = ();
        }
    }
};

@test:Config {}
public function testAnonServerAddedFile() {
    int timeoutInSeconds = 300;
    while timeoutInSeconds > 0 {
        if anonServerAddedFileInfo is FileInfo {
            test:assertEquals((<FileInfo>anonServerAddedFileInfo).path,
                "ftp://anonymous:anything@127.0.0.1:21210/home/in/test1.txt");
            test:assertEquals((<FileInfo>anonServerAddedFileInfo).size, 12);
            test:assertTrue((<FileInfo>anonServerAddedFileInfo).lastModifiedTimestamp > 0);
            test:assertEquals((<FileInfo>anonServerAddedFileInfo).name, "test1.txt");
            test:assertEquals((<FileInfo>anonServerAddedFileInfo).isFolder, false);
            test:assertEquals((<FileInfo>anonServerAddedFileInfo).isFile, true);
            test:assertEquals((<FileInfo>anonServerAddedFileInfo).pathDecoded, "/home/in/test1.txt");
            test:assertEquals((<FileInfo>anonServerAddedFileInfo).extension, "txt");
            test:assertEquals((<FileInfo>anonServerAddedFileInfo).publicURIString,
                "ftp://anonymous:***@127.0.0.1:21210/home/in/test1.txt");
            test:assertEquals((<FileInfo>anonServerAddedFileInfo).fileType, "file");
            test:assertEquals((<FileInfo>anonServerAddedFileInfo).isAttached, true);
            test:assertEquals((<FileInfo>anonServerAddedFileInfo).isContentOpen, false);
            test:assertEquals((<FileInfo>anonServerAddedFileInfo).isExecutable, false);
            test:assertEquals((<FileInfo>anonServerAddedFileInfo).isHidden, false);
            test:assertEquals((<FileInfo>anonServerAddedFileInfo).isReadable, true);
            test:assertEquals((<FileInfo>anonServerAddedFileInfo).isWritable, true);
            test:assertEquals((<FileInfo>anonServerAddedFileInfo).depth, 4);
            test:assertEquals((<FileInfo>anonServerAddedFileInfo).scheme, "ftp");
            test:assertEquals((<FileInfo>anonServerAddedFileInfo).uri,
                "//anonymous:anything@127.0.0.1:21210/home/in/test1.txt");
            test:assertEquals((<FileInfo>anonServerAddedFileInfo).rootURI,
                "ftp://anonymous:anything@127.0.0.1:21210/");
            test:assertEquals((<FileInfo>anonServerAddedFileInfo).friendlyURI,
                "ftp://anonymous:***@127.0.0.1:21210/home/in/test1.txt");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds -= 1;
        }
    }
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the `FileInfo` for 5 minuetes.");
    }
}

@test:Config {
}
public function testAddedFileCount() {
    int timeoutInSeconds = 300;
    // Test fails in 5 minutes if failed to receive watchEvent
    while timeoutInSeconds > 0 {
        if watchEventReceived {
            log:printInfo("Added file count: " + addedFileCount.toString());
            test:assertEquals(4, addedFileCount);
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
public function testFtpServerDeregistration() returns error? {
    Listener detachFtpServer = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {
            credentials: {
                username: "wso2",
                password: "wso2123"
            }
        },
        port: 21212,
        path: "/home/in",
        pollingInterval: 2,
        fileNamePattern: "(.*).txt"
    });

    Service detachService = service object {
        remote function onFileChange(WatchEvent & readonly event) {
        }
    };
    error? result1 = detachFtpServer.attach(detachService, "remote-server");
    if result1 is error {
        test:assertFail("Failed to attach to the FTP server: " + result1.message());
    } else {
        error? result2 = detachFtpServer.detach(detachService);
        if result2 is error {
            test:assertFail("Failed to detach from the FTP server: " + result2.message());
        }
    }
}

@test:Config {}
public function testServerRegisterFailureEmptyPassword() returns error? {
    Listener|Error emptyPasswordServer = new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {
            credentials: {
                username: "wso2",
                password: ""
            }
        },
        port: 21212,
        path: "/home/in",
        pollingInterval: 2,
        fileNamePattern: "(.*).txt"
    });

    if emptyPasswordServer is Error {
        test:assertEquals(emptyPasswordServer.message(), "Failed to initialize File server connector.");
    } else {
        test:assertFail("Non-error result when empty password is used for creating a Listener.");
    }
}

@test:Config {}
public function testServerRegisterFailureEmptyUsername() returns error? {
    Listener|Error emptyUsernameServer = new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {
            credentials: {
                username: "",
                password: "wso2"
            }
        },
        port: 21212,
        path: "/home/in",
        pollingInterval: 2,
        fileNamePattern: "(.*).txt"
    });

    if emptyUsernameServer is Error {
        test:assertEquals(emptyUsernameServer.message(), "Username cannot be empty");
    } else {
        test:assertFail("Non-error result when empty username is used for creating a Listener.");
    }
}

@test:Config {}
public function testServerRegisterFailureInvalidUsername() returns error? {
    Listener|Error invalidUsernameServer = new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {
            credentials: {
                username: "ballerina",
                password: "wso2"
            }
        },
        port: 21212,
        path: "/home/in",
        pollingInterval: 2,
        fileNamePattern: "(.*).txt"
    });

    if invalidUsernameServer is Error {
        test:assertEquals(invalidUsernameServer.message(), "Failed to initialize File server connector.");
    } else {
        test:assertFail("Non-error result when invalid username is used for creating a Listener.");
    }
}

@test:Config {}
public function testServerRegisterFailureInvalidPassword() returns error? {
    Listener|Error invalidPasswordServer = new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {
            credentials: {
                username: "wso2",
                password: "ballerina"
            }
        },
        port: 21212,
        path: "/home/in",
        pollingInterval: 2,
        fileNamePattern: "(.*).txt"
    });

    if invalidPasswordServer is Error {
        test:assertEquals(invalidPasswordServer.message(), "Failed to initialize File server connector.");
    } else {
        test:assertFail("Non-error result when invalid password is used for creating a Listener.");
    }
}

@test:Config {}
public function testConnectToInvalidUrl() returns error? {
    Listener|Error invalidUrlServer = new ({
        protocol: FTP,
        host: "localhost",
        port: 21218,
        auth: {
            credentials: {username: "wso2", password: "wso2123"}
        },
        path: "/home/in",
        pollingInterval: 2,
        fileNamePattern: "(.*).txt"
    });

    if invalidUrlServer is Error {
        test:assertEquals(invalidUrlServer.message(), "Failed to initialize File server connector.");
    } else {
        test:assertFail("Non-error result when trying to connect to an invalid url.");
    }
}

@test:Config {
    dependsOn: [testDeleteFile]
}
public function testMutableWatchEvent() returns error? {
    Service ftpService = service object {
        remote function onFileChange(WatchEvent event) {
            event.addedFiles.forEach(function (FileInfo fInfo) {
                fInfo.name = "overriden_name";
            });
            fileInfos = event.addedFiles;
        }
    };
    Listener ftpListener = check new ({
        protocol: FTP,
        host: "localhost",
        port: 21212,
        auth: {
            credentials: {username: "wso2", password: "wso2123"}
        },
        path: "/home/in",
        pollingInterval: 2,
        fileNamePattern: "(.*).txt"
    });
    check ftpListener.attach(ftpService);
    check ftpListener.'start();
    runtime:registerListener(ftpListener);

    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>clientEp)->put("/home/in/mutable/test1.txt", bStream, compressionType = ZIP);
    runtime:sleep(2);

    runtime:deregisterListener(ftpListener);
    check ftpListener.gracefulStop();

    test:assertTrue(fileInfos.length() != 0);
    fileInfos.forEach(function (FileInfo fInfo) {
        test:assertEquals(fInfo.name, "overriden_name");
    });
}

@test:Config {
    dependsOn: []
}
public function testValidateDeletedFilesFromListener() returns error? {
    Service ftpService = service object {
        remote function onFileChange(WatchEvent event) {
            event.deletedFiles.forEach(function (string deletedFile) {
                deletedFilesNames += deletedFile;
            });
        }
    };
    Listener ftpListener = check new ({
        protocol: FTP,
        host: "localhost",
        port: 21212,
        auth: {
            credentials: {username: "wso2", password: "wso2123"}
        },
        path: "/home/in",
        pollingInterval: 1,
        fileNamePattern: "(.*).txt"
    });
    check ftpListener.attach(ftpService);
    check ftpListener.'start();
    runtime:registerListener(ftpListener);

    stream<io:Block, io:Error?> bStream1 = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>clientEp)->put("/home/in/deleteFile1.txt", bStream1);
    stream<io:Block, io:Error?> bStream2 = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>clientEp)->put("/home/in/deleteFile2.txt", bStream2);
    stream<io:Block, io:Error?> bStream3 = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>clientEp)->put("/home/in/deleteFile3.txt", bStream3);

    runtime:sleep(2);
    check (<Client>clientEp)->delete("/home/in/deleteFile1.txt");
    check (<Client>clientEp)->delete("/home/in/deleteFile2.txt");
    check (<Client>clientEp)->delete("/home/in/deleteFile3.txt");
    runtime:sleep(5);

    runtime:deregisterListener(ftpListener);
    check ftpListener.gracefulStop();
    foreach int i in 1...3 {
        test:assertTrue(deletedFilesNames.includes(string`/home/in/deleteFile${i}.txt`));
    }
}

@test:Config {}
public function testIsolatedService() returns error? {
    Service ftpService = service object {
        remote function onFileChange(WatchEvent event) {
            event.addedFiles.forEach(function (FileInfo fileInfo) {
                lock {
                    addedFilename = fileInfo.name;
                }
            });
        }
    };
    Listener ftpListener = check new ({
        protocol: FTP,
        host: "localhost",
        port: 21212,
        auth: {
            credentials: {username: "wso2", password: "wso2123"}
        },
        path: "/home/in",
        pollingInterval: 1,
        fileNamePattern: "(.*).isolated"
    });
    check ftpListener.attach(ftpService);
    check ftpListener.'start();
    runtime:registerListener(ftpListener);

    stream<io:Block, io:Error?> bStream1 = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>clientEp)->put("/home/in/isolatedTestFile.isolated", bStream1);
    runtime:sleep(5);

    runtime:deregisterListener(ftpListener);
    check ftpListener.gracefulStop();
    check (<Client>clientEp)->delete("/home/in/isolatedTestFile.isolated");
    lock {
        test:assertEquals(addedFilename, "isolatedTestFile.isolated");
    }
}
