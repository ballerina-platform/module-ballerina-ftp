import wso2/ftp;
import ballerina/io;

endpoint ftp:Listener remoteServer {
    protocol: ftp:FTP,
    host: "localhost",
    secureSocket: {
        basicAuth: {
            username: "wso2",
            password: "wso2123"
        }
    },
    port: 48123,
    path: "/home/wso2",
    pollingInterval: 2000
};

int noOfFilesAdded = 0;

service ftpServerConnector bind remoteServer {
    fileResource(ftp:WatchEvent m) {
        io:println("Length: " + lengthof m.addedFiles);
        noOfFilesAdded = lengthof m.addedFiles;
    }
}

function getFileCount() returns int {
    return noOfFilesAdded;
}
