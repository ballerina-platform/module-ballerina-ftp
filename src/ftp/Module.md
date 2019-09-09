## Module Overview

The `wso2/ftp` module provides an FTP client and an FTP server listener implementation to facilitate an FTP connection 
to a remote location.

The following sections provide you details on how to use the FTP connector.

- [Compatibility](#compatibility)
- [Feature Overview](#feature-overview)
- [Getting Started](#getting-started)
- [Samples](#samples)

## Compatibility

| Ballerina Language Version  |
|:---------------------------:|
|  1.0.0                     |

## Feature Overview

### FTP Client
The `ftp:Client` connects to an FTP server and performs various operations on the files. Currently, it supports the 
generic FTP operations; `get`, `delete`, `put`, `append`, `mkdir`, `rmdir`, `isDirectory`,  `rename`, `size`, and
 `list`.

An FTP client endpoint is defined using the parameters `protocol` and `host`, and optionally the `port` and 
`secureSocket`. Authentication configuration can be configured using the `secureSocket` parameter for basicAuth, 
private key, or TrustStore/Keystore.

### FTP Listener
The `ftp:Listener` is used to listen to a remote FTP location and trigger an event of `WatchEvent` type, when new 
files are added to or deleted from the directory. The `fileResource` function is invoked when a new file is added 
and/or deleted.

An FTP listener endpoint is defined using the mandatory parameters `protocol`, `host` and  `path`. Authentication 
configuration can be done using `secureSocket` and polling interval can be configured using `pollingInterval`. 
Default polling interval is 60 seconds.

The `fileNamePattern` parameter can be used to define the type of files the FTP listener endpoint will listen to. 
For instance, if the listener should get invoked for text files, the value `(.*).txt` can be given for the config.

## Getting Started

### Prerequisites
Download and install [Ballerina](https://ballerinalang.org/downloads/).

### Pull the Module
You can pull the FTP module from Ballerina Central using the command:
```ballerina
$ ballerina pull wso2/ftp
```

## Samples

### FTP Listener Sample
The FTP Listener can be used to listen to a remote directory. It will keep listening to the specified directory and 
periodically notify the file addition and deletion.

```ballerina
import wso2/ftp;
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
    resource function fileResource(WatchEvent m) {

        foreach FileInfo v1 in m.addedFiles {
            log:printInfo("Added file path: " + v1.path);
        }
        foreach string v1 in m.deletedFiles {
            log:printInfo("Deleted file path: " + v1);
        }
    }
}
```

### FTP Client Sample
The FTP Client Connector can be used to connect to an FTP server and perform I/O operations.

```ballerina
import wso2/ftp;
import ballerina/io;
import ballerina/log;

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
ftp:Client ftpClient = new(ftpConfig);
    
public function main() {
    // To create a folder in remote server.
    error? dirCreErr = ftpClient->mkdir("<The directory path>");
    if (dirCreErr is error) {
        log:printError("Error occured in creating directory.", dirCreErr);
        return;
    }
    
    // Upload file to a remote server.
    io:ReadableByteChannel|error summaryChannel = io:openReadableFile("<The local data source path>");
    if(summaryChannel is io:ReadableByteChannel){
        error? filePutErr = ftpClient->put("<The resource path>", summaryChannel);   
        if(filePutErr is error) {
            log:printError("Error occured in uploading content.", filePutErr);
            return;
        }
    }
    
    // Get the size of a remote file.
    var size = ftpClient->size("<The resource path>");
    if (size is int) {
        log:printInfo("File size: " + size.toString());
    } else {
        log:printError("Error occured in retrieving size.", size);
        return;
    }
    
    // Read content of a remote file.
    var getResult = ftpClient->get("<The file path>");
    if (getResult is io:ReadableByteChannel) {
        io:ReadableCharacterChannel? characters = new io:ReadableCharacterChannel(getResult, "utf-8");
        if (characters is io:ReadableCharacterChannel) {
            var output = characters.read(<No of characters to read>);
            if (output is string) {
                log:printInfo("File content: "+ output);
            } else {
                log:printError("Error occured in retrieving content.", output);
                return;
            }
            var closeResult = characters.close();
        }
    } else {
        log:printError("Error occured in retrieving content.", getResult);
        return;
    }
    
    // Rename or move remote file to a another remote location in a same FTP server.
    error? renameErr = ftpClient->rename("<The source file path>", "<The destination file path>");
    
    // Delete remote file.
    error? fileDelCreErr = ftpClient->delete("<The resource path>");
    
    // Remove directory from remote server.
    var result = ftpClient->rmdir("<The directory path>");
    if (result is error) {
        io:println("Error occured in removing directory.", result); 
    }
}
```
