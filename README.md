[![Build Status](https://travis-ci.org/wso2-ballerina/module-ftp.svg?branch=master)](https://travis-ci.org/wso2-ballerina/module-ftp)

# **Ballerina FTP Listener and Client**

## FTP Listener
The FTP Listener can be used to listen to a remote directory. It will keep listening to the specified directory and periodically notify the file addition and deletion.
```ballerina
import wso2/ftp;
import ballerina/io;

listener ftp:Listener remoteServer = new({
    protocol:ftp:FTP,
    host:"localhost",
    port:48123,
    secureSocket: {
        basicAuth: {
            username: "ballerina",
            password: "ballerina123"
        }
    },
    path:"/home/ballerina"
});

service monitor on remoteLocation {
    resource function fileResource (ftp:WatchEvent m) {
        foreach v in m.addedFiles {
            io:println("Added file path: ", v.path);
        }
        
        foreach v in m.deletedFiles {
            io:println("Deleted file path: ", v);
        }
    }
}
```

## FTP Client
The FTP Client Connector can be used to connect to an FTP server and perform I/O operations.
```ballerina
import wso2/ftp;
import ballerina/io;
    
function main (string... args) {
    ftp:Client ftpClient = new({ protocol: ftp:FTP, host: "127.0.0.1", port: 21 });
    // To create a folder in remote server
    var dirCreErr = ftpClient -> mkdir("/ballerina-user/sample-dir");
    if (dirCreErr is error) {
        io:println("An error occured.");
        return;
    }
    
    // Upload file to a remote server
    io:ReadableByteChannel summaryChannel = io:openReadableFile("/home/ballerina/prog/summary.bal");
    var filePutErr = ftpClient -> put("/ballerina-user/sample-dir/summary.bal", summaryChannel);    
    if (filePutErr is error) {
        io:println("An error occured.");
        return;
    }
    
    // Get the content list of a given path
    var listResult = ftpClient -> list("/ballerina-user/sample-dir");
    if (listResult is string[]) {
        foreach file in listResult {
            io:println("File: " + file);
        }
    } else {
        io:println("An error occured.");
        return;
    }
    
    // Get the size of a remote file
    int size = check ftpClient -> size("/ballerina-user/sample-dir/stock.json");
    
    // Read content of a remote file
    var getResult = ftpClient -> get("/ballerina-user/sample-dir/stock.json");
    if (getResult is io:ReadableByteChannel) {
        io:ReadableByteChannel characters = check io:ReadableByteChannel(getResult, "utf-8");
        json stock = check characters.readJson();
        _ = byteChannel.close();
    } else {
        io:println("An error occured.");
        return;
    }
    
    // Rename or move remote file to a another remote location in a same FTP server
    error? renameErr = ftpClient -> rename("/ballerina-user/sample-dir/stock.json", "/ballerina-user/sample-dir/done/stock.json");
    
    // Delete remote file
    error? fileDelCreErr = ftpClient -> delete("/ballerina-user/sample-dir/temp/MyMockProxy.xml");
    
    // Remove direcotry from remote server 
    _ = ftpClient -> rmdir("/ballerina-user/sample-dir/temp");  
}
```
## How to install FTP Connectors
1. Download correct distribution.zip from [releases](https://github.com/wso2-ballerina/module-ftp/releases) that match with ballerina 
  version.
2. Unzip package distribution.
3. Run the install.<sh|bat> script to install the package. You can uninstall the package by running uninstall.<sh|bat>.

| Ballerina Version | File Connector Version |
| ----------------- | ---------------------- |
| 0.990.0| 0.99.0 |
| 0.983.0| 0.98.0 |
| 0.982.0| 0.97.5 |
| 0.981.0| 0.97.4 |
| 0.980.0| 0.97.3 |
| 0.975.0| 0.97.1 |
| 0.970.0| 0.97.0 |
| 0.963.0| 0.96.0 |
| 0.95.0 | 0.95.0 |
