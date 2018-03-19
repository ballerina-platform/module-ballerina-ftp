package ballerina.net.ftp;

@Field {value:"name: Absolute file URI for triggerd event"}
@Field {value:"size: Size of the file"}
@Field {value:"lastModifiedTimeStamp: Last modified timestamp of the file"}
public struct FTPServerEvent {
    string name;
    int size;
    int lastModifiedTimeStamp;
}

public connector ServerConnector () {}
