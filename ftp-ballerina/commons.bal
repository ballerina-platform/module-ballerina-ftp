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

# Represents the set of protocols supported by the FTP listener and client
public type Protocol "ftp"|"sftp"|"ftps";

# Underlying communication happens using FTP
public const FTP = "ftp";
# Underlying communication happens using SFTP
public const SFTP = "sftp";
# Underlying communication happens using FTPS
public const FTPS = "ftps";

# A record for providing `Truststore` related configurations
#
# + path - Path to the truststore file
# + password - Truststore password
public type TrustStore record {|
    string? path = ();
    string? password = ();
|};

# A record for providing `Keystore` related configurations
#
# + path - Path to the keystore file
# + password - Keystore password
public type KeyStore record {|
    string? path = ();
    string? password = ();
|};

# A record for providing `PrivateKey` related configurations
#
# + path - Path to the private key file
# + password - Private key password
public type PrivateKey record {|
    string? path = ();
    string? password = ();
|};

# A record for providing `BasicAuth` related configurations
#
# + username - Username of the user
# + password - Password of the user
public type BasicAuth record {|
    string username;
    string password;
|};

# Provides configurations for facilitating secure communication with a remote FTP server
#
# + trustStore - Configures the truststore to be used
# + keyStore - Configures the keystore to be used
# + basicAuth - Configure username/password to be used
# + privateKey - Configures the private key to be used
public type SecureSocket record {|
    TrustStore? trustStore = ();
    KeyStore? keyStore = ();
    BasicAuth? basicAuth = ();
    PrivateKey? privateKey = ();
|};

# Provides configuration for the input given for FTP `put` and `append` operations
#
# + filePath - Path of the file to create/append to
# + isFile - True if input type is a file
# + fileContent - The content read from the input file, if the input is a file
# + textContent - The input content, for other input types
# + compressInput - If true, input will be compressed before upload
public type InputContent record{|
    string filePath;
    boolean isFile = false;
    io:ReadableByteChannel? fileContent = ();
    string? textContent = ();
    boolean compressInput = false;
|};
