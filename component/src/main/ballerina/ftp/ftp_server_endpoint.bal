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

public struct ServiceEndpoint {
    Connection conn;
    ServiceEndpointConfiguration config;
}

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
@Field {value:"perPollFileCount: Maximum number of file names for per poll"}
@Field {value:"parallel: Whether file polling sequencial or parallel"}
@Field {value:"threadPoolSize: Number of thread to poll file information. Default is 5"}
@Field {value:"sftpIdentities: Username details for SFTP communication"}
@Field {value:"sftpIdentityPassPhrase: User password  for SFTP communication"}
@Field {value:"sftpUserDirIsRoot: Set user directory as a root or not. Default false"}
@Field {value:"sftpAvoidPermissionCheck: Whether to avoid permission check. Default false"}
@Field {value:"passiveMode: Whether to work on passive mode or not. Default true"}
public struct ServiceEndpointConfiguration {
    string protocol;
    string host;
    int port;
    string username;
    string passPhrase;
    string path;
    string fileNamePattern;
    string pollingInterval;
    string cronExpression;
    string perPollFileCount;
    string parallel;
    string threadPoolSize;
    string sftpIdentities;
    string sftpIdentityPassPhrase;
    string sftpUserDirIsRoot;
    string sftpAvoidPermissionCheck;
    string passiveMode;
}

@Description {value:"Gets called when the endpoint is being initialized during the package initialization."}
@Param {value:"ep: The endpoint to which the service should be registered to"}
@Param {value:"config: The ServiceEndpointConfiguration of the endpoint"}
@Return {value:"Error occured during initialization"}
public function <ServiceEndpoint ep> init (ServiceEndpointConfiguration config) {
    ep.config = config;
    var err = ep.initEndpoint();
    if (err != null) {
        throw err;
    }
}

public native function <ServiceEndpoint ep> initEndpoint () returns (error);

@Description {value:"Gets called every time a service attaches itself to this endpoint. Also happens at package initialization."}
@Param {value:"ep: The endpoint to which the service should be registered to"}
@Param {value:"serviceType: The type of the service to be registered"}
public native function <ServiceEndpoint ep> register (typedesc serviceType);

@Description {value:"Starts the registered service"}
@Param {value:"ep: The endpoint to which the service should be registered to"}
public native function <ServiceEndpoint ep> start ();

@Description {value:"Returns the connector that client code uses"}
@Param {value:"ep: The endpoint to which the service should be registered to"}
@Return {value:"The connector that client code uses"}
public native function <ServiceEndpoint ep> getClient () returns (Connection);

@Description {value:"Stops the registered service"}
@Param {value:"ep: The endpoint to which the service should be registered to"}
public native function <ServiceEndpoint ep> stop ();
