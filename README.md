# **Ballerina File System Connectors**

## File System Server Connector

The File System Server Connector can be used to listen to a directory in the local file system. It will keep listening to the specified directory and process the files in the directory as they get added to the directory.
```ballerina
import ballerina.net.fs;
import ballerina.log;
@fs:configuration {
    dirURI:"/home/ballerina/programs",
    events:"create,delete,modify",
    recursive:false
}
service<fs> fileSystem {
    resource fileResource (fs:FileSystemEvent m) {
        log:info(m.name);
        log:info(m.operation);
    }
}
```
FileSystemEvent struct consists of additional two functions that support file move and file delete:
move(string filePath) and delete().

## FTP Server Connector
The FTP Server Connector can be used to listen to a remote directory. It will keep listening to the specified directory and process the files in the directory as they get added to the directory.
```ballerina
import ballerina.net.ftp;
import ballerina.log;
@ftp:configuration {
    dirURI:"ftp://ballerina:ballerina123@localhost:48123/home/ballerina",
    pollingInterval:"2000",
    actionAfterProcess:"NONE",
    moveAfterFailure:"NONE",
    parallel:"false",
    createMoveDir:"true"
}
service<ftp> ftpServerConnector {
    resource fileResource (ftp:FTPServerEvent m) {
        log:info(m.name);
    }
}
```

## FTP Client Connector
The FTP Client Connector can be used to connect to an FTP server and perform I/O operations.
```ballerina
import ballerina.net.ftp;
import ballerina.file;

function main (string[] args) {
    endpoint<ftp:FTPClient> c { create ftp:FTPClient();}
    file:File target = {path:"ftp://127.0.0.1/ballerina-user/aa.txt"};
    boolean filesExists = c.exists(target);
    println("File exists: " + filesExists);
    
    file:File newDir = {path:"ftp://127.0.0.1/ballerina-user/new-dir/"};
    c.createFile(newDir, "folder");
    
    file:File txtFile = {path:"ftp://127.0.0.1/ballerina-user/bb.txt"};
    blob contentB = c.read(txtFile);
    println(contentB.toString("UTF-8"));
    
    files:File copyOfTxt = {path:"ftp://127.0.0.1/ballerina-user/new-dir/copy-of-bb.txt"};
    c.copy(txtFile, copyOfTxt);
    
    file:File mvSrc = {path:"ftp://127.0.0.1/ballerina-user/aa.txt"};
    file:File mvTarget = {path:"ftp://127.0.0.1/ballerina-user/move/moved-aa.txt"};
    c.move(mvSrc, mvTarget);
    
    file:File del = {path:"ftp://127.0.0.1/ballerina-user/cc.txt"};
    c.delete(del);
    
    file:File wrt = {path:"ftp://127.0.0.1/ballerina-user/dd.txt"};
    blob contentD = content.toBlob("Hello World!", "UTF-8");
    c.write(contentD, wrt);
}
```
## How to install File System Connectors
1. Download correct distribution.zip from [releases](https://github.com/ballerinalang/connector-file/releases) that match with ballerina 
  version.
2. Unzip connector distribution and copy to all jars to <BALLERINA_HOME>/bre/lib folder.

| Ballerina Version | File Connector Version |
| ----------------- | ---------------------- |
| 0.95.0 | 0.95.0 |