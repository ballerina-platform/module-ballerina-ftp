## Module overview
The `wso2/ftp` module provides an FTP client and an FTP server listener implementation to facilitate an FTP connection to a remote location. 

### FTP client
`ftp:Client` connects to an FTP server and performs various operations on the files. It supports `get`, `delete`, `put`, `append`, `mkdir`, `rmdir`, `isDirectory`,  `rename`, `size`, and `list` operations.

An FTP client endpoint is defined using these parameters: `protocol`, `host` and optionally, `port` and `secureSocket`.  Authentication configuration can be configured using the secureSocket parameter for basicAuth, private key, or TrustStore/Keystore.

### FTP listener
`ftp:Listener` is used to listen to a remote FTP location and trigger an event of type `WatchEvent` when new files are added/deleted to the directory. The `fileResource` function is invoked when a new file is added and/or deleted.

An FTP listener endpoint is defined using these parameters: `protocol`, `host`, and  `path` are mandatory parameters.  Authentication configuration can be done using `secureSocket` and polling interval can be configure using `pollingInterval`. Default polling interval is 60 seconds.


## Samples

### Obtaining the credentials to Run the Sample

1. Install and configure FTP Server. For more information, see [https://www.unixmen.com/install-configure-ftp-server-ubuntu/](https://www.unixmen.com/install-configure-ftp-server-ubuntu/)
2. obtain ftpUsername, ftpPassword, ftpServer, ftpPort
.
You can now enter the credentials in the FTP client config:

```ballerina
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
```
### Sample FTP client operations
All of the following operations return `FTPClientError` in case of an error. 

```ballerina
// Make a directory in the remote FTP location.
var dirCreErr = ftpClient -> mkdir("<The directory path>");
if (dirCreErr is error) {
    io:println("An error occured.");
    return;
}

//Add a file to the FTP location.
io:ReadableByteChannel summaryChannel = io:openReadableFile("<The local data source path>");
var filePutErr = ftpClient -> put("<The resource path>", summaryChannel);    
if (filePutErr is error) {
    io:println("An error occured.");
    return;
}

// List the files in the FTP location.
var listResult = ftpClient -> list("<The resource path>");
if (listResult is string[]) {
    foreach string file in listResult {
        io:println("File: " + file);
    }
} else {
    io:println("An error occured.");
    return;
}

// Read the size of a file in the FTP location.
var size = ftpClient -> size("<The resource path>");
if (size is int) {
    io:println("File size: " + size);
} else {
    io:println("An error occured.");
    return;
}

// Download a file from the FTP location.
var getResult = ftpClient -> get("/home/kalai/symbol193/a.bal");
if (getResult is io:ReadableByteChannel) {
    io:ReadableCharacterChannel? characters = new io:ReadableCharacterChannel(getResult, "utf-8");
    if (characters is io:ReadableCharacterChannel) {
        io:println("File content : ", characters.readJson()); 
        var closeResult = characters.close();
    }
} else {
    io:println("An error occured.");
    return;
}

// Rename or move remote file to a another remote location in a same FTP server.
error? renameErr = ftpClient -> rename("<The source file path>", "<The destination file path>");

// Delete a file in the FTP location.
error? fileDelCreErr = ftpClient -> delete("<The resource path>");

// Delete a directory in the FTP location.
var result = ftpClient -> rmdir("<The directory path>");
if (result is error) {
    io:println("An error occured."); 
}
   
```
### Sample FTP listener endpoint
```ballerina
listener ftp:Listener remoteServer = new ({
    protocol: ftp:FTP,
    host: "<The FTP host>",
    port: <The FTP port>,
    secureSocket: {
        basicAuth: {
            username: "<The FTP username>",
            password: "<The FTP passowrd>"
        }
    },
    path: "<The remote FTP direcotry location>"
});
```
### Sample service for the FTP listener endpoint
```ballerina
service monitor on remoteServer {
    resource function fileResource(ftp:WatchEvent m) {
        foreach ftp:FileInfo v1 in m.addedFiles {
            log:printInfo("Added file path: " + v1.path);
        }
        
        foreach string v2 in m.deletedFiles {
            log:printInfo("Deleted file path: " + v2);
        }
    }
}
```
