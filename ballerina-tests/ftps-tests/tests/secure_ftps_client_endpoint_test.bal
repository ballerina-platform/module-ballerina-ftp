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

import ballerina/ftp;
import ballerina/io;
import ballerina/lang.'string as strings;
import ballerina/lang.runtime as runtime;
import ballerina/test;

// Constants for test configuration
const KEYSTORE_PATH = "tests/resources/keystore.jks";
const KEYSTORE_PASSWORD = "changeit";
const FTPS_CLIENT_ROOT = "/ftps-client";
const PUT_FILE_PATH = "tests/resources/datafiles/file2.txt";

// Create the config to access mock FTPS server in EXPLICIT mode
ftp:ClientConfiguration ftpsExplicitConfig = {
    protocol: ftp:FTPS,
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
            mode: ftp:EXPLICIT,
            dataChannelProtection: ftp:PRIVATE
        }
    }
};

// Create the config to access mock FTPS server in IMPLICIT mode
ftp:ClientConfiguration ftpsImplicitConfig = {
    protocol: ftp:FTPS,
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
            mode: ftp:IMPLICIT,
            dataChannelProtection: ftp:PRIVATE
        }
    }
};

// Create the config with CLEAR data channel protection for testing
ftp:ClientConfiguration ftpsClearDataChannelConfig = {
    protocol: ftp:FTPS,
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
            mode: ftp:EXPLICIT,
            dataChannelProtection: ftp:CLEAR
        }
    }
};

ftp:Client? ftpsExplicitClientEp = ();
ftp:Client? ftpsImplicitClientEp = ();
ftp:Client? ftpsClearDataChannelClientEp = ();

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
    ftp:Client clientEp = <ftp:Client>ftpsExplicitClientEp;

    string[] files = [
        "file2.txt", "tempFtpsFile1.txt", "tempFtpsFile2.txt", 
        "tempFtpsPrivate.txt", "tempFtpsClear.txt", "tempFtpsFile3.txt", 
        "tempFtpsFile4.txt", "tempFtpsFile5.txt", "tempFtpsFile6.txt"
    ];

    foreach string f in files {
        string path = FTPS_CLIENT_ROOT + "/" + f;
        
        boolean fileExists = check clientEp->exists(path);

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

//Negative Tests
@test:Config {}
public function testFtpsConnectWithWrongProtocol() returns error? {
    ftp:ClientConfiguration ftpsConfig = {
        protocol: ftp:FTP,
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
                mode: ftp:EXPLICIT
            }
        }
    };

    ftp:Client|ftp:Error ftpsClientEp = new (ftpsConfig);
    if ftpsClientEp is ftp:Error {
        test:assertTrue(ftpsClientEp.message().includes("secureSocket can only be used with FTPS protocol"));
    } else {
        test:assertFail(msg = "Should have failed with protocol mismatch");
    }
}

@test:Config {}
public function testFtpsConnectWithEmptySecureSocket() returns error? {
    ftp:ClientConfiguration emptyFtpsConfig = {
        protocol: ftp:FTPS,
        host: "127.0.0.1",
        port: 21214,
        auth: {
            credentials: {username: "wso2", password: "wso2123"}
        }
    };

    ftp:Client|ftp:Error emptyFtpsClientEp = new (emptyFtpsConfig);
    if emptyFtpsClientEp is ftp:Error {
        test:assertTrue(emptyFtpsClientEp.message().startsWith("Error while connecting to the FTP server"));
    } else {
        test:assertFail(msg = "Should have failed with missing secureSocket");
    }
}

@test:Config {}
public function testFtpsConnectWithInvalidKeystorePath() returns error? {
    ftp:ClientConfiguration ftpsConfig = {
        protocol: ftp:FTPS,
        host: "127.0.0.1",
        port: 21214,
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            secureSocket: {
                key: {path: "tests/invalid/keystore.jks", password: KEYSTORE_PASSWORD},
                cert: {path: KEYSTORE_PATH, password: KEYSTORE_PASSWORD},
                mode: ftp:EXPLICIT
            }
        }
    };

    ftp:Client|ftp:Error ftpsClientEp = new (ftpsConfig);
    if ftpsClientEp is ftp:Error {
        test:assertTrue(ftpsClientEp.message().includes("Failed to load KeyStore"));
    } else {
        test:assertFail(msg = "Should have failed with invalid keystore path");
    }
}

@test:Config {}
public function testFtpsConnectWithInvalidTruststorePath() returns error? {
    ftp:ClientConfiguration ftpsConfig = {
        protocol: ftp:FTPS,
        host: "127.0.0.1",
        port: 21214,
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            secureSocket: {
                key: {path: KEYSTORE_PATH, password: KEYSTORE_PASSWORD},
                cert: {path: "tests/invalid/truststore.jks", password: KEYSTORE_PASSWORD},
                mode: ftp:EXPLICIT
            }
        }
    };

    ftp:Client|ftp:Error ftpsClientEp = new (ftpsConfig);
    if ftpsClientEp is ftp:Error {
        test:assertTrue(ftpsClientEp.message().includes("Failed to load KeyStore"));
    } else {
        test:assertFail(msg = "Should have failed with invalid truststore path");
    }
}

@test:Config {}
public function testFtpsConnectWithWrongPort() returns error? {
    ftp:ClientConfiguration ftpsConfig = {
        protocol: ftp:FTPS,
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
                mode: ftp:EXPLICIT
            }
        }
    };

    ftp:Client|ftp:Error ftpsClientEp = new (ftpsConfig);
    if ftpsClientEp is ftp:Error {
        test:assertTrue(ftpsClientEp.message().startsWith("Error while connecting to the FTP server"));
    } else {
        test:assertFail(msg = "Should have failed with invalid port");
    }
}

@test:Config {}
public function testFtpsConnectWithInvalidHost() returns error? {
    ftp:ClientConfiguration ftpsConfig = {
        protocol: ftp:FTPS,
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
                mode: ftp:EXPLICIT
            }
        }
    };

    ftp:Client|ftp:Error ftpsClientEp = new (ftpsConfig);
    if ftpsClientEp is ftp:Error {
        test:assertTrue(ftpsClientEp.message().startsWith("Error while connecting to the FTP server"));
    } else {
        test:assertFail(msg = "Should have failed with invalid host");
    }
}

@test:Config { dependsOn: [testFtpsConnectWithInvalidHost] }
public function testFtpsClientWithoutTruststore() returns error? {
    ftp:ClientConfiguration noTrustConfig = {
        protocol: ftp:FTPS,
        host: "127.0.0.1",
        port: 21214,
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            secureSocket: {
                key: {path: KEYSTORE_PATH, password: KEYSTORE_PASSWORD},
                mode: ftp:EXPLICIT
            }
        }
    };
    ftp:Client|ftp:Error result = new (noTrustConfig);
    if result is ftp:Error {
        test:assertTrue(result.message().startsWith("Error while connecting to the FTP server"));
    }
}

@test:Config { dependsOn: [testFtpsClientWithoutTruststore] }
public function testFtpsOneWaySsl() returns error? {
    ftp:ClientConfiguration oneWayConfig = {
        protocol: ftp:FTPS,
        host: "127.0.0.1",
        port: 21214,
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            secureSocket: {
                cert: {path: KEYSTORE_PATH, password: KEYSTORE_PASSWORD},
                mode: ftp:EXPLICIT
            }
        }
    };
    ftp:Client ftpsClient = check new (oneWayConfig);
    _ = check ftpsClient->exists(FTPS_CLIENT_ROOT);
}

// --- Explicit Mode Group ---

@test:Config { dependsOn: [testFtpsOneWaySsl] }
public function testFtpsExplicitPutFileContent() returns error? {
    string filePath = FTPS_CLIENT_ROOT + "/tempFtpsFile1.txt";
    stream<io:Block, io:Error?> localStream = check io:fileReadBlocksAsStream(PUT_FILE_PATH, 5);

    ftp:Error? putResult = (<ftp:Client>ftpsExplicitClientEp)->put(filePath, localStream);
    if putResult is ftp:Error {
        test:assertFail(msg = "Error in FTPS EXPLICIT `put`: " + putResult.message());
    }

    var contentStream = (<ftp:Client>ftpsExplicitClientEp)->get(filePath);
    if contentStream is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchFtpsStreamContent(contentStream, "Put content"));
        check contentStream.close();
    } else {
        test:assertFail(msg = "Found unexpected response type: " + contentStream.message());
    }
}

@test:Config { dependsOn: [testFtpsExplicitPutFileContent] }
public function testFtpsExplicitGetFileContent() returns error? {
    string filePath = FTPS_CLIENT_ROOT + "/file2.txt";
    
    // Setup: Put file first
    stream<io:Block, io:Error?> localStream = check io:fileReadBlocksAsStream(PUT_FILE_PATH, 5);
    check (<ftp:Client>ftpsExplicitClientEp)->put(filePath, localStream);

    var contentStream = (<ftp:Client>ftpsExplicitClientEp)->get(filePath);
    if contentStream is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchFtpsStreamContent(contentStream, "Put content"),
            msg = "Found unexpected content from FTPS EXPLICIT `get` operation");
        check contentStream.close();
    } else {
        test:assertFail("Found unexpected response type: " + contentStream.message());
    }
}

@test:Config { dependsOn: [testFtpsExplicitGetFileContent] }
public function testFtpsExplicitDeleteFileContent() returns error? {
    string filePath = FTPS_CLIENT_ROOT + "/tempFtpsFile1.txt";
    
    stream<io:Block, io:Error?> localStream = check io:fileReadBlocksAsStream(PUT_FILE_PATH, 5);
    check (<ftp:Client>ftpsExplicitClientEp)->put(filePath, localStream);

    ftp:Error? deleteResult = (<ftp:Client>ftpsExplicitClientEp)->delete(filePath);
    if deleteResult is ftp:Error {
        test:assertFail(msg = "Error in FTPS EXPLICIT `delete`: " + deleteResult.message());
    }

    var contentStream = (<ftp:Client>ftpsExplicitClientEp)->get(filePath);
    if contentStream is stream<byte[] & readonly, io:Error?> {
        check contentStream.close();
        test:assertFail(msg = "File was not deleted");
    } else {
        test:assertTrue(contentStream.message().includes("not found"));
    }
}

// Implicit Mode Group
@test:Config { dependsOn: [testFtpsExplicitDeleteFileContent] }
public function testFtpsImplicitPutFileContent() returns error? {
    string filePath = FTPS_CLIENT_ROOT + "/tempFtpsFile2.txt";
    stream<io:Block, io:Error?> localStream = check io:fileReadBlocksAsStream(PUT_FILE_PATH, 5);

    ftp:Error? putResult = (<ftp:Client>ftpsImplicitClientEp)->put(filePath, localStream);
    if putResult is ftp:Error {
        test:assertFail(msg = "Error in FTPS IMPLICIT `put`: " + putResult.message());
    }

    var contentStream = (<ftp:Client>ftpsImplicitClientEp)->get(filePath);
    if contentStream is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchFtpsStreamContent(contentStream, "Put content"));
        check contentStream.close();
    } else {
        test:assertFail(msg = "Found unexpected response type: " + contentStream.message());
    }
    
    check (<ftp:Client>ftpsImplicitClientEp)->delete(filePath);
}

@test:Config { dependsOn: [testFtpsImplicitPutFileContent] }
public function testFtpsImplicitGetFileContent() returns error? {
    string filePath = FTPS_CLIENT_ROOT + "/file2.txt";
    
    // Setup: Put file first using implicit client
    stream<io:Block, io:Error?> localStream = check io:fileReadBlocksAsStream(PUT_FILE_PATH, 5);
    check (<ftp:Client>ftpsImplicitClientEp)->put(filePath, localStream);

    var contentStream = (<ftp:Client>ftpsImplicitClientEp)->get(filePath);
    if contentStream is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchFtpsStreamContent(contentStream, "Put content"),
            msg = "Found unexpected content from FTPS IMPLICIT `get` operation");
        check contentStream.close();
    } else {
        test:assertFail("Found unexpected response type: " + contentStream.message());
    }
}

@test:Config { dependsOn: [testFtpsImplicitGetFileContent] }
public function testFtpsDataChannelProtectionPrivate() returns error? {
    string filePath = FTPS_CLIENT_ROOT + "/tempFtpsPrivate.txt";
    stream<io:Block, io:Error?> localStream = check io:fileReadBlocksAsStream(PUT_FILE_PATH, 5);

    check (<ftp:Client>ftpsExplicitClientEp)->put(filePath, localStream);

    var contentStream = (<ftp:Client>ftpsExplicitClientEp)->get(filePath);
    if contentStream is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchFtpsStreamContent(contentStream, "Put content"));
        check contentStream.close();
    } else {
        test:assertFail(msg = "Failed to get file with PRIVATE protection: " + contentStream.message());
    }
    check (<ftp:Client>ftpsExplicitClientEp)->delete(filePath);
}

@test:Config { dependsOn: [testFtpsDataChannelProtectionPrivate] }
public function testFtpsDataChannelProtectionClear() returns error? {
    string filePath = FTPS_CLIENT_ROOT + "/tempFtpsClear.txt";
    stream<io:Block, io:Error?> localStream = check io:fileReadBlocksAsStream(PUT_FILE_PATH, 5);

    check (<ftp:Client>ftpsClearDataChannelClientEp)->put(filePath, localStream);

    var contentStream = (<ftp:Client>ftpsClearDataChannelClientEp)->get(filePath);
    if contentStream is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchFtpsStreamContent(contentStream, "Put content"));
        check contentStream.close();
    } else {
        test:assertFail(msg = "Failed to get file with CLEAR protection: " + contentStream.message());
    }
    check (<ftp:Client>ftpsClearDataChannelClientEp)->delete(filePath);
}

// Protection/Advanced Group
@test:Config { 
    dependsOn: [testFtpsExplicitDeleteFileContent]
}
public function testFtpsFileStreamReuse() returns error? {
    string path1 = FTPS_CLIENT_ROOT + "/tempFtpsFile3.txt";
    string path2 = FTPS_CLIENT_ROOT + "/tempFtpsFile4.txt";
    
    stream<io:Block, io:Error?> localFileStream = check io:fileReadBlocksAsStream(PUT_FILE_PATH, 5);
    check (<ftp:Client>ftpsExplicitClientEp)->put(path1, localFileStream);
    
    stream<byte[] & readonly, io:Error?> remoteFileStream = check (<ftp:Client>ftpsExplicitClientEp)->get(path1);
    check (<ftp:Client>ftpsExplicitClientEp)->put(path2, remoteFileStream);
    
    stream<byte[] & readonly, io:Error?> remoteFileStream2 = check (<ftp:Client>ftpsExplicitClientEp)->get(path2);

    test:assertTrue(check matchFtpsStreamContent(remoteFileStream2, "Put content"));
    
    check (<ftp:Client>ftpsExplicitClientEp)->delete(path1);
    check (<ftp:Client>ftpsExplicitClientEp)->delete(path2);
}

@test:Config { dependsOn: [testFtpsFileStreamReuse] }
public function testFtpsLargeFileStreamReuse() returns error? {
    runtime:sleep(2); 
    string path1 = FTPS_CLIENT_ROOT + "/tempFtpsFile5.txt";
    string path2 = FTPS_CLIENT_ROOT + "/tempFtpsFile6.txt";
    
    string content = "";
    foreach int i in 0...999 { content += "123456789"; }

    check (<ftp:Client>ftpsExplicitClientEp)->put(path1, content);
    stream<byte[] & readonly, io:Error?> remoteFileStream = check (<ftp:Client>ftpsExplicitClientEp)->get(path1);
    check (<ftp:Client>ftpsExplicitClientEp)->put(path2, remoteFileStream);
    stream<byte[] & readonly, io:Error?> remoteFileStream2 = check (<ftp:Client>ftpsExplicitClientEp)->get(path2);

    test:assertTrue(check matchFtpsStreamContent(remoteFileStream2, content));
    check (<ftp:Client>ftpsExplicitClientEp)->delete(path1);
    check (<ftp:Client>ftpsExplicitClientEp)->delete(path2);
}
