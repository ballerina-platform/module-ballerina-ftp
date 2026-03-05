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

import ballerina/lang.runtime;
import ballerina/log;
import ballerina/test;

const LISTENER_RETRY_TEST_DIR = "/home/in/listener-retry";

// Verifies that retryConfig is accepted by ListenerConfiguration and passed through
// FtpListenerHelper → FtpListener → FtpContentCallbackHandler without interfering
// with normal file delivery on the happy path.
@test:Config {}
public function testListenerRetryConfig_SuccessPath() returns error? {
    boolean onFileInvoked = false;
    string receivedContent = "";

    Service retrySuccessService = service object {
        remote function onFileText(string content, FileInfo fileInfo, Caller caller) returns error? {
            log:printInfo(string `[retry-success] onFileText invoked: ${fileInfo.name}`);
            receivedContent = content;
            onFileInvoked = true;
        }
    };

    Listener retryListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: LISTENER_RETRY_TEST_DIR,
        pollingInterval: 3,
        fileNamePattern: "retry-success.*\\.txt",
        retryConfig: {
            count: 3,
            interval: 0.5,
            backOffFactor: 2.0,
            maxWaitInterval: 5.0
        }
    });

    check retryListener.attach(retrySuccessService);
    check retryListener.'start();
    runtime:registerListener(retryListener);

    check (<Client>clientEp)->putText(LISTENER_RETRY_TEST_DIR + "/retry-success.txt",
        "retry config test content");
    runtime:sleep(10);

    runtime:deregisterListener(retryListener);
    check retryListener.gracefulStop();

    test:assertTrue(onFileInvoked,
        "onFileText should be invoked when retryConfig is configured");
    test:assertEquals(receivedContent, "retry config test content",
        "Content should be delivered correctly with retryConfig");
}
