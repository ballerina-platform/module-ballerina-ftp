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

@test:Config{
    dependsOn: [testReadBlockNonFittingContent]
}
public function testConnectionWithNonExistingServer() returns error? {
    ClientConfiguration nonExistingServerConfig = {
            protocol: FTP,
            host: "127.0.0.1",
            port: 21218,
            auth: {credentials: {username: "wso2", password: "wso2123"}}
    };
    Client|Error nonExistingServerClientEp = new(nonExistingServerConfig);
    if nonExistingServerClientEp is Error {
        test:assertTrue(nonExistingServerClientEp.message().startsWith("Error while connecting to the FTP server with URL: "),
            msg = "Unexpected error when tried to connect to a non existing server. " + nonExistingServerClientEp.message());
    } else {
        test:assertFail(msg = "Found a non-error response when tried to connect to a non existing server.");
    }
}

@test:Config{
    dependsOn: [testReadBlockNonFittingContent]
}
public function testConnectionWithInvalidConfiguration() returns error? {
    ClientConfiguration invalidConfig = {
            protocol: FTP,
            host: "!@#$%^&*()",
            port: 21212,
            auth: {credentials: {username: "wso2", password: "wso2123"}}
    };
    Client|Error invalidServerClientEp = new(invalidConfig);
    if invalidServerClientEp is Error {
        test:assertTrue(invalidServerClientEp.message().startsWith("Error occurred while constructing a URI from host: "),
            msg = "Unexpected error when tried to connect with invalid parameters. " + invalidServerClientEp.message());
    } else {
        test:assertFail(msg = "Found a non-error response when tried to connect with invalid parameters.");
    }
}

@test:Config{
    dependsOn: [testReadBlockNonFittingContent]
}
public function testReadNonExistingFile() returns error? {
    stream<byte[] & readonly, io:Error?>|Error str = clientEp->get("/home/in/nonexisting.txt");
    if str is Error {
        test:assertEquals(str.message(), "Failed to read file: ftp://wso2:wso2123@127.0.0.1:21212/home/in/nonexisting.txt not found",
            msg = "Unexpected error during the `get` operation of an non-existing file.");
    } else {
        test:assertFail(msg = "Found a non-error response while accessing a non-existing file path");
    }
}

@test:Config{
    dependsOn: [testAppendContent]
}
public function testAppendContentToNonExistingFile() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(appendFilePath, 7);
    Error? receivedError =  clientEp->append("/../invalidFile", bStream);
    if receivedError is Error {
        test:assertEquals(receivedError.message(), "Invalid relative file name.",
            msg = "Unexpected error during the `append` operation of an invalid file.");
    } else {
        test:assertFail(msg = "Found a non-error response while accessing a non-existing file path");
    }
}

@test:Config{
    dependsOn: [testPutFileContent]
}
public function testPutFileContentAtInvalidFileLocation() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    Error? receivedError = clientEp->put("/../InvalidFile", bStream);
    if receivedError is Error {
        test:assertEquals(receivedError.message(), "Invalid relative file name.",
            msg = "Unexpected error during the `put` operation of an invalid file.");
    } else {
        test:assertFail(msg = "Found a non-error response while accessing a non-existing file path");
    }
}

@test:Config{
    dependsOn: [testIsDirectory]
}
public function testIsDirectoryWithNonExistingDirectory() {
    boolean|Error receivedError = clientEp->isDirectory("/home/in/nonexisting");
    if receivedError is Error {
        test:assertEquals(receivedError.message(), "/home/in/nonexisting does not exists to check if it is a directory.",
            msg = "Unexpected error during the `isDirectory` operation of an non-existing directory. " + receivedError.message());
    } else {
        test:assertFail(msg = "Found a non-error response while accessing a non-existing directory path.");
    }
}

@test:Config{
    dependsOn: [testCreateDirectory]
}
public function testCreateDirectoryAtInvalidLocation() {
    Error? receivedError = clientEp->mkdir("/../InvalidDirectory");
    if receivedError is Error {
        test:assertEquals(receivedError.message(), "Invalid relative file name.",
            msg = "Unexpected error during the `mkdir` operation of an non-existing directory. " + receivedError.message());
    } else {
        test:assertFail(msg = "Found a non-error response while accessing a non-existing directory path.");
    }
}

@test:Config{
    dependsOn: [testRenameDirectory]
}
public function testRenameNonExistingDirectory() {
    string existingName = "/nonExistingDirectory";
    string newName = "/home/in/differentDirectory";
    Error? receivedError = clientEp->rename(existingName, newName);

    if receivedError is Error {
        test:assertTrue(receivedError.message().startsWith("Failed to rename file: "),
            msg = "Unexpected error during the `rename` operation of an non-existing file. " + receivedError.message());
    } else {
        test:assertFail(msg = "Found a non-error response while accessing a non existing directory path.");
    }
}

@test:Config{
    dependsOn: [testGetFileSize]
}
public function testGetFileSizeFromNonExistingFile() {
    int|Error receivedError = clientEp->size("/nonExistingFile");
    if receivedError is Error {
        test:assertTrue(receivedError.message().startsWith("Could not determine the size of "),
            msg = "Unexpected error during the `size` operation of an non-existing file. " + receivedError.message());
    } else {
        test:assertFail(msg = "Found a non-error response while accessing a non-existing file path.");
    }
}

@test:Config{
    dependsOn: [testListFiles]
}
public function testListFilesFromNonExistingDirectory() {
    FileInfo[]|Error receivedError = clientEp->list("/nonExistingDirectory");
    if receivedError is Error {
        test:assertTrue(receivedError.message().startsWith("Could not list the contents of "),
            msg = "Unexpected error during the `list` operation of an non-existing directory. " + receivedError.message());
    } else {
        test:assertFail(msg = "Found a non-error response while accessing a non-existing directory path.");
    }
}

@test:Config{
    dependsOn: [testDeleteFile]
}
public function testDeleteFileAtNonExistingLocation() returns error? {
    Error? receivedError = clientEp->delete("/nonExistingFile");
    if receivedError is Error {
        test:assertTrue(receivedError.message().startsWith("Failed to delete file: "),
            msg = "Unexpected error during the `delete` operation of an non-existing file. " + receivedError.message());
    } else {
         test:assertFail(msg = "Found a non-error response while accessing a non-existing file path.");
    }
}

@test:Config{
    dependsOn: [testRemoveDirectory]
}
public function testRemoveDirectoryWithWrongUrl() {
    Error? receivedError = clientEp->rmdir("/nonExistingDirectory");
    if receivedError is Error {
        test:assertTrue(receivedError.message().startsWith("Failed to delete directory: "),
            msg = "Unexpected error during the `rmdir` operation of a non-existing directory. " + receivedError.message());
    } else {
        test:assertFail(msg = "Found a non-error response while accessing a non-existing directory path.");
    }
}

@test:Config{}
public function testSFTPConnectionToFTPServer() returns error? {
    ClientConfiguration serverConfig = {
        protocol: SFTP,
        host: "127.0.0.1",
        port: 21212,
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            privateKey: {
                path: "tests/resources/sftp.private.key",
                password: "changeit"
            }
        }
    };
    Client|Error clientEp = new(serverConfig);
    if clientEp is Error {
        test:assertTrue(clientEp.message().startsWith("Error while connecting to the FTP server with URL: "),
            msg = "Unexpected error when tried to connect to a existing FTP server via SFTP. " + clientEp.message());
    } else {
        test:assertFail(msg = "Found a non-error response when tried to connect to a existing FTP server via SFTP.");
    }
}
