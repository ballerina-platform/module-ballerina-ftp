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
public enum Protocol {
    # Unsecure File Transfer Protocol
    FTP = "ftp",
    # Secure File Transfer Protocol (FTP over SSL/TLS)
    FTPS = "ftps",
    # File Transfer Protocol over SSH
    SFTP = "sftp"
}

# FTPS connection mode.
public enum FtpsMode {
    # SSL/TLS connection is established immediately upon connection.
    IMPLICIT,
    # Starts as regular FTP, then upgrades to SSL/TLS using AUTH TLS command.
    EXPLICIT
}

# FTPS data channel protection level.
# Controls whether the data channel (file transfers) is encrypted.
public enum FtpsDataChannelProtection {
    # Data channel is not encrypted (PROT C). Not recommended for security
    CLEAR,
    # Data channel is encrypted (PROT P). Recommended for secure transfers
    PRIVATE,
    # Data channel has integrity protection only (PROT S). Rarely used
    SAFE,
    # Data channel is encrypted (PROT E). Similar to PRIVATE
    CONFIDENTIAL
}

# Private key configuration for SSH-based authentication (used with SFTP).
public type PrivateKey record {|
    # Path to the private key file
    string path;
    # Password to decrypt the private key, if it is encrypted
    string password?;
|};

# Secure socket configuration for FTPS (FTP over SSL/TLS).
# Used for configuring SSL/TLS certificates and keystores for FTPS connections.
public type SecureSocket record {|
    # Keystore configuration for client authentication
    crypto:KeyStore key?;
    # Certificate configuration for server certificate validation
    crypto:TrustStore cert?;
    # FTPS connection mode.
    FtpsMode mode = EXPLICIT;
    # Data channel protection level. Controls encryption of the data channel used for file transfers.
    FtpsDataChannelProtection dataChannelProtection = PRIVATE;
|};

# Basic authentication credentials for connecting to FTP/FTPS servers using username and password.
public type Credentials record {|
    # Username for authentication
    string username;
    # Password for authentication
    string password?;
|};

# Authentication options for FTP server connections.
public type AuthConfiguration record {|
    # Username and password for basic authentication
    Credentials credentials?;
    # Private key for SSH-based authentication (used with SFTP protocol)
    PrivateKey privateKey?;
    # Secure socket configuration for SSL/TLS (used with FTPS protocol)
    SecureSocket secureSocket?;
    # Preferred authentication methods in priority order (used with SFTP protocol)
    PreferredMethod[] preferredMethods = [PUBLICKEY, PASSWORD];
|};

# Authentication methods to use when connecting to FTP/SFTP servers.
public enum PreferredMethod {
    # Keyboard interactive authentication (user prompted for credentials)
    KEYBOARD_INTERACTIVE,
    # GSSAPI with MIC (Generic Security Service Application Program Interface)
    GSSAPI_WITH_MIC,
    # Password-based authentication using username and password
    PASSWORD,
    # Public key-based authentication using SSH key pairs
    PUBLICKEY
}

# File transfer mode for FTP connections.
public enum FileTransferMode {
    # Binary mode (no conversion, suitable for all file types)
    BINARY,
    # ASCII mode (CRLF conversion for text files)
    ASCII
}

# Compression algorithms for SFTP file transfers.
public enum TransferCompression {
    # Standard ZLIB compression
    ZLIB = "zlib",
    # OpenSSH variant of ZLIB compression
    ZLIBOPENSSH = "zlib@openssh.com",
    # No compression
    NO = "none"
}

# Proxy type for SFTP connections.
public enum ProxyType {
    # HTTP CONNECT proxy
    HTTP,
    # SOCKS version 5 proxy
    SOCKS5,
    # Stream proxy (advanced usage)
    STREAM
}

# Proxy authentication credentials.
public type ProxyCredentials record {|
    # Proxy username
    string username;
    # Proxy password
    string password;
|};

# Proxy configuration for SFTP connections.
public type ProxyConfiguration record {|
    # Proxy server hostname or IP address
    string host;
    # Proxy server port number
    int port;
    # Type of proxy (HTTP, SOCKS5, or STREAM)
    ProxyType 'type = HTTP;
    # Proxy authentication credentials
    ProxyCredentials auth?;
    # Proxy command for STREAM type (SFTP jump-host), e.g., `ssh -W %h:%p jumphost`
    string command?;
|};

# Socket timeout configurations.
public type SocketConfig record {|
    # Data transfer timeout in seconds (FTP only)
    decimal ftpDataTimeout = 120.0;
    # Socket operation timeout in seconds (FTP only)
    decimal ftpSocketTimeout = 60.0;
    # SSH session timeout in seconds (SFTP only)
    decimal sftpSessionTimeout = 300.0;
|};

# Configuration for retry behavior on transient failures.
# Enables automatic retry with exponential backoff for read operations.
public type RetryConfig record {|
    # Maximum number of retry attempts
    int count = 3;
    # Initial wait time in seconds between retries
    decimal interval = 1.0;
    # Multiplier applied to the wait interval after each retry (exponential backoff)
    decimal backOffFactor = 2.0;
    # Maximum wait time cap in seconds between retries
    decimal maxWaitInterval = 30.0;
|};

# Internal configuration for content to be written in put and append operations.
public type InputContent record {|
    # Path of the file to be created or appended
    string filePath;
    # `true` if the input type is a file stream
    boolean isFile = false;
    # Content read from the input file stream
    stream<byte[] & readonly, io:Error?> fileContent?;
    # Input content as text
    string textContent?;
    # If `true`, input will be compressed before uploading
    boolean compressInput = false;
|};

# Determines the timestamp used when calculating file age for filtering.
public enum AgeCalculationMode {
    # Use file's last modified timestamp
    LAST_MODIFIED,
    # Use file's creation timestamp (where supported by file system)
    CREATION_TIME
}

# Filters files based on their age to control which files trigger listener events.
# Useful for processing only files within a specific age range (e.g., skip very new files or very old files).
public type FileAgeFilter record {|
    # Minimum age of the file in seconds (inclusive). Files younger than this are skipped.
    # If not specified, no minimum age requirement is applied
    decimal minAge?;
    # Maximum age of the file in seconds (inclusive). Files older than this are skipped.
    # If not specified, no maximum age requirement is applied
    decimal maxAge?;
    # Whether to calculate age based on last modified time or creation time
    AgeCalculationMode ageCalculationMode = LAST_MODIFIED;
|};

# Determines how to match required files when evaluating file dependencies.
# Controls whether all dependencies must be present, at least one, or a specific count.
public enum DependencyMatchingMode {
    # All required file patterns must have at least one matching file
    ALL,
    # At least one required file pattern must have a matching file
    ANY,
    # Exact number of required files must match (count specified in `requiredFileCount`)
    EXACT_COUNT
}

# Defines a dependency condition where processing of target files depends on the existence of other files.
# This allows conditional file processing based on the presence of related files (e.g., processing a data file only
# when a corresponding marker file exists). Supports capture group substitution to dynamically match related files.
public type FileDependencyCondition record {|
    # Regex pattern for files that should be processed conditionally
    string targetPattern;
    # File patterns that must exist before processing. Supports capture group substitution (e.g., `$1`)
    string[] requiredFiles;
    # How to match required files
    DependencyMatchingMode matchingMode = ALL;
    # For EXACT_COUNT mode, specifies the exact number of required files that must match
    int requiredFileCount = 1;
|};

# Categories of errors that can trip the circuit breaker.
# Used to configure which types of failures should count towards the circuit breaker threshold.
public enum FailureCategory {
    # Connection-level failures (timeout, refused, reset, DNS resolution)
    CONNECTION_ERROR,
    # Authentication failures (invalid credentials, key rejected)
    AUTHENTICATION_ERROR,
    # Transient server errors that may succeed on retry (FTP codes 421, 425, 426, 450, 451, 452)
    TRANSIENT_ERROR,
    # All errors regardless of type
    ALL_ERRORS
}

# Configuration for the sliding time window used in failure calculation.
# The rolling window divides time into discrete buckets for efficient tracking of request success/failure rates.
public type RollingWindow record {|
    # Minimum number of requests in the window before the circuit breaker evaluates the failure threshold.
    # The circuit breaker will not trip until this many requests have been made
    int requestVolumeThreshold = 10;
    # Time period in seconds for the sliding window
    decimal timeWindow = 60;
    # Granularity of time buckets in seconds. Must be less than `timeWindow`
    decimal bucketSize = 10;
|};

# Configuration for circuit breaker behavior.
# The circuit breaker prevents cascade failures by temporarily blocking requests when the server is experiencing issues.
public type CircuitBreakerConfig record {|
    # Time window configuration for failure tracking
    RollingWindow rollingWindow = {};
    # Failure ratio threshold (0.0 to 1.0) that trips the circuit open.
    # For example, 0.5 means the circuit opens when 50% of requests fail
    float failureThreshold = 0.5;
    # Seconds to wait in OPEN state before transitioning to HALF_OPEN to test recovery
    decimal resetTime = 30;
    # Error categories that count as failures. Only errors matching these categories contribute to the failure ratio
    FailureCategory[] failureCategories = [CONNECTION_ERROR, TRANSIENT_ERROR];
|};
