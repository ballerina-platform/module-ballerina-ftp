import ballerina/net.fs;

function testFileMoveInvalidPath(fs:FileSystemEvent event, string path) returns (fs:FSError | null) {
    var result = event.move(path);
    match result {
        fs:FSError err => {return err;}
        any => {return null;}
    }
}