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

import ballerina/lang.runtime as runtime;
import ballerina/io;
import ballerina/test;

FileInfo[] fileList = [];
boolean fileGetContentCorrect = false;
int fileSize = 0;
boolean isDir = false;
string filename = "mutableWatchEvent.caller";
string addedFile = "";

ListenerConfiguration callerListenerConfig = {
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
    pollingInterval: 1,
    path: "/in",
    fileNamePattern: "(.*).caller"
};

Service callerService = service object {

    remote function onFileChange(WatchEvent & readonly event, Caller caller) returns error? {
        foreach FileInfo fileInfo in event.addedFiles {
            string addedFilepath = fileInfo.path;
            if addedFilepath.endsWith("/put.caller") {
                stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
                check caller->put("/out/put2.caller", bStream);
                check caller->rename("/in/put.caller", "/out/put.caller");
            } else if addedFilepath.endsWith("/append.caller") {
                stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(appendFilePath, 7);
                check caller->append("/in/append.caller", bStream);
                check caller->rename("/in/append.caller", "/out/append.caller");
            } else if addedFilepath.endsWith("/rename.caller") {
                check caller->rename("/in/rename.caller", "/out/rename.caller");
            } else if addedFilepath.endsWith("/delete.caller") {
                check caller->delete("/in/delete.caller");
            } else if addedFilepath.endsWith("/list.caller") {
                fileList = check caller->list("/in");
                check caller->rename("/in/list.caller", "/out/list.caller");
            } else if addedFilepath.endsWith("/get.caller") {
                stream<io:Block, io:Error?> fileStream = check caller->get("/in/get.caller");
                fileGetContentCorrect = check matchStreamContent(fileStream, "Put content");
                check caller->rename("/in/get.caller", "/out/get.caller");
            } else if addedFilepath.endsWith("/mkdir.caller") {
                check caller->mkdir("/out/callerDir");
                check caller->rename("/in/mkdir.caller", "/out/mkdir.caller");
            } else if addedFilepath.endsWith("/rmdir.caller") {
                check caller->rmdir("/out/callerDir");
                check caller->rename("/in/rmdir.caller", "/out/rmdir.caller");
            } else if addedFilepath.endsWith("/size.caller") {
                fileSize = check caller->size("/in/size.caller");
                check caller->rename("/in/size.caller", "/out/size.caller");
            } else if addedFilepath.endsWith("/isdirectory.caller") {
                isDir = check caller->isDirectory("/out/callerDir");
                check caller->rename("/in/isdirectory.caller", "/out/isdirectory.caller");
            }
        }

    }
};

@test:Config {}
public function testFilePutWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/in/put.caller", bStream);
    runtime:sleep(3);
    stream<byte[] & readonly, io:Error?> str = check (<Client>sftpClientEp)->get("/out/put2.caller");
    test:assertTrue(check matchStreamContent(str, "Put content"));
    check str.close();
    check (<Client>sftpClientEp)->delete("/out/put2.caller");
    check (<Client>sftpClientEp)->delete("/out/put.caller");
}

@test:Config {}
public function testFileAppendWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/in/append.caller", bStream);
    runtime:sleep(3);
    stream<byte[] & readonly, io:Error?> str = check (<Client>sftpClientEp)->get("/out/append.caller");
    test:assertTrue(check matchStreamContent(str, "Put contentAppend content"));
    check str.close();
    check (<Client>sftpClientEp)->delete("/out/append.caller");
}

@test:Config {}
public function testFileRenameWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/in/rename.caller", bStream);
    runtime:sleep(3);
    stream<byte[] & readonly, io:Error?> str = check (<Client>sftpClientEp)->get("/out/rename.caller");
    test:assertTrue(check matchStreamContent(str, "Put content"));
    check str.close();
    check (<Client>sftpClientEp)->delete("/out/rename.caller");
}

@test:Config {}
public function testFileDeleteWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/in/delete.caller", bStream);
    runtime:sleep(3);
    stream<byte[] & readonly, io:Error?>|Error result = (<Client>sftpClientEp)->get("/in/delete.caller");
    if result is Error {
        test:assertTrue(result.message().endsWith("delete.caller not found"));
    } else {
        test:assertFail("Caller delete operation has failed");
    }
}

@test:Config {}
public function testFileListWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/in/list.caller", bStream);
    runtime:sleep(3);
    test:assertEquals(fileList.length(), 1);
    test:assertEquals(fileList[0].path, "/in/list.caller");
    check (<Client>sftpClientEp)->delete("/out/list.caller");
}

@test:Config {}
public function testFileGetWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/in/get.caller", bStream);
    runtime:sleep(3);
    test:assertTrue(fileGetContentCorrect);
    check (<Client>sftpClientEp)->delete("/out/get.caller");
}

@test:Config {}
public function testMkDirWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/in/mkdir.caller", bStream);
    runtime:sleep(3);
    boolean isDir = check (<Client>sftpClientEp)->isDirectory("/out/callerDir");
    test:assertTrue(isDir);
    check (<Client>sftpClientEp)->delete("/out/mkdir.caller");
}

@test:Config {
    dependsOn: [testMkDirWithCaller]
}
public function testIsDirectoryWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/in/isdirectory.caller", bStream);
    runtime:sleep(3);
    test:assertTrue(isDir);
    check (<Client>sftpClientEp)->delete("/out/isdirectory.caller");
}

@test:Config {
    dependsOn: [testIsDirectoryWithCaller]
}
public function testRmDirWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/in/rmdir.caller", bStream);
    runtime:sleep(3);
    boolean|Error result = (<Client>sftpClientEp)->isDirectory("/out/callerDir");
    if result is Error {
        test:assertEquals(result.message(), "/out/callerDir does not exists to check if it is a directory.");
    } else {
        test:assertFail("Expected an error");
    }
    check (<Client>sftpClientEp)->delete("/out/rmdir.caller");
}

@test:Config {}
public function testFileSizeWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/in/size.caller", bStream);
    runtime:sleep(3);
    test:assertEquals(fileSize, 11);
    check (<Client>sftpClientEp)->delete("/out/size.caller");
}

@test:Config {
    dependsOn: [testSecureAddedFileCount]
}
public function testMutableWatchEventWithCaller() returns error? {
    Service watchEventService = service object {
        remote function onFileChange(WatchEvent event, Caller caller) returns error? {
            event.addedFiles.forEach(function (FileInfo fileInfo) {
                if fileInfo.name == filename {
                    addedFile = filename;
                }
            });
        }
    };
    Listener 'listener = check new (callerListenerConfig);
    check 'listener.attach(watchEventService);
    check 'listener.start();
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/in/" + filename, bStream);
    runtime:sleep(3);
    test:assertEquals(addedFile, filename);
    check (<Client>sftpClientEp)->delete("/in/" + filename);
}

boolean fileMoved = false;
boolean fileCopied = false;
boolean fileExists = false;

@test:Config {
    dependsOn: [testMutableWatchEventWithCaller]
}
public function testFileMoveWithCaller() returns error? {
    string sourceName = "moveSource.caller";
    string destName = "moveDestination.txt";
    Service moveService = service object {
        remote function onFileChange(WatchEvent event, Caller caller) returns error? {
            foreach FileInfo fileInfo in event.addedFiles {
                if fileInfo.name == sourceName {
                    check caller->move("/in/" + sourceName, "/in/" + destName);
                    fileMoved = true;
                }
            }
        }
    };
    Listener 'listener = check new (callerListenerConfig);
    check 'listener.attach(moveService);
    check 'listener.start();
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/in/" + sourceName, bStream);
    runtime:sleep(3);
    test:assertTrue(fileMoved);
    check (<Client>sftpClientEp)->delete("/in/" + destName);
}

@test:Config {
    dependsOn: [testFileMoveWithCaller]
}
public function testFileCopyWithCaller() returns error? {
    string sourceName = "copySource.caller";
    string destName = "copyDestination.txt";
    Service copyService = service object {
        remote function onFileChange(WatchEvent event, Caller caller) returns error? {
            foreach FileInfo fileInfo in event.addedFiles {
                if fileInfo.name == sourceName {
                    check caller->copy("/in/" + sourceName, "/in/" + destName);
                    fileCopied = true;
                }
            }
        }
    };
    Listener 'listener = check new (callerListenerConfig);
    check 'listener.attach(copyService);
    check 'listener.start();
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/in/" + sourceName, bStream);
    runtime:sleep(3);
    test:assertTrue(fileCopied);
    check (<Client>sftpClientEp)->delete("/in/" + sourceName);
    check (<Client>sftpClientEp)->delete("/in/" + destName);
}

@test:Config {
    dependsOn: [testFileCopyWithCaller]
}
public function testFileExistsWithCaller() returns error? {
    string checkName = "existsCheck.caller";
    Service existsService = service object {
        remote function onFileChange(WatchEvent event, Caller caller) returns error? {
            foreach FileInfo fileInfo in event.addedFiles {
                if fileInfo.name == checkName {
                    fileExists = check caller->exists("/in/" + checkName);
                }
            }
        }
    };
    Listener 'listener = check new (callerListenerConfig);
    check 'listener.attach(existsService);
    check 'listener.start();
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/in/" + checkName, bStream);
    runtime:sleep(3);
    test:assertTrue(fileExists);
    check (<Client>sftpClientEp)->delete("/in/" + checkName);
}
