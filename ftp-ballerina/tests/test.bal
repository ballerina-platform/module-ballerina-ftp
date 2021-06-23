// import ballerina/io;
// import ballerina/test;

// // Define FTP client configuration
// ClientConfiguration ftpConfig = {
//     protocol: FTP,
//     host: "localhost",
//     port: 9090
// };

// @test:Config {}
// public function testFunction() {
//     Client ftpClient = new(ftpConfig);
//     Error? mkdirResponse = ftpClient->mkdir("/Users/bhashinee/Documents/Development/examples");
//     if (mkdirResponse is Error) {
//         io:println(mkdirResponse.message());
//     } else {
//         io:println("mkdir successful");
//     }
// }