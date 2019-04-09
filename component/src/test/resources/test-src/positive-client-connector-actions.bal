import ballerina/io;
import wso2/ftp;

function createDirectory(string host, string url) returns error? {
    ftp:Client ftpClient = new({ protocol: ftp:FTP, host: host });
    check ftpClient->mkdir(url);
}

function removeDirectory(string host, string url) returns error? {
    ftp:Client ftpClient = new({ protocol: ftp:FTP, host: host });
    check ftpClient->rmdir(url);
}

function readContent(string host, string url) returns string|error? {
    ftp:Client ftpClient = new({ protocol: ftp:FTP, host: host });
    var output = ftpClient->get(url);
    io:ReadableByteChannel byteChannel = check output;
    string? returnValue = ();
    io:ReadableCharacterChannel? characterChannel = new io:ReadableCharacterChannel(byteChannel, "utf-8");
    if (characterChannel is io:ReadableCharacterChannel) {
        var characters = readAllCharacters(characterChannel);
        if (characters is string) {
            returnValue = untaint characters;
            io:println(returnValue);
        } else if (characters is error) {
            return characters;
        } else {
            io:println("Empty return from channel.");
        }
        var closeResult = characterChannel.close();
        if (closeResult is error) {
            io:println("ReadableCharacterChannel close error: ", closeResult.reason());
        } else {
            io:println("Connection closed successfully.");
        }
    } else {
        returnValue = ();
    }
    return returnValue;
}

function write(string host, string path, string filePath) returns error? {
    ftp:Client ftpClient = new({ protocol: ftp:FTP, host: host });
    io:ReadableByteChannel bchannel = io:openReadableFile(filePath);
    check ftpClient->put(path, bchannel);
}


function append(string host, string path, string filePath) returns error? {
    ftp:Client ftpClient = new({ protocol: ftp:FTP, host: host });
    io:ReadableByteChannel bchannel = io:openReadableFile(filePath);
    check ftpClient->append(path, bchannel);
}

function fileDelete(string host, string path) returns error? {
    ftp:Client ftpClient = new({ protocol: ftp:FTP, host: host });
    check ftpClient->delete(path);
}

function size(string host, string path) returns int|error {
    ftp:Client ftpClient = new({ protocol: ftp:FTP, host: host });
    int fileSize = 0;
    var result = ftpClient->size(path);
    fileSize = check result;
    return fileSize;
}

function list(string host, string path) returns string[]|error {
    ftp:Client ftpClient = new({ protocol: ftp:FTP, host: host });
    string[] fileList;
    var result = ftpClient->list(path);
    fileList = check result;
    return fileList;
}

function rename(string host, string source, string destination) returns error? {
    ftp:Client ftpClient = new({ protocol: ftp:FTP, host: host });
    check ftpClient->rename(source, destination);
}

function isDirectory(string host, string path) returns boolean|error {
    ftp:Client ftpClient = new({ protocol: ftp:FTP, host: host });
    return check ftpClient->isDirectory(path);
}

function readAllCharacters(io:ReadableCharacterChannel characterChannel) returns string|error? {
    int fixedSize = 500;
    boolean isDone = false;
    string result = "";
    while (!isDone) {
        var readResult = readCharacters(fixedSize, characterChannel);
        if (readResult is string) {
            result = result + readResult;
        } else {
            if (<string>readResult.detail()["message"] == "io.EOF") {
                isDone = true;
            } else {
                return readResult;
            }
        }
    }
    return result;
}

function readCharacters(int numberOfCharacters, io:ReadableCharacterChannel characterChannel) returns string|error {
    var result = characterChannel.read(numberOfCharacters);
    if (result is string) {
        return result;
    } else {
        return result;
    }
}
