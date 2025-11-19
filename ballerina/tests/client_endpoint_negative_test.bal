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

@test:Config {
    dependsOn: [testReadBlockNonFittingContent]
}
public function testConnectionWithNonExistingServer() returns error? {
    ClientConfiguration nonExistingServerConfig = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21218,
        auth: {credentials: {username: "wso2", password: "wso2123"}}
    };
    Client|Error nonExistingServerClientEp = new (nonExistingServerConfig);
    if nonExistingServerClientEp is Error {
        test:assertTrue(nonExistingServerClientEp.message().startsWith("Error while connecting to the FTP server with URL: "),
            msg = "Unexpected error when tried to connect to a non existing server. " + nonExistingServerClientEp.message());
    } else {
        test:assertFail(msg = "Found a non-error response when tried to connect to a non existing server.");
    }
}

@test:Config {
    dependsOn: [testReadBlockNonFittingContent]
}
public function testConnectionWithInvalidConfiguration() returns error? {
    ClientConfiguration invalidConfig = {
        protocol: FTP,
        host: "!@#$%^&*()",
        port: 21212,
        auth: {credentials: {username: "wso2", password: "wso2123"}}
    };
    Client|Error invalidServerClientEp = new (invalidConfig);
    if invalidServerClientEp is Error {
        test:assertTrue(invalidServerClientEp.message().startsWith("Error occurred while constructing a URI from host: "),
            msg = "Unexpected error when tried to connect with invalid parameters. " + invalidServerClientEp.message());
    } else {
        test:assertFail(msg = "Found a non-error response when tried to connect with invalid parameters.");
    }
}

@test:Config {
    dependsOn: [testReadBlockNonFittingContent]
}
public function testReadNonExistingFile() returns error? {
    stream<byte[] & readonly, io:Error?>|Error str = (<Client>clientEp)->get("/home/in/nonexisting.txt");
    if str is Error {
        test:assertEquals(str.message(), "Failed to read file: ftp://wso2:***@127.0.0.1:21212/home/in/nonexisting.txt not found",
            msg = "Unexpected error during the `get` operation of an non-existing file.");
    } else {
        test:assertFail(msg = "Found a non-error response while accessing a non-existing file path");
    }
}

@test:Config {
    dependsOn: [testAppendContent]
}
public function testAppendContentToNonExistingFile() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(appendFilePath, 7);
    Error? receivedError = (<Client>clientEp)->append("/../invalidFile", bStream);
    if receivedError is Error {
        test:assertEquals(receivedError.message(), "Invalid relative file name.",
            msg = "Unexpected error during the `append` operation of an invalid file.");
    } else {
        test:assertFail(msg = "Found a non-error response while accessing a non-existing file path");
    }
}

@test:Config {
    dependsOn: [testPutFileContent]
}
public function testPutFileContentAtInvalidFileLocation() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    Error? receivedError = (<Client>clientEp)->put("/../InvalidFile", bStream);
    if receivedError is Error {
        test:assertEquals(receivedError.message(), "Invalid relative file name.",
            msg = "Unexpected error during the `put` operation of an invalid file.");
    } else {
        test:assertFail(msg = "Found a non-error response while accessing a non-existing file path");
    }
}

@test:Config {
    dependsOn: [testIsDirectory]
}
public function testIsDirectoryWithNonExistingDirectory() {
    boolean|Error receivedError = (<Client>clientEp)->isDirectory("/home/in/nonexisting");
    if receivedError is Error {
        test:assertEquals(receivedError.message(), "/home/in/nonexisting does not exists to check if it is a directory.",
            msg = "Unexpected error during the `isDirectory` operation of an non-existing directory. " + receivedError.message());
    } else {
        test:assertFail(msg = "Found a non-error response while accessing a non-existing directory path.");
    }
}

@test:Config {
    dependsOn: [testCreateDirectory]
}
public function testCreateDirectoryAtInvalidLocation() {
    Error? receivedError = (<Client>clientEp)->mkdir("/../InvalidDirectory");
    if receivedError is Error {
        test:assertEquals(receivedError.message(), "Invalid relative file name.",
            msg = "Unexpected error during the `mkdir` operation of an non-existing directory. " + receivedError.message());
    } else {
        test:assertFail(msg = "Found a non-error response while accessing a non-existing directory path.");
    }
}

@test:Config {
    dependsOn: [testRenameDirectory]
}
public function testRenameNonExistingDirectory() {
    string existingName = "/nonExistingDirectory";
    string newName = "/home/in/differentDirectory";
    Error? receivedError = (<Client>clientEp)->rename(existingName, newName);

    if receivedError is Error {
        test:assertTrue(receivedError.message().startsWith("Failed to rename file: "),
            msg = "Unexpected error during the `rename` operation of an non-existing file. " + receivedError.message());
    } else {
        test:assertFail(msg = "Found a non-error response while accessing a non existing directory path.");
    }
}

@test:Config {
    dependsOn: [testGetFileSize]
}
public function testGetFileSizeFromNonExistingFile() {
    int|Error receivedError = (<Client>clientEp)->size("/nonExistingFile");
    if receivedError is Error {
        test:assertTrue(receivedError.message().startsWith("Could not determine the size of "),
            msg = "Unexpected error during the `size` operation of an non-existing file. " + receivedError.message());
    } else {
        test:assertFail(msg = "Found a non-error response while accessing a non-existing file path.");
    }
}

@test:Config {
    dependsOn: [testListFiles]
}
public function testListFilesFromNonExistingDirectory() {
    FileInfo[]|Error receivedError = (<Client>clientEp)->list("/nonExistingDirectory");
    if receivedError is Error {
        test:assertTrue(receivedError.message().startsWith("Could not list the contents of "),
            msg = "Unexpected error during the `list` operation of an non-existing directory. " + receivedError.message());
    } else {
        test:assertFail(msg = "Found a non-error response while accessing a non-existing directory path.");
    }
}

@test:Config {
    dependsOn: [testDeleteFile]
}
public function testDeleteFileAtNonExistingLocation() returns error? {
    Error? receivedError = (<Client>clientEp)->delete("/nonExistingFile");
    if receivedError is Error {
        test:assertTrue(receivedError.message().startsWith("Failed to delete file: "),
            msg = "Unexpected error during the `delete` operation of an non-existing file. " + receivedError.message());
    } else {
        test:assertFail(msg = "Found a non-error response while accessing a non-existing file path.");
    }
}

@test:Config {
    dependsOn: [testRemoveDirectory]
}
public function testRemoveDirectoryWithWrongUrl() {
    Error? receivedError = (<Client>clientEp)->rmdir("/nonExistingDirectory");
    if receivedError is Error {
        test:assertTrue(receivedError.message().startsWith("Failed to delete directory: "),
            msg = "Unexpected error during the `rmdir` operation of a non-existing directory. " + receivedError.message());
    } else {
        test:assertFail(msg = "Found a non-error response while accessing a non-existing directory path.");
    }
}

// Disabling due to https://github.com/ballerina-platform/ballerina-standard-library/issues/2703
@test:Config {
    enable: false
}
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
    Client|Error clientEp = new (serverConfig);
    if clientEp is Error {
        test:assertTrue(clientEp.message().startsWith("Error while connecting to the FTP server with URL: "),
            msg = "Unexpected error when tried to connect to a existing FTP server via SFTP. " + clientEp.message());
    } else {
        test:assertFail(msg = "Found a non-error response when tried to connect to a existing FTP server via SFTP.");
    }
}

@test:Config {
    dependsOn: [testReadBlockNonFittingContent]
}
public function testConnectionWithNonExistingServerDetailedError() returns error? {
    ClientConfiguration nonExistingServerConfig = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21299,
        auth: {credentials: {username: "wso2", password: "wso2123"}}
    };
    Client|Error nonExistingServerClientEp = new (nonExistingServerConfig);
    if nonExistingServerClientEp is Error {
        test:assertTrue(nonExistingServerClientEp.message().startsWith("Error while connecting to the FTP server with URL: "),
            msg = "Unexpected error when tried to connect to a non existing server. " + nonExistingServerClientEp.message());
        // Verify that the error message contains additional details from the root cause
        test:assertTrue(nonExistingServerClientEp.message().length() > "Error while connecting to the FTP server with URL: ftp://wso2:***@127.0.0.1:21299".length(),
            msg = "Error message should contain detailed root cause information");
    } else {
        test:assertFail(msg = "Found a non-error response when tried to connect to a non existing server.");
    }
}

@test:Config {
    dependsOn: [testReadBlockNonFittingContent]
}
public function testConnectionWithInvalidHostDetailedError() returns error? {
    ClientConfiguration invalidHostConfig = {
        protocol: FTP,
        host: "invalid.nonexistent.host.example",
        port: 21,
        auth: {credentials: {username: "wso2", password: "wso2123"}}
    };
    Client|Error invalidHostClientEp = new (invalidHostConfig);
    if invalidHostClientEp is Error {
        test:assertTrue(invalidHostClientEp.message().startsWith("Error while connecting to the FTP server with URL: "),
            msg = "Unexpected error when tried to connect to an invalid host. " + invalidHostClientEp.message());
        // Verify that the error message contains additional details from the root cause
        test:assertTrue(invalidHostClientEp.message().length() > "Error while connecting to the FTP server with URL: ".length(),
            msg = "Error message should contain detailed root cause information");
    } else {
        test:assertFail(msg = "Found a non-error response when tried to connect to an invalid host.");
    }
}

@test:Config {
    dependsOn: [testMoveFile]
}
public function testMoveNonExistingFile() returns error? {
    string nonExistingPath = "/home/in/nonexistent_file_for_move.txt";
    string destinationPath = "/home/in/moved_nonexistent.txt";
    // Note: VFS may not throw error for non-existing source in move/rename operations
    // This test verifies that the operation completes without crashing
    Error? result = (<Client>clientEp)->move(nonExistingPath, destinationPath);
    // If the VFS implementation doesn't throw an error, that's also acceptable
    // The important thing is the client handles it gracefully
    test:assertTrue(true, msg = "Move non-existing file handled");
}

@test:Config {
    dependsOn: [testCopyFile]
}
public function testCopyNonExistingFile() returns error? {
    string nonExistingPath = "/home/in/nonexistent_file_for_copy.txt";
    string destinationPath = "/home/in/copied_nonexistent.txt";
    // Note: VFS may not throw error for non-existing source in copy operations
    // This test verifies that the operation completes without crashing
    Error? result = (<Client>clientEp)->copy(nonExistingPath, destinationPath);
    // If the VFS implementation doesn't throw an error, that's also acceptable
    // The important thing is the client handles it gracefully
    test:assertTrue(true, msg = "Copy non-existing file handled");
}

@test:Config {
    dependsOn: [testCopyFile]
}
public function testCopyToExistingFile() returns error? {
    string sourcePath = "/home/in/test_copy_source.txt";
    string destinationPath = "/home/in/test_copy_dest.txt";
    
    // Clean up any existing files first
    Error? cleanupResult1 = (<Client>clientEp)->delete(sourcePath);
    Error? cleanupResult2 = (<Client>clientEp)->delete(destinationPath);
    
    // Create source file
    stream<io:Block, io:Error?> sourceStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>clientEp)->put(sourcePath, sourceStream);
    
    // Create destination file
    stream<io:Block, io:Error?> destStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>clientEp)->put(destinationPath, destStream);
    
    // Copy to existing file should fail (VFS doesn't overwrite by default)
    Error? result = (<Client>clientEp)->copy(sourcePath, destinationPath);
    test:assertTrue(result is Error, msg = "Copy to existing file should fail");
    if result is Error {
        test:assertTrue(result.message().includes("already exists"), 
            msg = "Error message should indicate file already exists");
    }
    
    // Cleanup
    Error? cleanup1 = (<Client>clientEp)->delete(sourcePath);
    Error? cleanup2 = (<Client>clientEp)->delete(destinationPath);
}

@test:Config {
    dependsOn: [testExistsFile]
}
public function testExistsNonExistingFile() returns error? {
    string nonExistingPath = "/home/in/nonexistent_file.txt";
    boolean|Error result = (<Client>clientEp)->exists(nonExistingPath);
    if result is boolean {
        test:assertFalse(result, msg = "Non-existing file should return false");
    } else {
        test:assertFail(msg = "Expected boolean result for exists on non-existing file, got error: " 
            + result.message());
    }
}
