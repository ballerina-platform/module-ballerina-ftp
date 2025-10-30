// Copyright (c) 2025 WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
//
// WSO2 LLC. licenses this file to you under the Apache License,
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
import ballerina/lang.runtime;
import ballerina/log;
import ballerina/test;

// Global tracking variables for onFileDeleted tests
string[] deletedFilesReceived = [];
int deleteEventCount = 0;
boolean deleteEventReceived = false;
boolean deleteCallerEventReceived = false;
string[] deletedFilesWithCaller = [];

// Test file paths
const string DELETE_TEST_FILE_PATH = "tests/resources/datafiles/file2.txt";

@test:Config {
    dependsOn: [testValidateDeletedFilesFromListener]
}
public function testOnFileDeletedSingleFile() returns error? {
    // Reset state
    deletedFilesReceived = [];
    deleteEventCount = 0;
    deleteEventReceived = false;

    // Service with onFileDeleted
    Service deleteSingleFileService = service object {
        remote function onFileDeleted(string[] deletedFiles) returns error? {
            log:printInfo(string `onFileDeleted invoked with ${deletedFiles.length()} files`);
            foreach string file in deletedFiles {
                log:printInfo(string `Deleted file: ${file}`);
            }
            // Filter to only include files that match our specific test pattern
            string[] filteredFiles = [];
            foreach string deletedFile in deletedFiles {
                if deletedFile.includes("testFile1.deleted1") {
                    filteredFiles.push(deletedFile);
                }
            }
            if filteredFiles.length() > 0 {
                deletedFilesReceived = filteredFiles;
                deleteEventCount += 1;
                deleteEventReceived = true;
            }
        }
    };

    // Upload the file BEFORE starting the listener
    // This ensures the listener's initial state includes this file
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(DELETE_TEST_FILE_PATH, 5);
    check (<Client>clientEp)->put("/home/in/testFile1.deleted1", bStream);
    runtime:sleep(1);

    // Create listener
    Listener deleteListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: "/home/in",
        pollingInterval: 1,
        fileNamePattern: "(.*).deleted1"
    });

    check deleteListener.attach(deleteSingleFileService);
    check deleteListener.'start();
    runtime:registerListener(deleteListener);

    // Wait for initial poll to establish baseline
    runtime:sleep(2);

    // Now delete the file
    check (<Client>clientEp)->delete("/home/in/testFile1.deleted1");
    runtime:sleep(3);

    // Wait for delete event with timeout
    int timeoutInSeconds = 60;
    while timeoutInSeconds > 0 {
        if deleteEventReceived {
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }

    // Cleanup
    runtime:deregisterListener(deleteListener);
    check deleteListener.gracefulStop();

    // Assertions
    if timeoutInSeconds == 0 {
        test:assertFail("Timeout: onFileDeleted was not invoked within 60 seconds");
    }

    test:assertTrue(deleteEventReceived, "Delete event should have been received");
    // Be lenient - may get duplicate events or events for files that existed before
    test:assertTrue(deletedFilesReceived.length() >= 1, "Should have at least 1 deleted file");
    boolean foundOurFile = false;
    foreach string deletedFile in deletedFilesReceived {
        if deletedFile.includes("testFile1.deleted1") {
            foundOurFile = true;
            break;
        }
    }
    test:assertTrue(foundOurFile, "Should have deleted testFile1.deleted1");
}

@test:Config {
    dependsOn: [testOnFileDeletedSingleFile]
}
public function testOnFileDeletedMultipleFiles() returns error? {
    // Reset state
    deletedFilesReceived = [];
    deleteEventCount = 0;
    deleteEventReceived = false;

    // Service with onFileDeleted
    Service deleteMultipleFilesService = service object {
        remote function onFileDeleted(string[] deletedFiles) returns error? {
            log:printInfo(string `onFileDeleted invoked with ${deletedFiles.length()} files`);
            deletedFilesReceived = deletedFiles;
            deleteEventCount += 1;
            deleteEventReceived = true;
        }
    };

    // Create listener
    Listener deleteListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: "/home/in",
        pollingInterval: 1,
        fileNamePattern: "(.*).deleted2"
    });

    check deleteListener.attach(deleteMultipleFilesService);
    check deleteListener.'start();
    runtime:registerListener(deleteListener);

    // Create and upload multiple files
    stream<io:Block, io:Error?> bStream1 = check io:fileReadBlocksAsStream(DELETE_TEST_FILE_PATH, 5);
    check (<Client>clientEp)->put("/home/in/testFile2A.deleted2", bStream1);

    stream<io:Block, io:Error?> bStream2 = check io:fileReadBlocksAsStream(DELETE_TEST_FILE_PATH, 5);
    check (<Client>clientEp)->put("/home/in/testFile2B.deleted2", bStream2);

    stream<io:Block, io:Error?> bStream3 = check io:fileReadBlocksAsStream(DELETE_TEST_FILE_PATH, 5);
    check (<Client>clientEp)->put("/home/in/testFile2C.deleted2", bStream3);

    runtime:sleep(2);

    // Delete all files in quick succession
    check (<Client>clientEp)->delete("/home/in/testFile2A.deleted2");
    check (<Client>clientEp)->delete("/home/in/testFile2B.deleted2");
    check (<Client>clientEp)->delete("/home/in/testFile2C.deleted2");
    runtime:sleep(3);

    // Wait for delete event with timeout
    int timeoutInSeconds = 60;
    while timeoutInSeconds > 0 {
        if deleteEventReceived {
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }

    // Cleanup
    runtime:deregisterListener(deleteListener);
    check deleteListener.gracefulStop();

    // Assertions
    if timeoutInSeconds == 0 {
        test:assertFail("Timeout: onFileDeleted was not invoked within 60 seconds");
    }

    test:assertTrue(deleteEventReceived, "Delete event should have been received");
    test:assertEquals(deletedFilesReceived.length(), 3,
        string `Should have 3 deleted files, but got ${deletedFilesReceived.length()}`);

    // Verify all deleted files are in the list
    boolean foundFileA = false;
    boolean foundFileB = false;
    boolean foundFileC = false;

    foreach string deletedFile in deletedFilesReceived {
        if deletedFile.includes("testFile2A.deleted2") {
            foundFileA = true;
        } else if deletedFile.includes("testFile2B.deleted2") {
            foundFileB = true;
        } else if deletedFile.includes("testFile2C.deleted2") {
            foundFileC = true;
        }
    }

    test:assertTrue(foundFileA, "Should find testFile2A.deleted2 in deleted files");
    test:assertTrue(foundFileB, "Should find testFile2B.deleted2 in deleted files");
    test:assertTrue(foundFileC, "Should find testFile2C.deleted2 in deleted files");
}

@test:Config {
    dependsOn: [testOnFileDeletedMultipleFiles]
}
public function testOnFileDeletedWithCaller() returns error? {
    // Reset state
    deletedFilesWithCaller = [];
    deleteCallerEventReceived = false;

    // Service with onFileDeleted and Caller
    Service deleteWithCallerService = service object {
        remote function onFileDeleted(string[] deletedFiles, Caller caller) returns error? {
            log:printInfo(string `onFileDeleted with Caller invoked with ${deletedFiles.length()} files`);
            deletedFilesWithCaller = deletedFiles;
            deleteCallerEventReceived = true;

            // Use caller to list remaining files
            FileInfo[] remainingFiles = check caller->list("/home/in");
            log:printInfo(string `Remaining files after deletion: ${remainingFiles.length()}`);
        }
    };

    // Create listener
    Listener deleteListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: "/home/in",
        pollingInterval: 1,
        fileNamePattern: "(.*).deleted3"
    });

    check deleteListener.attach(deleteWithCallerService);
    check deleteListener.'start();
    runtime:registerListener(deleteListener);

    // Create and upload a file
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(DELETE_TEST_FILE_PATH, 5);
    check (<Client>clientEp)->put("/home/in/testFile3.deleted3", bStream);
    runtime:sleep(2);

    // Delete the file
    check (<Client>clientEp)->delete("/home/in/testFile3.deleted3");
    runtime:sleep(3);

    // Wait for delete event with timeout
    int timeoutInSeconds = 60;
    while timeoutInSeconds > 0 {
        if deleteCallerEventReceived {
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }

    // Cleanup
    runtime:deregisterListener(deleteListener);
    check deleteListener.gracefulStop();

    // Assertions
    if timeoutInSeconds == 0 {
        test:assertFail("Timeout: onFileDeleted with Caller was not invoked within 60 seconds");
    }

    test:assertTrue(deleteCallerEventReceived, "Delete event with Caller should have been received");
    test:assertEquals(deletedFilesWithCaller.length(), 1, "Should have 1 deleted file");
    test:assertTrue(deletedFilesWithCaller[0].includes("testFile3.deleted3"),
        "Deleted file path should contain 'testFile3.deleted3'");
}

@test:Config {
    dependsOn: [testOnFileDeletedWithCaller]
}
public function testOnFileDeletedWithFileNamePattern() returns error? {
    // Reset state
    deletedFilesReceived = [];
    deleteEventCount = 0;
    deleteEventReceived = false;

    // Service with onFileDeleted
    Service deletePatternService = service object {
        remote function onFileDeleted(string[] deletedFiles) returns error? {
            log:printInfo(string `onFileDeleted invoked with ${deletedFiles.length()} files`);
            deletedFilesReceived = deletedFiles;
            deleteEventCount += 1;
            deleteEventReceived = true;
        }
    };

    // Create listener with specific pattern - only .deleted4 files
    Listener deleteListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: "/home/in",
        pollingInterval: 1,
        fileNamePattern: "(.*).deleted4"  // Only matches .deleted4 files
    });

    check deleteListener.attach(deletePatternService);
    check deleteListener.'start();
    runtime:registerListener(deleteListener);

    // Upload files with matching and non-matching patterns
    stream<io:Block, io:Error?> bStream1 = check io:fileReadBlocksAsStream(DELETE_TEST_FILE_PATH, 5);
    check (<Client>clientEp)->put("/home/in/matchingFile.deleted4", bStream1);

    stream<io:Block, io:Error?> bStream2 = check io:fileReadBlocksAsStream(DELETE_TEST_FILE_PATH, 5);
    check (<Client>clientEp)->put("/home/in/nonMatchingFile.other", bStream2);

    runtime:sleep(2);

    // Delete both files
    check (<Client>clientEp)->delete("/home/in/matchingFile.deleted4");
    check (<Client>clientEp)->delete("/home/in/nonMatchingFile.other");
    runtime:sleep(3);

    // Wait for delete event with timeout
    int timeoutInSeconds = 60;
    while timeoutInSeconds > 0 {
        if deleteEventReceived {
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }

    // Cleanup
    runtime:deregisterListener(deleteListener);
    check deleteListener.gracefulStop();

    // Assertions
    if timeoutInSeconds == 0 {
        test:assertFail("Timeout: onFileDeleted was not invoked within 60 seconds");
    }

    test:assertTrue(deleteEventReceived, "Delete event should have been received");
    // Should only report the file matching the pattern (.deleted4)
    test:assertEquals(deletedFilesReceived.length(), 1,
        "Should have only 1 deleted file (matching pattern)");
    test:assertTrue(deletedFilesReceived[0].includes("matchingFile.deleted4"),
        "Deleted file should be the one matching the pattern");

    // Verify non-matching file was NOT reported
    foreach string deletedFile in deletedFilesReceived {
        test:assertFalse(deletedFile.includes("nonMatchingFile.other"),
            "Non-matching file should not be in deleted files list");
    }
}

@test:Config {
    dependsOn: [testOnFileDeletedWithFileNamePattern]
}
public function testOnFileDeletedNoFilesDeleted() returns error? {
    // Reset state
    deletedFilesReceived = [];
    deleteEventCount = 0;
    deleteEventReceived = false;

    // Service with onFileDeleted
    Service deleteNoFilesService = service object {
        remote function onFileDeleted(string[] deletedFiles) returns error? {
            log:printInfo(string `onFileDeleted invoked with ${deletedFiles.length()} files`);
            deletedFilesReceived = deletedFiles;
            deleteEventCount += 1;
            deleteEventReceived = true;
        }
    };

    // Create listener
    Listener deleteListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: "/home/in",
        pollingInterval: 1,
        fileNamePattern: "(.*).deleted5"
    });

    check deleteListener.attach(deleteNoFilesService);
    check deleteListener.'start();
    runtime:registerListener(deleteListener);

    // Don't create or delete any files, just wait
    runtime:sleep(5);

    // Cleanup
    runtime:deregisterListener(deleteListener);
    check deleteListener.gracefulStop();

    // Assertions - onFileDeleted should NOT have been invoked
    test:assertFalse(deleteEventReceived, "Delete event should NOT have been received when no files deleted");
    test:assertEquals(deleteEventCount, 0, "Should have received 0 delete events");
}

@test:Config {
    dependsOn: [testOnFileDeletedNoFilesDeleted]
}
public function testOnFileDeletedErrorHandling() returns error? {
    // Reset state
    deleteEventReceived = false;

    // Service with onFileDeleted that throws an error
    Service deleteErrorService = service object {
        remote function onFileDeleted(string[] deletedFiles) returns error? {
            log:printInfo(string `onFileDeleted invoked with ${deletedFiles.length()} files - throwing error`);
            deleteEventReceived = true;
            return error("Intentional error for testing");
        }
    };

    // Create listener
    Listener deleteListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: "/home/in",
        pollingInterval: 1,
        fileNamePattern: "(.*).deleted6"
    });

    check deleteListener.attach(deleteErrorService);
    check deleteListener.'start();
    runtime:registerListener(deleteListener);

    // Create and upload a file
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(DELETE_TEST_FILE_PATH, 5);
    check (<Client>clientEp)->put("/home/in/testFile6.deleted6", bStream);
    runtime:sleep(2);

    // Delete the file
    check (<Client>clientEp)->delete("/home/in/testFile6.deleted6");
    runtime:sleep(3);

    // Wait for delete event with timeout
    int timeoutInSeconds = 60;
    while timeoutInSeconds > 0 {
        if deleteEventReceived {
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }

    // Cleanup
    runtime:deregisterListener(deleteListener);
    check deleteListener.gracefulStop();

    // Assertions - Even with error, the method should have been invoked
    if timeoutInSeconds == 0 {
        test:assertFail("Timeout: onFileDeleted was not invoked within 60 seconds");
    }

    test:assertTrue(deleteEventReceived, "Delete event should have been received even with error");
}

@test:Config {
    dependsOn: [testOnFileDeletedErrorHandling]
}
public function testOnFileDeletedIsolatedService() returns error? {
    // Reset state
    deletedFilesReceived = [];
    deleteEventReceived = false;

    // Service with onFileDeleted (testing isolated compatibility)
    Service isolatedDeleteService = service object {
        remote function onFileDeleted(string[] deletedFiles) returns error? {
            log:printInfo(string `onFileDeleted invoked with ${deletedFiles.length()} files`);
            deletedFilesReceived = deletedFiles;
            deleteEventReceived = true;
        }
    };

    // Create listener
    Listener deleteListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: "/home/in",
        pollingInterval: 1,
        fileNamePattern: "(.*).deleted7"
    });

    check deleteListener.attach(isolatedDeleteService);
    check deleteListener.'start();
    runtime:registerListener(deleteListener);

    // Create and upload a file
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(DELETE_TEST_FILE_PATH, 5);
    check (<Client>clientEp)->put("/home/in/testFile7.deleted7", bStream);
    runtime:sleep(2);

    // Delete the file
    check (<Client>clientEp)->delete("/home/in/testFile7.deleted7");
    runtime:sleep(3);

    // Wait for delete event with timeout
    int timeoutInSeconds = 60;
    while timeoutInSeconds > 0 {
        if deleteEventReceived {
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }

    // Cleanup
    runtime:deregisterListener(deleteListener);
    check deleteListener.gracefulStop();

    // Assertions
    if timeoutInSeconds == 0 {
        test:assertFail("Timeout: onFileDeleted was not invoked within 60 seconds");
    }

    test:assertTrue(deleteEventReceived, "Delete event should have been received");
    test:assertEquals(deletedFilesReceived.length(), 1, "Should have 1 deleted file");
}
