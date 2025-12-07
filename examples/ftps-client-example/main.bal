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
import ballerina/io;
import ballerina/log;

// Update these values to match your FileZilla Server configuration
string FTPS_HOST = "127.0.0.1";
int FTPS_EXPLICIT_PORT = 21;  // Explicit FTPS (FTPES) typically uses port 21
int FTPS_IMPLICIT_PORT = 990; // Implicit FTPS typically uses port 990
string FTPS_USERNAME = "testuser";  // Change to your FileZilla username
string FTPS_PASSWORD = "testpass"; // Change to your FileZilla password

// Path to your keystore/truststore files
// For FileZilla, you may not need these if using self-signed certificates
// or if you configure the truststore to accept all certificates
string KEYSTORE_PATH = "resources/keystore.jks";  // Update if needed
string KEYSTORE_PASSWORD = "changeit";  // Update if needed
string TRUSTSTORE_PATH = "resources/keystore.jks"; // Update if needed
string TRUSTSTORE_PASSWORD = "changeit"; // Update if needed

public function main() returns error? {
    log:printInfo("=== FTPS Client Example ===");
    
    // Test Explicit FTPS (FTPES)
    log:printInfo("\n1. Testing Explicit FTPS (FTPES) connection...");
    testExplicitFtps()?;
    
    // Test Implicit FTPS
    log:printInfo("\n2. Testing Implicit FTPS connection...");
    testImplicitFtps()?;
    
    log:printInfo("\n=== All tests completed ===");
}

function testExplicitFtps() returns error? {
    // Explicit FTPS (FTPES) configuration - starts as FTP then upgrades to SSL/TLS
    ftp:ClientConfiguration ftpsConfig = {
        protocol: ftp:FTPS,
        host: FTPS_HOST,
        port: FTPS_EXPLICIT_PORT,
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
                mode: ftp:EXPLICIT  // Explicit FTPS mode
            }
        }
    };
    
    ftp:Client|ftp:Error ftpsClient = new (ftpsConfig);
    if ftpsClient is ftp:Error {
        log:printError("Failed to create FTPS explicit client: " + ftpsClient.message());
        return ftpsClient;
    }
    
    // Test listing files
    log:printInfo("Listing files on server...");
    ftp:FileInfo[]|ftp:Error fileList = ftpsClient->list("/");
    if fileList is ftp:Error {
        log:printError("Failed to list files: " + fileList.message());
        return fileList;
    }
    log:printInfo("Found " + fileList.length().toString() + " items on server");
    
    // Test uploading a file
    string testContent = "Hello from Ballerina FTPS Explicit Test!";
    string testFilePath = "/test_explicit.txt";
    log:printInfo("Uploading test file...");
    ftp:Error? putResult = ftpsClient->putText(testFilePath, testContent);
    if putResult is ftp:Error {
        log:printError("Failed to upload file: " + putResult.message());
        return putResult;
    }
    log:printInfo("Successfully uploaded file via FTPS explicit");
    
    // Test downloading the file
    log:printInfo("Downloading test file...");
    string|ftp:Error getResult = ftpsClient->getText(testFilePath);
    if getResult is ftp:Error {
        log:printError("Failed to download file: " + getResult.message());
        return getResult;
    }
    log:printInfo("File content: " + getResult);
    
    // Clean up
    ftp:Error? deleteResult = ftpsClient->delete(testFilePath);
    if deleteResult is ftp:Error {
        log:printWarn("Failed to delete test file: " + deleteResult.message());
    } else {
        log:printInfo("Test file deleted successfully");
    }
}

function testImplicitFtps() returns error? {
    // Implicit FTPS configuration - SSL/TLS from the start
    ftp:ClientConfiguration ftpsConfig = {
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
    
    ftp:Client|ftp:Error ftpsClient = new (ftpsConfig);
    if ftpsClient is ftp:Error {
        log:printError("Failed to create FTPS implicit client: " + ftpsClient.message());
        return ftpsClient;
    }
    
    // Test listing files
    log:printInfo("Listing files on server...");
    ftp:FileInfo[]|ftp:Error fileList = ftpsClient->list("/");
    if fileList is ftp:Error {
        log:printError("Failed to list files: " + fileList.message());
        return fileList;
    }
    log:printInfo("Found " + fileList.length().toString() + " items on server");
    
    // Test uploading a file
    string testContent = "Hello from Ballerina FTPS Implicit Test!";
    string testFilePath = "/test_implicit.txt";
    log:printInfo("Uploading test file...");
    ftp:Error? putResult = ftpsClient->putText(testFilePath, testContent);
    if putResult is ftp:Error {
        log:printError("Failed to upload file: " + putResult.message());
        return putResult;
    }
    log:printInfo("Successfully uploaded file via FTPS implicit");
    
    // Test downloading the file
    log:printInfo("Downloading test file...");
    string|ftp:Error getResult = ftpsClient->getText(testFilePath);
    if getResult is ftp:Error {
        log:printError("Failed to download file: " + getResult.message());
        return getResult;
    }
    log:printInfo("File content: " + getResult);
    
    // Clean up
    ftp:Error? deleteResult = ftpsClient->delete(testFilePath);
    if deleteResult is ftp:Error {
        log:printWarn("Failed to delete test file: " + deleteResult.message());
    } else {
        log:printInfo("Test file deleted successfully");
    }
}

