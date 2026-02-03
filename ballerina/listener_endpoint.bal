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

# Listener for monitoring FTP/SFTP servers and triggering service functions when files are added or deleted.
public isolated class Listener {

    private handle EMPTY_JAVA_STRING = java:fromString("");
    private final readonly & ListenerConfiguration config;
    private final task:Listener taskListener;

    # Gets invoked during object initialization.
    #
    # + listenerConfig - Configurations for FTP listener
    # + return - `()` or else an `ftp:Error` upon failure to initialize the listener
    public isolated function init(*ListenerConfiguration listenerConfig) returns Error? {
        self.config = listenerConfig.cloneReadOnly();

        decimal pollingInterval = self.config.pollingInterval;
        CoordinationConfig? coordination = self.config.coordination;

        task:Listener|error taskListener;
        if coordination is CoordinationConfig {
            taskListener = new ({
                trigger: {interval: pollingInterval},
                warmBackupConfig: {
                    databaseConfig: coordination.databaseConfig,
                    livenessCheckInterval: coordination.livenessCheckInterval,
                    taskId: coordination.memberId,
                    groupId: coordination.coordinationGroup,
                    heartbeatFrequency: coordination.heartbeatFrequency
                }
            });
        } else {
            taskListener = new ({
                trigger: {interval: pollingInterval}
            });
        }

        if taskListener is error {
            return error Error("Failed to create internal task listener: " + taskListener.message());
        }
        self.taskListener = taskListener;
        return initListener(self, self.config);
    }

    # Starts the FTP listener and begins monitoring for file changes.
    # ```ballerina
    # error? response = listener.'start();
    # ```
    #
    # + return - `()` or else an `error` upon failure to start the listener
    public isolated function 'start() returns error? {
        return self.internalStart();
    }

    # Attaches an FTP service to the listener.
    # ```ballerina
    # error? response = listener.attach(service1);
    # ```
    #
    # + ftpService - Service to be attached to the listener
    # + name - Optional name for the service
    # + return - `()` or else an `error` upon failure to attach the service
    public isolated function attach(Service ftpService, string[]|string? name = ()) returns error? {
        if name is string? {
            return self.register(ftpService, name);
        }
    }

    # Stops the FTP listener and detaches the service.
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

    # Stops the FTP listener immediately.
    # ```ballerina
    # error? response = listener.immediateStop();
    # ```
    #
    # + return - `()` or else an `error` upon failure to stop the listener
    public isolated function immediateStop() returns error? {
        check self.stop();
    }

    # Stops the FTP listener gracefully.
    # ```ballerina
    # error? response = listener.gracefulStop();
    # ```
    #
    # + return - `()` or else an `error` upon failure to stop the listener
    public isolated function gracefulStop() returns error? {
        check self.stop();
    }

    isolated function internalStart() returns error? {
        check self.taskListener.attach(getPollingService(self));
        check self.taskListener.'start();
    }

    isolated function stop() returns error? {
        check self.taskListener.gracefulStop();
        return cleanup(self);
    }

    # Polls the FTP server for new or deleted files.
    # ```ballerina
    # error? response = listener.poll();
    # ```
    #
    # + return - An `error` if failed to establish communication with the FTP server
    public isolated function poll() returns error? {
        return poll(self);
    }

    # Registers an FTP service with the listener.
    # ```ballerina
    # error? response = listener.register(ftpService, name);
    # ```
    #
    # + ftpService - The FTP service to register
    # + name - Optional name of the service
    # + return - An `error` if failed to establish communication with the FTP server
    public isolated function register(Service ftpService, string? name) returns error? {
        return register(self, ftpService);
    }
}

isolated function getPollingService(Listener initializedListener) returns task:Service {
    return service object {
        private final Listener ftpListener = initializedListener;

        isolated function execute() {
            error? result = trap self.ftpListener.poll();
            if result is error {
                log:printError("Error while executing poll function", 'error = result);
            }
        }
    };
}

# Configuration for the FTP listener.
#
# + protocol - Protocol to use for the connection: FTP (unsecure) or SFTP (over SSH)
# + host - Target server hostname or IP address
# + port - Port number of the remote service
# + auth - Authentication options for connecting to the server
# + path - Directory path on the FTP server to monitor for file changes
# + fileNamePattern - File name pattern (regex) to filter which files trigger events
# + pollingInterval - Polling interval in seconds for checking file changes
# + userDirIsRoot - If set to `true`, treats the login home directory as the root (`/`) and
#                   prevents the underlying VFS from attempting to change to the actual server root.
#                   If `false`, treats the actual server root as `/`, which may cause a `CWD /` command
#                   that can fail on servers restricting root access (e.g., chrooted environments).
# + fileAgeFilter - Configuration for filtering files based on age (optional)
# + fileDependencyConditions - Array of dependency conditions for conditional file processing (default: [])
# + laxDataBinding - If set to `true`, enables relaxed data binding for XML and JSON responses.
#                    null values in JSON/XML are allowed to be mapped to optional fields
#                    missing fields in JSON/XML are allowed to be mapped as null values
# + connectTimeout - Connection timeout in seconds 
# + socketConfig - Socket timeout configurations 
# + fileTransferMode - File transfer mode: BINARY or ASCII (FTP only)
# + sftpCompression - Compression algorithms (SFTP only)
# + sftpSshKnownHosts - Path to SSH known_hosts file (SFTP only)
# + proxy - Proxy configuration for SFTP connections (SFTP only)
# + csvFailSafe - Configuration for fail-safe CSV content processing. In the fail-safe mode,
#                 malformed CSV records are skipped and written to a separate file in the current directory
# + coordination - Configuration for distributed task coordination using warm backup approach.
#                  When configured, only one member in the group will actively poll while others act as standby.
public type ListenerConfiguration record {|
    Protocol protocol = FTP;
    string host = "127.0.0.1";
    int port = 21;
    AuthConfiguration auth?;
    string path = "/";
    string fileNamePattern?;
    decimal pollingInterval = 60;
    boolean userDirIsRoot = false;
    FileAgeFilter fileAgeFilter?;
    FileDependencyCondition[] fileDependencyConditions = [];
    boolean laxDataBinding = false;
    decimal connectTimeout = 30.0;
    SocketConfig socketConfig?;
    ProxyConfiguration proxy?;
    FileTransferMode fileTransferMode = BINARY;
    TransferCompression[] sftpCompression = [NO];
    string sftpSshKnownHosts?;
    FailSafeOptions csvFailSafe?;
    CoordinationConfig coordination?;
|};

# Fail-safe options for CSV content processing.
#
# + contentType - Specifies the type of content to log in case of errors
public type FailSafeOptions record {|
    ErrorLogContentType contentType = METADATA;
|};

# Specifies the type of content to log in case of errors during fail-safe CSV processing.
public enum ErrorLogContentType {
    METADATA,
    RAW,
    RAW_AND_METADATA
};

# FTP service for handling file system change events.
public type Service distinct service object {
};

# Represents the configuration required for distributed task coordination.
# When configured, multiple FTP listener members coordinate so that only one actively polls
# while others act as warm standby members.
#
# + databaseConfig - The database configuration for task coordination
# + livenessCheckInterval - The interval (in seconds) to check the liveness of the active node. Default is 30 seconds.
# + memberId - Unique identifier for the current member. Must be distinct for each node in the distributed system.
# + coordinationGroup - The name of the coordination group of FTP listeners that coordinate together.
#                       It is recommended to use a unique name for each group.
# + heartbeatFrequency - The interval (in seconds) for the node to update its heartbeat status. Default is 1 second.
public type CoordinationConfig record {|
    task:DatabaseConfig databaseConfig = <task:MysqlConfig>{};
    int livenessCheckInterval = 30;
    string memberId;
    string coordinationGroup;
    int heartbeatFrequency = 1;
|};
