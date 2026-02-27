import ballerina/ftp;
import ballerina/lang.runtime as runtime;
import ballerina/log;
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
    runtime:registerListener(rsListener);
    remoteServerListener = rsListener;

    ftp:Listener arsListener = check new (anonymousRemoteServerConfig);
    check arsListener.attach(anonymousRemoteServerService);
    check arsListener.'start();
    runtime:registerListener(arsListener);
    anonymousRemoteServerListener = arsListener;
}

@test:AfterSuite {}
function cleanListenerTestEnvironment() {
    ftp:Listener? rsListener = remoteServerListener;
    if rsListener is ftp:Listener {
        runtime:deregisterListener(rsListener);
        error? e = rsListener.gracefulStop();
        if e is error {
            log:printError("Error stopping remoteServerListener", 'error = e);
        }
    }
    remoteServerListener = ();
    ftp:Listener? arsListener = anonymousRemoteServerListener;
    if arsListener is ftp:Listener {
        runtime:deregisterListener(arsListener);
        error? e = arsListener.gracefulStop();
        if e is error {
            log:printError("Error stopping anonymousRemoteServerListener", 'error = e);
        }
    }
    anonymousRemoteServerListener = ();
    triggerClient = ();
}
