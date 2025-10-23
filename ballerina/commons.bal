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

# Represents the set of protocols supported by the FTP listener and client.
#
# + FTP - Unsecure File Transfer Protocol
# + SFTP - FTP over SSH
public enum Protocol {
    FTP = "ftp",
    SFTP = "sftp"
}

# Configuration to read a privte key.
#
# + path - Path to the private key file
# + password - Private key password
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
    string password;
|};

# Configurations for facilitating secure communication with a remote
# FTP server.
#
# + credentials - Username and password to be used
# + privateKey - Private key to be used
# + preferredMethods - Preferred authentication methods
public type AuthConfiguration record {|
    Credentials credentials?;
    PrivateKey privateKey?;
    PreferredMethod[] preferredMethods = [PUBLICKEY, PASSWORD];
|};

# Authentication methods for the FTP listener.
# 
# + KEYBOARD_INTERACTIVE - Keyboard interactive authentication
# + GSSAPI_WITH_MIC - GSSAPI with MIC authentication
# + PASSWORD - Password authentication
# + PUBLICKEY - Public key authentication
public enum PreferredMethod {
    KEYBOARD_INTERACTIVE,
    GSSAPI_WITH_MIC,
    PASSWORD,
    PUBLICKEY
}

# FTP file transfer type
#
# + BINARY - Binary mode (no conversion, suitable for all file types)
# + ASCII - ASCII mode (CRLF conversion for text files)
public enum FtpFileType {
    BINARY,
    ASCII
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
