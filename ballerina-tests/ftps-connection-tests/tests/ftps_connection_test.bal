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

// Shared secureSocket config helpers — inline per test so each test is independent.

// =============================================================================
// EXPLICIT TLS mode — success cases
// =============================================================================

// Standard EXPLICIT FTPS connection with mutual TLS (key + cert) and PRIVATE
// data channel protection. Uses exists() as a lightweight connectivity probe.
@test:Config {
    groups: ["ftps-connection", "explicit"]
}
function testFtpsExplicitConnectionSuccess() returns error? {
    ftp:Client ftpsClient = check new ({
        protocol: ftp:FTPS,
        host: commons:FTP_HOST,
        port: commons:FTPS_EXPLICIT_PORT,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD},
            secureSocket: {
                key: {path: commons:KEYSTORE_PATH, password: "changeit"},
                cert: {path: commons:KEYSTORE_PATH, password: "changeit"},
                mode: ftp:EXPLICIT,
                dataChannelProtection: ftp:PRIVATE
            }
        }
    });
    boolean|ftp:Error result = ftpsClient->exists(commons:FTPS_ROOT);
    test:assertFalse(result is ftp:Error,
        "Expected successful FTPS EXPLICIT connection");
    check ftpsClient->close();
}

// EXPLICIT FTPS with CLEAR data channel protection (control channel is TLS,
// data channel is plain). Some servers allow this for performance.
@test:Config {
    groups: ["ftps-connection", "explicit"]
}
function testFtpsExplicitClearDataChannelConnection() returns error? {
    ftp:Client ftpsClient = check new ({
        protocol: ftp:FTPS,
        host: commons:FTP_HOST,
        port: commons:FTPS_EXPLICIT_PORT,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD},
            secureSocket: {
                key: {path: commons:KEYSTORE_PATH, password: "changeit"},
                cert: {path: commons:KEYSTORE_PATH, password: "changeit"},
                mode: ftp:EXPLICIT,
                dataChannelProtection: ftp:CLEAR
            }
        }
    });
    boolean|ftp:Error result = ftpsClient->exists(commons:FTPS_ROOT);
    test:assertFalse(result is ftp:Error,
        "Expected successful FTPS EXPLICIT connection with CLEAR data channel");
    check ftpsClient->close();
}

// One-way TLS: only the server certificate is provided (no client key).
// The server authenticates itself to the client but not vice versa.
@test:Config {
    groups: ["ftps-connection", "explicit"]
}
function testFtpsOneWaySslConnection() returns error? {
    ftp:Client ftpsClient = check new ({
        protocol: ftp:FTPS,
        host: commons:FTP_HOST,
        port: commons:FTPS_EXPLICIT_PORT,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD},
            secureSocket: {
                cert: {path: commons:KEYSTORE_PATH, password: "changeit"},
                mode: ftp:EXPLICIT
            }
        }
    });
    boolean|ftp:Error result = ftpsClient->exists(commons:FTPS_ROOT);
    test:assertFalse(result is ftp:Error,
        "Expected successful one-way TLS FTPS connection");
    check ftpsClient->close();
}

// =============================================================================
// IMPLICIT TLS mode — success cases
// =============================================================================

// IMPLICIT FTPS wraps the entire control connection in TLS from the first byte.
@test:Config {
    groups: ["ftps-connection", "implicit"]
}
function testFtpsImplicitConnectionSuccess() returns error? {
    ftp:Client ftpsClient = check new ({
        protocol: ftp:FTPS,
        host: commons:FTP_HOST,
        port: commons:FTPS_IMPLICIT_PORT,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD},
            secureSocket: {
                key: {path: commons:KEYSTORE_PATH, password: "changeit"},
                cert: {path: commons:KEYSTORE_PATH, password: "changeit"},
                mode: ftp:IMPLICIT,
                dataChannelProtection: ftp:PRIVATE
            }
        }
    });
    boolean|ftp:Error result = ftpsClient->exists(commons:FTPS_ROOT);
    test:assertFalse(result is ftp:Error,
        "Expected successful FTPS IMPLICIT connection");
    check ftpsClient->close();
}

// =============================================================================
// Connection lifecycle
// =============================================================================

// close() on an active FTPS connection must complete without error.
@test:Config {
    groups: ["ftps-connection", "lifecycle"]
}
function testFtpsConnectionCloseSuccess() returns error? {
    ftp:Client ftpsClient = check new ({
        protocol: ftp:FTPS,
        host: commons:FTP_HOST,
        port: commons:FTPS_EXPLICIT_PORT,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD},
            secureSocket: {
                key: {path: commons:KEYSTORE_PATH, password: "changeit"},
                cert: {path: commons:KEYSTORE_PATH, password: "changeit"},
                mode: ftp:EXPLICIT
            }
        }
    });
    ftp:Error? closeResult = ftpsClient->close();
    test:assertFalse(closeResult is ftp:Error,
        "Expected clean close of FTPS connection");
}

// Any operation after close() must return the standard "already closed" error.
@test:Config {
    groups: ["ftps-connection", "lifecycle"]
}
function testFtpsOperationAfterClose() returns error? {
    ftp:Client ftpsClient = check new ({
        protocol: ftp:FTPS,
        host: commons:FTP_HOST,
        port: commons:FTPS_EXPLICIT_PORT,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD},
            secureSocket: {
                key: {path: commons:KEYSTORE_PATH, password: "changeit"},
                cert: {path: commons:KEYSTORE_PATH, password: "changeit"},
                mode: ftp:EXPLICIT
            }
        }
    });
    check ftpsClient->close();
    boolean|ftp:Error result = ftpsClient->exists(commons:FTPS_ROOT);
    test:assertTrue(result is ftp:Error,
        "Expected error when operating on a closed FTPS connection");
    if result is ftp:Error {
        test:assertEquals(result.message(), commons:CLIENT_ALREADY_CLOSED_MSG,
            "Unexpected error message: " + result.message());
    }
}

// =============================================================================
// Negative: wrong protocol / missing secureSocket
// =============================================================================

// Using FTP protocol with a secureSocket config block must be rejected at
// construction time — the field is only meaningful for FTPS.
@test:Config {
    groups: ["ftps-connection", "negative"]
}
function testFtpsConnectWithWrongProtocol() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:FTPS_EXPLICIT_PORT,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD},
            secureSocket: {
                key: {path: commons:KEYSTORE_PATH, password: "changeit"},
                cert: {path: commons:KEYSTORE_PATH, password: "changeit"},
                mode: ftp:EXPLICIT
            }
        }
    });
    test:assertTrue(result is ftp:Error,
        "Expected error when using FTP protocol with a secureSocket config");
    if result is ftp:Error {
        test:assertTrue(result.message().includes("secureSocket can only be used with FTPS protocol"),
            "Unexpected error message: " + result.message());
    }
}

// FTPS without any secureSocket configuration must fail — TLS cannot be
// established without at least a truststore.
@test:Config {
    groups: ["ftps-connection", "negative"]
}
function testFtpsConnectWithNoSecureSocket() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:FTPS,
        host: commons:FTP_HOST,
        port: commons:FTPS_EXPLICIT_PORT,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}
        }
    });
    test:assertTrue(result is ftp:Error,
        "Expected error for FTPS connection without secureSocket");
    if result is ftp:Error {
        test:assertTrue(result.message().startsWith("Error while connecting to the FTP server"),
            "Unexpected error message: " + result.message());
    }
}

// =============================================================================
// Negative: invalid TLS material
// =============================================================================

// A keystore path that does not exist must cause a connection failure.
@test:Config {
    groups: ["ftps-connection", "negative"]
}
function testFtpsConnectWithInvalidKeystorePath() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:FTPS,
        host: commons:FTP_HOST,
        port: commons:FTPS_EXPLICIT_PORT,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD},
            secureSocket: {
                key: {path: "tests/resources/nonexistent.jks", password: "changeit"},
                cert: {path: commons:KEYSTORE_PATH, password: "changeit"},
                mode: ftp:EXPLICIT
            }
        }
    });
    test:assertTrue(result is ftp:Error,
        "Expected error for invalid keystore path");
    if result is ftp:Error {
        test:assertTrue(result.message().includes("Failed to load KeyStore"),
            "Unexpected error message: " + result.message());
    }
}

// A truststore (cert) path that does not exist must cause a connection failure.
@test:Config {
    groups: ["ftps-connection", "negative"]
}
function testFtpsConnectWithInvalidTruststorePath() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:FTPS,
        host: commons:FTP_HOST,
        port: commons:FTPS_EXPLICIT_PORT,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD},
            secureSocket: {
                key: {path: commons:KEYSTORE_PATH, password: "changeit"},
                cert: {path: "tests/resources/nonexistent.jks", password: "changeit"},
                mode: ftp:EXPLICIT
            }
        }
    });
    test:assertTrue(result is ftp:Error,
        "Expected error for invalid truststore path");
    if result is ftp:Error {
        test:assertTrue(result.message().includes("Failed to load KeyStore"),
            "Unexpected error message: " + result.message());
    }
}

// A wrong keystore password must cause a KeyStore load failure.
@test:Config {
    groups: ["ftps-connection", "negative"]
}
function testFtpsConnectWithWrongKeystorePassword() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:FTPS,
        host: commons:FTP_HOST,
        port: commons:FTPS_EXPLICIT_PORT,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD},
            secureSocket: {
                key: {path: commons:KEYSTORE_PATH, password: "wrongpassword"},
                cert: {path: commons:KEYSTORE_PATH, password: "changeit"},
                mode: ftp:EXPLICIT
            }
        }
    });
    test:assertTrue(result is ftp:Error,
        "Expected error for wrong keystore password");
}

// =============================================================================
// Negative: unreachable server
// =============================================================================

// A port with no server listening must yield a connection error.
@test:Config {
    groups: ["ftps-connection", "negative"]
}
function testFtpsConnectWithWrongPort() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:FTPS,
        host: commons:FTP_HOST,
        port: 59998,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD},
            secureSocket: {
                key: {path: commons:KEYSTORE_PATH, password: "changeit"},
                cert: {path: commons:KEYSTORE_PATH, password: "changeit"},
                mode: ftp:EXPLICIT
            }
        }
    });
    test:assertTrue(result is ftp:Error,
        "Expected connection error for non-listening port");
    if result is ftp:Error {
        test:assertTrue(result.message().startsWith("Error while connecting to the FTP server"),
            "Unexpected error message: " + result.message());
    }
}

// A non-resolvable hostname must yield a connection error.
@test:Config {
    groups: ["ftps-connection", "negative"]
}
function testFtpsConnectWithInvalidHost() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:FTPS,
        host: "nonexistent.invalid.ftps.example",
        port: commons:FTPS_EXPLICIT_PORT,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD},
            secureSocket: {
                key: {path: commons:KEYSTORE_PATH, password: "changeit"},
                cert: {path: commons:KEYSTORE_PATH, password: "changeit"},
                mode: ftp:EXPLICIT
            }
        }
    });
    test:assertTrue(result is ftp:Error,
        "Expected connection error for non-resolvable hostname");
    if result is ftp:Error {
        test:assertTrue(result.message().startsWith("Error while connecting to the FTP server"),
            "Unexpected error message: " + result.message());
    }
}

// =============================================================================
// Negative: wrong credentials
// =============================================================================

// Wrong password with valid TLS material must be rejected by the server.
@test:Config {
    groups: ["ftps-connection", "negative"]
}
function testFtpsConnectWithWrongPassword() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:FTPS,
        host: commons:FTP_HOST,
        port: commons:FTPS_EXPLICIT_PORT,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: "wrongpassword"},
            secureSocket: {
                key: {path: commons:KEYSTORE_PATH, password: "changeit"},
                cert: {path: commons:KEYSTORE_PATH, password: "changeit"},
                mode: ftp:EXPLICIT
            }
        }
    });
    test:assertTrue(result is ftp:Error,
        "Expected error when connecting with wrong password");
}

// Wrong username with valid TLS material must be rejected by the server.
@test:Config {
    groups: ["ftps-connection", "negative"]
}
function testFtpsConnectWithWrongUsername() {
    ftp:Client|ftp:Error result = new ({
        protocol: ftp:FTPS,
        host: commons:FTP_HOST,
        port: commons:FTPS_EXPLICIT_PORT,
        auth: {
            credentials: {username: "wronguser", password: commons:FTP_PASSWORD},
            secureSocket: {
                key: {path: commons:KEYSTORE_PATH, password: "changeit"},
                cert: {path: commons:KEYSTORE_PATH, password: "changeit"},
                mode: ftp:EXPLICIT
            }
        }
    });
    test:assertTrue(result is ftp:Error,
        "Expected error when connecting with wrong username");
}

// =============================================================================
// FTPS Listener — basic polling (exercises configureServerFtpsSecureSocket)
// =============================================================================

// An ftp:Listener with protocol=FTPS and a secureSocket auth config must poll
// the FTPS server and deliver file-change events.
// This is the first listener-side test for FTPS and exercises
// FtpListenerHelper.configureServerFtpsSecureSocket() which sets the TLS
// keystore/truststore VFS parameters for FTPS connections.

isolated boolean ftpsListenerFileReceived = false;

@test:Config {
    groups: ["ftps-connection", "ftps-listener"]
}
function testFtpsListener_DetectsNewFile() returns error? {
    lock { ftpsListenerFileReceived = false; }

    ftp:Service svc = service object {
        remote function onFileChange(ftp:WatchEvent & readonly event) {
            foreach ftp:FileInfo fi in event.addedFiles {
                if fi.name.endsWith(".ftps-lst") {
                    lock { ftpsListenerFileReceived = true; }
                }
            }
        }
    };

    ftp:Listener ftpsListener = check new ({
        protocol: ftp:FTPS,
        host: commons:FTP_HOST,
        port: commons:FTPS_EXPLICIT_PORT,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD},
            secureSocket: {
                key: {path: commons:KEYSTORE_PATH, password: "changeit"},
                cert: {path: commons:KEYSTORE_PATH, password: "changeit"},
                mode: ftp:EXPLICIT,
                dataChannelProtection: ftp:PRIVATE
            }
        },
        path: commons:FTPS_LISTENER_ROOT,
        pollingInterval: 2
    });
    check ftpsListener.attach(svc);
    check ftpsListener.'start();
    runtime:registerListener(ftpsListener);

    // Upload a file using an FTPS client so the listener can detect it.
    ftp:Client ftpsClient = check new ({
        protocol: ftp:FTPS,
        host: commons:FTP_HOST,
        port: commons:FTPS_EXPLICIT_PORT,
        auth: {
            credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD},
            secureSocket: {
                key: {path: commons:KEYSTORE_PATH, password: "changeit"},
                cert: {path: commons:KEYSTORE_PATH, password: "changeit"},
                mode: ftp:EXPLICIT,
                dataChannelProtection: ftp:PRIVATE
            }
        }
    });

    string remoteFile = commons:FTPS_LISTENER_ROOT + "/trigger.ftps-lst";
    check ftpsClient->putText(remoteFile, "ftps-listener-test");

    // Wait up to 15 s for the listener to fire.
    boolean received = false;
    int remaining = 15;
    while remaining > 0 {
        lock { received = ftpsListenerFileReceived; }
        if received {
            break;
        }
        runtime:sleep(1);
        remaining -= 1;
    }

    // Assert before cleanup so a slow teardown does not affect the verdict.
    test:assertTrue(received,
        "FTPS listener (with secureSocket TLS config) should detect a newly uploaded file");

    // Deregister the listener; skip gracefulStop() as it blocks indefinitely
    // on FTPS/TLS teardown in the test environment.
    runtime:deregisterListener(ftpsListener);

    do { check ftpsClient->delete(remoteFile); } on fail { }
    do { check ftpsClient->close(); } on fail { }
}
