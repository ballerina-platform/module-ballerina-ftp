## Overview

This module provides an FTP/SFTP client and an FTP/SFTP server listener implementation to facilitate an FTP/SFTP connection connected to a remote location.

### FTP Client

The `ftp:Client` connects to an FTP server and performs various operations on the files. Currently, it supports the
generic FTP operations; `get`, `delete`, `put`, `append`, `mkdir`, `rmdir`, `isDirectory`, `rename`, `size`, and
 `list`.

An FTP client is defined using the `protocol` and `host` parameters and optionally, the `port` and
`auth`. Authentication configuration can be configured using the `auth` parameter for Basic Auth and
private key.

Authentication related configuration can be given to the FTP client with `auth` configuration.

##### Creating a Client

The following code creates an FTP client and performs the I/O operations, which connect to the FTP server with Basic Auth.
```ballerina
// Define the FTP client configuration.
ftp:ClientConfiguration ftpConfig = {
    protocol: ftp:FTP,
    host: "<The FTP host>",
    port: <The FTP port>,
    auth: {
        basicAuth: {
            username: "<The FTP username>",
            password: "<The FTP passowrd>"
        }
    }
};

// Create the FTP client.
ftp:Client ftpClient = new(ftpConfig);
```

##### Creating a Directory

The following code creates a directory in the remote FTP server.

```ballerina
ftp:Error? mkdirResponse = ftpClient->mkdir("<The directory path>");
```

##### Uploading File to a Remote Server

The following code uploads a file to a remote FTP server.

```ballerina
stream<io:Block, io:Error?> fileByteStream
    = check io:fileReadBlocksAsStream(putFilePath, <Block size>);
ftp:Error? putResponse = ftpClient->put("<The resource path>", fileByteStream);
```

##### Compressing and Uploading a File to a Remote Server

The following code compresses and uploads a file to a remote FTP server.

```ballerina
// Set the optional boolean flag as 'true' to compress before uploading
stream<io:Block, io:Error?> fileByteStream
    = check io:fileReadBlocksAsStream(putFilePath, <Block size>);
ftp:Error? compressedPutResponse = ftpClient->put("<Resource path>",
    fileByteStream, true);
```

##### Getting the Size of a Remote File

The following code gets the size of a file in a remote FTP server.

```ballerina
int|ftp:Error sizeResponse = ftpClient->size("<The resource path>");
```

##### Reading the Content of a Remote File

The following code reads the content of a file in a remote FTP server.

```ballerina
stream<byte[], io:Error?>|Error str = clientEP -> get("<The file path>", <Block size>);
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

##### Renaming or Moving a Remote file to Another Remote Location in the Same FTP Server

The following code renames or moves a file to another location in the same remote FTP server.

```ballerina
ftp:Error? renameResponse = ftpClient->rename("<The source file path>",
    "<The destination file path>");
```

##### Deleting a Remote File

The following code deletes a remote file in a remote FTP server.

```ballerina
ftp:Error? deleteResponse = ftpClient->delete("<The resource path>");
```

##### Removing a Directory From a Remote Server

The following code removes a directory in a remote FTP server.

```ballerina
ftp:Error? rmdirResponse = ftpClient->rmdir("<The directory path>");
```

### FTP Listener

The `ftp:Listener` is used to listen to a remote FTP location and trigger a `WatchEvent` type of event when new
files are added to or deleted from the directory. The `fileResource` function is invoked when a new file is added
and/or deleted.

An FTP listener is defined using the mandatory `protocol`, `host`, and  `path` parameters. The authentication
configuration can be done using the `auth` parameter and the polling interval can be configured using the `pollingInterval` parameter.
The default polling interval is 60 seconds.

The `fileNamePattern` parameter can be used to define the type of files the FTP listener will listen to.
For instance, if the listener gets invoked for text files, the value `(.*).txt` can be given for the config.

Authentication related configuration can be given to the FTP listener with `auth` configuration.

##### Creating a Listener

The FTP Listener can be used to listen to a remote directory. It will keep listening to the specified directory and
notify on file addition and deletion periodically.

```ballerina
listener ftp:Listener remoteServer = new({
    protocol: ftp:FTP,
    host: "<The FTP host>",
    auth: {
        basicAuth: {
            username: "<The FTP username>",
            password: "<The FTP passowrd>"
        }
    },
    port: <The FTP port>,
    path: "<The remote FTP direcotry location>",
    pollingInterval: <Polling interval>,
    fileNamePattern: "<File name pattern>"
});

service ftpServerConnector on remoteServer {
    resource function onFileChange(ftp:WatchEvent fileEvent) {

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

SFTP is a secure protocol alternative to the FTP which runs on top of SSH protocol.
There are several ways to authenticate an SFTP server. One is using the username and the password.
Another way is using the client's private key. Ballerina SFTP client and the listener supports only those authentication standards.
Authentication related configuration can be given to the SFTP client/listener with `auth` configuration.
Password based authentication is defined with `basicAuth` configuration while the private key based authentication is defined with `privateKey` configuration.

#### SFTP Client Configuration

```ballerina
ftp:ClientConfiguration sftpConfig = {
    protocol: ftp:SFTP,
    host: "<The SFTP host>",
    port: <The SFTP port>,
    auth: {
        basicAuth: {username: "<The SFTP username>", password: "<The SFTP password>"},
        privateKey: {
            path: "<The private key file path>",
            password: "<The private key file password>"
        }
    }
};
```

#### SFTP Listener Configuration

```ballerina
listener ftp:Listener remoteServer = new({
    protocol: ftp:SFTP,
    host: "<The SFTP host>",
    auth: {
        basicAuth: {username: "<The SFTP username>", password: "<The SFTP password>"},
        privateKey: {
            path: "<The private key file path>",
            password: "<The private key file password>"
        }
    }
    port: <The SFTP port>,
    path: "<The remote SFTP direcotry location>",
    pollingInterval: <Polling interval>,
    fileNamePattern: "<File name pattern>"
});
```
