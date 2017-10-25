import ballerina.net.fs;
import ballerina.utils.logger;
@fs:configuration {
    dirURI:"target/fs",
    events:"create,delete,modify",
    recursive:false
}
service<fs> fileSystem {
    resource fileResource (fs:FileSystemEvent m) {
        logger:info(m.name);
        logger:info(m.operation);
    }
}
