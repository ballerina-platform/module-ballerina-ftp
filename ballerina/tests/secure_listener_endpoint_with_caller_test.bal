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
    pollingInterval: 2,
    fileNamePattern: "(.*).caller"
};

Service callerService = service object {
    string addedFilepath = "";

    remote function onFileChange(WatchEvent & readonly event, Caller caller) returns error? {
        event.addedFiles.forEach(function (FileInfo fileInfo) {
            self.addedFilepath = fileInfo.path;
        });
        if self.addedFilepath.endsWith("/put.caller") {
            stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
            check caller->put("/put2.caller", bStream);
        } else if self.addedFilepath.endsWith("/append.caller") {
            stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(appendFilePath, 7);
            check caller->append("/append.caller", bStream);
        } else if self.addedFilepath.endsWith("/rename.caller") {
            check caller->rename("/rename.caller", "/rename2.caller");
        } else if self.addedFilepath.endsWith("/delete.caller") {
            check caller->delete("/delete.caller");
        } else if self.addedFilepath.endsWith("/list.caller") {
             fileList = check caller->list("/");
        } else if self.addedFilepath.endsWith("/get.caller") {
            stream<io:Block, io:Error?> fileStream = check caller->get("/get.caller");
            fileGetContentCorrect = check matchStreamContent(fileStream, "Put content");
        } else if self.addedFilepath.endsWith("/mkdir.caller") {
            check caller->mkdir("/callerDir");
        } else if self.addedFilepath.endsWith("/rmdir.caller") {
            check caller->rmdir("/callerDir");
        } else if self.addedFilepath.endsWith("/size.caller") {
            fileSize = check caller->size("/size.caller");
        } else if self.addedFilepath.endsWith("/isdirectory.caller") {
            isDir = check caller->isDirectory("/callerDir");
        }
    }
};

@test:Config {}
public function testFilePutWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/put.caller", bStream);
    runtime:sleep(3);
    stream<byte[] & readonly, io:Error?> str = check (<Client>sftpClientEp)->get("/put2.caller");
    test:assertTrue(check matchStreamContent(str, "Put content"));
    check str.close();
    check (<Client>sftpClientEp)->delete("/put.caller");
    check (<Client>sftpClientEp)->delete("/put2.caller");
}

@test:Config {}
public function testFileAppendWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/append.caller", bStream);
    runtime:sleep(3);
    stream<byte[] & readonly, io:Error?> str = check (<Client>sftpClientEp)->get("/append.caller");
    test:assertTrue(check matchStreamContent(str, "Put contentAppend content"));
    check str.close();
    check (<Client>sftpClientEp)->delete("/append.caller");
}

@test:Config {}
public function testFileRenameWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/rename.caller", bStream);
    runtime:sleep(3);
    stream<byte[] & readonly, io:Error?> str = check (<Client>sftpClientEp)->get("/rename2.caller");
    test:assertTrue(check matchStreamContent(str, "Put content"));
    check str.close();
    check (<Client>sftpClientEp)->delete("/rename2.caller");
}

@test:Config {}
public function testFileDeleteWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/delete.caller", bStream);
    runtime:sleep(3);
    stream<byte[] & readonly, io:Error?>|Error result = (<Client>sftpClientEp)->get("/delete.caller");
    if result is Error {
        test:assertTrue(result.message().endsWith("delete.caller not found"));
    } else {
        test:assertFail("Caller delete operation has failed");
    }
}

@test:Config {}
public function testFileListWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/list.caller", bStream);
    runtime:sleep(3);
    test:assertEquals(fileList[0].path, "/list.caller");
    check (<Client>sftpClientEp)->delete("/list.caller");
}

@test:Config {}
public function testFileGetWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/get.caller", bStream);
    runtime:sleep(3);
    test:assertTrue(fileGetContentCorrect);
    check (<Client>sftpClientEp)->delete("/get.caller");
}

@test:Config {}
public function testMkDirWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/mkdir.caller", bStream);
    runtime:sleep(3);
    boolean isDir = check (<Client>sftpClientEp)->isDirectory("/callerDir");
    test:assertTrue(isDir);
    check (<Client>sftpClientEp)->delete("/mkdir.caller");
}

@test:Config {
    dependsOn: [testMkDirWithCaller]
}
public function testIsDirectoryWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/isdirectory.caller", bStream);
    runtime:sleep(3);
    test:assertTrue(isDir);
    check (<Client>sftpClientEp)->delete("/isdirectory.caller");
}

@test:Config {
    dependsOn: [testIsDirectoryWithCaller]
}
public function testRmDirWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/rmdir.caller", bStream);
    runtime:sleep(3);
    boolean|Error result = (<Client>sftpClientEp)->isDirectory("/callerDir");
    if result is Error {
        test:assertEquals(result.message(), "/callerDir does not exists to check if it is a directory.");
    } else {
        test:assertFail("Expected an error");
    }
    check (<Client>sftpClientEp)->delete("/rmdir.caller");
}

@test:Config {}
public function testFileSizeWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/size.caller", bStream);
    runtime:sleep(3);
    test:assertEquals(fileSize, 11);
    check (<Client>sftpClientEp)->delete("/size.caller");
}
