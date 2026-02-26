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

ftp:Listener? remoteServerListener = ();
ftp:Listener? anonymousRemoteServerListener = ();

string putFilePath = "tests/resources/datafiles/file2.txt";

@test:BeforeSuite
function initListenerTestEnvironment() returns error? {
    triggerClient = check new (ftpConfig);

    ftp:Listener rsListener = check new (remoteServerConfiguration);
    check rsListener.attach(remoteServerService);
    check rsListener.'start();
    remoteServerListener = rsListener;

    ftp:Listener arsListener = check new (anonymousRemoteServerConfig);
    check arsListener.attach(anonymousRemoteServerService);
    check arsListener.'start();
    anonymousRemoteServerListener = arsListener;
}

@test:AfterSuite {}
function cleanListenerTestEnvironment() returns error? {
    ftp:Listener? rsListener = remoteServerListener;
    if rsListener is ftp:Listener {
        check rsListener.gracefulStop();
    }
    remoteServerListener = ();
    ftp:Listener? arsListener = anonymousRemoteServerListener;
    if arsListener is ftp:Listener {
        check arsListener.gracefulStop();
    }
    anonymousRemoteServerListener = ();
    triggerClient = ();
}
