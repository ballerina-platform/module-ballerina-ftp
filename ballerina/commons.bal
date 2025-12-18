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
import ballerina/crypto;

# Protocol to use for FTP server connections.
# Determines whether to use basic FTP, FTPS, or SFTP.
# FTP - Unsecure File Transfer Protocol
# FTPS - Secure File Transfer Protocol (FTP over SSL/TLS)
# SFTP - File Transfer Protocol over SSH
public enum Protocol {
    FTP = "ftp",
    FTPS = "ftps",
    SFTP = "sftp"
}

# FTPS connection mode.
# IMPLICIT - SSL/TLS connection is established immediately upon connection (typically port 990)
# EXPLICIT - Starts as regular FTP, then upgrades to SSL/TLS using AUTH TLS command (typically port 21)
public enum FtpsMode {
    IMPLICIT,
    EXPLICIT
}

# FTPS data channel protection level.
# Controls whether the data channel (file transfers) is encrypted.
# CLEAR - Data channel is not encrypted (PROT C). Not recommended for security.
# PRIVATE - Data channel is encrypted (PROT P). Recommended for secure transfers.
# SAFE - Data channel has integrity protection only (PROT S). Rarely used.
# CONFIDENTIAL - Data channel is encrypted (PROT E). Similar to PRIVATE.
public enum FtpsDataChannelProtection {
    CLEAR,
    PRIVATE,
    SAFE,
    CONFIDENTIAL
}

# Private key configuration for SSH-based authentication (used with SFTP).
#
# + path - Path to the private key file
# + password - Optional password for the private key
public type PrivateKey record {|
    string path;
    string password?;
|};

# Secure socket configuration for FTPS (FTP over SSL/TLS).
# Used for configuring SSL/TLS certificates and keystores for FTPS connections.
#
# + key - Keystore configuration for client authentication
# + cert - Certificate configuration for server certificate validation
# + mode - FTPS connection mode (IMPLICIT or EXPLICIT). Defaults to EXPLICIT if not specified.
# + dataChannelProtection - Data channel protection level (CLEAR, PRIVATE, SAFE, or CONFIDENTIAL).
#                           Controls encryption of the data channel used for file transfers.
#                           Defaults to PRIVATE (encrypted) for secure transfers.
public type SecureSocket record {|
    crypto:KeyStore key?;
    crypto:TrustStore cert?;
    FtpsMode mode = EXPLICIT;
    FtpsDataChannelProtection dataChannelProtection = PRIVATE;
|};

# Basic authentication credentials for connecting to FTP/FTPS servers using username and password.
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
# + privateKey - Private key and password for SSH-based authentication (used with SFTP protocol)
# + secureSocket - Secure socket configuration for SSL/TLS (used with FTPS protocol)
# + preferredMethods - Preferred authentication methods (used with SFTP protocol)
public type AuthConfiguration record {|
    Credentials credentials?;
    PrivateKey privateKey?;
    SecureSocket secureSocket?;
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

# File transfer mode
#
# + BINARY - Binary mode (no conversion, suitable for all file types)
# + ASCII - ASCII mode (CRLF conversion for text files)
public enum FileTransferMode {
    BINARY,
    ASCII
}

# Compression algorithms for SFTP file transfers.
# Specifies which compression method to apply during data transfer.
#
# + ZLIB - Standard ZLIB compression
# + ZLIBOPENSSH - OpenSSH variant of ZLIB compression
# + NO - No compression
public enum TransferCompression {
    ZLIB = "zlib",
    ZLIBOPENSSH = "zlib@openssh.com",
    NO = "none"
}

# Proxy type for SFTP connections
#
# + HTTP - HTTP CONNECT proxy
# + SOCKS5 - SOCKS version 5 proxy
# + STREAM - Stream proxy (advanced usage)
public enum ProxyType {
    HTTP,
    SOCKS5,
    STREAM
}

# Proxy authentication credentials
#
# + username - Proxy username
# + password - Proxy password
public type ProxyCredentials record {|
    string username;
    string password;
|};

# Proxy configuration for SFTP connections
#
# + host - Proxy server hostname or IP address
# + port - Proxy server port number
# + type - Type of proxy (HTTP, SOCKS5, or STREAM)
# + auth - Optional proxy authentication credentials
# + command - STREAM-only: proxy command (SFTP jump-host), e.g., "ssh -W %h:%p jumphost"
public type ProxyConfiguration record {|
    string host;
    int port;
    ProxyType 'type = HTTP;
    ProxyCredentials auth?;
    string command?;
|};

# Socket timeout configurations
#
# + ftpDataTimeout - Data transfer timeout in seconds (FTP only, default: 120.0)
# + ftpSocketTimeout - Socket operation timeout in seconds (FTP only, default: 60.0)
# + sftpSessionTimeout - SSH session timeout in seconds (SFTP only, default: 300.0)
public type SocketConfig record {|
    decimal ftpDataTimeout = 120.0;
    decimal ftpSocketTimeout = 60.0;
    decimal sftpSessionTimeout = 300.0;
|};

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
