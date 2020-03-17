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
import ballerina/lang.'object as lang;
import ballerina/java;

# Represents a service listener that monitors the FTP location.
public type Listener object {

    *lang:Listener;

    private handle EMPTY_JAVA_STRING = java:fromString("");
    private ListenerConfig config = {};
    private task:Scheduler? appointment = ();
    private handle? serverConnector = ();

    public function __init(ListenerConfig listenerConfig) {
        self.config = listenerConfig;
    }

    public function __start() returns error? {
        return self.start();
    }

    public function __stop() returns error? {
        check self.stop();
    }

    public function __attach(service s, string? name) returns error? {
        return self.register(s, name);
    }

    public function __detach(service s) returns error? {

    }

    public function __immediateStop() returns error? {
        check self.stop();
    }

    public function __gracefulStop() returns error? {
        check self.stop();
    }

    function start() returns error? {
        var scheduler = self.config.cronExpression;
        if (scheduler is string) {
            task:AppointmentConfiguration config = { appointmentDetails: scheduler };
            self.appointment = new(config);
        } else {
            task:TimerConfiguration config = { intervalInMillis: self.config.pollingInterval, initialDelayInMillis: 100};
            self.appointment = new (config);
        }
        var appointment = self.appointment;
        if (appointment is task:Scheduler) {
            check appointment.attach(appointmentService, self);
            check appointment.start();
        }
        log:printInfo("Listening to remote server at " + self.config.host + "...");
    }

    function stop() returns error? {
        var appointment = self.appointment;
        if (appointment is task:Scheduler) {
            check appointment.stop();
        }
        log:printInfo("Stopped listening to remote server at " + self.config.host);
    }

    public function poll() returns error? {
        return poll(self.config);
    }

    public function register(service ftpService, string? name) returns error? {
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
};

service appointmentService = service {
    resource function onTrigger(Listener l) {
        var result = l.poll();
        if (result is error) {
            log:printError("Error while executing poll function", result);
        }
    }
};

# Configuration for FTP listener endpoint.
#
# + protocol - Supported FTP protocols
# + host - Target service url
# + port - Port number of the remote service
# + secureSocket - Authentication options
# + path - Remote FTP directory location
# + fileNamePattern - File name pattern that event need to trigger
# + pollingInterval - Periodic time interval to check new update
# + cronExpression - Cron expression to check new update
# + serverConnector - Server connector for service
public type ListenerConfig record {|
    Protocol protocol = FTP;
    string host = "127.0.0.1";
    int port = 21;
    SecureSocket? secureSocket = ();
    string path = "/home";
    string fileNamePattern = "(.*).txt";
    int pollingInterval = 60000;
    string? cronExpression = ();
    handle? serverConnector = ();
|};
