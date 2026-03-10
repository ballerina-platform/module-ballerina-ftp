# Specification: Ballerina FTP Library

_Owners_: @shafreenAnfar @dilanSachi @Bhashinee \
_Reviewers_: @shafreenAnfar @Bhashinee \
_Created_: 2020/10/28 \
_Updated_: 2026/03/10 \
_Edition_: Swan Lake

## Introduction

This is the specification for the FTP standard library of the [Ballerina language](https://ballerina.io/), which provides FTP client and listener functionalities to send and receive files by connecting to FTP/SFTP servers.

The FTP library specification has evolved and may continue to evolve in the future. The released versions of the specification can be found under the relevant GitHub tag.

If you have any feedback or suggestions about the library, start a discussion via a [GitHub issue](https://github.com/ballerina-platform/ballerina-standard-library/issues) or in the [Discord server](https://discord.gg/ballerinalang). Based on the outcome of the discussion, the specification and implementation can be updated. Community feedback is always welcome. Any accepted proposal, which affects the specification is stored under `/docs/proposals`. Proposals under discussion can be found with the label `type/proposal` on GitHub.

The conforming implementation of the specification is released and included in the distribution. Any deviation from the specification is considered a bug.

## Contents

1. [Overview](#1-overview)
2. [Security](#2-security)
   * 2.1 [Authentication](#21-authentication)
   * 2.2 [Authentication Methods](#22-authentication-methods)
3. [Client](#3-client)
   * 3.1 [Initializing the Client](#31-initializing-the-client)
      * 3.1.1 [Insecure Client](#311-insecure-client)
      * 3.1.2 [Secure Client](#312-secure-client)
   * 3.2 [Writing Files](#32-writing-files)
      * 3.2.1 [Write Operations](#321-write-operations)
      * 3.2.2 [Streaming Writes](#322-streaming-writes)
   * 3.3 [Reading Files](#33-reading-files)
      * 3.3.1 [Read Operations](#331-read-operations)
      * 3.3.2 [Streaming Reads](#332-streaming-reads)
      * 3.3.3 [Data Binding](#333-data-binding)
   * 3.4 [File Management](#34-file-management)
   * 3.5 [Retry Configuration](#35-retry-configuration)
   * 3.6 [Circuit Breaker](#36-circuit-breaker)
      * 3.6.1 [State Machine](#361-state-machine)
      * 3.6.2 [Configuration](#362-configuration)
      * 3.6.3 [Failure Categories](#363-failure-categories)
4. [Listener](#4-listener)
   * 4.1 [Initializing the Listener](#41-initializing-the-listener)
      * 4.1.1 [Insecure Listener](#411-insecure-listener)
      * 4.1.2 [Secure Listener](#412-secure-listener)
   * 4.2 [Service](#42-service)
      * 4.2.1 [Service Declaration](#421-service-declaration)
      * 4.2.2 [Service Configuration Annotation](#422-service-configuration-annotation)
   * 4.3 [File Change Callbacks](#43-file-change-callbacks)
      * 4.3.1 [Format-Specific Callbacks](#431-format-specific-callbacks)
      * 4.3.2 [File Delete Callback](#432-file-delete-callback)
      * 4.3.3 [Error Callback](#433-error-callback)
      * 4.3.4 [Generic File Change Callback (Deprecated)](#434-generic-file-change-callback-deprecated)
   * 4.4 [Post-Processing Actions](#44-post-processing-actions)
   * 4.5 [File Filtering](#45-file-filtering)
      * 4.5.1 [File Name Pattern](#451-file-name-pattern)
      * 4.5.2 [File Age Filter](#452-file-age-filter)
      * 4.5.3 [File Dependency Conditions](#453-file-dependency-conditions)
   * 4.6 [Distributed Coordination](#46-distributed-coordination)
5. [Caller](#5-caller)
6. [Errors](#6-errors)
   * 6.1 [Error Hierarchy](#61-error-hierarchy)
   * 6.2 [Error Handling](#62-error-handling)

## 1. Overview

FTP (File Transfer Protocol) is a standard network protocol for transferring files between a client and a server. SFTP (SSH File Transfer Protocol) adds a layer of security by encrypting the connection using SSH, protecting data in transit. The Ballerina FTP library supports both protocols.

The library exposes two core components:

- **Client** — The `ftp:Client` connects to an FTP/SFTP server and performs file operations such as reading, writing, moving, copying, and listing files.
- **Listener** — The `ftp:Listener` monitors a remote FTP/SFTP directory and invokes service callbacks when files are added or removed.

## 2. Security

### 2.1 Authentication

Both the `ftp:Client` and `ftp:Listener` support authenticated connections via the `auth` configuration field. Authentication is configured using credentials (username and password), a private key, or both. When both are provided, the preferred authentication method can be specified explicitly using an ordered list. If no preference is given, the server and client negotiate the method.

The `userDirIsRoot` configuration controls how the server root is interpreted. When set to `true`, the login home directory is treated as `/`, which is the correct setting for chrooted server environments. When `false`, the actual server root is used, which may cause failures on servers that restrict root access.

### 2.2 Authentication Methods

The following authentication methods are supported for SFTP connections:

- **PUBLICKEY** — Authenticates using a private key file, optionally protected by a passphrase.
- **PASSWORD** — Authenticates using a username and password.
- **KEYBOARD_INTERACTIVE** — An interactive challenge-response authentication mechanism.
- **GSSAPI_WITH_MIC** — Enterprise authentication using a GSS-API mechanism (e.g., Kerberos).

When a private key is configured, it is used for public key authentication. When credentials are configured, they are used for password or keyboard-interactive authentication. The `preferredMethods` field controls the order in which authentication methods are tried.

## 3. Client

The `ftp:Client` connects to an FTP or SFTP server and provides operations for reading, writing, and managing files. All client operations are isolated and can be called concurrently.

### 3.1 Initializing the Client

The `ftp:Client` is initialized with a `ClientConfiguration` record that specifies the target server. If initialization fails (for example, due to a connection error), an `ftp:Error` is returned.

#### 3.1.1 Insecure Client

An insecure FTP client is initialized by specifying the `FTP` protocol, along with the host and port of the target server.

###### Example: Insecure FTP Client

```ballerina
ftp:Client ftpClient = check new ({
    protocol: ftp:FTP,
    host: "ftp.example.com",
    port: 21
});
```

#### 3.1.2 Secure Client

A secure SFTP client is initialized by specifying the `SFTP` protocol and providing authentication details. Both credentials and a private key may be provided simultaneously.

###### Example: SFTP Client with Credentials

```ballerina
ftp:Client sftpClient = check new ({
    protocol: ftp:SFTP,
    host: "sftp.example.com",
    port: 22,
    auth: {
        credentials: {
            username: "user",
            password: "pass"
        }
    },
    userDirIsRoot: true
});
```

###### Example: SFTP Client with Private Key

```ballerina
ftp:Client sftpClient = check new ({
    protocol: ftp:SFTP,
    host: "sftp.example.com",
    port: 22,
    auth: {
        credentials: {username: "user"},
        privateKey: {
            path: "/path/to/private.key",
            password: "keypassphrase"
        },
        preferredMethods: [ftp:PUBLICKEY]
    },
    userDirIsRoot: true
});
```

### 3.2 Writing Files

#### 3.2.1 Write Operations

The client provides typed write methods for writing content in different formats to the server. All write methods accept a `FileWriteOption` parameter that controls whether the operation overwrites an existing file (`OVERWRITE`) or appends to it (`APPEND`). The default is `OVERWRITE`.

- `putBytes(path, content)` — Writes raw binary content to the specified path.
- `putText(path, content)` — Writes a UTF-8 encoded string to the specified path.
- `putJson(path, content)` — Serializes and writes a JSON value or a Ballerina record to the specified path.
- `putXml(path, content)` — Serializes and writes an XML value or a Ballerina record to the specified path.
- `putCsv(path, content)` — Serializes and writes tabular data (as a 2D string array or a record array) in CSV format to the specified path.

###### Example: Writing a Text File

```ballerina
check ftpClient->putText("/uploads/hello.txt", "Hello, World!");
```

###### Example: Appending to an Existing File

```ballerina
check ftpClient->putText("/logs/app.log", "New log entry\n", ftp:APPEND);
```

#### 3.2.2 Streaming Writes

For large files, the client supports streaming write methods that process data in chunks without loading the entire content into memory.

- `putBytesAsStream(path, content)` — Writes a stream of byte chunks to the specified path.
- `putCsvAsStream(path, content)` — Writes a stream of CSV rows (each row as a string array or record) to the specified path.

###### Example: Streaming a Large File

```ballerina
stream<io:Block, io:Error?> fileStream = check io:fileReadBlocksAsStream("/local/data.bin", 4096);
check ftpClient->putBytesAsStream("/uploads/data.bin", fileStream);
```

### 3.3 Reading Files

#### 3.3.1 Read Operations

The client provides typed read methods for reading files in different formats. These methods retrieve the entire file content into memory and support automatic retry when a retry configuration is provided (see [Section 3.5](#35-retry-configuration)).

- `getBytes(path)` — Reads the file at the specified path as a raw byte array.
- `getText(path)` — Reads the file at the specified path as a UTF-8 encoded string.
- `getJson(path)` — Reads and parses the file as JSON, with optional data binding to a target type.
- `getXml(path)` — Reads and parses the file as XML, with optional data binding to a target type.
- `getCsv(path)` — Reads and parses a CSV file, with optional data binding to a target type. The first row of the file is treated as the header row.

If the file content cannot be parsed or bound to the expected type, a `ContentBindingError` is returned.

###### Example: Reading a JSON File

```ballerina
type Order record {|
    int id;
    string item;
|};

Order order = check ftpClient->getJson("/data/order.json");
```

#### 3.3.2 Streaming Reads

For large files, the client supports streaming read methods that return data as a stream, allowing processing of individual chunks without loading the full file into memory.

- `get(path)` — Returns a raw byte stream from the remote file. The caller is responsible for closing the stream after use.
- `getBytesAsStream(path)` — Returns a stream of byte chunks from the remote file.
- `getCsvAsStream(path)` — Returns a stream of CSV rows, with optional data binding to a target row type.

###### Example: Streaming a Large CSV File

```ballerina
stream<Employee, error?> rows = check ftpClient->getCsvAsStream("/reports/employees.csv");
check rows.forEach(function(Employee emp) {
    io:println(emp.name);
});
```

#### 3.3.3 Data Binding

The typed read methods (`getJson`, `getXml`, `getCsv`, `getCsvAsStream`) support data binding via the `targetType` parameter. When a target type is provided, the parsed content is automatically bound to the specified Ballerina type. If parsing or binding fails, a `ContentBindingError` is returned.

The `laxDataBinding` configuration on the client controls whether missing or null fields are permitted when binding structured data. When `true`, missing fields are ignored and null values are accepted. When `false` (the default), strict binding is enforced.

### 3.4 File Management

The client provides the following file and directory management operations. All operations return an `ftp:Error` on failure.

- `mkdir(path)` — Creates a new directory at the specified path on the server.
- `rmdir(path)` — Deletes an empty directory at the specified path on the server. The operation fails if the directory is not empty.
- `delete(path)` — Deletes the file at the specified path on the server.
- `rename(origin, destination)` — Renames a file or moves it to a new location within the same server. The destination path must not already exist.
- `move(sourcePath, destinationPath)` — Moves a file from one location to another on the server.
- `copy(sourcePath, destinationPath)` — Creates a copy of a file at a new location on the server.
- `exists(path)` — Returns `true` if the file or directory at the specified path exists, or `false` otherwise.
- `size(path)` — Returns the size of the file at the specified path in bytes.
- `list(path)` — Returns an array of `ftp:FileInfo` records representing the contents of the specified directory.
- `isDirectory(path)` — Returns `true` if the resource at the specified path is a directory.

###### Example: Listing Files in a Directory

```ballerina
ftp:FileInfo[] files = check ftpClient->list("/incoming");
foreach ftp:FileInfo file in files {
    io:println(file.name + " (" + file.size.toString() + " bytes)");
}
```

### 3.5 Retry Configuration

The client can be configured to automatically retry failed read operations using exponential backoff. When a retry configuration is provided, the non-streaming read operations (`getBytes`, `getText`, `getJson`, `getXml`, `getCsv`) are automatically retried on transient failures.

The retry behavior is controlled by the following parameters:
- **count** — The maximum number of retry attempts. Defaults to `3`.
- **interval** — The initial wait interval in seconds before the first retry. Defaults to `1.0`.
- **backOffFactor** — The multiplier applied to the wait interval after each failed attempt. Defaults to `2.0`.
- **maxWaitInterval** — The maximum wait interval in seconds between retries, regardless of the backoff calculation. Defaults to `30.0`.

When all retry attempts are exhausted without success, an `AllRetryAttemptsFailedError` is returned.

###### Example: Client with Retry Configuration

```ballerina
ftp:Client ftpClient = check new ({
    protocol: ftp:FTP,
    host: "ftp.example.com",
    retryConfig: {
        count: 5,
        interval: 2.0,
        backOffFactor: 1.5,
        maxWaitInterval: 20.0
    }
});
```

### 3.6 Circuit Breaker

The circuit breaker pattern prevents cascading failures when the FTP server becomes unavailable. When the ratio of failed operations within a rolling time window exceeds a configured threshold, the circuit trips to the OPEN state and subsequent requests fail immediately with a `CircuitBreakerOpenError`, without attempting to connect to the server.

#### 3.6.1 State Machine

The circuit breaker operates in three states:

- **CLOSED** — Normal operating state. All requests proceed normally, and failures are tracked within the rolling window.
- **OPEN** — The failure threshold has been exceeded. All requests are rejected immediately with a `CircuitBreakerOpenError`. No connections to the server are attempted.
- **HALF_OPEN** — After the configured reset time elapses, the circuit transitions to HALF_OPEN. A single trial request is allowed. If it succeeds, the circuit returns to CLOSED. If it fails, the circuit returns to OPEN.

#### 3.6.2 Configuration

The circuit breaker is configured using a rolling window that tracks failures over a sliding time period. The following parameters control the behavior:

- **failureThreshold** — The ratio of failures to total requests (between `0.0` and `1.0`) that trips the circuit. Defaults to `0.5`.
- **resetTime** — The number of seconds to wait in the OPEN state before transitioning to HALF_OPEN. Defaults to `30`.
- **rollingWindow.requestVolumeThreshold** — The minimum number of requests that must occur within the time window before the circuit can trip. Defaults to `10`.
- **rollingWindow.timeWindow** — The duration of the rolling window in seconds for tracking failures. Defaults to `60`.
- **rollingWindow.bucketSize** — The size of each time bucket within the rolling window in seconds. Defaults to `10`.
- **failureCategories** — The categories of errors that count as failures towards tripping the circuit. Defaults to `[CONNECTION_ERROR, TRANSIENT_ERROR]`.

###### Example: Client with Circuit Breaker

```ballerina
ftp:Client ftpClient = check new ({
    protocol: ftp:FTP,
    host: "ftp.example.com",
    circuitBreaker: {
        failureThreshold: 0.5,
        resetTime: 30,
        rollingWindow: {
            requestVolumeThreshold: 5,
            timeWindow: 60,
            bucketSize: 10
        },
        failureCategories: [ftp:CONNECTION_ERROR, ftp:TRANSIENT_ERROR]
    }
});
```

#### 3.6.3 Failure Categories

The `failureCategories` field specifies which error types count as failures when evaluating the circuit breaker threshold:

- **CONNECTION_ERROR** — Network failures, connection timeouts, and unreachable hosts.
- **AUTHENTICATION_ERROR** — Invalid credentials or authorization failures.
- **TRANSIENT_ERROR** — Server disconnection or temporary unavailability during an operation.
- **ALL_ERRORS** — Every error type counts as a failure.

## 4. Listener

The `ftp:Listener` polls a remote FTP or SFTP directory at a configured interval and detects file changes. When files are added or removed, the listener dispatches events to the attached services by invoking their callback methods.

### 4.1 Initializing the Listener

The `ftp:Listener` is initialized with a `ListenerConfiguration` record that specifies the target server and polling behavior. The `pollingInterval` field controls how frequently (in seconds) the server is checked for changes. The default polling interval is 60 seconds.

#### 4.1.1 Insecure Listener

An insecure FTP listener is initialized by specifying the host and port. The monitored directory path is configured via the `@ftp:ServiceConfig` annotation on the attached service.

###### Example: Insecure FTP Listener

```ballerina
listener ftp:Listener ftpListener = check new ({
    protocol: ftp:FTP,
    host: "ftp.example.com",
    port: 21,
    pollingInterval: 30
});
```

#### 4.1.2 Secure Listener

A secure SFTP listener is initialized in the same way as a secure client, by specifying the `SFTP` protocol and providing authentication details.

###### Example: SFTP Listener

```ballerina
listener ftp:Listener ftpListener = check new ({
    protocol: ftp:SFTP,
    host: "sftp.example.com",
    port: 22,
    auth: {
        credentials: {
            username: "user",
            password: "pass"
        },
        privateKey: {
            path: "/path/to/private.key",
            password: "keypassphrase"
        }
    },
    pollingInterval: 60,
    userDirIsRoot: true
});
```

### 4.2 Service

#### 4.2.1 Service Declaration

A service is attached to an `ftp:Listener` to receive file change notifications. Services may be declared statically at module level or attached dynamically using the listener's `attach()` method.

###### Example: Static Service Declaration

```ballerina
service ftp:Service on ftpListener {
    remote function onFileChange(ftp:WatchEvent & readonly event, ftp:Caller caller) returns error? {
        foreach ftp:FileInfo addedFile in event.addedFiles {
            io:println("File added: " + addedFile.path);
        }
    }
}
```

#### 4.2.2 Service Configuration Annotation

The `@ftp:ServiceConfig` annotation configures the monitoring path and file filtering options at the service level. This allows multiple services attached to a single listener to monitor different directories independently.

The `path` field is mandatory and must be an absolute path starting with `/`. The `fileNamePattern` field accepts a regular expression to filter which files trigger events.

If any service attached to a listener uses `@ftp:ServiceConfig`, then all services attached to that listener must use it. Mixing annotated and unannotated services on the same listener results in an `InvalidConfigError`.

When `@ftp:ServiceConfig` is used, any monitoring-related fields set at the listener level (`path`, `fileNamePattern`, `fileAgeFilter`, `fileDependencyConditions`) are ignored and a deprecation warning is logged.

###### Example: Multiple Services on One Listener

```ballerina
listener ftp:Listener ftpListener = check new ({
    protocol: ftp:SFTP,
    host: "sftp.example.com",
    port: 22,
    auth: {credentials: {username: "user", password: "pass"}},
    pollingInterval: 30
});

@ftp:ServiceConfig {
    path: "/incoming/orders",
    fileNamePattern: ".*\\.csv"
}
service on ftpListener {
    remote function onFileCsv(record {}[] content, ftp:FileInfo fileInfo) returns error? {
        // Processes CSV files from /incoming/orders
    }
}

@ftp:ServiceConfig {
    path: "/incoming/configs",
    fileNamePattern: ".*\\.json"
}
service on ftpListener {
    remote function onFileJson(json content, ftp:FileInfo fileInfo) returns error? {
        // Processes JSON files from /incoming/configs
    }
}
```

### 4.3 File Change Callbacks

When the listener detects a file change, it invokes the appropriate callback method on the attached service. The `ftp:Caller` parameter is optional in all callbacks; it may be omitted if FTP operations are not required during processing.

The `ftp:FileInfo` record provides metadata about the file, including its path, name, size, last modified timestamp, and whether it is a file or directory.

#### 4.3.1 Format-Specific Callbacks

In addition to the generic `onFileChange` callback, the listener supports format-specific callbacks that automatically parse file content and pass it to the handler as a typed value. Files are routed to handlers based on their extension: `.txt` → `onFileText`, `.json` → `onFileJson`, `.xml` → `onFileXml`, `.csv` → `onFileCsv`. Files with any other extension are routed to `onFile`. Extension-based routing can be customized per callback using the `@ftp:FunctionConfig` annotation.

**`onFileText`** — Invoked when a `.txt` file is added. The file content is passed as a UTF-8 string.

###### Example: Text File Handler

```ballerina
remote function onFileText(string content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
    io:println("Processing: " + fileInfo.name);
    io:println(content);
}
```

**`onFileJson`** — Invoked when a `.json` file is added. The content is parsed as JSON and passed as either a `json` value or a data-bound record, depending on the declared parameter type.

###### Example: JSON File Handler with Data Binding

```ballerina
type Config record {|
    string env;
    int maxRetries;
|};

remote function onFileJson(Config content, ftp:FileInfo fileInfo) returns error? {
    io:println("Environment: " + content.env);
}
```

**`onFileXml`** — Invoked when a `.xml` file is added. The content is parsed as XML and passed as either an `xml` value or a data-bound record.

**`onFileCsv`** — Invoked when a `.csv` file is added. The first row of the CSV file is treated as the header row. The following parameter types are supported:
- `string[][]` — All rows loaded into memory as arrays of strings.
- `record {}[]` — All rows loaded into memory and data-bound to the record type.
- `stream<string[], error>` — Rows processed one at a time as string arrays (memory-efficient for large files).
- `stream<record {}, error>` — Rows processed one at a time and data-bound to the record type.

###### Example: CSV File Handler with Streaming

```ballerina
type Employee record {|
    string name;
    string department;
|};

remote function onFileCsv(stream<Employee, error> content, ftp:FileInfo fileInfo) returns error? {
    check content.forEach(function(Employee emp) {
        io:println(emp.name);
    });
}
```

**`onFile`** — Invoked when a file with an unrecognized extension is added. The content is passed as either a `byte[]` (entire file in memory) or a `stream<byte[], error>` (for large files).

#### 4.3.2 File Delete Callback

The `onFileDelete` callback is invoked when a file is removed from the monitored directory. The deleted file's path is passed as a string.

###### Example: File Delete Handler

```ballerina
remote function onFileDelete(string deletedFile, ftp:Caller caller) returns error? {
    io:println("Deleted: " + deletedFile);
}
```

#### 4.3.3 Error Callback

The `onError` callback is invoked when a content binding error occurs while parsing a file. This provides a centralized location for handling files that cannot be parsed into the expected format.

The callback receives an `ftp:Error` value. When the error is a `ContentBindingError`, its detail record contains the `filePath` of the file that failed and the raw `content` as a byte array.

If `onError` is not defined, binding errors are logged and the affected file is skipped.

###### Example: Error Handler

```ballerina
remote function onError(ftp:Error err, ftp:Caller caller) returns error? {
    if err is ftp:ContentBindingError {
        string? filePath = err.detail().filePath;
        log:printError("Binding failed for file: " + (filePath ?: "unknown"), err);
        if filePath is string {
            check caller->move(filePath, "/error/" + filePath);
        }
    }
}
```

#### 4.3.4 Generic File Change Callback (Deprecated)

The `onFileChange` callback is the general-purpose handler for file system events. It receives a `ftp:WatchEvent` record containing two fields:
- `addedFiles` — An array of `ftp:FileInfo` records for newly detected files.
- `deletedFiles` — An array of strings containing the paths of deleted files.

###### Example: Generic File Change Handler

```ballerina
remote function onFileChange(ftp:WatchEvent & readonly event, ftp:Caller caller) returns error? {
    foreach ftp:FileInfo file in event.addedFiles {
        io:println("New file: " + file.path);
    }
    foreach string path in event.deletedFiles {
        io:println("Deleted: " + path);
    }
}
```

### 4.4 Post-Processing Actions

The `@ftp:FunctionConfig` annotation supports automatic file actions after a callback completes. This eliminates the need for boilerplate file management at the end of each handler.

The annotation supports the following actions via the `afterProcess` and `afterError` fields:

- **`DELETE`** — The file is deleted after the handler returns.
- **`MOVE`** — The file is moved to a specified destination directory after the handler returns. The `moveTo` field specifies the destination path. The `preserveSubDirs` flag (default `true`) controls whether the subdirectory structure relative to the monitored path is preserved in the destination.

`afterProcess` is executed when the handler returns successfully. `afterError` is executed when the handler returns an error or panics. If neither is specified, no post-processing action is taken.

When using `MOVE` with `preserveSubDirs: true`, the destination directory structure must already exist on the server. For example, if monitoring `/input/` and a file at `/input/orders/2024/file.csv` is processed with `moveTo: "/archive/"`, the file is moved to `/archive/orders/2024/file.csv`.

###### Example: Delete After Processing

```ballerina
service on ftpListener {
    @ftp:FunctionConfig {
        afterProcess: ftp:DELETE
    }
    remote function onFileJson(json content, ftp:FileInfo fileInfo) returns error? {
        processJson(content);
    }
}
```

###### Example: Move to Archive on Success, Move to Error Directory on Failure

```ballerina
service on ftpListener {
    @ftp:FunctionConfig {
        afterProcess: {moveTo: "/archive/success/"},
        afterError: {moveTo: "/archive/failed/"}
    }
    remote function onFileXml(xml content, ftp:FileInfo fileInfo) returns error? {
        check processXml(content);
    }
}
```

###### Example: Routing by File Pattern

The `fileNamePattern` field on `@ftp:FunctionConfig` overrides the extension-based routing for that specific callback, allowing fine-grained control over which files trigger which handler.

```ballerina
service on ftpListener {
    @ftp:FunctionConfig {
        fileNamePattern: "order_.*\\.csv",
        afterProcess: {moveTo: "/processed/"}
    }
    remote function onFileCsv(Employee[] content, ftp:FileInfo fileInfo) returns error? {
        saveEmployees(content);
    }
}
```

### 4.5 File Filtering

#### 4.5.1 File Name Pattern

The `fileNamePattern` field in `@ftp:ServiceConfig` accepts a Java regular expression. Only files whose names match the pattern trigger events. If no pattern is specified, all files in the monitored directory trigger events.

#### 4.5.2 File Age Filter

The `fileAgeFilter` in `@ftp:ServiceConfig` allows filtering files based on how recently they were last modified. This is useful for skipping files that are still being written by an upstream process. The filter specifies a minimum age (in seconds); files newer than this threshold are excluded from triggering events.

#### 4.5.3 File Dependency Conditions

The `fileDependencyConditions` field in `@ftp:ServiceConfig` allows conditional file processing based on the presence of related files. A dependency condition specifies a target file pattern and a list of required companion files that must also be present before the target file triggers an event.

The `matchingMode` field controls whether `ALL` required files or `ANY` of them must be present. Capture groups in the target pattern can be referenced in the required file patterns using `$1`, `$2`, etc.

###### Example: Process a CSV Only When a Marker File Exists

```ballerina
@ftp:ServiceConfig {
    path: "/incoming/orders",
    fileNamePattern: "order_.*\\.csv",
    fileDependencyConditions: [
        {
            targetPattern: "order_(\\d+)\\.csv",
            requiredFiles: ["order_$1.marker"],
            matchingMode: ftp:ALL
        }
    ]
}
service on ftpListener {
    remote function onFileCsv(record {}[] content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
        check caller->move(fileInfo.path, "/processed/" + fileInfo.name);
    }
}
```

### 4.6 Distributed Coordination

The FTP listener supports distributed coordination for high-availability deployments. When multiple listener instances are deployed across nodes, coordination ensures that only one instance actively polls the FTP server at any time, while the others act as warm standby nodes. This prevents duplicate file processing and provides automatic failover.

Coordination is enabled by providing a `CoordinationConfig` in the `ListenerConfiguration`. All instances in a coordination group must be configured with the same `coordinationGroup` name and a unique `memberId` per node. Coordination state is managed through a shared database (MySQL or PostgreSQL).

The coordination mechanism works as follows:

1. Members in the same `coordinationGroup` elect an active member through the shared database.
2. The active member updates a heartbeat record at the configured `heartbeatFrequency` interval (default: 1 second).
3. Standby members monitor the active member's heartbeat every `livenessCheckInterval` seconds (default: 30 seconds).
4. If the heartbeat becomes stale, a standby member elects itself as the new active member and begins polling.
5. Only the active member's polling cycle executes; standby members skip polling silently.

###### Example: Listener with Distributed Coordination

```ballerina
listener ftp:Listener ftpListener = check new ({
    protocol: ftp:SFTP,
    host: "sftp.example.com",
    port: 22,
    auth: {credentials: {username: "user", password: "pass"}},
    coordination: {
        memberId: "node-1",
        coordinationGroup: "ftp-processors",
        livenessCheckInterval: 30,
        heartbeatFrequency: 1,
        databaseConfig: <task:MysqlConfig>{
            host: "db.example.com",
            user: "dbuser",
            password: "dbpass",
            database: "coordination_db"
        }
    }
});
```

## 5. Caller

The `ftp:Caller` is a facade over an `ftp:Client` that is created internally by the runtime when a service callback declares it as a parameter. It exposes the same operations as `ftp:Client`, allowing service callbacks to perform FTP operations (reading, writing, moving, deleting files) on the same server that the listener is monitoring.

The `ftp:Caller` inherits its connection type (secure or insecure) from the listener configuration. It cannot be created directly by user code.

The `caller` parameter is optional in all service callbacks. If FTP operations are not required within a callback, the parameter may be omitted.

###### Example: Using the Caller to Move a Processed File

```ballerina
service on ftpListener {
    remote function onFileText(string content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
        processContent(content);
        check caller->move(fileInfo.path, "/processed/" + fileInfo.name);
    }
}
```

## 6. Errors

### 6.1 Error Hierarchy

The FTP library defines a hierarchy of error types rooted at `ftp:Error`. All FTP-specific errors are distinct subtypes of this base type, enabling both specific and general error handling.

- **`Error`** — The base error type for all FTP-related errors. All other error types are subtypes of this.
- **`ConnectionError`** — Represents failures when connecting to the server, including network failures, unreachable hosts, and connection refusals.
- **`FileNotFoundError`** — Represents failures when a requested file or directory does not exist on the server.
- **`FileAlreadyExistsError`** — Represents failures when attempting to create a file or directory that already exists.
- **`InvalidConfigError`** — Represents failures due to invalid configuration values, such as an invalid port number, regex pattern, or timeout value.
- **`ServiceUnavailableError`** — Represents transient server-side failures. Common causes include server overload, connection issues, or temporary file locks. Operations that return this error may succeed on retry.
- **`ContentBindingError`** — Represents failures when file content cannot be parsed or bound to the expected Ballerina type. This includes JSON/XML parse errors, CSV format errors, and record type binding failures. The error's detail record includes the `filePath` and the raw `content` as bytes.
- **`AllRetryAttemptsFailedError`** — Represents the failure returned when all retry attempts are exhausted. It wraps the last encountered error.
- **`CircuitBreakerOpenError`** — A subtype of `ServiceUnavailableError` returned when the circuit breaker is in the OPEN state. It indicates that requests are being blocked to prevent cascading failures.

### 6.2 Error Handling

Because all error types are subtypes of `ftp:Error`, callers can handle errors at any level of specificity. More specific error types should be checked before more general ones.

###### Example: Handling Specific Error Types

```ballerina
byte[]|ftp:Error result = ftpClient->getBytes("/data/file.txt");
if result is ftp:CircuitBreakerOpenError {
    // Circuit is open — server is currently unavailable
    applyFallback();
} else if result is ftp:FileNotFoundError {
    // File does not exist
    log:printWarn("File not found");
} else if result is ftp:ConnectionError {
    // Network-level failure
    log:printError("Connection failed", result);
} else if result is ftp:Error {
    // Any other FTP error
    log:printError("FTP operation failed", result);
} else {
    processBytes(result);
}
```