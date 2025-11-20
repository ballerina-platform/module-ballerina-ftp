// Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

# Protocols supported by the FTP connector.
# FTP - Unsecure File Transfer Protocol
# SFTP - FTP over SSH
public enum Protocol {
    FTP = "ftp",
    SFTP = "sftp"
}

# Configuration for a private key used in key-based authentication.
#
# + path - Path to the private key file
# + password - Optional password for the private key
public type PrivateKey record {|
    string path;
    string password?;
|};

# Credentials for basic authentication using username and password.
#
# + username - Username for authentication
# + password - Optional password for authentication
public type Credentials record {|
    string username;
    string password?;
|};

# Configuration for authenticating with an FTP server.
#
# + credentials - Username and password for basic authentication
# + privateKey - Private key and password for key-based authentication
# + preferredMethods - Preferred authentication methods
public type AuthConfiguration record {|
    Credentials credentials?;
    PrivateKey privateKey?;
    PreferredMethod[] preferredMethods = [PUBLICKEY, PASSWORD];
|};

# Authentication methods supported by the FTP connector.
# + KEYBOARD_INTERACTIVE - Keyboard interactive authentication
# + GSSAPI_WITH_MIC - GSSAPI with MIC authentication
# + PASSWORD - Password-based authentication
# + PUBLICKEY - Public key-based authentication
public enum PreferredMethod {
    KEYBOARD_INTERACTIVE,
    GSSAPI_WITH_MIC,
    PASSWORD,
    PUBLICKEY
}

# Internal configuration for content to be written in put and append operations.
#
# + filePath - Path of the file to be created or appended
# + isFile - `true` if the input type is a file stream
# + fileContent - The content read from the input file stream
# + textContent - The input content as text
# + compressInput - If `true`, input will be compressed before uploading
public type InputContent record {|
    string filePath;
    boolean isFile = false;
    stream<byte[] & readonly, io:Error?> fileContent?;
    string textContent?;
    boolean compressInput = false;
|};
