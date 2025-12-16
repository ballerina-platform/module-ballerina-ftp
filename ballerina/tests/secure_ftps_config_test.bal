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

import ballerina/test;

// 1. Working config for Timeout/ASCII test (Uses PRIVATE)
ClientConfiguration ftpsDetailedConfig = {
    protocol: FTPS,
    host: "127.0.0.1",
    port: 21214,
    connectTimeout: 5.0,
    ftpFileTransfer: ASCII, // Hits setFileType(ASCII)
    socketConfig: {
        ftpDataTimeout: 10.0,
        ftpSocketTimeout: 5.0
    },
    auth: {
        credentials: {username: "wso2", password: "wso2123"},
        secureSocket: {
            key: {path: "tests/resources/keystore.jks", password: "changeit"},
            cert: {path: "tests/resources/keystore.jks", password: "changeit"},
            mode: EXPLICIT,
            dataChannelProtection: PRIVATE // Use supported protection
        }
    }
};

// 2. Config for "Unsupported Protection" test
ClientConfiguration ftpsUnsupportedProtectionConfig = {
    protocol: FTPS,
    host: "127.0.0.1",
    port: 21214,
    auth: {
        credentials: {username: "wso2", password: "wso2123"},
        secureSocket: {
            key: {path: "tests/resources/keystore.jks", password: "changeit"},
            cert: {path: "tests/resources/keystore.jks", password: "changeit"},
            mode: EXPLICIT,
            dataChannelProtection: SAFE // Server will reject this
        }
    }
};

@test:Config { groups: ["ftpsConfig"] }
function testFtpsWithTimeoutsAndAscii() returns error? {
    Client clientEp = check new (ftpsDetailedConfig);
    string content = "Simple ASCII Content";
    string path = "/ftps-client/ascii_timeout_test.txt";

    check clientEp->putText(path, content);

    string readContent = check clientEp->getText(path);
    test:assertEquals(readContent, content);

    check clientEp->delete(path);
}

// Negative Test: Verify client attempts to set 'S', even if server/VFS rejects it.
@test:Config { groups: ["ftpsConfig"] }
function testFtpsWithSafeProtection() {
    Client|error clientEp = new (ftpsUnsupportedProtectionConfig);
    if clientEp is error {
        // FIX: Check for the actual VFS wrapper error message
        // This confirms the Java code attempted to map 'SAFE' -> 'S'
        test:assertTrue(clientEp.message().includes("Failed to setup secure data channel level \"S\""),
            "Expected failure setting data channel level S, got: " + clientEp.message());
    } else {
        test:assertFail("Client initialized successfully with unsupported 'SAFE' protection (Unexpected)");
    }
}