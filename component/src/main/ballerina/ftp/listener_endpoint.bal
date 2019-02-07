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
import ballerina/task;
import ballerina/io;

# Represents a service listener that monitors the FTP location.
public type Listener object {

    *AbstractListener;

    private ListenerConfig config = {};
    private task:Appointment? appointment = ();
    private task:Timer? task = ();

    public function __init(ListenerConfig listenerConfig) {
        self.config = listenerConfig;
    }

    public function __start() returns error? {
        return self.start();
    }

    public function __stop() returns error? {
        self.stop();
        return ();
    }

    public function __attach(service s, map<any> annotationData) returns error? {
        return self.register(s, annotationData);
    }

    function start() returns error? {
        (function() returns error?) onTriggerFunction = () => self.poll();
        (function(error)) onErrorFunction = err => self.onError(err);
        var scheduler = self.config.cronExpression;
        if (scheduler is string) {
            self.appointment = new task:Appointment(onTriggerFunction, onErrorFunction, scheduler);
            _ = self.appointment.schedule();
        } else {
            self.task = new task:Timer(onTriggerFunction, onErrorFunction, self.config.pollingInterval, delay = 100);
            _ = self.task.start();
        }
        return ();
    }

    function stop() {
        var scheduler = self.appointment;
        if (scheduler is task:Appointment) {
            scheduler.cancel();
        } else {
            _ = self.task.stop();
        }
    }

    function onError(error e) {
        io:println("[ERROR] FTP listener poll failed. ");
        io:println(e);
    }

    extern function poll() returns error?;

    extern function register(service s, map<any> annotationData) returns error?;
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
public type ListenerConfig record {
    Protocol protocol = FTP;
    string host = "";
    int port = -1;
    SecureSocket? secureSocket = ();
    string path = "";
    string fileNamePattern = "";
    int pollingInterval = 60000;
    string? cronExpression = ();
    !...;
};
