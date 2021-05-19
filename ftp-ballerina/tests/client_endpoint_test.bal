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
import ballerina/lang.'string as strings;
import ballerina/log;
import ballerina/jballerina.java;

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
    Error? response = initFtpServer(config);
    return !(response is Error);
}

@test:Config{}
public function testReadBlockFittingContent() {
    var str = clientEP -> get(filePath, 6);
    if (str is stream<byte[], io:Error?>) {
        record {|byte[] value;|}|io:Error? arr1 = str.next();
        if (arr1 is record {|byte[] value;|}) {
            string fileContent = checkpanic strings:fromBytes(arr1.value);
            test:assertEquals(fileContent, "File c", msg = "Found unexpected content from `get` operation");
            record {|byte[] value;|}|io:Error? arr2 = str.next();
            if (arr2 is record {|byte[] value;|}) {
                string fileContent2 = checkpanic strings:fromBytes(arr2.value);
                test:assertEquals(fileContent2, "ontent",
                    msg = "Found unexpected content from `next` method of `get` operation");
                record {|byte[] value;|}|io:Error? arr3 = str.next();
                test:assertTrue(arr3 is (), msg = "Found unexpected content from 2nd `next` method of `get` operation");
            } else {
                test:assertFail(msg = "Found unexpected arr2 output type");
            }
        } else {
            test:assertFail(msg = "Found unexpected arr1 output type");
        }
        io:Error? closeResult = str.close();
    } else {
       test:assertFail(msg = "Found unexpected str output type" + str.message());
    }
}

@test:Config{
    dependsOn: [testReadBlockFittingContent]
}
public function testReadBlockNonFittingContent() {
    var str = clientEP -> get(filePath, 7);
    if (str is stream<byte[], io:Error?>) {
        record {|byte[] value;|}|io:Error? arr1 = str.next();
        if (arr1 is record {|byte[] value;|}) {
            string fileContent = checkpanic strings:fromBytes(arr1.value);
            test:assertEquals(fileContent, "File co", msg = "Found unexpected content from `get` operation");
            record {|byte[] value;|}|io:Error? arr2 = str.next();
            if (arr2 is record {|byte[] value;|}) {
                string fileContent2 = checkpanic strings:fromBytes(arr2.value);
                test:assertEquals(fileContent2, "ntent",
                    msg = "Found unexpected content from `next` method of `get` operation");
                record {|byte[] value;|}|io:Error? arr3 = str.next();
                test:assertTrue(arr3 is (), msg = "Found unexpected content from 2nd `next` method of `get` operation");
            } else {
                test:assertFail(msg = "Found unexpected arr2 output type");
            }
        } else {
            test:assertFail(msg = "Found unexpected arr1 output type");
        }
        io:Error? closeResult = str.close();
    } else {
       test:assertFail(msg = "Found unexpected str output type" + str.message());
    }
}

@test:Config{
    dependsOn: [testReadBlockNonFittingContent]
}
public function testAppendContent() {
    (byte[])[] bList = ["Append ".toBytes(), "content".toBytes()];
    stream<byte[], io:Error?> bStream = bList.toStream();
    Error? response = clientEP -> append(filePath, bStream);
    if(response is Error) {
        log:printError("Error in editing file", 'error = response);
    } else {
        log:printInfo("Executed Append operation");
    }
}

@test:Config{
    dependsOn: [testAppendContent]
}
public function testPutFileContent() {
    (byte[])[] bList = ["Put c".toBytes(), "onten".toBytes(), "t".toBytes()];
    stream<byte[], io:Error?> bStream = bList.toStream();
    Error? response = clientEP -> put(newFilePath, bStream);
    if(response is Error) {
        log:printError("Error in put operation", 'error = response);
    }
    log:printInfo("Executed Put operation");
}

@test:Config{
    dependsOn: [testPutFileContent]
}
public function testPutTextContent() {
    string textToPut = "Sample text content";
    Error? response = clientEP -> put(filePath, textToPut);
    if(response is Error) {
        log:printError("Error in put operation", 'error = response);
    } else {
        log:printInfo("Executed Put operation");
    }
}

@test:Config{
    dependsOn: [testPutTextContent]
}
public function testPutJsonContent() {
    json jsonToPut = { name: "Anne", age: 20 };
    Error? response = clientEP -> put(filePath, jsonToPut);
    if(response is Error) {
        log:printError("Error in put operation", 'error = response);
    } else {
        log:printInfo("Executed Put operation");
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
    Error? response = clientEP -> put(filePath, xmlToPut);
    if(response is Error) {
        log:printError("Error in put operation", 'error = response);
    } else {
        log:printInfo("Executed Put operation");
    }
}

@test:Config{
    dependsOn: [testPutXMLContent]
}
public function testIsDirectory() {
    boolean|Error response = clientEP -> isDirectory("/home/in");
    if(response is boolean) {
        log:printInfo("Is directory: " + response.toString());
        log:printInfo("Executed Is directory operation");
    } else {
        log:printError("Error in reading isDirectory", 'error = response);
    }
}

@test:Config{
    dependsOn: [testIsDirectory]
}
public function testCreateDirectory() {
    Error? response = clientEP -> mkdir("/home/in/out");
    if(response is Error) {
        log:printError("Error in creating directory", 'error = response);
    } else {
        log:printInfo("Executed Mkdir operation");
    }
}

@test:Config{
    dependsOn: [testCreateDirectory]
}
public function testRenameDirectory() {
    string existingName = "/home/in/out";
    string newName = "/home/in/test";
    Error? response = clientEP -> rename(existingName, newName);
    if(response is Error) {
        log:printError("Error in renaming directory", 'error = response);
    } else {
        log:printInfo("Executed Rename operation");
    }
}

@test:Config{
    dependsOn: [testRenameDirectory]
}
public function testGetFileSize() {
    int|Error response = clientEP -> size(filePath);
    if(response is int){
        log:printInfo("Size: " + response.toString());
        log:printInfo("Executed size operation.");
    } else {
        log:printError("Error in getting file size", 'error = response);
    }
}

@test:Config{
    dependsOn: [testGetFileSize]
}
public function testListFiles() {
    FileInfo[]|Error response = clientEP -> list("/home/in");
    if (response is FileInfo[]) {
        log:printInfo("List of files/directories: ");
        foreach var fileInfo in response {
            log:printInfo(fileInfo.toString());
        }
        log:printInfo("Executed List operation");
    } else {
        log:printError("Error in getting file list", 'error = response);
    }
}

@test:Config{
    dependsOn: [testListFiles]
}
public function testDeleteFile() {
    Error? response = clientEP -> delete(filePath);
    if(response is Error) {
        log:printError("Error in deleting file", 'error = response);
    } else {
        log:printInfo("Executed Delete operation");
    }
}

@test:Config{
    dependsOn: [testDeleteFile]
}
public function testRemoveDirectory() {
    Error? response = clientEP -> rmdir("/home/in/test");
    if(response is Error) {
        log:printError("Error in removing directory", 'error = response);
    } else {
        log:printInfo("Executed Rmdir operation");
    }
}

@test:AfterSuite{}
public function stopServer() returns error? {
    error? response = stopFtpServer();
}

function initFtpServer(map<anydata> config) returns Error? = @java:Method{
    name: "initServer",
    'class: "org.ballerinalang.stdlib.ftp.testutils.mockServerUtils.MockFTPServer"
} external;

function stopFtpServer() returns () = @java:Method{
    name: "stopServer",
    'class: "org.ballerinalang.stdlib.ftp.testutils.mockServerUtils.MockFTPServer"
} external;
