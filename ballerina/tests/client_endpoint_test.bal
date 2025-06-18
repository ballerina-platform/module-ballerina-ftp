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

string filePath = "/home/in/test1.txt";
string nonFittingFilePath = "/home/in/test4.txt";
string newFilePath = "/home/in/test2.txt";
string appendFilePath = "tests/resources/datafiles/file1.txt";
string putFilePath = "tests/resources/datafiles/file2.txt";

// Create the config to access anonymous mock FTP server
ClientConfiguration anonConfig = {
    protocol: FTP,
    host: "127.0.0.1",
    port: 21210
};

// Create the config to access mock FTP server
ClientConfiguration config = {
    protocol: FTP,
    host: "127.0.0.1",
    port: 21212,
    auth: {credentials: {username: "wso2", password: "wso2123"}}
};

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
        },
        preferredMethods: [GSSAPI_WITH_MIC, PUBLICKEY, KEYBOARD_INTERACTIVE, PASSWORD]
    }
};

Client? anonClientEp = ();
Client? clientEp = ();
Client? sftpClientEp = ();

Listener? callerListener = ();
Listener? remoteServerListener = ();
Listener? anonymousRemoteServerListener = ();
Listener? secureRemoteServerListener = ();

@test:BeforeSuite
function initTestEnvironment() returns error?  {
    io:println("Starting servers");
    anonClientEp = check new (anonConfig);
    clientEp = check new (config);
    sftpClientEp = check new (sftpConfig);

    callerListener = check new (callerListenerConfig);
    check (<Listener>callerListener).attach(callerService);
    check (<Listener>callerListener).'start();

    remoteServerListener = check new (remoteServerConfiguration);
    check (<Listener>remoteServerListener).attach(remoteServerService);
    check (<Listener>remoteServerListener).'start();

    anonymousRemoteServerListener = check new (anonymousRemoteServerConfig);
    check (<Listener>anonymousRemoteServerListener).attach(anonymousRemoteServerService);
    check (<Listener>anonymousRemoteServerListener).'start();

    secureRemoteServerListener = check new (secureRemoteServerConfig);
    check (<Listener>secureRemoteServerListener).attach(secureRemoteServerService);
    check (<Listener>secureRemoteServerListener).'start();
}

@test:Config {}
public function testReadFromAnonServer() returns error? {
    stream<byte[] & readonly, io:Error?>|Error str = (<Client>anonClientEp)->get(filePath);
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchStreamContent(str, "File content"), msg = "Found unexpected content from `get` operation");
        io:Error? closeResult = str.close();
        if closeResult is io:Error {
            test:assertFail(msg = "Error while closing stream in `get` operation." + closeResult.message());
        }
    } else {
        test:assertFail(msg = "Found unexpected response type" + str.message());
    }
}

@test:Config {
    dependsOn: [testReadFromAnonServer, testAddedFileCount, testSecureAddedFileCount]
}
public function testReadBlockFittingContent() returns error? {
    stream<byte[] & readonly, io:Error?>|Error str = (<Client>clientEp)->get(filePath);
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchStreamContent(str, "File content"), msg = "Found unexpected content from `get` operation");
        io:Error? closeResult = str.close();
        if closeResult is io:Error {
            test:assertFail(msg = "Error while closing stream in `get` operation." + closeResult.message());
        }
    } else {
        test:assertFail(msg = "Found unexpected response type" + str.message());
    }
}

@test:Config {
    dependsOn: [testReadBlockFittingContent]
}
public function testReadBlockNonFittingContent() returns error? {
    stream<byte[] & readonly, io:Error?>|Error str = (<Client>clientEp)->get(nonFittingFilePath);
    int i = 0;
    string nonFittingContent = "";
    while i < 1000 {
        nonFittingContent += "123456789";
        i += 1;
    }
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchStreamContent(str, nonFittingContent), msg = "Found unexpected content from `get` operation");
        io:Error? closeResult = str.close();
        if closeResult is io:Error {
            test:assertFail(msg = "Error while closing stream in `get` operation." + closeResult.message());
        }
    } else {
        test:assertFail(msg = "Found unexpected response type" + str.message());
    }
}

@test:Config {
    dependsOn: [testReadBlockNonFittingContent]
}
public function testAppendContent() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(appendFilePath, 7);

    Error? response = (<Client>clientEp)->append(filePath, bStream);
    if response is Error {
        test:assertFail(msg = "Error while appending a file: " + response.message());
    } else {
        log:printInfo("Executed `append` operation");
    }

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>clientEp)->get(filePath);
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchStreamContent(str, "File contentAppend content"),
            msg = "Found unexpected content from `get` operation after `append` operation");
        io:Error? closeResult = str.close();
        if closeResult is io:Error {
            test:assertFail(msg = "Error while closing stream in `append` operation." + closeResult.message());
        }
    } else {
        test:assertFail(msg = "Found unexpected response type" + str.message());
    }
}

@test:Config {
    dependsOn: [testAppendContent]
}
public function testPutFileContent() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);

    Error? response = (<Client>clientEp)->put(newFilePath, bStream);
    if response is Error {
        test:assertFail(msg = "Found unexpected response type from `put` operation" + response.message());
    }
    log:printInfo("Executed `put` operation");

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>clientEp)->get(newFilePath);
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchStreamContent(str, "Put content"),
            msg = "Found unexpected content from `get` operation after `put` operation");
        io:Error? closeResult = str.close();
        if closeResult is io:Error {
            test:assertFail(msg = "Error while closing stream in `get` operation." + closeResult.message());
        }
    } else {
        test:assertFail(msg = "Found unexpected response type" + str.message());
    }
}

@test:Config {
    dependsOn: [testPutFileContent]
}
public function testPutCompressedFileContent() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);

    Error? response = (<Client>clientEp)->put("/home/in/test3.txt", bStream, compressionType = ZIP);
    if response is Error {
        test:assertFail(msg = "Found unexpected response type from compressed `put` operation" + response.message());
    }
    log:printInfo("Executed compressed `put` operation");

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>clientEp)->get("/home/in/test3.zip");
    if str is Error {
        test:assertFail(msg = "Error occurred during compressed `put` operation" + str.message());
    }
}

@test:Config {
    dependsOn: [testPutCompressedFileContent]
}
public function testPutLargeFileContent() returns error? {

    byte[] firstByteArray = [];
    int i = 0;
    while i < 16390 {
        firstByteArray[i] = 65;
        i = i + 1;
    }
    string sendString1 = check string:fromBytes(firstByteArray);

    (byte[])[] & readonly bList = [
        firstByteArray.cloneReadOnly(),
        "123456".toBytes().cloneReadOnly(),
        "end.".toBytes().cloneReadOnly()
    ];
    stream<byte[] & readonly, io:Error?> bStream = bList.toStream();
    Error? response = (<Client>clientEp)->put(newFilePath, bStream);
    if response is Error {
        test:assertFail(msg = "Found unexpected response type from `put` operation");
    }
    log:printInfo("Executed `put` operation for large files");

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>clientEp)->get(newFilePath);
    if str is stream<byte[] & readonly, io:Error?> {
        string expectedString = sendString1 + "123456" + "end.";
        test:assertTrue(check matchStreamContent(str, expectedString),
            msg = "Found unexpected content from `get` operation after `put` operation with large chunks");
        io:Error? closeResult = str.close();
        if closeResult is io:Error {
            test:assertFail(msg = "Error while closing stream in `get` operation." + closeResult.message());
        }
    } else {
        test:assertFail(msg = "Found unexpected response type" + str.message());
    }
}

isolated function matchStreamContent(stream<byte[] & readonly, io:Error?> binaryStream, string matchedString) returns boolean|error {
    string fullContent = "";
    string tempContent = "";
    int maxLoopCount = 100000;
    while maxLoopCount > 0 {
        record {|byte[] value;|}|io:Error? binaryArray = binaryStream.next();
        if binaryArray is io:Error {
            break;
        } else if binaryArray is () {
            break;
        } else {
            tempContent = check strings:fromBytes(binaryArray.value);
            fullContent = fullContent + tempContent;
            maxLoopCount -= 1;
        }
    }
    return matchedString == fullContent;
}

@test:Config {
    dependsOn: [testPutLargeFileContent]
}
public function testPutTextContent() returns error? {
    string textToPut = "Sample text content";
    Error? response = (<Client>clientEp)->put(filePath, textToPut);
    if response is Error {
        test:assertFail(msg = "Found unexpected response type from `put` operation on text content" + response.message());
    } else {
        log:printInfo("Executed `put` operation on text");
    }

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>clientEp)->get(filePath);
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchStreamContent(str, "Sample text content"),
            msg = "Found unexpected content from `get` operation after `put` operation on text");
        io:Error? closeResult = str.close();
        if closeResult is io:Error {
            test:assertFail(msg = "Error while closing stream in `get` operation." + closeResult.message());
        }
    } else {
        test:assertFail(msg = "Found unexpected response type" + str.message());
    }
}

@test:Config {
    dependsOn: [testPutTextContent]
}
public function testPutJsonContent() returns error? {
    json jsonToPut = {name: "Anne", age: 20};
    Error? response = (<Client>clientEp)->put(filePath, jsonToPut);
    if response is Error {
        test:assertFail(msg = "Found unexpected response type from `put` operation on JSON content" + response.message());
    } else {
        log:printInfo("Executed `put` operation on JSON");
    }

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>clientEp)->get(filePath);
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchStreamContent(str, "{\"name\":\"Anne\", \"age\":20}"),
            msg = "Found unexpected content from `get` operation after `put` operation on JSON");
        io:Error? closeResult = str.close();
        if closeResult is io:Error {
            test:assertFail(msg = "Error while closing stream in `get` operation." + closeResult.message());
        }
    } else {
        test:assertFail(msg = "Found unexpected response type" + str.message());
    }
}

@test:Config {
    dependsOn: [testPutJsonContent]
}
public function testPutXMLContent() returns error? {
    xml xmlToPut = xml `<note><heading>Memo</heading><body>Memo content</body></note>`;
    Error? response = (<Client>clientEp)->put(filePath, xmlToPut);
    if response is Error {
        test:assertFail(msg = "Found unexpected response type from `put` operation on XML content" + response.message());
    } else {
        log:printInfo("Executed `put` operation on XML");
    }

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>clientEp)->get(filePath);
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchStreamContent(str, "<note><heading>Memo</heading><body>Memo content</body></note>"),
            msg = "Found unexpected content from `get` operation after `put` operation on XML");
        io:Error? closeResult = str.close();
        if closeResult is io:Error {
            test:assertFail(msg = "Error while closing stream in `get` operation." + closeResult.message());
        }
    } else {
        test:assertFail(msg = "Found unexpected response type" + str.message());
    }
}

@test:Config {
    dependsOn: [testPutXMLContent]
}
public function testIsDirectory() {
    boolean|Error response1 = (<Client>clientEp)->isDirectory("/home/in");
    log:printInfo("Executed `isDirectory` operation on a directory");
    if response1 is boolean {
        log:printInfo("Is directory: " + response1.toString());
        test:assertEquals(response1, true,
            msg = "A directory is not correctly recognized with `isDirectory` operation");
    } else {
        test:assertFail(msg = "Error while invoking `isDirectory` operation" + response1.message());
    }

    boolean|Error response2 = (<Client>clientEp)->isDirectory(filePath);
    log:printInfo("Executed `isDirectory` operation on a file");
    if response2 is boolean {
        log:printInfo("Is directory: " + response2.toString());
        test:assertEquals(response2, false,
            msg = "A file is not correctly recognized with `isDirectory` operation");
    } else {
        test:assertFail(msg = "Error while invoking `isDirectory` operation" + response2.message());
    }
}

@test:Config {
    dependsOn: [testIsDirectory]
}
public function testCreateDirectory() {
    Error? response1 = (<Client>clientEp)->mkdir("/home/in/out");
    if response1 is Error {
        test:assertFail(msg = "Error while creating a directory" + response1.message());
    } else {
        log:printInfo("Executed `mkdir` operation");
    }

    boolean|Error response2 = (<Client>clientEp)->isDirectory("/home/in/out");
    log:printInfo("Executed `isDirectory` operation after creating a directory");
    if response2 is boolean {
        log:printInfo("Is directory: " + response2.toString());
        test:assertEquals(response2, true, msg = "Directory was not created");
    } else {
        test:assertFail(msg = "Error while invoking `isDirectory` operation" + response2.message());
    }
}

@test:Config {
    dependsOn: [testCreateDirectory]
}
public function testRenameDirectory() {
    string existingName = "/home/in/out";
    string newName = "/home/in/test";
    Error? response1 = (<Client>clientEp)->rename(existingName, newName);
    if response1 is Error {
        test:assertFail(msg = "Error while invoking `rename` operation" + response1.message());
    } else {
        log:printInfo("Executed `rename` operation");
    }

    boolean|Error response2 = (<Client>clientEp)->isDirectory(existingName);
    log:printInfo("Executed `isDirectory` operation on original directory after renaming a directory");
    if response2 is Error {
        test:assertEquals(response2.message(), "/home/in/out does not exists to check if it is a directory.",
            msg = "Incorrect error message for non-existing file/directory at `isDirectory` operation");
    } else {
        test:assertFail("Error not created while invoking `isDirectory` operation after `rename` operation");
    }

    boolean|Error response3 = (<Client>clientEp)->isDirectory(newName);
    log:printInfo("Executed `isDirectory` operation on renamed directory after renaming a directory");
    if response3 is boolean {
        log:printInfo("Existance of renamed directory: " + response3.toString());
        test:assertEquals(response3, true, msg = "New directory name was not created during `rename` operation");
    } else {
        test:assertFail(msg = "Error while invoking `isDirectory` operation after `rename` operation" + response3.message());
    }

}

@test:Config {
    dependsOn: [testRenameDirectory]
}
public function testGetFileSize() {
    int|Error response = (<Client>clientEp)->size(filePath);
    log:printInfo("Executed `size` operation.");
    if response is int {
        log:printInfo("Size: " + response.toString());
        test:assertEquals(response, 61, msg = "File size is not given with `size` operation");
    } else {
        test:assertFail(msg = "Error while invoking the `size` operation" + response.message());
    }
}

@test:Config {
    dependsOn: [testGetFileSize]
}
public function testListFiles() {
    string[] resourceNames = [
        "child_directory",
        "test1.txt",
        "complexDirectory",
        "test",
        "folder1",
        "test3.zip",
        "childDirectory",
        "test2.txt",
        "test3.txt",
        "test4.txt"
    ];
    int[] fileSizes = [0, 61, 0, 0, 0, 145, 0, 16400, 12, 9000];
    FileInfo[]|Error response = (<Client>clientEp)->list("/home/in");
    if response is FileInfo[] {
        log:printInfo("List of files/directories: ");
        int i = 0;
        foreach var fileInfo in response {
            log:printInfo(fileInfo.toString());
            test:assertEquals(fileInfo.path, "/home/in/" + resourceNames[i],
                msg = "File path is not matched during the `list` operation");
            test:assertTrue(fileInfo.lastModifiedTimestamp > 0,
                msg = "Last Modified Timestamp of the file is not correct during the `list` operation");
            test:assertEquals(fileInfo.size, fileSizes[i],
                msg = "File size is not matched during the `list` operation");
            i = i + 1;
        }
        log:printInfo("Executed `list` operation");
    } else {
        test:assertFail(msg = "Error while invoking the `list` operation" + response.message());
    }
}

@test:Config {
    dependsOn: [testListFiles]
}
public function testDeleteFile() returns error? {
    Error? response = (<Client>clientEp)->delete(filePath);
    if response is Error {
        test:assertFail(msg = "Error while invoking the `delete` operation" + response.message());
    } else {
        log:printInfo("Executed `delete` operation");
    }

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>clientEp)->get(filePath);
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertFalse(check matchStreamContent(str, "<note><heading>Memo</heading><body>Memo content</body></note>"),
            msg = "File was not deleted with `delete` operation");
        io:Error? closeResult = str.close();
        if closeResult is io:Error {
            test:assertFail(msg = "Error while closing stream in `get` operation." + closeResult.message());
        }
    } else {
        test:assertEquals(str.message(),
            "Failed to read file: ftp://wso2:wso2123@127.0.0.1:21212/home/in/test1.txt not found",
            msg = "Correct error is not given when the file is deleted." + str.message());
    }
}

@test:Config {
    dependsOn: [testDeleteFile]
}
public function testRemoveDirectory() returns error? {
    return testGenericRmdir("/home/in/test");
}

@test:Config {
    dependsOn: [testRemoveDirectory]
}
public function testRemoveDirectoryWithSubdirectory() returns error? {
    return testGenericRmdir("/home/in/folder1");
}

@test:Config {
    dependsOn: [testRemoveDirectoryWithSubdirectory]
}
public function testRemoveDirectoryWithFiles() returns error? {
    return testGenericRmdir("/home/in/child_directory");
}

@test:Config {
    dependsOn: [testRemoveDirectoryWithFiles]
}
public function testRemoveComplexDirectory() returns error? {
    return testGenericRmdir("/home/in/complexDirectory");
}

function testGenericRmdir(string path) returns error? {
    boolean|Error response0 = (<Client>clientEp)->isDirectory(path);
    log:printInfo("Executed `isDirectory` operation before deleting a directory " + path);
    int retryCount = 0;
    while (response0 is Error || !response0) && retryCount < 10 {
        log:printInfo("Executed `isDirectory` operation before deleting a directory " + path);
        runtime:sleep(1);
        response0 = (<Client>clientEp)->isDirectory(path);
        retryCount += 1;
    }

    if retryCount >= 10 {
        test:assertFail(msg = "Error while invoking the `isDirectory` operation before invoking the `rmdir` operation");
    } else {
        Error? response1 = (<Client>clientEp)->rmdir(path);
        if response1 is Error {
            test:assertFail(msg = "Error while invoking the `rmdir` operation on " + path + ": " + response1.message());
        } else {
            log:printInfo("Executed `rmdir` operation on " + path);
        }
    }

    boolean|Error response2 = (<Client>clientEp)->isDirectory(path);
    log:printInfo("Executed `isDirectory` operation after deleting a directory " + path);

    int i = 0;
    while response2 is boolean && i < 10 {
        runtime:sleep(1);
        response2 = (<Client>clientEp)->isDirectory(path);
        log:printInfo("Executed `isDirectory` operation after deleting a directory " + path);
        i += 1;
    }
    if response2 is Error {
        test:assertEquals(response2.message(), path + " does not exists to check if it is a directory.",
            msg = "Incorrect error message for non-existing file/directory at `isDirectory` operation after `rmdir` operation");
    } else {
        // test:assertFail(msg = "Error not created while invoking `isDirectory` operation after `rmdir` operation on " + path );
    }
}

@test:AfterSuite {}
public function cleanTestEnvironment() returns error? {
    check (<Listener>callerListener).gracefulStop();
    check (<Listener>remoteServerListener).gracefulStop();
    check (<Listener>anonymousRemoteServerListener).gracefulStop();
    check (<Listener>secureRemoteServerListener).gracefulStop();
}
