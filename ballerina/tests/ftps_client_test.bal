// Copyright (c) 2024 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/ftp;
import ballerina/test;
import ballerina/log;
import ballerina/crypto;

// ============================================
// CONFIGURE THESE VALUES FOR YOUR FILEZILLA SERVER
// ============================================
// Update these values to match your FileZilla Server configuration
string FTPS_HOST = "127.0.0.1";
int FTPS_EXPLICIT_PORT = 21;  // Explicit FTPS (FTPES) typically uses port 21
int FTPS_IMPLICIT_PORT = 990; // Implicit FTPS typically uses port 990
string FTPS_USERNAME = "testuser";  // Change to your FileZilla username
string FTPS_PASSWORD = "testpass"; // Change to your FileZilla password

// Path to your keystore/truststore files
// For testing, you might need to create these or use FileZilla's certificates
string KEYSTORE_PATH = "tests/resources/keystore.jks";  // Update if needed
string KEYSTORE_PASSWORD = "changeit";  // Update if needed
string TRUSTSTORE_PATH = "tests/resources/keystore.jks"; // Update if needed
string TRUSTSTORE_PASSWORD = "changeit"; // Update if needed

// ============================================
// FTPS CLIENT CONFIGURATIONS
// ============================================

// Explicit FTPS (FTPES) configuration - starts as FTP then upgrades to SSL/TLS
ftp:ClientConfiguration ftpsExplicitConfig = {
    protocol: ftp:FTPS,
    host: FTPS_HOST,
    port: FTPS_EXPLICIT_PORT,
    auth: {
        credentials: {
            username: FTPS_USERNAME,
            password: FTPS_PASSWORD
        },
        secureSocket: {
            // Note: For FileZilla testing, you may need to configure keystore/truststore
            // based on your server's certificate setup
            key: {
                path: KEYSTORE_PATH,
                password: KEYSTORE_PASSWORD
            },
            trustStore: {
                path: TRUSTSTORE_PATH,
                password: TRUSTSTORE_PASSWORD
            },
            mode: ftp:EXPLICIT  // Explicit FTPS mode
        }
    }
};

// Implicit FTPS configuration - SSL/TLS from the start
ftp:ClientConfiguration ftpsImplicitConfig = {
    protocol: ftp:FTPS,
    host: FTPS_HOST,
    port: FTPS_IMPLICIT_PORT,
    auth: {
        credentials: {
            username: FTPS_USERNAME,
            password: FTPS_PASSWORD
        },
        secureSocket: {
            key: {
                path: KEYSTORE_PATH,
                password: KEYSTORE_PASSWORD
            },
            trustStore: {
                path: TRUSTSTORE_PATH,
                password: TRUSTSTORE_PASSWORD
            },
            mode: ftp:IMPLICIT  // Implicit FTPS mode
        }
    }
};

// ============================================
// TEST FUNCTIONS
// ============================================

@test:Config {
    // Skip this test by default - uncomment to run
    // groups: ["ftps"]
}
public function testFtpsExplicitConnection() returns error? {
    log:printInfo("Testing FTPS Explicit (FTPES) connection...");
    
    ftp:Client|ftp:Error ftpsClient = new (ftpsExplicitConfig);
    if ftpsClient is ftp:Error {
        log:printError("Failed to create FTPS explicit client: " + ftpsClient.message());
        test:assertFail("Failed to create FTPS explicit client: " + ftpsClient.message());
        return;
    }
    
    // Test basic operation - list files
    ftp:FileInfo[]|ftp:Error fileList = ftpsClient->list("/");
    if fileList is ftp:Error {
        log:printError("Failed to list files: " + fileList.message());
        test:assertFail("Failed to list files: " + fileList.message());
    } else {
        log:printInfo("Successfully connected to FTPS explicit server. Found " + fileList.length().toString() + " items.");
        test:assertTrue(true, "FTPS explicit connection successful");
    }
}

@test:Config {
    // Skip this test by default - uncomment to run
    // groups: ["ftps"]
}
public function testFtpsImplicitConnection() returns error? {
    log:printInfo("Testing FTPS Implicit connection...");
    
    ftp:Client|ftp:Error ftpsClient = new (ftpsImplicitConfig);
    if ftpsClient is ftp:Error {
        log:printError("Failed to create FTPS implicit client: " + ftpsClient.message());
        test:assertFail("Failed to create FTPS implicit client: " + ftpsClient.message());
        return;
    }
    
    // Test basic operation - list files
    ftp:FileInfo[]|ftp:Error fileList = ftpsClient->list("/");
    if fileList is ftp:Error {
        log:printError("Failed to list files: " + fileList.message());
        test:assertFail("Failed to list files: " + fileList.message());
    } else {
        log:printInfo("Successfully connected to FTPS implicit server. Found " + fileList.length().toString() + " items.");
        test:assertTrue(true, "FTPS implicit connection successful");
    }
}

@test:Config {
    // Skip this test by default - uncomment to run
    // groups: ["ftps"]
}
public function testFtpsExplicitPutAndGet() returns error? {
    log:printInfo("Testing FTPS Explicit PUT and GET operations...");
    
    ftp:Client|ftp:Error ftpsClient = new (ftpsExplicitConfig);
    if ftpsClient is ftp:Error {
        test:assertFail("Failed to create FTPS explicit client: " + ftpsClient.message());
        return;
    }
    
    string testContent = "Hello from Ballerina FTPS Explicit Test!";
    string testFilePath = "/test_ftps_explicit.txt";
    
    // Put a file
    ftp:Error? putResult = ftpsClient->putText(testFilePath, testContent);
    if putResult is ftp:Error {
        test:assertFail("Failed to put file: " + putResult.message());
        return;
    }
    log:printInfo("Successfully uploaded file via FTPS explicit");
    
    // Get the file back
    string|ftp:Error getResult = ftpsClient->getText(testFilePath);
    if getResult is ftp:Error {
        test:assertFail("Failed to get file: " + getResult.message());
        return;
    }
    
    test:assertEquals(getResult, testContent, "File content should match");
    log:printInfo("Successfully retrieved file via FTPS explicit");
    
    // Clean up
    ftp:Error? deleteResult = ftpsClient->delete(testFilePath);
    if deleteResult is ftp:Error {
        log:printWarn("Failed to delete test file: " + deleteResult.message());
    }
}

@test:Config {
    // Skip this test by default - uncomment to run
    // groups: ["ftps"]
}
public function testFtpsImplicitPutAndGet() returns error? {
    log:printInfo("Testing FTPS Implicit PUT and GET operations...");
    
    ftp:Client|ftp:Error ftpsClient = new (ftpsImplicitConfig);
    if ftpsClient is ftp:Error {
        test:assertFail("Failed to create FTPS implicit client: " + ftpsClient.message());
        return;
    }
    
    string testContent = "Hello from Ballerina FTPS Implicit Test!";
    string testFilePath = "/test_ftps_implicit.txt";
    
    // Put a file
    ftp:Error? putResult = ftpsClient->putText(testFilePath, testContent);
    if putResult is ftp:Error {
        test:assertFail("Failed to put file: " + putResult.message());
        return;
    }
    log:printInfo("Successfully uploaded file via FTPS implicit");
    
    // Get the file back
    string|ftp:Error getResult = ftpsClient->getText(testFilePath);
    if getResult is ftp:Error {
        test:assertFail("Failed to get file: " + getResult.message());
        return;
    }
    
    test:assertEquals(getResult, testContent, "File content should match");
    log:printInfo("Successfully retrieved file via FTPS implicit");
    
    // Clean up
    ftp:Error? deleteResult = ftpsClient->delete(testFilePath);
    if deleteResult is ftp:Error {
        log:printWarn("Failed to delete test file: " + deleteResult.message());
    }
}

@test:Config {
    // Skip this test by default - uncomment to run
    // groups: ["ftps"]
}
public function testFtpsModeValidation() returns error? {
    log:printInfo("Testing FTPS mode validation...");
    
    // Test that privateKey cannot be used with FTPS
    ftp:ClientConfiguration invalidConfig = {
        protocol: ftp:FTPS,
        host: FTPS_HOST,
        port: FTPS_EXPLICIT_PORT,
        auth: {
            credentials: {
                username: FTPS_USERNAME,
                password: FTPS_PASSWORD
            },
            privateKey: {
                path: "tests/resources/sftp.private.key",
                password: "changeit"
            }
        }
    };
    
    ftp:Client|ftp:Error ftpsClient = new (invalidConfig);
    if ftpsClient is ftp:Error {
        test:assertTrue(ftpsClient.message().includes("privateKey can only be used with SFTP"),
            "Should reject privateKey with FTPS protocol");
        log:printInfo("Correctly rejected invalid configuration");
    } else {
        test:assertFail("Should have rejected privateKey with FTPS protocol");
    }
    
    // Test that secureSocket cannot be used with SFTP
    ftp:ClientConfiguration invalidSftpConfig = {
        protocol: ftp:SFTP,
        host: FTPS_HOST,
        port: 22,
        auth: {
            credentials: {
                username: FTPS_USERNAME,
                password: FTPS_PASSWORD
            },
            secureSocket: {
                key: {
                    path: KEYSTORE_PATH,
                    password: KEYSTORE_PASSWORD
                },
                mode: ftp:EXPLICIT
            }
        }
    };
    
    ftp:Client|ftp:Error sftpClient = new (invalidSftpConfig);
    if sftpClient is ftp:Error {
        test:assertTrue(sftpClient.message().includes("secureSocket can only be used with FTPS"),
            "Should reject secureSocket with SFTP protocol");
        log:printInfo("Correctly rejected secureSocket with SFTP protocol");
    } else {
        test:assertFail("Should have rejected secureSocket with SFTP protocol");
    }
}

