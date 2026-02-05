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
// Tracking variables – written by the service handlers, read by assertions.
// ---------------------------------------------------------------------------
// Multi-path routing
int    routeAEventCount    = 0;
string routeALastFileName  = "";
int    routeBEventCount    = 0;
string routeBLastFileName  = "";

// Single-service @ServiceConfig
boolean singleSvcEventReceived = false;
string  singleSvcFileName      = "";

// ---------------------------------------------------------------------------
// Multi-path routing listener – auto-started by the runtime.
// Services are attached via the "on" clause; the path on the listener itself
// is irrelevant because @ServiceConfig overrides it for every attached service.
// ---------------------------------------------------------------------------
listener Listener routingListener = check new ({
    protocol: FTP,
    host: "127.0.0.1",
    auth: {credentials: {username: "wso2", password: "wso2123"}},
    port: 21212,
    pollingInterval: 2
});

@ServiceConfig {path: SC_ROUTE_A_DIR}
service on routingListener {
    remote function onFileText(string content, FileInfo fileInfo) returns error? {
        log:printInfo(string `[RouteA] received: ${fileInfo.name}`);
        routeALastFileName = fileInfo.name;
        routeAEventCount   = routeAEventCount + 1;
    }
}

@ServiceConfig {path: SC_ROUTE_B_DIR}
service on routingListener {
    remote function onFileText(string content, FileInfo fileInfo) returns error? {
        log:printInfo(string `[RouteB] received: ${fileInfo.name}`);
        routeBLastFileName = fileInfo.name;
        routeBEventCount   = routeBEventCount + 1;
    }
}

// ---------------------------------------------------------------------------
// Single-service listener – verifies a lone @ServiceConfig service works.
// ---------------------------------------------------------------------------
listener Listener singleListener = check new ({
    protocol: FTP,
    host: "127.0.0.1",
    auth: {credentials: {username: "wso2", password: "wso2123"}},
    port: 21212,
    pollingInterval: 2
});

@ServiceConfig {path: SC_SINGLE_DIR}
service on singleListener {
    remote function onFileText(string content, FileInfo fileInfo) returns error? {
        log:printInfo(string `[Single] received: ${fileInfo.name}`);
        singleSvcFileName      = fileInfo.name;
        singleSvcEventReceived = true;
    }
}

// ---------------------------------------------------------------------------
// Helper – upload a local file to an FTP path, blocking until the server
// acknowledges the transfer.
// ---------------------------------------------------------------------------
function uploadText(string remotePath, string localPath) returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(localPath, 5);
    check (<Client>clientEp)->put(remotePath, bStream);
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
public function testMultiPathRoutingRouteA() returns error? {
    // Reset counters before the upload
    routeAEventCount   = 0;
    routeBEventCount   = 0;
    routeALastFileName = "";
    routeBLastFileName = "";

    // Upload ONLY to route-A
    check uploadText(SC_ROUTE_A_DIR + "/routeA_only.txt", "tests/resources/datafiles/file2.txt");

    // Wait long enough for at least two polling cycles
    boolean received = waitFor(function() returns boolean { return routeAEventCount > 0; }, 30);

    test:assertTrue(received,         "RouteA handler must fire for a file in its directory");
    test:assertEquals("routeA_only.txt", routeALastFileName);
    test:assertEquals(0,              routeBEventCount, "RouteB handler must NOT fire for a RouteA file");
}

@test:Config {
    dependsOn: [testMultiPathRoutingRouteA]
}
public function testMultiPathRoutingRouteB() returns error? {
    // Reset counters – RouteA counter may still be non-zero from the previous test;
    // we only care that RouteB goes from 0 to 1.
    routeBEventCount   = 0;
    routeBLastFileName = "";
    int routeABefore   = routeAEventCount;

    // Upload ONLY to route-B
    check uploadText(SC_ROUTE_B_DIR + "/routeB_only.txt", "tests/resources/datafiles/file2.txt");

    boolean received = waitFor(function() returns boolean { return routeBEventCount > 0; }, 30);

    test:assertTrue(received,         "RouteB handler must fire for a file in its directory");
    test:assertEquals("routeB_only.txt", routeBLastFileName);
    test:assertEquals(routeABefore,   routeAEventCount, "RouteA handler must NOT fire for a RouteB file");
}

// ===========================================================================
// TEST: Single service with @ServiceConfig – basic smoke
// ===========================================================================
@test:Config {}
public function testSingleServiceWithAnnotation() returns error? {
    singleSvcEventReceived = false;
    singleSvcFileName      = "";

    check uploadText(SC_SINGLE_DIR + "/single_test.txt", "tests/resources/datafiles/file2.txt");

    boolean received = waitFor(function() returns boolean { return singleSvcEventReceived; }, 30);

    test:assertTrue(received, "Single @ServiceConfig service must receive its file event");
    test:assertEquals("single_test.txt", singleSvcFileName);
}

// ===========================================================================
// TEST: Backward compatibility – services without @ServiceConfig still work
// when the listener carries the path (legacy mode).
// ===========================================================================
@test:Config {}
public function testBackwardCompatibility() returns error? {
    boolean legacyReceived = false;
    string  legacyFileName = "";

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
    check legacyListener.gracefulStop();

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
    int  legacyFanOutCount = 0;

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
    check fanOutListener.gracefulStop();

    test:assertTrue(received, "Both legacy services must receive the same event (fan-out)");
    test:assertEquals(2, legacyFanOutCount);
}

// ===========================================================================
// TEST: ServiceConfiguration record structure – compile-time shape check
// ===========================================================================
@test:Config {}
public function testServiceConfigRecordStructure() returns error? {
    ServiceConfiguration fullConfig = {
        path: "/incoming/csv",
        fileNamePattern: ".*\\.csv",
        fileAgeFilter: {
            minAge: 10,
            maxAge: 3600,
            ageCalculationMode: LAST_MODIFIED
        },
        fileDependencyConditions: [
            {
                targetPattern: "order_(\\d+)\\.csv",
                requiredFiles: ["order_$1.marker"],
                matchingMode: ALL,
                requiredFileCount: 1
            }
        ]
    };

    test:assertEquals(fullConfig.path, "/incoming/csv");
    test:assertEquals(fullConfig.fileNamePattern, ".*\\.csv");
    test:assertEquals(fullConfig.fileAgeFilter?.minAge, 10d);
    test:assertEquals(fullConfig.fileAgeFilter?.maxAge, 3600d);
    test:assertEquals(fullConfig.fileDependencyConditions.length(), 1);
}

@test:Config {}
public function testServiceConfigRecordDefaults() returns error? {
    // Only the required field; everything else takes its zero-value or default
    ServiceConfiguration minConfig = {
        path: "/data"
    };

    test:assertEquals(minConfig.path, "/data");
    test:assertTrue(minConfig.fileNamePattern is ());
    test:assertTrue(minConfig.fileAgeFilter is ());
    test:assertEquals(minConfig.fileDependencyConditions.length(), 0);
}

// ===========================================================================
// TEST: Relative path in @ServiceConfig – FTP resolves relative to user home
// ===========================================================================
@test:Config {}
public function testServiceConfigRelativePath() returns error? {
    // Verifies that the record accepts a relative path (no leading /).
    // The consumer will resolve it against the FTP user's home directory.
    ServiceConfiguration relConfig = {
        path: "relative/subdir"
    };
    test:assertEquals(relConfig.path, "relative/subdir");
}
