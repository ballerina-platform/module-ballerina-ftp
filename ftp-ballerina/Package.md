## Module Overview

This module provides an FTP client and an FTP server listener implementation to facilitate an FTP connection to a remote location.

###FTP Client

The `ftp:Client` connects to an FTP server and performs various operations on the files. Currently, it supports the 
generic FTP operations; `get`, `delete`, `put`, `append`, `mkdir`, `rmdir`, `isDirectory`, `rename`, `size`, and
 `list`.

An FTP client endpoint is defined using the parameters `protocol` and `host`, and optionally the `port` and 
`secureSocket`. Authentication configuration can be configured using the `secureSocket` parameter for basicAuth, 
private key, or TrustStore/Keystore.

##### Creating a client

The following code creates an FTP client and perform I/O operations, which connects to the FTP server with Basic Auth.
```ballerina
import ballerina/ftp;

// Define FTP client configuration
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

// Create FTP client
ftp:Client ftpClient = new(ftpConfig);
```

##### Creating a directory

The following code creates a directory in the remote FTP server.

```ballerina
ftp:Error? mkdirResponse = ftpClient->mkdir("<The directory path>");
if (mkdirResponse is ftp:Error) {
    log:printError("Error occured in creating directory", err = mkdirResponse);
    return;
}
```

##### Uploading file to a remote server

The following code uploads a file to a remote FTP server.

```ballerina
io:ReadableByteChannel|error summaryChannel
    = io:openReadableFile("<The local data source path>");
if(summaryChannel is io:ReadableByteChannel){
    ftp:Error? putResponse = ftpClient->put("<The resource path>", summaryChannel);   
    if(putResponse is ftp:Error) {
        log:printError("Error occured in uploading content", err = putResponse);
        return;
    }
}
```

##### Compressing and uploading a file to a remote server

The following code compresses and uploads a file to a remote FTP server.

```ballerina
io:ReadableByteChannel|error inputChannel
    = io:openReadableFile("<Local data source path>");
if (inputChannel is io:ReadableByteChannel) {
    // Set the optional boolean flag as 'true' to compress before uploading
    ftp:Error? compressedPutResponse = ftpClient->put("<Resource path>",
        inputChannel, true);   
    if (compressedPutResponse is ftp:Error) {
        log:printError("Error occured in uploading content",
            err = compressedPutResponse);
        return;
    }
}
```

##### Getting the size of a remote file

The following code get and size of a file of a file in remote FTP server.

```ballerina
var sizeResponse = ftpClient->size("<The resource path>");
if (sizeResponse is int) {
    log:print("File size: " + sizeResponse.toString());
} else {
    log:printError("Error occured in retrieving size", err = sizeResponse);
    return;
}
```

##### Reading the content of a remote file

The following code read the content of a file in remote FTP server.

```ballerina
var getResponse = ftpClient->get("<The file path>");
if (getResponse is io:ReadableByteChannel) {
    io:ReadableCharacterChannel? characters
        = new io:ReadableCharacterChannel(getResponse, "utf-8");
    if (characters is io:ReadableCharacterChannel) {
        var output = characters.read(<No of characters to read>);
        if (output is string) {
            log:print("File content: " + output);
        } else {
            log:printError("Error occured in retrieving content", err = output);
            return;
        }
        var closeResult = characters.close();
        if (closeResult is error) {
            log:printError("Error occurred while closing the channel",
                err = closeResult);
            return;
        }
    }
} else {
    log:printError("Error occured in retrieving content", err = getResponse);
    return;
}
```

##### Renaming or moving a remote file to another remote location in a same FTP server

The following rename or move remote a file to another location in the same remote FTP server.

```ballerina
ftp:Error? renameResponse = ftpClient->rename("<The source file path>",
    "<The destination file path>");
if (renameResponse is ftp:Error) {
    log:printError("Error occurred while renaming the file", err = renameResponse);
    return;
}
```

##### Deleting a remote file

The following delete a remote file in a remote FTP server.

```ballerina
ftp:Error? deleteResponse = ftpClient->delete("<The resource path>");
if (deleteResponse is ftp:Error) {
    log:printError("Error occurred while deleting a file", err = deleteResponse);
    return;
}
```

##### Removing a directory from a remote server

The following remove a directory in a remote FTP server.

```ballerina
var rmdirResponse = ftpClient->rmdir("<The directory path>");
if (rmdirResponse is ftp:Error) {
    io:println("Error occured in removing directory.", rmdirResponse); 
    return;
}
```

###FTP Listener

The `ftp:Listener` is used to listen to a remote FTP location and trigger a `WatchEvent` type of event when new 
files are added to or deleted from the directory. The `fileResource` function is invoked when a new file is added 
and/or deleted.

An FTP listener endpoint is defined using the mandatory parameters `protocol`, `host` and  `path`. Authentication 
configuration can be done using `secureSocket` and polling interval can be configured using `pollingInterval`. 
Default polling interval is 60 seconds.

The `fileNamePattern` parameter can be used to define the type of files the FTP listener endpoint will listen to. 
For instance, if the listener gets invoked for text files, the value `(.*).txt` can be given for the config.

##### Creating a listener

The FTP Listener can be used to listen to a remote directory. It will keep listening to the specified directory and 
periodically notify on file addition and deletion.

```ballerina
import ballerina/ftp;
import ballerina/log;

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
