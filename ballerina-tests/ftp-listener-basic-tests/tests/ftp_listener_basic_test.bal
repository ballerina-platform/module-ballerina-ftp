// Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
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
import ballerina/io;
import ballerina/lang.runtime;
import ballerina/test;
import ballerina_tests/ftp_test_commons as commons;

// ─── Shared FTP client for file setup/teardown ────────────────────────────────

ftp:Client ftpClient = check new ({
    protocol: ftp:FTP,
    host: commons:FTP_HOST,
    port: commons:AUTH_FTP_PORT,
    auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
    userDirIsRoot: false
});

// ─── Helper: standard listener config ─────────────────────────────────────────

function listenerConfig(string pattern = "(.*)\\.txt") returns ftp:ListenerConfiguration {
    return {
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        path: commons:HOME_IN,
        pollingInterval: 1,
        fileNamePattern: pattern
    };
}

// =============================================================================
// Attach / detach
// =============================================================================

// Happy-path: attach then detach a service without errors.
@test:Config {
    groups: ["ftp-listener-basic", "attach-detach"]
}
function testAttachDetach_HappyPath() returns error? {
    ftp:Listener l = check new (listenerConfig());

    ftp:Service svc = service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {}
    };

    check l.attach(svc, "test-attach-detach");
    check l.detach(svc);
}

// Attach two services to the same listener, detach both.
@test:Config {
    groups: ["ftp-listener-basic", "attach-detach"]
}
function testAttachDetach_MultipleServices() returns error? {
    ftp:Listener l = check new (listenerConfig());

    ftp:Service svc1 = service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {}
    };
    ftp:Service svc2 = service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {}
    };

    check l.attach(svc1, "svc1");
    check l.attach(svc2, "svc2");
    check l.detach(svc1);
    check l.detach(svc2);
}

// attach() must fail when credentials are empty (wrong password).
@test:Config {
    groups: ["ftp-listener-basic", "attach-detach"]
}
function testAttach_FailsWithEmptyPassword() returns error? {
    ftp:Listener l = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: ""}},
        path: commons:HOME_IN,
        pollingInterval: 2,
        fileNamePattern: "(.*)\\.txt"
    });

    ftp:Service svc = service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {}
    };

    error? result = l.attach(svc);
    test:assertTrue(result is ftp:Error,
        "attach() should fail with an empty password");
    if result is ftp:Error {
        test:assertTrue(result.message().startsWith("Failed to initialize File server connector."),
            "Error should mention connector initialization failure");
    }
}

// Listener creation must fail when username is empty.
@test:Config {
    groups: ["ftp-listener-basic", "attach-detach"]
}
function testListenerCreation_FailsWithEmptyUsername() {
    ftp:Listener|ftp:Error result = new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: "", password: commons:FTP_PASSWORD}},
        path: commons:HOME_IN,
        pollingInterval: 2,
        fileNamePattern: "(.*)\\.txt"
    });

    test:assertTrue(result is ftp:Error, "Listener creation should fail with empty username");
    if result is ftp:Error {
        test:assertEquals(result.message(), "Username cannot be empty");
    }
}

// attach() must fail when credentials are invalid (wrong username).
@test:Config {
    groups: ["ftp-listener-basic", "attach-detach"]
}
function testAttach_FailsWithInvalidUsername() returns error? {
    ftp:Listener l = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: "ballerina", password: commons:FTP_PASSWORD}},
        path: commons:HOME_IN,
        pollingInterval: 2,
        fileNamePattern: "(.*)\\.txt"
    });

    ftp:Service svc = service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {}
    };

    error? result = l.attach(svc);
    test:assertTrue(result is ftp:Error,
        "attach() should fail with an invalid username");
}

// attach() must fail when credentials are invalid (wrong password).
@test:Config {
    groups: ["ftp-listener-basic", "attach-detach"]
}
function testAttach_FailsWithInvalidPassword() returns error? {
    ftp:Listener l = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: "ballerina"}},
        path: commons:HOME_IN,
        pollingInterval: 2,
        fileNamePattern: "(.*)\\.txt"
    });

    ftp:Service svc = service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {}
    };

    error? result = l.attach(svc);
    test:assertTrue(result is ftp:Error,
        "attach() should fail with an invalid password");
}

// attach() must fail when pointing at a non-existent server port.
@test:Config {
    groups: ["ftp-listener-basic", "attach-detach"]
}
function testAttach_FailsWithUnreachableServer() returns error? {
    ftp:Listener l = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: 21218,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        path: commons:HOME_IN,
        pollingInterval: 2,
        fileNamePattern: "(.*)\\.txt"
    });

    ftp:Service svc = service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {}
    };

    error? result = l.attach(svc);
    test:assertTrue(result is ftp:Error,
        "attach() should fail when the server is unreachable");
    if result is ftp:Error {
        test:assertTrue(result.message().startsWith("Failed to initialize File server connector."),
            "Error should mention connector initialization failure");
        test:assertTrue(result.message().length() > "Failed to initialize File server connector.".length(),
            "Error message should include root-cause details");
    }
}

// attach() error message must include root-cause details for invalid credentials.
@test:Config {
    groups: ["ftp-listener-basic", "attach-detach"]
}
function testAttach_ErrorMessageIncludesRootCause() returns error? {
    ftp:Listener l = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: "invaliduser123", password: "invalidpass123"}},
        path: commons:HOME_IN,
        pollingInterval: 2,
        fileNamePattern: "(.*)\\.txt"
    });

    ftp:Service svc = service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {}
    };

    error? result = l.attach(svc);
    test:assertTrue(result is ftp:Error,
        "attach() should fail with invalid credentials");
    if result is ftp:Error {
        test:assertTrue(result.message().startsWith("Failed to initialize File server connector."),
            "Error should mention connector initialization failure");
        test:assertTrue(result.message().length() > "Failed to initialize File server connector.".length(),
            "Error message should include root-cause details");
    }
}

// =============================================================================
// File-name patterns
// =============================================================================

// Listener with a .txt pattern must only fire for matching files.
int fileCount = 0;
@test:Config {
    groups: ["ftp-listener-basic", "patterns"]
}
function testPattern_TxtFilesDetected() returns error? {
    fileCount = 0;

    ftp:Service svc = service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {
            lock {
                fileCount += event.addedFiles.length();
            }
        }
    };

    ftp:Listener l = check new (listenerConfig("(.*)\\.txt"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    // Upload one matching and one non-matching file.
    string txtPath = commons:HOME_IN + "/pattern-match.txt";
    string jsonPath = commons:HOME_IN + "/pattern-no-match.json";

    stream<io:Block, io:Error?> s1 = check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH, 5);
    check ftpClient->put(txtPath, s1);
    stream<io:Block, io:Error?> s2 = check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH, 5);
    check ftpClient->put(jsonPath, s2);

    runtime:sleep(4);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    int snapshot;
    lock {
        snapshot = fileCount;
    }
    // The .json file must NOT have been counted.
    test:assertTrue(snapshot >= 1,
        "At least the .txt file should have been detected; got: " + snapshot.toString());

    // Cleanup.
    check ftpClient->delete(txtPath);
    check ftpClient->delete(jsonPath);
}


int customeExtensionCount = 0;
string lastName = "";

// Listener with a custom extension pattern (.bal) only fires for that extension.
@test:Config {
    groups: ["ftp-listener-basic", "patterns"]
}
function testPattern_CustomExtension() returns error? {
    customeExtensionCount = 0;
    lastName = "";

    ftp:Service svc = service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {
            lock {
                customeExtensionCount += event.addedFiles.length();
                if event.addedFiles.length() > 0 {
                    lastName = event.addedFiles[event.addedFiles.length() - 1].name;
                }
            }
        }
    };

    ftp:Listener l = check new (listenerConfig("(.*)\\.bal"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    string balPath = commons:HOME_IN + "/pattern-custom.bal";
    string txtPath = commons:HOME_IN + "/pattern-custom-ignored.txt";

    stream<io:Block, io:Error?> s1 = check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH, 5);
    check ftpClient->put(balPath, s1);
    stream<io:Block, io:Error?> s2 = check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH, 5);
    check ftpClient->put(txtPath, s2);

    runtime:sleep(4);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(customeExtensionCount >= 1,
        "At least one .bal file should have been detected; got: " + customeExtensionCount.toString());
    test:assertEquals(lastName, "pattern-custom.bal",
        "Only the .bal file should have been captured");

    check ftpClient->delete(balPath);
    check ftpClient->delete(txtPath);
}

// An invalid regex pattern must cause Listener creation to fail with InvalidConfigError.
@test:Config {
    groups: ["ftp-listener-basic", "patterns"]
}
function testPattern_InvalidRegexRejected() {
    ftp:Listener|ftp:Error result = new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        path: commons:HOME_IN,
        pollingInterval: 2,
        fileNamePattern: "[unclosed"
    });

    test:assertTrue(result is ftp:InvalidConfigError,
        "An invalid regex pattern should return InvalidConfigError");
    if result is ftp:InvalidConfigError {
        test:assertTrue(result.message().includes("Invalid regex pattern"),
            "Error message should describe the invalid regex");
    }
}

// =============================================================================
// FileInfo fields
// =============================================================================

ftp:FileInfo? captured = ();
// The anonymous FTP server pre-seeds /home/in/test1.txt (content "File content",
// 12 bytes). Verify every field of the FileInfo record for that file.
@test:Config {
    groups: ["ftp-listener-basic", "fileinfo"]
}
function testFileInfo_AllFields() returns error? {
    captured = ();

    ftp:Service svc = service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {
            // Capture only the first poll that contains exactly one added file
            // matching the seed file, to avoid noise from other tests.
            foreach ftp:FileInfo fi in event.addedFiles {
                if fi.name == "test1.txt" {
                        captured = fi;
                    break;
                }
            }
        }
    };

    ftp:Listener l = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:ANON_FTP_PORT,
        auth: {credentials: {username: commons:ANON_USERNAME, password: commons:ANON_PASSWORD}},
        path: commons:HOME_IN,
        pollingInterval: 2,
        fileNamePattern: "test1\\.txt"
    });

    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    // Wait up to 30 s for the first poll.
    int remaining = 30;
    while remaining > 0 {
        ftp:FileInfo? snap;
        lock {
            snap = captured;
        }
        if snap is ftp:FileInfo {
            break;
        }
        runtime:sleep(1);
        remaining -= 1;
    }

    runtime:deregisterListener(l);
    check l.gracefulStop();

    ftp:FileInfo? fi;
    lock {
        fi = captured;
    }

    if fi is () {
        test:assertFail("FileInfo was not captured within the timeout");
    } else {
        string host = commons:FTP_HOST;
        int port = commons:ANON_FTP_PORT;
        string user = commons:ANON_USERNAME;
        string pass = commons:ANON_PASSWORD;

        test:assertEquals(fi.path,
            string `ftp://${user}:${pass}@${host}:${port}/home/in/test1.txt`,
            "FileInfo.path mismatch");
        test:assertEquals(fi.size, 12, "FileInfo.size should be 12 bytes");
        test:assertTrue(fi.lastModifiedTimestamp > 0, "FileInfo.lastModifiedTimestamp should be positive");
        test:assertEquals(fi.name, "test1.txt", "FileInfo.name mismatch");
        test:assertEquals(fi.isFolder, false, "FileInfo.isFolder should be false");
        test:assertEquals(fi.isFile, true, "FileInfo.isFile should be true");
        test:assertEquals(fi.pathDecoded, "/home/in/test1.txt", "FileInfo.pathDecoded mismatch");
        test:assertEquals(fi.extension, "txt", "FileInfo.extension should be 'txt'");
        test:assertEquals(fi.publicURIString,
            string `ftp://${user}:***@${host}:${port}/home/in/test1.txt`,
            "FileInfo.publicURIString mismatch");
        test:assertEquals(fi.fileType, "file", "FileInfo.fileType should be 'file'");
        test:assertEquals(fi.isAttached, true, "FileInfo.isAttached should be true");
        test:assertEquals(fi.isContentOpen, false, "FileInfo.isContentOpen should be false");
        test:assertEquals(fi.isExecutable, false, "FileInfo.isExecutable should be false");
        test:assertEquals(fi.isHidden, false, "FileInfo.isHidden should be false");
        test:assertEquals(fi.isReadable, true, "FileInfo.isReadable should be true");
        test:assertEquals(fi.isWritable, true, "FileInfo.isWritable should be true");
        test:assertEquals(fi.depth, 4, "FileInfo.depth should be 4");
        test:assertEquals(fi.scheme, "ftp", "FileInfo.scheme should be 'ftp'");
        test:assertEquals(fi.uri,
            string `//${user}:${pass}@${host}:${port}/home/in/test1.txt`,
            "FileInfo.uri mismatch");
        test:assertEquals(fi.rootURI,
            string `ftp://${user}:${pass}@${host}:${port}/`,
            "FileInfo.rootURI mismatch");
        test:assertEquals(fi.friendlyURI,
            string `ftp://${user}:***@${host}:${port}/home/in/test1.txt`,
            "FileInfo.friendlyURI mismatch");
    }
}

// =============================================================================
// Isolated service
// =============================================================================

string isolatedLastName = "";
// An isolated service object must be able to safely read/write shared state
// inside a lock block.
@test:Config {
    groups: ["ftp-listener-basic", "isolated"]
}
function testIsolatedService() returns error? {
    isolatedLastName = "";

    ftp:Service svc = service object {
        remote function onFileChange(ftp:WatchEvent event) {
            event.addedFiles.forEach(function(ftp:FileInfo fi) {
                lock {
                    isolatedLastName = fi.name;
                }
            });
        }
    };

    ftp:Listener l = check new (listenerConfig("(.*)\\.isolated"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    string remotePath = commons:HOME_IN + "/basic-isolated-test.isolated";
    stream<io:Block, io:Error?> s = check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH, 5);
    check ftpClient->put(remotePath, s);

    runtime:sleep(5);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    check ftpClient->delete(remotePath);

    lock {
        test:assertEquals(isolatedLastName, "basic-isolated-test.isolated",
            "Isolated service should have captured the correct file name");
    }
}

// =============================================================================
// Deleted-files detection
// =============================================================================

string deletedNames = "";
// Listener must report deleted files in WatchEvent.deletedFiles.
@test:Config {
    groups: ["ftp-listener-basic", "fileinfo"]
}
function testWatchEvent_DeletedFilesReported() returns error? {
    deletedNames = "";

    ftp:Service svc = service object {
        remote function onFileChange(ftp:WatchEvent event) {
            event.deletedFiles.forEach(function(string name) {
                    deletedNames += name;
            });
        }
    };

    ftp:Listener l = check new (listenerConfig("(.*)\\.del"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    string p1 = commons:HOME_IN + "/del-test-1.del";
    string p2 = commons:HOME_IN + "/del-test-2.del";
    string p3 = commons:HOME_IN + "/del-test-3.del";

    foreach string p in [p1, p2, p3] {
        stream<io:Block, io:Error?> s = check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH, 5);
        check ftpClient->put(p, s);
    }

    // Wait for the listener to register the new files in its state.
    runtime:sleep(3);

    check ftpClient->delete(p1);
    check ftpClient->delete(p2);
    check ftpClient->delete(p3);

    runtime:sleep(4);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    foreach int i in 1...3 {
        test:assertTrue(deletedNames.includes(string `/home/in/del-test-${i}.del`),
            string `Deleted file del-test-${i}.del should have been reported`);
    }
}

// =============================================================================
// gracefulStop
// =============================================================================

int eventCountBeforeStop = 0;
// gracefulStop() must let in-flight events complete before shutting down.
@test:Config {
    groups: ["ftp-listener-basic", "graceful-stop"]
}
function testGracefulStop_EventsReceivedBeforeStop() returns error? {
    eventCountBeforeStop = 0;

    ftp:Service svc = service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {
            lock {
                eventCountBeforeStop += event.addedFiles.length();
            }
        }
    };

    ftp:Listener l = check new (listenerConfig("(.*)\\.graceful"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    string remotePath = commons:HOME_IN + "/graceful-stop-test.graceful";
    stream<io:Block, io:Error?> s = check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH, 5);
    check ftpClient->put(remotePath, s);

    // Allow at least one polling cycle.
    runtime:sleep(4);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    int snapshot;
    lock {
        snapshot = eventCountBeforeStop;
    }
    test:assertTrue(snapshot >= 1,
        "At least one file event should have been received before gracefulStop");

    check ftpClient->delete(remotePath);
}

// =============================================================================
// immediateStop
// =============================================================================

boolean eventReceivedAfterStop = false;
string capturedNameAfterStop = "";
// After immediateStop() no further WatchEvents must be delivered.
@test:Config {
    groups: ["ftp-listener-basic", "immediate-stop"]
}
function testImmediateStop_NoEventsAfterStop() returns error? {
    eventReceivedAfterStop = false;
    capturedNameAfterStop = "";

    ftp:Service svc = service object {
        remote function onFileChange(ftp:WatchEvent event) {
            event.addedFiles.forEach(function(ftp:FileInfo fi) {
                lock {
                    eventReceivedAfterStop = true;
                    capturedNameAfterStop = fi.name;
                }
            });
        }
    };

    ftp:Listener l = check new (listenerConfig("(.*)\\.immediateStop"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    // Let the scheduler register before stopping.
    runtime:sleep(2);

    runtime:deregisterListener(l);
    check l.immediateStop();

    // Upload a file after the stop — the listener must NOT detect it.
    string remotePath = commons:HOME_IN + "/immediate-stop-test.immediateStop";
    stream<io:Block, io:Error?> s = check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH, 5);
    check ftpClient->put(remotePath, s);

    runtime:sleep(3);

    boolean received;
    string name;
    lock {
        received = eventReceivedAfterStop;
        name = capturedNameAfterStop;
    }
    test:assertFalse(received,
        "No events should be received after immediateStop(); got file: " + name);

    check ftpClient->delete(remotePath);
}
