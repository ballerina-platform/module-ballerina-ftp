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

# How should we connect to the file server
#
# + FTP - Unsecure File Transfer Protocol
# + SFTP - FTP over SSH
public enum Protocol {
    FTP = "ftp",
    SFTP = "sftp"
}

# Configuration for using a security key file.
#
# + path - Path to the private key file
# + password - Password to unlock the security key (if it has one)
public type PrivateKey record {|
    string path;
    string password?;
|};

# Username/password authentication related configurations.
#
# + username - Username of the user
# + password - Password of the user
public type Credentials record {|
    string username;
    string password?;
|};

# How to sign in to the file server.
#
# + credentials - Username and password
# + privateKey - Security key file for SFTP
# + preferredMethods - Which sign-in methods to use for authentication
public type AuthConfiguration record {|
    Credentials credentials?;
    PrivateKey privateKey?;
    PreferredMethod[] preferredMethods = [PUBLICKEY, PASSWORD];
|};

# Ways the file server can verify your identity.
#
# + PASSWORD - Username and password
# + PUBLICKEY - Security key file
# + KEYBOARD_INTERACTIVE - Interactive sign-in (promt for username and password)
# + GSSAPI_WITH_MIC - Enterprise authentication system
public enum PreferredMethod {
    KEYBOARD_INTERACTIVE,
    GSSAPI_WITH_MIC,
    PASSWORD,
    PUBLICKEY
}

# Configuration for the input given for `put` and `append` operations of
# the FTP module.
#
# + filePath - Path of the file to be created or appended
# + isFile - `true` if the input type is a file
# + fileContent - The content read from the input file, if the input is a file
# + textContent - The input content, for other input types
# + compressInput - If true, input will be compressed before uploading
public type InputContent record {|
    string filePath;
    boolean isFile = false;
    stream<byte[] & readonly, io:Error?> fileContent?;
    string textContent?;
    boolean compressInput = false;
|};
