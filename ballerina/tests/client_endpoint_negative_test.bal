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

import ballerina/io;
import ballerina/test;
import ballerina/log;

// Create the incorrect config with invalid host
ClientConfiguration wrongConfig = {
        protocol: FTP,
        host: "#!@$%^&*(_+",
        port: 21212,
        auth: {credentials: {username: "wso2", password: "wso2123"}}
};

Client wrongClientEp = new(wrongConfig);

// Create the incorrect config for non-existing server
ClientConfiguration nonExistingServerConfig = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21218,
        auth: {credentials: {username: "wso2", password: "wso2123"}}
};

Client nonExistingServerClientEp = new(nonExistingServerConfig);

@test:Config{
    dependsOn: [testReadBlockNonFittingContent]
}
public function testReadWithWrongUrl() returns error? {
    stream<byte[] & readonly, io:Error?>|Error str = wrongClientEp->get(filePath, 6);
    if (str is stream<byte[] & readonly, io:Error?>) {
        var receivedError = trap str.next();
        if (receivedError is error) {
            test:assertFail(msg = "Found unexpected response type" + receivedError.message());
        } else {
            test:assertFail(msg = "Found a non-error response with a wrong URL");
        }
    } else {
       log:printInfo("Received error: " + str.message());
    }
}

@test:Config{
    dependsOn: [testReadBlockNonFittingContent]
}
public function testReadNonExistingFile() returns error? {
    stream<byte[] & readonly, io:Error?>|Error str = clientEp->get("/home/in/nonexisting.txt", 6);
    if (str is stream<byte[] & readonly, io:Error?>) {
        var receivedError = trap str.next();
        if (receivedError is error) {
            test:assertFail(msg = "Found unexpected response type" + receivedError.message());
        } else {
            test:assertFail(msg = "Found a non-error response from a non-existing file path");
        }
    } else {
       log:printInfo("Received error: " + str.message());
    }
}

@test:Config{
    dependsOn: [testAppendContent]
}
public function testAppendContentWithWrongUrl() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(appendFilePath, 7);

    Error? receivedError =  wrongClientEp->append(filePath, bStream);
    if (receivedError is Error) {
        log:printInfo("Received error: " + receivedError.message());
    } else {
        test:assertFail(msg = "Found a non-error response with a wrong URL");
    }
}

@test:Config{
    dependsOn: [testPutFileContent]
}
public function testPutFileContentWithWrongUrl() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);

    Error? receivedError = wrongClientEp->put(newFilePath, bStream);

    if (receivedError is Error) {
        log:printInfo("Received error: " + receivedError.message());
    } else {
        test:assertFail(msg = "Found a non-error response with a wrong URL");
    }
}

@test:Config{
    dependsOn: [testIsDirectory]
}
public function testIsDirectoryWithWrongUrl() {
    boolean|Error receivedError = wrongClientEp->isDirectory("/home/in");
    if (receivedError is Error) {
        log:printInfo("Received error: " + receivedError.message());
    } else {
        test:assertFail(msg = "Found a non-error response with a wrong URL");
    }
}

@test:Config{
    dependsOn: [testIsDirectory]
}
public function testIsDirectoryWithNonExistingServer() {
    boolean|Error receivedError = nonExistingServerClientEp->isDirectory("/home/in");
    if (receivedError is Error) {
        log:printInfo("Received error for non-existing server: " + receivedError.message());
    } else {
        test:assertFail(msg = "Found a non-error response with a wrong URL");
    }
}

@test:Config{
    dependsOn: [testCreateDirectory]
}
public function testCreateDirectoryWithWrongUrl() {
    Error? response = wrongClientEp->mkdir("/home/in/out");
    if (response is Error) {
        log:printInfo("Received error: " + response.message());
    } else {
        test:assertFail(msg = "Found a non-error response with a wrong URL");
    }
}

@test:Config{
    dependsOn: [testCreateDirectoryWithWrongUrl]
}
public function testCreateDirectoryWithNonExistingServer() {
    Error? receivedError = nonExistingServerClientEp->mkdir("/home/in/out");
    if (receivedError is Error) {
        log:printInfo("Received error for non-existing server: " + receivedError.message());
    } else {
        test:assertFail(msg = "Found a non-error response with a wrong URL");
    }
}

@test:Config{
    dependsOn: [testRenameDirectory]
}
public function testRenameDirectoryWithWrongUrl() {
    string existingName = "/home/in/out";
    string newName = "/home/in/test";
    Error? receivedError = wrongClientEp->rename(existingName, newName);
    if (receivedError is Error) {
        log:printInfo("Received error: " + receivedError.message());
    } else {
        test:assertFail(msg = "Found a non-error response with a wrong URL");
    }
}

@test:Config{
    dependsOn: [testGetFileSize]
}
public function testGetFileSizeWithWrongUrl() {
    int|Error receivedError = wrongClientEp->size(filePath);
    if (receivedError is Error) {
        log:printInfo("Received error: " + receivedError.message());
    } else {
        test:assertFail(msg = "Found a non-error response with a wrong URL");
    }
}

@test:Config{
    dependsOn: [testGetFileSize]
}
public function testGetFileSizeWithNonExistingServer() {
    int|Error receivedError = nonExistingServerClientEp->size(filePath);
    if (receivedError is Error) {
        log:printInfo("Received error for non-existing server: " + receivedError.message());
    } else {
        test:assertFail(msg = "Found a non-error response with a wrong URL");
    }
}

@test:Config{
    dependsOn: [testListFiles]
}
public function testListFilesWithWrongUrl() {
    FileInfo[]|Error receivedError = wrongClientEp->list("/home/in");
    if (receivedError is Error) {
        log:printInfo("Received error: " + receivedError.message());
    } else {
        test:assertFail(msg = "Found a non-error response with a wrong URL");
    }
}

@test:Config{
    dependsOn: [testListFiles]
}
public function testListFilesWithNonExistingServer() {
    FileInfo[]|Error receivedError = nonExistingServerClientEp->list("/home/in");
    if (receivedError is Error) {
        log:printInfo("Received error for non-existing server: " + receivedError.message());
    } else {
        test:assertFail(msg = "Found a non-error response with a wrong URL");
    }
}

@test:Config{
    dependsOn: [testDeleteFile]
}
public function testDeleteFileWithWrongUrl() returns error? {
    Error? receivedError = wrongClientEp->delete(filePath);
    if (receivedError is Error) {
        log:printInfo("Received error: " + receivedError.message());
    } else {
        test:assertFail(msg = "Found a non-error response with a wrong URL");
    }
}

@test:Config{
    dependsOn: [testRemoveDirectory]
}
public function testRemoveDirectoryWithWrongUrl() {
    Error? receivedError = wrongClientEp->rmdir("/home/in/test");
    if (receivedError is Error) {
        log:printInfo("Received error: " + receivedError.message());
    } else {
        test:assertFail(msg = "Found a non-error response with a wrong URL");
    }
}
