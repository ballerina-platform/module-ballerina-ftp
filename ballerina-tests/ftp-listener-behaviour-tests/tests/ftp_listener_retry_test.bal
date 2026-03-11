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

const string RETRY_LISTENER_DIR = "/home/in/listener-retry";

ftp:Client retryListenerClient = check new ({
    protocol: ftp:FTP,
    host: commons:FTP_HOST,
    port: commons:AUTH_FTP_PORT,
    auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}}
});

// ─── TEST: retryConfig — happy path: file is delivered correctly ───────────────
//
// Verifies that retryConfig is accepted by ListenerConfiguration and passed
// through FtpListenerHelper → FtpListener → FtpContentCallbackHandler without
// interfering with normal file delivery on the happy path.

boolean retryListenerDelivered = false;
string retryListenerContent = "";

@test:Config {
    groups: ["ftp-listener-behaviour", "vfs-config"]
}
function testListenerRetryConfig_SuccessPath() returns error? {
        retryListenerDelivered = false;
        retryListenerContent = "";

    ftp:Service retrySvc = @ftp:ServiceConfig {
        path: RETRY_LISTENER_DIR,
        fileNamePattern: "retry-success.*\\.txt"
    }
    service object {
        remote function onFileText(string content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
            log:printInfo(string `[retry-success] onFileText invoked: ${fileInfo.name}`);
                retryListenerContent = content;
                retryListenerDelivered = true;
        }
    };

    ftp:Listener retryListener = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        pollingInterval: 3,
        retryConfig: {
            count: 3,
            interval: 0.5,
            backOffFactor: 2.0,
            maxWaitInterval: 5.0
        }
    });

    check retryListener.attach(retrySvc);
    check retryListener.'start();
    runtime:registerListener(retryListener);

    check retryListenerClient->putText(RETRY_LISTENER_DIR + "/retry-success.txt",
        "retry config test content");

    boolean delivered = waitUntilVfs(function() returns boolean {
        return retryListenerDelivered;
    }, 20);

    runtime:deregisterListener(retryListener);
    check retryListener.gracefulStop();

    test:assertTrue(delivered,
        "onFileText should be invoked when retryConfig is configured");
    test:assertEquals(retryListenerContent, "retry config test content",
        "Content should be delivered correctly with retryConfig");

    check deleteIfExistsVfs(retryListenerClient, RETRY_LISTENER_DIR + "/retry-success.txt");

}

// ─── TEST: RetryConfig record — default values ────────────────────────────────

@test:Config {
    groups: ["ftp-listener-behaviour", "vfs-config"]
}
function testRetryConfig_RecordDefaults() {
    ftp:RetryConfig cfg = {};
    test:assertEquals(cfg.count, 3, "Default count should be 3");
    test:assertEquals(cfg.interval, 1.0d, "Default interval should be 1.0");
    test:assertEquals(cfg.backOffFactor, 2.0d, "Default backOffFactor should be 2.0");
    test:assertEquals(cfg.maxWaitInterval, 30.0d, "Default maxWaitInterval should be 30.0");
}

// ─── TEST: retryConfig validation — invalid count ─────────────────────────────

@test:Config {
    groups: ["ftp-listener-behaviour", "vfs-config"]
}
function testListenerRetryConfig_InvalidCount() returns error? {
    ftp:Listener|ftp:Error result = new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        retryConfig: {count: 0, interval: 1.0, backOffFactor: 2.0, maxWaitInterval: 30.0}
    });
    test:assertTrue(result is ftp:InvalidConfigError,
        "count=0 should produce ftp:InvalidConfigError");
    if result is ftp:InvalidConfigError {
        test:assertTrue(result.message().includes("count"),
            "Error message should mention 'count': " + result.message());
    }
}

// ─── TEST: retryConfig validation — invalid interval ─────────────────────────

@test:Config {
    groups: ["ftp-listener-behaviour", "vfs-config"]
}
function testListenerRetryConfig_InvalidInterval() returns error? {
    ftp:Listener|ftp:Error result = new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        retryConfig: {count: 3, interval: 0.0, backOffFactor: 2.0, maxWaitInterval: 30.0}
    });
    test:assertTrue(result is ftp:InvalidConfigError,
        "interval=0 should produce ftp:InvalidConfigError");
    if result is ftp:InvalidConfigError {
        test:assertTrue(result.message().includes("interval"),
            "Error message should mention 'interval': " + result.message());
    }
}

// ─── TEST: retryConfig validation — backOffFactor below minimum ───────────────

@test:Config {
    groups: ["ftp-listener-behaviour", "vfs-config"]
}
function testListenerRetryConfig_InvalidBackOffFactor() returns error? {
    ftp:Listener|ftp:Error result = new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        retryConfig: {count: 3, interval: 1.0, backOffFactor: 0.5, maxWaitInterval: 30.0}
    });
    test:assertTrue(result is ftp:InvalidConfigError,
        "backOffFactor=0.5 (< 1.0) should produce ftp:InvalidConfigError");
    if result is ftp:InvalidConfigError {
        test:assertTrue(result.message().includes("backOffFactor"),
            "Error message should mention 'backOffFactor': " + result.message());
    }
}

// ─── TEST: retryConfig validation — maxWaitInterval is zero ──────────────────

@test:Config {
    groups: ["ftp-listener-behaviour", "vfs-config"]
}
function testListenerRetryConfig_InvalidMaxWaitInterval() returns error? {
    ftp:Listener|ftp:Error result = new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        retryConfig: {count: 3, interval: 1.0, backOffFactor: 2.0, maxWaitInterval: 0.0}
    });
    test:assertTrue(result is ftp:InvalidConfigError,
        "maxWaitInterval=0 should produce ftp:InvalidConfigError");
    if result is ftp:InvalidConfigError {
        test:assertTrue(result.message().includes("maxWaitInterval"),
            "Error message should mention 'maxWaitInterval': " + result.message());
    }
}

// ─── TEST: retryConfig validation — maxWaitInterval less than interval ────────

@test:Config {
    groups: ["ftp-listener-behaviour", "vfs-config"]
}
function testListenerRetryConfig_MaxWaitLessThanInterval() returns error? {
    ftp:Listener|ftp:Error result = new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        retryConfig: {count: 3, interval: 10.0, backOffFactor: 2.0, maxWaitInterval: 5.0}
    });
    test:assertTrue(result is ftp:InvalidConfigError,
        "maxWaitInterval < interval should produce ftp:InvalidConfigError");
    if result is ftp:InvalidConfigError {
        test:assertTrue(result.message().includes("maxWaitInterval"),
            "Error message should mention 'maxWaitInterval': " + result.message());
    }
}
