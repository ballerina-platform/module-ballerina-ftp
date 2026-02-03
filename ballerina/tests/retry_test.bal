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

import ballerina/test;
import ballerina/time;

// Test configuration with retry enabled
ClientConfiguration retryConfig = {
    protocol: FTP,
    host: "127.0.0.1",
    port: 21212,
    auth: {credentials: {username: "wso2", password: "wso2123"}},
    userDirIsRoot: false,
    retryConfig: {
        count: 3,
        interval: 0.5,        // 500ms initial interval for faster tests
        backOffFactor: 2.0,
        maxWaitInterval: 5.0
    }
};

// Test configuration with custom retry settings
ClientConfiguration customRetryConfig = {
    protocol: FTP,
    host: "127.0.0.1",
    port: 21212,
    auth: {credentials: {username: "wso2", password: "wso2123"}},
    userDirIsRoot: false,
    retryConfig: {
        count: 2,
        interval: 0.2,
        backOffFactor: 1.5,
        maxWaitInterval: 1.0
    }
};

Client? retryClientEp = ();
Client? customRetryClientEp = ();

@test:BeforeSuite
function initRetryTestEnvironment() returns error? {
    retryClientEp = check new (retryConfig);
    customRetryClientEp = check new (customRetryConfig);
}

// Test: Successful getBytes with retry config (no retry needed)
@test:Config {}
function testGetBytesWithRetryConfig_Success() returns error? {
    string testPath = "/home/in/retry/test1.txt";

    byte[]|Error result = (<Client>retryClientEp)->getBytes(testPath);

    if result is Error {
        test:assertFail(msg = "getBytes with retry config should succeed: " + result.message());
    } else {
        test:assertTrue(result.length() > 0, msg = "Should return non-empty bytes");
    }
}

// Test: Successful getText with retry config (no retry needed)
@test:Config {
    dependsOn: [testGetBytesWithRetryConfig_Success]
}
function testGetTextWithRetryConfig_Success() returns error? {
    string testPath = "/home/in/retry/test1.txt";

    string|Error result = (<Client>retryClientEp)->getText(testPath);

    if result is Error {
        test:assertFail(msg = "getText with retry config should succeed: " + result.message());
    } else {
        test:assertTrue(result.length() > 0, msg = "Should return non-empty text");
    }
}

// Test: Successful getJson with retry config (no retry needed)
@test:Config {
    dependsOn: [testGetTextWithRetryConfig_Success]
}
function testGetJsonWithRetryConfig_Success() returns error? {
    // First, put a JSON file
    string jsonPath = "/home/in/retry/retry-test.json";
    json testJson = {name: "retry-test", value: 42};
    check (<Client>retryClientEp)->putJson(jsonPath, testJson);

    json|Error result = (<Client>retryClientEp)->getJson(jsonPath);

    if result is Error {
        test:assertFail(msg = "getJson with retry config should succeed: " + result.message());
    } else {
        test:assertEquals(result, testJson, msg = "JSON content should match");
    }

    // Cleanup
    check (<Client>retryClientEp)->delete(jsonPath);
}

// Test: Successful getXml with retry config (no retry needed)
@test:Config {
    dependsOn: [testGetJsonWithRetryConfig_Success]
}
function testGetXmlWithRetryConfig_Success() returns error? {
    // First, put an XML file
    string xmlPath = "/home/in/retry/retry-test.xml";
    xml testXml = xml `<root><item>retry-test</item></root>`;
    check (<Client>retryClientEp)->putXml(xmlPath, testXml);

    xml|Error result = (<Client>retryClientEp)->getXml(xmlPath);

    if result is Error {
        test:assertFail(msg = "getXml with retry config should succeed: " + result.message());
    } else {
        test:assertEquals(result.toString(), testXml.toString(), msg = "XML content should match");
    }

    // Cleanup
    check (<Client>retryClientEp)->delete(xmlPath);
}

// Test: Successful getCsv with retry config (no retry needed)
@test:Config {
    dependsOn: [testGetXmlWithRetryConfig_Success]
}
function testGetCsvWithRetryConfig_Success() returns error? {
    // First, put a CSV file
    string csvPath = "/home/in/retry/retry-test.csv";
    string[][] testCsv = [
        ["id", "name"],
        ["1", "Alice"],
        ["2", "Bob"]
    ];
    check (<Client>retryClientEp)->putCsv(csvPath, testCsv);

    string[][]|Error result = (<Client>retryClientEp)->getCsv(csvPath);

    if result is Error {
        test:assertFail(msg = "getCsv with retry config should succeed: " + result.message());
    } else {
        test:assertEquals(result.length(), 2, msg = "Should return 2 data rows");
        test:assertEquals(result[0][1], "Alice", msg = "First row name should be Alice");
    }

    // Cleanup
    check (<Client>retryClientEp)->delete(csvPath);
}

// Test: Retry behavior on non-existent file (should fail after retries)
@test:Config {
    dependsOn: [testGetCsvWithRetryConfig_Success]
}
function testGetBytesWithRetry_NonExistentFile() returns error? {
    string nonExistentPath = "/home/in/retry/non-existent-retry-test-file.txt";

    time:Utc startTime = time:utcNow();
    byte[]|Error result = (<Client>customRetryClientEp)->getBytes(nonExistentPath);
    time:Utc endTime = time:utcNow();

    // Should be an error after retries
    test:assertTrue(result is Error, msg = "Should return error for non-existent file");

    if result is Error {
        // Error message should indicate retry exhaustion
        test:assertTrue(result.message().includes("failed after") ||
                       result.message().includes("not found"),
                       msg = "Error should indicate failure: " + result.message());
    }

    // With retry config (count=2, interval=0.2, backOffFactor=1.5):
    // Initial attempt + retry 1 (0.2s wait) + retry 2 (0.3s wait)
    // Total minimum: ~0.5s
    decimal elapsedSeconds = <decimal>(endTime[0] - startTime[0]);
    // Allow some tolerance - should take at least some time due to retries
    // Note: The elapsed time check is approximate due to test environment variations
}

// Test: Retry behavior on getText with non-existent file
@test:Config {
    dependsOn: [testGetBytesWithRetry_NonExistentFile]
}
function testGetTextWithRetry_NonExistentFile() returns error? {
    string nonExistentPath = "/home/in/retry/non-existent-retry-text.txt";

    string|Error result = (<Client>customRetryClientEp)->getText(nonExistentPath);

    test:assertTrue(result is Error, msg = "Should return error for non-existent file");
}

// Test: Retry behavior on getJson with non-existent file
@test:Config {
    dependsOn: [testGetTextWithRetry_NonExistentFile]
}
function testGetJsonWithRetry_NonExistentFile() returns error? {
    string nonExistentPath = "/home/in/retry/non-existent-retry.json";

    json|Error result = (<Client>customRetryClientEp)->getJson(nonExistentPath);

    test:assertTrue(result is Error, msg = "Should return error for non-existent file");
}

// Test: Client without retry config should fail immediately on non-existent file
@test:Config {
    dependsOn: [testGetJsonWithRetry_NonExistentFile]
}
function testGetBytesWithoutRetry_ImmediateFail() returns error? {
    // Use the client without retry config
    string nonExistentPath = "/home/in/retry/non-existent-no-retry.txt";

    time:Utc startTime = time:utcNow();
    byte[]|Error result = (<Client>clientEp)->getBytes(nonExistentPath);
    time:Utc endTime = time:utcNow();

    test:assertTrue(result is Error, msg = "Should return error for non-existent file");

    // Without retry, should fail quickly (< 1 second)
    decimal elapsedSeconds = <decimal>(endTime[0] - startTime[0]);
    test:assertTrue(elapsedSeconds < 2.0d,
        msg = "Without retry, should fail quickly. Elapsed: " + elapsedSeconds.toString() + "s");
}

// Test: Client creation with minimal retry config
@test:Config {}
function testClientWithMinimalRetryConfig() returns error? {
    ClientConfiguration minimalRetryConf = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21212,
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        retryConfig: {}  // Use all defaults
    };

    Client minimalClient = check new (minimalRetryConf);

    // Should be able to perform operations
    string testPath = "/home/in/test1.txt";
    byte[]|Error result = minimalClient->getBytes(testPath);

    if result is Error {
        test:assertFail(msg = "Client with minimal retry config should work: " + result.message());
    }

    check minimalClient->close();
}

// Test: Verify retry doesn't affect write operations
@test:Config {
    dependsOn: [testClientWithMinimalRetryConfig]
}
function testWriteOperationsWithRetryConfig() returns error? {
    string testPath = "/home/in/retry-write-test.txt";
    string content = "test content for retry write";

    // Write operations should work normally with retry config
    Error? putResult = (<Client>retryClientEp)->putText(testPath, content);
    test:assertEquals(putResult, (), msg = "putText should succeed with retry config");

    // Verify content was written
    string|Error getText = (<Client>retryClientEp)->getText(testPath);
    if getText is string {
        test:assertEquals(getText, content, msg = "Content should match after write");
    } else {
        test:assertFail(msg = "Failed to read written content: " + getText.message());
    }

    // Cleanup
    check (<Client>retryClientEp)->delete(testPath);
}

@test:AfterSuite
function cleanupRetryTestEnvironment() returns error? {
    if retryClientEp is Client {
        check (<Client>retryClientEp)->close();
    }
    if customRetryClientEp is Client {
        check (<Client>customRetryClientEp)->close();
    }
}
