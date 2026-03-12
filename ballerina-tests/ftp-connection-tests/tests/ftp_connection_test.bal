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
import ballerina/test;
import ballerina_tests/ftp_test_commons as commons;

// =============================================================================
// Anonymous FTP — success
// =============================================================================

// Anonymous FTP server allows connections without explicit credentials.
// Validates that list() succeeds as a basic connectivity probe.
@test:Config {
    groups: ["ftp-connection", "anon"]
}
function testAnonFtpConnectionSuccess() returns error? {
    ftp:Client ftpClient = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:ANON_FTP_PORT
    });
    ftp:FileInfo[]|ftp:Error listResult = ftpClient->list(commons:HOME_IN);
    test:assertFalse(listResult is ftp:Error,
        "Expected successful directory listing on anonymous FTP server");
    check ftpClient->close();
}

// Anonymous FTP with explicit anonymous credentials (username=anonymous) should
// behave identically to an unauthenticated connection.
@test:Config {
    groups: ["ftp-connection", "anon"]
}
function testAnonFtpConnectionWithExplicitCredentials() returns error? {
    ftp:Client ftpClient = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:ANON_FTP_PORT,
        auth: {
            credentials: {
                username: commons:ANON_USERNAME,
                password: commons:ANON_PASSWORD
            }
        }
    });
    ftp:FileInfo[]|ftp:Error listResult = ftpClient->list(commons:HOME_IN);
    test:assertFalse(listResult is ftp:Error,
        "Expected successful directory listing with explicit anonymous credentials");
    check ftpClient->close();
}

// =============================================================================
// Authenticated FTP — success
// =============================================================================

// Standard username/password authentication against a protected FTP server.
// Validates the connection with a list() probe.
@test:Config {
    groups: ["ftp-connection", "auth"]
}
function testAuthFtpConnectionSuccess() returns error? {
    ftp:Client ftpClient = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {
            credentials: {
                username: commons:FTP_USERNAME,
                password: commons:FTP_PASSWORD
            }
        }
    });
    ftp:FileInfo[]|ftp:Error listResult = ftpClient->list(commons:HOME_IN);
    test:assertFalse(listResult is ftp:Error,
        "Expected successful directory listing on authenticated FTP server");
    check ftpClient->close();
}

// userDirIsRoot=false is the default. Paths are absolute from the server root.
@test:Config {
    groups: ["ftp-connection", "auth"]
}
function testAuthFtpConnectionWithUserDirIsRootFalse() returns error? {
    ftp:Client ftpClient = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {
            credentials: {
                username: commons:FTP_USERNAME,
                password: commons:FTP_PASSWORD
            }
        },
        userDirIsRoot: false
    });
    ftp:FileInfo[]|ftp:Error listResult = ftpClient->list(commons:HOME_IN);
    test:assertFalse(listResult is ftp:Error,
        "Expected successful listing with userDirIsRoot=false");
    check ftpClient->close();
}

// =============================================================================
// Connection errors
// =============================================================================

// Connecting to a port with no server must yield a ConnectionError.
@test:Config {
    groups: ["ftp-connection", "negative"]
}
function testFtpConnectionToNonExistentPort() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: 59999,
        auth: {credentials: {username: "test", password: "test"}}
    });
    test:assertTrue(result is ftp:ConnectionError,
        "Expected ConnectionError when no server is listening on the port");
    if result is ftp:ConnectionError {
        test:assertTrue(result.message().startsWith("Error while connecting to the FTP server"),
            "Unexpected error message: " + result.message());
    }
}

// A syntactically invalid hostname must fail before even attempting a connection.
@test:Config {
    groups: ["ftp-connection", "negative"]
}
function testFtpConnectionWithInvalidHostname() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:FTP,
        host: "!@#invalid-host!@#",
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}}
    });
    test:assertTrue(result is ftp:Error,
        "Expected Error for malformed hostname");
    if result is ftp:Error {
        test:assertTrue(result.message().startsWith("Error occurred while constructing a URI from host"),
            "Unexpected error message: " + result.message());
    }
}

// Wrong password on an auth-required server must fail.
@test:Config {
    groups: ["ftp-connection", "negative"]
}
function testFtpConnectionWithWrongPassword() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: "wrongpassword"}}
    });
    test:assertTrue(result is ftp:Error,
        "Expected error when connecting with wrong password");
}

// Wrong username on an auth-required server must fail.
@test:Config {
    groups: ["ftp-connection", "negative"]
}
function testFtpConnectionWithWrongUsername() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: "wronguser", password: commons:FTP_PASSWORD}}
    });
    test:assertTrue(result is ftp:Error,
        "Expected error when connecting with wrong username");
}

// =============================================================================
// Connection lifecycle
// =============================================================================

// close() on an active connection must complete without error.
@test:Config {
    groups: ["ftp-connection", "lifecycle"]
}
function testFtpConnectionCloseSuccess() returns error? {
    ftp:Client ftpClient = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}}
    });
    ftp:Error? closeResult = ftpClient->close();
    test:assertFalse(closeResult is ftp:Error,
        "Expected clean close of FTP connection");
}

// Any operation after close() must return an error carrying the "already closed" message.
@test:Config {
    groups: ["ftp-connection", "lifecycle"]
}
function testFtpOperationAfterClose() returns error? {
    ftp:Client ftpClient = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}}
    });
    check ftpClient->close();

    ftp:FileInfo[]|ftp:Error listResult = ftpClient->list(commons:HOME_IN);
    test:assertTrue(listResult is ftp:Error,
        "Expected error when listing after connection is closed");
    if listResult is ftp:Error {
        test:assertEquals(listResult.message(), commons:CLIENT_ALREADY_CLOSED_MSG,
            "Unexpected error message after close: " + listResult.message());
    }
}
