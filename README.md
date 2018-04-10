# **Ballerina FTP Listener and Client**

## FTP Listener
The FTP Listener can be used to listen to a remote directory. It will keep listening to the specified directory and process the files in the directory as they get added to the directory.
```ballerina
import wso2/ftp;
import ballerina/log;

endpoint ftp:Listener remoteLocation {
    protocol:"ftp",
    host:"localhost",
    port:48123,
    username:"ballerina",
    passPhrase:"ballerina123",
    path:"/home/ballerina",
    pollingInterval:"2000",
    parallel:"false"
};

service monitor bind remoteLocation {
    fileResource (ftp:FileEvent m) {
        log:printInfo(m.uri);
        log:printInfo(m.baseName);
        log:printInfo(m.path);
    }
}
```

## FTP Client
The FTP Client Connector can be used to connect to an FTP server and perform I/O operations.
```ballerina
import wso2/ftp;
import ballerina/file;
import ballerina/io;

endpoint ftp:Client client {
    protocol: "ftp",
    host:"127.0.0.1",
    port:21
};
    
function main (string[] args) {
    // To create a folder in remote server
    ftp:FTPClientError? dirCreErr = client -> mkdir("/ballerina-user/sample-dir");
    match dirCreErr {
        ftp:FTPClientError => {
            io:println("An error occured.");
            return;
        }
        () => {}
    }
    
    // Upload file to a remote server
    io:ByteChannel summaryChannel = io:openFile("/home/ballerina/prog/summary.bal", "r");
    ftp:FTPClientError? filePutErr = client -> put("/ballerina-user/sample-dir/summary.bal", summaryChannel);    
    match filePutErr {
        ftp:FTPClientError => {
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
        ftp:FTPClientError => {
            io:println("An error occured.");
            return;
        }
    }    
    
    // Get a size of a remote file
    int size = check client -> size("/ballerina-user/sample-dir/stock.json");
    
    // Read content of a remote file
    var getResult = client -> get("/ballerina-user/sample-dir/stock.json");
    match getRsult {
        io:ByteChannel channel => {
            io:CharacterChannel characters = check io:createCharacterChannel(channel, "UTF-8");
            json stock = characters.readJson();
            _ = channel.close();
        }
        ftp:FTPClientError => {
            io:println("An error occured.");
            return;
        }
    }    
    
    // Rename or move remote file to a another remote location in a same FTP server
    ftp:FTPClientError? renameErr = client -> rename("/ballerina-user/sample-dir/stock.json", "/ballerina-user/sample-dir/done/stock.json");
    
    // Delete remote file
    ftp:FTPClientError? fileDelCreErr = client -> delete("/ballerina-user/sample-dir/temp/MyMockProxy.xml");
    
    // 
    _ = client -> rmdir("/ballerina-user/sample-dir/temp");  
}
```
## How to install File System Connectors
1. Download correct distribution.zip from [releases](https://github.com/ballerinalang/connector-file/releases) that match with ballerina 
  version.
2. Unzip connector distribution and copy to all jars to <BALLERINA_HOME>/bre/lib folder.

| Ballerina Version | File Connector Version |
| ----------------- | ---------------------- |
| 0.95.0 | 0.95.0 |
| 0.963.0| 0.96.0 |