import ballerina/io;
import wso2/ftp;

function createDirectory(string host, string url) {
    endpoint ftp:Client client {
        protocol: ftp:FTP,
        host: host
    };

    _ = client->mkdir(url);
}

function removeDirectory(string host, string url) {
    endpoint ftp:Client client {
        protocol: ftp:FTP,
        host: host
    };

    _ = client->rmdir(url);
}

function readContent(string host, string url) returns string? {
    endpoint ftp:Client client {
        protocol: ftp:FTP,
        host: host
    };

    var output = client->get(url);
    io:ReadableByteChannel byteChannel = check output;
    string? returnValue;
    io:ReadableCharacterChannel? characterChannel1 = new io:ReadableCharacterChannel(byteChannel, "utf-8");
    match characterChannel1 {
        io:ReadableCharacterChannel characterChannel => {
            match readAllCharacters(characterChannel) {
                string str => {
                    returnValue = untaint str;
                    io:println(returnValue);
                }
                error err => {
                    throw err;
                }
                () => {
                    io:println("Empty return from channel.");
                }
            }
            match characterChannel.close() {
                error e1 => {
                    io:println("ReadableCharacterChannel close error: ", e1.message);
                }
                () => {
                    io:println("Connection closed successfully.");
                }
            }
        }
        () => {
        }
    }
    return returnValue;
}

function write(string host, string path, string filePath) {
    endpoint ftp:Client client {
        protocol: ftp:FTP,
        host: host
    };

    io:ReadableByteChannel bchannel = io:openReadableFile(filePath);
    _ = client->put(path, bchannel);
}


function append(string host, string path, string filePath) {
    endpoint ftp:Client client {
        protocol: ftp:FTP,
        host: host
    };

    io:ReadableByteChannel bchannel = io:openReadableFile(filePath);
    _ = client->append(path, bchannel);
}

function fileDelete(string host, string path) {
    endpoint ftp:Client client {
        protocol: ftp:FTP,
        host: host
    };

    _ = client->delete(path);
}

function size(string host, string path) returns int {
    endpoint ftp:Client client {
        protocol: ftp:FTP,
        host: host
    };
    int fileSize = 0;
    var result = client->size(path);
    fileSize = check result;
    return fileSize;
}

function list(string host, string path) returns string[] {
    endpoint ftp:Client client {
        protocol: ftp:FTP,
        host: host
    };
    string[] fileList;
    var result = client->list(path);
    fileList = check result;
    return fileList;
}

function rename(string host, string source, string destination) {
    endpoint ftp:Client client {
        protocol: ftp:FTP,
        host: host
    };

    _ = client->rename(source, destination);
}

function isDirectory(string host, string path) returns boolean {
    endpoint ftp:Client client {
        protocol: ftp:FTP,
        host: host
    };

    return check client->isDirectory(path);
}

function readAllCharacters(io:ReadableCharacterChannel characterChannel) returns string|error? {
    int fixedSize = 50;
    boolean isDone = false;
    string result;
    while (!isDone) {
        match readCharacters(fixedSize, characterChannel) {
            string value => {
                result = result + value;
            }
            error err => {
                if (err.message == "io.EOF"){
                    isDone = true;
                } else {
                    return err;
                }
            }
        }
    }
    return result;
}

function readCharacters(int numberOfCharacters, io:ReadableCharacterChannel characterChannel) returns string|error {
    var result = characterChannel.read(numberOfCharacters);
    match result {
        string characters => {
            return characters;
        }
        error err => {
            return err;
        }
    }
}
