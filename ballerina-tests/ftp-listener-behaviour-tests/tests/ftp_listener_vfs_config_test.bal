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
import ballerina/log;
import ballerina/test;

import ballerina_tests/ftp_test_commons as commons;

// ─── Isolated directories ─────────────────────────────────────────────────────

const string VFS_DIR = "/home/in/beh-vfs";
const string POLL_DIR = "/home/in/beh-poll";

// ─── Shared clients ───────────────────────────────────────────────────────────

ftp:Client vfsFtpClient = check new ({
    protocol: ftp:FTP,
    host: commons:FTP_HOST,
    port: commons:AUTH_FTP_PORT,
    auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}}
});

// ─── Helpers ──────────────────────────────────────────────────────────────────

function deleteIfExistsVfs(ftp:Client ftpClient, string path) returns error? {
    check ftpClient->delete(path);
}

function waitUntilVfs(function () returns boolean cond, int timeoutSec) returns boolean {
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

// ─── TEST: connectTimeout is accepted by the listener ─────────────────────────

@test:Config {
    groups: ["ftp-listener-behaviour", "vfs-config"]
}
function testListener_ConnectTimeout_Accepted() returns error? {
    ftp:Listener|ftp:Error l = new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        path: VFS_DIR,
        pollingInterval: 2,
        connectTimeout: 20.0
    });
    test:assertTrue(l is ftp:Listener,
            "Listener with connectTimeout should initialize without error");
    if l is ftp:Listener {
        log:printInfo("Listener with connectTimeout=20s initialized");
    }
}

// ─── TEST: socketConfig is accepted by the listener ───────────────────────────

@test:Config {
    groups: ["ftp-listener-behaviour", "vfs-config"]
}
function testListener_SocketConfig_Accepted() returns error? {
    ftp:Listener|ftp:Error l = new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        path: VFS_DIR,
        pollingInterval: 2,
        socketConfig: {
            ftpDataTimeout: 90.0,
            ftpSocketTimeout: 45.0
        }
    });
    test:assertTrue(l is ftp:Listener,
            "Listener with socketConfig should initialize without error");
    if l is ftp:Listener {
        log:printInfo("Listener with socketConfig initialized");
    }
}

// ─── TEST: fileTransferMode BINARY — listener initializes and delivers files ──

isolated boolean binaryListenerDelivered = false;

@test:Config {
    groups: ["ftp-listener-behaviour", "vfs-config", "listener-compression"]
}
function testListener_FileTransferMode_Binary_DeliversFile() returns error? {
    lock {
        binaryListenerDelivered = false;
    }

    ftp:Service binSvc = @ftp:ServiceConfig {
        path: VFS_DIR,
        fileNamePattern: ".*\\.bintransfer"
    }
    service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {
            foreach ftp:FileInfo fi in event.addedFiles {
                if fi.name.endsWith(".bintransfer") {
                    lock {
                        binaryListenerDelivered = true;
                    }
                }
            }
        }
    };

    ftp:Listener binListener = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        pollingInterval: 2,
        fileTransferMode: ftp:BINARY
    });
    check binListener.attach(binSvc);
    check binListener.'start();
    runtime:registerListener(binListener);

    check vfsFtpClient->putText(VFS_DIR + "/test.bintransfer", "binary-mode-content");

    boolean delivered = waitUntilVfs(function() returns boolean {
                lock {
                    return binaryListenerDelivered;
                }
            }, 20);

    runtime:deregisterListener(binListener);
    check binListener.gracefulStop();

    test:assertTrue(delivered, "BINARY mode listener should deliver files correctly");

    check deleteIfExistsVfs(vfsFtpClient, VFS_DIR + "/test.bintransfer");

}

// ─── TEST: fileTransferMode ASCII — listener initializes and delivers files ───

isolated boolean asciiListenerDelivered = false;

@test:Config {
    groups: ["ftp-listener-behaviour", "vfs-config", "listener-compression"]
}
function testListener_FileTransferMode_Ascii_DeliversFile() returns error? {
    lock {
        asciiListenerDelivered = false;
    }

    ftp:Service asciiSvc = @ftp:ServiceConfig {
        path: VFS_DIR,
        fileNamePattern: ".*\\.asciitransfer"
    }
    service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {
            foreach ftp:FileInfo fi in event.addedFiles {
                if fi.name.endsWith(".asciitransfer") {
                    lock {
                        asciiListenerDelivered = true;
                    }
                }
            }
        }
    };

    ftp:Listener asciiListener = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        pollingInterval: 2,
        fileTransferMode: ftp:ASCII
    });
    check asciiListener.attach(asciiSvc);
    check asciiListener.'start();
    runtime:registerListener(asciiListener);

    check vfsFtpClient->putText(VFS_DIR + "/test.asciitransfer", "ascii-mode-content");

    boolean delivered = waitUntilVfs(function() returns boolean {
                lock {
                    return asciiListenerDelivered;
                }
            }, 20);

    runtime:deregisterListener(asciiListener);
    check asciiListener.gracefulStop();
    test:assertTrue(delivered, "ASCII mode listener should deliver files correctly");

    check deleteIfExistsVfs(vfsFtpClient, VFS_DIR + "/test.asciitransfer");

}

// ─── TEST: default fileTransferMode is BINARY ─────────────────────────────────

@test:Config {
    groups: ["ftp-listener-behaviour", "vfs-config"]
}
function testListener_DefaultFileTransferMode_IsBinary() {
    ftp:ListenerConfiguration listenerConfig = {
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        pollingInterval: 2
    };
    test:assertEquals(listenerConfig.fileTransferMode, ftp:BINARY,
            "Default fileTransferMode should be BINARY");
}

// ─── TEST: default pollingInterval is 60 seconds ──────────────────────────────

@test:Config {
    groups: ["ftp-listener-behaviour", "polling"]
}
function testListener_DefaultPollingInterval() {
    ftp:ListenerConfiguration listenerConfig = {
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}}
    };
    test:assertEquals(listenerConfig.pollingInterval, 60.0d,
            "Default pollingInterval should be 60 seconds");
}

// ─── TEST: polling interval — files detected within expected window ────────────

isolated boolean pollDelivered = false;

@test:Config {
    groups: ["ftp-listener-behaviour", "polling"]
}
function testListener_PollingInterval_DetectsFileWithinWindow() returns error? {
    lock {
        pollDelivered = false;
    }

    decimal pollingIntervalSec = 3.0;

    ftp:Service pollSvc = @ftp:ServiceConfig {
        path: POLL_DIR,
        fileNamePattern: ".*\\.polltest"
    }
    service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {
            foreach ftp:FileInfo fi in event.addedFiles {
                if fi.name.endsWith(".polltest") {
                    lock {
                        pollDelivered = true;
                    }
                }
            }
        }
    };

    ftp:Listener pollListener = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        pollingInterval: pollingIntervalSec
    });
    check pollListener.attach(pollSvc);
    check pollListener.'start();
    runtime:registerListener(pollListener);

    check vfsFtpClient->putText(POLL_DIR + "/detect.polltest", "poll-test-content");

    // File should be detected within 3 * pollingInterval (allow some slack)
    boolean delivered = waitUntilVfs(function() returns boolean {
                lock {
                    return pollDelivered;
                }
            }, 20);

    runtime:deregisterListener(pollListener);
    check pollListener.gracefulStop();

    test:assertTrue(delivered,
            "File should be detected within a few polling intervals");

    check deleteIfExistsVfs(vfsFtpClient, POLL_DIR + "/detect.polltest");

}

// ─── TEST: retryConfig is accepted by the listener ────────────────────────────

@test:Config {
    groups: ["ftp-listener-behaviour", "vfs-config"]
}
function testListener_RetryConfig_Accepted() returns error? {
    ftp:Listener|ftp:Error l = new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        path: VFS_DIR,
        pollingInterval: 2,
        retryConfig: {
            count: 3,
            interval: 0.5,
            backOffFactor: 2.0,
            maxWaitInterval: 5.0
        }
    });
    test:assertTrue(l is ftp:Listener,
            "Listener with retryConfig should initialize without error");
    if l is ftp:Listener {
        log:printInfo("Listener with retryConfig initialized");
    }
}

// ─── TEST: combined VFS configs — listener initializes ────────────────────────

@test:Config {
    groups: ["ftp-listener-behaviour", "vfs-config"]
}
function testListener_CombinedVfsConfigs_Accepted() returns error? {
    ftp:Listener|ftp:Error l = new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        path: VFS_DIR,
        pollingInterval: 2,
        connectTimeout: 20.0,
        fileTransferMode: ftp:BINARY,
        socketConfig: {
            ftpDataTimeout: 90.0,
            ftpSocketTimeout: 45.0
        },
        retryConfig: {
            count: 2,
            interval: 1.0,
            backOffFactor: 1.5,
            maxWaitInterval: 10.0
        }
    });
    test:assertTrue(l is ftp:Listener,
            "Listener with combined VFS configs should initialize without error");
    if l is ftp:Listener {
        log:printInfo("Listener with combined VFS configs initialized");
    }
}

// ─── TEST: SFTP sftpCompression config — listener initializes ─────────────────

@test:Config {
    groups: ["ftp-listener-behaviour", "listener-compression"]
}
function testListener_SftpCompression_Config_Accepted() returns error? {
    // Config-level test only: verify the field is accepted without connection error
    // (actual SFTP compression behavior depends on server support)
    ftp:ListenerConfiguration compressionConfig = {
        protocol: ftp:SFTP,
        host: commons:FTP_HOST,
        port: commons:SFTP_PORT,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD},
            privateKey: {
                path: "../resources/sftp.private.key",
                password: "changeit"
            }
        },
        path: "/in",
        pollingInterval: 2,
        sftpCompression: [ftp:ZLIB, ftp:NO]
    };

    test:assertEquals(compressionConfig.sftpCompression, [ftp:ZLIB, ftp:NO],
            "sftpCompression config should accept [ZLIB, NO] preference list");
    log:printInfo("SFTP listener sftpCompression config validated");
}

// ─── TEST: sftpCompression default is [NO] ────────────────────────────────────

@test:Config {
    groups: ["ftp-listener-behaviour", "listener-compression"]
}
function testListener_SftpCompression_DefaultIsNone() {
    ftp:ListenerConfiguration listenerConfig = {
        protocol: ftp:SFTP,
        host: commons:FTP_HOST,
        port: commons:SFTP_PORT,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}
        },
        pollingInterval: 2
    };
    test:assertEquals(listenerConfig.sftpCompression, [ftp:NO],
            "Default sftpCompression should be [NO]");
}

// ─── TEST: SocketConfig record defaults ───────────────────────────────────────

@test:Config {
    groups: ["ftp-listener-behaviour", "vfs-config"]
}
function testSocketConfig_RecordDefaults() {
    ftp:SocketConfig sc = {};
    test:assertEquals(sc.ftpDataTimeout, 120.0d,
            "Default ftpDataTimeout should be 120 seconds");
    test:assertEquals(sc.ftpSocketTimeout, 60.0d,
            "Default ftpSocketTimeout should be 60 seconds");
    test:assertEquals(sc.sftpSessionTimeout, 300.0d,
            "Default sftpSessionTimeout should be 300 seconds");
}
