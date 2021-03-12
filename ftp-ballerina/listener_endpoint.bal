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

import ballerina/log;
import ballerina/task;
import ballerina/jballerina.java;

# Represents a service listener that monitors the FTP location.
public class Listener {

    private handle EMPTY_JAVA_STRING = java:fromString("");
    private ListenerConfig config = {};
    private task:JobId? jobId = ();
    private handle? serverConnector = ();

    # Gets invoked during object initialization.
    #
    # + listenerConfig - Configurations for FTP listener
    public isolated function init(ListenerConfig listenerConfig) {
        self.config = listenerConfig;
    }

    # Starts the `ftp:Listener`.
    # ```ballerina
    # error? response = listener->start();
    # ```
    #
    # + return - () or else `error` upon failure to start the listener
    public isolated function 'start() returns @tainted error? {
        return self.internalStart();
    }

    # Stops the `ftp:Listener`.
    # ```ballerina
    # error? response = listener->__stop();
    # ```
    #
    # + return - () or else `error` upon failure to stop the listener
    public isolated function __stop() returns error? {
        check self.stop();
    }

    # Binds a service to the `ftp:Listener`.
    # ```ballerina
    # error? response = listener->attach(service1);
    # ```
    #
    # + s - Service to be detached from the listener
    # + name - Name of the service to be detached from the listener
    # + return - `()` or else a `error` upon failure to register the listener
    public isolated function attach(service object {} s, string[]|string? name = ()) returns error? {
        if (name is string?) {
            return self.register(s, name);
        }
    }

    # Stops consuming messages and detaches the service from the `ftp:Listener`.
    # ```ballerina
    # error? response = listener->detach(service1);
    # ```
    #
    # + emailService - Service to be detached from the listener
    # + return - `()` or else a `error` upon failure to detach the service
    public isolated function detach(service object {} emailService) returns error? {

    }

    # Stops the `ftp:Listener` forcefully.
    # ```ballerina
    # error? response = listener->immediateStop();
    # ```
    #
    # + return - `()` or else a `error` upon failure to stop the listener
    public isolated function immediateStop() returns error? {
        check self.stop();
    }

    # Stops the `ftp:Listener` gracefully.
    # ```ballerina
    # error? response = listener->gracefulStop();
    # ```
    #
    # + return - () or else `error` upon failure to stop the listener
    public isolated function gracefulStop() returns error? {
        check self.stop();
    }

    isolated function internalStart() returns @tainted error? {
        self.jobId = check task:scheduleJobRecurByFrequency(new Job(self), self.config.pollingInterval);
        log:printInfo("Listening to remote server at " + self.config.host + "...");
    }

    isolated function stop() returns error? {
        var id = self.jobId;
        if (id is task:JobId) {
            check task:unscheduleJob(id);
        }
        log:printInfo("Stopped listening to remote server at " + self.config.host);
    }

    # Poll new files from a FTP server.
    # ```ballerina
    # error? response = listener->poll();
    # ```
    #
    # + return - An `error` if failed to establish communication with the FTP
    #            server
    public isolated function poll() returns error? {
        return poll(self.config);
    }

    # Register a FTP service in an FTP listener
    # server.
    # ```ballerina
    # error? response = listener->register(ftpService, name);
    # ```
    #
    # + ftpService - The FTP service
    # + name - Name of the FTP service
    # + return - An `error` if failed to establish communication with the FTP
    #            server
    public isolated function register(service object {} ftpService, string? name) returns error? {
        error? response = ();
        handle serviceName = self.EMPTY_JAVA_STRING;
        if(name is string){
            serviceName = java:fromString(name);
        }
        handle|error result = register(self, self.config,  ftpService, serviceName);
        if(result is handle){
            self.config.serverConnector = result;
        } else {
            response = result;
        }
        return response;
    }
}

class Job {

    *task:Job;
    private Listener l;

    public function execute() {
        var result = self.l.poll();
        if (result is error) {
            log:printError("Error while executing poll function", 'error = result);
        }
    }

    public isolated function init(Listener l) {
        self.l = l;
    }
}

# Configuration for FTP listener endpoint.
#
# + protocol - Supported FTP protocols
# + host - Target service url
# + port - Port number of the remote service
# + secureSocket - Authentication options
# + path - Remote FTP directory location
# + fileNamePattern - File name pattern that event need to trigger
# + pollingInterval - Periodic time interval to check new update
# + serverConnector - Server connector for service
public type ListenerConfig record {|
    Protocol protocol = FTP;
    string host = "127.0.0.1";
    int port = 21;
    SecureSocket? secureSocket = ();
    string path = "/home";
    string fileNamePattern = "(.*).txt";
    decimal pollingInterval = 60;
    handle? serverConnector = ();
|};
