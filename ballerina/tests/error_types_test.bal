// Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/io;
import ballerina/test;

// Test FileAlreadyExistsError type when creating a directory that already exists
@test:Config {
    dependsOn: [testCreateDirectory]
}
public function testFileAlreadyExistsErrorOnMkdir() returns error? {
    string dirPath = "/home/in/test_existing_dir";

    // First create the directory
    Error? createResult = (<Client>clientEp)->mkdir(dirPath);

    // Try to create it again - should fail with FileAlreadyExistsError
    Error? result = (<Client>clientEp)->mkdir(dirPath);
    test:assertTrue(result is FileAlreadyExistsError,
        msg = "Expected FileAlreadyExistsError when creating existing directory");
    if result is FileAlreadyExistsError {
        test:assertTrue(result.message().includes("Directory exists"),
            msg = "FileAlreadyExistsError message should indicate directory exists");
    }

    // Cleanup
    Error? cleanup = (<Client>clientEp)->rmdir(dirPath);
}

// Test FileAlreadyExistsError type when renaming/moving to an existing destination
@test:Config {
    dependsOn: [testRenameDirectory]
}
public function testFileAlreadyExistsErrorOnRename() returns error? {
    string sourcePath = "/home/in/test_rename_error_src.txt";
    string destPath = "/home/in/test_rename_error_dest.txt";

    // Clean up first
    Error? cleanup1 = (<Client>clientEp)->delete(sourcePath);
    Error? cleanup2 = (<Client>clientEp)->delete(destPath);

    // Create source file
    stream<io:Block, io:Error?> srcStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>clientEp)->put(sourcePath, srcStream);

    // Create destination file
    stream<io:Block, io:Error?> destStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>clientEp)->put(destPath, destStream);

    // Try to rename to existing destination - should fail with FileAlreadyExistsError
    Error? result = (<Client>clientEp)->rename(sourcePath, destPath);
    test:assertTrue(result is FileAlreadyExistsError,
        msg = "Expected FileAlreadyExistsError when renaming to existing file");
    if result is FileAlreadyExistsError {
        test:assertTrue(result.message().includes("already exists"),
            msg = "FileAlreadyExistsError message should indicate file already exists");
    }

    // Cleanup
    Error? cleanupSrc = (<Client>clientEp)->delete(sourcePath);
    Error? cleanupDest = (<Client>clientEp)->delete(destPath);
}
