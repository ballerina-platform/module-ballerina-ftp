import ballerina/io;
import ballerina/test;
import ballerina/log;

ClientEndpointConfig config = {
    protocol: FTP,
    host: "192.168.112.14",
    port: 21,
    secureSocket: {basicAuth: {username: "ftp-user", password: "ftp123"}}
};

string filePath = "/home/in/test.txt";
string appendFilePath = "src/ftp/tests/resources/file1.txt";
string putFilePath = "src/ftp/tests/resources/file2.txt";

Client clientEP = new(config);

@test:Config{
}
public function testGet() {
    io:ReadableByteChannel|error response = clientEP -> get(filePath);
    if(response is io:ReadableByteChannel){
        log:printInfo("Initial content in file: " + response.read(20).toString());
    } else {
        log:printError(response.reason().toString());
    }
    log:printInfo("Executed Get operation");
}

@test:Config{
    dependsOn: ["testGet"]
}
public function testAppend() {
    io:ReadableByteChannel|error byteChannel = io:openReadableFile(appendFilePath);
    if(byteChannel is io:ReadableByteChannel){
        error? response = clientEP -> append(filePath, byteChannel);
        if(response is error) {
            log:printError(response.reason().toString());
        }
    }
    log:printInfo("Executed Append operation.");
}

@test:Config{
    dependsOn: ["testAppend"]
}
public function testPut() {
    io:ReadableByteChannel|error byteChannelToPut = io:openReadableFile(putFilePath);
    if(byteChannelToPut is io:ReadableByteChannel){
        error? response = clientEP -> put(filePath, byteChannelToPut);
        if(response is error) {
            log:printError(response.reason().toString());
        }
    }
    log:printInfo("Executed Put operation.");
}

@test:Config{
    dependsOn: ["testPut"]
}
public function testIsDirectory() {
    boolean|error response = clientEP -> isDirectory("/home/in");
    if(response is boolean) {
        log:printInfo("Is directory: " + response.toString());
    } else {
        log:printError(response.reason().toString());
    }
    log:printInfo("Executed Is directory operation.");
}

@test:Config{
    dependsOn: ["testIsDirectory"]
}
public function testMkdir() {
    error? response = clientEP -> mkdir("/home/in/out");
    if(response is error) {
        log:printError(response.reason().toString());
    }
    log:printInfo("Executed Mkdir operation.");
}

@test:Config{
    dependsOn: ["testMkdir"]
}
public function testRename() {
    string existingName = "/home/in/out";
    string newName = "/home/in/test";
    error? response = clientEP -> rename(existingName, newName);
    if(response is error) {
        log:printError(response.reason().toString());
    }
    log:printInfo("Executed Rename operation.");
}

@test:Config{
    dependsOn: ["testRename"]
}
public function testSize() {
    int|error response = clientEP -> size(filePath);
    if(response is int){
        log:printInfo("Size: "+response.toString());
    } else {
        log:printError(response.reason().toString());
    }
    log:printInfo("Executed size operation.");
}

@test:Config{
    dependsOn: ["testSize"]
}
public function testList() {
    string[]|error response = clientEP -> list("/home/in");
    if(response is string[]){
        log:printInfo("List of directories: " + response[0] + ", " + response[1]);
    } else {
        log:printError(response.reason().toString());
    }
    log:printInfo("Executed List operation.");
}

@test:Config{
    dependsOn: ["testList"]
}
public function testDelete() {
    error? response = clientEP -> delete(filePath);
    if(response is error) {
        log:printError(response.reason().toString());
    }
    log:printInfo("Executed Delete operation.");
}

@test:Config{
    dependsOn: ["testDelete"]
}
public function testRmdir() {
    error? response = clientEP -> rmdir("/home/in/test");
    if(response is error) {
        log:printError(response.reason().toString());
    }
    log:printInfo("Executed Rmdir operation.");
}

