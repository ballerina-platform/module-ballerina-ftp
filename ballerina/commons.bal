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

# Mode for calculating file age.
#
# + LAST_MODIFIED - Use file's last modified timestamp (default)
# + CREATION_TIME - Use file's creation timestamp (where supported by file system)
public enum AgeCalculationMode {
    LAST_MODIFIED,
    CREATION_TIME
}

# Configuration for file age filtering.
#
# + minAge - Minimum age of file in seconds since last modification/creation (inclusive).
#            Files younger than this will be skipped. If not specified, no minimum age requirement.
# + maxAge - Maximum age of file in seconds since last modification/creation (inclusive).
#            Files older than this will be skipped. If not specified, no maximum age requirement.
# + ageCalculationMode - Whether to calculate age based on last modified time or creation time
public type FileAgeFilter record {|
    decimal minAge?;
    decimal maxAge?;
    AgeCalculationMode ageCalculationMode = LAST_MODIFIED;
|};

# How required dependency files should be matched.
#
# + ALL - All required file patterns must have at least one matching file (default)
# + ANY - At least one required file pattern must have a matching file
# + EXACT_COUNT - Exact number of required files must match (count specified in requiredFileCount)
public enum DependencyMatchingMode {
    ALL,
    ANY,
    EXACT_COUNT
}

# Represents a dependency condition where processing of target files depends on existence of other files.
#
# + targetPattern - Regex pattern for files that should be processed conditionally
# + requiredFiles - Array of file patterns that must exist. Supports capture group substitution (e.g., "$1")
# + matchingMode - How to match required files (ALL, ANY, or EXACT_COUNT)
# + requiredFileCount - For EXACT_COUNT mode, specifies the exact number of required files
public type FileDependencyCondition record {|
    string targetPattern;
    string[] requiredFiles;
    DependencyMatchingMode matchingMode = ALL;
    int requiredFileCount = 1;
|};
