import ballerina.net.fs;
import ballerina.lang.system;
@fs:configuration {
    dirURI:"target/fs",
    events:"create,delete,modify",
    recursive:false
}
service<fs> fileSystem {
    resource fileResource (fs:SystemEvent m) {
        system:println(m.name);
        system:println(m.operation);
    }
}
