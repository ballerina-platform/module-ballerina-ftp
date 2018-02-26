import ballerina.net.ftp;
import ballerina.io;

@ftp:configuration {
    dirURI:"ftp://wso2:wso2123@localhost:48123/home/wso2",
    pollingInterval:"2000",
    actionAfterProcess:"NONE",
    parallel:"false",
    createMoveDir:"true"
}
service<ftp> ftpServerConnector {
    resource fileResource (ftp:FTPServerEvent m) {
        io:println(m.name);
    }
}
