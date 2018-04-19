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

package wso2.ftp;

public type Listener object {
    private {
        ListenerEndpointConfig config;
    }

    @Description {value:"Gets called when the endpoint is being initialized during the package initialization."}
    @Param {value:"ep: The endpoint to which the service should be registered to"}
    @Param {value:"config: The ListenerEndpointConfiguration of the endpoint"}
    @Return {value:"Error occured during initialization"}
    public function init(ListenerEndpointConfig config) {
        self.config = config;
    }

    @Description {value:"Gets called every time a service attaches itself to this endpoint. Also happens at package initialization."}
    @Param {value:"ep: The endpoint to which the service should be registered to"}
    @Param {value:"serviceType: The type of the service to be registered"}
    public native function register(typedesc serviceType);

    @Description {value:"Starts the registered service"}
    @Param {value:"ep: The endpoint to which the service should be registered to"}
    public native function start();

    @Description {value:"Stops the registered service"}
    @Param {value:"ep: The endpoint to which the service should be registered to"}
    public native function stop();
};

@Description {value:"Configuration for FTP monitor service endpoint"}
@Field {value:"protocol: Either ftp or sftp"}
@Field {value:"host: Target service url"}
@Field {value:"port: Port number of the remote service"}
@Field {value:"username: Username for authentication"}
@Field {value:"passPhrase: Password for authentication"}
@Field {value:"path: Remote FTP direcotry location"}
@Field {value:"fileNamePattern: File name pattern that event need to trigger"}
@Field {value:"pollingInterval: Periodic time interval to check new update"}
@Field {value:"cronExpression: Cron expression to check new update"}
@Field {value:"identity: Username details for SFTP communication"}
@Field {value:"identityPassPhrase: User password  for SFTP communication"}
@Field {value:"userDirIsRoot: Set user directory as a root or not. Default false"}
@Field {value:"avoidPermissionCheck: Whether to avoid permission check. Default true"}
@Field {value:"passiveMode: Whether to work on passive mode or not. Default true"}
public type ListenerEndpointConfig {
    string protocol,
    string host,
    int port,
    string username,
    string passPhrase,
    string path,
    string fileNamePattern,

    int pollingInterval = 1000, // Timer
    string cronExpression, //

    string identity,
    string identityPassPhrase,
    boolean userDirIsRoot = false,
    boolean avoidPermissionCheck = true,
    boolean passiveMode = true,
};
