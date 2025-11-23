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

# Protocol to use for FTP server connections.
# Determines whether to use basic FTP (unsecure) or SFTP (secure over SSH).
# FTP - Unsecure File Transfer Protocol
# SFTP - File Transfer Protocol over SSH
public enum Protocol {
    FTP = "ftp",
    SFTP = "sftp"
}

# Private key configuration for SSH-based authentication.
#
# + path - Path to the private key file
# + password - Optional password for the private key
public type PrivateKey record {|
    string path;
    string password?;
|};

# Basic authentication credentials for connecting to FTP servers using username and password.
#
# + username - Username for authentication
# + password - Optional password for authentication
public type Credentials record {|
    string username;
    string password?;
|};

# Specifies authentication options for FTP server connections.
#
# + credentials - Username and password for basic authentication
# + privateKey - Private key and password for key-based authentication
# + preferredMethods - Preferred authentication methods
public type AuthConfiguration record {|
    Credentials credentials?;
    PrivateKey privateKey?;
    PreferredMethod[] preferredMethods = [PUBLICKEY, PASSWORD];
|};

# Authentication methods to use when connecting to FTP/SFTP servers.
#
# KEYBOARD_INTERACTIVE - Keyboard interactive authentication (user prompted for credentials)
# GSSAPI_WITH_MIC - GSSAPI with MIC (Generic Security Service Application Program Interface)
# PASSWORD - Password-based authentication using username and password
# PUBLICKEY - Public key-based authentication using SSH key pairs
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

# Determines the timestamp used when calculating file age for filtering.
#
# LAST_MODIFIED - Use file's last modified timestamp (default)
# CREATION_TIME - Use file's creation timestamp (where supported by file system)
public enum AgeCalculationMode {
    LAST_MODIFIED,
    CREATION_TIME
}

# Filters files based on their age to control which files trigger listener events.
# Useful for processing only files within a specific age range (e.g., skip very new files or very old files).
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

# Determines how to match required files when evaluating file dependencies.
# Controls whether all dependencies must be present, at least one, or a specific count.
# ALL - All required file patterns must have at least one matching file (default)
# ANY - At least one required file pattern must have a matching file
# EXACT_COUNT - Exact number of required files must match (count specified in requiredFileCount)
public enum DependencyMatchingMode {
    ALL,
    ANY,
    EXACT_COUNT
}

# Defines a dependency condition where processing of target files depends on the existence of other files.
# This allows conditional file processing based on the presence of related files (e.g., processing a data file only
# when a corresponding marker file exists). Supports capture group substitution to dynamically match related files.
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
