## Module overview
The `wso2/ftp` module provides an FTP client and an FTP server listener implementation to facilitate an FTP connection to a remote location. 

### FTP client
`ftp:Client` connects to an FTP server and performs various operations on the files. It supports `get`, `delete`, `put`, `append`, `mkdir`, `rmdir`, `isDirectory`,  `rename`, `size`, and `list` operations.

An FTP client endpoint is defined using these parameters: `protocol`, `host` and optionally, `port` and `secureSocket`.  Authentication configuration can be configured using the secureSocket parameter for basicAuth, private key, or TrustStore/Keystore.

### FTP listener
`ftp:Listener` is used to listen to a remote FTP location and trigger an event of type `WatchEvent` when new files are added/deleted to the directory. The `fileResource` function is invoked when a new file is added and/or deleted.

An FTP listener endpoint is defined using these parameters: `protocol`, `host`, and  `path` are mandatory parameters.  Authentication configuration can be done using `secureSocket` and polling interval can be configure using `pollingInterval`. Default polling interval is 60 seconds.

## Samples

### Sample FTP client endpoint
```ballerina
endpoint ftp:Client client {
    protocol: io:SFTP",
    host: "ftp.ballerina.com",
    secureSocket: {
        basicAuth: {
            username: "john",
            password: "password"
        }
    }
};
```
### Sample FTP client operations
All of the following operations return `FTPClientError` in case of an error. 

```ballerina
// Make a directory in the remote FTP location.
error? dirCreErr client -> mkdir("/personal/files");  

//Add a file to the FTP location.
io:ByteChannel bchannel = io:openFile("/home/john/files/MyFile.xml", io:READ);
error? filePutErr = client -> put("/personal/files/MyFile.xml", bchannel);

// List the files in the FTP location.
var listOrError = client -> list("/personal/files");

// Rename or move a file in the FTP location.
error? renameErr = client -> rename("/personal/files/MyFile.xml", "/personal/New.xml");

// Read the size of a file in the FTP location.
var sizeOrError = client -> size("/personal/New.xml");

// Download a file from the FTP location.
var byteChannelOrError = client -> get("/personal/New.xml");

// Delete a file in the FTP location.
error? fileDelErr = client -> delete("/personal/New.xml");

// Delete a directory in the FTP location.
error? dirDelErr = client -> rmdir("/personal/files");    
```
### Sample FTP listener endpoint
```ballerina
endpoint ftp:Listener remoteFolder {
    protocol: ftp:SFTP,
    host: "ftp.ballerina.com",
    secureSocket: {
        basicAuth: {
            username: "john",
            password: "password"
        }
    },
    path:"/personal"
};
```
### Sample service for the FTP listener endpoint
```ballerina
service myRemoteFiles bind remoteFolder {
    fileResource (ftp:WatchEvent m) {
        foreach v in m.addedFiles {
            io:println("Added file path: ", v.path);
        }		
        foreach v in m.deletedFiles {
            io:println("Deleted file path: ", v);
        }
    }
}
```
