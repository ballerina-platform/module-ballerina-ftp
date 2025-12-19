Ballerina FTP Library
=====================

  [![Build](https://github.com/ballerina-platform/module-ballerina-ftp/actions/workflows/build-timestamped-master.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerina-ftp/actions/workflows/build-timestamped-master.yml)
  [![codecov](https://codecov.io/gh/ballerina-platform/module-ballerina-ftp/branch/master/graph/badge.svg)](https://codecov.io/gh/ballerina-platform/module-ballerina-ftp)
  [![Trivy](https://github.com/ballerina-platform/module-ballerina-ftp/actions/workflows/trivy-scan.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerina-ftp/actions/workflows/trivy-scan.yml)
  [![GraalVM Check](https://github.com/ballerina-platform/module-ballerina-ftp/actions/workflows/build-with-bal-test-graalvm.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerina-ftp/actions/workflows/build-with-bal-test-graalvm.yml)
  [![GitHub Last Commit](https://img.shields.io/github/last-commit/ballerina-platform/module-ballerina-ftp.svg?label=Last%20Commit)](https://github.com/ballerina-platform/module-ballerina-ftp/commits/master)
  [![Github issues](https://img.shields.io/github/issues/ballerina-platform/ballerina-standard-library/module/ftp.svg?label=Open%20Issues)](https://github.com/ballerina-platform/ballerina-standard-library/labels/module%2Fftp)

This module provides a client and listener implementation for FTP, FTPS (FTP over SSL/TLS), and SFTP (SSH File Transfer Protocol) to facilitate secure and unsecure file transfers with remote servers.

## FTP Client

The `ftp:Client` connects to a remote server and performs various operations. It supports generic operations such as `get`, `delete`, `put`, `append`, `mkdir`, `rmdir`, `isDirectory`, `rename`, `size`, and `list`.

The client also provides typed operations for reading and writing files as text, JSON, XML, CSV, and binary data, with streaming support for large files.

An FTP client is defined using the `protocol` and `host` parameters and optionally, the `port` and `auth`. Authentication configuration can be configured using the `auth` parameter for Basic Auth and private key. 

### Create an FTP Client (Unsecure)

```ballerina
ftp:ClientConfiguration ftpConfig = {
    protocol: ftp:FTP,
    host: "<The FTP host>",
    port: <The FTP port>,
    auth: {
        credentials: {
            username: "<The FTP username>",
            password: "<The FTP passowrd>"
        }
    }
};

// Create the FTP client.
ftp:Client|ftp:Error ftpClient = new(ftpConfig);
```

## Secure Access with FTPS (FTP over SSL/TLS)

FTPS provides security by encrypting the control and data channels using SSL/TLS. This module supports both Explicit (starts as regular FTP and upgrades via AUTH TLS) and Implicit (SSL/TLS established immediately) modes.

### FTPS Client Configuration

To use FTPS, set the protocol to FTPS and provide the secureSocket configuration including your keystore and truststore.

```ballerina
ftp:ClientConfiguration ftpsConfig = {
    protocol: ftp:FTPS,
    host: "ftps.example.com",
    port: 21, // 21 for EXPLICIT, 990 for IMPLICIT
    auth: {
        credentials: { username: "user", password: "password" },
        secureSocket: {
            key: {
                path: "/path/to/keystore.p12",
                password: "keystore-password"
            },
            cert: {
                path: "/path/to/truststore.p12",
                password: "truststore-password"
            },
            mode: ftp:EXPLICIT, // or ftp:IMPLICIT
            dataChannelProtection: ftp:PRIVATE // PROT P (Encrypted data channel)
        }
    }
};

ftp:Client ftpsClient = check new(ftpsConfig);
```

### FTPS Listener Configuration

The listener can monitor an FTPS-enabled directory and trigger service remote functions.

```ballerina
listener ftp:Listener ftpsListener = check new({
    protocol: ftp:FTPS,
    host: "ftps.example.com",
    port: 990,
    path: "/upload",
    pollingInterval: 5,
    auth: {
        credentials: { username: "user", password: "password" },
        secureSocket: {
            cert: { path: "/path/to/truststore.jks", password: "password" },
            mode: ftp:IMPLICIT
        }
    }
});
```

## Secure Access with SFTP (SSH File Transfer Protocol)

SFTP runs over the SSH protocol. It supports password-based authentication and public-key authentication via private key files.

### SFTP Client Configuration

```ballerina
ftp:ClientConfiguration sftpConfig = {
    protocol: ftp:SFTP,
    host: "sftp.example.com",
    port: 22,
    auth: {
        credentials: { username: "ssh-user", password: "password" },
        privateKey: {
            path: "/home/user/.ssh/id_rsa",
            password: "key-passphrase" // Optional
        }
    }
};
```

## Operations and Data Handling

### Uploading Content

The client supports uploading streams or typed data.

```ballerina
// Upload XML
xml data = xml `<order><id>123</id></order>`;
check ftpClient->putXml("/orders/123.xml", data);

// Upload CSV from records
Person[] people = [{name: "John", age: 30}, {name: "Jane", age: 25}];
check ftpClient->putCsv("/data/people.csv", people);
```

### Reading Content

You can read files directly into Ballerina types or as a stream.

```ballerina
// Read as text
string content = check ftpClient->getText("/notes.txt");

// Read as a stream of bytes (ideal for large files)
stream<byte[], io:Error?> byteStream = check ftpClient->getBytesAsStream("/large-file.dat");
```

### Create a directory

The following code creates a directory in the remote FTP server.

```ballerina
ftp:Error? mkdirResponse = ftpClient->mkdir("<The directory path>");
```

##### Upload a file to a remote server

The following code uploads a file to a remote FTP server.

```ballerina
stream<io:Block, io:Error?> fileByteStream
    = check io:fileReadBlocksAsStream(putFilePath, <Block size>);
ftp:Error? putResponse = ftpClient->put("<The resource path>", fileByteStream);
```

You can also upload files as specific types:

Upload as text:
```ballerina
ftp:Error? result = ftpClient->putText("<The file path>", "Hello, World!");
```

Upload as JSON or record:
```ballerina
// Upload JSON
json jsonData = {name: "John", age: 30};
ftp:Error? result = ftpClient->putJson("<The file path>", jsonData);

// Upload a record
type User record {
    string name;
    int age;
};

User user = {name: "Jane", age: 25};
ftp:Error? result = ftpClient->putJson("<The file path>", user);
```

Upload as XML:
```ballerina
xml xmlData = xml `<config><database>mydb</database></config>`;
ftp:Error? result = ftpClient->putXml("<The file path>", xmlData);
```

Upload as CSV (string arrays or typed records):
```ballerina
// Upload as string array of arrays
string[][] csvData = [["Name", "Age"], ["John", "30"], ["Jane", "25"]];
ftp:Error? result = ftpClient->putCsv("<The file path>", csvData);

// Upload records as CSV
type Person record {
    string name;
    int age;
};

Person[] people = [{name: "John", age: 30}, {name: "Jane", age: 25}];
ftp:Error? result = ftpClient->putCsv("<The file path>", people);
```

Upload as bytes:
```ballerina
byte[] binaryData = [0x48, 0x65, 0x6C, 0x6C, 0x6F]; // "Hello"
ftp:Error? result = ftpClient->putBytes("<The file path>", binaryData);
```

##### Compress and upload a file to a remote server

The following code compresses and uploads a file to a remote FTP server.

```ballerina
// Set the optional boolean flag as 'true' to compress before uploading
stream<io:Block, io:Error?> fileByteStream
    = check io:fileReadBlocksAsStream(putFilePath, <Block size>);
ftp:Error? compressedPutResponse = ftpClient->put("<Resource path>",
    fileByteStream, compressionType=ZIP);
```

##### Get the size of a remote file

The following code gets the size of a file in a remote FTP server.

```ballerina
int|ftp:Error sizeResponse = ftpClient->size("<The resource path>");
```

##### Read the content of a remote file

The following code reads the content of a file in a remote FTP server. The FTP client supports various data types including text, JSON, XML, CSV, and binary data through typed get operations.

Read as text:
```ballerina
string fileContent = check ftpClient->getText("<The file path>");
```

Read as JSON or typed record:
```ballerina
// Read as JSON
json jsonData = check ftpClient->getJson("<The file path>");

// Read as a specific record type
type User record {
    string name;
    int age;
};

User userData = check ftpClient->getJson("<The file path>");
```

Read as XML or typed record:
```ballerina
// Read as XML
xml xmlData = check ftpClient->getXml("<The file path>");

// Read as a specific record type
type Config record {
    string database;
    int timeout;
};

Config config = check ftpClient->getXml("<The file path>");
```

Read as CSV (string arrays or typed records):
```ballerina
// Read as string array of arrays
string[][] csvData = check ftpClient->getCsv("<The file path>");

// Read as an array of typed records
type CsvRecord record {
    string id;
    string name;
    string email;
};

CsvRecord[] records = check ftpClient->getCsv("<The file path>");
```

Read as bytes:
```ballerina
// Read entire file as byte array
byte[] fileBytes = check ftpClient->getBytes("<The file path>");

// Read as streaming bytes
stream<byte[], io:Error?> byteStream = check ftpClient->getBytesAsStream("<The file path>");
record {|byte[] value;|} nextBytes = check byteStream.next();
check byteStream.close();
```

##### Rename/move a remote file

The following code renames or moves a file to another location in the same remote FTP server.

```ballerina
ftp:Error? renameResponse = ftpClient->rename("<The source file path>",
    "<The destination file path>");
```

##### Delete a remote file

The following code deletes a remote file in a remote FTP server.

```ballerina
ftp:Error? deleteResponse = ftpClient->delete("<The resource path>");
```

##### Remove a directory from a remote server

The following code removes a directory in a remote FTP server.

```ballerina
ftp:Error? rmdirResponse = ftpClient->rmdir("<The directory path>");
```

## FTP Listener and Event Handling

The `ftp:Listener` supports format-specific content handlers. Files are automatically routed to handlers based on their extensions or custom patterns defined via `@ftp:FunctionConfig`.

The `ftp:Listener` is used to listen to a remote FTP location and trigger events when new files are added to or deleted from the directory. The listener supports both a generic `onFileChange` handler for file system events and format-specific content handlers (`onFileText`, `onFileJson`, `onFileXml`, `onFileCsv`, `onFile`) that automatically deserialize file content based on the file type.

An FTP listener is defined using the mandatory `protocol`, `host`, and  `path` parameters. The authentication
configuration can be done using the `auth` parameter and the polling interval can be configured using the `pollingInterval` parameter. The default polling interval is 60 seconds.

The `fileNamePattern` parameter can be used to define the type of files the FTP listener will listen to.
For instance, if the listener gets invoked for text files, the value `(.*).txt` can be given for the config.

An authentication-related configuration can be given to the FTP listener with the `auth` configuration.

### Example Service

Handle text files:
```ballerina
service on remoteServer {
    remote function onFileText(string content, ftp:FileInfo fileInfo) returns error? {
        log:print("Text file: " + fileInfo.path);
        log:print("Content: " + content);
    }
}
```

Handle JSON files (as generic JSON or typed record):
```ballerina
type User record {
    string name;
    int age;
    string email;
};

service on remoteServer {
    // Handle as typed record
    remote function onFileJson(User content, ftp:FileInfo fileInfo) returns error? {
        log:print("User file: " + fileInfo.path);
        log:print("User name: " + content.name);
    }
}
```

Handle XML files (as generic XML or typed record):
```ballerina
type Config record {
    string database;
    int timeout;
    boolean debug;
};

service on remoteServer {
    // Handle as typed record
    remote function onFileXml(Config content, ftp:FileInfo fileInfo) returns error? {
        log:print("Config file: " + fileInfo.path);
        log:print("Database: " + content.database);
    }
}
```

Handle CSV files (as string arrays or typed record arrays):
```ballerina
type CsvRecord record {
    string id;
    string name;
    string email;
};

service on remoteServer {
    // Handle as array of typed records
    remote function onFileCsv(CsvRecord[] content, ftp:FileInfo fileInfo) returns error? {
        log:print("CSV file: " + fileInfo.path);
        foreach CsvRecord record in content {
            log:print("Record: " + record.id + ", " + record.name);
        }
    }
}
```

Handle binary files:
```ballerina
service on remoteServer {
    // Handle as byte array
    remote function onFile(byte[] content, ftp:FileInfo fileInfo) returns error? {
        log:print("Binary file: " + fileInfo.path);
        log:print("File size: " + content.length().toString());
    }
}
```

Stream large files:
```ballerina
service on remoteServer {
    // Stream binary content
    remote function onFile(stream<byte[], error?> content, ftp:FileInfo fileInfo) returns error? {
        log:print("Streaming file: " + fileInfo.path);
        record {|byte[] value;|} nextBytes = check content.next();
        while nextBytes is record {|byte[] value;|} {
            log:print("Received chunk: " + nextBytes.value.length().toString() + " bytes");
            nextBytes = content.next();
        }
        check content.close();
    }
}
```

Stream CSV data as typed records:
```ballerina
type DataRow record {
    string timestamp;
    string value;
};

service on remoteServer {
    // Stream CSV as records
    remote function onFileCsv(stream<DataRow, error?> content, ftp:FileInfo fileInfo) returns error? {
        log:print("Streaming CSV file: " + fileInfo.path);
        record {|DataRow value;|}|error? nextRow = content.next();
        while nextRow is record {|DataRow value;|} {
            log:print("Row: " + nextRow.value.timestamp + " = " + nextRow.value.value);
            nextRow = content.next();
        }
        check content.close();
    }
}
```

The FTP listener automatically routes files to the appropriate content handler based on file extension: `.txt` → `onFileText()`, `.json` → `onFileJson()`, `.xml` → `onFileXml()`, `.csv` → `onFileCsv()`, and other extensions → `onFile()` (fallback handler). You can override the default routing using the `@ftp:FunctionConfig` annotation to specify a custom file name pattern for each handler method.


## Issues and projects 

Issues and Projects tabs are disabled for this repository as this is part of the Ballerina Standard Library. To report bugs, request new features, start new discussions, view project boards, etc. please visit Ballerina Standard Library [parent repository](https://github.com/ballerina-platform/ballerina-standard-library). 

This repository only contains the source code for the library.

## Build from Source

### Prerequisites

Download and install Java SE Development Kit (JDK) version 21.
Set the JAVA_HOME environment variable to the JDK installation directory.

### Build Commands

1. To build the library:
   ```    
   ./gradlew clean build
   ```

2. To run the tests:
   ```
   ./gradlew clean test
   ```

3. To run a group of tests
   ```
   ./gradlew clean test -Pgroups=<test_group_names>
   ```

4. To build the library without the tests:
   ```
   ./gradlew clean build -x test
   ```

5. To debug library implementation:
   ```
   ./gradlew clean build -Pdebug=<port>
   ```

6. To debug with Ballerina language:
   ```
   ./gradlew clean build -PbalJavaDebug=<port>
   ```

7. Publish the generated artifacts to the local Ballerina central repository:
    ```
    ./gradlew clean build -PpublishToLocalCentral=true
    ```

8. Publish the generated artifacts to the Ballerina central repository:
   ```
   ./gradlew clean build -PpublishToCentral=true
   ```

## Contribute to Ballerina

As an open source project, Ballerina welcomes contributions from the community. 

For more information, go to the [contribution guidelines](https://github.com/ballerina-platform/ballerina-lang/blob/master/CONTRIBUTING.md).

## Code of conduct

All contributors are encouraged to read the [Ballerina Code of Conduct](https://ballerina.io/code-of-conduct).

## Useful links

* For more information go to the [`ftp` library](https://lib.ballerina.io/ballerina/ftp/latest).
* For example demonstrations of the usage, go to [Ballerina By Examples](https://ballerina.io/learn/by-example/).
* Chat live with us via our [Discord server](https://discord.gg/ballerinalang).
* Post all technical questions on Stack Overflow with the [#ballerina](https://stackoverflow.com/questions/tagged/ballerina) tag.
