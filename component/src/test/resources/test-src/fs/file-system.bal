import ballerina.net.fs;
import ballerina.io;

@fs:configuration {
    dirURI:"target/fs",
    events:"create,delete,modify",
    recursive:false
}
service<fs> fileSystem {
    resource fileResource (fs:FileSystemEvent m) {
        io:println(m.name);
        io:println(m.operation);
    }
}
