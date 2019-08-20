import ballerina/io;
import ballerina/test;

@test:Config{}
public function testOne() {

    ClientEndpointConfig config = {
        protocol: FTP,
        host: "192.168.112.3",
        port: 1212,
        secureSocket: {basicAuth: {username: "ftp-user", password: "ftp123"}}
    };
    log:printInfo("in test method");
    Client clientEP = new(config);

    clientEP -> get("/home/ubuntu/in/test.txt");

    //io:ReadableByteChannel|error byteChannel = io:openReadableFile("./tests/resources/file1.txt");
    //if(byteChannel is io:ReadableByteChannel){
    //    clientEP -> append("/home/ubuntu/in/test.txt", byteChannel);
    //}
    //
    //clientEP -> delete("/home/ubuntu/in/test.txt");
    //
    //io:ReadableByteChannel|error byteChannel = io:openReadableFile("./tests/resources/file1.txt");
    //if(byteChannel is io:ReadableByteChannel){
    //    clientEP -> put("/home/ubuntu/in/test.txt", byteChannel);
    //}
    //
    //clientEP -> isDirectory("/home/ubuntu/in");

    test:assertTrue(true);
}
