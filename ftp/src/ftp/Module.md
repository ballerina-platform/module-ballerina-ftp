Connects to an FTP server using Ballerina.

## Module Overview

The `wso2/ftp` module provides an FTP client and an FTP server listener implementation to facilitate an FTP connection 
to a remote location.

**FTP Client**

The `ftp:Client` connects to an FTP server and performs various operations on the files. Currently, it supports the 
generic FTP operations; `get`, `delete`, `put`, `append`, `mkdir`, `rmdir`, `isDirectory`, `rename`, `size`, and
 `list`.

An FTP client endpoint is defined using the parameters `protocol` and `host`, and optionally the `port` and 
`secureSocket`. Authentication configuration can be configured using the `secureSocket` parameter for basicAuth, 
private key, or TrustStore/Keystore.

**FTP Listener**

The `ftp:Listener` is used to listen to a remote FTP location and trigger a `WatchEvent` type of event when new 
files are added to or deleted from the directory. The `fileResource` function is invoked when a new file is added 
and/or deleted.

An FTP listener endpoint is defined using the mandatory parameters `protocol`, `host` and  `path`. Authentication 
configuration can be done using `secureSocket` and polling interval can be configured using `pollingInterval`. 
Default polling interval is 60 seconds.

The `fileNamePattern` parameter can be used to define the type of files the FTP listener endpoint will listen to. 
For instance, if the listener gets invoked for text files, the value `(.*).txt` can be given for the config.

## Compatibility

|                             |           Version           |
|:---------------------------:|:---------------------------:|
| Ballerina Language          |            1.1.4            |

## Samples

**FTP Listener Sample**

The FTP Listener can be used to listen to a remote directory. It will keep listening to the specified directory and 
periodically notify on file addition and deletion.

```ballerina
import ballerina/log;
import wso2/ftp;

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
            log:printInfo("Added file path: " + addedFile.path);
        }
        foreach string deletedFile in fileEvent.deletedFiles {
            log:printInfo("Deleted file path: " + deletedFile);
        }
    }
}
```

**FTP Client Sample**

The FTP Client Connector can be used to connect to an FTP server and perform I/O operations.

```ballerina
import ballerina/log;
import ballerina/io;
import wso2/ftp;


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
    
public function main() {
    // Create a folder in remote server
    error? mkdirResponse = ftpClient->mkdir("<The directory path>");
    if (mkdirResponse is error) {
        log:printError("Error occured in creating directory", mkdirResponse);
        return;
    }
    
    // Upload file to a remote server
    io:ReadableByteChannel|error summaryChannel = io:openReadableFile("<The local data source path>");
    if(summaryChannel is io:ReadableByteChannel){
        error? putResponse = ftpClient->put("<The resource path>", summaryChannel);   
        if(putResponse is error) {
            log:printError("Error occured in uploading content", putResponse);
            return;
        }
    }

    // Compress and upload file to a remote server
    io:ReadableByteChannel|error inputChannel = io:openReadableFile("<Local data source path>");
    if (inputChannel is io:ReadableByteChannel) {
        // Set the optional boolean flag as 'true' to compress before uploading
        error? compressedPutResponse = ftpClient->put("<Resource path>", inputChannel, true);   
        if (compressedPutResponse is error) {
            log:printError("Error occured in uploading content", compressedPutResponse);
            return;
        }
    }
    
    // Get the size of a remote file
    var sizeResponse = ftpClient->size("<The resource path>");
    if (sizeResponse is int) {
        log:printInfo("File size: " + sizeResponse.toString());
    } else {
        log:printError("Error occured in retrieving size", sizeResponse);
        return;
    }
    
    // Read content of a remote file
    var getResponse = ftpClient->get("<The file path>");
    if (getResponse is io:ReadableByteChannel) {
        io:ReadableCharacterChannel? characters = new io:ReadableCharacterChannel(getResponse, "utf-8");
        if (characters is io:ReadableCharacterChannel) {
            var output = characters.read(<No of characters to read>);
            if (output is string) {
                log:printInfo("File content: " + output);
            } else {
                log:printError("Error occured in retrieving content", output);
                return;
            }
            var closeResult = characters.close();
            if (closeResult is error) {
                log:printError("Error occurred while closing the channel", closeResult);
                return;
            }
        }
    } else {
        log:printError("Error occured in retrieving content", getResponse);
        return;
    }
    
    // Rename or move remote file to a another remote location in a same FTP server
    error? renameResponse = ftpClient->rename("<The source file path>", "<The destination file path>");
    if (renameResponse is error) {
        log:printError("Error occurred while renaming the file", renameResponse);
        return;
    }
    
    // Delete remote file
    error? deleteResponse = ftpClient->delete("<The resource path>");
    if (deleteResponse is error) {
        log:printError("Error occurred while deleting a file", deleteResponse);
        return;
    }
    
    // Remove directory from remote server
    var rmdirResponse = ftpClient->rmdir("<The directory path>");
    if (rmdirResponse is error) {
        io:println("Error occured in removing directory.", rmdirResponse); 
        return;
    }
}
```
