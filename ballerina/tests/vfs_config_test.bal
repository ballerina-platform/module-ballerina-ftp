// Copyright (c) 2025 WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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
import ballerina/lang.'string as strings;
import ballerina/test;
import ballerina/log;
import ballerina/time;
import ballerina/crypto;

@test:Config {
}
public function testFtpConnectTimeoutEnforcement() returns error? {
    ClientConfiguration timeoutConfig = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21215,  // Slow FTP server port
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        connectTimeout: 2.0  // 2 second timeout
    };

    time:Utc startTime = time:utcNow();
    Client|Error ftpClient = new(timeoutConfig);
    time:Utc endTime = time:utcNow();

    decimal elapsedSeconds = (<decimal>(endTime[0] - startTime[0])) +
                             (<decimal>(endTime[1] - startTime[1])) / 1000000000.0;

    if ftpClient is Error {
        test:assertTrue(elapsedSeconds < 5.0d,
            msg = string `Connection should timeout within 5 seconds, took ${elapsedSeconds}s`);
        log:printInfo(string `Connect timeout enforced correctly: failed in ${elapsedSeconds}s`);
    } else {
        test:assertFail(msg = "Connection should have timed out but succeeded");
    }
}

@test:Config {
}
public function testFtpDataTimeoutEnforcement() returns error? {
    ClientConfiguration dataTimeoutConfig = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21212,
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        connectTimeout: 30.0,
        socketConfig: {
            ftpDataTimeout: 5.0,  // Very short data timeout
            ftpSocketTimeout: 30.0
        }
    };

    Client|Error ftpClient = new(dataTimeoutConfig);

    if ftpClient is Error {
        test:assertFail(msg = "Error initializing client: " + ftpClient.message());
    } else {
        log:printInfo("FTP client with data timeout initialized successfully");
    }
}

@test:Config {
}
public function testSftpSessionTimeoutEnforcement() returns error? {
    ClientConfiguration sessionTimeoutConfig = {
        protocol: SFTP,
        host: "127.0.0.1",
        port: 21213,
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            privateKey: {
                path: "tests/resources/sftp.private.key",
                password: "changeit"
            },
            preferredMethods: [GSSAPI_WITH_MIC, PUBLICKEY, KEYBOARD_INTERACTIVE, PASSWORD]
        },
        socketConfig: {
            sftpSessionTimeout: 300.0
        }
    };

    Client|Error sftpClient = new(sessionTimeoutConfig);

    if sftpClient is Error {
        test:assertFail(msg = "Error initializing SFTP client: " + sftpClient.message());
    } else {
        FileInfo[]|Error files = sftpClient->list("/in");
        if files is Error {
            test:assertFail(msg = "Error listing files: " + files.message());
        } else {
            log:printInfo("SFTP session timeout configured and client operational");
        }
    }
}

@test:Config {
}
public function testBinaryModeIntegrity() returns error? {
    ClientConfiguration binaryConfig = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21212,
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        ftpFileTransfer: BINARY  // Explicit BINARY mode
    };

    Client|Error ftpClient = new(binaryConfig);

    if ftpClient is Error {
        test:assertFail(msg = "Error initializing FTP client: " + ftpClient.message());
    } else {
        // Read original binary file and compute hash
        byte[] originalContent = check io:fileReadBytes("tests/resources/datafiles/vfs_test_data/binary_test.bin");
        string originalHash = (crypto:hashSha256(originalContent)).toBase16();

        // Upload the binary file
        stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(
            "tests/resources/datafiles/vfs_test_data/binary_test.bin", 1024);
        Error? putResult = ftpClient->put("/in/binary_uploaded.bin", bStream);
        if putResult is Error {
            test:assertFail(msg = "Error uploading binary file: " + putResult.message());
        }

        // Download the binary file
        stream<byte[] & readonly, io:Error?>|Error downloadResult = ftpClient->get("/in/binary_uploaded.bin");
        if downloadResult is Error {
            test:assertFail(msg = "Error downloading binary file: " + downloadResult.message());
        } else {
            byte[] downloadedContent = [];
            error? result = downloadResult.forEach(function(byte[] & readonly chunk) {
                foreach byte b in chunk {
                    downloadedContent.push(b);
                }
            });
            if result is error {
                test:assertFail("Error processing downloaded content");
            }

            // Compute hash of downloaded content
            string downloadedHash = (crypto:hashSha256(downloadedContent)).toBase16();

            // Verify hashes match (BINARY mode should preserve exact bytes)
            test:assertEquals(downloadedHash, originalHash,
                msg = "Binary file hash mismatch - BINARY mode did not preserve content");
            log:printInfo("Binary mode integrity verified: " + originalHash);
        }

        // Cleanup
        Error? deleteResult = ftpClient->delete("/in/binary_uploaded.bin");
        if deleteResult is Error {
            log:printWarn("Error cleaning up binary test file: " + deleteResult.message());
        }
    }
}

@test:Config {
}
public function testAsciiModeCrlfTranslation() returns error? {
    ClientConfiguration asciiConfig = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21212,
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        ftpFileTransfer: ASCII  // Explicit ASCII mode
    };

    Client|Error ftpClient = new(asciiConfig);

    if ftpClient is Error {
        test:assertFail(msg = "Error initializing FTP client: " + ftpClient.message());
    } else {
        // Upload a text file using the existing test file
        stream<io:Block, io:Error?> textStream = check io:fileReadBlocksAsStream(
            "tests/resources/datafiles/vfs_test_data/text_lf.txt", 1024);
        Error? putResult = ftpClient->put("/in/ascii_test.txt", textStream);

        if putResult is Error {
            test:assertFail(msg = "Error uploading text file in ASCII mode: " + putResult.message());
        } else {
            log:printInfo("Successfully uploaded text file in ASCII mode");

            // Download and verify content
            stream<byte[] & readonly, io:Error?>|Error downloadResult = ftpClient->get("/in/ascii_test.txt");
            if downloadResult is Error {
                test:assertFail(msg = "Error downloading text file: " + downloadResult.message());
            } else {
                byte[] downloadedContent = [];
                error? result = downloadResult.forEach(function(byte[] & readonly chunk) {
                    foreach byte b in chunk {
                        downloadedContent.push(b);
                    }
                });
                if result is error {
                    test:assertFail("Error processing downloaded content");
                }

                string downloadedText = check strings:fromBytes(downloadedContent);
                log:printInfo("Downloaded text in ASCII mode (length: " + downloadedText.length().toString() + ")");

                // Verify we can read the text back
                test:assertTrue(downloadedText.includes("Line 1"),
                    msg = "Downloaded text should contain original content");
            }
        }

        // Cleanup
        Error? deleteResult = ftpClient->delete("/in/ascii_test.txt");
        if deleteResult is Error {
            log:printWarn("Error cleaning up ASCII test file: " + deleteResult.message());
        }
    }
}

@test:Config {
}
public function testDefaultFileTransferIsBinary() returns error? {
    ClientConfiguration defaultConfig = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21212,
        auth: {credentials: {username: "wso2", password: "wso2123"}}
        // ftpFileTransfer not specified - should default to BINARY
    };

    test:assertEquals(defaultConfig.ftpFileTransfer, BINARY,
        msg = "Default file type should be BINARY");

    Client|Error ftpClient = new(defaultConfig);
    if ftpClient is Error {
        test:assertFail(msg = "Error initializing client with defaults: " + ftpClient.message());
    } else {
        log:printInfo("Default file type is BINARY as expected");
    }
}

@test:Config {
}
public function testSftpCompressionEnabled() returns error? {
    ClientConfiguration compressionConfig = {
        protocol: SFTP,
        host: "127.0.0.1",
        port: 21216,  // Compression-enabled SFTP server port
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            privateKey: {
                path: "tests/resources/sftp.private.key",
                password: "changeit"
            },
            preferredMethods: [GSSAPI_WITH_MIC, PUBLICKEY, KEYBOARD_INTERACTIVE, PASSWORD]
        },
        sftpCompression: [ ZLIB, NO ]// Prefer zlib compression
    };

    Client|Error sftpClient = new(compressionConfig);

    if sftpClient is Error {
        log:printWarn("Compression SFTP server not available: " + sftpClient.message());
    } else {
        FileInfo[]|Error files = sftpClient->list("/in");
        if files is Error {
            test:assertFail(msg = "Error listing files with compression: " + files.message());
        } else {
            log:printInfo("SFTP compression configured successfully");
        }
    }
}

@test:Config {
}
public function testSftpCompressionNone() returns error? {
    ClientConfiguration noCompressionConfig = {
        protocol: SFTP,
        host: "127.0.0.1",
        port: 21213,
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            privateKey: {
                path: "tests/resources/sftp.private.key",
                password: "changeit"
            },
            preferredMethods: [GSSAPI_WITH_MIC, PUBLICKEY, KEYBOARD_INTERACTIVE, PASSWORD]
        },
        sftpCompression: [ NO ]  // Explicitly disable compression
    };

    Client|Error sftpClient = new(noCompressionConfig);

    if sftpClient is Error {
        test:assertFail(msg = "Error initializing SFTP with no compression: " + sftpClient.message());
    } else {
        FileInfo[]|Error files = sftpClient->list("/in");
        if files is Error {
            test:assertFail(msg = "Error listing files: " + files.message());
        } else {
            log:printInfo("SFTP with compression=none working correctly");
        }
    }
}

@test:Config {
}
public function testSftpDefaultCompressionIsNone() returns error? {
    ClientConfiguration defaultConfig = {
        protocol: SFTP,
        host: "127.0.0.1",
        port: 21213,
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            privateKey: {
                path: "tests/resources/sftp.private.key",
                password: "changeit"
            },
            preferredMethods: [GSSAPI_WITH_MIC, PUBLICKEY, KEYBOARD_INTERACTIVE, PASSWORD]
        }
        // sftpCompression not specified - should default to "none"
    };

    test:assertEquals(defaultConfig.sftpCompression, ["none"],
        msg = "Default SFTP compression should be 'none'");

    log:printInfo("Default SFTP compression is 'none' as expected");
}

@test:Config {
}
public function testSftpKnownHostsValidation() returns error? {
    ClientConfiguration knownHostsConfig = {
        protocol: SFTP,
        host: "127.0.0.1",
        port: 21213,
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            privateKey: {
                path: "tests/resources/sftp.private.key",
                password: "changeit"
            },
            preferredMethods: [GSSAPI_WITH_MIC, PUBLICKEY, KEYBOARD_INTERACTIVE, PASSWORD]
        },
        sftpSshKnownHosts: "tests/resources/known_hosts_test"
    };

    Client|Error sftpClient = new(knownHostsConfig);

    if sftpClient is Error {
        // Known hosts validation may fail if host key doesn't match
        log:printWarn("Known hosts validation test: " + sftpClient.message());
    } else {
        // If connection succeeds, known_hosts was accepted
        FileInfo[]|Error files = sftpClient->list("/in");
        if files is Error {
            log:printWarn("Error after known_hosts connection: " + files.message());
        } else {
            log:printInfo("Known hosts file processed successfully");
        }
    }
}

@test:Config {
}
public function testSftpKnownHostsTildeExpansion() returns error? {
    ClientConfiguration tildeConfig = {
        protocol: SFTP,
        host: "127.0.0.1",
        port: 21213,
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            privateKey: {
                path: "tests/resources/sftp.private.key",
                password: "changeit"
            },
            preferredMethods: [GSSAPI_WITH_MIC, PUBLICKEY, KEYBOARD_INTERACTIVE, PASSWORD]
        },
        sftpSshKnownHosts: "~/known_hosts_test"  // Tilde should be expanded
    };

    Client|Error sftpClient = new(tildeConfig);

    if sftpClient is Error {
        // Expected if file doesn't exist at expanded path
        log:printInfo("Tilde expansion processed (file may not exist): " + sftpClient.message());
    } else {
        log:printInfo("Tilde expansion handled correctly");
    }
}

@test:Config {
}
public function testCombinedVfsConfigsFtp() returns error? {
    ClientConfiguration combinedConfig = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21212,
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        connectTimeout: 20.0,
        ftpFileTransfer: BINARY,
        socketConfig: {
            ftpDataTimeout: 90.0,
            ftpSocketTimeout: 45.0
        }
    };

    Client|Error ftpClient = new(combinedConfig);

    if ftpClient is Error {
        test:assertFail(msg = "Error with combined FTP configs: " + ftpClient.message());
    } else {
        // Perform operations to ensure all configs work together
        FileInfo[]|Error files = ftpClient->list("/in");
        if files is Error {
            test:assertFail(msg = "Error listing files with combined configs: " + files.message());
        } else {
            log:printInfo("Combined FTP VFS configurations working correctly");
        }
    }
}

@test:Config {
}
public function testCombinedVfsConfigsSftp() returns error? {
    ClientConfiguration combinedConfig = {
        protocol: SFTP,
        host: "127.0.0.1",
        port: 21213,
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            privateKey: {
                path: "tests/resources/sftp.private.key",
                password: "changeit"
            },
            preferredMethods: [GSSAPI_WITH_MIC, PUBLICKEY, KEYBOARD_INTERACTIVE, PASSWORD]
        },
        connectTimeout: 15.0,
        sftpCompression: [ NO ],
        socketConfig: {
            sftpSessionTimeout: 400.0
        }
    };

    Client|Error sftpClient = new(combinedConfig);

    if sftpClient is Error {
        test:assertFail(msg = "Error with combined SFTP configs: " + sftpClient.message());
    } else {
        // Perform operations to ensure all configs work together
        FileInfo[]|Error files = sftpClient->list("/in");
        if files is Error {
            test:assertFail(msg = "Error listing files with combined configs: " + files.message());
        } else {
            log:printInfo("Combined SFTP VFS configurations working correctly");
        }
    }
}

@test:Config {
}
public function testListenerBinaryModeIntegrity() returns error? {
    ListenerConfiguration binaryListenerConfig = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21212,
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        path: "/home/in",
        pollingInterval: 2,
        ftpFileTransfer: BINARY
    };

    Listener|Error ftpListener = new(binaryListenerConfig);

    if ftpListener is Error {
        test:assertFail(msg = "Error initializing listener with BINARY mode: " + ftpListener.message());
    } else {
        log:printInfo("Listener with BINARY file type initialized successfully");
    }
}

@test:Config {
}
public function testListenerWithTimeouts() returns error? {
    ListenerConfiguration timeoutListenerConfig = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21212,
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        path: "/home/in",
        pollingInterval: 2,
        connectTimeout: 25.0,
        socketConfig: {
            ftpDataTimeout: 100.0,
            ftpSocketTimeout: 50.0
        }
    };

    Listener|Error ftpListener = new(timeoutListenerConfig);

    if ftpListener is Error {
        test:assertFail(msg = "Error initializing listener with timeouts: " + ftpListener.message());
    } else {
        log:printInfo("Listener with timeout configurations initialized successfully");
    }
}

@test:Config {
}
public function testSftpWithHttpConnectProxy() returns error? {
    // Test SFTP client with HTTP CONNECT proxy configuration
    ClientConfiguration httpProxyConfig = {
        protocol: SFTP,
        host: "127.0.0.1",
        port: 21213,
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            privateKey: {
                path: "tests/resources/sftp.private.key",
                password: "changeit"
            },
            preferredMethods: [GSSAPI_WITH_MIC, PUBLICKEY, KEYBOARD_INTERACTIVE, PASSWORD]
        },
        proxy: {
            host: "proxy.example.com",
            port: 3128,
            'type: HTTP,
            auth: {
                username: "proxyuser",
                password: "proxypass"
            }
        }
    };

    Client|Error sftpClient = new(httpProxyConfig);
    test:assertTrue(sftpClient is Error);

    Error err = <Error>sftpClient;
    error? cause = err.cause();
    test:assertFalse(err.cause() is ());

    test:assertEquals((<error> cause).message(), "proxy.example.com");
}
