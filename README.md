[![Build Status](https://travis-ci.org/wso2-ballerina/module-ftp.svg?branch=master)](https://travis-ci.org/wso2-ballerina/module-ftp)

# **Ballerina FTP Listener and Client**

## FTP Listener
The FTP Listener can be used to listen to a remote directory. It will keep listening to the specified directory and periodically notify the file addition and deletion.

```ballerina
import wso2/ftp;
import ballerina/log;

listener ftp:Listener remoteServer = new({
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

## FTP Client
The FTP Client Connector can be used to connect to an FTP server and perform I/O operations.

```ballerina
import wso2/ftp;
import ballerina/io;

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
    
public function main(string... args) {
    // To create a folder in remote server.
    var dirCreErr = ftpClient -> mkdir("<The directory path>");
    if (dirCreErr is error) {
        io:println("An error occured.");
        return;
    }
    
    // Upload file to a remote server.
    io:ReadableByteChannel summaryChannel = io:openReadableFile("<The local data source path>");
    var filePutErr = ftpClient -> put("<The resource path>", summaryChannel);    
    if (filePutErr is error) {
        io:println("An error occured.");
        return;
    }
    
    // Get the content list of a given path.
    var listResult = ftpClient -> list("<The resource path>");
    if (listResult is string[]) {
        foreach string file in listResult {
            io:println("File: " + file);
        }
    } else {
        io:println("An error occured.");
        return;
    }
    
    // Get the size of a remote file.
    var size = ftpClient -> size("<The resource path>");
    if (size is int) {
        io:println("File size: " + size);
    } else {
        io:println("An error occured.");
        return;
    }
    
    // Read content of a remote file.
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
    
    // Delete remote file.
    error? fileDelCreErr = ftpClient -> delete("<The resource path>");
    
    // Remove directory from remote server.
   var result = ftpClient -> rmdir("<The directory path>");
   if (result is error) {
        io:println("An error occured."); 
   }
}
```

## How to install FTP Connectors

1. Download correct distribution.zip from [releases](https://github.com/wso2-ballerina/module-ftp/releases) that match with ballerina 
  version.
2. Unzip package distribution.
3. Run the install.<sh|bat> script to install the package. You can uninstall the package by running uninstall.<sh|bat>.

| Ballerina Version | File Connector Version |
| ----------------- | ---------------------- |
| 0.991.0| 0.99.2 |
| 0.990.3| 0.99.1 |
| 0.990.0| 0.99.0 |
| 0.983.0| 0.98.0 |
| 0.982.0| 0.97.5 |
| 0.981.0| 0.97.4 |
| 0.980.0| 0.97.3 |
| 0.975.0| 0.97.1 |
| 0.970.0| 0.97.0 |
| 0.963.0| 0.96.0 |
| 0.95.0 | 0.95.0 |
