import wso2/ftp;
import ballerina/log;

listener ftp:Listener remoteServer = new({
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
});

int noOfFilesAdded = 0;

service ftpServerConnector on remoteServer {
    resource function fileResource(ftp:WatchEvent m) {
        foreach ftp:FileInfo v1 in m.addedFiles {
            log:printInfo("Added file path: " + v1.path);
        }
        log:printInfo("Length: " + m.addedFiles.length());
        noOfFilesAdded = m.addedFiles.length();
    }
}

function getFileCount() returns int {
    return noOfFilesAdded;
}
