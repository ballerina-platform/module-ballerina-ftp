import ballerina/net.fs;

function testFileMove(fs:FileSystemEvent event, string path) {
    _ = event.move(path);
}

function testFileDelete(fs:FileSystemEvent event) {
    _ = event.delete();
}