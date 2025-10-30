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

import ballerina/jballerina.java;
import ballerina/log;
import ballerina/task;

# Represents a service listener that monitors the FTP location.
public isolated class Listener {

    private handle EMPTY_JAVA_STRING = java:fromString("");
    private ListenerConfiguration config = {};
    private task:JobId? jobId = ();

    # Gets invoked during object initialization.
    #
    # + listenerConfig - Configurations for FTP listener
    # + return - `()` or else an `ftp:Error` upon failure to initialize the listener
    public isolated function init(*ListenerConfiguration listenerConfig) returns Error? {
        self.config = listenerConfig.clone();
        lock {
            return initListener(self, self.config);
        }
    }

    # Starts the `ftp:Listener`.
    # ```ballerina
    # error? response = listener.'start();
    # ```
    #
    # + return - `()` or else an `error` upon failure to start the listener
    public isolated function 'start() returns error? {
        return self.internalStart();
    }

    # Binds a service to the `ftp:Listener`.
    # ```ballerina
    # error? response = listener.attach(service1);
    # ```
    #
    # + ftpService - Service to be detached from the listener
    # + name - Name of the service to be detached from the listener
    # + return - `()` or else an `error` upon failure to register the listener
    public isolated function attach(Service ftpService, string[]|string? name = ()) returns error? {
        if name is string? {
            return self.register(ftpService, name);
        }
    }

    # Stops consuming messages and detaches the service from the `ftp:Listener`.
    # ```ballerina
    # error? response = listener.detach(service1);
    # ```
    #
    # + ftpService - Service to be detached from the listener
    # + return - `()` or else an `error` upon failure to detach the service
    public isolated function detach(Service ftpService) returns error? {
        check self.stop();
        return deregister(self, ftpService);
    }

    # Stops the `ftp:Listener` forcefully.
    # ```ballerina
    # error? response = listener.immediateStop();
    # ```
    #
    # + return - `()` or else an `error` upon failure to stop the listener
    public isolated function immediateStop() returns error? {
        check self.stop();
    }

    # Stops the `ftp:Listener` gracefully.
    # ```ballerina
    # error? response = listener.gracefulStop();
    # ```
    #
    # + return - `()` or else an `error` upon failure to stop the listener
    public isolated function gracefulStop() returns error? {
        check self.stop();
    }

    isolated function internalStart() returns error? {
        lock {
            // Check if pollingInterval is a cron expression (string) or interval (decimal)
            decimal|string pollingInterval = self.config.pollingInterval;

            if pollingInterval is string {
                // Cron-based scheduling - delegate to native implementation
                return startCronScheduler(self, pollingInterval);
            } else {
                // Fixed interval scheduling using task scheduler
                self.jobId = check task:scheduleJobRecurByFrequency(new Job(self), pollingInterval);
            }
        }
    }

    isolated function stop() returns error? {
        lock {
            // Stop task scheduler if used
            var id = self.jobId;
            if id is task:JobId {
                check task:unscheduleJob(id);
            }
            // Stop cron scheduler if used
            decimal|string pollingInterval = self.config.pollingInterval;
            if pollingInterval is string {
                check stopCronScheduler(self);
            }
        }
    }

    # Poll new files from a FTP server.
    # ```ballerina
    # error? response = listener.poll();
    # ```
    #
    # + return - An `error` if failed to establish communication with the FTP
    #            server
    public isolated function poll() returns error? {
        return poll(self);
    }

    # Register a FTP service in an FTP listener
    # server.
    # ```ballerina
    # error? response = listener.register(ftpService, name);
    # ```
    #
    # + ftpService - The FTP service
    # + name - Name of the FTP service
    # + return - An `error` if failed to establish communication with the FTP
    #            server
    public isolated function register(Service ftpService, string? name) returns error? {
        return register(self, ftpService);
    }
}

class Job {

    *task:Job;
    private Listener ftpListener;

    public isolated function execute() {
        var result = self.ftpListener.poll();
        if result is error {
            log:printError("Error while executing poll function", 'error = result);
        }
    }

    public isolated function init(Listener initializedListener) {
        self.ftpListener = initializedListener;
    }
}

# Configuration for FTP listener.
#
# + protocol - Supported FTP protocols
# + host - Target service url
# + port - Port number of the remote service
# + auth - Authentication options
# + path - Remote FTP directory location
# + fileNamePattern - File name pattern that event need to trigger
# + pollingInterval - Polling interval in seconds (decimal, default: 60) OR cron expression (string, e.g., "0 */15 * * * *")
# + userDirIsRoot - If set to `true`, treats the login home directory as the root (`/`) and
#                   prevents the underlying VFS from attempting to change to the actual server root.
#                   If `false`, treats the actual server root as `/`, which may cause a `CWD /` command
#                   that can fail on servers restricting root access (e.g., chrooted environments).
# + fileAgeFilter - Configuration for filtering files based on age (optional)
# + fileDependencyConditions - Array of dependency conditions for conditional file processing (default: [])
public type ListenerConfiguration record {|
    Protocol protocol = FTP;
    string host = "127.0.0.1";
    int port = 21;
    AuthConfiguration auth?;
    string path = "/";
    string fileNamePattern?;
    decimal|string pollingInterval = 60;
    boolean userDirIsRoot = false;
    FileAgeFilter fileAgeFilter?;
    FileDependencyCondition[] fileDependencyConditions = [];
|};

# Represents a FTP service.
public type Service distinct service object {
};
