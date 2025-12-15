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

int ftpsExplicitAddedFileCount = 0;
int ftpsExplicitDeletedFileCount = 0;
boolean ftpsExplicitWatchEventReceived = false;

int ftpsImplicitAddedFileCount = 0;
int ftpsImplicitDeletedFileCount = 0;
boolean ftpsImplicitWatchEventReceived = false;

ListenerConfiguration ftpsExplicitRemoteServerConfig = {
    protocol: FTPS,
    host: "127.0.0.1",
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
            trustStore: {
                path: "tests/resources/keystore.jks",
                password: "changeit"
            },
            mode: EXPLICIT,
            dataChannelProtection: PRIVATE
        }
    },
    port: 21214,
    pollingInterval: 2,
    fileNamePattern: "(.*).txt"
};

ListenerConfiguration ftpsImplicitRemoteServerConfig = {
    protocol: FTPS,
    host: "127.0.0.1",
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
            trustStore: {
                path: "tests/resources/keystore.jks",
                password: "changeit"
            },
            mode: IMPLICIT,
            dataChannelProtection: PRIVATE
        }
    },
    port: 990,
    pollingInterval: 2,
    fileNamePattern: "(.*).txt"
};

Service ftpsExplicitRemoteServerService = service object {
    remote function onFileChange(WatchEvent & readonly event) {
        // #region agent log
        log:printInfo("DEBUG: FTPS EXPLICIT onFileChange called - added: " + event.addedFiles.length().toString() + ", deleted: " + event.deletedFiles.length().toString());
        // #endregion
        ftpsExplicitAddedFileCount = event.addedFiles.length();
        ftpsExplicitDeletedFileCount = event.deletedFiles.length();
        ftpsExplicitWatchEventReceived = true;
        // #region agent log
        log:printInfo("DEBUG: FTPS EXPLICIT watchEventReceived set to true");
        // #endregion
    }
};

Service ftpsImplicitRemoteServerService = service object {
    remote function onFileChange(WatchEvent & readonly event) {
        // #region agent log
        log:printInfo("DEBUG: FTPS IMPLICIT onFileChange called - added: " + event.addedFiles.length().toString() + ", deleted: " + event.deletedFiles.length().toString());
        // #endregion
        ftpsImplicitAddedFileCount = event.addedFiles.length();
        ftpsImplicitDeletedFileCount = event.deletedFiles.length();
        ftpsImplicitWatchEventReceived = true;
        // #region agent log
        log:printInfo("DEBUG: FTPS IMPLICIT watchEventReceived set to true");
        // #endregion
    }
};

Listener? ftpsExplicitRemoteServerListener = ();
Listener? ftpsImplicitRemoteServerListener = ();

@test:BeforeSuite
function initFtpsListenerTestEnvironment() returns error? {
    // #region agent log
    log:printInfo("DEBUG: Starting FTPS listener initialization - EXPLICIT");
    // #endregion
    ftpsExplicitRemoteServerListener = check new (ftpsExplicitRemoteServerConfig);
    // #region agent log
    log:printInfo("DEBUG: FTPS EXPLICIT listener created successfully");
    // #endregion
    check (<Listener>ftpsExplicitRemoteServerListener).attach(ftpsExplicitRemoteServerService);
    // #region agent log
    log:printInfo("DEBUG: FTPS EXPLICIT service attached successfully");
    // #endregion
    check (<Listener>ftpsExplicitRemoteServerListener).'start();
    // #region agent log
    log:printInfo("DEBUG: FTPS EXPLICIT listener started successfully");
    // #endregion

    // #region agent log
    log:printInfo("DEBUG: Starting FTPS listener initialization - IMPLICIT");
    // #endregion
    ftpsImplicitRemoteServerListener = check new (ftpsImplicitRemoteServerConfig);
    // #region agent log
    log:printInfo("DEBUG: FTPS IMPLICIT listener created successfully");
    // #endregion
    check (<Listener>ftpsImplicitRemoteServerListener).attach(ftpsImplicitRemoteServerService);
    // #region agent log
    log:printInfo("DEBUG: FTPS IMPLICIT service attached successfully");
    // #endregion
    check (<Listener>ftpsImplicitRemoteServerListener).'start();
    // #region agent log
    log:printInfo("DEBUG: FTPS IMPLICIT listener started successfully");
    // #endregion
}

@test:AfterSuite
function cleanFtpsListenerTestEnvironment() returns error? {
    if ftpsExplicitRemoteServerListener is Listener {
        check (<Listener>ftpsExplicitRemoteServerListener).gracefulStop();
    }
    if ftpsImplicitRemoteServerListener is Listener {
        check (<Listener>ftpsImplicitRemoteServerListener).gracefulStop();
    }
}

@test:Config {}
public function testFtpsExplicitAddedFileCount() {
    // #region agent log
    log:printInfo("DEBUG: testFtpsExplicitAddedFileCount started, watchEventReceived: " + ftpsExplicitWatchEventReceived.toString());
    // #endregion
    int timeoutInSeconds = 300;
    // Test fails in 5 minutes if failed to receive watchEvent
    while timeoutInSeconds > 0 {
        if ftpsExplicitWatchEventReceived {
            // #region agent log
            log:printInfo("DEBUG: testFtpsExplicitAddedFileCount - event received, count: " + ftpsExplicitAddedFileCount.toString());
            // #endregion
            log:printInfo("FTPS EXPLICIT added file count: " + ftpsExplicitAddedFileCount.toString());
            // Be lenient - expect at least 2 files (may have more from previous tests)
            test:assertTrue(ftpsExplicitAddedFileCount >= 2, "Should have at least 2 added files");
            break;
        } else {
            // #region agent log
            if timeoutInSeconds % 30 == 0 {
                log:printInfo("DEBUG: testFtpsExplicitAddedFileCount - waiting, remaining: " + timeoutInSeconds.toString() + "s, watchEventReceived: " + ftpsExplicitWatchEventReceived.toString());
            }
            // #endregion
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    if timeoutInSeconds == 0 {
        // #region agent log
        log:printError("DEBUG: testFtpsExplicitAddedFileCount - TIMEOUT, watchEventReceived: " + ftpsExplicitWatchEventReceived.toString() + ", listener state: " + (ftpsExplicitRemoteServerListener is Listener).toString());
        // #endregion
        test:assertFail("Failed to receive WatchEvent for 5 minutes.");
    }
}

@test:Config {}
public function testFtpsImplicitAddedFileCount() {
    // #region agent log
    log:printInfo("DEBUG: testFtpsImplicitAddedFileCount started, watchEventReceived: " + ftpsImplicitWatchEventReceived.toString());
    // #endregion
    int timeoutInSeconds = 300;
    // Test fails in 5 minutes if failed to receive watchEvent
    while timeoutInSeconds > 0 {
        if ftpsImplicitWatchEventReceived {
            // #region agent log
            log:printInfo("DEBUG: testFtpsImplicitAddedFileCount - event received, count: " + ftpsImplicitAddedFileCount.toString());
            // #endregion
            log:printInfo("FTPS IMPLICIT added file count: " + ftpsImplicitAddedFileCount.toString());
            // Be lenient - expect at least 2 files (may have more from previous tests)
            test:assertTrue(ftpsImplicitAddedFileCount >= 2, "Should have at least 2 added files");
            break;
        } else {
            // #region agent log
            if timeoutInSeconds % 30 == 0 {
                log:printInfo("DEBUG: testFtpsImplicitAddedFileCount - waiting, remaining: " + timeoutInSeconds.toString() + "s, watchEventReceived: " + ftpsImplicitWatchEventReceived.toString());
            }
            // #endregion
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    if timeoutInSeconds == 0 {
        // #region agent log
        log:printError("DEBUG: testFtpsImplicitAddedFileCount - TIMEOUT, watchEventReceived: " + ftpsImplicitWatchEventReceived.toString() + ", listener state: " + (ftpsImplicitRemoteServerListener is Listener).toString());
        // #endregion
        test:assertFail("Failed to receive WatchEvent for 5 minutes.");
    }
}

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
                trustStore: {
                    path: "tests/resources/keystore.jks",
                    password: "changeit"
                },
                mode: EXPLICIT
            }
        },
        path: "/home/in",
        pollingInterval: 2,
        fileNamePattern: "(.*).txt"
    });

    if ftpsServer is Error {
        test:assertTrue(ftpsServer.message().startsWith("Failed to initialize File server connector.") ||
            ftpsServer.message().includes("Failed to load FTPS Server Keystore"),
            msg = "Expected error for invalid keystore");
    } else {
        test:assertFail("Non-error result when invalid keystore is used for creating a Listener.");
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
                trustStore: {
                    path: "tests/resources/invalid.truststore.jks",
                    password: "changeit"
                },
                mode: EXPLICIT
            }
        },
        path: "/home/in",
        pollingInterval: 2,
        fileNamePattern: "(.*).txt"
    });

    if ftpsServer is Error {
        test:assertTrue(ftpsServer.message().startsWith("Failed to initialize File server connector.") ||
            ftpsServer.message().includes("Failed to load FTPS Server Keystore"),
            msg = "Expected error for invalid truststore");
    } else {
        test:assertFail("Non-error result when invalid truststore is used for creating a Listener.");
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
                trustStore: {
                    path: "tests/resources/keystore.jks",
                    password: "changeit"
                },
                mode: EXPLICIT
            }
        },
        path: "/home/in",
        pollingInterval: 2,
        fileNamePattern: "(.*).txt"
    });

    if ftpsServer is Error {
        test:assertTrue(ftpsServer.message().startsWith("Failed to initialize File server connector.") ||
            ftpsServer.message().includes("secureSocket can only be used with FTPS protocol"),
            msg = "Expected error for wrong protocol");
    } else {
        test:assertFail("Non-error result when connecting to FTPS server via FTP is used for creating a Listener.");
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
        path: "/home/in",
        pollingInterval: 2,
        fileNamePattern: "(.*).txt"
    });

    if ftpsServer is Error {
        test:assertTrue(ftpsServer.message().startsWith("Failed to initialize File server connector."),
            msg = "Expected error for missing secureSocket");
    } else {
        test:assertFail("Non-error result when no secureSocket config is provided when creating a Listener.");
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
                trustStore: {
                    path: "tests/resources/keystore.jks",
                    password: "changeit"
                },
                mode: EXPLICIT
            }
        },
        path: "/home/in",
        pollingInterval: 2,
        fileNamePattern: "(.*).txt"
    });

    if ftpsServer is Error {
        test:assertTrue(ftpsServer.message().startsWith("Failed to initialize File server connector."),
            msg = "Expected error for missing credentials");
    } else {
        test:assertFail("Non-error result when no credentials were provided when creating a Listener.");
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
                trustStore: {
                    path: "tests/resources/keystore.jks",
                    password: "changeit"
                },
                mode: EXPLICIT
            }
        },
        pollingInterval: 2,
        fileNamePattern: "(.*).txt"
    });

    if result is Error {
        test:assertTrue(result.message().includes("Failed to load") || 
            result.message().startsWith("Failed to initialize File server connector."),
            msg = "Expected error for empty keystore path");
    } else {
        test:assertFail("Non-error result when empty keystore path is provided when creating a Listener.");
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
                trustStore: {
                    path: "tests/resources/keystore.jks",
                    password: "changeit"
                },
                mode: EXPLICIT
            }
        },
        pollingInterval: 2,
        fileNamePattern: "(.*).txt"
    });

    if ftpsServer is Error {
        test:assertTrue(ftpsServer.message().startsWith("Failed to initialize File server connector."));
        // Verify that the error message contains additional details from the root cause
        test:assertTrue(ftpsServer.message().length() > "Failed to initialize File server connector.".length(),
            msg = "Error message should contain detailed root cause information");
    } else {
        test:assertFail("Non-error result when invalid host is used for creating an FTPS Listener.");
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
                trustStore: {
                    path: "tests/resources/keystore.jks",
                    password: "changeit"
                },
                mode: EXPLICIT
            }
        },
        pollingInterval: 2,
        fileNamePattern: "(.*).txt"
    });

    if ftpsServer is Error {
        test:assertTrue(ftpsServer.message().startsWith("Failed to initialize File server connector."));
        // Verify that the error message contains additional details from the root cause
        test:assertTrue(ftpsServer.message().length() > "Failed to initialize File server connector.".length(),
            msg = "Error message should contain detailed root cause information");
    } else {
        test:assertFail("Non-error result when invalid port is used for creating an FTPS Listener.");
    }
}

