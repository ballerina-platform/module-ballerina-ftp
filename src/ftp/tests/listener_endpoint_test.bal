import ballerina/log;

listener Listener remoteServer = new({
    protocol: FTP,
    host: "192.168.112.16",
    secureSocket: {
        basicAuth: {
            username: "ftp-user",
            password: "ftp123"
        }
    },
    port: 21,
    path: "/home/in",
    pollingInterval: 2000,
    fileNamePattern: "(.*).txt"
});

service ftpServerConnector on remoteServer {
    resource function fileResource(WatchEvent m) {
        log:printInfo("In service resource");
        log:printInfo("Files: "+ m.addedFiles.toString());
        log:printInfo("Files: "+ m.deletedFiles.toString());

        int i =0;
        log:printInfo("Length: "+m.addedFiles.length().toString());
        while(i < m.addedFiles.length()){
            log:printInfo("i: "+i.toString());
            FileInfo file = m.addedFiles[i];
            log:printInfo("New: " + file.toString());
            log:printInfo("File: "+ file.path);
            i = i+1;
        }

        foreach FileInfo v1 in m.addedFiles {
            log:printInfo("Added file path: " + v1.path);
        }
        log:printInfo("Length: " + m.addedFiles.length().toString());
        //noOfFilesAdded = <@untainted> m.addedFiles.length();
        foreach string v1 in m.deletedFiles {
            log:printInfo("Deleted file path: " + v1);
        }
        log:printInfo("Length: " + m.deletedFiles.length().toString());
    }
}
