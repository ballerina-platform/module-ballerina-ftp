import ballerina.net.fs;
import ballerina.log;
@fs:configuration {
    dirURI:"target/fs",
    events:"create,delete,modify",
    recursive:false
}
service<fs> fileSystem {
    resource fileResource (fs:FileSystemEvent m) {
        log:info(m.name);
        log:info(m.operation);
    }
}
