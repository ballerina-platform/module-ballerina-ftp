import ballerina.net.fs;
import ballerina.io;

service<fs> fileSystem {
    resource fileResource (fs:FileSystemEvent m) {
        io:println(m.name);
        io:println(m.operation);
    }
}
