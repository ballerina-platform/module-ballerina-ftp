import ballerina.net.fs;
import ballerina.lang.system;
@fs:configuration {
    dirURI:"/home/gihan/Desktop/temp",
    events:"create,delete,modify",
    recursive:false
}
service<fs> fileSystem {
    resource fileResource1 (fs:FileSystemEvent m) {
        system:println(m.name);
        system:println(m.operation);
    }
    resource fileResource2 (fs:FileSystemEvent m) {
        system:println(m.name);
        system:println(m.operation);
    }
    resource fileResource3 (fs:FileSystemEvent m) {
        system:println(m.name);
        system:println(m.operation);
    }
}
