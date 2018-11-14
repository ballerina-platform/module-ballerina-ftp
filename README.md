[![Build Status](https://travis-ci.org/wso2-ballerina/module-ftp.svg?branch=master)](https://travis-ci.org/wso2-ballerina/module-ftp)

# **Ballerina FTP Listener and Client**

## FTP Listener
The FTP Listener can be used to listen to a remote directory. It will keep listening to the specified directory and periodically notify the file addition and deletion.
```ballerina
import wso2/ftp;
import ballerina/io;

endpoint ftp:Listener remoteLocation {
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
};

service monitor bind remoteLocation {
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

## FTP Client
The FTP Client Connector can be used to connect to an FTP server and perform I/O operations.
```ballerina
import wso2/ftp;
import ballerina/io;

endpoint ftp:Client client {
    protocol: ftp:FTP,
    host:"127.0.0.1",
    port:21
};
    
function main (string... args) {
    // To create a folder in remote server
    error? dirCreErr = client -> mkdir("/ballerina-user/sample-dir");
    match dirCreErr {
        error err => {
            io:println("An error occured.");
            return;
        }
        () => {}
    }
    
    // Upload file to a remote server
    io:ReadableByteChannel summaryChannel = io:openReadableFile("/home/ballerina/prog/summary.bal");
    error? filePutErr = client -> put("/ballerina-user/sample-dir/summary.bal", summaryChannel);    
    match filePutErr {
        error err => {
            io:println("An error occured.");
            return;
        }
        () => {}
    }
    
    // Get the content list of a given path
    var listResult = client -> list("/ballerina-user/sample-dir");
    match  listResult {
        string[] list => {
            foreach file in list {
                io:println("File: " + file);
            }
        }
        error err=> {
            io:println("An error occured.");
            return;
        }
    }    
    
    // Get the size of a remote file
    int size = check client -> size("/ballerina-user/sample-dir/stock.json");
    
    // Read content of a remote file
    var getResult = client -> get("/ballerina-user/sample-dir/stock.json");
    match getResult {
        io:ReadableByteChannel byteChannel => {
            io:ReadableByteChannel characters = check io:ReadableByteChannel(byteChannel, "utf-8");
            json stock = check characters.readJson();
            _ = byteChannel.close();
        }
        error err => {
            io:println("An error occured.");
            return;
        }
    }    
    
    // Rename or move remote file to a another remote location in a same FTP server
    error? renameErr = client -> rename("/ballerina-user/sample-dir/stock.json", "/ballerina-user/sample-dir/done/stock.json");
    
    // Delete remote file
    error? fileDelCreErr = client -> delete("/ballerina-user/sample-dir/temp/MyMockProxy.xml");
    
    // Remove direcotry from remote server 
    _ = client -> rmdir("/ballerina-user/sample-dir/temp");  
}
```
## How to install FTP Connectors
1. Download correct distribution.zip from [releases](https://github.com/wso2-ballerina/module-ftp/releases) that match with ballerina 
  version.
2. Unzip package distribution.
3. Run the install.<sh|bat> script to install the package. You can uninstall the package by running uninstall.<sh|bat>.

| Ballerina Version | File Connector Version |
| ----------------- | ---------------------- |
| 0.983.0| 0.98.0 |
| 0.982.0| 0.97.5 |
| 0.981.0| 0.97.4 |
| 0.980.0| 0.97.3 |
| 0.975.0| 0.97.1 |
| 0.970.0| 0.97.0 |
| 0.963.0| 0.96.0 |
| 0.95.0 | 0.95.0 |
