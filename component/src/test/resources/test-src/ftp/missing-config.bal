import ballerina.net.ftp;
import ballerina.io;

service<ftp> ftpServerConnector {
    resource fileResource (ftp:FTPServerEvent m) {
        io:println(m.name);
    }
}
