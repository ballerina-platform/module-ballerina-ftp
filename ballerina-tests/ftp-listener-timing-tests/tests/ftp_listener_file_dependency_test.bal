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
import ballerina/lang.runtime;
import ballerina/test;
import ballerina_tests/ftp_test_commons as commons;

// ─── Isolated directories ─────────────────────────────────────────────────────

const string DEP_ALL_DIR = "/home/in/beh-dep-all";
const string DEP_ANY_DIR = "/home/in/beh-dep-any";
const string DEP_EXACT_DIR = "/home/in/beh-dep-exact";
const string DEP_LITERAL_DIR = "/home/in/beh-dep-literal";
const string DEP_BADNAME_DIR = "/home/in/beh-dep-badname";
const string DEP_OPTIONAL_DIR = "/home/in/beh-dep-optional";

// ─── Shared client ────────────────────────────────────────────────────────────

ftp:Client depFtpClient = check new ({
    protocol: ftp:FTP,
    host: commons:FTP_HOST,
    port: commons:AUTH_FTP_PORT,
    auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}}
});

// ─── Helpers ──────────────────────────────────────────────────────────────────

function deleteIfExistsDep(ftp:Client ftpClient, string path) {
    do {
        check ftpClient->delete(path);
    } on fail {
        // file may not exist — silently ignore
    }
}

function waitUntilDep(function() returns boolean cond, int timeoutSec) returns boolean {
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

// ─── TEST: ALL mode — target blocked until ALL deps present ──────────────────

isolated boolean depAllDelivered = false;
isolated string depAllFile = "";

@test:Config {
    groups: ["ftp-listener-behaviour", "file-dependency"]
}
function testFileDependency_ALL_BlockedThenDelivered() returns error? {
    lock { depAllDelivered = false; }
    lock { depAllFile = ""; }

    string xmlPath = DEP_ALL_DIR + "/invoice_all.xml";
    string csvPath = DEP_ALL_DIR + "/invoice_all.csv";
    string flagPath = DEP_ALL_DIR + "/invoice_all.flag";

    deleteIfExistsDep(depFtpClient, xmlPath);
    deleteIfExistsDep(depFtpClient, csvPath);
    deleteIfExistsDep(depFtpClient, flagPath);

    ftp:Service depService = @ftp:ServiceConfig {
        path: DEP_ALL_DIR,
        fileNamePattern: ".*\\.xml",
        fileDependencyConditions: [
            {
                targetPattern: "(.*)\\.xml",
                requiredFiles: ["$1.csv", "$1.flag"],
                matchingMode: ftp:ALL
            }
        ]
    }
    service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {
            foreach ftp:FileInfo fi in event.addedFiles {
                if fi.name.endsWith(".xml") {
                    lock { depAllDelivered = true; }
                    lock { depAllFile = fi.name; }
                }
            }
        }
    };

    ftp:Listener depListener = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        pollingInterval: 2
    });
    check depListener.attach(depService);
    check depListener.'start();
    runtime:registerListener(depListener);

    // Upload the target without any dependencies — must be blocked
    check depFtpClient->put(xmlPath, "invoice-data".toBytes());
    runtime:sleep(6);

    boolean deliveredEarly;
    lock { deliveredEarly = depAllDelivered; }
    test:assertFalse(deliveredEarly, "Target file should be blocked when ALL deps are absent");

    // Upload only one dep — still blocked (ALL requires both)
    check depFtpClient->put(csvPath, "csv-data".toBytes());
    runtime:sleep(4);
    boolean deliveredPartial;
    lock { deliveredPartial = depAllDelivered; }
    test:assertFalse(deliveredPartial, "Target should remain blocked when only one of ALL deps is present");

    // Upload the final dep — now the target should be delivered
    check depFtpClient->put(flagPath, "flag".toBytes());

    boolean delivered = waitUntilDep(function() returns boolean {
        lock { return depAllDelivered; }
    }, 15);

    runtime:deregisterListener(depListener);
    check depListener.gracefulStop();

    test:assertTrue(delivered, "Target file should be delivered once ALL deps are present");
    string deliveredName;
    lock { deliveredName = depAllFile; }
    test:assertEquals(deliveredName, "invoice_all.xml", "Delivered file name should match target");

    deleteIfExistsDep(depFtpClient, xmlPath);
    deleteIfExistsDep(depFtpClient, csvPath);
    deleteIfExistsDep(depFtpClient, flagPath);
}

// ─── TEST: ANY mode — target delivered when at least ONE dep is present ───────

isolated boolean depAnyDelivered = false;

@test:Config {
    groups: ["ftp-listener-behaviour", "file-dependency"]
}
function testFileDependency_ANY_DeliveredWithOneDep() returns error? {
    lock { depAnyDelivered = false; }

    string xmlPath = DEP_ANY_DIR + "/report_any.xml";
    string csvPath = DEP_ANY_DIR + "/report_any.csv";
    string txtPath = DEP_ANY_DIR + "/report_any.txt";

    deleteIfExistsDep(depFtpClient, xmlPath);
    deleteIfExistsDep(depFtpClient, csvPath);
    deleteIfExistsDep(depFtpClient, txtPath);

    ftp:Service depService = @ftp:ServiceConfig {
        path: DEP_ANY_DIR,
        fileNamePattern: ".*\\.xml",
        fileDependencyConditions: [
            {
                targetPattern: "(.*)\\.xml",
                requiredFiles: ["$1.csv", "$1.txt"],
                matchingMode: ftp:ANY
            }
        ]
    }
    service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {
            foreach ftp:FileInfo fi in event.addedFiles {
                if fi.name.endsWith(".xml") {
                    lock { depAnyDelivered = true; }
                }
            }
        }
    };

    ftp:Listener depListener = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        pollingInterval: 2
    });
    check depListener.attach(depService);
    check depListener.'start();
    runtime:registerListener(depListener);

    // Upload the target alone — blocked (no deps yet)
    check depFtpClient->put(xmlPath, "report-data".toBytes());
    runtime:sleep(4);
    boolean blockedEarly;
    lock { blockedEarly = depAnyDelivered; }
    test:assertFalse(blockedEarly, "Target should be blocked when no deps are present in ANY mode");

    // Upload only CSV (one of two options) — ANY mode: should unblock the target
    check depFtpClient->put(csvPath, "csv-data".toBytes());

    boolean delivered = waitUntilDep(function() returns boolean {
        lock { return depAnyDelivered; }
    }, 15);

    runtime:deregisterListener(depListener);
    check depListener.gracefulStop();

    test:assertTrue(delivered, "Target should be delivered when ANY required dep is present");

    deleteIfExistsDep(depFtpClient, xmlPath);
    deleteIfExistsDep(depFtpClient, csvPath);
    deleteIfExistsDep(depFtpClient, txtPath);
}

// ─── TEST: Capture group substitution in requiredFiles ────────────────────────

isolated boolean depCaptureDelivered = false;

@test:Config {
    groups: ["ftp-listener-behaviour", "file-dependency"]
}
function testFileDependency_CaptureGroupSubstitution() returns error? {
    lock { depCaptureDelivered = false; }

    string xmlPath = DEP_ALL_DIR + "/order_123.xml";
    string csvPath = DEP_ALL_DIR + "/order_123.csv";

    deleteIfExistsDep(depFtpClient, xmlPath);
    deleteIfExistsDep(depFtpClient, csvPath);

    // targetPattern uses capture group (\\d+); requiredFiles uses $1 substitution
    ftp:Service depService = @ftp:ServiceConfig {
        path: DEP_ALL_DIR,
        fileNamePattern: "order_.*\\.xml",
        fileDependencyConditions: [
            {
                targetPattern: "order_(\\d+)\\.xml",
                requiredFiles: ["order_$1.csv"],
                matchingMode: ftp:ALL
            }
        ]
    }
    service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {
            foreach ftp:FileInfo fi in event.addedFiles {
                if fi.name == "order_123.xml" {
                    lock { depCaptureDelivered = true; }
                }
            }
        }
    };

    ftp:Listener depListener = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        pollingInterval: 2
    });
    check depListener.attach(depService);
    check depListener.'start();
    runtime:registerListener(depListener);

    // Upload without the CSV dep — blocked
    check depFtpClient->put(xmlPath, "order".toBytes());
    runtime:sleep(4);
    boolean earlyDelivered;
    lock { earlyDelivered = depCaptureDelivered; }
    test:assertFalse(earlyDelivered, "Should be blocked without the capture-group-resolved dep");

    // Now add the matching dep using the capture group value
    check depFtpClient->put(csvPath, "order-csv".toBytes());

    boolean delivered = waitUntilDep(function() returns boolean {
        lock { return depCaptureDelivered; }
    }, 15);

    runtime:deregisterListener(depListener);
    check depListener.gracefulStop();

    test:assertTrue(delivered, "Capture group substitution in requiredFiles should resolve correctly");

    deleteIfExistsDep(depFtpClient, xmlPath);
    deleteIfExistsDep(depFtpClient, csvPath);

}

// ─── TEST: FileDependencyCondition record defaults ────────────────────────────

@test:Config {
    groups: ["ftp-listener-behaviour", "file-dependency"]
}
function testFileDependencyCondition_RecordDefaults() {
    ftp:FileDependencyCondition cond = {
        targetPattern: ".*\\.xml",
        requiredFiles: ["marker.flag"]
    };
    test:assertEquals(cond.matchingMode, ftp:ALL,
        "Default matchingMode should be ALL");
    test:assertEquals(cond.requiredFileCount, 1,
        "Default requiredFileCount should be 1");
}

// ─── TEST: Invalid regex in @ServiceConfig.fileDependencyConditions.targetPattern ─

@test:Config {
    groups: ["ftp-listener-behaviour", "file-dependency"]
}
function testFileDependency_InvalidTargetPattern() returns error? {
    ftp:Service invalidSvc = @ftp:ServiceConfig {
        path: DEP_ALL_DIR,
        fileDependencyConditions: [
            {
                targetPattern: "(unclosed*regex",
                requiredFiles: ["data.txt"]
            }
        ]
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

    error? result = l.attach(invalidSvc);
    test:assertTrue(result is ftp:InvalidConfigError,
        "Invalid targetPattern regex should produce InvalidConfigError");
    if result is ftp:InvalidConfigError {
        test:assertTrue(result.message().includes("Invalid regex pattern"),
            "Error message should mention invalid regex pattern");
    }
}

// ─── TEST: Invalid regex in @ServiceConfig.fileDependencyConditions.requiredFiles ─

@test:Config {
    groups: ["ftp-listener-behaviour", "file-dependency"]
}
function testFileDependency_InvalidRequiredFilesPattern() returns error? {
    ftp:Service invalidSvc = @ftp:ServiceConfig {
        path: DEP_ALL_DIR,
        fileDependencyConditions: [
            {
                targetPattern: "data.*\\.xml",
                requiredFiles: ["[broken-regex"]
            }
        ]
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

    error? result = l.attach(invalidSvc);
    test:assertTrue(result is ftp:InvalidConfigError,
        "Invalid requiredFiles regex should produce InvalidConfigError");
    if result is ftp:InvalidConfigError {
        test:assertTrue(result.message().includes("Invalid regex pattern"),
            "Error message should mention invalid regex pattern");
    }
}

// ─── TEST: EXACT_COUNT mode — delivered only when precisely N deps are present ─

isolated boolean depExactDelivered = false;

@test:Config {
    groups: ["ftp-listener-behaviour", "file-dependency"]
}
function testFileDependency_ExactCount_BlockedUntilCountMet() returns error? {
    lock { depExactDelivered = false; }

    string xmlPath  = DEP_EXACT_DIR + "/data_ec.xml";
    string dep1Path = DEP_EXACT_DIR + "/data_ec_001.dep";
    string dep2Path = DEP_EXACT_DIR + "/data_ec_002.dep";

    deleteIfExistsDep(depFtpClient, xmlPath);
    deleteIfExistsDep(depFtpClient, dep1Path);
    deleteIfExistsDep(depFtpClient, dep2Path);

    // requiredFiles is a regex: exactly 2 files matching data_ec_\d+\.dep must exist
    ftp:Service depService = @ftp:ServiceConfig {
        path: DEP_EXACT_DIR,
        fileNamePattern: "data_ec\\.xml",
        fileDependencyConditions: [
            {
                targetPattern: "data_ec\\.xml",
                requiredFiles: ["data_ec_\\d+\\.dep"],
                matchingMode: ftp:EXACT_COUNT,
                requiredFileCount: 2
            }
        ]
    }
    service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {
            foreach ftp:FileInfo fi in event.addedFiles {
                if fi.name == "data_ec.xml" {
                    lock { depExactDelivered = true; }
                }
            }
        }
    };

    ftp:Listener depListener = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        pollingInterval: 2
    });
    check depListener.attach(depService);
    check depListener.'start();
    runtime:registerListener(depListener);

    // Upload target with no deps → 0 matches ≠ 2 → blocked
    check depFtpClient->put(xmlPath, "ec-data".toBytes());
    runtime:sleep(6);
    boolean blockedNoDeps;
    lock { blockedNoDeps = depExactDelivered; }
    test:assertFalse(blockedNoDeps, "Target should be blocked with 0 dep files (need exactly 2)");

    // Upload only one dep → 1 match ≠ 2 → still blocked
    check depFtpClient->put(dep1Path, "dep1".toBytes());
    runtime:sleep(4);
    boolean blockedOneDep;
    lock { blockedOneDep = depExactDelivered; }
    test:assertFalse(blockedOneDep, "Target should remain blocked with 1 dep file (need exactly 2)");

    // Upload second dep → 2 matches = 2 → delivered
    check depFtpClient->put(dep2Path, "dep2".toBytes());
    boolean delivered = waitUntilDep(function() returns boolean {
        lock { return depExactDelivered; }
    }, 15);

    runtime:deregisterListener(depListener);
    check depListener.gracefulStop();

    test:assertTrue(delivered, "Target should be delivered once exactly 2 dep files are present");

    deleteIfExistsDep(depFtpClient, xmlPath);
    deleteIfExistsDep(depFtpClient, dep1Path);
    deleteIfExistsDep(depFtpClient, dep2Path);
}

// ─── TEST: Captured text is matched literally in requiredFiles ────────────────

isolated boolean depLiteralDelivered = false;

@test:Config {
    groups: ["ftp-listener-behaviour", "file-dependency"]
}
function testFileDependency_CaptureMatchedLiterally() returns error? {
    lock { depLiteralDelivered = false; }

    string targetPath = DEP_LITERAL_DIR + "/1.2.txt";
    string lookalikePath = DEP_LITERAL_DIR + "/1x2.marker";
    string markerPath = DEP_LITERAL_DIR + "/1.2.marker";

    ftp:Service depService = @ftp:ServiceConfig {
        path: DEP_LITERAL_DIR,
        fileNamePattern: ".*\\.txt",
        fileDependencyConditions: [
            {targetPattern: "(.+)\\.txt", requiredFiles: ["$1\\.marker"], matchingMode: ftp:ALL}
        ]
    }
    service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {
            foreach ftp:FileInfo fi in event.addedFiles {
                if fi.name == "1.2.txt" {
                    lock { depLiteralDelivered = true; }
                }
            }
        }
    };

    ftp:Listener depListener = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        pollingInterval: 2
    });
    check depListener.attach(depService);
    check depListener.'start();
    runtime:registerListener(depListener);

    // The '.' captured from 1.2.txt must not match the 'x' in 1x2.marker
    check depFtpClient->put(lookalikePath, "marker".toBytes());
    check depFtpClient->put(targetPath, "data".toBytes());
    runtime:sleep(4);
    boolean earlyDelivered;
    lock { earlyDelivered = depLiteralDelivered; }

    check depFtpClient->put(markerPath, "marker".toBytes());
    boolean delivered = waitUntilDep(function() returns boolean {
        lock { return depLiteralDelivered; }
    }, 15);

    runtime:deregisterListener(depListener);
    check depListener.gracefulStop();
    deleteIfExistsDep(depFtpClient, targetPath);
    deleteIfExistsDep(depFtpClient, lookalikePath);
    deleteIfExistsDep(depFtpClient, markerPath);

    test:assertFalse(earlyDelivered, "1x2.marker should not release 1.2.txt");
    test:assertTrue(delivered, "1.2.marker should release 1.2.txt");
}

// ─── TEST: A file name with regex characters does not block other files ──────

isolated boolean depBadNameOtherDelivered = false;

@test:Config {
    groups: ["ftp-listener-behaviour", "file-dependency"]
}
function testFileDependency_RegexCharactersInNameDoNotBlockOthers() returns error? {
    lock { depBadNameOtherDelivered = false; }

    // The mock server seeds a[.txt in this folder
    string otherPath = DEP_BADNAME_DIR + "/b.txt";
    string otherMarkerPath = DEP_BADNAME_DIR + "/b.marker";

    ftp:Service depService = @ftp:ServiceConfig {
        path: DEP_BADNAME_DIR,
        fileNamePattern: ".*\\.txt",
        fileDependencyConditions: [
            {targetPattern: "(.+)\\.txt", requiredFiles: ["$1\\.marker"], matchingMode: ftp:ALL}
        ]
    }
    service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {
            foreach ftp:FileInfo fi in event.addedFiles {
                if fi.name == "b.txt" {
                    lock { depBadNameOtherDelivered = true; }
                }
            }
        }
    };

    ftp:Listener depListener = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        pollingInterval: 2
    });
    check depListener.attach(depService);
    check depListener.'start();
    runtime:registerListener(depListener);

    check depFtpClient->put(otherMarkerPath, "marker".toBytes());
    check depFtpClient->put(otherPath, "data".toBytes());
    boolean delivered = waitUntilDep(function() returns boolean {
        lock { return depBadNameOtherDelivered; }
    }, 15);

    runtime:deregisterListener(depListener);
    check depListener.gracefulStop();
    deleteIfExistsDep(depFtpClient, otherPath);
    deleteIfExistsDep(depFtpClient, otherMarkerPath);

    test:assertTrue(delivered, "b.txt should be delivered although a[.txt is in the same folder");
}

// ─── TEST: An optional group that does not match does not stop the poll ──────

isolated boolean depOptionalDelivered = false;

@test:Config {
    groups: ["ftp-listener-behaviour", "file-dependency"]
}
function testFileDependency_UnmatchedOptionalGroup() returns error? {
    lock { depOptionalDelivered = false; }

    string targetPath = DEP_OPTIONAL_DIR + "/order_1.csv";
    string markerPath = DEP_OPTIONAL_DIR + "/order_1.marker";

    // order_1.csv has no "_final", so group 2 does not take part in the match
    ftp:Service depService = @ftp:ServiceConfig {
        path: DEP_OPTIONAL_DIR,
        fileNamePattern: "order_.*\\.csv",
        fileDependencyConditions: [
            {targetPattern: "order_(\\d+)(_final)?\\.csv", requiredFiles: ["order_$1\\.marker"]}
        ]
    }
    service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {
            foreach ftp:FileInfo fi in event.addedFiles {
                if fi.name == "order_1.csv" {
                    lock { depOptionalDelivered = true; }
                }
            }
        }
    };

    ftp:Listener depListener = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        pollingInterval: 2
    });
    check depListener.attach(depService);
    check depListener.'start();
    runtime:registerListener(depListener);

    check depFtpClient->put(markerPath, "marker".toBytes());
    check depFtpClient->put(targetPath, "data".toBytes());
    boolean delivered = waitUntilDep(function() returns boolean {
        lock { return depOptionalDelivered; }
    }, 15);

    runtime:deregisterListener(depListener);
    check depListener.gracefulStop();
    deleteIfExistsDep(depFtpClient, targetPath);
    deleteIfExistsDep(depFtpClient, markerPath);

    test:assertTrue(delivered, "order_1.csv should be delivered when the optional group does not match");
}
