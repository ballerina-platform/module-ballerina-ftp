// Copyright (c) 2025 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
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

import ballerina/lang.runtime as runtime;
import ballerina/log;
import ballerina/test;

// --- Global State for Event Capture (Managed per test) ---
isolated boolean ftpsEventReceived = false;
isolated int ftpsFileCount = 0;

function resetFtpsState() {
    lock {
        ftpsEventReceived = false;
    }
    lock {
        ftpsFileCount = 0;
    }
}

// --- Reusable Configs ---

// 1. Trigger Client Config (To create files that the listener watches)
ClientConfiguration triggerClientConfig = {
    protocol: FTPS,
    host: "127.0.0.1",
    port: 21214,
    auth: {
        credentials: {
            username: "wso2",
            password: "wso2123"
        },
        secureSocket: {
            key: {
                path: "tests/resources/keystore.jks",
                password: "changeit"
            },
            cert: {
                path: "tests/resources/keystore.jks",
                password: "changeit"
            },
            mode: EXPLICIT,
            dataChannelProtection: PRIVATE
        }
    }
};

// 2. Trigger Client Config for Implicit Mode
ClientConfiguration triggerImplicitClientConfig = {
    protocol: FTPS,
    host: "127.0.0.1",
    port: 21217,
    auth: {
        credentials: {
            username: "wso2",
            password: "wso2123"
        },
        secureSocket: {
            key: {
                path: "tests/resources/keystore.jks",
                password: "changeit"
            },
            cert: {
                path: "tests/resources/keystore.jks",
                password: "changeit"
            },
            mode: IMPLICIT,
            dataChannelProtection: PRIVATE
        }
    }
};

// --- Positive Tests ---

@test:Config {}
function testFtpsExplicitListener() returns error? {
    // 1. Setup specific path for isolation
    string watchPath = "/ftps-listener"; 
    string targetFile = watchPath + "/explicit_trigger.txt";
    
    // 2. Initialize Helper Client
    Client triggerClient = check new(triggerClientConfig);
    check removeIfExists(triggerClient, targetFile);
    resetFtpsState();

    // 3. Define Service
    Service ftpsService = service object {
        remote function onFileChange(WatchEvent & readonly event) {
            if event.addedFiles.length() == 0 { return; }
            lock {
                ftpsEventReceived = true;
            }
            lock {
                ftpsFileCount = event.addedFiles.length();
            }
            log:printInfo("Explicit Event: " + event.addedFiles.toString());
        }
    };

    // 4. Start Listener (Self-Contained)
    Listener ftpsListener = check new ({
        protocol: FTPS,
        host: "127.0.0.1",
        port: 21214,
        auth: triggerClientConfig.auth, 
        path: watchPath, // Watch ONLY this folder
        pollingInterval: 2,
        fileNamePattern: "explicit_trigger.txt"
    });

    check ftpsListener.attach(ftpsService);
    check ftpsListener.'start();
    runtime:registerListener(ftpsListener);

    // 5. Trigger Event
    check triggerClient->put(targetFile, "data");

    // 6. Wait for Event
    int waitCount = 0;
    while waitCount < 20 {
        boolean seen;
        lock { seen = ftpsEventReceived; }
        if seen { break; }
        runtime:sleep(1);
        waitCount += 1;
    }

    // 7. Stop Listener
    check ftpsListener.gracefulStop();
    runtime:deregisterListener(ftpsListener);
    
    // 8. Assertions
    boolean eventSeen;
    lock { eventSeen = ftpsEventReceived; }
    test:assertTrue(eventSeen, "FTPS Explicit Listener failed to detect file.");

    // 9. Cleanup
    check removeIfExists(triggerClient, targetFile);
}

@test:Config {}
function testFtpsImplicitListener() returns error? {
    string watchPath = "/ftps-listener"; 
    string targetFile = watchPath + "/implicit_trigger.txt";
    
    Client triggerClient = check new(triggerImplicitClientConfig);
    check removeIfExists(triggerClient, targetFile);
    resetFtpsState();

    Service ftpsService = service object {
        remote function onFileChange(WatchEvent & readonly event) {
            if event.addedFiles.length() == 0 { return; }
            lock {
                ftpsEventReceived = true;
            }
            log:printInfo("Implicit Event: " + event.addedFiles.toString());
        }
    };

    Listener ftpsListener = check new ({
        protocol: FTPS,
        host: "127.0.0.1",
        port: 21217,
        auth: triggerImplicitClientConfig.auth,
        path: watchPath,
        pollingInterval: 2,
        fileNamePattern: "implicit_trigger.txt"
    });

    check ftpsListener.attach(ftpsService);
    check ftpsListener.'start();
    runtime:registerListener(ftpsListener);

    check triggerClient->put(targetFile, "data");

    int waitCount = 0;
    while waitCount < 20 {
        boolean seen;
        lock { seen = ftpsEventReceived; }
        if seen { break; }
        runtime:sleep(1);
        waitCount += 1;
    }

    check ftpsListener.gracefulStop();
    runtime:deregisterListener(ftpsListener);
    
    boolean eventSeen;
    lock { eventSeen = ftpsEventReceived; }
    test:assertTrue(eventSeen, "FTPS Implicit Listener failed to detect file.");

    check removeIfExists(triggerClient, targetFile);
}

// --- Negative Tests ---

@test:Config {}
public function testFtpsConnectWithInvalidKeystore() returns error? {
    Listener|Error ftpsServer = new ({
        protocol: FTPS,
        host: "localhost",
        port: 21214,
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            secureSocket: {
                key: {
                    path: "tests/resources/invalid.keystore.jks",
                    password: "changeit"
                },
                cert: {
                    path: "tests/resources/keystore.jks",
                    password: "changeit"
                },
                mode: EXPLICIT
            }
        },
        path: "/ftps-listener",
        pollingInterval: 2
    });

    if ftpsServer is Error {
        test:assertTrue(ftpsServer.message().startsWith("Failed to initialize File server connector.") ||
            ftpsServer.message().includes("Failed to load FTPS Server Keystore"),
            msg = "Expected error for invalid keystore. Got: " + ftpsServer.message());
    } else {
        test:assertFail("Non-error result when invalid keystore is used.");
    }
}

@test:Config {}
public function testFtpsConnectWithInvalidTruststore() returns error? {
    Listener|Error ftpsServer = new ({
        protocol: FTPS,
        host: "localhost",
        port: 21214,
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            secureSocket: {
                key: {
                    path: "tests/resources/keystore.jks",
                    password: "changeit"
                },
                cert: {
                    path: "tests/resources/invalid.truststore.jks",
                    password: "changeit"
                },
                mode: EXPLICIT
            }
        },
        path: "/ftps-listener",
        pollingInterval: 2
    });

    if ftpsServer is Error {
        test:assertTrue(ftpsServer.message().startsWith("Failed to initialize File server connector.") ||
            ftpsServer.message().includes("Failed to load FTPS Server Truststore"),
            msg = "Expected error for invalid truststore. Got: " + ftpsServer.message());
    } else {
        test:assertFail("Non-error result when invalid truststore is used.");
    }
}

@test:Config {}
public function testFtpsConnectToFTPServerWithFTPProtocol() returns error? {
    Listener|Error ftpsServer = new ({
        protocol: FTP,
        host: "localhost",
        port: 21214,
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            secureSocket: {
                key: {
                    path: "tests/resources/keystore.jks",
                    password: "changeit"
                },
                cert: {
                    path: "tests/resources/keystore.jks",
                    password: "changeit"
                },
                mode: EXPLICIT
            }
        },
        path: "/ftps-listener",
        pollingInterval: 2
    });

    if ftpsServer is Error {
        test:assertTrue(ftpsServer.message().startsWith("Failed to initialize File server connector.") ||
            ftpsServer.message().includes("secureSocket can only be used with FTPS protocol"),
            msg = "Expected error for wrong protocol");
    } else {
        test:assertFail("Non-error result when connecting to FTPS server via FTP.");
    }
}

@test:Config {}
public function testFtpsListenerConnectWithEmptySecureSocket() returns error? {
    Listener|Error ftpsServer = new ({
        protocol: FTPS,
        host: "localhost",
        port: 21214,
        auth: {
            credentials: {username: "wso2", password: "wso2123"}
        },
        path: "/ftps-listener",
        pollingInterval: 2
    });

    if ftpsServer is Error {
        test:assertTrue(ftpsServer.message().startsWith("Failed to initialize File server connector."),
            msg = "Expected error for missing secureSocket");
    } else {
        test:assertFail("Non-error result when no secureSocket config is provided.");
    }
}

@test:Config {}
public function testFtpsConnectWithEmptyCredentials() returns error? {
    Listener|Error ftpsServer = new ({
        protocol: FTPS,
        host: "localhost",
        port: 21214,
        auth: {
            secureSocket: {
                key: {
                    path: "tests/resources/keystore.jks",
                    password: "changeit"
                },
                cert: {
                    path: "tests/resources/keystore.jks",
                    password: "changeit"
                },
                mode: EXPLICIT
            }
        },
        path: "/ftps-listener",
        pollingInterval: 2
    });

    if ftpsServer is Error {
        test:assertTrue(ftpsServer.message().startsWith("Failed to initialize File server connector."),
            msg = "Expected error for missing credentials");
    } else {
        test:assertFail("Non-error result when no credentials were provided.");
    }
}

@test:Config {}
public function testFtpsConnectWithEmptyKeystorePath() returns error? {
    Listener|Error result = new ({
        protocol: FTPS,
        host: "localhost",
        port: 21214,
        auth: {
            credentials: {
                username: "wso2",
                password: "wso2123"
            },
            secureSocket: {
                key: {
                    path: "",
                    password: ""
                },
                cert: {
                    path: "tests/resources/keystore.jks",
                    password: "changeit"
                },
                mode: EXPLICIT
            }
        },
        path: "/ftps-listener",
        pollingInterval: 2
    });

    if result is Error {
        test:assertTrue(result.message().includes("Failed to load") || 
            result.message().startsWith("Failed to initialize File server connector."),
            msg = "Expected error for empty keystore path");
    } else {
        test:assertFail("Non-error result when empty keystore path is provided.");
    }
}

@test:Config {}
public function testFtpsServerConnectWithInvalidHostWithDetails() returns error? {
    Listener|Error ftpsServer = new ({
        protocol: FTPS,
        host: "nonexistent.invalid.host",
        port: 21214,
        auth: {
            credentials: {
                username: "wso2",
                password: "wso2123"
            },
            secureSocket: {
                key: {
                    path: "tests/resources/keystore.jks",
                    password: "changeit"
                },
                cert: {
                    path: "tests/resources/keystore.jks",
                    password: "changeit"
                },
                mode: EXPLICIT
            }
        },
        path: "/ftps-listener",
        pollingInterval: 2
    });

    if ftpsServer is Error {
        test:assertTrue(ftpsServer.message().startsWith("Failed to initialize File server connector."));
        test:assertTrue(ftpsServer.message().length() > "Failed to initialize File server connector.".length(),
            msg = "Error message should contain detailed root cause information");
    } else {
        test:assertFail("Non-error result when invalid host is used.");
    }
}

@test:Config {}
public function testFtpsServerConnectWithInvalidPortWithDetails() returns error? {
    Listener|Error ftpsServer = new ({
        protocol: FTPS,
        host: "127.0.0.1",
        port: 21299,
        auth: {
            credentials: {
                username: "wso2",
                password: "wso2123"
            },
            secureSocket: {
                key: {
                    path: "tests/resources/keystore.jks",
                    password: "changeit"
                },
                cert: {
                    path: "tests/resources/keystore.jks",
                    password: "changeit"
                },
                mode: EXPLICIT
            }
        },
        path: "/ftps-listener",
        pollingInterval: 2
    });

    if ftpsServer is Error {
        test:assertTrue(ftpsServer.message().startsWith("Failed to initialize File server connector."));
        test:assertTrue(ftpsServer.message().length() > "Failed to initialize File server connector.".length(),
            msg = "Error message should contain detailed root cause information");
    } else {
        test:assertFail("Non-error result when invalid port is used.");
    }
}
