import ballerina.net.fs;
import ballerina.log;

endpoint<fs:Service> localFolder {
    dirURI:"target/fs",
    events:"create,delete,modify",
    recursive:false
}

@fs:serviceConfig {
    endpoints:[localFolder]
}
service<fs:Service> fileSystem {
    resource fileResource (fs:FileSystemEvent m) {
        log:printInfo(m.name);
        log:printInfo(m.operation);
    }
}
