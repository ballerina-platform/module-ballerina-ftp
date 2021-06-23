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
        auth: {basicAuth: {username: "wso2", password: "wso2123"}}
};

Client wrongClientEp = new(wrongConfig);

// Create the incorrect config for non-existing server
ClientConfiguration nonExistingServerConfig = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21218,
        auth: {basicAuth: {username: "wso2", password: "wso2123"}}
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
            log:printInfo("Received error: " + receivedError.message());
        } else {
            test:assertFail(msg = "Found a non-error response with a wrong URL");
        }
    } else {
       test:assertFail(msg = "Found unexpected response type" + str.message());
    }
}

@test:Config{
    dependsOn: [testAppendContent]
}
public function testAppendContentWithWrongUrl() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(appendFilePath, 7);

    var receivedError =  wrongClientEp -> append(filePath, bStream);
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

    var receivedError = wrongClientEp->put(newFilePath, bStream);

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
    var receivedError = wrongClientEp->isDirectory("/home/in");
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
    var receivedError = nonExistingServerClientEp->isDirectory("/home/in");
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
    var response1 = wrongClientEp->mkdir("/home/in/out");
    if(response1 is Error) {
        log:printInfo("Received error: " + response1.message());
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
    var receivedError = wrongClientEp->rename(existingName, newName);
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
    var receivedError = wrongClientEp->size(filePath);
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
    var receivedError = nonExistingServerClientEp->size(filePath);
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
    var receivedError = wrongClientEp->list("/home/in");
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
    var receivedError = nonExistingServerClientEp->list("/home/in");
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
    var receivedError = wrongClientEp->delete(filePath);
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
    var receivedError = wrongClientEp->rmdir("/home/in/test");
    if (receivedError is Error) {
        log:printInfo("Received error: " + receivedError.message());
    } else {
        test:assertFail(msg = "Found a non-error response with a wrong URL");
    }
}
