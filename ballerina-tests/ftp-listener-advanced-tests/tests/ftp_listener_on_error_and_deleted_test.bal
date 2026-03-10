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

// ─── Record type: intentionally has a required field missing from test JSON ───

type StrictPerson record {|
    string name;
    int age;
    string email;   // not present in test_data.json → binding fails
|};

// ─── Shared client ────────────────────────────────────────────────────────────

ftp:Client errorDeleteFtpClient = check new ({
    protocol: ftp:FTP,
    host: commons:FTP_HOST,
    port: commons:AUTH_FTP_PORT,
    auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
    userDirIsRoot: false
});

// ─── Isolated directories ─────────────────────────────────────────────────────

const ON_ERROR_DIR   = "/home/in/adv-onerror";
const ON_DELETED_DIR = "/home/in/adv-deleted";

// ─── Helper ───────────────────────────────────────────────────────────────────

function waitUntilBool(function() returns boolean cond, int timeoutSec) returns boolean {
    int remaining = timeoutSec;
    while remaining > 0 {
        if cond() {
            return true;
        }
        runtime:sleep(1);
        remaining -= 1;
    }
    return false;
}

function errDelListenerConfig(string dir, string pattern, decimal poll = 2)
        returns ftp:ListenerConfiguration {
    return {
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        path: dir,
        pollingInterval: poll,
        fileNamePattern: pattern
    };
}

// =============================================================================
// onError — ContentBindingError when strict-type binding fails
// =============================================================================

boolean onErrorInvoked = false;
ftp:ContentBindingError? lastBindingError = ();
string lastErrorFilePath = "";

@test:Config {
    groups: ["ftp-listener-advanced", "on-error"]
}
function testOnError_Basic() returns error? {
    onErrorInvoked = false;
    lastBindingError = ();
    lastErrorFilePath = "";

    ftp:Service svc = service object {
        remote function onFileJson(StrictPerson content, ftp:FileInfo fileInfo,
                                   ftp:Caller caller) returns error? {
            // Should not reach here — binding fails because 'email' is missing
        }

        remote function onError(ftp:Error err, ftp:Caller caller) returns error? {
            onErrorInvoked = true;
            if err is ftp:ContentBindingError {
                lastBindingError = err;
                lastErrorFilePath = err.detail().filePath ?: "";
            }
        }
    };

    ftp:Listener l = check new (errDelListenerConfig(ON_ERROR_DIR, "onerr-basic.*\\.json"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    // Upload JSON missing the required 'email' field
    check errorDeleteFtpClient->putText(ON_ERROR_DIR + "/onerr-basic.json",
        "{\"name\": \"John\", \"age\": 30}");

    boolean received = waitUntilBool(function() returns boolean { return onErrorInvoked; }, 30);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(received, "onError should be invoked when binding fails");
    test:assertTrue(lastBindingError is ftp:ContentBindingError, "Error should be ContentBindingError");
    test:assertTrue(lastErrorFilePath.endsWith(".json"), "Error filePath should reference the .json file");

    check errorDeleteFtpClient->delete(ON_ERROR_DIR + "/onerr-basic.json");
}

// onError with minimal signature (no Caller)
boolean onErrorMinimalInvoked = false;

@test:Config {
    groups: ["ftp-listener-advanced", "on-error"],
    dependsOn: [testOnError_Basic]
}
function testOnError_MinimalParams() returns error? {
    onErrorMinimalInvoked = false;

    ftp:Service svc = service object {
        remote function onFileJson(StrictPerson content, ftp:FileInfo fileInfo) returns error? {}

        remote function onError(ftp:Error err) returns error? {
            onErrorMinimalInvoked = true;
        }
    };

    ftp:Listener l = check new (errDelListenerConfig(ON_ERROR_DIR, "onerr-min.*\\.json"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    check errorDeleteFtpClient->putText(ON_ERROR_DIR + "/onerr-min.json",
        "{\"name\": \"Jane\", \"age\": 25}");

    boolean received = waitUntilBool(function() returns boolean { return onErrorMinimalInvoked; }, 30);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(received, "onError without Caller should be invoked");

    check errorDeleteFtpClient->delete(ON_ERROR_DIR + "/onerr-min.json");
}

// onError with Caller: use caller to delete the problematic file
boolean onErrorCallerInvoked = false;
string onErrorCallerFilePath = "";

@test:Config {
    groups: ["ftp-listener-advanced", "on-error"],
    dependsOn: [testOnError_MinimalParams]
}
function testOnError_CallerDeletesFile() returns error? {
    onErrorCallerInvoked = false;
    onErrorCallerFilePath = "";

    ftp:Service svc = service object {
        remote function onFileJson(StrictPerson content, ftp:FileInfo fileInfo,
                                   ftp:Caller caller) returns error? {}

        remote function onError(ftp:Error err, ftp:Caller caller) returns error? {
            onErrorCallerInvoked = true;
            if err is ftp:ContentBindingError {
                string? filePath = err.detail().filePath;
                if filePath is string {
                    onErrorCallerFilePath = filePath;
                    ftp:Error? del = caller->delete(filePath);
                    // Ignore deletion errors in the handler
                }
            }
        }
    };

    ftp:Listener l = check new (errDelListenerConfig(ON_ERROR_DIR, "onerr-caller.*\\.json"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    string remotePath = ON_ERROR_DIR + "/onerr-caller.json";
    check errorDeleteFtpClient->putText(remotePath, "{\"name\": \"Bob\", \"age\": 40}");

    boolean received = waitUntilBool(function() returns boolean { return onErrorCallerInvoked; }, 30);
    runtime:sleep(2);   // allow delete to complete

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(received, "onError with Caller should be invoked");
    test:assertTrue(onErrorCallerFilePath.endsWith(".json"), "filePath should reference the .json file");

    // File should already be gone; ignore any error
    check errorDeleteFtpClient->delete(remotePath);
}

// ContentBindingError type structure is correct
@test:Config {
    groups: ["ftp-listener-advanced", "on-error"]
}
function testContentBindingError_TypeStructure() {
    ftp:ContentBindingError err = error ftp:ContentBindingError("Test binding error",
        filePath = "/test/file.json",
        content = [72, 101, 108, 108, 111]   // "Hello"
    );

    test:assertTrue(err is ftp:Error, "ContentBindingError should be a subtype of ftp:Error");
    test:assertEquals(err.message(), "Test binding error");
    test:assertEquals(err.detail().filePath, "/test/file.json");
    test:assertEquals(err.detail().content, [72, 101, 108, 108, 111]);
}

// =============================================================================
// onFileDeleted — batch variant: onFileDeleted(string[] deletedFiles)
// =============================================================================

string[] onDeletedFilesReceived = [];
boolean onDeletedEventReceived = false;

@test:Config {
    groups: ["ftp-listener-advanced", "on-file-deleted"]
}
function testOnFileDeleted_SingleFile() returns error? {
    onDeletedFilesReceived = [];
    onDeletedEventReceived = false;

    ftp:Service svc = service object {
        remote function onFileDeleted(string[] deletedFiles) returns error? {
            foreach string f in deletedFiles {
                onDeletedFilesReceived.push(f);
            }
            onDeletedEventReceived = true;
        }
    };

    // Upload the file before starting the listener so it is in the initial snapshot
    stream<io:Block, io:Error?> s = check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH);
    check errorDeleteFtpClient->put(ON_DELETED_DIR + "/del-single.deleted1", s);
    runtime:sleep(1);

    ftp:Listener l = check new (errDelListenerConfig(ON_DELETED_DIR, "(.*)\\.deleted1", 1));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    // Wait for initial poll to establish baseline state
    runtime:sleep(2);

    check errorDeleteFtpClient->delete(ON_DELETED_DIR + "/del-single.deleted1");

    boolean received = waitUntilBool(function() returns boolean { return onDeletedEventReceived; }, 20);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(received, "onFileDeleted should be invoked after file deletion");
    boolean found = false;
    foreach string f in onDeletedFilesReceived {
        if f.includes("del-single.deleted1") {
            found = true;
            break;
        }
    }
    test:assertTrue(found, "Deleted file path should be reported in onFileDeleted");
}

// onFileDelete — per-file variant: onFileDelete(string deletedFile)
string[] onDeleteFilePaths = [];
int onDeleteFileCount = 0;

@test:Config {
    groups: ["ftp-listener-advanced", "on-file-deleted"],
    dependsOn: [testOnFileDeleted_SingleFile]
}
function testOnFileDelete_MultipleFiles() returns error? {
    onDeleteFilePaths = [];
    onDeleteFileCount = 0;

    ftp:Service svc = service object {
        remote function onFileDelete(string deletedFile) returns error? {
            onDeleteFilePaths.push(deletedFile);
            onDeleteFileCount += 1;
        }
    };

    ftp:Listener l = check new (errDelListenerConfig(ON_DELETED_DIR, "(.*)\\.deleted2", 1));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    string p1 = ON_DELETED_DIR + "/del-multi-a.deleted2";
    string p2 = ON_DELETED_DIR + "/del-multi-b.deleted2";
    string p3 = ON_DELETED_DIR + "/del-multi-c.deleted2";

    stream<io:Block, io:Error?> s1 = check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH, 5);
    check errorDeleteFtpClient->put(p1, s1);
    stream<io:Block, io:Error?> s2 = check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH, 5);
    check errorDeleteFtpClient->put(p2, s2);
    stream<io:Block, io:Error?> s3 = check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH, 5);
    check errorDeleteFtpClient->put(p3, s3);

    runtime:sleep(2);

    check errorDeleteFtpClient->delete(p1);
    check errorDeleteFtpClient->delete(p2);
    check errorDeleteFtpClient->delete(p3);

    boolean received = waitUntilBool(function() returns boolean { return onDeleteFileCount >= 3; }, 20);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(received, "onFileDelete should fire for each deleted file");
    test:assertTrue(onDeleteFileCount >= 3, "Should have 3 delete events");

    boolean foundA = false;
    boolean foundB = false;
    boolean foundC = false;
    foreach string f in onDeleteFilePaths {
        if f.includes("del-multi-a.deleted2") { foundA = true; }
        if f.includes("del-multi-b.deleted2") { foundB = true; }
        if f.includes("del-multi-c.deleted2") { foundC = true; }
    }
    test:assertTrue(foundA, "del-multi-a should be reported");
    test:assertTrue(foundB, "del-multi-b should be reported");
    test:assertTrue(foundC, "del-multi-c should be reported");
}

// onFileDelete with Caller: use caller to list remaining files
boolean onDeleteCallerInvoked = false;
string[] onDeleteCallerFilePaths = [];

@test:Config {
    groups: ["ftp-listener-advanced", "on-file-deleted"],
    dependsOn: [testOnFileDelete_MultipleFiles]
}
function testOnFileDelete_WithCaller() returns error? {
    onDeleteCallerInvoked = false;
    onDeleteCallerFilePaths = [];

    ftp:Service svc = service object {
        remote function onFileDelete(string deletedFile, ftp:Caller caller) returns error? {
            onDeleteCallerFilePaths.push(deletedFile);
            onDeleteCallerInvoked = true;
            // Use caller to list remaining files — proves caller is functional
            ftp:FileInfo[]|ftp:Error remaining = caller->list(ON_DELETED_DIR);
            // Ignore error; the intent is to verify caller is available
        }
    };

    ftp:Listener l = check new (errDelListenerConfig(ON_DELETED_DIR, "(.*)\\.deleted3", 1));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    stream<io:Block, io:Error?> s = check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH, 5);
    check errorDeleteFtpClient->put(ON_DELETED_DIR + "/del-caller-test.deleted3", s);
    runtime:sleep(2);
    check errorDeleteFtpClient->delete(ON_DELETED_DIR + "/del-caller-test.deleted3");

    boolean received = waitUntilBool(function() returns boolean { return onDeleteCallerInvoked; }, 20);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(received, "onFileDelete with Caller should be invoked");
    test:assertTrue(onDeleteCallerFilePaths.length() >= 1, "Should have at least 1 deleted file path");
    test:assertTrue(onDeleteCallerFilePaths[0].includes("del-caller-test.deleted3"),
        "Reported path should match the deleted file");
}

// Pattern filter: only files matching the listener pattern are tracked
boolean onDeletePatternMatchFound = false;
boolean onDeletePatternNonMatchReported = false;

@test:Config {
    groups: ["ftp-listener-advanced", "on-file-deleted"],
    dependsOn: [testOnFileDelete_WithCaller]
}
function testOnFileDelete_PatternFilter() returns error? {
    onDeletePatternMatchFound = false;
    onDeletePatternNonMatchReported = false;

    ftp:Service svc = service object {
        remote function onFileDelete(string deletedFile) returns error? {
            if deletedFile.includes("match.deleted4") {
                onDeletePatternMatchFound = true;
            }
            if deletedFile.includes("nomatch.other") {
                onDeletePatternNonMatchReported = true;
            }
        }
    };

    ftp:Listener l = check new (errDelListenerConfig(ON_DELETED_DIR, "(.*)\\.deleted4", 1));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    stream<io:Block, io:Error?> sm = check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH, 5);
    check errorDeleteFtpClient->put(ON_DELETED_DIR + "/match.deleted4", sm);
    stream<io:Block, io:Error?> sn = check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH, 5);
    check errorDeleteFtpClient->put(ON_DELETED_DIR + "/nomatch.other", sn);
    runtime:sleep(2);

    check errorDeleteFtpClient->delete(ON_DELETED_DIR + "/match.deleted4");
    check errorDeleteFtpClient->delete(ON_DELETED_DIR + "/nomatch.other");

    boolean received = waitUntilBool(function() returns boolean {
        return onDeletePatternMatchFound;
    }, 20);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(received, "Matching file should be reported in onFileDelete");
    test:assertFalse(onDeletePatternNonMatchReported, "Non-matching file must NOT be reported");
}

// No deletion → onFileDelete must not fire
boolean onDeleteNoFilesEventFired = false;

@test:Config {
    groups: ["ftp-listener-advanced", "on-file-deleted"],
    dependsOn: [testOnFileDelete_PatternFilter]
}
function testOnFileDelete_NoFiles() returns error? {
    onDeleteNoFilesEventFired = false;

    ftp:Service svc = service object {
        remote function onFileDelete(string deletedFile) returns error? {
            onDeleteNoFilesEventFired = true;
        }
    };

    ftp:Listener l = check new (errDelListenerConfig(ON_DELETED_DIR, "(.*)\\.deleted5", 1));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    // Don't create or delete any files; wait for several polling cycles
    runtime:sleep(5);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertFalse(onDeleteNoFilesEventFired, "onFileDelete should NOT fire when no files are deleted");
}

// Service handler that returns error: listener should still invoke the handler
boolean onDeleteErrorHandlerFired = false;

@test:Config {
    groups: ["ftp-listener-advanced", "on-file-deleted"],
    dependsOn: [testOnFileDelete_NoFiles]
}
function testOnFileDelete_HandlerReturnsError() returns error? {
    onDeleteErrorHandlerFired = false;

    ftp:Service svc = service object {
        remote function onFileDelete(string deletedFile) returns error? {
            onDeleteErrorHandlerFired = true;
            return error("Intentional test error");
        }
    };

    ftp:Listener l = check new (errDelListenerConfig(ON_DELETED_DIR, "(.*)\\.deleted6", 1));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    stream<io:Block, io:Error?> s = check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH, 5);
    check errorDeleteFtpClient->put(ON_DELETED_DIR + "/del-err-handler.deleted6", s);
    runtime:sleep(2);
    check errorDeleteFtpClient->delete(ON_DELETED_DIR + "/del-err-handler.deleted6");

    boolean received = waitUntilBool(function() returns boolean { return onDeleteErrorHandlerFired; }, 20);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(received, "onFileDelete handler should be invoked even when it returns an error");
}
