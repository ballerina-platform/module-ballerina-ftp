import ballerina/ftp;

listener ftp:Listener ftpListener = check new ({
    host: "127.0.0.1",
    auth: {
        credentials: {
            username: "wso2",
            password: "wso2123"
        }
    },
    port: 21212,
    path: "/home/in",
    pollingInterval: 2,
    fileNamePattern: "(.*).txt"
});

service on ftpListener {
    remote function onFileDelete(string deletedFile, ftp:Caller caller, string extra) returns error? {
        return;
    }
}
