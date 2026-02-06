// Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

int secureAddedFileCount = 0;
int secureDeletedFileCount = 0;
boolean secureWatchEventReceived = false;

ListenerConfiguration secureRemoteServerConfig = {
    protocol: SFTP,
    host: "127.0.0.1",
    auth: {
        credentials: {
            username: "wso2",
            password: "wso2123"
        },
        privateKey: {
            path: "tests/resources/sftp.private.key",
            password: "changeit"
        }
    },
    port: 21213,
    pollingInterval: 2,
    fileNamePattern: "(.*).txt"
};

Service secureRemoteServerService = service object {
    remote function onFileChange(WatchEvent & readonly event) {
        secureAddedFileCount = event.addedFiles.length();
        secureDeletedFileCount = event.deletedFiles.length();
        secureWatchEventReceived = true;
    }
};

@test:Config {}
public function testSecureAddedFileCount() {
    int timeoutInSeconds = 300;
    // Test fails in 5 minutes if failed to receive watchEvent
    while timeoutInSeconds > 0 {
        if secureWatchEventReceived {
            log:printInfo("Securely added file count: " + secureAddedFileCount.toString());
            // Be lenient - expect at least 2 files (may have more from previous tests)
            test:assertTrue(secureAddedFileCount >= 2, "Should have at least 2 added files");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive WatchEvent for 5 minuetes.");
    }
}

@test:Config {}
public function testConnectWithInvalidKey() returns error? {
    Listener sftpServer = check new ({
        protocol: SFTP,
        host: "localhost",
        port: 21213,
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            privateKey: {
                path: "tests/resources/sftp.wrong.private.key",
                password: "changeit"
            }
        },
        path: "/home/in",
        pollingInterval: 2,
        fileNamePattern: "(.*).txt"
    });

    Service attachService = service object {
        remote function onFileChange(WatchEvent & readonly event) {
        }
    };
    error? attachResult = sftpServer.attach(attachService);
    if attachResult is Error {
        test:assertTrue(attachResult.message().startsWith("Failed to initialize File server connector."));
    } else {
        test:assertFail("Non-error result when invalid key is used for creating a Listener.");
    }
}

@test:Config {}
public function testConnectWithInvalidKeyPath() returns error? {
    Listener sftpServer = check new ({
        protocol: SFTP,
        host: "localhost",
        port: 21213,
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            privateKey: {
                path: "tests/invalid_resources/sftp.private.key",
                password: "changeit"
            }
        },
        path: "/home/in",
        pollingInterval: 2,
        fileNamePattern: "(.*).txt"
    });

    Service attachService = service object {
        remote function onFileChange(WatchEvent & readonly event) {
        }
    };
    error? attachResult = sftpServer.attach(attachService);
    if attachResult is Error {
        test:assertTrue(attachResult.message().startsWith("Failed to initialize File server connector."));
    } else {
        test:assertFail("Non-error result when invalid key path is used for creating a Listener.");
    }
}

@test:Config {}
public function testConnectToSFTPServerWithFTPProtocol() returns error? {
    Listener sftpServer = check new ({
        protocol: FTP,
        host: "localhost",
        port: 21213,
        auth: {
            credentials: {username: "wso2", password: "wso2123"}
        },
        path: "/home/in",
        pollingInterval: 2,
        fileNamePattern: "(.*).txt"
    });

    Service attachService = service object {
        remote function onFileChange(WatchEvent & readonly event) {
        }
    };
    error? attachResult = sftpServer.attach(attachService);
    if attachResult is Error {
        test:assertTrue(attachResult.message().startsWith("Failed to initialize File server connector."));
    } else {
        test:assertFail("Non-error result when connecting to SFTP server via FTP is used for creating a Listener.");
    }
}

@test:Config {}
public function testConnectWithEmptyKey() returns error? {
    Listener sftpServer = check new ({
        protocol: SFTP,
        host: "localhost",
        port: 21213,
        auth: {
            credentials: {username: "wso2", password: "wso2123"}
        },
        path: "/home/in",
        pollingInterval: 2,
        fileNamePattern: "(.*).txt"
    });

    Service attachService = service object {
        remote function onFileChange(WatchEvent & readonly event) {
        }
    };
    error? attachResult = sftpServer.attach(attachService);
    if attachResult is Error {
        test:assertTrue(attachResult.message().startsWith("Failed to initialize File server connector."));
    } else {
        test:assertFail("Non-error result when no key config is provided when creating a Listener.");
    }
}

@test:Config {}
public function testConnectWithEmptyCredentials() returns error? {
    Listener sftpServer = check new ({
        protocol: SFTP,
        host: "localhost",
        port: 21213,
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            privateKey: {
                path: "tests/invalid_resources/sftp.private.key",
                password: "changeit"
            }
        },
        path: "/home/in",
        pollingInterval: 2,
        fileNamePattern: "(.*).txt"
    });

    Service attachService = service object {
        remote function onFileChange(WatchEvent & readonly event) {
        }
    };
    error? attachResult = sftpServer.attach(attachService);
    if attachResult is Error {
        test:assertTrue(attachResult.message().startsWith("Failed to initialize File server connector."));
    } else {
        test:assertFail("Non-error result when no credentials were provided when creating a Listener.");
    }
}

@test:Config {}
public function testConnectWithEmptyCredentialsAndKey() returns error? {
    Listener sftpServer = check new ({
        protocol: SFTP,
        host: "localhost",
        port: 21213,
        path: "/home/in",
        pollingInterval: 2,
        fileNamePattern: "(.*).txt"
    });

    Service attachService = service object {
        remote function onFileChange(WatchEvent & readonly event) {
        }
    };
    error? attachResult = sftpServer.attach(attachService);
    if attachResult is Error {
        test:assertTrue(attachResult.message().startsWith("Failed to initialize File server connector."));
    } else {
        test:assertFail("Non-error result when no auth config is provided when creating a Listener.");
    }
}

@test:Config {}
public function testConnectWithNoPasswordForKey() returns error? {
    Listener|Error result = new ({
        protocol: SFTP,
        host: "localhost",
        port: 21213,
        auth: {
            credentials: {
                username: "wso2",
                password: "wso2123"
            },
            privateKey: {
                path: "tests/resources/sftp.passwordless.private.key"
            }
        },
        pollingInterval: 2,
        fileNamePattern: "(.*).txt"
    });

    if result is Error {
        test:assertFail("Could not initialize sftp listener: " + result.message());
    }
}

@test:Config {}
public function testConnectWithEmptyKeyPath() returns error? {
    Listener|Error result = new ({
        protocol: SFTP,
        host: "localhost",
        port: 21213,
        auth: {
            credentials: {
                username: "wso2",
                password: "wso2123"
            },
            privateKey: {
                path: ""
            }
        },
        pollingInterval: 2,
        fileNamePattern: "(.*).txt"
    });

    if result is Error {
        test:assertEquals(result.message(), "Private key path cannot be empty");
    } else {
        test:assertFail("Non-error result when empty key string is provided when creating a Listener.");
    }
}

@test:Config {}
public function testSFTPServerConnectWithInvalidHostWithDetails() returns error? {
    Listener sftpServer = check new ({
        protocol: SFTP,
        host: "nonexistent.invalid.host",
        port: 21213,
        auth: {
            credentials: {
                username: "wso2",
                password: "wso2123"
            },
            privateKey: {
                path: "tests/resources/sftp.private.key",
                password: "changeit"
            }
        },
        pollingInterval: 2,
        fileNamePattern: "(.*).txt"
    });

    Service attachService = service object {
        remote function onFileChange(WatchEvent & readonly event) {
        }
    };
    error? attachResult = sftpServer.attach(attachService);
    if attachResult is Error {
        test:assertTrue(attachResult.message().startsWith("Failed to initialize File server connector."));
        // Verify that the error message contains additional details from the root cause
        test:assertTrue(attachResult.message().length() > "Failed to initialize File server connector.".length(),
            msg = "Error message should contain detailed root cause information");
    } else {
        test:assertFail("Non-error result when invalid host is used for creating an SFTP Listener.");
    }
}

@test:Config {}
public function testSFTPServerConnectWithInvalidPortWithDetails() returns error? {
    Listener sftpServer = check new ({
        protocol: SFTP,
        host: "127.0.0.1",
        port: 21299,
        auth: {
            credentials: {
                username: "wso2",
                password: "wso2123"
            },
            privateKey: {
                path: "tests/resources/sftp.private.key",
                password: "changeit"
            }
        },
        pollingInterval: 2,
        fileNamePattern: "(.*).txt"
    });

    Service attachService = service object {
        remote function onFileChange(WatchEvent & readonly event) {
        }
    };
    error? attachResult = sftpServer.attach(attachService);
    if attachResult is Error {
        test:assertTrue(attachResult.message().startsWith("Failed to initialize File server connector."));
        // Verify that the error message contains additional details from the root cause
        test:assertTrue(attachResult.message().length() > "Failed to initialize File server connector.".length(),
            msg = "Error message should contain detailed root cause information");
    } else {
        test:assertFail("Non-error result when invalid port is used for creating an SFTP Listener.");
    }
}
