// Copyright (c) 2025 WSO2 LLC (http://www.wso2.com).
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
import ballerina/lang.runtime as runtime;
import ballerina/test;

// Constants for test configuration
const string KEYSTORE_PATH = "tests/resources/keystore.jks";
const string KEYSTORE_PASSWORD = "changeit";
const string FTPS_CLIENT_ROOT = "/ftps-client";

// Create the config to access mock FTPS server in EXPLICIT mode
ClientConfiguration ftpsExplicitConfig = {
    protocol: FTPS,
    host: "127.0.0.1",
    port: 21214,
    connectTimeout: 60.0, // Increase to 60s for GraalVM/CI stability
    socketConfig: {
        ftpDataTimeout: 60.0,
        ftpSocketTimeout: 60.0
    },
    auth: {
        credentials: {username: "wso2", password: "wso2123"},
        secureSocket: {
            key: {
                path: KEYSTORE_PATH,
                password: KEYSTORE_PASSWORD
            },
            cert: {
                path: KEYSTORE_PATH,
                password: KEYSTORE_PASSWORD
            },
            mode: EXPLICIT,
            dataChannelProtection: PRIVATE
        }
    }
};

// Create the config to access mock FTPS server in IMPLICIT mode
ClientConfiguration ftpsImplicitConfig = {
    protocol: FTPS,
    host: "127.0.0.1",
    port: 21217,
    connectTimeout: 60.0, // Increase to 60s for GraalVM/CI stability
    socketConfig: {
        ftpDataTimeout: 60.0,
        ftpSocketTimeout: 60.0
    },
    auth: {
        credentials: {username: "wso2", password: "wso2123"},
        secureSocket: {
            key: {
                path: KEYSTORE_PATH,
                password: KEYSTORE_PASSWORD
            },
            cert: {
                path: KEYSTORE_PATH,
                password: KEYSTORE_PASSWORD
            },
            mode: IMPLICIT,
            dataChannelProtection: PRIVATE
        }
    }
};

// Create the config with CLEAR data channel protection for testing
ClientConfiguration ftpsClearDataChannelConfig = {
    protocol: FTPS,
    host: "127.0.0.1",
    port: 21214,
    auth: {
        credentials: {username: "wso2", password: "wso2123"},
        secureSocket: {
            key: {
                path: KEYSTORE_PATH,
                password: KEYSTORE_PASSWORD
            },
            cert: {
                path: KEYSTORE_PATH,
                password: KEYSTORE_PASSWORD
            },
            mode: EXPLICIT,
            dataChannelProtection: CLEAR
        }
    }
};

Client? ftpsExplicitClientEp = ();
Client? ftpsImplicitClientEp = ();
Client? ftpsClearDataChannelClientEp = ();

@test:BeforeSuite
function initFtpsTestEnvironment() returns error? {
    io:println("Initializing FTPS test clients...");
    
    ftpsExplicitClientEp = check new (ftpsExplicitConfig);
    runtime:sleep(1); // Give the mock server 1s to stabilize
    
    ftpsImplicitClientEp = check new (ftpsImplicitConfig);
    runtime:sleep(1);
    
    ftpsClearDataChannelClientEp = check new (ftpsClearDataChannelConfig);
    
    // Clean the sandbox
    check cleanFtpsTarget();
}

@test:AfterSuite
function cleanupFtpsTestEnvironment() returns error? {
    io:println("Cleaning up FTPS test files...");
    check cleanFtpsTarget();
}

function cleanFtpsTarget() returns error? {
    // Cast once for efficiency
    Client clientEp = <Client>ftpsExplicitClientEp;

    string[] files = [
        "file2.txt", "tempFtpsFile1.txt", "tempFtpsFile2.txt", 
        "tempFtpsPrivate.txt", "tempFtpsClear.txt", "tempFtpsFile3.txt", 
        "tempFtpsFile4.txt", "tempFtpsFile5.txt", "tempFtpsFile6.txt"
    ];

    foreach string f in files {
        string path = FTPS_CLIENT_ROOT + "/" + f;
        
        // 1. Perform the action and propagate error via 'check'
        // This is a statement, which is allowed.
        boolean fileExists = check clientEp->exists(path);

        // 2. Use the resulting boolean in the 'if' condition
        if fileExists {
            check clientEp->delete(path);
        }
    }
}

function matchFtpsStreamContent(stream<byte[] & readonly, io:Error?> binaryStream, string matchedString) returns boolean|error {
    string fullContent = "";
    string tempContent = "";
    int maxLoopCount = 100000;
    while maxLoopCount > 0 {
        record {|byte[] value;|}|io:Error? binaryArray = binaryStream.next();
        if binaryArray is io:Error {
            break;
        } else if binaryArray is () {
            break;
        } else {
            tempContent = check strings:fromBytes(binaryArray.value);
            fullContent = fullContent + tempContent;
            maxLoopCount -= 1;
        }
    }
    return matchedString == fullContent;
}

// --- Negative Tests (Now Independent & First) ---
@test:Config {}
public function testFtpsConnectWithWrongProtocol() returns error? {
    ClientConfiguration ftpsConfig = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21214,
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            secureSocket: {
                key: {
                    path: KEYSTORE_PATH,
                    password: KEYSTORE_PASSWORD
                },
                cert: {
                    path: KEYSTORE_PATH,
                    password: KEYSTORE_PASSWORD
                },
                mode: EXPLICIT
            }
        }
    };

    Client|Error ftpsClientEp = new (ftpsConfig);
    if ftpsClientEp is Error {
        test:assertTrue(ftpsClientEp.message().startsWith("Error while connecting to the FTP server with URL: ") ||
            ftpsClientEp.message().includes("secureSocket can only be used with FTPS protocol"),
            msg = "Unexpected error during the FTP client initialization with a FTPS server. " + ftpsClientEp.message());
    } else {
        test:assertFail(msg = "Found a non-error response while initializing FTP client with a FTPS server.");
    }
}

@test:Config {}
public function testFtpsConnectWithEmptySecureSocket() returns error? {
    ClientConfiguration emptyFtpsConfig = {
        protocol: FTPS,
        host: "127.0.0.1",
        port: 21214,
        auth: {
            credentials: {username: "wso2", password: "wso2123"}
        }
    };

    Client|Error emptyFtpsClientEp = new (emptyFtpsConfig);
    if emptyFtpsClientEp is Error {
        test:assertTrue(emptyFtpsClientEp.message().startsWith("Error while connecting to the FTP server with URL: "),
            msg = "Unexpected error during the FTPS client initialization with no secureSocket configs. " + emptyFtpsClientEp.message());
    } else {
        test:assertFail(msg = "Found a non-error response while initializing FTPS client with no secureSocket configs.");
    }
}

@test:Config {}
public function testFtpsConnectWithInvalidKeystorePath() returns error? {
    ClientConfiguration ftpsConfig = {
        protocol: FTPS,
        host: "127.0.0.1",
        port: 21214,
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            secureSocket: {
                key: {
                    path: "tests/invalid_resources/keystore.jks", // Only change the bad part
                    password: KEYSTORE_PASSWORD
                },
                cert: {
                    path: KEYSTORE_PATH, // Use constant for the "good" part
                    password: KEYSTORE_PASSWORD
                },
                mode: EXPLICIT
            }
        }
    };

    Client|Error ftpsClientEp = new (ftpsConfig);
    if ftpsClientEp is Error {
        test:assertTrue(ftpsClientEp.message().startsWith("Error while connecting to the FTP server with URL: ") ||
            ftpsClientEp.message().includes("Failed to load KeyStore"),
            msg = "Unexpected error during the FTPS client initialization with an invalid keystore path. " + ftpsClientEp.message());
    } else {
        test:assertFail(msg = "Found a non-error response while initializing FTPS client with an invalid keystore path.");
    }
}

@test:Config {}
public function testFtpsConnectWithInvalidTruststorePath() returns error? {
    ClientConfiguration ftpsConfig = {
        protocol: FTPS,
        host: "127.0.0.1",
        port: 21214,
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            secureSocket: {
                key: {
                    path: KEYSTORE_PATH, // Use constant for the "good" part
                    password: KEYSTORE_PASSWORD
                },
                cert: {
                    path: "tests/invalid_resources/truststore.jks", // Only change the bad part
                    password: KEYSTORE_PASSWORD
                },
                mode: EXPLICIT
            }
        }
    };

    Client|Error ftpsClientEp = new (ftpsConfig);
    if ftpsClientEp is Error {
        test:assertTrue(ftpsClientEp.message().startsWith("Error while connecting to the FTP server with URL: ") ||
            ftpsClientEp.message().includes("Failed to load KeyStore"),
            msg = "Unexpected error during the FTPS client initialization with an invalid truststore path. " + ftpsClientEp.message());
    } else {
        test:assertFail(msg = "Found a non-error response while initializing FTPS client with an invalid truststore path.");
    }
}

@test:Config {}
public function testFtpsConnectWithWrongPort() returns error? {
    ClientConfiguration ftpsConfig = {
        protocol: FTPS,
        host: "127.0.0.1",
        port: 21299,
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            secureSocket: {
                key: {
                    path: KEYSTORE_PATH,
                    password: KEYSTORE_PASSWORD
                },
                cert: {
                    path: KEYSTORE_PATH,
                    password: KEYSTORE_PASSWORD
                },
                mode: EXPLICIT
            }
        }
    };

    Client|Error ftpsClientEp = new (ftpsConfig);
    if ftpsClientEp is Error {
        test:assertTrue(ftpsClientEp.message().startsWith("Error while connecting to the FTP server with URL: "),
            msg = "Unexpected error during the FTPS client initialization with an invalid port. " + ftpsClientEp.message());
        test:assertTrue(ftpsClientEp.message().length() > "Error while connecting to the FTP server with URL: ftps://wso2:***@127.0.0.1:21299".length(),
            msg = "Error message should contain detailed root cause information");
    } else {
        test:assertFail(msg = "Found a non-error response while initializing FTPS client with an invalid port.");
    }
}

@test:Config {}
public function testFtpsConnectWithInvalidHost() returns error? {
    ClientConfiguration ftpsConfig = {
        protocol: FTPS,
        host: "nonexistent.invalid.host.example",
        port: 21214,
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            secureSocket: {
                key: {
                    path: KEYSTORE_PATH,
                    password: KEYSTORE_PASSWORD
                },
                cert: {
                    path: KEYSTORE_PATH,
                    password: KEYSTORE_PASSWORD
                },
                mode: EXPLICIT
            }
        }
    };

    Client|Error ftpsClientEp = new (ftpsConfig);
    if ftpsClientEp is Error {
        test:assertTrue(ftpsClientEp.message().startsWith("Error while connecting to the FTP server with URL: "),
            msg = "Unexpected error during the FTPS client initialization with an invalid host. " + ftpsClientEp.message());
        test:assertTrue(ftpsClientEp.message().length() > "Error while connecting to the FTP server with URL: ".length(),
            msg = "Error message should contain detailed root cause information");
    } else {
        test:assertFail(msg = "Found a non-error response while initializing FTPS client with an invalid host.");
    }
}

// --- Explicit Mode Group ---
@test:Config {}
public function testFtpsExplicitPutFileContent() returns error? {
    string filePath = FTPS_CLIENT_ROOT + "/tempFtpsFile1.txt";
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);

    Error? response = (<Client>ftpsExplicitClientEp)->put(filePath, bStream);
    if response is Error {
        test:assertFail(msg = "Error in FTPS EXPLICIT `put`: " + response.message());
    }

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>ftpsExplicitClientEp)->get(filePath);
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchFtpsStreamContent(str, "Put content"));
        check str.close();
    } else {
        test:assertFail(msg = "Found unexpected response type" + str.message());
    }
}

@test:Config { dependsOn: [testFtpsExplicitPutFileContent] }
public function testFtpsExplicitGetFileContent() returns error? {
    string filePath = FTPS_CLIENT_ROOT + "/file2.txt";
    
    // Setup: Put file first
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>ftpsExplicitClientEp)->put(filePath, bStream);

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>ftpsExplicitClientEp)->get(filePath);
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchFtpsStreamContent(str, "Put content"),
            msg = "Found unexpected content from FTPS EXPLICIT `get` operation");
        check str.close();
    } else {
        test:assertFail("Found unexpected response type" + str.message());
    }
}

@test:Config { dependsOn: [testFtpsExplicitGetFileContent] }
public function testFtpsExplicitDeleteFileContent() returns error? {
    string filePath = FTPS_CLIENT_ROOT + "/tempFtpsFile1.txt";
    
    // Ensure file exists first (robustness)
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>ftpsExplicitClientEp)->put(filePath, bStream);

    Error? response = (<Client>ftpsExplicitClientEp)->delete(filePath);
    if response is Error {
        test:assertFail(msg = "Error in FTPS EXPLICIT `delete`: " + response.message());
    }

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>ftpsExplicitClientEp)->get(filePath);
    if str is stream<byte[] & readonly, io:Error?> {
        check str.close();
        test:assertFail(msg = "File was not deleted with FTPS EXPLICIT `delete` operation");
    } else {
        // Assert specific error message if possible, or just that it failed
        test:assertTrue(str.message().includes("not found"), 
            msg = "Expected 'not found' error, got: " + str.message());
    }
}

// --- Implicit Mode Group (Independent of Explicit) ---
@test:Config {}
public function testFtpsImplicitPutFileContent() returns error? {
    string filePath = FTPS_CLIENT_ROOT + "/tempFtpsFile2.txt";
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);

    Error? response = (<Client>ftpsImplicitClientEp)->put(filePath, bStream);
    if response is Error {
        test:assertFail(msg = "Error in FTPS IMPLICIT `put`: " + response.message());
    }

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>ftpsImplicitClientEp)->get(filePath);
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchFtpsStreamContent(str, "Put content"));
        check str.close();
    } else {
        test:assertFail(msg = "Found unexpected response type" + str.message());
    }
    
    check (<Client>ftpsImplicitClientEp)->delete(filePath);
}

@test:Config { dependsOn: [testFtpsImplicitPutFileContent] }
public function testFtpsImplicitGetFileContent() returns error? {
    string filePath = FTPS_CLIENT_ROOT + "/file2.txt";
    
    // Setup: Put file first using implicit client
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>ftpsImplicitClientEp)->put(filePath, bStream);

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>ftpsImplicitClientEp)->get(filePath);
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchFtpsStreamContent(str, "Put content"),
            msg = "Found unexpected content from FTPS IMPLICIT `get` operation");
        check str.close();
    } else {
        test:assertFail("Found unexpected response type" + str.message());
    }
}

// --- Data Channel Group ---
@test:Config {}
public function testFtpsDataChannelProtectionPrivate() returns error? {
    string filePath = FTPS_CLIENT_ROOT + "/tempFtpsPrivate.txt";
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);

    check (<Client>ftpsExplicitClientEp)->put(filePath, bStream);

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>ftpsExplicitClientEp)->get(filePath);
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchFtpsStreamContent(str, "Put content"));
        check str.close();
    } else {
        test:assertFail(msg = "Failed to get file with PRIVATE protection: " + str.message());
    }
    check (<Client>ftpsExplicitClientEp)->delete(filePath);
}

@test:Config { dependsOn: [testFtpsDataChannelProtectionPrivate] }
public function testFtpsDataChannelProtectionClear() returns error? {
    string filePath = FTPS_CLIENT_ROOT + "/tempFtpsClear.txt";
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);

    check (<Client>ftpsClearDataChannelClientEp)->put(filePath, bStream);

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>ftpsClearDataChannelClientEp)->get(filePath);
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchFtpsStreamContent(str, "Put content"));
        check str.close();
    } else {
        test:assertFail(msg = "Failed to get file with CLEAR protection: " + str.message());
    }
    check (<Client>ftpsClearDataChannelClientEp)->delete(filePath);
}

// --- Advanced / Flaky Group ---
@test:Config { 
    dependsOn: [testFtpsExplicitDeleteFileContent]
}
public function testFtpsFileStreamReuse() returns error? {
    string path1 = FTPS_CLIENT_ROOT + "/tempFtpsFile3.txt";
    string path2 = FTPS_CLIENT_ROOT + "/tempFtpsFile4.txt";
    
    stream<io:Block, io:Error?> localFileStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>ftpsExplicitClientEp)->put(path1, localFileStream);
    
    stream<byte[] & readonly, io:Error?> remoteFileStream = check (<Client>ftpsExplicitClientEp)->get(path1);
    check (<Client>ftpsExplicitClientEp)->put(path2, remoteFileStream);
    
    stream<byte[] & readonly, io:Error?> remoteFileStream2 = check (<Client>ftpsExplicitClientEp)->get(path2);

    test:assertTrue(check matchFtpsStreamContent(remoteFileStream2, "Put content"));
    
    check (<Client>ftpsExplicitClientEp)->delete(path1);
    check (<Client>ftpsExplicitClientEp)->delete(path2);
}

@test:Config { 
    dependsOn: [testFtpsFileStreamReuse]
}
public function testFtpsLargeFileStreamReuse() returns error? {
    runtime:sleep(2); // Give the OS a breather
    string path1 = FTPS_CLIENT_ROOT + "/tempFtpsFile5.txt";
    string path2 = FTPS_CLIENT_ROOT + "/tempFtpsFile6.txt";
    
    int i = 0;
    string nonFittingContent = "";
    while i < 1000 {
        nonFittingContent += "123456789";
        i += 1;
    }
    check (<Client>ftpsExplicitClientEp)->put(path1, nonFittingContent);
    stream<byte[] & readonly, io:Error?> remoteFileStream = check (<Client>ftpsExplicitClientEp)->get(path1);
    check (<Client>ftpsExplicitClientEp)->put(path2, remoteFileStream);
    stream<byte[] & readonly, io:Error?> remoteFileStream2 = check (<Client>ftpsExplicitClientEp)->get(path2);

    test:assertTrue(check matchFtpsStreamContent(remoteFileStream2, nonFittingContent));
    check (<Client>ftpsExplicitClientEp)->delete(path1);
    check (<Client>ftpsExplicitClientEp)->delete(path2);
}
