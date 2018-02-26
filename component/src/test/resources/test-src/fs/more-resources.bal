import ballerina.net.fs;
import ballerina.io;

@fs:configuration {
    dirURI:"target/fs",
    events:"create,delete,modify",
    recursive:false
}
service<fs> fileSystem {
    resource fileResource1 (fs:FileSystemEvent m) {
        io:println(m.name);
        io:println(m.operation);
    }
    resource fileResource2 (fs:FileSystemEvent m) {
        io:println(m.name);
        io:println(m.operation);
    }
    resource fileResource3 (fs:FileSystemEvent m) {
        io:println(m.name);
        io:println(m.operation);
    }
}
