## Overview

This module provides an FTP client and an FTP server listener implementation to facilitate an FTP connection connected to a remote location.

### FTP Client

The `ftp:Client` connects to an FTP server and performs various operations on the files. Currently, it supports the
generic FTP operations; `get`, `delete`, `put`, `append`, `mkdir`, `rmdir`, `isDirectory`, `rename`, `size`, and
 `list`.

An FTP client endpoint is defined using the `protocol` and `host` parameters and optionally the `port` and
`secureSocket`. Authentication configuration can be configured using the `secureSocket` parameter for Basic Auth,
private key, or TrustStore/Keystore.

##### Creating a Client

The following code creates an FTP client and performs the I/O operations, which connect to the FTP server with Basic Auth.
```ballerina
// Define the FTP client configuration.
ftp:ClientEndpointConfig ftpConfig = {
    protocol: ftp:FTP,
    host: "<The FTP host>",
    port: <The FTP port>,
    secureSocket: {
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
io:ReadableByteChannel summaryChannel
    = check io:openReadableFile("<The local data source path>");
ftp:Error? putResponse = ftpClient->put("<The resource path>", summaryChannel);
```

##### Compressing and Uploading a File to a Remote Server

The following code compresses and uploads a file to a remote FTP server.

```ballerina
io:ReadableByteChannel inputChannel
    = check io:openReadableFile("<Local data source path>");
// Set the optional boolean flag as 'true' to compress before uploading
ftp:Error? compressedPutResponse = ftpClient->put("<Resource path>",
    inputChannel, true);
```

##### Getting the Size of a Remote File

The following code gets the size of a file in a remote FTP server.

```ballerina
int|ftp:Error sizeResponse = ftpClient->size("<The resource path>");
```

##### Reading the Content of a Remote File

The following code reads the content of a file in a remote FTP server.

```ballerina
io:ReadableByteChannel getResponse = check ftpClient->get("<The file path>");
io:ReadableCharacterChannel? characters
    = new io:ReadableCharacterChannel(getResponse, "utf-8");
if (characters is io:ReadableCharacterChannel) {
    string output = check characters.read(<No of characters to read>);
    var closeResult = characters.close();
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

###FTP Listener

The `ftp:Listener` is used to listen to a remote FTP location and trigger a `WatchEvent` type of event when new
files are added to or deleted from the directory. The `fileResource` function is invoked when a new file is added
and/or deleted.

An FTP listener endpoint is defined using the mandatory `protocol`, `host`, and  `path` parameters. The authentication
configuration can be done using a `secureSocket` and the polling interval can be configured using the `pollingInterval` parameter.
The default polling interval is 60 seconds.

The `fileNamePattern` parameter can be used to define the type of files the FTP listener endpoint will listen to.
For instance, if the listener gets invoked for text files, the value `(.*).txt` can be given for the config.

##### Creating a Listener

The FTP Listener can be used to listen to a remote directory. It will keep listening to the specified directory and
notify on file addition and deletion periodically.

```ballerina
listener ftp:Listener remoteServer = new({
    protocol: ftp:FTP,
    host: "<The FTP host>",
    secureSocket: {
        basicAuth: {
            username: "<The FTP username>",
            password: "<The FTP passowrd>"
        }
    },
    port: <The FTP port>,
    path: "<The remote FTP direcotry location>",
    pollingInterval: <Polling interval>,
    fileNamePattern: "<File type>"
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
