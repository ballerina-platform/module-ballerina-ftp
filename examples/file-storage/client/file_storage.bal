// Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/ftp;
import ballerina/io;

public function main() returns error? {
    // Creates the client with the connection parameters, host, port,
    // password and username. An error is returned in a failure.
    ftp:ClientConfiguration config = {
        protocol: ftp:FTP,
        host: "localhost",
        port: 20210,
        auth: {credentials: {username: "wso2", password: "wso2123"}}
    };
    ftp:Client clientEp = check new(config);

    // Add a new file to the given file location. In error cases,
    // an error is returned. The local file is provided as a stream of
    // `io:Block` in which 1024 is the block size.
    stream<io:Block, io:Error?> bStream
        = check io:fileReadBlocksAsStream("../resources/dog.png", 1024);
    check clientEp->put("/home/in/dog.png", bStream);

    // Reads a file from a FTP server for a given file path. In error cases,
    // an error is returned.
    stream<byte[] & readonly, io:Error?> fileStream
        = check clientEp->get("/home/in/dog.png");

    // Writes the received image to the local instance
    check io:fileWriteBlocksFromStream("localDog.png", fileStream);

    // Closes the file stream to finish the `get` operation.
    check fileStream.close();

}
