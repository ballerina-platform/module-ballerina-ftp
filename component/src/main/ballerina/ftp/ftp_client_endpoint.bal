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

package ballerina.ftp;

////////////////////////////
/// FTP Client Endpoint ///
///////////////////////////

@Description {value:"Represents an FTP client"}
public type Client object {
    public {
        ClientEndpointConfiguration config;
    }

    @Description {value:"Gets called when the endpoint is being initialized during the package initialization."}
    @Param {value:"ep: The endpoint to be initialized"}
    @Param {value:"config: The ClientEndpointConfiguration of the endpoint"}
    public function init (ClientEndpointConfiguration config);

    public native function initEndpoint ();

    public function register (typedesc serviceType) {
    }

    public function start () {
    }

    @Description {value:"Returns the connector that client code uses"}
    @Return {value:"The connector that client code uses"}
    public native function  getClient () returns ClientConnector;

    @Description {value:"Stops the registered service"}
    public function stop () {
    }
};


@Description {value:"ClientEndpointConfiguration struct represents options to be used for FTP client invocation"}
@Field {value:"protocol: Either ftp or sftp"}
@Field {value:"host: Target service url"}
@Field {value:"port: Port number of the remote service"}
@Field {value:"username: Username for authentication"}
@Field {value:"passPhrase: Password for authentication"}
public type ClientEndpointConfiguration {
    string protocol,
    string host,
    int port,
    string username,
    string passPhrase,
};

@Description {value:"Gets called when the endpoint is being initialized during the package initialization."}
@Param {value:"ep: The endpoint to be initialized"}
@Param {value:"config: The ClientEndpointConfiguration of the endpoint"}
public function Client::init (ClientEndpointConfiguration config) {
    self.config = config;
    self.initEndpoint();
}
