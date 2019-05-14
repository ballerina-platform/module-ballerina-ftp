import ballerina/io;
import wso2/ftp;

ftp:ClientEndpointConfig ftpConfig = {
    protocol: ftp:FTP,
    host: "localhost",
    port: 49567,
    secureSocket: {
        basicAuth: {
            username: "wso2",
            password: "wso2123"
        }
    }
};

ftp:Client ftpClient = new(ftpConfig);

function createDirectory(string path) returns error? {
    check ftpClient -> mkdir(path);
}

function removeDirectory(string path) returns error? {
    check ftpClient -> rmdir(path);
}

function readContent(string path) returns string|error? {
    var output = ftpClient -> get(path);
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

function write(string path, string filePath) returns error? {
    io:ReadableByteChannel bchannel = io:openReadableFile(filePath);
    check ftpClient -> put(path, bchannel);
}


function append(string path, string filePath) returns error? {
    io:ReadableByteChannel bchannel = io:openReadableFile(filePath);
    check ftpClient -> append(path, bchannel);
}

function fileDelete(string path) returns error? {
    check ftpClient -> delete(path);
}

function size(string path) returns int|error {
    int fileSize = 0;
    var result = ftpClient -> size(path);
    fileSize = check result;
    return fileSize;
}

function list(string path) returns string[]|error {
    string[] fileList;
    var result = ftpClient -> list(path);
    fileList = check result;
    return fileList;
}

function rename(string source, string destination) returns error? {
    check ftpClient -> rename(source, destination);
}

function isDirectory(string path) returns boolean|error {
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
