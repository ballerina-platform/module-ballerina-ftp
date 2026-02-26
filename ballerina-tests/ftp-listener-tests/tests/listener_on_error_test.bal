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

// Record type for testing JSON binding errors
type StrictPerson record {|
    string name;
    int age;
    string email;  // Required field that's missing in test JSON
|};

// Global tracking variables for onError tests
boolean onErrorInvoked = false;
ftp:ContentBindingError? lastBindingError = ();
string lastErrorFilePath = "";
int onErrorInvocationCount = 0;

// Directory for onError tests
const ON_ERROR_TEST_DIR = "/home/in/onerror-tests";

@test:Config {
    enable: true
}
public function testOnErrorBasic() returns error? {
    // Reset state
    onErrorInvoked = false;
    lastBindingError = ();
    lastErrorFilePath = "";
    onErrorInvocationCount = 0;

    // Service with onFileJson that will fail and onError handler
    ftp:Service onErrorService = service object {
        remote function onFileJson(StrictPerson content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
            // This should not be called since binding will fail
            log:printInfo(string `onFileJson invoked for: ${fileInfo.name}`);
        }

        remote function onError(ftp:Error err, ftp:Caller caller) returns error? {
            log:printInfo("onError invoked");
            log:printError("Binding error", err);
            onErrorInvoked = true;
            // Verify that the error is a ContentBindingError
            if err is ftp:ContentBindingError {
                lastBindingError = err;
                // Access filePath from error detail
                lastErrorFilePath = err.detail().filePath ?: "";
            }
            onErrorInvocationCount += 1;
        }
    };

    // Create listener
    ftp:Listener onErrorListener = check new ({
        protocol: ftp:FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: ON_ERROR_TEST_DIR,
        pollingInterval: 4,
        fileNamePattern: "errortest.*\\.json"
    });

    check onErrorListener.attach(onErrorService);
    check onErrorListener.'start();
    runtime:registerListener(onErrorListener);

    // Upload invalid JSON file (missing required field 'email')
    check (<ftp:Client>triggerClient)->putText(ON_ERROR_TEST_DIR + "/errortest.json",
        "{\"name\": \"John\", \"age\": 30}");
    runtime:sleep(10);

    // Cleanup
    runtime:deregisterListener(onErrorListener);
    check onErrorListener.gracefulStop();

    test:assertTrue(onErrorInvoked, "onError should have been invoked");
    test:assertTrue(lastBindingError is ftp:ContentBindingError, "Should have received ContentBindingError");
    test:assertTrue(lastErrorFilePath.endsWith(".json"), "Error file path should end with .json");
}

@test:Config {
    enable: true,
    dependsOn: [testOnErrorBasic]
}
public function testOnErrorWithMinimalParameters() returns error? {
    // Reset state
    onErrorInvoked = false;
    lastBindingError = ();
    onErrorInvocationCount = 0;

    // Service with onError handler with only error parameter
    ftp:Service minimalOnErrorService = service object {
        remote function onFileJson(StrictPerson content, ftp:FileInfo fileInfo) returns error? {
            // This should not be called since binding will fail
            log:printInfo(string `onFileJson invoked for: ${fileInfo.name}`);
        }

        remote function onError(ftp:Error err) returns error? {
            log:printInfo("onError (minimal) invoked");
            log:printError("Binding error", err);
            onErrorInvoked = true;
            // Verify that the error is a ContentBindingError
            if err is ftp:ContentBindingError {
                lastBindingError = err;
            }
            onErrorInvocationCount += 1;
        }
    };

    // Create listener
    ftp:Listener minimalListener = check new ({
        protocol: ftp:FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: ON_ERROR_TEST_DIR,
        pollingInterval: 4,
        fileNamePattern: "minimal.*\\.json"
    });

    check minimalListener.attach(minimalOnErrorService);
    check minimalListener.'start();
    runtime:registerListener(minimalListener);

    // Upload invalid JSON
    check (<ftp:Client>triggerClient)->putText(ON_ERROR_TEST_DIR + "/minimal.json",
        "{\"name\": \"Jane\", \"age\": 25}");
    runtime:sleep(10);

    // Cleanup
    runtime:deregisterListener(minimalListener);
    check minimalListener.gracefulStop();

    test:assertTrue(onErrorInvoked, "onError (minimal) should have been invoked");
    test:assertTrue(lastBindingError is ftp:ContentBindingError, "Should have received ContentBindingError");
}

@test:Config {
    enable: true,
    dependsOn: [testOnErrorWithMinimalParameters]
}
public function testOnErrorWithCallerOperations() returns error? {
    // Reset state
    onErrorInvoked = false;
    lastBindingError = ();
    lastErrorFilePath = "";
    onErrorInvocationCount = 0;

    // Service that uses caller to perform operations on failed files
    ftp:Service moveOnErrorService = service object {
        remote function onFileJson(StrictPerson content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
            log:printInfo(string `onFileJson invoked for: ${fileInfo.name}`);
        }

        remote function onError(ftp:Error err, ftp:Caller caller) returns error? {
            onErrorInvoked = true;
            onErrorInvocationCount += 1;

            // Verify that the error is a ContentBindingError and access details
            if err is ftp:ContentBindingError {
                string? filePath = err.detail().filePath;
                log:printInfo(string `onError invoked for file: ${filePath ?: "unknown"}`);
                lastBindingError = err;
                lastErrorFilePath = filePath ?: "";

                // Try to delete the problematic file using caller
                if filePath is string {
                    ftp:Error? deleteResult = caller->delete(filePath);
                    if deleteResult is () {
                        log:printInfo("Successfully deleted error file");
                    } else {
                        log:printError("Failed to delete error file", deleteResult);
                    }
                }
            } else {
                log:printError("Unexpected error type", err);
            }
        }
    };

    // Create listener
    ftp:Listener moveListener = check new ({
        protocol: ftp:FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: ON_ERROR_TEST_DIR,
        pollingInterval: 4,
        fileNamePattern: "moveerror.*\\.json"
    });

    check moveListener.attach(moveOnErrorService);
    check moveListener.'start();
    runtime:registerListener(moveListener);

    // Upload invalid JSON
    check (<ftp:Client>triggerClient)->putText(ON_ERROR_TEST_DIR + "/moveerror.json",
        "{\"name\": \"Bob\", \"age\": 40}");
    runtime:sleep(10);

    // Cleanup
    runtime:deregisterListener(moveListener);
    check moveListener.gracefulStop();

    test:assertTrue(onErrorInvoked, "onError should have been invoked");
    test:assertTrue(lastBindingError is ftp:ContentBindingError, "Should have received ContentBindingError");
}

@test:Config {
    enable: true,
    dependsOn: [testOnErrorWithCallerOperations]
}
public function testContentBindingErrorType() returns error? {
    // Test that ContentBindingError is a distinct error type with detail record
    ftp:ContentBindingError testError = error ftp:ContentBindingError("Test binding error",
        filePath = "/test/file.json",
        content = [72, 101, 108, 108, 111]  // "Hello" as bytes
    );

    test:assertTrue(testError is ftp:Error, "ContentBindingError should be a subtype of Error");
    test:assertEquals(testError.message(), "Test binding error", "Error message should match");
    test:assertEquals(testError.detail().filePath, "/test/file.json", "filePath should match");
    test:assertEquals(testError.detail().content, [72, 101, 108, 108, 111], "content should match");
}
