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

# Defines the common error type for the module.
public type Error distinct error;

# Represents an error that occurs when connecting to the FTP/SFTP server.
# This includes network failures, host unreachable, connection refused, etc.
public type ConnectionError distinct Error;

# Represents an error that occurs when a requested file or directory is not found.
public type FileNotFoundError distinct Error;

# Represents an error that occurs when attempting to create a file or directory that already exists.
public type FileAlreadyExistsError distinct Error;

# Represents an error that occurs when FTP/SFTP configuration is invalid.
# This includes invalid port numbers, invalid regex patterns, invalid timeout values, etc.
public type InvalidConfigError distinct Error;

# Represents an error that occurs when the FTP/SFTP service is temporarily unavailable.
# This is a transient error indicating the operation may succeed on retry.
# Common causes include: server overload (421), connection issues (425, 426),
# temporary file locks (450), or server-side processing errors (451).
public type ServiceUnavailableError distinct Error;

# Represents an error that occurs when file content cannot be converted to the expected type.
# This includes JSON/XML parsing errors, CSV format errors, and record type binding failures.
# This error type is applicable to both Client operations and Listener callbacks.
#
# When used with the Listener, if an `onError` remote function is defined in the service,
# it will be invoked with this error type, allowing for custom error handling such as
# moving failed files to an error folder or sending notifications.
public type ContentBindingError distinct Error & error<ContentBindingErrorDetail>;

# Detail record for ContentBindingError providing additional context about the binding failure.
#
# + filePath - The file path that caused the error
# + content - The raw file content as bytes that failed to bind
public type ContentBindingErrorDetail record {|
    string filePath?;
    byte[] content?;
|};

# Represents an error that occurs when all retry attempts have been exhausted.
# This error wraps the last failure encountered during retry attempts.
public type AllRetryAttemptsFailedError distinct Error;

# Error returned when the circuit breaker is in OPEN state.
# This indicates the FTP server is unavailable and requests are being blocked
# to prevent cascade failures. The client should implement fallback logic
# or wait for the circuit to transition to HALF_OPEN state.
public type CircuitBreakerOpenError distinct ServiceUnavailableError;
