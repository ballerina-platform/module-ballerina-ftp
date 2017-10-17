import ballerina.net.ftp;
import ballerina.lang.system;

service<ftp> ftpServerConnector {
    resource fileResource (ftp:FTPServerEvent m) {
        system:println(m.name);
    }
}
