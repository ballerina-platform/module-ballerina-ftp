# Specification: Ballerina FTP Library

_Owners_: @shafreenAnfar @dilanSachi @Bhashinee \
_Reviewers_: @shafreenAnfar @Bhashinee \
_Created_: 2020/10/28 \
_Updated_: 2026/09/01 \
_Edition_: Swan Lake

## Introduction

This is the specification for the FTP library of the [Ballerina language](https://ballerina.io/). The library reads and writes files on a remote FTP, FTPS, or SFTP server, and watches a directory on one for files arriving and leaving.

This specification may change in future versions. Released versions can be found under the matching GitHub tag.

If you have feedback or suggestions, start a discussion with a [GitHub issue](https://github.com/ballerina-platform/ballerina-library/issues) or in the [Discord server](https://discord.gg/ballerinalang). The specification and the implementation can then be updated together. An accepted proposal that affects the specification is stored under `/docs/proposals`; proposals still under discussion carry the `type/proposal` label on GitHub.

The implementation that matches this specification is released with the distribution. Anything the library does differently from this document is a bug.

## Contents

1. [Overview](#1-overview)
2. [Security](#2-security)
   * 2.1 [Authentication](#21-authentication)
     * 2.1.1 [Password Authentication](#211-password-authentication)
     * 2.1.2 [Public Key Authentication](#212-public-key-authentication)
     * 2.1.3 [Anonymous Authentication](#213-anonymous-authentication)
   * 2.2 [SFTP Authentication Methods](#22-sftp-authentication-methods)
   * 2.3 [SFTP Host Key Verification](#23-sftp-host-key-verification)
   * 2.4 [FTPS Transport Security](#24-ftps-transport-security)
   * 2.5 [The Server Root](#25-the-server-root)
3. [Client](#3-client)
   * 3.1 [Initializing the Client](#31-initializing-the-client)
   * 3.2 [Transport Options](#32-transport-options)
   * 3.3 [Writing Files](#33-writing-files)
   * 3.4 [Reading Files](#34-reading-files)
   * 3.5 [Data Binding](#35-data-binding)
   * 3.6 [File Management](#36-file-management)
   * 3.7 [Retry](#37-retry)
   * 3.8 [Circuit Breaker](#38-circuit-breaker)
4. [Listener](#4-listener)
   * 4.1 [Initializing the Listener](#41-initializing-the-listener)
   * 4.2 [Service](#42-service)
   * 4.3 [Content Handlers](#43-content-handlers)
   * 4.4 [Handler Selection](#44-handler-selection)
   * 4.5 [File Filtering](#45-file-filtering)
   * 4.6 [Post-Processing Actions](#46-post-processing-actions)
   * 4.7 [Error Handling](#47-error-handling)
   * 4.8 [Distributed Coordination](#48-distributed-coordination)
   * 4.9 [The Event Handler (Deprecated)](#49-the-event-handler-deprecated)
5. [Caller](#5-caller)
6. [Errors](#6-errors)
7. [Observability](#7-observability)
   * 7.1 [Metrics](#71-metrics)
      * 7.1.1 [Gauges](#711-gauges)
      * 7.1.2 [Explicit Counters](#712-explicit-counters)
      * 7.1.3 [Duration Gauges](#713-duration-gauges)
      * 7.1.4 [Querying Metrics](#714-querying-metrics)
   * 7.2 [Tags](#72-tags)
      * 7.2.1 [Identity Tags](#721-identity-tags)
      * 7.2.2 [Action Tags](#722-action-tags)
      * 7.2.3 [Outcome Tags](#723-outcome-tags)
      * 7.2.4 [Tag Consistency Rule](#724-tag-consistency-rule)
      * 7.2.5 [File-Scoped Tags (Trace-Only)](#725-file-scoped-tags-trace-only)
      * 7.2.6 [Client Operation Tag Mapping](#726-client-operation-tag-mapping)
      * 7.2.7 [Listener Event Tag Mapping](#727-listener-event-tag-mapping)
      * 7.2.8 [File Lifecycle Stages](#728-file-lifecycle-stages)
   * 7.3 [Observability Outputs per File](#73-observability-outputs-per-file)
   * 7.4 [Sample PromQL Queries](#74-sample-promql-queries)
   * 7.5 [Enabling Observability](#75-enabling-observability)
   * 7.6 [Observability Safety Rules](#76-observability-safety-rules)

## 1. Overview

The library has three parts.

| Part | What it does |
| --- | --- |
| `ftp:Client` | Performs file system operations on an FTP, FTPS, or SFTP server |
| `ftp:Listener` | Polls a directory on a server and triggers on file changes |
| `ftp:Caller` | Provides server access to an `ftp:Service` handler for file operations |

A connection is bound to a single protocol, chosen by the `protocol` field.

```ballerina
public enum Protocol {
    FTP = "ftp",
    FTPS = "ftps",
    SFTP = "sftp"
}
```

`FTP` is plain, unencrypted FTP. `FTPS` is FTP over TLS. `SFTP` is file transfer over SSH. Not every configuration field applies to every protocol; each field says which ones it applies to.

Every path is a `/`-separated path on the server, resolved against the root the server presents at login. [Section 2.5](#25-the-server-root) covers what that root is.

Every client operation is `isolated` and may be called concurrently on one client.

## 2. Security

### 2.1 Authentication

The `auth` field of the client and listener configuration says who connects.

```ballerina
public type AuthConfiguration record {|
    Credentials credentials?;
    PrivateKey privateKey?;
    SecureSocket secureSocket?;
    PreferredMethod[] preferredMethods = [PUBLICKEY, PASSWORD];
|};
```

`secureSocket` configures TLS and applies to FTPS only; see [Section 2.4](#24-ftps-transport-security). When both `credentials` and `privateKey` are present, `preferredMethods` decides which is offered first.

#### 2.1.1 Password Authentication

`credentials` is a username with an optional password. The password is optional because public key authentication needs the username alone.

```ballerina
public type Credentials record {|
    string username;
    string password?;
|};
```

#### 2.1.2 Public Key Authentication

`privateKey` is an SSH key for SFTP. `password` decrypts the key file when the key is encrypted.

```ballerina
public type PrivateKey record {|
    string path;
    string password?;
|};
```

#### 2.1.3 Anonymous Authentication

Omitting `auth` does not skip authentication. The username defaults to `anonymous`, with an empty password on FTP and FTPS and none on SFTP.

On an FTP or FTPS server that permits anonymous login, that is the anonymous connection. **SFTP has no anonymous mode**: the SSH login is attempted as user `anonymous`, and a server that does not know that user rejects it.

### 2.2 SFTP Authentication Methods

`preferredMethods` lists the SSH authentication methods to attempt, best first.

```ballerina
public enum PreferredMethod {
    KEYBOARD_INTERACTIVE,
    GSSAPI_WITH_MIC,
    PASSWORD,
    PUBLICKEY
}
```

The default is `[PUBLICKEY, PASSWORD]`, so a key is tried before a password. `PUBLICKEY` uses `privateKey`, `PASSWORD` and `KEYBOARD_INTERACTIVE` use `credentials`, and `GSSAPI_WITH_MIC` uses a GSS-API mechanism such as Kerberos. Listing a method whose configuration is absent leaves that method with nothing to offer, and the server moves on to the next one.

```ballerina
ftp:Client sftpClient = check new ({
    protocol: ftp:SFTP,
    host: "sftp.example.com",
    port: 22,
    auth: {
        credentials: {username: "alice"},
        privateKey: {path: "/keys/id_rsa", password: "***"},
        preferredMethods: [ftp:PUBLICKEY]
    },
    userDirIsRoot: true
});
```

### 2.3 SFTP Host Key Verification

**Host key verification is off unless `sftpSshKnownHosts` names an existing file.** With such a file, strict host key checking is enabled against it, and a server whose key is not listed is refused. Without it — the default — the server key is accepted unseen, and a man-in-the-middle is not detected.

```ballerina
ftp:Client sftpClient = check new ({
    protocol: ftp:SFTP,
    host: "sftp.example.com",
    port: 22,
    auth: {credentials: {username: "alice", password: "***"}},
    sftpSshKnownHosts: "~/.ssh/known_hosts"
});
```

A `~` at the start of the path is expanded to the home directory of the user running the program. **A path that does not exist is a warning in the log, not an error.** The connection then proceeds with verification off, so a typo in this path silently removes the protection it was added for.

### 2.4 FTPS Transport Security

`secureSocket` controls TLS on an FTPS connection.

```ballerina
public type SecureSocket record {|
    crypto:KeyStore key?;
    string|crypto:TrustStore cert?;
    FtpsMode mode = EXPLICIT;
    FtpsDataChannelProtection dataChannelProtection = PRIVATE;
    boolean verifyHostName = true;
|};
```

`key` is a client keystore, and is needed only for mutual TLS. `cert` is what the server certificate chain is validated against — a PEM file path, or a `crypto:TrustStore` for JKS and PKCS12. When `cert` is absent, the JDK's own truststore (`cacerts`) is used, so a certificate from a public CA validates without any configuration.

```ballerina
public enum FtpsMode {
    IMPLICIT,
    EXPLICIT
}
```

`EXPLICIT`, the default, connects in plain FTP and upgrades with `AUTH TLS`; the conventional port is 21. `IMPLICIT` negotiates TLS from the first byte; the conventional port is 990.

```ballerina
public enum FtpsDataChannelProtection {
    CLEAR,
    PRIVATE,
    SAFE,
    CONFIDENTIAL
}
```

`dataChannelProtection` covers the data channel that carries file content, separately from the command channel. `PRIVATE` (the default) and `CONFIDENTIAL` encrypt it, `SAFE` gives integrity only, and **`CLEAR` leaves file content unencrypted on the wire** even though the command channel stays protected.

`verifyHostName` checks the CN or SAN of the server certificate against the host being connected to. It defaults to `true`. Setting it to `false` accepts a certificate belonging to a different name, and is for development against a certificate whose identity does not match the host. It does not accept an untrusted certificate: a self-signed or private-CA certificate still has to be trusted through `cert`.

A certificate the truststore does not trust, and a certificate whose identity does not match the host while `verifyHostName` is `true`, both fail at the TLS handshake with an `ftp:Error`.

```ballerina
ftp:Client ftpsClient = check new ({
    protocol: ftp:FTPS,
    host: "ftps.example.com",
    port: 21,
    auth: {
        credentials: {username: "alice", password: "***"},
        secureSocket: {
            cert: {path: "/certs/truststore.p12", password: "***"},
            mode: ftp:EXPLICIT
        }
    }
});
```

### 2.5 The Server Root

`userDirIsRoot` decides what `/` means. When `true`, the login home directory is the root, which is what a chrooted or jailed account needs. When `false` — the default — the actual server root is used, and a server that refuses to change directory above the home directory fails.

## 3. Client

### 3.1 Initializing the Client

The `ftp:Client` is initialized using an `ftp:ClientConfiguration` record. Every field has a default, so a client for a local server on the standard port needs no configuration at all.

The `protocol`, `host`, and `port` identify the server, while `auth` says who connects and `userDirIsRoot` says what its root is. Data binding, timeouts, proxying, transfer mode, compression, host key verification, retry, and the circuit breaker can be configured through the other fields.

```ballerina
public type ClientConfiguration record {|
    Protocol protocol = FTP;
    string host = "127.0.0.1";
    int port = 21;
    AuthConfiguration auth?;
    boolean userDirIsRoot = false;
    boolean laxDataBinding = false;
    decimal connectTimeout = 30.0;
    SocketConfig socketConfig?;
    ProxyConfiguration proxy?;
    FileTransferMode fileTransferMode = BINARY;
    TransferCompression[] sftpCompression = [NO];
    string sftpSshKnownHosts?;
    FailSafeOptions csvFailSafe?;
    RetryConfig retryConfig?;
    CircuitBreakerConfig circuitBreaker?;
|};
```

`connectTimeout` is in seconds. `port` defaults to 21, which suits FTP and explicit FTPS; SFTP normally wants 22 and implicit FTPS normally wants 990.

Creating the client resolves the server root, so an unreachable host, a rejected identity, or an untrusted certificate fails here rather than on the first operation.

```ballerina
ftp:Client ftpClient = check new ({
    protocol: ftp:FTP,
    host: "ftp.example.com",
    port: 21
});
```

`close` releases the connection.

```ballerina
check ftpClient->close();
```

### 3.2 Transport Options

These fields tune the transport. Each one applies to a subset of the protocols, and is ignored by the others.

`socketConfig` sets the timeouts that apply once a connection is open. All three are in seconds.

```ballerina
public type SocketConfig record {|
    decimal ftpDataTimeout = 120.0;
    decimal ftpSocketTimeout = 60.0;
    decimal sftpSessionTimeout = 300.0;
|};
```

`fileTransferMode` applies to FTP. `BINARY`, the default, transfers bytes unchanged. `ASCII` converts line endings, and corrupts anything that is not text.

```ballerina
public enum FileTransferMode {
    BINARY,
    ASCII
}
```

`sftpCompression` applies to SFTP and lists the compression algorithms to offer, best first. The default offers none.

```ballerina
public enum TransferCompression {
    ZLIB = "zlib",
    ZLIBOPENSSH = "zlib@openssh.com",
    NO = "none"
}
```

`proxy` applies to SFTP and routes the connection through a proxy.

```ballerina
public type ProxyConfiguration record {|
    string host;
    int port;
    ProxyType 'type = HTTP;
    ProxyCredentials auth?;
    string command?;
|};

public enum ProxyType {
    HTTP,
    SOCKS5,
    STREAM
}
```

`command` belongs to `STREAM`, which reaches the server through a jump host by running a local command such as `ssh -W %h:%p jumphost`. `HTTP` and `SOCKS5` ignore it.

### 3.3 Writing Files

| Method | Content |
| --- | --- |
| `putBytes` | `byte[]` |
| `putText` | `string` |
| `putJson` | `json` or `record {}` |
| `putXml` | `xml` or `record {}` |
| `putCsv` | `string[][]` or `record {}[]` |
| `putBytesAsStream` | `stream<byte[], error?>` |
| `putCsvAsStream` | `stream<string[]\|record {}, error?>` |

Every one of them takes an `ftp:FileWriteOption`, which defaults to `OVERWRITE`.

```ballerina
public enum FileWriteOption {
    OVERWRITE,
    APPEND
}
```

A write creates the file when it is not there. `putText` and `putJson` encode as UTF-8.

`record {}` carries an `anydata` rest field, so the record accepted by `putJson` and `putXml` is wider than the content each one writes. `putJson` serializes whatever it is given with `toJsonString`, and `putXml` returns an `ftp:Error` when `xmldata:toXml` cannot convert the record.

`putCsv` writes a header row taken from the record field names when the content is a `record {}[]` and the option is not `APPEND`. Appending a `record {}[]` writes data rows only, so a file built entirely by appends has no header. A `string[][]` never gets a header row; whatever the first row holds is written as-is.

The streaming writes take a stream instead of a value in memory, which is what a file too large to hold needs.

```ballerina
check ftpClient->putText("/uploads/hello.txt", "Hello, World!");
check ftpClient->putText("/logs/app.log", "New log entry\n", ftp:APPEND);
```

```ballerina
stream<io:Block, io:Error?> fileStream = check io:fileReadBlocksAsStream("/local/data.bin", 4096);
check ftpClient->putBytesAsStream("/uploads/data.bin", fileStream);
```

`put` and `append` are the earlier untyped writes. Both are deprecated in favour of the typed methods above.

### 3.4 Reading Files

| Method | Returns |
| --- | --- |
| `getBytes` | `byte[]` |
| `getText` | `string` |
| `getJson` | `json` or `record {}` |
| `getXml` | `xml` or `record {}` |
| `getCsv` | `string[][]` or `record {}[]` |
| `getBytesAsStream` | `stream<byte[], error?>` |
| `getCsvAsStream` | `stream<string[]\|record {}, error?>` |

Reading a path that is not there gives an `ftp:FileNotFoundError`.

The first five read the whole file into memory, and are the operations `retryConfig` retries; see [Section 3.7](#37-retry). The two streaming reads hold the file open until the stream is consumed or closed, so always close them.

`getCsv` and `getCsvAsStream` treat the first row of the file as the header row.

```ballerina
type Order record {|
    int id;
    string item;
|};

Order 'order = check ftpClient->getJson("/data/order.json");
```

```ballerina
stream<Employee, error?> rows = check ftpClient->getCsvAsStream("/reports/employees.csv");
check rows.forEach(function(Employee emp) {
    io:println(emp.name);
});
```

`get` is the earlier untyped read, returning a raw byte stream. It is deprecated in favour of the typed methods above.

### 3.5 Data Binding

`getJson`, `getXml`, `getCsv`, and `getCsvAsStream` bind the content to the type expected at the call site. There is no separate conversion step.

Content that does not match the target type gives an `ftp:ContentBindingError`, whose detail record carries the path and the raw bytes that failed:

```ballerina
public type ContentBindingErrorDetail record {|
    string filePath?;
    byte[] content?;
|};
```

`laxDataBinding` relaxes the match. With it, a JSON or XML null binds to an optional field and an absent field binds to a nilable type; strict binding, the default, rejects both.

`csvFailSafe` applies to `getCsv`. A record that cannot be bound is then skipped and recorded, instead of failing the whole read. `contentType` decides what is recorded for each skipped record.

```ballerina
public type FailSafeOptions record {|
    ErrorLogContentType contentType = METADATA;
|};

public enum ErrorLogContentType {
    METADATA,
    RAW,
    RAW_AND_METADATA
}
```

Skipped records are appended to `<file-name>_error.log` in the working directory of the Ballerina program, not on the server. Under `RAW` and `RAW_AND_METADATA` that file holds the raw text of the skipped records, and so is as sensitive as the data being read.

`csvFailSafe` does not reach `getCsvAsStream`. A record that cannot be bound in a streaming CSV read fails the stream.

### 3.6 File Management

`list` returns an `ftp:FileInfo` for every entry of a directory.

```ballerina
public type FileInfo record {|
    string path;
    int size;
    int lastModifiedTimestamp;
    string name;
    boolean isFolder;
    boolean isFile;
    string pathDecoded;
    string extension;
    string publicURIString;
    string fileType;
    boolean isAttached;
    boolean isContentOpen;
    boolean isExecutable;
    boolean isHidden;
    boolean isReadable;
    boolean isWritable;
    int depth;
    string scheme;
    string uri;
    string rootURI;
    string friendlyURI;
|};
```

`lastModifiedTimestamp` is UNIX epoch time. `extension` carries no leading dot.

| Method | Behaviour |
| --- | --- |
| `mkdir` | Creates a directory, and the directories above it. Fails with `ftp:FileAlreadyExistsError` when the path is taken |
| `rmdir` | **Removes a directory and everything inside it, recursively.** Fails with `ftp:FileNotFoundError` when the path is absent |
| `delete` | Removes a file. Fails with `ftp:FileNotFoundError` when the path is absent |
| `rename` | Moves the file to the destination path |
| `move` | The same operation as `rename` |
| `copy` | Duplicates the file at the destination path |
| `exists` | Reports whether the path is there |
| `size` | Reports the size of the file in bytes |
| `isDirectory` | Reports whether the path is a directory. Fails with `ftp:FileNotFoundError` when the path is absent |

`rename` and `move` are one operation under two names, so either one can move a file to another directory. Both, and `copy`, create the directories leading to the destination when they are absent, and all three fail with `ftp:FileAlreadyExistsError` rather than overwriting an existing destination.

```ballerina
ftp:FileInfo[] files = check ftpClient->list("/incoming");
foreach ftp:FileInfo file in files {
    io:println(string `${file.name} (${file.size} bytes)`);
}
```

### 3.7 Retry

`retryConfig` retries a failed read with exponential backoff. It covers the reads that load the whole file — `getBytes`, `getText`, `getJson`, `getXml`, and `getCsv` — and not the streaming reads, whose transfer happens later, as the stream is consumed.

```ballerina
public type RetryConfig record {|
    int count = 3;
    decimal interval = 1.0;
    decimal backOffFactor = 2.0;
    decimal maxWaitInterval = 30.0;
|};
```

`interval` is the wait before the first retry, in seconds. Each subsequent wait is multiplied by `backOffFactor`, up to `maxWaitInterval`. With the defaults, the waits are 1, 2, and 4 seconds.

When every attempt has failed, the operation returns an `ftp:AllRetryAttemptsFailedError` wrapping the last failure. A `CircuitBreakerOpenError` is not retried, since the circuit is open precisely because retrying is pointless.

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

### 3.8 Circuit Breaker

`circuitBreaker` stops a client from queueing work against a server that is failing. Failures are counted over a rolling window, and once the ratio of failures crosses `failureThreshold`, the circuit opens and every operation returns an `ftp:CircuitBreakerOpenError` at once, without touching the server. It covers every client operation, not only reads.

The circuit has three states.

| State | Behaviour |
| --- | --- |
| `CLOSED` | Operations proceed, and outcomes are counted in the rolling window |
| `OPEN` | Operations fail at once with `ftp:CircuitBreakerOpenError`. No connection is attempted |
| `HALF_OPEN` | Reached after `resetTime` in `OPEN`. One trial operation is allowed: success closes the circuit, failure opens it again |

```ballerina
public type CircuitBreakerConfig record {|
    RollingWindow rollingWindow = {};
    float failureThreshold = 0.5;
    decimal resetTime = 30;
    FailureCategory[] failureCategories = [CONNECTION_ERROR, TRANSIENT_ERROR];
|};

public type RollingWindow record {|
    int requestVolumeThreshold = 10;
    decimal timeWindow = 60;
    decimal bucketSize = 10;
|};
```

`failureThreshold` is a ratio between `0.0` and `1.0`. `resetTime`, `timeWindow`, and `bucketSize` are in seconds, and `bucketSize` must be smaller than `timeWindow`. **The circuit does not open until `requestVolumeThreshold` operations have happened inside the window**, so a threshold crossed by two failures out of two does nothing on its own.

`failureCategories` decides which failures count towards the ratio. Anything outside the listed categories is returned to the caller without moving the circuit.

```ballerina
public enum FailureCategory {
    CONNECTION_ERROR,
    AUTHENTICATION_ERROR,
    TRANSIENT_ERROR,
    ALL_ERRORS
}
```

`CONNECTION_ERROR` covers timeouts, refusals, resets, and DNS failures. `AUTHENTICATION_ERROR` covers a rejected identity. `TRANSIENT_ERROR` covers the FTP replies that mean try again — 421, 425, 426, 450, 451, and 452. `ALL_ERRORS` counts everything, a missing file included, so a client that reads paths which may not exist should not use it.

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

## 4. Listener

### 4.1 Initializing the Listener

The listener configuration is the client configuration plus polling, coordination, and the deprecated monitoring fields.

```ballerina
public type ListenerConfiguration record {|
    Protocol protocol = FTP;
    string host = "127.0.0.1";
    int port = 21;
    AuthConfiguration auth?;
    @deprecated
    string path = "/";
    @deprecated
    string fileNamePattern?;
    decimal pollingInterval = 60;
    boolean userDirIsRoot = false;
    @deprecated
    FileAgeFilter fileAgeFilter?;
    @deprecated
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
    RetryConfig retryConfig?;
|};
```

`pollingInterval` is the number of seconds between polls. On each cycle the listener polls the watched directory of every attached service, and compares what it finds with the previous cycle: a path that has appeared is a new file, and a path that has gone is a deleted file.

The four monitoring fields — `path`, `fileNamePattern`, `fileAgeFilter`, and `fileDependencyConditions` — are deprecated at this level. They belong on `@ftp:ServiceConfig`, which is what [Section 4.2](#42-service) covers.

`retryConfig` here retries the read the listener does to get file content before binding it, on the same terms as on the client.

```ballerina
listener ftp:Listener ftpListener = check new ({
    protocol: ftp:SFTP,
    host: "sftp.example.com",
    port: 22,
    auth: {credentials: {username: "alice", password: "***"}},
    pollingInterval: 30,
    userDirIsRoot: true
});
```

`start`, `attach`, `detach`, `gracefulStop`, and `immediateStop` return a plain `error?`. Configuration the listener can only check once services are attached — a duplicate path, a bad regular expression, an inconsistent age filter — fails at `start`, with an `ftp:InvalidConfigError`.

### 4.2 Service

A service attached to a listener watches one directory. `@ftp:ServiceConfig` names it and says which of its files matter.

```ballerina
public type ServiceConfiguration record {|
    string path;
    string fileNamePattern?;
    FileAgeFilter fileAgeFilter?;
    FileDependencyCondition[] fileDependencyConditions = [];
|};
```

`path` is required, and is taken as a path from the server root; a leading `/` is added when it is missing. Several services may attach to one listener, each watching a different directory. **Two services on one listener may not watch the same path**, and the second one fails with an `ftp:InvalidConfigError`.

```ballerina
@ftp:ServiceConfig {
    path: "/incoming/orders",
    fileNamePattern: ".*\\.csv"
}
service on ftpListener {
    remote function onFileCsv(record {}[] content, ftp:FileInfo fileInfo) returns error? {
    }
}

@ftp:ServiceConfig {
    path: "/incoming/configs",
    fileNamePattern: ".*\\.json"
}
service on ftpListener {
    remote function onFileJson(json content, ftp:FileInfo fileInfo) returns error? {
    }
}
```

The annotation is all-or-nothing per listener. **If any service on a listener carries `@ftp:ServiceConfig`, every service on that listener must carry it**, and mixing the two fails with an `ftp:InvalidConfigError`. A listener whose services all go without it falls back to the deprecated `path` and filtering fields of the listener configuration, and watches one directory for all of them. Setting both is not an error: the annotation wins, and a deprecation warning is logged for the listener-level fields.

A service must declare at least one handler. The handler methods of a service, and its `ftp:Caller`, are resolved when the service is attached, not once per file.

### 4.3 Content Handlers

A service declares one or more content handlers. The listener reads the file, binds the content, and passes it as the **first** parameter. A handler never reads the file itself.

| Handler | Content parameter |
| --- | --- |
| `onFileText` | `string` |
| `onFileJson` | `json` or `record {}` |
| `onFileXml` | `xml` or `record {}` |
| `onFileCsv` | `string[][]`, `record {}[]`, `stream<string[], error?>`, or `stream<record {}, error?>` |
| `onFile` | `byte[]`, or a `stream<byte[], error?>` |

Declaring a stream as the content parameter of `onFileCsv` or `onFile` streams the file instead of holding it in memory. **A handler that declares a stream must consume or close it**, since the file stays open until the stream reaches its end or is closed.

After the content parameter, a handler may declare an `ftp:FileInfo` parameter, then an `ftp:Caller` parameter. Both are optional, but **the order is fixed**: `ftp:FileInfo` second, `ftp:Caller` third. A handler returns `error?` or `ftp:Error?`.

```ballerina
remote function onFileJson(Config content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
}
```

`onFileDelete` gets the path of a file that has gone from the watched directory since the previous poll, and may declare an optional `ftp:Caller` second parameter.

```ballerina
remote function onFileDelete(string deletedFile, ftp:Caller caller) returns error? {
    io:println("Deleted: " + deletedFile);
}
```

`onFileDeleted` is its predecessor, taking a `string[]` of paths rather than one path. It is deprecated in favour of `onFileDelete`, and declaring both is a compile error.

A file with no handler for it is left alone, and the poll that found it logs a warning.

### 4.4 Handler Selection

A file is routed by a pattern on a handler first, and by its extension second.

A handler that carries `fileNamePattern` on `@ftp:FunctionConfig` claims every file whose whole name matches that regular expression, whatever the extension says. **When two handlers carry patterns that both match a file, which one gets it is not defined**, so patterns on one service should not overlap.

Otherwise the extension decides, case-insensitively.

| Extension | Handler |
| --- | --- |
| `txt`, `log`, `md` | `onFileText` |
| `json` | `onFileJson` |
| `xml` | `onFileXml` |
| `csv` | `onFileCsv` |
| any other, or none | `onFile` |

When the handler for an extension is not declared, the file goes to `onFile`. A file reaches at most one handler.

### 4.5 File Filtering

Filtering decides which files the listener picks up at all, and is separate from the routing of [Section 4.4](#44-handler-selection). A file the filters reject reaches no handler.

`fileNamePattern` on `@ftp:ServiceConfig` is a Java regular expression matched against the whole file name. Only files that match are picked up. Without it, every file in the directory is picked up. A `fileNamePattern` on a handler narrows this no further; the active service-level pattern, or the deprecated listener-level pattern when no service annotation is used, has already decided what the poll sees.

`fileAgeFilter` skips files by age.

```ballerina
public type FileAgeFilter record {|
    decimal minAge?;
    decimal maxAge?;
    AgeCalculationMode ageCalculationMode = LAST_MODIFIED;
|};

public enum AgeCalculationMode {
    LAST_MODIFIED,
    CREATION_TIME
}
```

Both bounds are in seconds and inclusive, and either may be set alone. `minAge` is what keeps a file still being written by an upstream process from being picked up half-finished. `ageCalculationMode` chooses the timestamp the age is measured from; `CREATION_TIME` needs a server that reports one. A negative bound, or a `minAge` above `maxAge`, fails at listener start with an `ftp:InvalidConfigError`.

`fileDependencyConditions` holds a file back until the files it belongs with have arrived too.

```ballerina
public type FileDependencyCondition record {|
    string targetPattern;
    string[] requiredFiles;
    DependencyMatchingMode matchingMode = ALL;
    int requiredFileCount = 1;
|};

public enum DependencyMatchingMode {
    ALL,
    ANY,
    EXACT_COUNT
}
```

A file whose name matches `targetPattern` is picked up only once `requiredFiles` are present. `ALL` needs every required pattern matched, `ANY` needs one, and `EXACT_COUNT` needs exactly `requiredFileCount` of them. A capture group of `targetPattern` may be referenced in a required pattern as `$1`, `$2`, and so on, which is what ties a data file to its own marker rather than to any marker.

```ballerina
@ftp:ServiceConfig {
    path: "/incoming/orders",
    fileNamePattern: "order_.*\\.csv",
    fileAgeFilter: {minAge: 30},
    fileDependencyConditions: [
        {
            targetPattern: "order_(\\d+)\\.csv",
            requiredFiles: ["order_$1.marker"],
            matchingMode: ftp:ALL
        }
    ]
}
service on ftpListener {
    remote function onFileCsv(record {}[] content, ftp:FileInfo fileInfo) returns error? {
    }
}
```

### 4.6 Post-Processing Actions

`@ftp:FunctionConfig` says what becomes of the file once the handler has run.

```ballerina
public type FtpFunctionConfig record {|
    string fileNamePattern?;
    MOVE|DELETE afterProcess?;
    MOVE|DELETE afterError?;
|};
```

`afterProcess` applies when the handler returns successfully, and `afterError` when it returns an error or panics. A file whose applicable action is not set stays where it is, and is picked up again on the next poll. At most one action applies to a file.

```ballerina
public const DELETE = "DELETE";

public type Move record {|
    string moveTo;
    boolean preserveSubDirs = true;
|};

public type MOVE Move;
```

`DELETE` is a constant, and removes the file. `MOVE` is an alias for the `Move` record, and relocates the file under `moveTo`, creating the directories leading to the destination when they are absent. `preserveSubDirs`, on by default, recreates the file's subdirectory structure relative to the watched directory under the destination — watching `/input` and processing `/input/orders/2026/file.csv` with `moveTo: "/archive"` puts it at `/archive/orders/2026/file.csv`. With `preserveSubDirs: false` it lands directly in `moveTo`, which collapses two same-named files from different subdirectories onto one destination path. An empty `moveTo` fails at listener start with an `ftp:InvalidConfigError`.

```ballerina
service on ftpListener {
    @ftp:FunctionConfig {
        afterProcess: {moveTo: "/archive/success"},
        afterError: {moveTo: "/archive/failed"}
    }
    remote function onFileXml(xml content, ftp:FileInfo fileInfo) returns error? {
        check processXml(content);
    }

    @ftp:FunctionConfig {
        afterProcess: ftp:DELETE
    }
    remote function onFileJson(json content, ftp:FileInfo fileInfo) returns error? {
        processJson(content);
    }
}
```

A post-processing action runs on the service's `ftp:Caller`, so a service that declares one gets a caller connection whether or not any handler asks for it. **A failing action is logged, and reaches neither the handler nor `onError`**, so a file whose move fails stays in the watched directory and is processed again on the next poll.

A handler that moves or deletes the file itself and also declares `afterProcess` leaves the listener acting on a path that is no longer there.

### 4.7 Error Handling

`onError` is called when file content cannot be bound to the handler's content parameter. It takes the error first — as `ftp:Error` or `error` — and an optional `ftp:Caller` second.

```ballerina
remote function onError(ftp:Error err, ftp:Caller caller) returns error? {
    if err is ftp:ContentBindingError {
        string? filePath = err.detail().filePath;
        log:printError("Binding failed", err);
        if filePath is string {
            check caller->move(filePath, "/error/" + filePath);
        }
    }
}
```

The error is an `ftp:ContentBindingError`, whose detail record carries the `filePath` and the raw `content` as bytes, so a handler can quarantine or inspect the file that failed.

**`onError` is not a general error handler.** An error the handler itself returns does not reach it — that is what `afterError` is for. Nor does a failure to read the file, a failure in a post-processing action, or a failed poll; those are logged.

Which post-processing action follows a binding failure depends on whether `onError` is declared.

| `onError` | What runs after the failure |
| --- | --- |
| Not declared | The error is logged, and the **content handler's** `afterError` runs |
| Declared | `onError` runs, and then the action on **`onError`'s own** `@ftp:FunctionConfig` — `afterProcess` when `onError` succeeded, `afterError` when it failed |

So declaring `onError` takes the content handler's `afterError` out of the binding-failure path. A service that wants the file quarantined either way puts the action on `onError` too, or moves the file from inside `onError`.

```ballerina
service on ftpListener {
    @ftp:FunctionConfig {
        afterProcess: {moveTo: "/archive"},
        afterError: {moveTo: "/failed"}
    }
    remote function onFileCsv(record {}[] content) returns error? {
    }

    // Without this, a malformed CSV would go to /failed. With it, /malformed.
    @ftp:FunctionConfig {
        afterProcess: {moveTo: "/malformed"}
    }
    remote function onError(ftp:Error err) returns error? {
        log:printError("Binding failed", err);
    }
}
```

### 4.8 Distributed Coordination

`coordination` lets several listener instances share one watched directory without processing the same file twice. Members of a group elect one active member through a shared database; the active member polls, and the rest wait.

```ballerina
public type CoordinationConfig record {|
    task:DatabaseConfig databaseConfig = <task:MysqlConfig>{};
    int livenessCheckInterval = 30;
    string memberId;
    string coordinationGroup;
    int heartbeatFrequency = 1;
|};
```

Every member of a group is configured with the same `coordinationGroup` and its own `memberId`. `databaseConfig` is a MySQL or PostgreSQL database, and all members must point at the same one.

The active member writes a heartbeat every `heartbeatFrequency` seconds. Standby members check it every `livenessCheckInterval` seconds, and when it has gone stale, one of them takes over and starts polling. A standby member's polling cycle does nothing at all, so its services see no events until it becomes active. Both intervals are in seconds; `livenessCheckInterval` sets how long a failover takes to notice, and should stay comfortably above `heartbeatFrequency`.

```ballerina
listener ftp:Listener ftpListener = check new ({
    protocol: ftp:SFTP,
    host: "sftp.example.com",
    port: 22,
    auth: {credentials: {username: "alice", password: "***"}},
    coordination: {
        memberId: "node-1",
        coordinationGroup: "ftp-processors",
        livenessCheckInterval: 30,
        heartbeatFrequency: 1,
        databaseConfig: <task:MysqlConfig>{
            host: "db.example.com",
            user: "dbuser",
            password: "***",
            database: "coordination_db"
        }
    }
});
```

### 4.9 The Event Handler (Deprecated)

`onFileChange` is the original handler, and gets the whole result of a poll rather than one file.

```ballerina
public type WatchEvent record {|
    FileInfo[] addedFiles;
    string[] deletedFiles;
|};
```

It takes an `ftp:WatchEvent` or `ftp:WatchEvent & readonly` first, and an optional `ftp:Caller` second. It reads and binds nothing: a service using it fetches content itself, through the caller.

```ballerina
service ftp:Service on ftpListener {
    remote function onFileChange(ftp:WatchEvent & readonly event, ftp:Caller caller) returns error? {
        foreach ftp:FileInfo file in event.addedFiles {
            io:println("New file: " + file.path);
        }
    }
}
```

It is deprecated in favour of the content handlers of [Section 4.3](#43-content-handlers), and **cannot be combined with them** — a service declares either `onFileChange` or content handlers, and mixing the two is a compile error. Post-processing actions and `onError` do not apply to it.

## 5. Caller

An `ftp:Caller` declared as a handler parameter lets a handler act on the server while processing a file. It cannot be constructed directly.

The caller offers the same operations as the client, less `close`: the write, read, and file management methods, including the deprecated `get`, `put`, and `append`. Its read operations bind to the type expected at the call site, exactly as the client's do. It inherits the listener's protocol, identity, and transport settings.

A listener creates a caller only when something needs one: a handler that declares an `ftp:Caller` parameter, or a service with post-processing actions. How many callers exist depends on how the services are configured. With `@ftp:ServiceConfig`, each service gets its own caller connection, rooted at that service's watched path. Without it, one caller is created and shared by every service on the listener. Either way a listener that has callers holds one connection for polling plus one per caller.

```ballerina
service on ftpListener {
    remote function onFileText(string content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
        processContent(content);
        check caller->move(fileInfo.path, "/processed/" + fileInfo.name);
    }
}
```

The caller belongs to the listener, which closes it when it stops.

## 6. Errors

The library defines a hierarchy rooted at `ftp:Error`, so a caller can handle a failure at whatever level of specificity it wants.

```ballerina
public type Error distinct error;
```

| Error | Means |
| --- | --- |
| `Error` | The base type. Every error below is a subtype |
| `ConnectionError` | The server could not be reached — network failure, unreachable host, refused connection |
| `FileNotFoundError` | The path is not on the server |
| `FileAlreadyExistsError` | The destination of a `mkdir`, `rename`, `move`, or `copy` is taken |
| `InvalidConfigError` | A configuration value is unusable — a bad port, regular expression, timeout, path, or filter bound |
| `ServiceUnavailableError` | A transient server failure that may succeed on retry — overload (421), data connection trouble (425, 426), a temporary lock (450), or a server-side processing failure (451) |
| `ContentBindingError` | File content could not be parsed or bound to the expected type. Its detail record carries the `filePath` and the raw `content` |
| `AllRetryAttemptsFailedError` | Every retry attempt failed. It wraps the last failure |
| `CircuitBreakerOpenError` | A subtype of `ServiceUnavailableError`, returned while the circuit is open |

Every client and caller operation that can fail returns an `ftp:Error`. The listener lifecycle methods `start`, `attach`, `detach`, `gracefulStop`, and `immediateStop` return a plain `error?`, and also propagate errors raised by the task scheduler the listener polls with.

Check the specific types before the general ones — `CircuitBreakerOpenError` before `ServiceUnavailableError`, and both before `Error`.

```ballerina
byte[]|ftp:Error result = ftpClient->getBytes("/data/file.txt");
if result is ftp:CircuitBreakerOpenError {
    applyFallback();
} else if result is ftp:FileNotFoundError {
    log:printWarn("File not found");
} else if result is ftp:ConnectionError {
    log:printError("Connection failed", result);
} else if result is ftp:Error {
    log:printError("FTP operation failed", result);
} else {
    processBytes(result);
}
```

## 7. Observability

The FTP library provides built-in observability support through metrics and distributed tracing, following the unified observability specification for Ballerina file integration libraries. When observability is enabled in the Ballerina runtime, the FTP client and listener automatically report telemetry data without any additional configuration.

The observability model is module-agnostic: the FTP module publishes the same metric names and tag keys as other file integration modules (SMB, S3, etc.). Module-specific values appear only in tag values (e.g. `module=ftp`), never in metric names.

### 7.1 Metrics

#### 7.1.1 Gauges

| Metric Name | Type | Description |
|---|---|---|
| `ftp_active_connections` | Gauge | Number of active FTP/FTPS/SFTP client and listener objects. Incremented on init, decremented on close. Retained for backward compatibility. |

#### 7.1.2 Explicit Counters

| Metric Name | Type | Description |
|---|---|---|
| `file_bytes_transferred_total` | Counter | Total bytes read or written across operations. A sum of bytes, not a count of spans. |
| `file_events_total` | Counter | Total file lifecycle and poll events. Distinguishable by `action.type` tag: `poll_cycle` for poll cycles, `file_event` for file lifecycle stages. |

`file_bytes_transferred_total` is incremented for all non-streaming client read operations (`getBytes`, `getText`, `getJson`, `getXml`, `getCsv`), all client write operations (`putBytes`, `putText`, `putJson`, `putXml`, `putCsv`), and listener content reads during file processing. Every increment carries an `operation.type` tag (`get` or `put`) so that total bytes read across both client and listener can be queried uniformly via `file_bytes_transferred_total{operation_type="get"}`.

`file_events_total` is a single counter that tracks both poll cycles (`action.type=poll_cycle`) and all four file lifecycle stages (`action.type=file_event`, `file.stage=found|dispatched|handled|cleaned_up`). Each event produces an independent `+1` to the counter with its respective tags. Poll cycles are derived via `file_events_total{action_type="poll_cycle"}`; file stages via `file_events_total{file_stage="..."}`. This ensures everything is queryable from a single metric name.

For stages that go through `callMethod` (handled, cleaned_up), the framework additionally creates auto-instrumented spans that appear in distributed traces (Jaeger). However, `requests_total_value` is the **runtime's** metric — it counts one increment per span, not per file. A single poll that finds 50 files still produces only one span. Therefore, per-file counts must always come from `file_events_total`, not from `requests_total_value`. The only valid use of `requests_total_value` is for **client operations**, where one method call (e.g. `getBytes()`) equals one span equals one increment.

#### 7.1.3 Duration Gauges

| Metric Name | Type | Description |
|---|---|---|
| `file_databinding_duration_seconds` | Gauge (distribution) | Time in seconds to fetch and convert file content into the target type (JSON, XML, CSV, text, bytes, stream). Each invocation records a separate observation into a sliding-window distribution. |
| `file_resource_execution_duration_seconds` | Gauge (distribution) | Time in seconds to execute the user's resource/handler method. Each invocation records a separate observation into the same sliding-window distribution. |

Both duration gauges are configured with a `StatisticConfig` that tracks p50, p75, p90, p95, and p99 percentiles over a 5-minute sliding window. They carry `handler.name`, `outcome`, `protocol`, and `remote.url` tags.

- **`file_databinding_duration_seconds`** covers the full data binding pipeline: resolving the remote file, reading bytes over the network, and converting to the handler's parameter type (e.g. `json`, `xml`, `csv` record). For streaming handlers, this measures stream creation time only — actual data transfer is lazy.
- **`file_resource_execution_duration_seconds`** covers the actual elapsed time of the handler method invocation. This includes everything inside the user's handler — FTP operations, HTTP calls, database queries, custom logic, etc.

#### 7.1.4 Querying Metrics

The following table shows the recommended PromQL source for each logical metric:

| Logical Metric | Description | PromQL Source |
|---|---|---|
| Poll cycles | Poll cycles completed by a listener | `file_events_total{action_type="poll_cycle"}` |
| Files found | Files discovered during a poll | `file_events_total{file_stage="found"}` |
| Files dispatched | Files matched to a handler and handed over | `file_events_total{file_stage="dispatched"}` |
| Files skipped | Files found but matched no handler | `file_events_total{file_stage="found", outcome="skipped"}` |
| Files handled | Handler invocations completed | `file_events_total{file_stage="handled"}` |
| Files cleaned up | Post-processing actions completed (move/delete) | `file_events_total{file_stage="cleaned_up"}` |
| Handler errors | Errors from any code inside handler (FTP, HTTP, DB, etc.) | `file_events_total{file_stage="handled", outcome="failure"}` |
| Data binding duration | Time to fetch and convert file content | `file_databinding_duration_seconds` |
| Resource execution duration | Time to execute the handler method | `file_resource_execution_duration_seconds` |
| Bytes read (client + listener) | Total bytes read across all get operations | `file_bytes_transferred_total{operation_type="get"}` |
| Bytes written (client) | Total bytes written across all put operations | `file_bytes_transferred_total{operation_type="put"}` |
| Client operations | Client-initiated file operations (get, put, manage) | `requests_total_value{action_type="client_operation"}` |

### 7.2 Tags

All metrics and trace spans carry tags that identify the connection, operation, and lifecycle stage.

> **Note:** Prometheus normalizes `.` to `_` in label names (e.g. `action.type` → `action_type`, `file.stage` → `file_stage`). Jaeger and other trace backends preserve the original dotted names. The PromQL examples in this section use the Prometheus-normalized form.

#### 7.2.1 Identity Tags

| Tag | Values | Metrics | Traces | Notes |
|---|---|---|---|---|
| `module` | `ftp` | Yes | Yes | Identifies the Ballerina module. Always `ftp` regardless of wire protocol (FTP, FTPS, SFTP). |
| `protocol` | `ftp`, `ftps`, `sftp` | Yes | Yes | The wire protocol. |
| `type` | `client`, `listener` | Yes | Yes | Whether this is a client or listener operation. |
| `remote.url` | `host:port` | Yes | Yes | The server endpoint. Added on every observer context at construction time, from the connection configuration. Does not include the protocol prefix since `protocol` is a separate tag. |
| `watched.path` | Monitored directory path (e.g. `/uploads`) | Yes | Yes | Present on listener events and poll cycles. Distinguishes services monitoring different paths on the same server. |
| `host` | Local hostname | Yes | Yes | Hostname of the current instance. |

#### 7.2.2 Action Tags

These tags capture the sequence of events during a file's journey. They help map out the lifecycle stages, showing exactly how a file moves through the system.

| Tag | Values | Metrics | Traces | Notes |
|---|---|---|---|---|
| `action.type` | `poll_cycle`, `file_event`, `client_operation` | Yes | Yes | `poll_cycle` — Added on each poll cycle completion (`file_events_total` counter). One entry per poll, regardless of how many files are found. Only applicable to poll-based modules. `file_event` — Added on listener file lifecycle events (found, dispatched, handled, cleaned_up, skipped). `client_operation` — Added on client API calls. |
| `file.stage` | `found`, `dispatched`, `handled`, `cleaned_up` | Yes | Yes | Maps to the four-stage file lifecycle. Present on listener event spans and metrics. `found` — Added when a file is first discovered during a poll cycle. Every discovered file gets this, including files that will be skipped as no relevant handler found for that. `dispatched` — Added when the file is matched to a content handler and handed over for processing. `handled` — Added when the handler invocation completes. `cleaned_up` — Added when a post-processing action completes. Only present when `afterProcess` or `afterError` is configured. |
| `event.type` | `create`, `delete`, `error` | Yes | Yes | Type of listener event. `create` — File was added or modified. Added on handler invocation spans for content-based callbacks. `delete` — File was deleted. Added on `onFileDelete` handler spans. `error` — Content-binding failure. Added on `onError` handler spans. |
| `operation.type` | `get`, `put`, `manage` | Yes | Yes | Present on client operation spans (`action.type=client_operation`) and on `file_bytes_transferred_total` for both client and listener. `get` — Added on `getBytes()`, `getText()`, `getJson()`, `getXml()`, `getCsv()`, `getBytesAsStream()`, `getCsvAsStream()`. `put` — Added on `putBytes()`, `putText()`, `putJson()`, `putXml()`, `putCsv()`, `putBytesAsStream()`, `putCsvAsStream()`. `manage` — Added on `delete()`, `rename()`, `move()`, `copy()`, `mkdir()`, `rmdir()`, `isDirectory()`, `list()`, `exists()`, `size()`. |
| `handler.name` | Handler method name (e.g. `onFileJson`, `onFileCsv`) | Yes | Yes | Identifies which handler processed the file. Added on: `file.stage=dispatched` — when the handler is selected. `file.stage=handled` — when the handler completes. `file.stage=cleaned_up` — to link cleanup back to the handler that triggered it. Not present on `file.stage=found` (handler not yet determined) or skipped files. |
| `cleanup.action` | `move`, `delete` | Yes | Yes | `move` — When the file was moved to a destination directory. `delete` — When the file was deleted. Added on `file.stage=cleaned_up` events only. |

#### 7.2.3 Outcome Tags

| Tag | Values | Metrics | Traces | Notes |
|---|---|---|---|---|
| `outcome` | `success`, `failure`, `skipped` | Yes | Yes | Result of an operation. `skipped` indicates a file found but not matched to any handler. |
| `error.type` | `ConnectionError`, `AuthenticationError`, `FileNotFoundError`, `ContentBindingError`, `CloseError`, `no_handler_matched`, `binding_failed`, `move_failed`, `delete_failed`, etc. | Yes | Yes | Present when `outcome=failure` or `outcome=skipped`. Set to the Ballerina error type name for handler and client errors (which can be **any** error type — not just FTP errors, e.g. `ClientError` from HTTP, `ApplicationError` from DB). For lifecycle failures, predefined values are used: `no_handler_matched`, `binding_failed`, `move_failed`, `delete_failed`. Set to `none` when not applicable. |

#### 7.2.4 Tag Consistency Rule

Every increment of a given metric must carry the **same set of label keys**. Prometheus treats a series with labels `{a, b}` and a series with labels `{a, b, c}` as two different time series, even under the same metric name. If tags are conditionally absent, queries like `sum by (file_stage)` silently drop or double-count rows.

When a tag is not applicable for a given stage, the sentinel value `"none"` is used instead of omitting the tag. For example, `file_events_total` always carries `outcome`, `error.type`, `handler.name`, and `watched.path` on every increment — set to `"none"` when not applicable:

```
file_events_total{file_stage="found",   outcome="none",    error_type="none",            handler_name="none"}
file_events_total{file_stage="handled", outcome="success", error_type="none",            handler_name="onFileJson"}
file_events_total{file_stage="handled", outcome="failure", error_type="ConnectionError", handler_name="onFileJson"}
```

This rule applies to all explicit counters and gauges published by the library. All modules sharing the observability vocabulary must use the same sentinel value.

#### 7.2.5 File-Scoped Tags (Trace-Only)

| Tag | Values | Metrics | Traces | Notes |
|---|---|---|---|---|
| `file.path` | Full path of the file | No | Yes | Excluded from metrics to avoid cardinality explosion. |
| `destination.path` | Target path for move/rename/copy | No | Yes | Excluded from metrics. |
| `file.size` | Size in bytes | No | Yes | Exact file size on the trace span. |
| `file.modified_time` | Last-modified timestamp | No | Yes | Needed for stable file identity. |

#### 7.2.6 Client Operation Tag Mapping

Client operation spans use `type=client` and `action.type=client_operation`. The `operation.type` tag maps to the client method invoked:

| `operation.type` | Triggered by |
|---|---|
| `get` | `getBytes()`, `getText()`, `getJson()`, `getXml()`, `getCsv()`, `getBytesAsStream()`, `getCsvAsStream()` |
| `put` | `putBytes()`, `putText()`, `putJson()`, `putXml()`, `putCsv()`, `putBytesAsStream()`, `putCsvAsStream()` |
| `manage` | `delete()`, `rename()`, `move()`, `copy()`, `mkdir()`, `rmdir()`, `isDirectory()`, `list()`, `exists()`, `size()` |

Listener content reads also carry `operation.type=get` on the `file_bytes_transferred_total` counter, since the listener fetches file content from the remote server in the same way as client get operations.

#### 7.2.7 Listener Event Tag Mapping

Listener event spans use `type=listener` and `action.type=file_event`. The `event.type` tag identifies the event:

| `event.type` | Triggered by |
|---|---|
| `create` | File added or modified; dispatched to format-specific callbacks or `onFileChange` |
| `delete` | File deleted; dispatched to `onFileDelete` |
| `error` | Content-binding or deserialization failure; dispatched to `onError` |

#### 7.2.8 File Lifecycle Stages

The listener tracks files through a four-stage lifecycle. Each stage publishes an independent `file_events_total` counter increment with its respective tags.

1. **Found** (`file.stage=found`) — A file is discovered during a poll cycle. Each discovered file produces exactly one `found` increment. If the file matches a handler, `outcome=none`. If no handler matches, `outcome=skipped` and `error.type=no_handler_matched` — the file goes no further in the lifecycle.
2. **Dispatched** (`file.stage=dispatched`) — The file is matched to a content handler and handed over for processing. The `handler.name` tag identifies the target handler.
3. **Handled** (`file.stage=handled`) — The handler invocation has completed. Tagged with `outcome=success` or `outcome=failure`. On failure, `error.type` is set to the Ballerina error type name returned by the handler. This captures errors from **any** code inside the handler — not just FTP operations, but also HTTP calls, database queries, custom logic, etc. Any error that causes the handler to return an error (via `check` or explicit `return error(...)`) is captured.
4. **Cleaned up** (`file.stage=cleaned_up`) — Post-processing (move or delete) has completed. Tagged with `cleanup.action` (move/delete) and `outcome` (success/failure). If the cleanup fails, `error.type` is set to `move_failed` or `delete_failed`.

### 7.3 Observability Outputs per File

For each file processed by the listener, the library produces three types of observability output:

1. **Explicit counters** (`file_events_total`) — All four lifecycle stages publish here. These are direct `MetricRegistry.counter().increment()` calls. No span is involved. Every stage is queryable from this single metric name.

2. **Per-file parent span** — A library-created span (`BSpan.start("ftp", "file-lifecycle", false)`) that covers the entire file lifecycle from discovery to cleanup. The `file.path` tag on this span enables searching for a specific file in Jaeger. The `handled` and `cleaned_up` child spans are automatically parented to it via the `ObserverContext.setParent()` mechanism.

3. **Framework child spans** (Jaeger traces) — The `handled` and `cleaned_up` stages invoke Ballerina methods via `callMethod`, which creates auto-instrumented spans. Because the strand properties contain an `ObserverContext` whose parent has the per-file span set on it, these auto-instrumented spans become **children** of the parent span. This connects the entire file lifecycle into a single trace. Note: these spans also increment the runtime's `requests_total_value`, but that metric counts spans, not files — it must not be used for per-file counting.

The per-file flow:

```
processContentCallbacks() — for each file:
  │
  │  [ftp / file-lifecycle] ─────────────────────────────── parent span (file.path tag)
  │    │
  │    ├─ file_events_total{file_stage="found"}            ← counter +1
  │    │
  │    ├─ file_events_total{file_stage="dispatched"}        ← counter +1
  │    │
  │    ├─ convertFileContent(onFileText) ───────────────────← timed
  │    │   │
  │    │   └─ file_databinding_duration_seconds                     ← gauge (seconds, with handler_name + outcome)
  │    │
  │    ├─ [onFileText] ────────────────────────────────────── child span (handled)
  │    │   │
  │    │   ├─ file_events_total{file_stage="handled"}       ← counter +1 (with outcome + error_type)
  │    │   └─ file_resource_execution_duration_seconds              ← gauge (seconds, with handler_name + outcome)
  │    │
  │    ├─ [delete|move] ───────────────────────────────────── child span (cleaned_up)
  │    │   │
  │    │   └─ file_events_total{file_stage="cleaned_up"}    ← counter +1 (with outcome + error_type)
  │    │
  │    └─ finishSpan() ──────────────────────────────────── parent span closed
```

For a skipped file (no handler matched), no parent span is created:

```
  └─ file_events_total{file_stage="found", outcome="skipped"}     ← counter +1 (no further stages)
```

- **Poll cycles** are reported via `file_events_total{action_type="poll_cycle"}` because `poll()` is not auto-instrumented.
- **Client operations** produce auto-instrumented spans with `action.type=client_operation`, visible in both `requests_total_value` and Jaeger. This is the only case where `requests_total_value` gives correct per-operation counts (one call = one span = one increment).

### 7.4 Sample PromQL Queries

All lifecycle stages can be queried uniformly from `file_events_total`:

```promql
# ── Poll health ──
rate(file_events_total{action_type="poll_cycle", outcome="success"}[5m])
rate(file_events_total{action_type="poll_cycle", outcome="failure"}[5m])

# ── File lifecycle stages (all from file_events_total) ──
rate(file_events_total{file_stage="found"}[5m])
rate(file_events_total{file_stage="dispatched"}[5m])
rate(file_events_total{file_stage="found", outcome="skipped"}[5m])
rate(file_events_total{file_stage="handled"}[5m])
rate(file_events_total{file_stage="cleaned_up"}[5m])

# ── Handler outcomes (four-box grid) ──
rate(file_events_total{file_stage="handled", outcome="success"}[5m])
rate(file_events_total{file_stage="handled", outcome="failure"}[5m])
rate(file_events_total{file_stage="cleaned_up", outcome="success"}[5m])
rate(file_events_total{file_stage="cleaned_up", outcome="failure"}[5m])

# ── Per-handler breakdown ──
sum by (handler_name) (rate(file_events_total{file_stage="handled"}[5m]))

# ── Handler errors by error type (captures errors from any code — FTP, HTTP, DB, etc.) ──
sum by (error_type) (rate(file_events_total{file_stage="handled", outcome="failure"}[5m]))

# ── Cleanup errors by type ──
sum by (error_type) (rate(file_events_total{file_stage="cleaned_up", outcome="failure"}[5m]))

# ── Data binding duration (p99 by handler) ──
file_databinding_duration_seconds{quantile="0.99"}
avg by (handler_name) (file_databinding_duration_seconds_mean)
file_databinding_duration_seconds{handler_name="onFileJson", outcome="success", quantile="0.5"}

# ── Resource execution duration (p99 by handler) ──
file_resource_execution_duration_seconds{quantile="0.99"}
avg by (handler_name) (file_resource_execution_duration_seconds_mean)
file_resource_execution_duration_seconds{handler_name="onFileJson", outcome="success", quantile="0.5"}

# ── Client operations (framework auto-instrumented spans) ──
rate(requests_total_value{action_type="client_operation", operation_type="get"}[5m])
rate(requests_total_value{action_type="client_operation", operation_type="put"}[5m])
rate(requests_total_value{action_type="client_operation", operation_type="manage"}[5m])

# ── Bytes transferred ──
rate(file_bytes_transferred_total{operation_type="get"}[5m])   # total bytes read (client + listener)
rate(file_bytes_transferred_total{operation_type="put"}[5m])   # total bytes written (client only)
rate(file_bytes_transferred_total{type="client"}[5m])          # all client bytes (get + put)
rate(file_bytes_transferred_total{type="listener"}[5m])        # all listener bytes (get)
```

### 7.5 Enabling Observability

Observability must be enabled in the Ballerina runtime configuration. Add the following to `Config.toml`:

```toml
[ballerina.observe]
metricsEnabled=true
metricsReporter="prometheus"
tracingEnabled=true
tracingProvider="jaeger"
```

Refer to the [Ballerina Observability documentation](https://ballerina.io/learn/observe-ballerina-programs/) for details on configuring reporters and exporters.

### 7.6 Observability Safety Rules

Observability must never break file operations. All metric and tracing calls are guarded by the following rules:

1. **Exception isolation.** Every public method in the metrics and tracing utilities wraps its body in `try/catch(Throwable)` and swallows the exception with a debug-level log. A registry error, NPE, or any other observability failure must never propagate to callers. This follows the same pattern as `module-ballerina-sql`.

2. **Early guard.** All metric methods check `ObserveUtils.isMetricsEnabled()` and return immediately when metrics are disabled. Tracing factory methods check `ObserveUtils.isObservabilityEnabled()` and return `null`. This ensures zero overhead when observability is off.

3. **Null-safe returns.** Tracing methods that return strand property maps return `null` on failure — the same value returned when observability is disabled. Callers already handle `null` (the `StrandMetadata` constructor accepts it), so no caller changes are required.
