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

// ─── Shared clients ───────────────────────────────────────────────────────────

ftp:Client ftpClient = check new ({
    protocol: ftp:FTP,
    host: commons:FTP_HOST,
    port: commons:AUTH_FTP_PORT,
    auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
    userDirIsRoot: false
});

ftp:Client retryClient = check new ({
    protocol: ftp:FTP,
    host: commons:FTP_HOST,
    port: commons:AUTH_FTP_PORT,
    auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
    userDirIsRoot: false,
    retryConfig: {
        count: 3,
        interval: 0.5,
        backOffFactor: 2.0,
        maxWaitInterval: 5.0
    }
});

ftp:Client customRetryClient = check new ({
    protocol: ftp:FTP,
    host: commons:FTP_HOST,
    port: commons:AUTH_FTP_PORT,
    auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
    userDirIsRoot: false,
    retryConfig: {
        count: 2,
        interval: 0.2,
        backOffFactor: 1.5,
        maxWaitInterval: 1.0
    }
});

// =============================================================================
// Retry — success paths (no retry needed)
// =============================================================================

@test:Config {
    groups: ["ftp-client-behaviour", "retry"]
}
function testGetBytesWithRetryConfig_Success() returns error? {
    byte[] result = check retryClient->getBytes(commons:HOME_IN + "/test1.txt");
    test:assertTrue(result.length() > 0, "Should return non-empty bytes with retry config");
}

@test:Config {
    groups: ["ftp-client-behaviour", "retry"],
    dependsOn: [testGetBytesWithRetryConfig_Success]
}
function testGetTextWithRetryConfig_Success() returns error? {
    string result = check retryClient->getText(commons:HOME_IN + "/test1.txt");
    test:assertTrue(result.length() > 0, "Should return non-empty text with retry config");
}

@test:Config {
    groups: ["ftp-client-behaviour", "retry"],
    dependsOn: [testGetTextWithRetryConfig_Success]
}
function testGetJsonWithRetryConfig_Success() returns error? {
    string path = commons:HOME_IN + "/beh-retry-test.json";
    json payload = {name: "retry-test", value: 42};
    check retryClient->putJson(path, payload);
    json result = check retryClient->getJson(path);
    test:assertEquals(result, payload, "JSON content should round-trip correctly with retry config");
    check retryClient->delete(path);
}

@test:Config {
    groups: ["ftp-client-behaviour", "retry"],
    dependsOn: [testGetJsonWithRetryConfig_Success]
}
function testGetXmlWithRetryConfig_Success() returns error? {
    string path = commons:HOME_IN + "/beh-retry-test.xml";
    xml payload = xml `<root><item>retry-test</item></root>`;
    check retryClient->putXml(path, payload);
    xml result = check retryClient->getXml(path);
    test:assertEquals(result.toString(), payload.toString(), "XML content should round-trip correctly with retry config");
    check retryClient->delete(path);
}

@test:Config {
    groups: ["ftp-client-behaviour", "retry"],
    dependsOn: [testGetXmlWithRetryConfig_Success]
}
function testGetCsvWithRetryConfig_Success() returns error? {
    string path = commons:HOME_IN + "/beh-retry-test.csv";
    string[][] payload = [["id", "name"], ["1", "Alice"], ["2", "Bob"]];
    check retryClient->putCsv(path, payload);
    string[][] result = check retryClient->getCsv(path);
    test:assertEquals(result.length(), 2, "Should return 2 data rows");
    test:assertEquals(result[0][1], "Alice", "First row name should be Alice");
    check retryClient->delete(path);
}

// =============================================================================
// Retry — failure paths (exhausted retries)
// =============================================================================

@test:Config {
    groups: ["ftp-client-behaviour", "retry"]
}
function testGetBytesWithRetry_NonExistentFile() returns error? {
    ftp:Error|byte[] result = customRetryClient->getBytes(commons:HOME_IN + "/beh-no-such-file-retry.txt");
    test:assertTrue(result is ftp:Error, "Should return error after exhausting retries on non-existent file");
}

@test:Config {
    groups: ["ftp-client-behaviour", "retry"],
    dependsOn: [testGetBytesWithRetry_NonExistentFile]
}
function testGetTextWithRetry_NonExistentFile() returns error? {
    ftp:Error|string result = customRetryClient->getText(commons:HOME_IN + "/beh-no-such-text-retry.txt");
    test:assertTrue(result is ftp:Error, "Should return error after exhausting retries on non-existent file");
}

@test:Config {
    groups: ["ftp-client-behaviour", "retry"],
    dependsOn: [testGetTextWithRetry_NonExistentFile]
}
function testGetJsonWithRetry_NonExistentFile() returns error? {
    ftp:Error|json result = customRetryClient->getJson(commons:HOME_IN + "/beh-no-such.json");
    test:assertTrue(result is ftp:Error, "Should return error after exhausting retries on non-existent file");
}

@test:Config {
    groups: ["ftp-client-behaviour", "retry"]
}
function testClientWithMinimalRetryConfig() returns error? {
    ftp:Client c = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        retryConfig: {}
    });
    byte[] result = check c->getBytes(commons:HOME_IN + "/test1.txt");
    test:assertTrue(result.length() > 0, "Should return non-empty bytes with minimal retry config");
    check c->close();
}

@test:Config {
    groups: ["ftp-client-behaviour", "retry"]
}
function testWriteOperationsWithRetryConfig() returns error? {
    string path = commons:HOME_IN + "/beh-retry-write.txt";
    string content = "test content for retry write";
    check retryClient->putText(path, content);
    string getText = check retryClient->getText(path);
    test:assertEquals(getText, content, "Content should match after write with retry config");
    check retryClient->delete(path);
}

// =============================================================================
// Circuit breaker — invalid configuration
// =============================================================================

@test:Config {
    groups: ["ftp-client-behaviour", "circuit-breaker"]
}
function testCircuitBreaker_InvalidFailureThresholdHigh() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        circuitBreaker: {failureThreshold: 1.5, resetTime: 30}
    });
    test:assertTrue(result is ftp:Error, "Client creation should fail when failureThreshold > 1.0");
    if result is ftp:Error {
        test:assertTrue(result.message().includes("failureThreshold"),
            "Error message should mention failureThreshold");
    }
}

@test:Config {
    groups: ["ftp-client-behaviour", "circuit-breaker"]
}
function testCircuitBreaker_InvalidFailureThresholdLow() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        circuitBreaker: {failureThreshold: -0.1, resetTime: 30}
    });
    test:assertTrue(result is ftp:Error, "Client creation should fail when failureThreshold < 0.0");
    if result is ftp:Error {
        test:assertTrue(result.message().includes("failureThreshold"),
            "Error message should mention failureThreshold");
    }
}

@test:Config {
    groups: ["ftp-client-behaviour", "circuit-breaker"]
}
function testCircuitBreaker_BucketSizeExceedsTimeWindow() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        circuitBreaker: {rollingWindow: {timeWindow: 30, bucketSize: 60}}
    });
    test:assertTrue(result is ftp:Error, "Client creation should fail when bucketSize > timeWindow");
    if result is ftp:Error {
        test:assertTrue(result.message().includes("timeWindow") || result.message().includes("bucketSize"),
            "Error message should mention timeWindow or bucketSize");
    }
}

@test:Config {
    groups: ["ftp-client-behaviour", "circuit-breaker"]
}
function testCircuitBreaker_InvalidRequestVolumeThreshold() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        circuitBreaker: {
            rollingWindow: {requestVolumeThreshold: 0, timeWindow: 60, bucketSize: 10}
        }
    });
    test:assertTrue(result is ftp:Error, "Client creation should fail when requestVolumeThreshold <= 0");
    if result is ftp:Error {
        test:assertTrue(result.message().includes("requestVolumeThreshold"),
            "Error message should mention requestVolumeThreshold");
    }
}

@test:Config {
    groups: ["ftp-client-behaviour", "circuit-breaker"]
}
function testCircuitBreaker_InvalidResetTime() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        circuitBreaker: {
            rollingWindow: {requestVolumeThreshold: 1, timeWindow: 60, bucketSize: 10},
            resetTime: 0
        }
    });
    test:assertTrue(result is ftp:Error, "Client creation should fail when resetTime <= 0");
    if result is ftp:Error {
        test:assertTrue(result.message().includes("resetTime"),
            "Error message should mention resetTime");
    }
}

@test:Config {
    groups: ["ftp-client-behaviour", "circuit-breaker"]
}
function testCircuitBreaker_EmptyFailureCategories() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        circuitBreaker: {
            rollingWindow: {requestVolumeThreshold: 1, timeWindow: 60, bucketSize: 10},
            failureThreshold: 0.5,
            resetTime: 30,
            failureCategories: []
        }
    });
    test:assertTrue(result is ftp:Error, "Client creation should fail when failureCategories is empty");
    if result is ftp:Error {
        test:assertTrue(result.message().includes("failureCategories"),
            "Error message should mention failureCategories");
    }
}

@test:Config {
    groups: ["ftp-client-behaviour", "circuit-breaker"]
}
function testCircuitBreaker_TimeWindowNotDivisibleByBucketSize() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        circuitBreaker: {
            rollingWindow: {requestVolumeThreshold: 1, timeWindow: 60, bucketSize: 11}
        }
    });
    test:assertTrue(result is ftp:Error, "Client creation should fail when timeWindow is not divisible by bucketSize");
    if result is ftp:Error {
        test:assertTrue(result.message().includes("timeWindow") || result.message().includes("bucketSize"),
            "Error message should mention timeWindow or bucketSize");
    }
}

// =============================================================================
// Circuit breaker — normal operation
// =============================================================================

@test:Config {
    groups: ["ftp-client-behaviour", "circuit-breaker"]
}
function testCircuitBreaker_OperationsSucceedWhenClosed() returns error? {
    ftp:Client c = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        circuitBreaker: {
            rollingWindow: {requestVolumeThreshold: 5, timeWindow: 60, bucketSize: 10},
            failureThreshold: 0.5,
            resetTime: 30
        }
    });
    boolean exists = check c->exists(commons:HOME_IN);
    test:assertTrue(exists, "Directory should exist with circuit breaker enabled");
    ftp:FileInfo[] files = check c->list(commons:HOME_IN);
    test:assertTrue(files.length() >= 0, "List should return files array with circuit breaker enabled");
    check c->close();
}

@test:Config {
    groups: ["ftp-client-behaviour", "circuit-breaker"]
}
function testCircuitBreaker_CreationFailsForNonExistentServer() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: 21299,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        circuitBreaker: {
            rollingWindow: {requestVolumeThreshold: 1, timeWindow: 60, bucketSize: 10},
            failureThreshold: 0.5,
            resetTime: 5,
            failureCategories: [ftp:CONNECTION_ERROR]
        }
    });
    test:assertTrue(result is ftp:Error, "Client creation should fail when server does not exist");
}

// =============================================================================
// Circuit breaker — state transitions
// =============================================================================

@test:Config {
    groups: ["ftp-client-behaviour", "circuit-breaker"]
}
function testCircuitBreaker_OpensAfterFailureThreshold() returns error? {
    ftp:Client c = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        circuitBreaker: {
            rollingWindow: {requestVolumeThreshold: 2, timeWindow: 60, bucketSize: 10},
            failureThreshold: 0.5,
            resetTime: 30,
            failureCategories: [ftp:ALL_ERRORS]
        }
    });

    string absent = "/non/existent/beh-cb-trip.txt";

    ftp:Error|byte[] r1 = c->getBytes(absent);
    test:assertTrue(r1 is ftp:Error, "First request should fail with file-not-found error");
    test:assertFalse(r1 is ftp:CircuitBreakerOpenError, "First failure should not be a circuit-breaker error");

    ftp:Error|byte[] r2 = c->getBytes(absent);
    test:assertTrue(r2 is ftp:Error, "Second request should fail with file-not-found error");
    test:assertFalse(r2 is ftp:CircuitBreakerOpenError, "Second failure should not be a circuit-breaker error");

    // Circuit is now OPEN — next request must be rejected immediately.
    ftp:Error|byte[] r3 = c->getBytes(absent);
    test:assertTrue(r3 is ftp:CircuitBreakerOpenError,
        "Third request should be rejected by the open circuit breaker");
    if r3 is ftp:CircuitBreakerOpenError {
        test:assertTrue(r3.message().includes("Circuit breaker is OPEN"),
            "Error message should indicate circuit breaker is open");
    }

    check c->close();
}

@test:Config {
    groups: ["ftp-client-behaviour", "circuit-breaker"]
}
function testCircuitBreaker_HalfOpenTrialFailReopens() returns error? {
    ftp:Client c = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        circuitBreaker: {
            rollingWindow: {requestVolumeThreshold: 1, timeWindow: 60, bucketSize: 10},
            failureThreshold: 0.5,
            resetTime: 1,
            failureCategories: [ftp:ALL_ERRORS]
        }
    });

    string absent = "/non/existent/beh-half-open-fail.txt";

    // Trip the circuit.
    ftp:Error|byte[] r1 = c->getBytes(absent);
    test:assertTrue(r1 is ftp:Error, "Initial request should fail");

    ftp:Error|byte[] r2 = c->getBytes(absent);
    test:assertTrue(r2 is ftp:CircuitBreakerOpenError, "Circuit should be OPEN after failure threshold");

    // Wait for reset time so circuit enters HALF_OPEN.
    runtime:sleep(2);

    // Trial request is allowed but fails — circuit should reopen.
    ftp:Error|byte[] trial = c->getBytes(absent);
    test:assertFalse(trial is ftp:CircuitBreakerOpenError, "HALF_OPEN should allow a single trial request");

    ftp:Error|byte[] r3 = c->getBytes(absent);
    test:assertTrue(r3 is ftp:CircuitBreakerOpenError, "Circuit should reopen after failed HALF_OPEN trial");

    check c->close();
}

@test:Config {
    groups: ["ftp-client-behaviour", "circuit-breaker"]
}
function testCircuitBreaker_HalfOpenTrialSuccessCloses() returns error? {
    ftp:Client c = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        circuitBreaker: {
            rollingWindow: {requestVolumeThreshold: 1, timeWindow: 60, bucketSize: 10},
            failureThreshold: 0.5,
            resetTime: 1,
            failureCategories: [ftp:ALL_ERRORS]
        }
    });

    string absent = "/non/existent/beh-half-open-success.txt";

    // Trip the circuit.
    ftp:Error|byte[] r1 = c->getBytes(absent);
    test:assertTrue(r1 is ftp:Error, "Initial request should fail");

    ftp:Error|byte[] r2 = c->getBytes(absent);
    test:assertTrue(r2 is ftp:CircuitBreakerOpenError, "Circuit should be OPEN after failure threshold");

    // Wait for reset time so circuit enters HALF_OPEN.
    runtime:sleep(2);

    // Successful trial request should close the circuit.
    ftp:Error|boolean trial = c->exists(commons:HOME_IN);
    test:assertFalse(trial is ftp:CircuitBreakerOpenError, "HALF_OPEN should allow a trial request");
    test:assertTrue(trial is boolean, "Trial request on a live path should succeed");

    // Circuit is now CLOSED — subsequent requests should succeed.
    ftp:Error|boolean r3 = c->exists(commons:HOME_IN);
    test:assertFalse(r3 is ftp:CircuitBreakerOpenError, "Circuit should be CLOSED after successful trial");
    test:assertTrue(r3 is boolean, "Request should succeed when circuit is closed");

    check c->close();
}

@test:Config {
    groups: ["ftp-client-behaviour", "circuit-breaker"]
}
function testCircuitBreaker_RetryInteraction() returns error? {
    // Each retry attempt is counted as an individual request by the circuit breaker.
    // count=3 → 1 initial + 3 retries = 4 attempts per call.
    // requestVolumeThreshold=8 → circuit trips after 2 full retry cycles (8 attempts).
    ftp:Client c = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        retryConfig: {count: 3, interval: 0.5, backOffFactor: 1.0, maxWaitInterval: 2.0},
        circuitBreaker: {
            rollingWindow: {requestVolumeThreshold: 8, timeWindow: 60, bucketSize: 10},
            failureThreshold: 0.5,
            resetTime: 30,
            failureCategories: [ftp:ALL_ERRORS]
        }
    });

    string absent = "/non/existent/beh-retry-cb.txt";

    // First call: 4 attempts, all fail → AllRetryAttemptsFailedError.
    ftp:Error|byte[] r1 = c->getBytes(absent);
    test:assertTrue(r1 is ftp:AllRetryAttemptsFailedError,
        "First call should fail after all retries");

    // Second call: 4 more attempts → circuit now has 8 failures total and trips.
    ftp:Error|byte[] r2 = c->getBytes(absent);
    test:assertTrue(r2 is ftp:AllRetryAttemptsFailedError,
        "Second call should fail after all retries");

    // Third call: circuit is OPEN — rejected immediately without retrying.
    ftp:Error|byte[] r3 = c->getBytes(absent);
    test:assertTrue(r3 is ftp:CircuitBreakerOpenError,
        "Third call should be rejected by the open circuit breaker");

    check c->close();
}

// =============================================================================
// Error types
// =============================================================================

@test:Config {
    groups: ["ftp-client-behaviour", "error-types"]
}
function testErrorType_FileNotFoundOnGet() returns error? {
    ftp:Error|stream<byte[] & readonly, error?> result =
        ftpClient->get(commons:HOME_IN + "/beh-nonexistent-error-test.txt");
    test:assertTrue(result is ftp:FileNotFoundError,
        "get() on a non-existent file should return FileNotFoundError");
}

@test:Config {
    groups: ["ftp-client-behaviour", "error-types"]
}
function testErrorType_FileNotFoundOnDelete() returns error? {
    ftp:Error? result = ftpClient->delete(commons:HOME_IN + "/beh-nonexistent-delete-test.txt");
    test:assertTrue(result is ftp:FileNotFoundError,
        "delete() on a non-existent file should return FileNotFoundError");
}

@test:Config {
    groups: ["ftp-client-behaviour", "error-types"]
}
function testErrorType_FileAlreadyExistsOnMkdir() returns error? {
    string dir = commons:HOME_IN + "/beh-existing-dir";
    ftp:Error? first = ftpClient->mkdir(dir);
    // If the directory already exists from a previous run, ignore that error.
    ftp:Error? result = ftpClient->mkdir(dir);
    test:assertTrue(result is ftp:FileAlreadyExistsError,
        "mkdir() on an existing directory should return FileAlreadyExistsError");
    // Cleanup (best effort).
    check ftpClient->rmdir(dir);
}

@test:Config {
    groups: ["ftp-client-behaviour", "error-types"]
}
function testErrorType_FileAlreadyExistsOnRename() returns error? {
    string src = commons:HOME_IN + "/beh-rename-src.txt";
    string dst = commons:HOME_IN + "/beh-rename-dst.txt";
    // Ensure both files exist.
    check ftpClient->putText(src, "source");
    check ftpClient->putText(dst, "destination");
    ftp:Error? result = ftpClient->rename(src, dst);
    test:assertTrue(result is ftp:FileAlreadyExistsError,
        "rename() to an existing path should return FileAlreadyExistsError");
    // Cleanup (best effort).
    check ftpClient->delete(src);
    check ftpClient->delete(dst);
}

@test:Config {
    groups: ["ftp-client-behaviour", "error-types"]
}
function testErrorType_ConnectionErrorOnInvalidServer() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: 59999,
        auth: {credentials: {username: "test", password: "test"}}
    });
    test:assertTrue(result is ftp:ConnectionError,
        "Connecting to a non-existent server should return ConnectionError");
    if result is ftp:ConnectionError {
        test:assertTrue(result.message().includes("Error while connecting"),
            "ConnectionError message should indicate connection failure");
    }
}

@test:Config {
    groups: ["ftp-client-behaviour", "error-types"]
}
function testErrorType_InvalidConfigErrorOnBadRegex() {
    ftp:Listener|ftp:Error result = new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        path: commons:HOME_IN,
        pollingInterval: 2,
        fileNamePattern: "[unclosed"
    });
    test:assertTrue(result is ftp:InvalidConfigError,
        "Creating a Listener with an invalid regex pattern should return InvalidConfigError");
    if result is ftp:InvalidConfigError {
        test:assertTrue(result.message().includes("Invalid regex pattern"),
            "InvalidConfigError message should indicate invalid regex");
    }
}

// ─── Circuit breaker: selective failure-category filtering ────────────────────
// These tests exercise the FailureCategorizer code path inside
// CircuitBreaker.shouldCountAsFailure(). All previous CB tests use
// failureCategories: [ftp:ALL_ERRORS] which bypasses categorisation entirely.

@test:Config {
    groups: ["ftp-client-behaviour", "circuit-breaker"]
}
function testCircuitBreaker_SelectiveCategory_NonMatchingErrorDoesNotTrip() returns error? {
    // Only CONNECTION_ERROR failures should count. FileNotFoundError is not a
    // connection error, so the circuit must stay CLOSED even after the request-
    // volume threshold is exceeded with nothing but FileNotFoundErrors.
    ftp:Client c = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        circuitBreaker: {
            rollingWindow: {requestVolumeThreshold: 2, timeWindow: 60, bucketSize: 10},
            failureThreshold: 0.5,
            resetTime: 30,
            failureCategories: [ftp:CONNECTION_ERROR]
        }
    });

    string absent = "/non/existent/beh-cb-selective.txt";

    // Failure 1 — FileNotFoundError is not CONNECTION_ERROR → must NOT count.
    ftp:Error|byte[] r1 = c->getBytes(absent);
    test:assertTrue(r1 is ftp:Error, "Request should fail with a file error");
    test:assertFalse(r1 is ftp:CircuitBreakerOpenError,
        "FileNotFoundError should not trip a CONNECTION_ERROR-only circuit breaker");

    // Failure 2 — still not CONNECTION_ERROR → still must NOT count.
    ftp:Error|byte[] r2 = c->getBytes(absent);
    test:assertTrue(r2 is ftp:Error, "Second request should fail with a file error");
    test:assertFalse(r2 is ftp:CircuitBreakerOpenError,
        "Circuit should remain CLOSED: non-matching errors do not count as failures");

    // Third request: threshold exceeded only by non-matching errors → still CLOSED.
    ftp:Error|byte[] r3 = c->getBytes(absent);
    test:assertFalse(r3 is ftp:CircuitBreakerOpenError,
        "Circuit must stay CLOSED when all failures are of a non-configured category");

    check c->close();
}

@test:Config {
    groups: ["ftp-client-behaviour", "circuit-breaker"],
    dependsOn: [testCircuitBreaker_SelectiveCategory_NonMatchingErrorDoesNotTrip]
}
function testCircuitBreaker_SelectiveCategory_AuthCategory_DoesNotTripOnFileErrors() returns error? {
    // Configure CB to only count AUTHENTICATION_ERROR failures.
    // File-not-found errors are not auth errors — circuit must stay CLOSED.
    // This exercises the same FailureCategorizer path as the CONNECTION_ERROR test
    // but with a different category configured, confirming the
    // configuredCategories.contains(category) check works for each value.
    ftp:Client c = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        circuitBreaker: {
            rollingWindow: {requestVolumeThreshold: 2, timeWindow: 60, bucketSize: 10},
            failureThreshold: 0.5,
            resetTime: 30,
            failureCategories: [ftp:AUTHENTICATION_ERROR]
        }
    });

    string absent = "/non/existent/beh-cb-auth-cat.txt";

    ftp:Error|byte[] r1 = c->getBytes(absent);
    test:assertTrue(r1 is ftp:Error, "Request should fail with a file error");
    test:assertFalse(r1 is ftp:CircuitBreakerOpenError,
        "FileNotFoundError should not trip an AUTHENTICATION_ERROR-only circuit breaker");

    ftp:Error|byte[] r2 = c->getBytes(absent);
    test:assertFalse(r2 is ftp:CircuitBreakerOpenError,
        "Circuit should remain CLOSED: file errors are not auth errors");

    check c->close();
}
