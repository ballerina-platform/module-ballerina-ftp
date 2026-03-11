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

const string AGE_MAX_DIR = "/home/in/beh-age-max";
const string AGE_MIN_DIR = "/home/in/beh-age-min";

// ─── Shared client ────────────────────────────────────────────────────────────

ftp:Client ageFtpClient = check new ({
    protocol: ftp:FTP,
    host: commons:FTP_HOST,
    port: commons:AUTH_FTP_PORT,
    auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}}
});

// ─── Helpers ──────────────────────────────────────────────────────────────────

function deleteIfExistsAge(ftp:Client ftpClient, string path) {
    do {
        check ftpClient->delete(path);
    } on fail {
        // file may not exist — silently ignore
    }
}

function waitUntilAge(function() returns boolean cond, int timeoutSec) returns boolean {
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

// ─── TEST: maxAge — fresh file within window IS delivered ─────────────────────

isolated boolean ageMaxFreshDelivered = false;

@test:Config {
    groups: ["ftp-listener-behaviour", "file-age-filter"]
}
function testFileAgeFilter_MaxAge_DeliversFreshFile() returns error? {
    lock { ageMaxFreshDelivered = false; }

    ftp:Service ageService = @ftp:ServiceConfig {
        path: AGE_MAX_DIR,
        fileNamePattern: ".*\\.maxfresh",
        fileAgeFilter: {maxAge: 30.0}
    }
    service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {
            foreach ftp:FileInfo fi in event.addedFiles {
                if fi.name.endsWith(".maxfresh") {
                    lock { ageMaxFreshDelivered = true; }
                }
            }
        }
    };

    ftp:Listener ageListener = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        pollingInterval: 2
    });
    check ageListener.attach(ageService);
    check ageListener.'start();
    runtime:registerListener(ageListener);

    check ageFtpClient->putText(AGE_MAX_DIR + "/fresh.maxfresh", "fresh-content");

    boolean delivered = waitUntilAge(function() returns boolean {
        lock { return ageMaxFreshDelivered; }
    }, 20);

    runtime:deregisterListener(ageListener);
    check ageListener.gracefulStop();

    test:assertTrue(delivered, "Fresh file within maxAge window should be delivered");

    deleteIfExistsAge(ageFtpClient, AGE_MAX_DIR + "/fresh.maxfresh");
}

// ─── TEST: maxAge — file already older than maxAge is NOT delivered ────────────

isolated boolean ageMaxOldDelivered = false;

@test:Config {
    groups: ["ftp-listener-behaviour", "file-age-filter"]
}
function testFileAgeFilter_MaxAge_SkipsStaleFile() returns error? {
    lock { ageMaxOldDelivered = false; }

    // Upload the file BEFORE starting the listener so it can age
    check ageFtpClient->putText(AGE_MAX_DIR + "/stale.maxold", "stale-content");

    // Wait for file to exceed maxAge (10 seconds)
    runtime:sleep(15);

    ftp:Service ageService = @ftp:ServiceConfig {
        path: AGE_MAX_DIR,
        fileNamePattern: ".*\\.maxold",
        fileAgeFilter: {maxAge: 10.0}
    }
    service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {
            foreach ftp:FileInfo fi in event.addedFiles {
                if fi.name.endsWith(".maxold") {
                    lock { ageMaxOldDelivered = true; }
                }
            }
        }
    };

    ftp:Listener ageListener = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        pollingInterval: 2
    });
    check ageListener.attach(ageService);
    check ageListener.'start();
    runtime:registerListener(ageListener);

    // Several polls — stale file should not be delivered
    runtime:sleep(10);

    runtime:deregisterListener(ageListener);
    check ageListener.gracefulStop();

    boolean delivered;
    lock { delivered = ageMaxOldDelivered; }
    test:assertFalse(delivered, "File already older than maxAge should NOT be delivered");

    deleteIfExistsAge(ageFtpClient, AGE_MAX_DIR + "/stale.maxold");
}

// ─── TEST: minAge — file is withheld until old enough, then delivered ──────────

isolated boolean ageMinDelivered = false;

@test:Config {
    enable: false,
    groups: ["ftp-listener-behaviour", "file-age-filter"]
}
function testFileAgeFilter_MinAge_DeliversOnceOldEnough() returns error? {
    lock { ageMinDelivered = false; }

    ftp:Service ageService = @ftp:ServiceConfig {
        path: AGE_MIN_DIR,
        fileNamePattern: ".*\\.minage",
        fileAgeFilter: {minAge: 15.0}
    }
    service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {
            foreach ftp:FileInfo fi in event.addedFiles {
                if fi.name.endsWith(".minage") {
                    lock { ageMinDelivered = true; }
                }
            }
        }
    };

    ftp:Listener ageListener = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        pollingInterval: 2
    });
    check ageListener.attach(ageService);
    check ageListener.'start();
    runtime:registerListener(ageListener);

    check ageFtpClient->putText(AGE_MIN_DIR + "/aging.minage", "aging-content");

    // Several polls — file is too young to be delivered
    runtime:sleep(5);
    boolean tooEarly;
    lock { tooEarly = ageMinDelivered; }
    test:assertFalse(tooEarly, "File should NOT be delivered before minAge elapses");

    // Wait for the file to age past minAge
    boolean delivered = waitUntilAge(function() returns boolean {
        lock { return ageMinDelivered; }
    }, 25);

    runtime:deregisterListener(ageListener);
    check ageListener.gracefulStop();

    test:assertTrue(delivered, "File should be delivered once it satisfies minAge");

    deleteIfExistsAge(ageFtpClient, AGE_MIN_DIR + "/aging.minage");
}

// ─── TEST: FileAgeFilter record — default values ───────────────────────────────

@test:Config {
        enable: false,
    groups: ["ftp-listener-behaviour", "file-age-filter"]
}
function testFileAgeFilter_RecordDefaults() {
    ftp:FileAgeFilter filter = {};
    test:assertEquals(filter.ageCalculationMode, ftp:LAST_MODIFIED,
        "Default ageCalculationMode should be LAST_MODIFIED");
    test:assertEquals(filter.minAge, (),
        "Default minAge should be nil");
    test:assertEquals(filter.maxAge, (),
        "Default maxAge should be nil");
}

// ─── TEST: Both minAge and maxAge in the window — file IS delivered ────────────

isolated boolean ageBothDelivered = false;

@test:Config {
    enable: false,
    groups: ["ftp-listener-behaviour", "file-age-filter"]
}
function testFileAgeFilter_MinAndMax_DeliverInWindow() returns error? {
    lock { ageBothDelivered = false; }

    // maxAge=60 means any file younger than 60s is accepted.
    // minAge=15 means the file must be at least 15s old.
    // Upload file, wait 20s → file should be in the [15, 60] window → delivered.
    check ageFtpClient->putText(AGE_MIN_DIR + "/window.minmax", "window-content");
    runtime:sleep(20);

    ftp:Service ageService = @ftp:ServiceConfig {
        path: AGE_MIN_DIR,
        fileNamePattern: ".*\\.minmax",
        fileAgeFilter: {minAge: 15.0, maxAge: 60.0}
    }
    service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {
            foreach ftp:FileInfo fi in event.addedFiles {
                if fi.name.endsWith(".minmax") {
                    lock { ageBothDelivered = true; }
                }
            }
        }
    };

    ftp:Listener ageListener = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        pollingInterval: 2
    });
    check ageListener.attach(ageService);
    check ageListener.'start();
    runtime:registerListener(ageListener);

    boolean delivered = waitUntilAge(function() returns boolean {
        lock { return ageBothDelivered; }
    }, 20);

    runtime:deregisterListener(ageListener);
    check ageListener.gracefulStop();

    test:assertTrue(delivered, "File within [minAge, maxAge] window should be delivered");

    deleteIfExistsAge(ageFtpClient, AGE_MIN_DIR + "/window.minmax");
}

// ─── TEST: ageCalculationMode=CREATION_TIME falls back to lastModified ─────────
// FTP servers don't expose creationTime via VFS, so the implementation falls
// back to lastModified. The fresh file should still be delivered within maxAge.

isolated boolean ageCreationTimeFallbackDelivered = false;

@test:Config {
    groups: ["ftp-listener-behaviour", "file-age-filter"]
}
function testFileAgeFilter_CreationTimeMode_FallsBackToLastModified() returns error? {
    lock { ageCreationTimeFallbackDelivered = false; }

    ftp:Service ageService = @ftp:ServiceConfig {
        path: AGE_MAX_DIR,
        fileNamePattern: ".*\\.ctfb",
        fileAgeFilter: {maxAge: 30.0, ageCalculationMode: ftp:CREATION_TIME}
    }
    service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {
            foreach ftp:FileInfo fi in event.addedFiles {
                if fi.name.endsWith(".ctfb") {
                    lock { ageCreationTimeFallbackDelivered = true; }
                }
            }
        }
    };

    ftp:Listener ageListener = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        pollingInterval: 2
    });
    check ageListener.attach(ageService);
    check ageListener.'start();
    runtime:registerListener(ageListener);

    check ageFtpClient->putText(AGE_MAX_DIR + "/fresh.ctfb", "ct-content");

    boolean delivered = waitUntilAge(function() returns boolean {
        lock { return ageCreationTimeFallbackDelivered; }
    }, 20);

    runtime:deregisterListener(ageListener);
    check ageListener.gracefulStop();

    test:assertTrue(delivered,
        "CREATION_TIME mode should fall back to lastModified and deliver the fresh file");

    deleteIfExistsAge(ageFtpClient, AGE_MAX_DIR + "/fresh.ctfb");
}
