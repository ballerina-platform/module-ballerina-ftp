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

// ─── Shared client ────────────────────────────────────────────────────────────

ftp:Client callerFtpClient = check new ({
    protocol: ftp:FTP,
    host: commons:FTP_HOST,
    port: commons:AUTH_FTP_PORT,
    auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
    userDirIsRoot: false
});

// ─── Isolated directories ─────────────────────────────────────────────────────

const CALLER_IN  = "/home/in/adv-caller-in";
const CALLER_OUT = "/home/in/adv-caller-out";

// ─── Helpers ─────────────────────────────────────────────────────────────────

function callerListenerConfig(string pattern, decimal poll = 1) returns ftp:ListenerConfiguration {
    return {
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        path: CALLER_IN,
        pollingInterval: poll,
        fileNamePattern: pattern
    };
}

function waitUntilCallerCond(function() returns boolean cond, int timeoutSec) returns boolean {
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

// =============================================================================
// Caller — basic file operations via onFileChange
// =============================================================================

// put: service uses caller->put() to write a file, then caller->rename() to archive the trigger
boolean callerPutOk = false;

@test:Config {
    groups: ["ftp-listener-advanced", "caller"]
}
function testCaller_Put() returns error? {
    callerPutOk = false;

    ftp:Service svc = service object {
        remote function onFileChange(ftp:WatchEvent & readonly event, ftp:Caller caller) returns error? {
            foreach ftp:FileInfo fi in event.addedFiles {
                if fi.name == "caller-put.trigger" {
                    stream<io:Block, io:Error?> s =
                        check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH, 5);
                    check caller->put(CALLER_OUT + "/caller-put-result.txt", s);
                    check caller->rename(CALLER_IN + "/caller-put.trigger",
                                         CALLER_OUT + "/caller-put.trigger");
                    callerPutOk = true;
                }
            }
        }
    };

    ftp:Listener l = check new (callerListenerConfig("caller-put.*\\.trigger"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    stream<io:Block, io:Error?> ts = check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH, 5);
    check callerFtpClient->put(CALLER_IN + "/caller-put.trigger", ts);
    runtime:sleep(4);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(callerPutOk, "Caller put/rename should succeed");

    // Verify the result file was created
    ftp:Error|boolean exists = callerFtpClient->exists(CALLER_OUT + "/caller-put-result.txt");
    if exists is boolean {
        test:assertTrue(exists, "Result file should have been created by caller->put()");
    }

    check callerFtpClient->delete(CALLER_OUT + "/caller-put-result.txt");
    check callerFtpClient->delete(CALLER_OUT + "/caller-put.trigger");
}

// delete: service uses caller->delete() to remove the trigger file
boolean callerDeleteOk = false;

@test:Config {
    groups: ["ftp-listener-advanced", "caller"],
    dependsOn: [testCaller_Put]
}
function testCaller_Delete() returns error? {
    callerDeleteOk = false;

    ftp:Service svc = service object {
        remote function onFileChange(ftp:WatchEvent & readonly event, ftp:Caller caller) returns error? {
            foreach ftp:FileInfo fi in event.addedFiles {
                if fi.name == "caller-delete.trigger" {
                    check caller->delete(CALLER_IN + "/caller-delete.trigger");
                    callerDeleteOk = true;
                }
            }
        }
    };

    ftp:Listener l = check new (callerListenerConfig("caller-delete.*\\.trigger"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    stream<io:Block, io:Error?> ts = check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH, 5);
    check callerFtpClient->put(CALLER_IN + "/caller-delete.trigger", ts);
    runtime:sleep(4);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(callerDeleteOk, "Caller delete should succeed");

    ftp:Error|stream<byte[] & readonly, io:Error?> result =
        callerFtpClient->get(CALLER_IN + "/caller-delete.trigger");
    test:assertTrue(result is ftp:FileNotFoundError,
        "File should have been deleted by caller->delete()");
}

// list: service uses caller->list() to enumerate a directory
ftp:FileInfo[] callerListResult = [];

@test:Config {
    groups: ["ftp-listener-advanced", "caller"],
    dependsOn: [testCaller_Delete]
}
function testCaller_List() returns error? {
    callerListResult = [];

    ftp:Service svc = service object {
        remote function onFileChange(ftp:WatchEvent & readonly event, ftp:Caller caller) returns error? {
            foreach ftp:FileInfo fi in event.addedFiles {
                if fi.name == "caller-list.trigger" {
                    callerListResult = check caller->list(CALLER_IN);
                    check caller->rename(CALLER_IN + "/caller-list.trigger",
                                         CALLER_OUT + "/caller-list.trigger");
                }
            }
        }
    };

    ftp:Listener l = check new (callerListenerConfig("caller-list.*\\.trigger"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    stream<io:Block, io:Error?> ts = check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH, 5);
    check callerFtpClient->put(CALLER_IN + "/caller-list.trigger", ts);
    runtime:sleep(4);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(callerListResult.length() >= 1, "Caller list should return at least one file");
    test:assertEquals(callerListResult[0].path, CALLER_IN + "/caller-list.trigger",
        "Listed file path should match");

    check callerFtpClient->delete(CALLER_OUT + "/caller-list.trigger");
}

// get: service reads the trigger file via caller->get() and validates content
boolean callerGetOk = false;

@test:Config {
    groups: ["ftp-listener-advanced", "caller"],
    dependsOn: [testCaller_List]
}
function testCaller_Get() returns error? {
    callerGetOk = false;

    ftp:Service svc = service object {
        remote function onFileChange(ftp:WatchEvent & readonly event, ftp:Caller caller) returns error? {
            foreach ftp:FileInfo fi in event.addedFiles {
                if fi.name == "caller-get.trigger" {
                    stream<io:Block, io:Error?> fileStream =
                        check caller->get(CALLER_IN + "/caller-get.trigger");
                    byte[] allBytes = [];
                    check fileStream.forEach(function(io:Block chunk) {
                        allBytes.push(...chunk);
                    });
                    string content = check string:fromBytes(allBytes);
                    if content.length() > 0 {
                        callerGetOk = true;
                    }
                    check caller->rename(CALLER_IN + "/caller-get.trigger",
                                         CALLER_OUT + "/caller-get.trigger");
                }
            }
        }
    };

    ftp:Listener l = check new (callerListenerConfig("caller-get.*\\.trigger"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    stream<io:Block, io:Error?> ts = check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH, 5);
    check callerFtpClient->put(CALLER_IN + "/caller-get.trigger", ts);
    runtime:sleep(4);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(callerGetOk, "Caller get should return non-empty content");

    check callerFtpClient->delete(CALLER_OUT + "/caller-get.trigger");
}

// size: service reads file size via caller->size()
int callerFileSize = 0;

@test:Config {
    groups: ["ftp-listener-advanced", "caller"],
    dependsOn: [testCaller_Get]
}
function testCaller_Size() returns error? {
    callerFileSize = 0;

    ftp:Service svc = service object {
        remote function onFileChange(ftp:WatchEvent & readonly event, ftp:Caller caller) returns error? {
            foreach ftp:FileInfo fi in event.addedFiles {
                if fi.name == "caller-size.trigger" {
                    callerFileSize = check caller->size(CALLER_IN + "/caller-size.trigger");
                    check caller->rename(CALLER_IN + "/caller-size.trigger",
                                         CALLER_OUT + "/caller-size.trigger");
                }
            }
        }
    };

    ftp:Listener l = check new (callerListenerConfig("caller-size.*\\.trigger"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    stream<io:Block, io:Error?> ts = check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH, 5);
    check callerFtpClient->put(CALLER_IN + "/caller-size.trigger", ts);
    runtime:sleep(4);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(callerFileSize > 0, "Caller size should return a positive byte count");

    check callerFtpClient->delete(CALLER_OUT + "/caller-size.trigger");
}

// mkdir / rmdir / isDirectory lifecycle
boolean callerMkdirOk = false;
boolean callerIsDirOk = false;
boolean callerRmdirOk = false;

@test:Config {
    groups: ["ftp-listener-advanced", "caller"],
    dependsOn: [testCaller_Size]
}
function testCaller_MkdirRmdirIsDirectory() returns error? {
    callerMkdirOk = false;
    callerIsDirOk = false;
    callerRmdirOk = false;

    string callerDir = CALLER_OUT + "/adv-caller-dir";

    ftp:Service svc = service object {
        remote function onFileChange(ftp:WatchEvent & readonly event, ftp:Caller caller) returns error? {
            foreach ftp:FileInfo fi in event.addedFiles {
                if fi.name == "caller-mkdir.trigger" {
                    check caller->mkdir(callerDir);
                    callerMkdirOk = true;
                    callerIsDirOk = check caller->isDirectory(callerDir);
                    check caller->rmdir(callerDir);
                    boolean existCheck = check caller->exists(callerDir);
                    callerRmdirOk = existCheck is true ? false : true;
                    check caller->rename(CALLER_IN + "/caller-mkdir.trigger",
                                         CALLER_OUT + "/caller-mkdir.trigger");
                }
            }
        }
    };

    ftp:Listener l = check new (callerListenerConfig("caller-mkdir.*\\.trigger"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    stream<io:Block, io:Error?> ts = check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH, 5);
    check callerFtpClient->put(CALLER_IN + "/caller-mkdir.trigger", ts);
    runtime:sleep(4);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(callerMkdirOk, "Caller mkdir should succeed");
    test:assertTrue(callerIsDirOk, "isDirectory should return true after mkdir");
    test:assertTrue(callerRmdirOk, "rmdir should remove the directory");

    check callerFtpClient->delete(CALLER_OUT + "/caller-mkdir.trigger");
}

// =============================================================================
// Caller — typed write/read methods
// =============================================================================

// putText / getText round-trip
boolean callerPutTextOk = false;

@test:Config {
    groups: ["ftp-listener-advanced", "caller"],
    dependsOn: [testCaller_MkdirRmdirIsDirectory]
}
function testCaller_PutText() returns error? {
    callerPutTextOk = false;

    ftp:Service svc = service object {
        remote function onFileChange(ftp:WatchEvent & readonly event, ftp:Caller caller) returns error? {
            foreach ftp:FileInfo fi in event.addedFiles {
                if fi.name == "caller-puttext.trigger" {
                    check caller->putText(CALLER_OUT + "/caller-puttext-result.txt",
                        "Caller Text Content", ftp:OVERWRITE);
                    callerPutTextOk = true;
                    check caller->rename(CALLER_IN + "/caller-puttext.trigger",
                                         CALLER_OUT + "/caller-puttext.trigger");
                }
            }
        }
    };

    ftp:Listener l = check new (callerListenerConfig("caller-puttext.*\\.trigger"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    check callerFtpClient->putText(CALLER_IN + "/caller-puttext.trigger", "trigger");
    runtime:sleep(5);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(callerPutTextOk, "Caller putText should execute");

    string|ftp:Error content = callerFtpClient->getText(CALLER_OUT + "/caller-puttext-result.txt");
    if content is string {
        test:assertEquals(content, "Caller Text Content", "Text content should match what was written");
    } else {
        test:assertFail("Failed to read putText result: " + content.message());
    }

    check callerFtpClient->delete(CALLER_OUT + "/caller-puttext-result.txt");
    check callerFtpClient->delete(CALLER_OUT + "/caller-puttext.trigger");
}

// putJson / getJson round-trip
boolean callerPutJsonOk = false;

@test:Config {
    groups: ["ftp-listener-advanced", "caller"],
    dependsOn: [testCaller_PutText]
}
function testCaller_PutJson() returns error? {
    callerPutJsonOk = false;

    json expectedJson = {name: "Caller", active: true, count: 99};

    ftp:Service svc = service object {
        remote function onFileChange(ftp:WatchEvent & readonly event, ftp:Caller caller) returns error? {
            foreach ftp:FileInfo fi in event.addedFiles {
                if fi.name == "caller-putjson.trigger" {
                    check caller->putJson(CALLER_OUT + "/caller-putjson-result.json",
                        expectedJson, ftp:OVERWRITE);
                    callerPutJsonOk = true;
                    check caller->rename(CALLER_IN + "/caller-putjson.trigger",
                                         CALLER_OUT + "/caller-putjson.trigger");
                }
            }
        }
    };

    ftp:Listener l = check new (callerListenerConfig("caller-putjson.*\\.trigger"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    check callerFtpClient->putText(CALLER_IN + "/caller-putjson.trigger", "trigger");
    runtime:sleep(5);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(callerPutJsonOk, "Caller putJson should execute");

    json|ftp:Error content = callerFtpClient->getJson(CALLER_OUT + "/caller-putjson-result.json");
    if content is json {
        test:assertEquals(content.toJsonString(), expectedJson.toJsonString());
    } else {
        test:assertFail("Failed to read putJson result: " + content.message());
    }

    check callerFtpClient->delete(CALLER_OUT + "/caller-putjson-result.json");
    check callerFtpClient->delete(CALLER_OUT + "/caller-putjson.trigger");
}

// putCsv / getCsv round-trip
boolean callerPutCsvOk = false;

@test:Config {
    groups: ["ftp-listener-advanced", "caller"],
    dependsOn: [testCaller_PutJson]
}
function testCaller_PutCsv() returns error? {
    callerPutCsvOk = false;

    string[][] csvData = [["Name", "Age", "City"], ["Alice", "30", "NYC"], ["Bob", "25", "LA"]];

    ftp:Service svc = service object {
        remote function onFileChange(ftp:WatchEvent & readonly event, ftp:Caller caller) returns error? {
            foreach ftp:FileInfo fi in event.addedFiles {
                if fi.name == "caller-putcsv.trigger" {
                    check caller->putCsv(CALLER_OUT + "/caller-putcsv-result.csv",
                        csvData, ftp:OVERWRITE);
                    callerPutCsvOk = true;
                    check caller->rename(CALLER_IN + "/caller-putcsv.trigger",
                                         CALLER_OUT + "/caller-putcsv.trigger");
                }
            }
        }
    };

    ftp:Listener l = check new (callerListenerConfig("caller-putcsv.*\\.trigger"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    check callerFtpClient->putText(CALLER_IN + "/caller-putcsv.trigger", "trigger");
    runtime:sleep(5);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(callerPutCsvOk, "Caller putCsv should execute");

    string[][]|ftp:Error rows = callerFtpClient->getCsv(CALLER_OUT + "/caller-putcsv-result.csv");
    if rows is string[][] {
        test:assertEquals(rows.length(), 2, "Should have 2 data rows (header excluded)");
        test:assertEquals(rows[0], ["Alice", "30", "NYC"]);
        test:assertEquals(rows[1], ["Bob", "25", "LA"]);
    } else {
        test:assertFail("Failed to read putCsv result: " + rows.message());
    }

    check callerFtpClient->delete(CALLER_OUT + "/caller-putcsv-result.csv");
    check callerFtpClient->delete(CALLER_OUT + "/caller-putcsv.trigger");
}

// move and copy
boolean callerMoveOk = false;
boolean callerCopyOk = false;

@test:Config {
    groups: ["ftp-listener-advanced", "caller"],
    dependsOn: [testCaller_PutCsv]
}
function testCaller_MoveAndCopy() returns error? {
    callerMoveOk = false;
    callerCopyOk = false;

    ftp:Service svc = service object {
        remote function onFileChange(ftp:WatchEvent & readonly event, ftp:Caller caller) returns error? {
            foreach ftp:FileInfo fi in event.addedFiles {
                if fi.name == "caller-move.trigger" {
                    check caller->move(CALLER_IN + "/caller-move.trigger",
                                       CALLER_OUT + "/caller-move-dest.txt");
                    callerMoveOk = true;
                } else if fi.name == "caller-copy.trigger" {
                    check caller->copy(CALLER_IN + "/caller-copy.trigger",
                                       CALLER_OUT + "/caller-copy-dest.txt");
                    callerCopyOk = true;
                    check caller->delete(CALLER_IN + "/caller-copy.trigger");
                }
            }
        }
    };

    ftp:Listener l = check new (callerListenerConfig("caller-(move|copy).*\\.trigger"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    stream<io:Block, io:Error?> ms = check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH, 5);
    check callerFtpClient->put(CALLER_IN + "/caller-move.trigger", ms);
    stream<io:Block, io:Error?> cs = check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH, 5);
    check callerFtpClient->put(CALLER_IN + "/caller-copy.trigger", cs);

    boolean received = waitUntilCallerCond(function() returns boolean {
        return callerMoveOk && callerCopyOk;
    }, 20);
    runtime:sleep(2);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(received, "Both move and copy should succeed via Caller");

    ftp:Error|boolean moveDest = callerFtpClient->exists(CALLER_OUT + "/caller-move-dest.txt");
    if moveDest is boolean {
        test:assertTrue(moveDest, "Moved file should exist at destination");
    }
    ftp:Error|boolean copyDest = callerFtpClient->exists(CALLER_OUT + "/caller-copy-dest.txt");
    if copyDest is boolean {
        test:assertTrue(copyDest, "Copied file should exist at destination");
    }

    check callerFtpClient->delete(CALLER_OUT + "/caller-move-dest.txt");
    ftp:Error? __ = callerFtpClient->delete(CALLER_OUT + "/caller-copy-dest.txt");
}

// =============================================================================
// @ServiceConfig — per-service path configuration
// =============================================================================

// Directories for @ServiceConfig tests
const SC_PATH_A = "/home/in/adv-sc-route-a";
const SC_PATH_B = "/home/in/adv-sc-route-b";
const SC_SINGLE = "/home/in/adv-sc-single";
const SC_LEGACY = "/home/in/adv-sc-legacy";

// Multi-path routing: two services on one listener, each with its own path
int scRouteACount = 0;
string scRouteAName = "";
int scRouteBCount = 0;
string scRouteBName = "";

@test:Config {
    groups: ["ftp-listener-advanced", "service-config"]
}
function testServiceConfig_MultiPathRouting() returns error? {
    scRouteACount = 0;
    scRouteAName = "";
    scRouteBCount = 0;
    scRouteBName = "";

    ftp:Service svcA = @ftp:ServiceConfig {path: SC_PATH_A}
    service object {
        remote function onFileText(string content, ftp:FileInfo fileInfo) returns error? {
            scRouteAName = fileInfo.name;
            scRouteACount += 1;
        }
    };

    ftp:Service svcB = @ftp:ServiceConfig {path: SC_PATH_B}
    service object {
        remote function onFileText(string content, ftp:FileInfo fileInfo) returns error? {
            scRouteBName = fileInfo.name;
            scRouteBCount += 1;
        }
    };

    ftp:Listener l = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        pollingInterval: 2
    });

    check l.attach(svcA);
    check l.attach(svcB);
    check l.'start();
    runtime:registerListener(l);

    stream<io:Block, io:Error?> sa = check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH, 5);
    check callerFtpClient->put(SC_PATH_A + "/route-a-only.txt", sa);
    stream<io:Block, io:Error?> sb = check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH, 5);
    check callerFtpClient->put(SC_PATH_B + "/route-b-only.txt", sb);

    boolean received = waitUntilCallerCond(function() returns boolean {
        return scRouteACount > 0 && scRouteBCount > 0;
    }, 30);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(received, "Both @ServiceConfig services must fire for files in their own directory");
    test:assertEquals(scRouteAName, "route-a-only.txt", "Service A should have received its file");
    test:assertEquals(scRouteBName, "route-b-only.txt", "Service B should have received its file");

    check callerFtpClient->delete(SC_PATH_A + "/route-a-only.txt");
    ftp:Error? __ = callerFtpClient->delete(SC_PATH_B + "/route-b-only.txt");
}

// Single service with @ServiceConfig
boolean scSingleReceived = false;
string scSingleName = "";

@test:Config {
    groups: ["ftp-listener-advanced", "service-config"]
}
function testServiceConfig_SingleService() returns error? {
    scSingleReceived = false;
    scSingleName = "";

    ftp:Service svc = @ftp:ServiceConfig {path: SC_SINGLE}
    service object {
        remote function onFileText(string content, ftp:FileInfo fileInfo) returns error? {
            scSingleName = fileInfo.name;
            scSingleReceived = true;
        }
    };

    ftp:Listener l = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        pollingInterval: 2
    });

    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    stream<io:Block, io:Error?> s = check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH, 5);
    check callerFtpClient->put(SC_SINGLE + "/sc-single-test.txt", s);

    boolean received = waitUntilCallerCond(function() returns boolean { return scSingleReceived; }, 30);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(received, "Single @ServiceConfig service should receive its file event");
    test:assertEquals(scSingleName, "sc-single-test.txt");

    check callerFtpClient->delete(SC_SINGLE + "/sc-single-test.txt");
}

// Backward compatibility: service without @ServiceConfig on a listener that has path set
boolean scLegacyReceived = false;
string scLegacyName = "";

@test:Config {
    groups: ["ftp-listener-advanced", "service-config"]
}
function testServiceConfig_BackwardCompatibility() returns error? {
    scLegacyReceived = false;
    scLegacyName = "";

    ftp:Service svc = service object {
        remote function onFileText(string content, ftp:FileInfo fileInfo) returns error? {
            scLegacyName = fileInfo.name;
            scLegacyReceived = true;
        }
    };

    ftp:Listener l = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        path: SC_LEGACY,
        pollingInterval: 2,
        fileNamePattern: "legacy.*\\.txt"
    });

    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    stream<io:Block, io:Error?> s = check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH, 5);
    check callerFtpClient->put(SC_LEGACY + "/legacy-compat.txt", s);

    boolean received = waitUntilCallerCond(function() returns boolean { return scLegacyReceived; }, 30);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(received, "Legacy service (no @ServiceConfig) must receive events via listener path");
    test:assertEquals(scLegacyName, "legacy-compat.txt");

    check callerFtpClient->delete(SC_LEGACY + "/legacy-compat.txt");
}

// Duplicate path → attach must return an error
@test:Config {
    groups: ["ftp-listener-advanced", "service-config"]
}
function testServiceConfig_DuplicatePathError() returns error? {
    ftp:Service svc1 = @ftp:ServiceConfig {path: "/home/in/adv-sc-dup"}
    service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {}
    };

    ftp:Service svc2 = @ftp:ServiceConfig {path: "/home/in/adv-sc-dup"}
    service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {}
    };

    ftp:Listener l = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        pollingInterval: 2
    });

    check l.attach(svc1);
    error? result = l.attach(svc2);

    check l.immediateStop();

    test:assertTrue(result is error, "Attaching a service with a duplicate path should fail");
    if result is error {
        test:assertTrue(result.message().includes("Duplicate path"),
            "Error message should mention duplicate path; got: " + result.message());
    }
}

// Mixed annotated + non-annotated services → attach must return an error
@test:Config {
    groups: ["ftp-listener-advanced", "service-config"]
}
function testServiceConfig_MixedServicesError() returns error? {
    ftp:Service annotated = @ftp:ServiceConfig {path: "/home/in/adv-sc-mixed"}
    service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {}
    };

    ftp:Service nonAnnotated = service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {}
    };

    ftp:Listener l = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        pollingInterval: 2
    });

    check l.attach(annotated);
    error? result = l.attach(nonAnnotated);

    check l.immediateStop();

    test:assertTrue(result is error,
        "Attaching a non-annotated service to a listener that already has an annotated service should fail");
    if result is error {
        test:assertTrue(result.message().includes("ServiceConfig") || result.message().includes("annotation"),
            "Error message should mention ServiceConfig or annotation; got: " + result.message());
    }
}

// Invalid regex in @ServiceConfig.fileNamePattern → attach returns InvalidConfigError
@test:Config {
    groups: ["ftp-listener-advanced", "service-config"]
}
function testServiceConfig_InvalidFileNamePattern() returns error? {
    ftp:Service svc = @ftp:ServiceConfig {
        path: "/home/in/adv-sc-badregex",
        fileNamePattern: "[unclosed"
    }
    service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {}
    };

    ftp:Listener l = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        pollingInterval: 2
    });

    error? result = l.attach(svc);
    check l.immediateStop();

    test:assertTrue(result is ftp:InvalidConfigError,
        "attach() should return InvalidConfigError for an invalid regex in @ServiceConfig.fileNamePattern");
    if result is ftp:InvalidConfigError {
        test:assertTrue(result.message().includes("Invalid regex pattern"),
            "Error message should indicate invalid regex");
    }
}

// ServiceConfiguration record structure and defaults
@test:Config {
    groups: ["ftp-listener-advanced", "service-config"]
}
function testServiceConfig_RecordDefaults() {
    ftp:ServiceConfiguration minimal = {path: "/home/in/minimal"};
    test:assertEquals(minimal.path, "/home/in/minimal");
    test:assertEquals(minimal.fileDependencyConditions.length(), 0,
        "fileDependencyConditions should default to empty array");
}
