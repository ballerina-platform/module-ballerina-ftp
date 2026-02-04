// Copyright (c) 2026 WSO2 LLC. (http://www.wso2.com)
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

import ballerina/test;
import ballerina/lang.runtime;

// Test client creation with invalid failure threshold (> 1.0)
@test:Config {}
public function testClientWithInvalidFailureThresholdHigh() {
    ClientConfiguration config = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21212,
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        circuitBreaker: {
            failureThreshold: 1.5,
            resetTime: 30
        }
    };
    Client|Error cbClient = new (config);
    test:assertTrue(cbClient is Error, msg = "Client creation should fail with invalid failure threshold > 1.0");
    if cbClient is Error {
        test:assertTrue(cbClient.message().includes("failureThreshold"),
            msg = "Error message should mention failureThreshold");
    }
}

// Test client creation with invalid failure threshold (< 0.0)
@test:Config {}
public function testClientWithInvalidFailureThresholdLow() {
    ClientConfiguration config = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21212,
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        circuitBreaker: {
            failureThreshold: -0.1,
            resetTime: 30
        }
    };
    Client|Error cbClient = new (config);
    test:assertTrue(cbClient is Error, msg = "Client creation should fail with invalid failure threshold < 0.0");
    if cbClient is Error {
        test:assertTrue(cbClient.message().includes("failureThreshold"),
            msg = "Error message should mention failureThreshold");
    }
}

// Test client creation with bucketSize greater than timeWindow
@test:Config {}
public function testClientWithInvalidBucketSize() {
    ClientConfiguration config = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21212,
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        circuitBreaker: {
            rollingWindow: {
                timeWindow: 30,
                bucketSize: 60  // bucket larger than window
            }
        }
    };
    Client|Error cbClient = new (config);
    test:assertTrue(cbClient is Error, msg = "Client creation should fail when bucketSize > timeWindow");
    if cbClient is Error {
        test:assertTrue(cbClient.message().includes("timeWindow") || cbClient.message().includes("bucketSize"),
            msg = "Error message should mention timeWindow or bucketSize");
    }
}

// Test client creation with invalid requestVolumeThreshold (<= 0)
@test:Config {}
public function testClientWithInvalidRequestVolumeThreshold() {
    ClientConfiguration config = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21212,
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        circuitBreaker: {
            rollingWindow: {
                requestVolumeThreshold: 0,
                timeWindow: 60,
                bucketSize: 10
            }
        }
    };
    Client|Error cbClient = new (config);
    test:assertTrue(cbClient is Error, msg = "Client creation should fail when requestVolumeThreshold <= 0");
    if cbClient is Error {
        test:assertTrue(cbClient.message().includes("requestVolumeThreshold"),
            msg = "Error message should mention requestVolumeThreshold");
    }
}

// Test client creation with invalid resetTime (<= 0)
@test:Config {}
public function testClientWithInvalidResetTime() {
    ClientConfiguration config = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21212,
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        circuitBreaker: {
            rollingWindow: {
                requestVolumeThreshold: 1,
                timeWindow: 60,
                bucketSize: 10
            },
            resetTime: 0
        }
    };
    Client|Error cbClient = new (config);
    test:assertTrue(cbClient is Error, msg = "Client creation should fail when resetTime <= 0");
    if cbClient is Error {
        test:assertTrue(cbClient.message().includes("resetTime"),
            msg = "Error message should mention resetTime");
    }
}

// Test client creation with timeWindow not divisible by bucketSize
@test:Config {}
public function testClientWithNonDivisibleTimeWindow() {
    ClientConfiguration config = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21212,
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        circuitBreaker: {
            rollingWindow: {
                requestVolumeThreshold: 1,
                timeWindow: 60,
                bucketSize: 11
            }
        }
    };
    Client|Error cbClient = new (config);
    test:assertTrue(cbClient is Error, msg = "Client creation should fail when timeWindow is not divisible by bucketSize");
    if cbClient is Error {
        test:assertTrue(cbClient.message().includes("timeWindow") || cbClient.message().includes("bucketSize"),
            msg = "Error message should mention timeWindow or bucketSize");
    }
}

// Test that operations work normally with circuit breaker enabled
@test:Config {}
public function testOperationsWithCircuitBreaker() returns error? {
    ClientConfiguration config = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21212,
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        circuitBreaker: {
            rollingWindow: {
                requestVolumeThreshold: 5,
                timeWindow: 60,
                bucketSize: 10
            },
            failureThreshold: 0.5,
            resetTime: 30
        }
    };
    Client cbClient = check new (config);

    // Test basic operations work with circuit breaker enabled
    boolean exists = check cbClient->exists("/home/in");
    test:assertTrue(exists, msg = "Directory should exist");

    FileInfo[] files = check cbClient->list("/home/in");
    test:assertTrue(files.length() >= 0, msg = "List operation should return files array");

    check cbClient->close();
}

// Test that client creation fails when server doesn't exist (connection error, not circuit breaker)
@test:Config {}
public function testClientCreationFailsForNonExistentServer() {
    // Create a client pointing to a non-existent server
    ClientConfiguration config = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21299,  // Non-existent port
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        circuitBreaker: {
            rollingWindow: {
                requestVolumeThreshold: 1,
                timeWindow: 60,
                bucketSize: 10
            },
            failureThreshold: 0.5,
            resetTime: 5,
            failureCategories: [CONNECTION_ERROR]
        }
    };

    Client|Error cbClient = new (config);
    test:assertTrue(cbClient is Error, msg = "Client creation should fail for non-existent server");
}

// Test that circuit breaker opens after failures and returns CircuitBreakerOpenError
@test:Config {}
public function testCircuitBreakerOpensAfterFailures() returns error? {
    // Configure circuit breaker with low thresholds to trigger quickly
    // - requestVolumeThreshold: 2 (only need 2 requests before circuit can trip)
    // - failureThreshold: 0.5 (50% failure rate trips the circuit)
    // - ALL_ERRORS: any error counts as failure
    ClientConfiguration config = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21212,
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        circuitBreaker: {
            rollingWindow: {
                requestVolumeThreshold: 2,
                timeWindow: 60,
                bucketSize: 10
            },
            failureThreshold: 0.5,
            resetTime: 30,
            failureCategories: [ALL_ERRORS]
        }
    };

    Client cbClient = check new (config);

    // Make requests that will fail (reading non-existent files)
    // These failures should accumulate and trip the circuit
    string nonExistentPath = "/non/existent/file/path/that/does/not/exist.txt";

    // First failure
    byte[]|Error result1 = cbClient->getBytes(nonExistentPath);
    test:assertTrue(result1 is Error, msg = "First request should fail (file not found)");
    test:assertFalse(result1 is CircuitBreakerOpenError,
        msg = "First failure should be regular error, not circuit breaker error");

    // Second failure - after this, we have 2 requests with 100% failure rate
    byte[]|Error result2 = cbClient->getBytes(nonExistentPath);
    test:assertTrue(result2 is Error, msg = "Second request should fail");
    test:assertFalse(result2 is CircuitBreakerOpenError,
        msg = "Second failure should be regular error, not circuit breaker error");

    // Third request - circuit should now be OPEN and reject immediately
    byte[]|Error result3 = cbClient->getBytes(nonExistentPath);
    test:assertTrue(result3 is CircuitBreakerOpenError,
        msg = "Third request should fail with CircuitBreakerOpenError (circuit is open)");

    if result3 is CircuitBreakerOpenError {
        string message = result3.message();
        test:assertTrue(message.includes("Circuit breaker is OPEN"),
            msg = "Error message should indicate circuit breaker is open");
    }

    check cbClient->close();
}

// Test HALF_OPEN transition: allow a single trial request after reset time, then reopen on failure
@test:Config {}
public function testCircuitBreakerHalfOpenTrial() returns error? {
    ClientConfiguration config = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21212,
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        circuitBreaker: {
            rollingWindow: {
                requestVolumeThreshold: 1,
                timeWindow: 60,
                bucketSize: 10
            },
            failureThreshold: 0.5,
            resetTime: 1,
            failureCategories: [ALL_ERRORS]
        }
    };

    Client cbClient = check new (config);
    string nonExistentPath = "/non/existent/file/path/half-open-test.txt";

    // Trip the circuit quickly
    byte[]|Error result1 = cbClient->getBytes(nonExistentPath);
    test:assertTrue(result1 is Error, msg = "Initial request should fail");

    // Circuit should now be OPEN
    byte[]|Error result2 = cbClient->getBytes(nonExistentPath);
    test:assertTrue(result2 is CircuitBreakerOpenError,
        msg = "Circuit should be OPEN after failure threshold is met");

    // Wait for reset time to elapse
    runtime:sleep(2);

    // First request after reset should be allowed (HALF_OPEN trial)
    byte[]|Error trialResult = cbClient->getBytes(nonExistentPath);
    test:assertFalse(trialResult is CircuitBreakerOpenError,
        msg = "HALF_OPEN should allow a single trial request");

    // Trial fails, circuit should move back to OPEN and block immediately
    byte[]|Error result3 = cbClient->getBytes(nonExistentPath);
    test:assertTrue(result3 is CircuitBreakerOpenError,
        msg = "Circuit should reopen after failed HALF_OPEN trial");

    check cbClient->close();
}

// Test HALF_OPEN transition: successful trial request should close the circuit
@test:Config {}
public function testCircuitBreakerHalfOpenSuccessfulTrial() returns error? {
    ClientConfiguration config = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21212,
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        circuitBreaker: {
            rollingWindow: {
                requestVolumeThreshold: 1,
                timeWindow: 60,
                bucketSize: 10
            },
            failureThreshold: 0.5,
            resetTime: 1,
            failureCategories: [ALL_ERRORS]
        }
    };

    Client cbClient = check new (config);
    string nonExistentPath = "/non/existent/file/path/half-open-success-test.txt";

    // Trip the circuit quickly
    byte[]|Error result1 = cbClient->getBytes(nonExistentPath);
    test:assertTrue(result1 is Error, msg = "Initial request should fail");

    // Circuit should now be OPEN
    byte[]|Error result2 = cbClient->getBytes(nonExistentPath);
    test:assertTrue(result2 is CircuitBreakerOpenError,
        msg = "Circuit should be OPEN after failure threshold is met");

    // Wait for reset time to elapse
    runtime:sleep(2);

    // First request after reset should be allowed (HALF_OPEN trial)
    // This time, make a successful request
    boolean|Error trialResult = cbClient->exists("/home/in");
    test:assertFalse(trialResult is CircuitBreakerOpenError,
        msg = "HALF_OPEN should allow a single trial request");
    test:assertTrue(trialResult is boolean,
        msg = "Trial request should succeed");

    // Circuit should now be CLOSED, so next request should succeed
    boolean|Error result3 = cbClient->exists("/home/in");
    test:assertFalse(result3 is CircuitBreakerOpenError,
        msg = "Circuit should be CLOSED after successful HALF_OPEN trial");
    test:assertTrue(result3 is boolean,
        msg = "Request should succeed when circuit is CLOSED");

    check cbClient->close();
}

// Test interaction between retry and circuit breaker mechanisms
@test:Config {}
public function testRetryAndCircuitBreakerInteraction() returns error? {
    // Configure both retry and circuit breaker
    // Each retry attempt counts as a separate request in circuit breaker
    // With 1 initial + 3 retries = 4 attempts per request
    // Setting threshold to 8 means circuit trips after 2 full request cycles
    ClientConfiguration config = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21212,
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        retryConfig: {
            count: 3,
            interval: 0.5,
            backOffFactor: 1.0,
            maxWaitInterval: 2.0
        },
        circuitBreaker: {
            rollingWindow: {
                requestVolumeThreshold: 8,  // 2 full requests (4 attempts each)
                timeWindow: 60,
                bucketSize: 10
            },
            failureThreshold: 0.5,  // 50% failure rate trips circuit
            resetTime: 30,
            failureCategories: [ALL_ERRORS]
        }
    };

    Client cbClient = check new (config);
    string nonExistentPath = "/non/existent/file/retry-cb-test.txt";

    // First request: Will fail after all retries (4 attempts total)
    // Circuit breaker records 4 failures but threshold (8) not met yet
    byte[]|Error result1 = cbClient->getBytes(nonExistentPath);
    test:assertTrue(result1 is AllRetryAttemptsFailedError,
        msg = "First request should fail after all retries");

    // Second request: Will also fail after all retries (4 more attempts)
    // After 8 total failures, circuit trips at 100% failure rate
    byte[]|Error result2 = cbClient->getBytes(nonExistentPath);
    test:assertTrue(result2 is AllRetryAttemptsFailedError,
        msg = "Second request should fail after all retries");

    // Third request: Circuit should now be OPEN and block immediately
    // This should NOT go through retries, should fail fast
    byte[]|Error result3 = cbClient->getBytes(nonExistentPath);
    test:assertTrue(result3 is CircuitBreakerOpenError,
        msg = "Third request should fail fast with circuit breaker open");

    check cbClient->close();
}
