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

# Delete action for file processing.
# When specified, the file will be deleted after processing.
public const DELETE = "DELETE";

# Configuration for moving a file after processing.
public type Move record {|
    # Destination directory path where the file will be moved
    string moveTo;
    # If `true`, preserves the subdirectory structure relative to the listener's root path
    boolean preserveSubDirs = true;
|};

# Type alias for Move record, used in union types for post-processing actions.
public type MOVE Move;

# Configuration for FTP service remote functions.
# Use this to override default file extension routing for content methods and
# specify automatic file actions after processing.
public type FtpFunctionConfig record {|
    # File name pattern (regex) that routes files to this method. Must be a subset of the listener's `fileNamePattern`
    string fileNamePattern?;
    # Action to perform after successful processing. Can be `DELETE` or `MOVE`.
    # If not specified, no action is taken (file remains in place)
    MOVE|DELETE afterProcess?;
    # Action to perform after a processing error. Can be `DELETE` or `MOVE`.
    # Executed immediately after the content handler returns an error or panics.
    # If not specified, no action is taken (file remains in place)
    MOVE|DELETE afterError?;
|};

# Annotation to configure FTP service remote functions.
# This can be used to specify which file patterns should be handled by a particular content method
# and what actions to perform after processing.
public annotation FtpFunctionConfig FunctionConfig on service remote function;

# Configuration for FTP service monitoring.
# Use this to specify the directory path and file patterns this service should monitor.
public type ServiceConfiguration record {|
    # Directory path on the FTP server to monitor for file changes
    string path;
    # File name pattern (regex) to filter which files trigger events
    string fileNamePattern?;
    # Configuration for filtering files based on age
    FileAgeFilter fileAgeFilter?;
    # Dependency conditions for conditional file processing
    FileDependencyCondition[] fileDependencyConditions = [];
|};

# Annotation to configure FTP service monitoring path and file patterns.
# This annotation allows each service to define its own monitoring configuration,
# enabling multiple services on a single listener to monitor different paths independently.
public annotation ServiceConfiguration ServiceConfig on service;
