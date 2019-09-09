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
import ballerinax/java;


string filePath = "/home/in/file1.txt";
string appendFilePath = "src/ftp/tests/resources/file1.txt";
string putFilePath = "src/ftp/tests/resources/file2.txt";

ClientEndpointConfig config = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21212,
        secureSocket: {basicAuth: {username: "wso2", password: "wso2123"}}
};

boolean startedServer = initServer();
Client clientEP = new(config);


function initServer() returns boolean {
    map<anydata>|error configMap = map<anydata>.constructFrom(config);
    if(configMap is map<anydata>){
        error? response = initFtpServer(configMap);
        return true;
    }
    return false;
}

@test:Config{
}
public function testReadContent() {
    io:ReadableByteChannel|error response = clientEP -> get(filePath);
    if(response is io:ReadableByteChannel){
        io:ReadableCharacterChannel? characters = new io:ReadableCharacterChannel(response, "utf-8");
        if (characters is io:ReadableCharacterChannel) {
            string|error content = characters.read(20);
            if(content is string){
                log:printInfo("Initial content in file: " + content);
            } else {
                log:printError("Error in retrieving content", content);
            }
        }
    } else {
        log:printError("Error in retrieving content", response);
    }
    log:printInfo("Executed Get operation");
}

@test:Config{
    dependsOn: ["testReadContent"]
}
public function testAppendContent() {
    io:ReadableByteChannel|error byteChannel = io:openReadableFile(appendFilePath);
    if(byteChannel is io:ReadableByteChannel){
        error? response = clientEP -> append(filePath, byteChannel);
        if(response is error) {
            log:printError(response.reason().toString());
        }
    }
    log:printInfo("Executed Append operation.");
}

@test:Config{
    dependsOn: ["testAppendContent"]
}
public function testPutContent() {
    io:ReadableByteChannel|error byteChannelToPut = io:openReadableFile(putFilePath);
    if(byteChannelToPut is io:ReadableByteChannel){
        error? response = clientEP -> put(filePath, byteChannelToPut);
        if(response is error) {
            log:printError(response.reason().toString());
        }
    }
    log:printInfo("Executed Put operation.");
}

@test:Config{
    dependsOn: ["testPutContent"]
}
public function testIsDirectory() {
    boolean|error response = clientEP -> isDirectory("/home/in");
    if(response is boolean) {
        log:printInfo("Is directory: " + response.toString());
    } else {
        log:printError(response.reason().toString());
    }
    log:printInfo("Executed Is directory operation.");
}

@test:Config{
    dependsOn: ["testIsDirectory"]
}
public function testCreateDirectory() {
    error? response = clientEP -> mkdir("/home/in/out");
    if(response is error) {
        log:printError(response.reason().toString());
    }
    log:printInfo("Executed Mkdir operation.");
}

@test:Config{
    dependsOn: ["testCreateDirectory"]
}
public function testRenameDirectory() {
    string existingName = "/home/in/out";
    string newName = "/home/in/test";
    error? response = clientEP -> rename(existingName, newName);
    if(response is error) {
        log:printError(response.reason().toString());
    }
    log:printInfo("Executed Rename operation.");
}

@test:Config{
    dependsOn: ["testRenameDirectory"]
}
public function testGetFileSize() {
    int|error response = clientEP -> size(filePath);
    if(response is int){
        log:printInfo("Size: "+response.toString());
    } else {
        log:printError(response.reason().toString());
    }
    log:printInfo("Executed size operation.");
}

@test:Config{
    dependsOn: ["testGetFileSize"]
}
public function testListFiles() {
    string[]|error response = clientEP -> list("/home/in");
    if(response is string[]){
        log:printInfo("List of directories: " + response[0] + ", " + response[1]);
    } else {
        log:printError(response.reason().toString());
    }
    log:printInfo("Executed List operation.");
}

@test:Config{
    dependsOn: ["testListFiles"]
}
public function testDeleteFile() {
    error? response = clientEP -> delete(filePath);
    if(response is error) {
        log:printError(response.reason().toString());
    }
    log:printInfo("Executed Delete operation.");
}

@test:Config{
    dependsOn: ["testDeleteFile"]
}
public function testRemoveDirectory() {
    error? response = clientEP -> rmdir("/home/in/test");
    if(response is error) {
        log:printError(response.reason().toString());
    }
    log:printInfo("Executed Rmdir operation.");
}

@test:AfterSuite
public function stopServer() returns error? {
    error? response = stopFtpServer();
}

function initFtpServer(map<anydata> config) returns error? = @java:Method{
    name: "initServer",
    class: "org.wso2.ei.testutil.MockFTPServer"
} external;

function stopFtpServer() returns () = @java:Method{
    name: "stopServer",
    class: "org.wso2.ei.testutil.MockFTPServer"
} external;
