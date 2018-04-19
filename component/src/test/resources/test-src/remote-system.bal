import wso2/ftp;
import ballerina/io;

endpoint ftp:Listener remoteServer {
    protocol:"ftp",
    host:"localhost",
    username:"wso2",
    passPhrase:"wso2123",
    port:48123,
    path:"/home/wso2",
    pollingInterval:2000
};

boolean invoked = false;

service ftpServerConnector bind remoteServer {
    fileResource (ftp:FileEvent m) {
        io:println(m.uri);
        invoked = true;
    }
}

function isInvoked () returns boolean {
    return invoked;
}
