import ballerina/ftp;
import ballerina/io;

endpoint ftp:ServiceEndpoint remoteServer {
    protocol:"ftp",
    host:"localhost",
    username:"wso2",
    passPhrase:"wso2123",
    port:48123,
    path:"/home/wso2",
    pollingInterval:"2000",
    parallel:"false"
};

boolean invoked = false;

@ftp:serviceConfig {
}
service<ftp:Service> ftpServerConnector bind remoteServer {
    fileResource (ftp:FTPServerEvent m) {
        io:println(m.name);
        invoked = true;
    }
}

function isInvoked () returns boolean {
    return invoked;
}
