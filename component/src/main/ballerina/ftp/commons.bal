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

documentation {
    Represents the set of protocols supported by the FTP listener.

    FTP - Communication happen using FTP
    SFTP - Communication happen using SFTP
    FTPS - Communication happen using FTPS
}
public type Protocol "ftp"|"sftp"|"ftps";

@final public Protocol FTP = "ftp";
@final public Protocol SFTP = "sftp";
@final public Protocol FTPS = "ftps";

documentation {
    A record for providing TrustStore related configurations.

    F{{path}} Path to the TrustStore file
    F{{password}} TrustStore password
}
public type TrustStore record {
    string path,
    string password,
};

documentation {
    A record for providing KeyStore related configurations.

    F{{path}} Path to the KeyStore file
    F{{password}} KeyStore password
}
public type KeyStore record {
    string path,
    string password,
};

documentation {
    A record for providing PrivateKey related configurations.

    F{{path}} Path to the PrivateKey file
    F{{password}} PrivateKey password
}
public type PrivateKey record {
    string path,
    string password,
};

documentation {
    A record for providing BasicAuth related configurations.

    F{{username}} Username of the user
    F{{password}} Password of the user
}
public type BasicAuth record {
    string username,
    string password,
};

documentation {
    Provides configurations for facilitating secure communication with a remote FTP server.

    F{{trustStore}} Configurations associated with TrustStore
    F{{keyStore}} Configurations associated with KeyStore
    F{{basicAuth}} Configurations associated with BasicAuth
    F{{privateKey}} Configurations associated with PrivateKey
}
public type SecureSocket record {
    TrustStore? trustStore,
    KeyStore? keyStore,
    BasicAuth? basicAuth,
    PrivateKey? privateKey,
};
