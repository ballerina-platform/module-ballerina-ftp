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

// =============================================================================
// Success cases — authentication variants
// =============================================================================

// Standard SFTP connection: credentials + encrypted private key + explicit
// preferred auth method ordering. Uses list("/") as a connectivity probe.
@test:Config {
    groups: ["sftp-connection", "auth"]
}
function testSftpConnectionWithKeyAndPassword() returns error? {
    ftp:Client sftpClient = check new ({
        protocol: ftp:SFTP,
        host: commons:FTP_HOST,
        port: commons:SFTP_PORT,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD},
            privateKey: {path: commons:SFTP_PRIVATE_KEY_PATH, password: "changeit"},
            preferredMethods: [ftp:GSSAPI_WITH_MIC, ftp:PUBLICKEY, ftp:KEYBOARD_INTERACTIVE, ftp:PASSWORD]
        }
    });
    ftp:FileInfo[]|ftp:Error result = sftpClient->list("/");
    test:assertFalse(result is ftp:Error,
            "Expected successful SFTP connection with key + password");
    check sftpClient->close();
}

// Passwordless private key: the key file has no passphrase, so no password
// field is required. Credentials (username) are still needed for SSH handshake.
@test:Config {
    groups: ["sftp-connection", "auth"]
}
function testSftpConnectionWithPasswordlessKey() returns error? {
    ftp:Client sftpClient = check new ({
        protocol: ftp:SFTP,
        host: commons:FTP_HOST,
        port: commons:SFTP_PORT,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD},
            privateKey: {path: commons:SFTP_PASSWORDLESS_KEY_PATH}
        }
    });
    ftp:FileInfo[]|ftp:Error result = sftpClient->list("/");
    test:assertFalse(result is ftp:Error,
            "Expected successful SFTP connection with passwordless private key");
    check sftpClient->close();
}

// Minimal viable config: credentials only, no private key. The server falls
// back to password authentication when no key is offered.
@test:Config {
    groups: ["sftp-connection", "auth"]
}
function testSftpConnectionWithPasswordOnly() returns error? {
    ftp:Client sftpClient = check new ({
        protocol: ftp:SFTP,
        host: commons:FTP_HOST,
        port: commons:SFTP_PORT,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}
        }
    });
    ftp:FileInfo[]|ftp:Error result = sftpClient->list("/");
    test:assertFalse(result is ftp:Error,
            "Expected successful SFTP connection with password-only auth");
    check sftpClient->close();
}

// preferredMethods controls the SSH auth negotiation order. Putting PUBLICKEY
// first means the server will try key auth before password, which succeeds
// when both the key and credentials are valid.
@test:Config {
    groups: ["sftp-connection", "auth"]
}
function testSftpConnectionWithPreferredMethodsPublicKeyFirst() returns error? {
    ftp:Client sftpClient = check new ({
        protocol: ftp:SFTP,
        host: commons:FTP_HOST,
        port: commons:SFTP_PORT,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD},
            privateKey: {path: commons:SFTP_PRIVATE_KEY_PATH, password: "changeit"},
            preferredMethods: [ftp:PUBLICKEY, ftp:PASSWORD]
        }
    });
    ftp:FileInfo[]|ftp:Error result = sftpClient->list("/");
    test:assertFalse(result is ftp:Error,
            "Expected successful SFTP connection with PUBLICKEY preferred");
    check sftpClient->close();
}

// =============================================================================
// Connection lifecycle
// =============================================================================

// close() on an active SFTP connection must complete without error.
@test:Config {
    groups: ["sftp-connection", "lifecycle"]
}
function testSftpConnectionCloseSuccess() returns error? {
    ftp:Client sftpClient = check new ({
        protocol: ftp:SFTP,
        host: commons:FTP_HOST,
        port: commons:SFTP_PORT,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD},
            privateKey: {path: commons:SFTP_PRIVATE_KEY_PATH, password: "changeit"}
        }
    });
    ftp:Error? closeResult = sftpClient->close();
    test:assertFalse(closeResult is ftp:Error,
            "Expected clean close of SFTP connection");
}

// Any operation after close() must return the standard "already closed" error.
@test:Config {
    groups: ["sftp-connection", "lifecycle"]
}
function testSftpOperationAfterClose() returns error? {
    ftp:Client sftpClient = check new ({
        protocol: ftp:SFTP,
        host: commons:FTP_HOST,
        port: commons:SFTP_PORT,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD},
            privateKey: {path: commons:SFTP_PRIVATE_KEY_PATH, password: "changeit"}
        }
    });
    check sftpClient->close();
    ftp:FileInfo[]|ftp:Error result = sftpClient->list("/");
    test:assertTrue(result is ftp:Error,
            "Expected error when listing after SFTP connection is closed");
    if result is ftp:Error {
        test:assertEquals(result.message(), commons:CLIENT_ALREADY_CLOSED_MSG,
                "Unexpected error message: " + result.message());
    }
}

// =============================================================================
// Negative: wrong protocol / missing auth
// =============================================================================

// Using FTP protocol when a privateKey is supplied must be rejected — the
// privateKey field is only meaningful for SFTP.
@test:Config {
    groups: ["sftp-connection", "negative"]
}
function testSftpConnectWithWrongProtocol() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:SFTP_PORT,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD},
            privateKey: {path: commons:SFTP_PRIVATE_KEY_PATH, password: "changeit"}
        }
    });
    test:assertTrue(result is ftp:Error,
            "Expected error when using FTP protocol with a privateKey config");
    if result is ftp:Error {
        test:assertTrue(result.message().includes("privateKey can only be used with SFTP protocol"),
                "Unexpected error message: " + result.message());
    }
}

// Completely empty auth block — the server requires at least credentials.
@test:Config {
    groups: ["sftp-connection", "negative"]
}
function testSftpConnectWithNoAuth() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:SFTP,
        host: commons:FTP_HOST,
        port: commons:SFTP_PORT
    });
    test:assertTrue(result is ftp:Error,
            "Expected error for SFTP connection with no auth configuration");
    if result is ftp:Error {
        test:assertTrue(result.message().startsWith("Error while connecting to the FTP server with URL: "),
                "Unexpected error message: " + result.message());
    }
}

// Private key only, no credentials — SSH requires a username at minimum.
@test:Config {
    groups: ["sftp-connection", "negative"]
}
function testSftpConnectWithKeyOnly() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:SFTP,
        host: commons:FTP_HOST,
        port: commons:SFTP_PORT,
        auth: {
            privateKey: {path: commons:SFTP_PRIVATE_KEY_PATH, password: "changeit"}
        }
    });
    test:assertTrue(result is ftp:Error,
            "Expected error for SFTP connection with key but no credentials");
    if result is ftp:Error {
        test:assertTrue(result.message().startsWith("Error while connecting to the FTP server with URL: "),
                "Unexpected error message: " + result.message());
    }
}

// =============================================================================
// Negative: wrong credentials
// =============================================================================

// Wrong password with a valid key — server rejects both auth methods.
@test:Config {
    groups: ["sftp-connection", "negative"]
}
function testSftpConnectWithWrongPassword() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:SFTP,
        host: commons:FTP_HOST,
        port: commons:SFTP_PORT,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: "wrongpassword"},
            privateKey: {path: commons:SFTP_PRIVATE_KEY_PATH, password: "changeit"}
        }
    });
    test:assertTrue(result is ftp:Error,
            "Expected error for wrong SFTP password");
    if result is ftp:Error {
        test:assertTrue(result.message().startsWith("Error while connecting to the FTP server with URL: "),
                "Unexpected error message: " + result.message());
    }
}

// Wrong username — the user does not exist on the server.
@test:Config {
    groups: ["sftp-connection", "negative"]
}
function testSftpConnectWithWrongUsername() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:SFTP,
        host: commons:FTP_HOST,
        port: commons:SFTP_PORT,
        auth: {
            credentials: {username: "nosuchuser", password: commons:FTP_PASSWORD},
            privateKey: {path: commons:SFTP_PRIVATE_KEY_PATH, password: "changeit"}
        }
    });
    test:assertTrue(result is ftp:Error,
            "Expected error for unknown SFTP username");
    if result is ftp:Error {
        test:assertTrue(result.message().startsWith("Error while connecting to the FTP server with URL: "),
                "Unexpected error message: " + result.message());
    }
}

// Empty username — SSH protocol requires a non-empty username.
@test:Config {
    groups: ["sftp-connection", "negative"]
}
function testSftpConnectWithEmptyUsername() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:SFTP,
        host: commons:FTP_HOST,
        port: commons:SFTP_PORT,
        auth: {
            credentials: {username: "", password: commons:FTP_PASSWORD},
            privateKey: {path: commons:SFTP_PRIVATE_KEY_PATH, password: "changeit"}
        }
    });
    test:assertTrue(result is ftp:Error,
            "Expected error for empty SFTP username");
    if result is ftp:Error {
        test:assertTrue(result.message().startsWith("Error while connecting to the FTP server with URL: "),
                "Unexpected error message: " + result.message());
    }
}

// Empty password with an encrypted key — both auth paths fail.
@test:Config {
    groups: ["sftp-connection", "negative"]
}
function testSftpConnectWithEmptyPassword() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:SFTP,
        host: commons:FTP_HOST,
        port: commons:SFTP_PORT,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: ""},
            privateKey: {path: commons:SFTP_PRIVATE_KEY_PATH, password: "changeit"}
        }
    });
    test:assertTrue(result is ftp:Error,
            "Expected error for empty SFTP password");
    if result is ftp:Error {
        test:assertTrue(result.message().startsWith("Error while connecting to the FTP server with URL: "),
                "Unexpected error message: " + result.message());
    }
}

// =============================================================================
// Negative: wrong or invalid private key
// =============================================================================

// A key that exists but is not registered in the server's authorized_keys.
@test:Config {
    groups: ["sftp-connection", "negative"]
}
function testSftpConnectWithUnregisteredKey() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:SFTP,
        host: commons:FTP_HOST,
        port: commons:SFTP_PORT,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD},
            privateKey: {path: commons:SFTP_WRONG_KEY_PATH, password: "changeit"}
        }
    });
    test:assertTrue(result is ftp:Error,
            "Expected error for an unregistered SFTP private key");
    if result is ftp:Error {
        test:assertTrue(result.message().startsWith("Error while connecting to the FTP server with URL: "),
                "Unexpected error message: " + result.message());
    }
}

// A key path that does not exist on disk.
@test:Config {
    groups: ["sftp-connection", "negative"]
}
function testSftpConnectWithInvalidKeyPath() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:SFTP,
        host: commons:FTP_HOST,
        port: commons:SFTP_PORT,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD},
            privateKey: {path: "tests/resources/nonexistent.private.key", password: "changeit"}
        }
    });
    test:assertTrue(result is ftp:Error,
            "Expected error for non-existent SFTP key path");
    if result is ftp:Error {
        test:assertTrue(result.message().startsWith("Error while connecting to the FTP server with URL: "),
                "Unexpected error message: " + result.message());
    }
}

// =============================================================================
// Negative: unreachable server
// =============================================================================

// A port with no server listening must yield a connection error with details.
@test:Config {
    groups: ["sftp-connection", "negative"]
}
function testSftpConnectWithWrongPort() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:SFTP,
        host: commons:FTP_HOST,
        port: 59997,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD},
            privateKey: {path: commons:SFTP_PRIVATE_KEY_PATH, password: "changeit"}
        }
    });
    test:assertTrue(result is ftp:Error,
            "Expected error for non-listening SFTP port");
    if result is ftp:Error {
        test:assertTrue(result.message().startsWith("Error while connecting to the FTP server with URL: "),
                "Unexpected error message: " + result.message());
        // Error must carry root-cause detail beyond the prefix.
        test:assertTrue(
                result.message().length() > "Error while connecting to the FTP server with URL: sftp://wso2:***@127.0.0.1:59997".length(),
                "Expected error to include root-cause detail");
    }
}

// A non-resolvable hostname must yield a connection error with details.
@test:Config {
    groups: ["sftp-connection", "negative"]
}
function testSftpConnectWithInvalidHost() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:SFTP,
        host: "nonexistent.invalid.sftp.example",
        port: commons:SFTP_PORT,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD},
            privateKey: {path: commons:SFTP_PRIVATE_KEY_PATH, password: "changeit"}
        }
    });
    test:assertTrue(result is ftp:Error,
            "Expected error for non-resolvable SFTP hostname");
    if result is ftp:Error {
        test:assertTrue(result.message().startsWith("Error while connecting to the FTP server with URL: "),
                "Unexpected error message: " + result.message());
        test:assertTrue(
                result.message().length() > "Error while connecting to the FTP server with URL: ".length(),
                "Expected error to include root-cause detail");
    }
}

// =============================================================================
// SFTP Listener — basic polling (exercises configureServerPrivateKey)
// =============================================================================

// An ftp:Listener with protocol=SFTP and a private-key auth config must poll
// the SFTP server and deliver file-change events.
// This is the first listener-side test for SFTP and exercises
// FtpListenerHelper.configureServerPrivateKey() which sets the IDENTITY and
// IDENTITY_PASS_PHRASE VFS parameters used by the Apache Commons VFS SFTP connector.

isolated boolean sftpListenerFileReceived = false;

@test:Config {
    groups: ["sftp-connection", "sftp-listener"]
}
function testSftpListener_DetectsNewFile() returns error? {
    lock {
        sftpListenerFileReceived = false;
    }

    ftp:Service svc = service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {
            foreach ftp:FileInfo fi in event.addedFiles {
                if fi.name.endsWith(".sftp-lst") {
                    lock {
                        sftpListenerFileReceived = true;
                    }
                }
            }
        }
    };

    ftp:Listener sftpListener = check new ({
        protocol: ftp:SFTP,
        host: commons:FTP_HOST,
        port: commons:SFTP_PORT,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD},
            privateKey: {path: commons:SFTP_PRIVATE_KEY_PATH, password: "changeit"}
        },
        path: commons:SFTP_LISTENER_ROOT,
        pollingInterval: 2
    });
    check sftpListener.attach(svc);
    check sftpListener.'start();
    runtime:registerListener(sftpListener);

    // Upload a file using an SFTP client so the listener can detect it.
    ftp:Client sftpClient = check new ({
        protocol: ftp:SFTP,
        host: commons:FTP_HOST,
        port: commons:SFTP_PORT,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD},
            privateKey: {path: commons:SFTP_PRIVATE_KEY_PATH, password: "changeit"}
        }
    });

    string remoteFile = commons:SFTP_LISTENER_ROOT + "/trigger.sftp-lst";
    check sftpClient->putText(remoteFile, "sftp-listener-test");

    // Wait up to 15 s for the listener to fire.
    boolean received = false;
    int remaining = 15;
    while remaining > 0 {
        lock {
            received = sftpListenerFileReceived;
        }
        if received {
            break;
        }
        runtime:sleep(1);
        remaining -= 1;
    }

    // Assert before cleanup so a slow teardown does not affect the verdict.
    test:assertTrue(received,
            "SFTP listener (with privateKey auth) should detect a newly uploaded file");

    // Deregister the listener; skip gracefulStop() as it blocks indefinitely
    // on SFTP/SSH teardown in the test environment.
    runtime:deregisterListener(sftpListener);

    do {
        check sftpClient->delete(remoteFile);
    } on fail {
    }
    do {
        check sftpClient->close();
    } on fail {
    }
}
