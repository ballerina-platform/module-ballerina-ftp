// Copyright (c) 2025 WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.

# Represents the delete action for file processing.
# When specified, the file will be deleted after processing.
public const DELETE = "DELETE";

# Configuration for moving a file after processing.
#
# + moveTo - Destination directory path where the file will be moved
# + preserveSubDirs - If true, preserves the subdirectory structure relative to the
#                     listener's root path. Defaults to true.
public type Move record {|
    string moveTo;
    boolean preserveSubDirs = true;
|};

# Type alias for Move record, used in union types for post-processing actions.
public type MOVE Move;

# Configuration for FTP service remote functions.
# Use this to override default file extension routing for content methods and
# specify automatic file actions after processing.
#
# + fileNamePattern - File name pattern (regex) that should be routed to this method.
#                     Must be a subset of the listener's `fileNamePattern`.
# + afterProcess - Action to perform after successful processing. Can be DELETE or MOVE.
#                  If not specified, no action is taken (file remains in place).
# + afterError - Action to perform after processing error. Can be DELETE or MOVE.
#                This action is executed immediately after the content handler returns an error or panics.
#                If not specified, no action is taken (file remains in place).
public type FtpFunctionConfig record {|
    string fileNamePattern?;
    MOVE|DELETE afterProcess?;
    MOVE|DELETE afterError?;
|};

# Annotation to configure FTP service remote functions.
# This can be used to specify which file patterns should be handled by a particular content method
# and what actions to perform after processing.
public annotation FtpFunctionConfig FunctionConfig on service remote function;

# Configuration for FTP service monitoring.
# Use this to specify the directory path and file patterns this service should monitor.
# When this annotation is used, the listener-level `path`, `fileNamePattern`, `fileAgeFilter`,
# and `fileDependencyConditions` fields are ignored.
#
# + path - Directory path on the FTP server to monitor for file changes
# + fileNamePattern - File name pattern (regex) to filter which files trigger events
# + fileAgeFilter - Configuration for filtering files based on age (optional)
# + fileDependencyConditions - Array of dependency conditions for conditional file processing
public type ServiceConfiguration record {|
    string path;
    string fileNamePattern?;
    FileAgeFilter fileAgeFilter?;
    FileDependencyCondition[] fileDependencyConditions = [];
|};

# Annotation to configure FTP service monitoring path and file patterns.
# This annotation allows each service to define its own monitoring configuration,
# enabling multiple services on a single listener to monitor different paths independently.
public annotation ServiceConfiguration ServiceConfig on service;
