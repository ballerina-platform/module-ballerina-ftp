import ballerina.net.ftp;
import ballerina.io;

@ftp:configuration {
    dirURI:"ftp://baluser@localhost/folder",
    pollingInterval:"2000",
    actionAfterProcess:"NONE",
    parallel:"false",
    createMoveDir:"true"
}
service<ftp> ftpServerConnector {
    resource fileResource (ftp:FTPServerEvent m) {
        io:println(m.name);
    }
    resource fileResource2 (ftp:FTPServerEvent m) {
        io:println(m.name);
    }
    resource fileResource3 (ftp:FTPServerEvent m) {
        io:println(m.name);
    }
}
