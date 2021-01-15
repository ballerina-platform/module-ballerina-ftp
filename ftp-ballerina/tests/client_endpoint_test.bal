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

import ballerina/io;
import ballerina/test;
import ballerina/log;
import ballerina/java;

string filePath = "/home/in/test1.txt";
string newFilePath = "/home/in/test2.txt";
string appendFilePath = "tests/resources/datafiles/file1.txt";
string putFilePath = "tests/resources/datafiles/file2.txt";

// Create the config to access mock FTP server
ClientEndpointConfig config = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21212,
        secureSocket: {basicAuth: {username: "wso2", password: "wso2123"}}
};

Client clientEP = new(config);

// Start mock FTP server
boolean startedServer = initServer();

function initServer() returns boolean {
    error? response = initFtpServer(config);
    return !(response is error);
}

@test:Config{}
public function testReadContent() {
    io:ReadableByteChannel|error response = clientEP -> get(filePath);
    if(response is io:ReadableByteChannel){
        io:ReadableCharacterChannel? characters = new io:ReadableCharacterChannel(response, "utf-8");
        if (characters is io:ReadableCharacterChannel) {
            string|error content = characters.read(100);
            if(content is string){
                log:print("Initial content in file: " + content);
                log:print("Executed Get operation");
            } else {
                log:printError("Error in retrieving content", err = content);
            }
            var closeResult = characters.close();
            if (closeResult is error) {
                log:printError("Error occurred while closing the channel", err = closeResult);
            }
        }
    } else {
        log:printError("Error in retrieving content", err = response);
    }
}

@test:Config{
    dependsOn: [testReadContent]
}
public function testAppendContent() {
    io:ReadableByteChannel|error byteChannel = io:openReadableFile(appendFilePath);
    if(byteChannel is io:ReadableByteChannel){
        error? response = clientEP -> append(filePath, byteChannel);
        if(response is error) {
            log:printError("Error in editing file", err = response);
        } else {
            log:print("Executed Append operation");
        }
    } else {
        log:printError("Error in reading input file", err = byteChannel);
    }
}

@test:Config{
    dependsOn: [testAppendContent]
}
public function testPutFileContent() {
    io:ReadableByteChannel|error byteChannelToPut = io:openReadableFile(putFilePath);

    if(byteChannelToPut is io:ReadableByteChannel){
        error? response = clientEP -> put(newFilePath, byteChannelToPut);
        if(response is error) {
            log:printError("Error in put operation", err = response);
        }
        log:print("Executed Put operation");
    } else {
        log:print("Error in reading input file");
    }
}

@test:Config{
    dependsOn: [testPutFileContent]
}
public function testPutTextContent() {
    string textToPut = "Sample text content";
    error? response = clientEP -> put(filePath, textToPut);
    if(response is error) {
        log:printError("Error in put operation", err = response);
    } else {
        log:print("Executed Put operation");
    }
}

@test:Config{
    dependsOn: [testPutTextContent]
}
public function testPutJsonContent() {
    json jsonToPut = { name: "Anne", age: 20 };
    error? response = clientEP -> put(filePath, jsonToPut);
    if(response is error) {
        log:printError("Error in put operation", err = response);
    } else {
        log:print("Executed Put operation");
    }
}

@test:Config{
    dependsOn: [testPutJsonContent]
}
public function testPutXMLContent() {
    xml xmlToPut = xml `<note>
                              <to>A</to>
                              <from>B</from>
                              <heading>Memo</heading>
                              <body>Memo content</body>
                          </note>`;
    error? response = clientEP -> put(filePath, xmlToPut);
    if(response is error) {
        log:printError("Error in put operation", err = response);
    } else {
        log:print("Executed Put operation");
    }
}

@test:Config{
    dependsOn: [testPutXMLContent]
}
public function testIsDirectory() {
    boolean|error response = clientEP -> isDirectory("/home/in");
    if(response is boolean) {
        log:print("Is directory: " + response.toString());
        log:print("Executed Is directory operation");
    } else {
        log:printError("Error in reading isDirectory", err = response);
    }
}

@test:Config{
    dependsOn: [testIsDirectory]
}
public function testCreateDirectory() {
    error? response = clientEP -> mkdir("/home/in/out");
    if(response is error) {
        log:printError("Error in creating directory", err = response);
    } else {
        log:print("Executed Mkdir operation");
    }
}

@test:Config{
    dependsOn: [testCreateDirectory]
}
public function testRenameDirectory() {
    string existingName = "/home/in/out";
    string newName = "/home/in/test";
    error? response = clientEP -> rename(existingName, newName);
    if(response is error) {
        log:printError("Error in renaming directory", err = response);
    } else {
        log:print("Executed Rename operation");
    }
}

@test:Config{
    dependsOn: [testRenameDirectory]
}
public function testGetFileSize() {
    int|error response = clientEP -> size(filePath);
    if(response is int){
        log:print("Size: " + response.toString());
        log:print("Executed size operation.");
    } else {
        log:printError("Error in getting file size", err = response);
    }
}

@test:Config{
    dependsOn: [testGetFileSize]
}
public function testListFiles() {
    FileInfo[]|error response = clientEP -> list("/home/in");
    if (response is FileInfo[]) {
        log:print("List of files/directories: ");
        foreach var fileInfo in response {
            log:print(fileInfo.toString());
        }
        log:print("Executed List operation");
    } else {
        log:printError("Error in getting file list", err = response);
    }
}

@test:Config{
    dependsOn: [testListFiles]
}
public function testDeleteFile() {
    error? response = clientEP -> delete(filePath);
    if(response is error) {
        log:printError("Error in deleting file", err = response);
    } else {
        log:print("Executed Delete operation");
    }
}

@test:Config{
    dependsOn: [testDeleteFile]
}
public function testRemoveDirectory() {
    error? response = clientEP -> rmdir("/home/in/test");
    if(response is error) {
        log:printError("Error in removing directory", err = response);
    } else {
        log:print("Executed Rmdir operation");
    }
}

@test:AfterSuite{}
public function stopServer() returns error? {
    error? response = stopFtpServer();
}

function initFtpServer(map<anydata> config) returns error? = @java:Method{
    name: "initServer",
    'class: "org.ballerinalang.stdlib.ftp.testutils.mockServerUtils.MockFTPServer"
} external;

function stopFtpServer() returns () = @java:Method{
    name: "stopServer",
    'class: "org.ballerinalang.stdlib.ftp.testutils.mockServerUtils.MockFTPServer"
} external;
