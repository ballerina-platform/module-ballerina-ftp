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

////////////////////////////
/// FTP Client Endpoint ///
///////////////////////////

documentation {
    Represents an FTP client that intracts with an FTP server.
}
public type Client object {
    public {
        ClientEndpointConfiguration config;
    }

    public function init(ClientEndpointConfiguration clientConfig) {
        self.config = clientConfig;
        self.initEndpoint();
    }

    native function initEndpoint();

    public native function  getCallerActions() returns ClientActions;
};

documentation {
    Configuration for FTP client endpoint.

    F{{protocol}} Supported FTP protocols
    F{{host}} Target service URL
    F{{port}} Port number of the remote service
    F{{secureSocket}} Authenthication options
}
public type ClientEndpointConfiguration record {
    Protocol protocol = FTP,
    string host,
    int port,
    SecureSocket? secureSocket,
};
