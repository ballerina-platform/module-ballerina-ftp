import ballerina/net.fs;

endpoint fs:DirectoryListener localFolder {
    path:"target/fs",
    recursive:false
};

boolean invoked = false;

@fs:ServiceConfig {
}
service<fs:Service> fileSystem bind localFolder {
    onCreate (fs:FileEvent m) {
        invoked = true;
    }
}

function isInvoked() returns boolean {
    return invoked;
}