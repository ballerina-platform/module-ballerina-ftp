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

import ballerina/io;
import ballerina/test;
import ballerina/log;

@test:Config {
    dependsOn: [testRemoveDirectory]
}
public function testSecureGetFileContent() returns error? {
    stream<byte[] & readonly, io:Error?>|Error str = (<Client>sftpClientEp)->get("/file2.txt");
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchStreamContent(str, "Put content"),
            msg = "Found unexpected content from secure `get` operation");
        io:Error? closeResult = str.close();
        if closeResult is io:Error {
            test:assertFail(msg = "Error while closing stream in `get` operation." + closeResult.message());
        }
    } else {
        test:assertFail("Found unexpected response type" + str.message());
    }
}

@test:Config {
    dependsOn: [testSecureGetFileContent]
}
public function testSecureConnectWithWrongProtocol() returns error? {
    ClientConfiguration sftpConfig = {
        protocol: FTP,
        host: "127.0.0.1",
        port: 21213,
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            privateKey: {
                path: "tests/resources/sftp.private.key",
                password: "changeit"
            }
        }
    };

    Client|Error sftpClientEp = new (sftpConfig);
    if sftpClientEp is Error {
        test:assertTrue(sftpClientEp.message().startsWith("Error while connecting to the FTP server with URL: "),
            msg = "Unexpected error during the FTP client initialization with a SFTP server. " + sftpClientEp.message());
    } else {
        test:assertFail(msg = "Found a non-error response while initializing FTP client with a SFTP server.");
    }
}

@test:Config {
    dependsOn: [testSecureGetFileContent]
}
public function testSecureConnectWithEmptySecurityConfigs() returns error? {
    ClientConfiguration emptySftpConfig = {
        protocol: SFTP,
        host: "127.0.0.1",
        port: 21213
    };

    Client|Error emptySftpClientEp = new (emptySftpConfig);
    if emptySftpClientEp is Error {
        test:assertTrue(emptySftpClientEp.message().startsWith("Error while connecting to the FTP server with URL: "),
            msg = "Unexpected error during the SFTP client initialization with no security configs. " + emptySftpClientEp.message());
    } else {
        test:assertFail(msg = "Found a non-error response while initializing SFTP client with no security configs.");
    }
}

@test:Config {
    dependsOn: [testSecureGetFileContent]
}
public function testSecureConnectWithEmptyCredentialsConfig() returns error? {
    ClientConfiguration sftpClientConfig = {
        protocol: SFTP,
        host: "127.0.0.1",
        port: 21213,
        auth: {
            privateKey: {
                path: "tests/resources/sftp.private.key",
                password: "changeit"
            }
        }
    };

    Client|Error sftpClientEp = new (sftpClientConfig);
    if sftpClientEp is Error {
        test:assertTrue(sftpClientEp.message().startsWith("Error while connecting to the FTP server with URL: "),
            msg = "Unexpected error during the SFTP client initialization with no credentials. " + sftpClientEp.message());
    } else {
        test:assertFail(msg = "Found a non-error response while initializing SFTP client with no credentials.");
    }
}

@test:Config {
    dependsOn: [testSecureGetFileContent]
}
public function testSecureConnectWithWrongPassword() returns error? {
    ClientConfiguration incorrectSftpConfig = {
        protocol: SFTP,
        host: "127.0.0.1",
        port: 21213,
        auth: {
            credentials: {username: "wso2", password: "wrongPassword"},
            privateKey: {
                path: "tests/resources/sftp.private.key",
                password: "changeit"
            }
        }
    };

    Client|Error incorrectSftpClientEp = new (incorrectSftpConfig);
    if incorrectSftpClientEp is Error {
        test:assertTrue(incorrectSftpClientEp.message().startsWith("Error while connecting to the FTP server with URL: "),
            msg = "Unexpected error during the SFTP client initialization with wrong password. " + incorrectSftpClientEp.message());
    } else {
        test:assertFail(msg = "Found a non-error response while initializing SFTP client with wrong password.");
    }
}

@test:Config {
    dependsOn: [testSecureGetFileContent]
}
public function testSecureConnectWithWrongUsername() returns error? {
    ClientConfiguration sftpConfig = {
        protocol: SFTP,
        host: "127.0.0.1",
        port: 21213,
        auth: {
            credentials: {username: "ballerina", password: "wso2123"},
            privateKey: {
                path: "tests/resources/sftp.private.key",
                password: "changeit"
            }
        }
    };

    Client|Error sftpClientEp = new (sftpConfig);
    if sftpClientEp is Error {
        test:assertTrue(sftpClientEp.message().startsWith("Error while connecting to the FTP server with URL: "),
            msg = "Unexpected error during the SFTP client initialization with wrong username. " + sftpClientEp.message());
    } else {
        test:assertFail(msg = "Found a non-error response while initializing SFTP client with wrong username.");
    }
}

@test:Config {
    dependsOn: [testSecureGetFileContent]
}
public function testSecureConnectWithEmptyUsername() returns error? {
    ClientConfiguration sftpConfig = {
        protocol: SFTP,
        host: "127.0.0.1",
        port: 21213,
        auth: {
            credentials: {username: "", password: "wso2123"},
            privateKey: {
                path: "tests/resources/sftp.private.key",
                password: "changeit"
            }
        }
    };

    Client|Error sftpClientEp = new (sftpConfig);
    if sftpClientEp is Error {
        test:assertTrue(sftpClientEp.message().startsWith("Error while connecting to the FTP server with URL: "),
            msg = "Unexpected error during the SFTP client initialization with empty username. " + sftpClientEp.message());
    } else {
        test:assertFail(msg = "Found a non-error response while initializing SFTP client with empty username.");
    }
}

@test:Config {
    dependsOn: [testSecureGetFileContent]
}
public function testSecureConnectWithEmptyPassword() returns error? {
    ClientConfiguration sftpConfig = {
        protocol: SFTP,
        host: "127.0.0.1",
        port: 21213,
        auth: {
            credentials: {username: "wso2", password: ""},
            privateKey: {
                path: "tests/resources/sftp.private.key",
                password: "changeit"
            }
        }
    };

    Client|Error sftpClientEp = new (sftpConfig);
    if sftpClientEp is Error {
        test:assertTrue(sftpClientEp.message().startsWith("Error while connecting to the FTP server with URL: "),
            msg = "Unexpected error during the SFTP client initialization with empty password. " + sftpClientEp.message());
    } else {
        test:assertFail(msg = "Found a non-error response while initializing SFTP client with empty password.");
    }
}

@test:Config {
    dependsOn: [testSecureConnectWithWrongPassword]
}
public function testSecureConnectWithWrongKey() returns error? {
    ClientConfiguration incorrectSftpConfig = {
        protocol: SFTP,
        host: "127.0.0.1",
        port: 21213,
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            privateKey: {
                path: "tests/resources/sftp.wrong.private.key",
                password: "changeit"
            }
        }
    };

    Client|Error incorrectSftpClientEp = new (incorrectSftpConfig);
    if incorrectSftpClientEp is Error {
        test:assertTrue(incorrectSftpClientEp.message().startsWith("Error while connecting to the FTP server with URL: "),
            msg = "Unexpected error during the SFTP client initialization with an invalid key. " + incorrectSftpClientEp.message());
    } else {
        test:assertFail(msg = "Found a non-error response while initializing SFTP client with an invalid key.");
    }
}

@test:Config {
    dependsOn: [testSecureConnectWithWrongPassword]
}
public function testSecureConnectWithInvalidKeyPath() returns error? {
    ClientConfiguration sftpConfig = {
        protocol: SFTP,
        host: "127.0.0.1",
        port: 21213,
        auth: {
            credentials: {username: "wso2", password: "wso2123"},
            privateKey: {
                path: "tests/invalid_resources/sftp.wrong.private.key",
                password: "changeit"
            }
        }
    };
    Client|Error sftpClientEp = new (sftpConfig);
    if sftpClientEp is Error {
        test:assertTrue(sftpClientEp.message().startsWith("Error while connecting to the FTP server with URL: "),
            msg = "Unexpected error during the SFTP client initialization with an invalid key path. " + sftpClientEp.message());
    } else {
        test:assertFail(msg = "Found a non-error response while initializing SFTP client with an invalid key path.");
    }
}

@test:Config {
    dependsOn: [testSecureGetFileContent]
}
public function testSecurePutFileContent() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);

    Error? response = (<Client>sftpClientEp)->put("/tempFile1.txt", bStream);
    if response is Error {
        test:assertFail(msg = "Error in secure `put` operation" + response.message());
    }
    log:printInfo("Executed secure `put` operation");

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>sftpClientEp)->get("/tempFile1.txt");
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchStreamContent(str, "Put content"),
            msg = "Found unexpected content from secure `get` operation after `put` operation");
        io:Error? closeResult = str.close();
        if closeResult is io:Error {
            test:assertFail(msg = "Error while closing stream in secure `get` operation." + closeResult.message());
        }
    } else {
        test:assertFail(msg = "Found unexpected response type" + str.message());
    }
}

@test:Config {
    dependsOn: [testSecurePutFileContent]
}
public function testSecureDeleteFileContent() returns error? {
    Error? response = (<Client>sftpClientEp)->delete("/tempFile1.txt");
    if response is Error {
        test:assertFail(msg = "Error in secure `delete` operation" + response.message());
    }
    log:printInfo("Executed secure `delete` operation");

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>sftpClientEp)->get("/tempFile1.txt");
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertFalse(check matchStreamContent(str, "Put content"),
            msg = "File was not deleted with secure `delete` operation");
        io:Error? closeResult = str.close();
        if closeResult is io:Error {
            test:assertFail(msg = "Error while closing the stream in secure `get` operation." + closeResult.message());
        }
    } else {
        test:assertEquals(str.message(),
            "Failed to read file: sftp://wso2:wso2123@127.0.0.1:21213/tempFile1.txt not found",
            msg = "Correct error is not given when trying to get a non-existing file.");
    }
}
