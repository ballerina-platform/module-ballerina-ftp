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

import ballerina/ftp;
import ballerina/lang.runtime;
import ballerina/log;
import ballerina/test;

// Test directory for post-process action tests
const POST_PROCESS_TEST_DIR = "/home/in/post-process";
const POST_PROCESS_ARCHIVE_DIR = "/home/in/post-process-archive";
const POST_PROCESS_ERROR_DIR = "/home/in/post-process-error";

// Global tracking variables for post-process tests
boolean deleteAfterProcessInvoked = false;
boolean moveAfterProcessInvoked = false;
boolean deleteAfterErrorInvoked = false;
boolean moveAfterErrorInvoked = false;
string lastProcessedFilePath = "";

@test:Config {
}
public function testAfterProcessDelete() returns error? {
    // Reset state
    deleteAfterProcessInvoked = false;
    lastProcessedFilePath = "";

    // Service with afterProcess: DELETE
    ftp:Service deleteAfterProcessService = service object {
        @ftp:FunctionConfig {
            fileNamePattern: "delete-test.*\\.json",
            afterProcess: ftp:DELETE
        }
        remote function onFileJson(json content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
            log:printInfo(string `Processing file for deletion: ${fileInfo.path}`);
            lastProcessedFilePath = fileInfo.path;
            deleteAfterProcessInvoked = true;
        }
    };

    // Create listener
    ftp:Listener deleteListener = check new ({
        protocol: ftp:FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: POST_PROCESS_TEST_DIR,
        pollingInterval: 4,
        fileNamePattern: "delete-test.*\\.json"
    });

    check deleteListener.attach(deleteAfterProcessService);
    check deleteListener.'start();
    runtime:registerListener(deleteListener);

    // Upload test file
    string testFilePath = POST_PROCESS_TEST_DIR + "/delete-test.json";
    check (<ftp:Client>triggerClient)->putJson(testFilePath, <json>{"name": "test", "action": "delete"});
    runtime:sleep(15);

    // Cleanup listener
    runtime:deregisterListener(deleteListener);
    check deleteListener.gracefulStop();

    // Verify file was processed
    test:assertTrue(deleteAfterProcessInvoked, "onFileJson should have been invoked");
    test:assertTrue(lastProcessedFilePath.includes("delete-test"), "Should have processed delete-test file");

    // Verify file was deleted
    boolean fileExists = check (<ftp:Client>triggerClient)->exists(testFilePath);
    test:assertFalse(fileExists, "File should have been deleted after processing");
}

@test:Config {
    dependsOn: [testAfterProcessDelete]
}
public function testAfterProcessMove() returns error? {
    // Reset state
    moveAfterProcessInvoked = false;
    lastProcessedFilePath = "";

    // Create archive directory
    boolean archiveExists = check (<ftp:Client>triggerClient)->exists(POST_PROCESS_ARCHIVE_DIR);
    if !archiveExists {
        check (<ftp:Client>triggerClient)->mkdir(POST_PROCESS_ARCHIVE_DIR);
    }

    // Service with afterProcess: MOVE
    ftp:Service moveAfterProcessService = service object {
        @ftp:FunctionConfig {
            fileNamePattern: "move-test.*\\.json",
            afterProcess: {
                moveTo: POST_PROCESS_ARCHIVE_DIR
            }
        }
        remote function onFileJson(json content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
            log:printInfo(string `Processing file for move: ${fileInfo.path}`);
            lastProcessedFilePath = fileInfo.path;
            moveAfterProcessInvoked = true;
        }
    };

    // Create listener
    ftp:Listener moveListener = check new ({
        protocol: ftp:FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: POST_PROCESS_TEST_DIR,
        pollingInterval: 4,
        fileNamePattern: "move-test.*\\.json"
    });

    check moveListener.attach(moveAfterProcessService);
    check moveListener.'start();
    runtime:registerListener(moveListener);

    // Upload test file
    string testFilePath = POST_PROCESS_TEST_DIR + "/move-test.json";
    string archiveFilePath = POST_PROCESS_ARCHIVE_DIR + "/move-test.json";
    check (<ftp:Client>triggerClient)->putJson(testFilePath, <json>{"name": "test", "action": "move"});
    runtime:sleep(15);

    // Cleanup listener
    runtime:deregisterListener(moveListener);
    check moveListener.gracefulStop();

    // Verify file was processed
    test:assertTrue(moveAfterProcessInvoked, "onFileJson should have been invoked");
    test:assertTrue(lastProcessedFilePath.includes("move-test"), "Should have processed move-test file");

    // Verify file was moved
    boolean sourceExists = check (<ftp:Client>triggerClient)->exists(testFilePath);
    test:assertFalse(sourceExists, "Source file should not exist after move");

    boolean archiveExists2 = check (<ftp:Client>triggerClient)->exists(archiveFilePath);
    test:assertTrue(archiveExists2, "File should exist in archive directory after move");

    // Cleanup
    check (<ftp:Client>triggerClient)->delete(archiveFilePath);
}

@test:Config {
    dependsOn: [testAfterProcessMove]
}
public function testAfterProcessMoveWithPreserveSubDirsFalse() returns error? {
    // Reset state
    moveAfterProcessInvoked = false;
    lastProcessedFilePath = "";

    // Create subdirectory for testing
    string subDir = POST_PROCESS_TEST_DIR + "/subdir";
    boolean subDirExists = check (<ftp:Client>triggerClient)->exists(subDir);
    if !subDirExists {
        check (<ftp:Client>triggerClient)->mkdir(subDir);
    }

    // Service with afterProcess: MOVE with preserveSubDirs: false
    ftp:Service moveNoPreserveService = service object {
        @ftp:FunctionConfig {
            fileNamePattern: "nopreserve-test.*\\.json",
            afterProcess: {
                moveTo: POST_PROCESS_ARCHIVE_DIR,
                preserveSubDirs: false
            }
        }
        remote function onFileJson(json content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
            log:printInfo(string `Processing file (no preserve): ${fileInfo.path}`);
            lastProcessedFilePath = fileInfo.path;
            moveAfterProcessInvoked = true;
        }
    };

    // Create listener for subdirectory
    ftp:Listener noPreserveListener = check new ({
        protocol: ftp:FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: POST_PROCESS_TEST_DIR,
        pollingInterval: 4,
        fileNamePattern: "(subdir|nopreserve-test.*\\.json)"
    });

    check noPreserveListener.attach(moveNoPreserveService);
    check noPreserveListener.'start();
    runtime:registerListener(noPreserveListener);

    // Upload test file to subdirectory
    string testFilePath = subDir + "/nopreserve-test.json";
    string archiveFilePath = POST_PROCESS_ARCHIVE_DIR + "/nopreserve-test.json";
    check (<ftp:Client>triggerClient)->putJson(testFilePath, <json>{"name": "test", "preserveSubDirs": false});
    runtime:sleep(15);

    // Cleanup listener
    runtime:deregisterListener(noPreserveListener);
    check noPreserveListener.gracefulStop();

    // Verify file was processed
    test:assertTrue(moveAfterProcessInvoked, "onFileJson should have been invoked");

    // Verify file was moved directly to archive (not preserving subdirectory)
    boolean sourceExists = check (<ftp:Client>triggerClient)->exists(testFilePath);
    test:assertFalse(sourceExists, "Source file should not exist after move");

    boolean archiveExists = check (<ftp:Client>triggerClient)->exists(archiveFilePath);
    test:assertTrue(archiveExists, "File should exist directly in archive directory (not in subdir)");

    // Cleanup
    check (<ftp:Client>triggerClient)->delete(archiveFilePath);
}

@test:Config {
    dependsOn: [testAfterProcessMoveWithPreserveSubDirsFalse]
}
public function testAfterProcessMoveWithPreserveSubDirsTrue() returns error? {
    // Reset state
    moveAfterProcessInvoked = false;
    lastProcessedFilePath = "";

    // Create subdirectory for testing
    string subDir = POST_PROCESS_TEST_DIR + "/preserve-subdir";
    boolean subDirExists = check (<ftp:Client>triggerClient)->exists(subDir);
    if !subDirExists {
        check (<ftp:Client>triggerClient)->mkdir(subDir);
    }

    // Service with afterProcess: MOVE (preserveSubDirs defaults to true)
    ftp:Service movePreserveService = service object {
        @ftp:FunctionConfig {
            fileNamePattern: "preserve-test.*\\.json",
            afterProcess: {
                moveTo: POST_PROCESS_ARCHIVE_DIR
            }
        }
        remote function onFileJson(json content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
            log:printInfo(string `Processing file (preserve): ${fileInfo.path}`);
            lastProcessedFilePath = fileInfo.path;
            moveAfterProcessInvoked = true;
        }
    };

    // Create listener for subdirectory
    ftp:Listener preserveListener = check new ({
        protocol: ftp:FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: POST_PROCESS_TEST_DIR,
        pollingInterval: 4,
        fileNamePattern: "(preserve-subdir|preserve-test.*\\.json)"
    });

    check preserveListener.attach(movePreserveService);
    check preserveListener.'start();
    runtime:registerListener(preserveListener);

    // Upload test file to subdirectory
    string testFilePath = subDir + "/preserve-test.json";
    string archiveFilePath = POST_PROCESS_ARCHIVE_DIR + "/preserve-subdir/preserve-test.json";
    check (<ftp:Client>triggerClient)->putJson(testFilePath, <json>{"name": "test", "preserveSubDirs": true});
    runtime:sleep(15);

    // Cleanup listener
    runtime:deregisterListener(preserveListener);
    check preserveListener.gracefulStop();

    // Verify file was processed
    test:assertTrue(moveAfterProcessInvoked, "onFileJson should have been invoked");

    // Verify file was moved preserving subdirectory
    boolean sourceExists = check (<ftp:Client>triggerClient)->exists(testFilePath);
    test:assertFalse(sourceExists, "Source file should not exist after move");

    boolean archiveExists = check (<ftp:Client>triggerClient)->exists(archiveFilePath);
    test:assertTrue(archiveExists, "File should exist in archive subdirectory after move");

    // Cleanup
    check (<ftp:Client>triggerClient)->delete(archiveFilePath);
    check (<ftp:Client>triggerClient)->rmdir(POST_PROCESS_ARCHIVE_DIR + "/preserve-subdir");
    check (<ftp:Client>triggerClient)->rmdir(subDir);
}

@test:Config {
    dependsOn: [testAfterProcessMoveWithPreserveSubDirsTrue]
}
public function testAfterErrorDelete() returns error? {
    // Reset state
    deleteAfterErrorInvoked = false;
    lastProcessedFilePath = "";

    // Service with afterError: DELETE that throws an error
    ftp:Service errorDeleteService = service object {
        @ftp:FunctionConfig {
            fileNamePattern: "error-delete-test.*\\.json",
            afterError: ftp:DELETE
        }
        remote function onFileJson(json content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
            log:printInfo(string `Processing file that will error: ${fileInfo.path}`);
            lastProcessedFilePath = fileInfo.path;
            deleteAfterErrorInvoked = true;
            // Return an error to trigger afterError action
            return error("Simulated processing error");
        }
    };

    // Create listener
    ftp:Listener errorDeleteListener = check new ({
        protocol: ftp:FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: POST_PROCESS_TEST_DIR,
        pollingInterval: 4,
        fileNamePattern: "error-delete-test.*\\.json"
    });

    check errorDeleteListener.attach(errorDeleteService);
    check errorDeleteListener.'start();
    runtime:registerListener(errorDeleteListener);

    // Upload test file
    string testFilePath = POST_PROCESS_TEST_DIR + "/error-delete-test.json";
    check (<ftp:Client>triggerClient)->putJson(testFilePath, <json>{"name": "test", "action": "error-delete"});
    runtime:sleep(15);

    // Cleanup listener
    runtime:deregisterListener(errorDeleteListener);
    check errorDeleteListener.gracefulStop();

    // Verify file was processed (even though it errored)
    test:assertTrue(deleteAfterErrorInvoked, "onFileJson should have been invoked");

    // Verify file was deleted after error
    boolean fileExists = check (<ftp:Client>triggerClient)->exists(testFilePath);
    test:assertFalse(fileExists, "File should have been deleted after error");
}

@test:Config {
    dependsOn: [testAfterErrorDelete]
}
public function testAfterErrorMove() returns error? {
    // Reset state
    moveAfterErrorInvoked = false;
    lastProcessedFilePath = "";

    // Create error directory
    boolean errorDirExists = check (<ftp:Client>triggerClient)->exists(POST_PROCESS_ERROR_DIR);
    if !errorDirExists {
        check (<ftp:Client>triggerClient)->mkdir(POST_PROCESS_ERROR_DIR);
    }

    // Service with afterError: MOVE that throws an error
    ftp:Service errorMoveService = service object {
        @ftp:FunctionConfig {
            fileNamePattern: "error-move-test.*\\.json",
            afterError: {
                moveTo: POST_PROCESS_ERROR_DIR
            }
        }
        remote function onFileJson(json content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
            log:printInfo(string `Processing file that will error and move: ${fileInfo.path}`);
            lastProcessedFilePath = fileInfo.path;
            moveAfterErrorInvoked = true;
            // Return an error to trigger afterError action
            return error("Simulated processing error for move");
        }
    };

    // Create listener
    ftp:Listener errorMoveListener = check new ({
        protocol: ftp:FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: POST_PROCESS_TEST_DIR,
        pollingInterval: 4,
        fileNamePattern: "error-move-test.*\\.json"
    });

    check errorMoveListener.attach(errorMoveService);
    check errorMoveListener.'start();
    runtime:registerListener(errorMoveListener);

    // Upload test file
    string testFilePath = POST_PROCESS_TEST_DIR + "/error-move-test.json";
    string errorFilePath = POST_PROCESS_ERROR_DIR + "/error-move-test.json";
    check (<ftp:Client>triggerClient)->putJson(testFilePath, <json>{"name": "test", "action": "error-move"});
    runtime:sleep(15);

    // Cleanup listener
    runtime:deregisterListener(errorMoveListener);
    check errorMoveListener.gracefulStop();

    // Verify file was processed
    test:assertTrue(moveAfterErrorInvoked, "onFileJson should have been invoked");

    // Verify file was moved to error directory
    boolean sourceExists = check (<ftp:Client>triggerClient)->exists(testFilePath);
    test:assertFalse(sourceExists, "Source file should not exist after error move");

    boolean errorExists = check (<ftp:Client>triggerClient)->exists(errorFilePath);
    test:assertTrue(errorExists, "File should exist in error directory after error move");

    // Cleanup
    check (<ftp:Client>triggerClient)->delete(errorFilePath);
}

@test:Config {
    dependsOn: [testAfterErrorMove]
}
public function testBothAfterProcessAndAfterError() returns error? {
    // Reset state
    moveAfterProcessInvoked = false;
    lastProcessedFilePath = "";

    // Service with both afterProcess and afterError configured
    ftp:Service bothActionsService = service object {
        @ftp:FunctionConfig {
            fileNamePattern: "both-success-test.*\\.json",
            afterProcess: {
                moveTo: POST_PROCESS_ARCHIVE_DIR
            },
            afterError: {
                moveTo: POST_PROCESS_ERROR_DIR
            }
        }
        remote function onFileJson(json content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
            log:printInfo(string `Processing file with both actions: ${fileInfo.path}`);
            lastProcessedFilePath = fileInfo.path;
            moveAfterProcessInvoked = true;
            // Success case - should trigger afterProcess
        }
    };

    // Create listener
    ftp:Listener bothListener = check new ({
        protocol: ftp:FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: POST_PROCESS_TEST_DIR,
        pollingInterval: 4,
        fileNamePattern: "both-success-test.*\\.json"
    });

    check bothListener.attach(bothActionsService);
    check bothListener.'start();
    runtime:registerListener(bothListener);

    // Upload test file
    string testFilePath = POST_PROCESS_TEST_DIR + "/both-success-test.json";
    string archiveFilePath = POST_PROCESS_ARCHIVE_DIR + "/both-success-test.json";
    string errorFilePath = POST_PROCESS_ERROR_DIR + "/both-success-test.json";
    check (<ftp:Client>triggerClient)->putJson(testFilePath, <json>{"name": "test", "action": "both-success"});
    runtime:sleep(15);

    // Cleanup listener
    runtime:deregisterListener(bothListener);
    check bothListener.gracefulStop();

    // Verify file was processed
    test:assertTrue(moveAfterProcessInvoked, "onFileJson should have been invoked");

    // Verify file was moved to archive (success case)
    boolean sourceExists = check (<ftp:Client>triggerClient)->exists(testFilePath);
    test:assertFalse(sourceExists, "Source file should not exist");

    boolean archiveExists = check (<ftp:Client>triggerClient)->exists(archiveFilePath);
    test:assertTrue(archiveExists, "File should be in archive directory on success");

    boolean errorExists = check (<ftp:Client>triggerClient)->exists(errorFilePath);
    test:assertFalse(errorExists, "File should NOT be in error directory on success");

    // Cleanup
    check (<ftp:Client>triggerClient)->delete(archiveFilePath);
}

boolean noActionInvoked = false;
string noActionFilePath = "";

@test:Config {
    dependsOn: [testBothAfterProcessAndAfterError]
}
public function testNoActionWhenNotConfigured() returns error? {
    // Reset state
    noActionInvoked = false;
    noActionFilePath = "";

    // Service without any afterProcess or afterError
    ftp:Service noActionService = service object {
        @ftp:FunctionConfig {
            fileNamePattern: "no-action-test.*\\.json"
        }
        remote function onFileJson(json content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
            log:printInfo(string `Processing file with no post-action: ${fileInfo.path}`);
            noActionFilePath = fileInfo.path;
            noActionInvoked = true;
        }
    };

    // Create listener
    ftp:Listener noActionListener = check new ({
        protocol: ftp:FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: POST_PROCESS_TEST_DIR,
        pollingInterval: 4,
        fileNamePattern: "no-action-test.*\\.json"
    });

    check noActionListener.attach(noActionService);
    check noActionListener.'start();
    runtime:registerListener(noActionListener);

    // Upload test file
    string testFilePath = POST_PROCESS_TEST_DIR + "/no-action-test.json";
    check (<ftp:Client>triggerClient)->putJson(testFilePath, <json>{"name": "test", "action": "none"});
    runtime:sleep(15);

    // Cleanup listener
    runtime:deregisterListener(noActionListener);
    check noActionListener.gracefulStop();

    // Verify file was processed
    test:assertTrue(noActionInvoked, "onFileJson should have been invoked");

    // Verify file still exists (no action configured)
    boolean fileExists = check (<ftp:Client>triggerClient)->exists(testFilePath);
    test:assertTrue(fileExists, "File should still exist when no action is configured");

    // Cleanup
    check (<ftp:Client>triggerClient)->delete(testFilePath);
}

@test:AfterSuite
function cleanupPostProcessTestDirectories() returns error? {
    ftp:Client? client = triggerClient;
    if client is () {
        return;
    }
    // Clean up test directories
    boolean testDirExists = check client->exists(POST_PROCESS_TEST_DIR);
    if testDirExists {
        ftp:FileInfo[] files = check client->list(POST_PROCESS_TEST_DIR);
        foreach ftp:FileInfo file in files {
            if file.isFile {
                check client->delete(file.path);
            }
        }
        // Try to remove subdirectories
        boolean subDirExists = check client->exists(POST_PROCESS_TEST_DIR + "/subdir");
        if subDirExists {
            check client->rmdir(POST_PROCESS_TEST_DIR + "/subdir");
        }
        boolean preserveSubDirExists = check client->exists(POST_PROCESS_TEST_DIR + "/preserve-subdir");
        if preserveSubDirExists {
            ftp:FileInfo[] preserveFiles = check client->list(POST_PROCESS_TEST_DIR + "/preserve-subdir");
            foreach ftp:FileInfo file in preserveFiles {
                if file.isFile {
                    check client->delete(file.path);
                }
            }
            check client->rmdir(POST_PROCESS_TEST_DIR + "/preserve-subdir");
        }
    }

    boolean archiveDirExists = check client->exists(POST_PROCESS_ARCHIVE_DIR);
    if archiveDirExists {
        ftp:FileInfo[] archiveFiles = check client->list(POST_PROCESS_ARCHIVE_DIR);
        foreach ftp:FileInfo file in archiveFiles {
            if file.isFile {
                check client->delete(file.path);
            }
        }
        boolean preserveArchiveDirExists = check client->exists(POST_PROCESS_ARCHIVE_DIR +
            "/preserve-subdir");
        if preserveArchiveDirExists {
            ftp:FileInfo[] preserveArchiveFiles = check client->list(POST_PROCESS_ARCHIVE_DIR +
                "/preserve-subdir");
            foreach ftp:FileInfo file in preserveArchiveFiles {
                if file.isFile {
                    check client->delete(file.path);
                }
            }
            check client->rmdir(POST_PROCESS_ARCHIVE_DIR + "/preserve-subdir");
        }
    }

    boolean errorDirExists = check client->exists(POST_PROCESS_ERROR_DIR);
    if errorDirExists {
        ftp:FileInfo[] errorFiles = check client->list(POST_PROCESS_ERROR_DIR);
        foreach ftp:FileInfo file in errorFiles {
            if file.isFile {
                check client->delete(file.path);
            }
        }
    }
}
