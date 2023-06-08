Ballerina FTP Library
=====================

  [![Build](https://github.com/ballerina-platform/module-ballerina-ftp/actions/workflows/build-timestamped-master.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerina-ftp/actions/workflows/build-timestamped-master.yml)
  [![codecov](https://codecov.io/gh/ballerina-platform/module-ballerina-ftp/branch/master/graph/badge.svg)](https://codecov.io/gh/ballerina-platform/module-ballerina-ftp)
  [![Trivy](https://github.com/ballerina-platform/module-ballerina-ftp/actions/workflows/trivy-scan.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerina-ftp/actions/workflows/trivy-scan.yml)
  [![GraalVM Check](https://github.com/ballerina-platform/module-ballerina-ftp/actions/workflows/build-with-bal-test-graalvm.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerina-ftp/actions/workflows/build-with-bal-test-graalvm.yml)
  [![GitHub Last Commit](https://img.shields.io/github/last-commit/ballerina-platform/module-ballerina-ftp.svg?label=Last%20Commit)](https://github.com/ballerina-platform/module-ballerina-ftp/commits/master)
  [![Github issues](https://img.shields.io/github/issues/ballerina-platform/ballerina-standard-library/module/ftp.svg?label=Open%20Issues)](https://github.com/ballerina-platform/ballerina-standard-library/labels/module%2Fftp)

This library provides an FTP/SFTP client, and an FTP/SFTP server listener implementation to facilitate an FTP/SFTP connection connected to a remote location.

### FTP client

The `ftp:Client` connects to an FTP server and performs various operations on the files. Currently, it supports the
generic FTP operations; `get`, `delete`, `put`, `append`, `mkdir`, `rmdir`, `isDirectory`, `rename`, `size`, and
`list`.

An FTP client is defined using the `protocol` and `host` parameters and optionally, the `port` and
`auth`. Authentication configuration can be configured using the `auth` parameter for Basic Auth and
private key.

An authentication-related configuration can be given to the FTP client with the `auth` configuration.

##### Create a client

The following code creates an FTP client and performs the I/O operations, which connect to the FTP server with Basic Auth.
```ballerina
// Define the FTP client configuration.
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

##### Create a directory

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

The following code reads the content of a file in a remote FTP server.

```ballerina
stream<byte[], io:Error?>|Error str = clientEP -> get("<The file path>");
if (str is stream<byte[], io:Error?>) {
    record {|byte[] value;|}|io:Error? arr1 = str.next();
    if (arr1 is record {|byte[] value;|}) {
        string fileContent = check strings:fromBytes(arr1.value);
        // `fileContent` is the `string` value of first byte array
        record {|byte[] value;|}|io:Error? arr2 = str.next();
        // Similarly following content chunks can be iteratively read with `next` method.
        // Final chunk will contain the terminal value which is `()`.
    }
    io:Error? closeResult = str.close();
}
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

### FTP listener

The `ftp:Listener` is used to listen to a remote FTP location and trigger a `WatchEvent` type of event when new
files are added to or deleted from the directory. The `fileResource` function is invoked when a new file is added
and/or deleted.

An FTP listener is defined using the mandatory `protocol`, `host`, and  `path` parameters. The authentication
configuration can be done using the `auth` parameter and the polling interval can be configured using the `pollingInterval` parameter.
The default polling interval is 60 seconds.

The `fileNamePattern` parameter can be used to define the type of files the FTP listener will listen to.
For instance, if the listener gets invoked for text files, the value `(.*).txt` can be given for the config.

An authentication-related configuration can be given to the FTP listener with the `auth` configuration.

##### Create a listener

The FTP Listener can be used to listen to a remote directory. It will keep listening to the specified directory and
notify on file addition and deletion periodically.

```ballerina
listener ftp:Listener remoteServer = check new({
    protocol: ftp:FTP,
    host: "<The FTP host>",
    auth: {
        credentials: {
            username: "<The FTP username>",
            password: "<The FTP passowrd>"
        }
    },
    port: <The FTP port>,
    path: "<The remote FTP direcotry location>",
    pollingInterval: <Polling interval>,
    fileNamePattern: "<File name pattern>"
});

service on remoteServer {
    remote function onFileChange(ftp:WatchEvent fileEvent) {

        foreach ftp:FileInfo addedFile in fileEvent.addedFiles {
            log:print("Added file path: " + addedFile.path);
        }
        foreach string deletedFile in fileEvent.deletedFiles {
            log:print("Deleted file path: " + deletedFile);
        }
    }
}
```

### Secure access with SFTP

SFTP is a secure protocol alternative to the FTP, which runs on top of the SSH protocol.
There are several ways to authenticate an SFTP server. One is using the username and the password.
Another way is using the client's private key. The Ballerina SFTP client and the listener support only those authentication standards.
An authentication-related configuration can be given to the SFTP client/listener with the `auth` configuration.
Password-based authentication is defined with the `credentials` configuration while the private key based authentication is defined with the `privateKey` configuration.

#### SFTP client configuration

```ballerina
ftp:ClientConfiguration sftpConfig = {
    protocol: ftp:SFTP,
    host: "<The SFTP host>",
    port: <The SFTP port>,
    auth: {
        credentials: {username: "<The SFTP username>", password: "<The SFTP password>"},
        privateKey: {
            path: "<The private key file path>",
            password: "<The private key file password>"
        }
    }
};
```

#### SFTP listener configuration

```ballerina
listener ftp:Listener remoteServer = check new({
    protocol: ftp:SFTP,
    host: "<The SFTP host>",
    port: <The SFTP port>,
    path: "<The remote SFTP direcotry location>",
    pollingInterval: <Polling interval>,
    fileNamePattern: "<File name pattern>",
    auth: {
        credentials: {username: "<The SFTP username>", password: "<The SFTP password>"},
        privateKey: {
            path: "<The private key file path>",
            password: "<The private key file password>"
        }
    }
});
```

## Issues and projects 

Issues and Projects tabs are disabled for this repository as this is part of the Ballerina Standard Library. To report bugs, request new features, start new discussions, view project boards, etc. please visit Ballerina Standard Library [parent repository](https://github.com/ballerina-platform/ballerina-standard-library). 

This repository only contains the source code for the library.

## Build from the source

### Set up the prerequisites

1. Download and install Java SE Development Kit (JDK) version 11 (from one of the following locations).

   * [Oracle](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)

   * [OpenJDK](https://adoptium.net/)

        > **Note:** Set the JAVA_HOME environment variable to the path name of the directory into which you installed JDK.
     
### Build the source

Execute the commands below to build from source.

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
