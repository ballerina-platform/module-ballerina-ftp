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

import ballerina/io;
import ballerina/test;
import ballerina/log;

// Create the config to access mock FTPS server in EXPLICIT mode
ClientConfiguration ftpsExplicitConfig = {
    protocol: FTPS,
    host: "127.0.0.1",
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
            mode: EXPLICIT,
            dataChannelProtection: PRIVATE
        }
    }
};

// Create the config to access mock FTPS server in IMPLICIT mode
ClientConfiguration ftpsImplicitConfig = {
    protocol: FTPS,
    host: "127.0.0.1",
    port: 990,
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
                path: "tests/resources/keystore.jks",
                password: "changeit"
            },
            cert: {
                path: "tests/resources/keystore.jks",
                password: "changeit"
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
    io:println("Initializing FTPS test clients");
    ftpsExplicitClientEp = check new (ftpsExplicitConfig);
    ftpsImplicitClientEp = check new (ftpsImplicitConfig);
    ftpsClearDataChannelClientEp = check new (ftpsClearDataChannelConfig);
}


@test:Config {
    dependsOn: [testRemoveDirectory]
}
public function testFtpsExplicitGetFileContent() returns error? {
    // First, put a file to ensure it exists
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    Error? putResponse = (<Client>ftpsExplicitClientEp)->put("/file2.txt", bStream);
    if putResponse is Error {
        test:assertFail(msg = "Error in FTPS EXPLICIT `put` operation for setup: " + putResponse.message());
    }

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>ftpsExplicitClientEp)->get("/file2.txt");
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchStreamContent(str, "Put content"),
            msg = "Found unexpected content from FTPS EXPLICIT `get` operation");
        io:Error? closeResult = str.close();
        if closeResult is io:Error {
            test:assertFail(msg = "Error while closing stream in FTPS EXPLICIT `get` operation." + closeResult.message());
        }
    } else {
        test:assertFail("Found unexpected response type" + str.message());
    }
}

@test:Config {
    dependsOn: [testFtpsExplicitGetFileContent]
}
public function testFtpsImplicitGetFileContent() returns error? {
    // First, put a file to ensure it exists
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    Error? putResponse = (<Client>ftpsImplicitClientEp)->put("/file2.txt", bStream);
    if putResponse is Error {
        test:assertFail(msg = "Error in FTPS IMPLICIT `put` operation for setup: " + putResponse.message());
    }

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>ftpsImplicitClientEp)->get("/file2.txt");
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchStreamContent(str, "Put content"),
            msg = "Found unexpected content from FTPS IMPLICIT `get` operation");
        io:Error? closeResult = str.close();
        if closeResult is io:Error {
            test:assertFail(msg = "Error while closing stream in FTPS IMPLICIT `get` operation." + closeResult.message());
        }
    } else {
        test:assertFail("Found unexpected response type" + str.message());
    }
}

@test:Config {
    dependsOn: [testFtpsExplicitGetFileContent]
}
public function testFtpsExplicitPutFileContent() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);

    Error? response = (<Client>ftpsExplicitClientEp)->put("/tempFtpsFile1.txt", bStream);
    if response is Error {
        test:assertFail(msg = "Error in FTPS EXPLICIT `put` operation" + response.message());
    }
    log:printInfo("Executed FTPS EXPLICIT `put` operation");

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>ftpsExplicitClientEp)->get("/tempFtpsFile1.txt");
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchStreamContent(str, "Put content"),
            msg = "Found unexpected content from FTPS EXPLICIT `get` operation after `put` operation");
        io:Error? closeResult = str.close();
        if closeResult is io:Error {
            test:assertFail(msg = "Error while closing stream in FTPS EXPLICIT `get` operation." + closeResult.message());
        }
    } else {
        test:assertFail(msg = "Found unexpected response type" + str.message());
    }
}

@test:Config {
    dependsOn: [testFtpsExplicitPutFileContent]
}
public function testFtpsImplicitPutFileContent() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);

    Error? response = (<Client>ftpsImplicitClientEp)->put("/tempFtpsFile2.txt", bStream);
    if response is Error {
        test:assertFail(msg = "Error in FTPS IMPLICIT `put` operation" + response.message());
    }
    log:printInfo("Executed FTPS IMPLICIT `put` operation");

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>ftpsImplicitClientEp)->get("/tempFtpsFile2.txt");
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchStreamContent(str, "Put content"),
            msg = "Found unexpected content from FTPS IMPLICIT `get` operation after `put` operation");
        io:Error? closeResult = str.close();
        if closeResult is io:Error {
            test:assertFail(msg = "Error while closing stream in FTPS IMPLICIT `get` operation." + closeResult.message());
        }
    } else {
        test:assertFail(msg = "Found unexpected response type" + str.message());
    }
    
    // Clean up the temp file
    check (<Client>ftpsImplicitClientEp)->delete("/tempFtpsFile2.txt");
}

@test:Config {
    dependsOn: [testFtpsExplicitPutFileContent]
}
public function testFtpsExplicitDeleteFileContent() returns error? {
    Error? response = (<Client>ftpsExplicitClientEp)->delete("/tempFtpsFile1.txt");
    if response is Error {
        test:assertFail(msg = "Error in FTPS EXPLICIT `delete` operation" + response.message());
    }
    log:printInfo("Executed FTPS EXPLICIT `delete` operation");

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>ftpsExplicitClientEp)->get("/tempFtpsFile1.txt");
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertFalse(check matchStreamContent(str, "Put content"),
            msg = "File was not deleted with FTPS EXPLICIT `delete` operation");
        io:Error? closeResult = str.close();
        if closeResult is io:Error {
            test:assertFail(msg = "Error while closing the stream in FTPS EXPLICIT `get` operation." + closeResult.message());
        }
    } else {
        test:assertEquals(str.message(),
            "Failed to read file: ftps://wso2:***@127.0.0.1:21214/tempFtpsFile1.txt not found",
            msg = "Correct error is not given when trying to get a non-existing file.");
    }
}

@test:Config {
    dependsOn: [testFtpsExplicitGetFileContent]
}
public function testFtpsDataChannelProtectionPrivate() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);

    Error? response = (<Client>ftpsExplicitClientEp)->put("/tempFtpsPrivate.txt", bStream);
    if response is Error {
        test:assertFail(msg = "Error in FTPS PUT with PRIVATE data channel protection" + response.message());
    }

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>ftpsExplicitClientEp)->get("/tempFtpsPrivate.txt");
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchStreamContent(str, "Put content"),
            msg = "Found unexpected content with PRIVATE data channel protection");
        check str.close();
    } else {
        test:assertFail(msg = "Failed to get file with PRIVATE data channel protection" + str.message());
    }
    check (<Client>ftpsExplicitClientEp)->delete("/tempFtpsPrivate.txt");
}

@test:Config {
    dependsOn: [testFtpsExplicitGetFileContent]
}
public function testFtpsDataChannelProtectionClear() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);

    Error? response = (<Client>ftpsClearDataChannelClientEp)->put("/tempFtpsClear.txt", bStream);
    if response is Error {
        test:assertFail(msg = "Error in FTPS PUT with CLEAR data channel protection" + response.message());
    }

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>ftpsClearDataChannelClientEp)->get("/tempFtpsClear.txt");
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchStreamContent(str, "Put content"),
            msg = "Found unexpected content with CLEAR data channel protection");
        check str.close();
    } else {
        test:assertFail(msg = "Failed to get file with CLEAR data channel protection" + str.message());
    }
    check (<Client>ftpsClearDataChannelClientEp)->delete("/tempFtpsClear.txt");
}

@test:Config {
    dependsOn: [testFtpsExplicitGetFileContent]
}
public function testFtpsConnectWithWrongProtocol() returns error? {
    ClientConfiguration ftpsConfig = {
        protocol: FTP,
        host: "127.0.0.1",
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

@test:Config {
    dependsOn: [testFtpsExplicitGetFileContent]
}
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

@test:Config {
    dependsOn: [testFtpsExplicitGetFileContent]
}
public function testFtpsConnectWithInvalidKeystorePath() returns error? {
    ClientConfiguration ftpsConfig = {
        protocol: FTPS,
        host: "127.0.0.1",
        port: 21214,
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            secureSocket: {
                key: {
                    path: "tests/invalid_resources/keystore.jks",
                    password: "changeit"
                },
                cert: {
                    path: "tests/resources/keystore.jks",
                    password: "changeit"
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

@test:Config {
    dependsOn: [testFtpsExplicitGetFileContent]
}
public function testFtpsConnectWithInvalidTruststorePath() returns error? {
    ClientConfiguration ftpsConfig = {
        protocol: FTPS,
        host: "127.0.0.1",
        port: 21214,
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            secureSocket: {
                key: {
                    path: "tests/resources/keystore.jks",
                    password: "changeit"
                },
                cert: {
                    path: "tests/invalid_resources/truststore.jks",
                    password: "changeit"
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

@test:Config {
    dependsOn: [testFtpsExplicitGetFileContent]
}
public function testFtpsConnectWithWrongPort() returns error? {
    ClientConfiguration ftpsConfig = {
        protocol: FTPS,
        host: "127.0.0.1",
        port: 21299,
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

@test:Config {
    dependsOn: [testFtpsExplicitGetFileContent]
}
public function testFtpsConnectWithInvalidHost() returns error? {
    ClientConfiguration ftpsConfig = {
        protocol: FTPS,
        host: "nonexistent.invalid.host.example",
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

@test:Config {
    dependsOn: [testFtpsExplicitPutFileContent]
}
public function testFtpsFileStreamReuse() returns error? {
    stream<io:Block, io:Error?> localFileStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>ftpsExplicitClientEp)->put("/tempFtpsFile3.txt", localFileStream);
    stream<byte[] & readonly, io:Error?> remoteFileStream = check (<Client>ftpsExplicitClientEp)->get("/tempFtpsFile3.txt");
    check (<Client>ftpsExplicitClientEp)->put("/tempFtpsFile4.txt", remoteFileStream);
    stream<byte[] & readonly, io:Error?> remoteFileStream2 = check (<Client>ftpsExplicitClientEp)->get("/tempFtpsFile4.txt");

    test:assertTrue(check matchStreamContent(remoteFileStream2, "Put content"));
    check (<Client>ftpsExplicitClientEp)->delete("/tempFtpsFile3.txt");
    check (<Client>ftpsExplicitClientEp)->delete("/tempFtpsFile4.txt");
}

@test:Config {
    dependsOn: [testFtpsFileStreamReuse]
}
public function testFtpsLargeFileStreamReuse() returns error? {
    int i = 0;
    string nonFittingContent = "";
    while i < 1000 {
        nonFittingContent += "123456789";
        i += 1;
    }
    check (<Client>ftpsExplicitClientEp)->put("/tempFtpsFile5.txt", nonFittingContent);
    stream<byte[] & readonly, io:Error?> remoteFileStream = check (<Client>ftpsExplicitClientEp)->get("/tempFtpsFile5.txt");
    check (<Client>ftpsExplicitClientEp)->put("/tempFtpsFile6.txt", remoteFileStream);
    stream<byte[] & readonly, io:Error?> remoteFileStream2 = check (<Client>ftpsExplicitClientEp)->get("/tempFtpsFile6.txt");

    test:assertTrue(check matchStreamContent(remoteFileStream2, nonFittingContent));
    check (<Client>ftpsExplicitClientEp)->delete("/tempFtpsFile5.txt");
    check (<Client>ftpsExplicitClientEp)->delete("/tempFtpsFile6.txt");
}

