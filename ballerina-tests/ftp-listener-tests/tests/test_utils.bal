import ballerina/ftp;
import ballerina/test;

// FTP client config for triggering listener events
ftp:ClientConfiguration ftpConfig = {
    protocol: ftp:FTP,
    host: "127.0.0.1",
    port: 21212,
    auth: {credentials: {username: "wso2", password: "wso2123"}},
    userDirIsRoot: false
};

ftp:Client? triggerClient = ();

string putFilePath = "tests/resources/datafiles/file2.txt";

@test:BeforeSuite
function initListenerTestEnvironment() returns error? {
    triggerClient = check new (ftpConfig);
}

@test:AfterSuite {}
function cleanListenerTestEnvironment() returns error? {
    ftp:Client? client = triggerClient;
    if client is ftp:Client {
        triggerClient = ();
    }
}
