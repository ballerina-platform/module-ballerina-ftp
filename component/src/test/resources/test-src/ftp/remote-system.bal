import ballerina/io;
import ballerina/net.ftp;

endpoint ftp:ServiceEndpoint remoteServer {
    dirURI:"ftp://wso2:wso2123@localhost:48123/home/wso2",
    pollingInterval:"2000",
    actionAfterProcess:"NONE",
    parallel:"false",
    createMoveDir:"true"
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

function isInvoked() returns boolean {
    return invoked;
}