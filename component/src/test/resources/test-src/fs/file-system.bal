import ballerina/net.fs;

endpoint fs:ServiceEndpoint localFolder {
    dirURI:"target/fs",
    events:"create,delete,modify",
    recursive:false
};

boolean invoked = false;

@fs:serviceConfig {
}
service<fs:Service> fileSystem bind localFolder {
    fileResource (fs:FileSystemEvent m) {
        invoked = true;
    }
}

function isInvoked() returns boolean {
    return invoked;
}