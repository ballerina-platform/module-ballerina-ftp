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
import ballerina/log;
import ballerina/task;

# Represents a service listener that monitors the FTP location.
public type Listener object {

    *AbstractListener;

    private ListenerConfig config = {};
    private task:Scheduler? appointment = ();

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

    function start() returns error? {
        var scheduler = self.config.cronExpression;
        if (scheduler is string) {
            task:AppointmentConfiguration config = { appointmentDetails: scheduler };
            self.appointment = new(config);
        } else {
            task:TimerConfiguration config = { interval: self.config.pollingInterval, initialDelay: 100};
            self.appointment = new (config);
        }
        check self.appointment.attach(appointmentService, attachment = self);
        check self.appointment.start();
    }

    function stop() returns error? {
        check self.appointment.stop();
    }

    function poll() returns error? = external;

    function register(service s, string? name) returns error? = external;
};

    service appointmentService = service {
        resource function onTrigger(Listener l) {
            var result = l.poll();
            if (result is error) {
                log:printError("Error while executing poll function", err = result);
            }
        }
    };

# Configuration for FTP listener endpoint.
#
# + protocol - Supported FTP protocols
# + host - Target service url
# + port - Port number of the remote service
# + secureSocket - Authenthication options
# + path - Remote FTP direcotry location
# + fileNamePattern - File name pattern that event need to trigger
# + pollingInterval - Periodic time interval to check new update
# + cronExpression - Cron expression to check new update
public type ListenerConfig record {|
    Protocol protocol = FTP;
    string host = "";
    int port = -1;
    SecureSocket? secureSocket = ();
    string path = "";
    string fileNamePattern = "";
    int pollingInterval = 60000;
    string? cronExpression = ();
|};
