#**Ballerina File System Connectors**

##File System Server Connector

The File System Server Connector can be used to listen to a directory in the local file system. It will keep listening to the specified directory and process the files in the directory as they get added to the directory.
```ballerina
import ballerina.net.fs;
import ballerina.utils.logger;
@fs:configuration {
    dirURI:"/home/ballerina/programs",
    events:"create,delete,modify",
    recursive:false
}
service<fs> fileSystem {
    resource fileResource (fs:FileSystemEvent m) {
        logger:info(m.name);
        logger:info(m.operation);
    }
}
```
FileSystemEvent struct consists additional two functions to support to file move and file delete.
move(string filePath) and delete()

##FTP Server Connector
The FTP Server Connector can be used to listen to a remote directory. It will keep listening to the specified directory and process the files in the directory as they get added to the directory.
```ballerina
import ballerina.net.ftp;
import ballerina.utils.logger;
@ftp:configuration {
    dirURI:"ftp://ballerina:ballerina123@localhost:48123/home/ballerina",
    pollingInterval:"2000",
    actionAfterProcess:"NONE",
    parallel:"false",
    createMoveDir:"true"
}
service<ftp> ftpServerConnector {
    resource fileResource (ftp:FTPServerEvent m) {
        logger:info(m.name);
    }
}
```

##FTP Client Connector
The FTP Client Connector can be used to connect to an FTP server and perform I/O operations.
```ballerina
import ballerina.lang.system;
import ballerina.net.ftp;
import ballerina.lang.files;
import ballerina.lang.blobs;
import ballerina.lang.strings;

function main (string[] args) {
    ftp:ClientConnector c = create ftp:ClientConnector();
    files:File target = {path:"ftp://127.0.0.1/ballerina-user/aa.txt"};
    boolean filesExists = c.exists(target);
    system:println("File exists: " + filesExists);
    
    files:File newDir = {path:"ftp://127.0.0.1/ballerina-user/new-dir/"};
    c.createFile(newDir, "folder");
    
    files:File txtFile = {path:"ftp://127.0.0.1/ballerina-user/bb.txt"};
    blob contentB = c.read(txtFile);
    system:println(blobs:toString(contentB, "UTF-8"));
    
    files:File copyOfTxt = {path:"ftp://127.0.0.1/ballerina-user/new-dir/copy-of-bb.txt"};
    c.copy(txtFile, copyOfTxt);
    files:File mvSrc = {path:"ftp://127.0.0.1/ballerina-user/aa.txt"};
    files:File mvTarget = {path:"ftp://127.0.0.1/ballerina-user/move/moved-aa.txt"};
    c.move(mvSrc, mvTarget);
    
    files:File del = {path:"ftp://127.0.0.1/ballerina-user/cc.txt"};
    c.delete(del);
    
    files:File wrt = {path:"ftp://127.0.0.1/ballerina-user/dd.txt"};
    blob contentD = strings:toBlob("Hello World!", "UTF-8");
    c.write(contentD, wrt);
}
```

| Ballerina Version | File Connector Version |
| ----------------- | ---------------------- |
| 0.94 | 0.94 |