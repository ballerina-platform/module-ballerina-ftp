import ballerina.net.fs;
import ballerina.lang.system;
service<fs> fileSystem {
    resource fileResource (fs:FileSystemEvent m) {
        system:println(m.name);
        system:println(m.operation);
    }
}
