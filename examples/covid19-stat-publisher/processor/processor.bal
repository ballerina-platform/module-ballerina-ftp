// Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import ballerina/log;
import ballerina/file;
import ballerinax/rabbitmq;

ftp:AuthConfiguration authConfig = {
    credentials: {username: "wso2", password: "wso2123"},
    privateKey: {
        path: "../resources/keys/sftp.private.key",
        password: "changeit"
    }
};

ftp:ClientConfiguration sftpClientConfig = {
    protocol: ftp:SFTP,
    host: "localhost",
    port: 21213,
    auth: authConfig
};

listener ftp:Listener secureRemoteServer = check new({
    protocol: ftp:SFTP,
    host: "localhost",
    auth: authConfig,
    port: 21213,
    pollingInterval: 2,
    fileNamePattern: "(.*).csv"
});

service "Covid19UpdateDownloader" on secureRemoteServer {

    private rabbitmq:Client rabbitmqClient;
    private ftp:Client sftpClient = check new(sftpClientConfig);

    public function init() returns error? {
        self.rabbitmqClient = check new(rabbitmq:DEFAULT_HOST, rabbitmq:DEFAULT_PORT);
        check self.rabbitmqClient->queueDeclare("InfectionQueue", {durable: true, autoDelete: false});
        log:printInfo("Initialized the process job.");
    }

    remote function onFileChange(ftp:WatchEvent event) {
        foreach ftp:FileInfo addedFile in event.addedFiles {
            string fileName = addedFile.name.clone();
            log:printInfo("Added file: " + fileName);
            stream<byte[] & readonly, io:Error?>|error fileStream = self.sftpClient->get(fileName);
            if fileStream is stream<byte[] & readonly, io:Error?> {
                error? writeResult = io:fileWriteBlocksFromStream(fileName, fileStream, option = io:APPEND);
                error? fileCloseResult = fileStream.close();
                if writeResult is error || fileCloseResult is error {
                    continue;
                }
                _ = start self.publishToRabbitmq(fileName);
            } else {
                log:printInfo(fileStream.message());
            }
        }
    }

    private function publishToRabbitmq(string newFileName) returns error? {
        log:printInfo("Going to process new file: " + newFileName);
        stream<string[], io:Error?> csvStream = check io:fileReadCsvAsStream(newFileName);
        _ = check from var entry in csvStream
        where entry[2] != "" && entry[3] != "" && entry[4] != ""
        do {
            log:printInfo("Processing new file: " + newFileName);
            json messageJson = {country: entry[2], date: entry[3], totalCases: entry[4]};
            string message = messageJson.toJsonString();
            log:printInfo("Going to publish message: " + message);
            error? result = self.rabbitmqClient->publishMessage({content: message.toBytes(),
                routingKey: "InfectionQueue"});
            if result is error {
                log:printError("Error while trying to publish to the queue.");
            }
        };
        check csvStream.close();
        check file:remove(newFileName);
    }

}
