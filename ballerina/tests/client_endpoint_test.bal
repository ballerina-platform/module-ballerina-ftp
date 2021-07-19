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
import ballerina/lang.runtime as runtime;
import ballerina/lang.'string as strings;
import ballerina/log;
import ballerina/jballerina.java;

string filePath = "/home/in/test1.txt";
string newFilePath = "/home/in/test2.txt";
string appendFilePath = "tests/resources/datafiles/file1.txt";
string putFilePath = "tests/resources/datafiles/file2.txt";

// Create the config to access anonymous mock FTP server
ClientConfiguration anonConfig = {
    protocol: FTP,
    host: "127.0.0.1",
    port: 21210
};

Client anonClientEp = new(anonConfig);

// Create the config to access mock FTP server
ClientConfiguration config = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21212,
        auth: {credentials: {username: "wso2", password: "wso2123"}}
};

Client clientEp = new(config);

// Create the config to access mock SFTP server
ClientConfiguration sftpConfig = {
    protocol: SFTP,
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

Client sftpClientEp = new(sftpConfig);

// Start mock FTP servers
boolean startedServers = initServers();

function initServers() returns boolean {
    Error? response0 = initAnonymousFtpServer(anonConfig);
    Error? response1 = initFtpServer(config);
    Error? response2 = initSftpServer(sftpConfig);
    return !(response0 is Error || response1 is Error || response2 is Error);
}

@test:Config{}
public function testReadFromAnonServer() returns error? {
    test:assertTrue(startedServers, msg = "Test servers are not properly started.");
    stream<byte[] & readonly, io:Error?>|Error str = anonClientEp->get(filePath, 6);
    if (str is stream<byte[] & readonly, io:Error?>) {
        record {|byte[] value;|}|io:Error? arr1 = str.next();
        if (arr1 is record {|byte[] value;|}) {
            string fileContent = check strings:fromBytes(arr1.value);
            test:assertEquals(fileContent, "File c", msg = "Found unexpected content from `get` operation");
            record {|byte[] value;|}|io:Error? arr2 = str.next();
            if (arr2 is record {|byte[] value;|}) {
                string fileContent2 = check strings:fromBytes(arr2.value);
                test:assertEquals(fileContent2, "ontent",
                    msg = "Found unexpected content from `next` method of `get` operation");
                record {|byte[] value;|}|io:Error? arr3 = str.next();
                log:printInfo("Executed first `get` operation");
                log:printInfo("Later content in file: " + fileContent + fileContent2);
                test:assertTrue(arr3 is (), msg = "Found unexpected content from 2nd `next` method of `get` operation");
            } else {
                test:assertFail(msg = "Found unexpected arr2 output type");
            }
        } else {
            test:assertFail(msg = "Found unexpected arr1 output type");
        }
        io:Error? closeResult = str.close();
        if (closeResult is io:Error) {
            test:assertFail(msg = "Error while closing stream in `get` operation.");
        }
    } else {
       test:assertFail(msg = "Found unexpected response type" + str.message());
    }
}

@test:Config{
    dependsOn: [testReadFromAnonServer, testAddedFileCount, testSecureAddedFileCount]
}
public function testReadBlockFittingContent() returns error? {
    test:assertTrue(startedServers, msg = "Test servers are not properly started.");
    stream<byte[] & readonly, io:Error?>|Error str = clientEp->get(filePath, 6);
    if (str is stream<byte[] & readonly, io:Error?>) {
        record {|byte[] value;|}|io:Error? arr1 = str.next();
        if (arr1 is record {|byte[] value;|}) {
            string fileContent = check strings:fromBytes(arr1.value);
            test:assertEquals(fileContent, "File c", msg = "Found unexpected content from `get` operation");
            record {|byte[] value;|}|io:Error? arr2 = str.next();
            if (arr2 is record {|byte[] value;|}) {
                string fileContent2 = check strings:fromBytes(arr2.value);
                test:assertEquals(fileContent2, "ontent",
                    msg = "Found unexpected content from `next` method of `get` operation");
                record {|byte[] value;|}|io:Error? arr3 = str.next();
                log:printInfo("Executed first `get` operation");
                log:printInfo("Later content in file: " + fileContent + fileContent2);
                test:assertTrue(arr3 is (), msg = "Found unexpected content from 2nd `next` method of `get` operation");
            } else {
                test:assertFail(msg = "Found unexpected arr2 output type");
            }
        } else {
            test:assertFail(msg = "Found unexpected arr1 output type");
        }
        io:Error? closeResult = str.close();
        if (closeResult is io:Error) {
            test:assertFail(msg = "Error while closing stream in `get` operation.");
        }
    } else {
       test:assertFail(msg = "Found unexpected response type" + str.message());
    }
}

@test:Config{
    dependsOn: [testReadBlockFittingContent]
}
public function testReadBlockNonFittingContent() returns error? {
    stream<byte[] & readonly, io:Error?>|Error str = clientEp->get(filePath, 7);
    if (str is stream<byte[] & readonly, io:Error?>) {
        record {|byte[] value;|}|io:Error? arr1 = str.next();
        if (arr1 is record {|byte[] value;|}) {
            string fileContent = check strings:fromBytes(arr1.value);
            test:assertEquals(fileContent, "File co", msg = "Found unexpected content from `get` operation");
            record {|byte[] value;|}|io:Error? arr2 = str.next();
            if (arr2 is record {|byte[] value;|}) {
                string fileContent2 = check strings:fromBytes(arr2.value);
                test:assertEquals(fileContent2, "ntent",
                    msg = "Found unexpected content from `next` method of `get` operation");
                record {|byte[] value;|}|io:Error? arr3 = str.next();
                log:printInfo("Executed second Get operation");
                log:printInfo("Later content in file: " + fileContent + fileContent2);
                test:assertTrue(arr3 is (), msg = "Found unexpected content from 2nd `next` method of `get` operation");
            } else {
                test:assertFail(msg = "Found unexpected arr2 output type");
            }
        } else {
            test:assertFail(msg = "Found unexpected arr1 output type");
        }
        io:Error? closeResult = str.close();
        if (closeResult is io:Error) {
            test:assertFail(msg = "Error while closing stream in `get` operation.");
        }
    } else {
       test:assertFail(msg = "Found unexpected response type" + str.message());
    }
}

@test:Config{
    dependsOn: [testReadBlockNonFittingContent]
}
public function testAppendContent() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(appendFilePath, 7);

    Error? response = clientEp->append(filePath, bStream);
    if (response is Error) {
        log:printError("Error while appending a file", 'error = response);
    } else {
        log:printInfo("Executed `append` operation");
    }

    stream<byte[] & readonly, io:Error?>|Error str = clientEp->get(filePath, 26);
    if (str is stream<byte[] & readonly, io:Error?>) {
        record {|byte[] value;|}|io:Error? arr1 = str.next();
        if (arr1 is record {|byte[] value;|}) {
            string fileContent = check strings:fromBytes(arr1.value);
            test:assertEquals(fileContent, "File contentAppend content",
                msg = "Found unexpected content from `get` operation after `append` operation");
            record {|byte[] value;|}|io:Error? arr2 = str.next();
            test:assertTrue(arr2 is (),
                msg = "Unexpected content from 2nd `next` method of `get` operation after `append` operation");
        } else {
            test:assertFail(msg = "Found unexpected arr1 output type");
        }
        io:Error? closeResult = str.close();
        if (closeResult is io:Error) {
            test:assertFail(msg = "Error while closing stream in `append` operation.");
        }
    } else {
       test:assertFail(msg = "Found unexpected response type" + str.message());
    }
}

@test:Config{
    dependsOn: [testAppendContent]
}
public function testPutFileContent() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);

    Error? response = clientEp->put(newFilePath, bStream);
    if (response is Error) {
        log:printError("Error in put operation", 'error = response);
    }
    log:printInfo("Executed `put` operation");

    stream<byte[] & readonly, io:Error?>|Error str = clientEp->get(newFilePath, 11);
    if (str is stream<byte[] & readonly, io:Error?>) {
        record {|byte[] value;|}|io:Error? arr1 = str.next();
        if (arr1 is record {|byte[] value;|}) {
            string fileContent = check strings:fromBytes(arr1.value);
            test:assertEquals(fileContent, "Put content",
                msg = "Found unexpected content from `get` operation after `put` operation");
            record {|byte[] value;|}|io:Error? arr2 = str.next();
            test:assertTrue(arr2 is (),
                msg = "Unexpected content from 2nd `next` method of `get` operation after `put` operation");
        } else {
            test:assertFail(msg = "Found unexpected arr1 output type");
        }
        io:Error? closeResult = str.close();
        if (closeResult is io:Error) {
            test:assertFail(msg = "Error while closing stream in `get` operation.");
        }
    } else {
       test:assertFail(msg = "Found unexpected response type" + str.message());
    }
}

@test:Config{
    dependsOn: [testPutFileContent]
}
public function testPutCompressedFileContent() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);

    Error? response = clientEp->put("/home/in/test3.txt", bStream, compressInput=true);
    if (response is Error) {
        log:printError("Error in put operation", 'error = response);
    }
    log:printInfo("Executed `put` operation");

    stream<byte[] & readonly, io:Error?>|Error str = clientEp->get("/home/in/test3.zip", 11);
    if (str is error) {
        test:assertFail(msg = "Error during compressed `put` operation");
    }
}

@test:Config{
    dependsOn: [testPutCompressedFileContent]
}
public function testPutLargeFileContent() returns error? {

    byte[] firstByteArray = [];
    int i = 0;
    while (i < 16390) {
        firstByteArray[i] = 65;
        i = i + 1;
    }
    string sendString1 = check string:fromBytes(firstByteArray);

    (byte[])[] & readonly bList = [firstByteArray.cloneReadOnly(), "123456".toBytes().cloneReadOnly(),
        "end.".toBytes().cloneReadOnly()];
    stream<byte[] & readonly, io:Error?> bStream = bList.toStream();
    Error? response = clientEp->put(newFilePath, bStream);
    if (response is Error) {
        log:printError("Error in put operation", 'error = response);
    }
    log:printInfo("Executed `put` operation");

    stream<byte[] & readonly, io:Error?>|Error str = clientEp->get(newFilePath, 16400);
    if (str is stream<byte[] & readonly, io:Error?>) {
        record {|byte[] value;|}|io:Error? arr1 = str.next();
        if (arr1 is record {|byte[] value;|}) {
            string fileContent = check strings:fromBytes(arr1.value);
            test:assertEquals(fileContent, sendString1 + "123456" + "end.",
                msg = "Found unexpected content from `get` operation after `put` operation with large chunks");
            record {|byte[] value;|}|io:Error? arr2 = str.next();
            test:assertTrue(arr2 is (),
                msg = "Unexpected content from 2nd `next` method of `get` operation after `put` operation");
        } else {
            test:assertFail(msg = "Found unexpected arr1 output type");
        }
        io:Error? closeResult = str.close();
        if (closeResult is io:Error) {
            test:assertFail(msg = "Error while closing stream in `get` operation.");
        }
    } else {
       test:assertFail(msg = "Found unexpected response type" + str.message());
    }
}

@test:Config{
    dependsOn: [testPutLargeFileContent]
}
public function testPutTextContent() returns error? {
    string textToPut = "Sample text content";
    Error? response = clientEp->put(filePath, textToPut);
    if (response is Error) {
        log:printError("Error while invoking `put` operation", 'error = response);
    } else {
        log:printInfo("Executed `put` operation on text");
    }

    stream<byte[] & readonly, io:Error?>|Error str = clientEp->get(filePath, 19);
    if (str is stream<byte[] & readonly, io:Error?>) {
        record {|byte[] value;|}|io:Error? arr1 = str.next();
        if (arr1 is record {|byte[] value;|}) {
            string fileContent = check strings:fromBytes(arr1.value);
            test:assertEquals(fileContent, "Sample text content",
                msg = "Found unexpected content from `get` operation after `put` operation on text");
            record {|byte[] value;|}|io:Error? arr2 = str.next();
            test:assertTrue(arr2 is (),
                msg = "Unexpected content from 2nd `next` method of `get` operation after `put` operation on text");
        } else {
            test:assertFail(msg = "Found unexpected arr1 output type");
        }
        io:Error? closeResult = str.close();
        if (closeResult is io:Error) {
            test:assertFail(msg = "Error while closing stream in `get` operation.");
        }
    } else {
       test:assertFail(msg = "Found unexpected response type" + str.message());
    }
}

@test:Config{
    dependsOn: [testPutTextContent]
}
public function testPutJsonContent() returns error? {
    json jsonToPut = { name: "Anne", age: 20 };
    Error? response = clientEp->put(filePath, jsonToPut);
    if (response is Error) {
        log:printError("Error while invoking `put` operation", 'error = response);
    } else {
        log:printInfo("Executed `put` operation on JSON");
    }

    stream<byte[] & readonly, io:Error?>|Error str = clientEp->get(filePath, 25);
    if (str is stream<byte[] & readonly, io:Error?>) {
        record {|byte[] value;|}|io:Error? arr1 = str.next();
        if (arr1 is record {|byte[] value;|}) {
            string fileContent = check strings:fromBytes(arr1.value);
            test:assertEquals(fileContent, "{\"name\":\"Anne\", \"age\":20}",
                msg = "Found unexpected content from `get` operation after `put` operation on JSON");
            record {|byte[] value;|}|io:Error? arr2 = str.next();
            test:assertTrue(arr2 is (),
                msg = "Unexpected content from 2nd `next` method of `get` operation after `put` operation on JSON");
        } else {
            test:assertFail(msg = "Found unexpected arr1 output type");
        }
        io:Error? closeResult = str.close();
        if (closeResult is io:Error) {
            test:assertFail(msg = "Error while closing stream in `get` operation.");
        }
    } else {
       test:assertFail(msg = "Found unexpected response type" + str.message());
    }
}

@test:Config{
    dependsOn: [testPutJsonContent]
}
public function testPutXMLContent() returns error? {
    xml xmlToPut = xml `<note><heading>Memo</heading><body>Memo content</body></note>`;
    Error? response = clientEp->put(filePath, xmlToPut);
    if (response is Error) {
        log:printError("Error while invoking `put` operation", 'error = response);
    } else {
        log:printInfo("Executed `put` operation on XML");
    }

    stream<byte[] & readonly, io:Error?>|Error str = clientEp->get(filePath, 85);
    if (str is stream<byte[] & readonly, io:Error?>) {
        record {|byte[] value;|}|io:Error? arr1 = str.next();
        if (arr1 is record {|byte[] value;|}) {
            string fileContent = check strings:fromBytes(arr1.value);
            test:assertEquals(fileContent, "<note><heading>Memo</heading><body>Memo content</body></note>",
                msg = "Found unexpected content from `get` operation after `put` operation on XML");
            record {|byte[] value;|}|io:Error? arr2 = str.next();
            test:assertTrue(arr2 is (),
                msg = "Unexpected content from 2nd `next` method of `get` operation after `put` operation on xml");
        } else {
            test:assertFail(msg = "Found unexpected arr1 output type");
        }
        io:Error? closeResult = str.close();
        if (closeResult is io:Error) {
            test:assertFail(msg = "Error while closing stream in `get` operation.");
        }
    } else {
       test:assertFail(msg = "Found unexpected response type" + str.message());
    }
}

@test:Config{
    dependsOn: [testPutXMLContent]
}
public function testIsDirectory() {
    boolean|Error response1 = clientEp->isDirectory("/home/in");
    log:printInfo("Executed `isDirectory` operation on a directory");
    if (response1 is boolean) {
        log:printInfo("Is directory: " + response1.toString());
        test:assertEquals(response1, true,
            msg = "A directory is not correctly recognized with `isDirectory` operation");
    } else {
        log:printError("Error while invoking `isDirectory` operation", 'error = response1);
    }

    boolean|Error response2 = clientEp->isDirectory(filePath);
    log:printInfo("Executed `isDirectory` operation on a file");
    if (response2 is boolean) {
        log:printInfo("Is directory: " + response2.toString());
        test:assertEquals(response2, false,
            msg = "A file is not correctly recognized with `isDirectory` operation");
    } else {
        log:printError("Error while invoking `isDirectory` operation", 'error = response2);
    }
}

@test:Config{
    dependsOn: [testIsDirectory]
}
public function testCreateDirectory() {
    Error? response1 = clientEp->mkdir("/home/in/out");
    if (response1 is Error) {
        log:printError("Error while creating directory", 'error = response1);
    } else {
        log:printInfo("Executed `mkdir` operation");
    }

    boolean|Error response2 = clientEp->isDirectory("/home/in/out");
    log:printInfo("Executed `isDirectory` operation after creating a directory");
    if (response2 is boolean) {
        log:printInfo("Is directory: " + response2.toString());
        test:assertEquals(response2, true, msg = "Directory was not created");
    } else {
        log:printError("Error while invoking `isDirectory` operation", 'error = response2);
    }
}

@test:Config{
    dependsOn: [testCreateDirectory]
}
public function testRenameDirectory() {
    string existingName = "/home/in/out";
    string newName = "/home/in/test";
    Error? response1 = clientEp->rename(existingName, newName);
    if (response1 is Error) {
        log:printError("Error in renaming directory", 'error = response1);
    } else {
        log:printInfo("Executed `rename` operation");
    }

    boolean|Error response2 = clientEp->isDirectory(existingName);
    log:printInfo("Executed `isDirectory` operation on original directory after renaming a directory");
    if (response2 is boolean) {
        log:printInfo("Existance of original directory: " + response2.toString());
        test:assertEquals(response2, false, msg = "Directory was not removed during `rename` operation");
    } else {
        log:printError("Error while invoking `isDirectory` operation", 'error = response2);
    }

    boolean|Error response3 = clientEp->isDirectory(newName);
    log:printInfo("Executed `isDirectory` operation on renamed directory after renaming a directory");
    if (response3 is boolean) {
        log:printInfo("Existance of renamed directory: " + response3.toString());
        test:assertEquals(response3, true, msg = "New directory name was not created during `rename` operation");
    } else {
        log:printError("Error while invoking `isDirectory` operation", 'error = response3);
    }

}

@test:Config{
    dependsOn: [testRenameDirectory]
}
public function testGetFileSize() {
    int|Error response = clientEp->size(filePath);
    log:printInfo("Executed `size` operation.");
    if (response is int){
        log:printInfo("Size: " + response.toString());
        test:assertEquals(response, 61, msg = "File size is not given with `size` operation");
    } else {
        log:printError("Error in getting file size", 'error = response);
    }
}

@test:Config{
    dependsOn: [testGetFileSize]
}
public function testListFiles() {
    string[] resourceNames
        = ["child_directory", "test1.txt", "complexDirectory", "test", "folder1", "test3.zip", "childDirectory",
            "test2.txt", "test3.txt"];
    FileInfo[]|Error response = clientEp->list("/home/in");
    if (response is FileInfo[]) {
        log:printInfo("List of files/directories: ");
        int i = 0;
        foreach var fileInfo in response {
            log:printInfo(fileInfo.toString());
            test:assertEquals(fileInfo.path, "/home/in/" + resourceNames[i],
                msg = "File path is not matched during `list` operation");
            i = i + 1;
        }
        log:printInfo("Executed `list` operation");
    } else {
        log:printError("Error in getting file list", 'error = response);
    }
}

@test:Config{
    dependsOn: [testListFiles]
}
public function testDeleteFile() returns error? {
    Error? response = clientEp->delete(filePath);
    if (response is Error) {
        log:printError("Error in deleting file", 'error = response);
    } else {
        log:printInfo("Executed `delete` operation");
    }

    stream<byte[] & readonly, io:Error?>|Error str = clientEp->get(filePath, 61);
    if (str is stream<byte[] & readonly, io:Error?>) {
        (record {|byte[] value;|}|io:Error)|error? arr1 = trap str.next();
        if (arr1 is record {|byte[] value;|}) {
            string fileContent = check strings:fromBytes(arr1.value);
            test:assertNotEquals(fileContent, "<note><heading>Memo</heading><body>Memo content</body></note>",
                msg = "File was not deleted with `delete` operation");
        } else if (arr1 is io:Error) {
            test:assertFail(msg = "I/O Error during `get` operation after `delete` operation");
        } else if (arr1 is error) {
            test:assertTrue(true);
        } else {
            test:assertFail(msg = "Nil type during `get` operation after `delete` operation");
        }
        io:Error? closeResult = str.close();
        if (closeResult is io:Error) {
            test:assertFail(msg = "Error while closing stream in `get` operation.");
        }
    } else {
       test:assertFail(msg = "Found unexpected output type ");
    }
}

@test:Config{
    dependsOn: [testDeleteFile]
}
public function testRemoveDirectory() {
    Error? response1 = clientEp->rmdir("/home/in/test");
    if (response1 is Error) {
        log:printError("Error in removing directory", 'error = response1);
    } else {
        log:printInfo("Executed `rmdir` operation");
    }

    boolean|Error response2 = clientEp->isDirectory("/home/in/test");
    log:printInfo("Executed `isDirectory` operation after deleting a directory");
    int i = 0;
    while (response2 is boolean && response2 && i < 10) {
         runtime:sleep(1);
         response2 = clientEp->isDirectory("/home/in/test");
         log:printInfo("Executed `isDirectory` operation after deleting a directory");
         i += 1;
    }
    if (response2 is boolean) {
        log:printInfo("Existence of the directory: " + response2.toString());
        test:assertEquals(response2, false, msg = "Directory was not removed during `rmdir` operation");
    } else {
        log:printError("Error in reading `isDirectory`", 'error = response2);
    }
}

@test:Config{
    dependsOn: [testRemoveDirectory]
}
public function testRemoveDirectoryWithSubdirectory() {
    Error? response1 = clientEp->rmdir("/home/in/folder1");
    if (response1 is Error) {
        log:printError("Error in removing directory", 'error = response1);
    } else {
        log:printInfo("Executed `rmdir` operation");
    }

    boolean|Error response2 = clientEp->isDirectory("/home/in/folder1");
    log:printInfo("Executed `isDirectory` operation after deleting a directory");
    int i = 0;
    while (response2 is boolean && response2 && i < 10) {
         runtime:sleep(1);
         response2 = clientEp->isDirectory("/home/in/folder1");
         log:printInfo("Executed `isDirectory` operation after deleting a directory");
         i += 1;
    }
    if (response2 is boolean) {
        log:printInfo("Existence of the directory: " + response2.toString());
        test:assertEquals(response2, false, msg = "Directory was not removed during `rmdir` operation");
    } else {
        log:printError("Error in reading `isDirectory`", 'error = response2);
    }
}

@test:Config{
    dependsOn: [testRemoveDirectoryWithSubdirectory]
}
public function testRemoveDirectoryWithFiles() {
    Error? response1 = clientEp->rmdir("/home/in/child_directory");
    if (response1 is Error) {
        log:printError("Error in removing directory", 'error = response1);
    } else {
        log:printInfo("Executed `rmdir` operation");
    }

    boolean|Error response2 = clientEp->isDirectory("/home/in/child_directory");
    log:printInfo("Executed `isDirectory` operation after deleting a directory");
    int i = 0;
    while (response2 is boolean && response2 && i < 10) {
         runtime:sleep(1);
         response2 = clientEp->isDirectory("/home/in/child_directory");
         log:printInfo("Executed `isDirectory` operation after deleting a directory");
         i += 1;
    }
    if (response2 is boolean) {
        log:printInfo("Existence of the directory: " + response2.toString());
        test:assertEquals(response2, false, msg = "Directory was not removed during `rmdir` operation");
    } else {
        log:printError("Error in reading `isDirectory`", 'error = response2);
    }
}

@test:Config{
    dependsOn: [testRemoveDirectoryWithFiles]
}
public function testRemoveComplexDirectory() {
    Error? response1 = clientEp->rmdir("/home/in/complexDirectory");
    if (response1 is Error) {
        log:printError("Error in removing directory", 'error = response1);
    } else {
        log:printInfo("Executed `rmdir` operation");
    }

    boolean|Error response2 = clientEp->isDirectory("/home/in/complexDirectory");
    log:printInfo("Executed `isDirectory` operation after deleting a directory");
    int i = 0;
    while (response2 is boolean && response2 && i < 10) {
         runtime:sleep(1);
         response2 = clientEp->isDirectory("/home/in/complexDirectory");
         log:printInfo("Executed `isDirectory` operation after deleting a directory");
         i += 1;
    }
    if (response2 is boolean) {
        log:printInfo("Existence of the directory: " + response2.toString());
        test:assertEquals(response2, false, msg = "Directory was not removed during `rmdir` operation");
    } else {
        log:printError("Error in reading `isDirectory`", 'error = response2);
    }
}

@test:AfterSuite{}
public function stopServer() returns error? {
    error? response0 = stopAnonymousFtpServer();
    error? response1 = stopFtpServer();
    error? response2 = stopSftpServer();
}

function initAnonymousFtpServer(map<anydata> config) returns Error? = @java:Method{
    name: "initAnonymousFtpServer",
    'class: "io.ballerina.stdlib.ftp.testutils.mockServerUtils.MockFtpServer"
} external;

function initFtpServer(map<anydata> config) returns Error? = @java:Method{
    name: "initFtpServer",
    'class: "io.ballerina.stdlib.ftp.testutils.mockServerUtils.MockFtpServer"
} external;

function initSftpServer(map<anydata> config) returns Error? = @java:Method{
    name: "initSftpServer",
    'class: "io.ballerina.stdlib.ftp.testutils.mockServerUtils.MockFtpServer"
} external;

function stopFtpServer() returns () = @java:Method{
    name: "stopFtpServer",
    'class: "io.ballerina.stdlib.ftp.testutils.mockServerUtils.MockFtpServer"
} external;

function stopAnonymousFtpServer() returns () = @java:Method{
    name: "stopAnonymousFtpServer",
    'class: "io.ballerina.stdlib.ftp.testutils.mockServerUtils.MockFtpServer"
} external;

function stopSftpServer() returns error? = @java:Method{
    name: "stopSftpServer",
    'class: "io.ballerina.stdlib.ftp.testutils.mockServerUtils.MockFtpServer"
} external;
