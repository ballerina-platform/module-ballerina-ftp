// Copyright (c) 2026 WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/io;
import ballerina/lang.runtime;
import ballerina/log;
import ballerina/test;

// ---------------------------------------------------------------------------
// Isolated directories – each test group gets its own FTP subtree so that
// polling from one listener never picks up files meant for another.
// ---------------------------------------------------------------------------
const string SC_ROUTE_A_DIR  = "/home/in/sc-route-a";
const string SC_ROUTE_B_DIR  = "/home/in/sc-route-b";
const string SC_SINGLE_DIR   = "/home/in/sc-single";
const string SC_LEGACY_DIR   = "/home/in/sc-legacy";

// ---------------------------------------------------------------------------
// Test-specific client for file uploads
// ---------------------------------------------------------------------------
Client? testClient = ();

// ---------------------------------------------------------------------------
// Tracking variables – written by the service handlers, read by assertions.
// Initialized once at module load; reset at the start of each test.
// ---------------------------------------------------------------------------
int    routeAEventCount    = 0;
string routeALastFileName  = "";
int    routeBEventCount    = 0;
string routeBLastFileName  = "";

boolean singleSvcEventReceived = false;
string  singleSvcFileName      = "";

boolean legacyReceived = false;
string  legacyFileName = "";

int legacyFanOutCount = 0;

// ---------------------------------------------------------------------------
// Initialize test client and listeners
// ---------------------------------------------------------------------------
@test:BeforeSuite
function initializeTestEnvironment() returns error? {
    // Initialize test client
    testClient = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212
    });
}

@test:AfterSuite
function cleanupTestEnvironment() returns error? {
    if testClient is Client {
        check (<Client>testClient)->close();
    }
}

// ---------------------------------------------------------------------------
// Helper – upload a local file to an FTP path, blocking until the server
// acknowledges the transfer.
// ---------------------------------------------------------------------------
function uploadText(string remotePath, string localPath) returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(localPath, 5);
    check (<Client>testClient)->put(remotePath, bStream);
}

// ---------------------------------------------------------------------------
// Helper – poll a boolean flag with a timeout; returns false on timeout.
// ---------------------------------------------------------------------------
function waitFor(function() returns boolean condition,
                 int timeoutSec) returns boolean {
    int remaining = timeoutSec;
    while remaining > 0 {
        if condition() {
            return true;
        }
        runtime:sleep(1);
        remaining = remaining - 1;
    }
    return false;
}

// ===========================================================================
// TEST: Multi-path routing – the key value-add of @ServiceConfig
//
// Upload one file to route-A's directory and another to route-B's directory.
// After the polling interval, exactly one service should have fired and the
// other should remain at zero.
// ===========================================================================
@test:Config {}
public function testMultiPathRouting() returns error? {

        // Create and start routing listener with two services
    Listener tempRoutingListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        pollingInterval: 2
    });

    Service routeAService = @ServiceConfig {path: SC_ROUTE_A_DIR}
    service object {
        remote function onFileText(string content, FileInfo fileInfo) returns error? {
            log:printInfo(string `[RouteA] received: ${fileInfo.name}`);
            routeALastFileName = fileInfo.name;
            routeAEventCount   = routeAEventCount + 1;
        }
    };

    Service routeBService = @ServiceConfig {path: SC_ROUTE_B_DIR}
    service object {
        remote function onFileText(string content, FileInfo fileInfo) returns error? {
            log:printInfo(string `[RouteB] received: ${fileInfo.name}`);
            routeBLastFileName = fileInfo.name;
            routeBEventCount   = routeBEventCount + 1;
        }
    };

    check tempRoutingListener.attach(routeAService);
    check tempRoutingListener.attach(routeBService);
    check tempRoutingListener.'start();
    runtime:registerListener(tempRoutingListener);

    // Upload ONLY to route-A
    check uploadText(SC_ROUTE_A_DIR + "/routeA_only.txt", "tests/resources/datafiles/file2.txt");
    check uploadText(SC_ROUTE_B_DIR + "/routeB_only.txt", "tests/resources/datafiles/file2.txt");

    // Wait long enough for at least two polling cycles
    boolean received = waitFor(
        function() returns boolean { return routeAEventCount > 0 && routeBEventCount > 0; }, 30);

    test:assertTrue(received,         "RouteA and Route B handler must fire for a file in its directory");
    test:assertEquals("routeA_only.txt", routeALastFileName);
    test:assertEquals("routeB_only.txt", routeBLastFileName);

    runtime:deregisterListener(tempRoutingListener);
    check tempRoutingListener.gracefulStop();
}

// ===========================================================================
// TEST: Single service with @ServiceConfig – basic smoke
// ===========================================================================
@test:Config {}
public function testSingleServiceWithAnnotation() returns error? {

    singleSvcEventReceived = false;
    singleSvcFileName      = "";

    // Create and start single service listener
    Listener tempSingleListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        pollingInterval: 2
    });

    Service singleService = @ServiceConfig {path: SC_SINGLE_DIR}
    service object {
        remote function onFileText(string content, FileInfo fileInfo) returns error? {
            log:printInfo(string `[Single] received: ${fileInfo.name}`);
            singleSvcFileName      = fileInfo.name;
            singleSvcEventReceived = true;
        }
    };

    check tempSingleListener.attach(singleService);
    check tempSingleListener.'start();
    runtime:registerListener(tempSingleListener);

    check uploadText(SC_SINGLE_DIR + "/single_test.txt", "tests/resources/datafiles/file2.txt");

    boolean received = waitFor(function() returns boolean { return singleSvcEventReceived; }, 30);

    test:assertTrue(received, "Single @ServiceConfig service must receive its file event");
    test:assertEquals("single_test.txt", singleSvcFileName);

    runtime:deregisterListener(tempSingleListener);
    check tempSingleListener.immediateStop();

}

// ===========================================================================
// TEST: Backward compatibility – services without @ServiceConfig still work
// when the listener carries the path (legacy mode).
// ===========================================================================
@test:Config {}
public function testBackwardCompatibility() returns error? {
    legacyReceived = false;
    legacyFileName = "";

    Service legacyService = service object {
        remote function onFileText(string content, FileInfo fileInfo) returns error? {
            log:printInfo(string `[Legacy] received: ${fileInfo.name}`);
            legacyFileName = fileInfo.name;
            legacyReceived = true;
        }
    };

    Listener legacyListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: SC_LEGACY_DIR,
        pollingInterval: 2,
        fileNamePattern: "legacy.*\\.txt"
    });

    check legacyListener.attach(legacyService);
    check legacyListener.'start();
    runtime:registerListener(legacyListener);

    check uploadText(SC_LEGACY_DIR + "/legacy_compat.txt", "tests/resources/datafiles/file2.txt");

    boolean received = waitFor(function() returns boolean { return legacyReceived; }, 30);

    runtime:deregisterListener(legacyListener);
    check legacyListener.immediateStop();

    test:assertTrue(received, "Legacy (no annotation) service must still receive events via listener path");
    test:assertEquals("legacy_compat.txt", legacyFileName);
}

// ===========================================================================
// TEST: Multiple legacy services on the same listener – backward-compat fan-out
// ===========================================================================
@test:Config {
    dependsOn: [testBackwardCompatibility]
}
public function testMultipleServicesBackwardCompatible() returns error? {
    legacyFanOutCount = 0;

    Service legacyService1 = service object {
        remote function onFileText(string content, FileInfo fileInfo) returns error? {
            log:printInfo(string `[LegacyFan1] received: ${fileInfo.name}`);
            legacyFanOutCount = legacyFanOutCount + 1;
        }
    };

    Service legacyService2 = service object {
        remote function onFileText(string content, FileInfo fileInfo) returns error? {
            log:printInfo(string `[LegacyFan2] received: ${fileInfo.name}`);
            legacyFanOutCount = legacyFanOutCount + 1;
        }
    };

    Listener fanOutListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: SC_LEGACY_DIR,
        pollingInterval: 2,
        fileNamePattern: "fanout.*\\.txt"
    });

    check fanOutListener.attach(legacyService1);
    check fanOutListener.attach(legacyService2);
    check fanOutListener.'start();
    runtime:registerListener(fanOutListener);

    check uploadText(SC_LEGACY_DIR + "/fanout_test.txt", "tests/resources/datafiles/file2.txt");

    // Both services should fire – wait until count reaches 2
    boolean received = waitFor(function() returns boolean { return legacyFanOutCount >= 2; }, 30);

    runtime:deregisterListener(fanOutListener);
    check fanOutListener.immediateStop();

    test:assertTrue(received, "Both legacy services must receive the same event (fan-out)");
    test:assertEquals(2, legacyFanOutCount);
}


// ===========================================================================
// TEST: Duplicate path registration error – verifying error handling
// ===========================================================================
@test:Config {}
public function testDuplicatePathRegistrationError() returns error? {
    // This test verifies that attempting to register two services with the same
    // path on the same listener results in an error.

    Service duplicateService1 = @ServiceConfig {path: "/home/in/duplicate-test"}
    service object {
        remote function onFileText(string content, FileInfo fileInfo) returns error? {
            log:printInfo(string `[Duplicate1] received: ${fileInfo.name}`);
        }
    };

    Service duplicateService2 = @ServiceConfig {path: "/home/in/duplicate-test"}
    service object {
        remote function onFileText(string content, FileInfo fileInfo) returns error? {
            log:printInfo(string `[Duplicate2] received: ${fileInfo.name}`);
        }
    };

    Listener duplicateListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        pollingInterval: 2
    });

    // Attach first service with path /home/in/duplicate-test
    check duplicateListener.attach(duplicateService1);

    // Attempt to attach second service with the same path - should fail
    error? attachResult = duplicateListener.attach(duplicateService2);

    test:assertTrue(attachResult is error, "Attaching a service with duplicate path should return an error");

    if attachResult is error {
        string errorMsg = attachResult.message();
        test:assertTrue(errorMsg.includes("Duplicate path"),
            "Error message should mention duplicate path, got: " + errorMsg);
    }

    // Clean up
    check duplicateListener.immediateStop();
}

// ===========================================================================
// TEST: Mixed services error – some with @ServiceConfig, some without
// ===========================================================================
@test:Config {}
public function testMixedServicesError() returns error? {
    // This test verifies that mixing services with and without @ServiceConfig
    // annotation on the same listener results in an error.

    Service annotatedService = @ServiceConfig {path: "/home/in/mixed-test-annotated"}
    service object {
        remote function onFileText(string content, FileInfo fileInfo) returns error? {
            log:printInfo(string `[Annotated] received: ${fileInfo.name}`);
        }
    };

    Service nonAnnotatedService = service object {
        remote function onFileText(string content, FileInfo fileInfo) returns error? {
            log:printInfo(string `[NonAnnotated] received: ${fileInfo.name}`);
        }
    };

    Listener mixedListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        pollingInterval: 2
    });

    // Attach service WITH @ServiceConfig annotation
    check mixedListener.attach(annotatedService);

    // Attempt to attach service WITHOUT @ServiceConfig - should fail
    error? attachResult = mixedListener.attach(nonAnnotatedService);

    test:assertTrue(attachResult is error,
        "Attaching a non-annotated service when annotated services exist should return an error");

    if attachResult is error {
        string errorMsg = attachResult.message();
        test:assertTrue(errorMsg.includes("ServiceConfig") || errorMsg.includes("annotation"),
            "Error message should mention ServiceConfig or annotation, got: " + errorMsg);
    }

    // Clean up
    check mixedListener.immediateStop();

    // Now test the opposite: attach non-annotated first, then annotated
    Listener mixedListener2 = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        pollingInterval: 2
    });

    // Attach service WITHOUT @ServiceConfig annotation first
    check mixedListener2.attach(nonAnnotatedService);

    // Attempt to attach service WITH @ServiceConfig - should fail
    error? attachResult2 = mixedListener2.attach(annotatedService);

    test:assertTrue(attachResult2 is error,
        "Attaching an annotated service when non-annotated services exist should return an error");

    if attachResult2 is error {
        string errorMsg = attachResult2.message();
        test:assertTrue(errorMsg.includes("ServiceConfig") || errorMsg.includes("annotation"),
            "Error message should mention ServiceConfig or annotation, got: " + errorMsg);
    }

    // Clean up
    check mixedListener2.immediateStop();
}

// ===========================================================================
// TEST: Record structure validation – compile-time checks
// ===========================================================================
@test:Config {}
public function testServiceConfigRecordStructure() {
    // This test verifies the ServiceConfiguration record accepts all fields
    ServiceConfiguration fullConfig = {
        path: "/home/in/test",
        fileNamePattern: ".*\\.txt",
        fileAgeFilter: {
            minAge: 10.0,
            maxAge: 3600.0,
            ageCalculationMode: LAST_MODIFIED
        },
        fileDependencyConditions: [
            {
                targetPattern: "data-(\\d+)\\.txt",
                requiredFiles: ["marker-$1.flag"],
                matchingMode: ALL,
                requiredFileCount: 1
            }
        ]
    };

    test:assertEquals(fullConfig.path, "/home/in/test");
    test:assertEquals(fullConfig.fileNamePattern, ".*\\.txt");
}

// ===========================================================================
// TEST: Record default values
// ===========================================================================
@test:Config {}
public function testServiceConfigRecordDefaults() {
    // This test verifies default values for optional fields
    ServiceConfiguration minConfig = {
        path: "/home/in/minimal"
    };

    test:assertEquals(minConfig.path, "/home/in/minimal");
    test:assertEquals(minConfig.fileDependencyConditions.length(), 0);
}

// ===========================================================================
// TEST: Relative path acceptance
// ===========================================================================
@test:Config {}
public function testServiceConfigRelativePath() {
    // This test verifies that relative paths are accepted (FTP resolves them)
    ServiceConfiguration relativeConfig = {
        path: "relative/path"
    };

    test:assertEquals(relativeConfig.path, "relative/path");
}

// ===========================================================================
// TEST: Invalid regex pattern in @ServiceConfig fileNamePattern
// ===========================================================================
@test:Config {}
public function testServiceConfigInvalidFileNamePattern() returns error? {
    ListenerConfiguration config = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21212,
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        pollingInterval: 2
    };
    Listener ftpListener = check new (config);

    Service invalidRegexService = @ServiceConfig {
        path: "/home/in/invalid-regex",
        fileNamePattern: "[unclosed"  // Invalid regex - unclosed bracket
    } service object {
        remote function onFileChange(Caller caller, WatchEvent & readonly event) {
        }
    };

    error? result = ftpListener.attach(invalidRegexService, "invalidRegexService");
    test:assertTrue(result is InvalidConfigError,
        msg = "Expected InvalidConfigError for invalid regex pattern in @ServiceConfig.fileNamePattern");
    if result is InvalidConfigError {
        test:assertTrue(result.message().includes("Invalid regex pattern"),
            msg = "InvalidConfigError message should indicate invalid regex");
    }
}

// ===========================================================================
// TEST: Invalid regex pattern in @ServiceConfig fileDependencyConditions targetPattern
// ===========================================================================
@test:Config {}
public function testServiceConfigInvalidTargetPattern() returns error? {
    ListenerConfiguration config = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21212,
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        pollingInterval: 2
    };
    Listener ftpListener = check new (config);

    Service invalidTargetService = @ServiceConfig {
        path: "/home/in/invalid-target",
        fileDependencyConditions: [
            {
                targetPattern: "(invalid*regex",  // Invalid regex - unclosed parenthesis
                requiredFiles: ["data.txt"]
            }
        ]
    } service object {
        remote function onFileChange(Caller caller, WatchEvent & readonly event) {
        }
    };

    error? result = ftpListener.attach(invalidTargetService, "invalidTargetService");
    test:assertTrue(result is InvalidConfigError,
        msg = "Expected InvalidConfigError for invalid regex pattern in @ServiceConfig.fileDependencyConditions.targetPattern");
    if result is InvalidConfigError {
        test:assertTrue(result.message().includes("Invalid regex pattern"),
            msg = "InvalidConfigError message should indicate invalid regex");
    }
}

// ===========================================================================
// TEST: Invalid regex pattern in @ServiceConfig fileDependencyConditions requiredFiles
// ===========================================================================
@test:Config {}
public function testServiceConfigInvalidRequiredFilesPattern() returns error? {
    ListenerConfiguration config = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21212,
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        pollingInterval: 2
    };
    Listener ftpListener = check new (config);

    Service invalidRequiredService = @ServiceConfig {
        path: "/home/in/invalid-required",
        fileDependencyConditions: [
            {
                targetPattern: "data.*\\.csv",
                requiredFiles: ["[broken-regex"]  // Invalid regex - unclosed bracket
            }
        ]
    } service object {
        remote function onFileChange(Caller caller, WatchEvent & readonly event) {
        }
    };

    error? result = ftpListener.attach(invalidRequiredService, "invalidRequiredService");
    test:assertTrue(result is InvalidConfigError,
        msg = "Expected InvalidConfigError for invalid regex pattern in @ServiceConfig.fileDependencyConditions.requiredFiles");
    if result is InvalidConfigError {
        test:assertTrue(result.message().includes("Invalid regex pattern"),
            msg = "InvalidConfigError message should indicate invalid regex");
    }
}
