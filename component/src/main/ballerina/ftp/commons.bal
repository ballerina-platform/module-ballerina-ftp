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
    A record for providing trust store related configurations.

    F{{path}} Path to the trust store file
    F{{password}} Trust store password
}
public type TrustStore {
    string path,
    string password,
};

documentation {
    A record for providing key store related configurations.

    F{{path}} Path to the key store file
    F{{password}} Key store password
}
public type KeyStore {
    string path,
    string password,
};

documentation {
    A record for providing key store related configurations.

    F{{path}} Path to the key store file
    F{{password}} Key store password
}
public type PrivateKey {
    string path,
    string password,
};

documentation {
    A record for providing basic auth related configurations.

    F{{username}} Username of the user
    F{{password}} Password of the user
}
public type BasicAuth {
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
public type SecureSocket {
    TrustStore? trustStore,
    KeyStore? keyStore,
    BasicAuth? basicAuth,
    PrivateKey? privateKey,
};
