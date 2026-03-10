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

isolated boolean retryListenerDelivered = false;
isolated string retryListenerContent = "";

@test:Config {
    groups: ["ftp-listener-behaviour", "vfs-config"]
}
function testListenerRetryConfig_SuccessPath() returns error? {
    lock {
        retryListenerDelivered = false;
        retryListenerContent = "";
    }

    ftp:Service retrySvc = @ftp:ServiceConfig {
        path: RETRY_LISTENER_DIR,
        fileNamePattern: "retry-success.*\\.txt"
    }
    service object {
        remote function onFileText(string content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
            log:printInfo(string `[retry-success] onFileText invoked: ${fileInfo.name}`);
            lock {
                retryListenerContent = content;
                retryListenerDelivered = true;
            }
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
        lock { return retryListenerDelivered; }
    }, 20);

    runtime:deregisterListener(retryListener);
    check retryListener.gracefulStop();

    deleteIfExistsVfs(retryListenerClient, RETRY_LISTENER_DIR + "/retry-success.txt");

    test:assertTrue(delivered,
        "onFileText should be invoked when retryConfig is configured");
    string content;
    lock { content = retryListenerContent; }
    test:assertEquals(content, "retry config test content",
        "Content should be delivered correctly with retryConfig");
}
