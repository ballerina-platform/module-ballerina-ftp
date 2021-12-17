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
import ballerina/task;
import ballerina/file;
import ballerinax/rabbitmq;

public string[] newFiles = [];

listener ftp:Listener secureRemoteServer = check new({
    protocol: ftp:SFTP,
    host: "localhost",
    auth: {
        credentials: {
            username: "wso2",
            password: "wso2123"
        },
        privateKey: {
            path: "../resources/keys/sftp.private.key",
            password: "changeit"
        }
    },
    port: 21213,
    path: "",
    pollingInterval: 2,
    fileNamePattern: "(.*).csv"
});

service "ftpServerConnector" on secureRemoteServer {
    function onFileChange(ftp:WatchEvent event) {
        foreach ftp:FileInfo addedFile in event.addedFiles {
            int startIndex = (addedFile.path.lastIndexOf("/") ?: 0) + 1;
            int lastIndex = addedFile.path.length();
            string fileName = addedFile.path.substring(startIndex, lastIndex);
            log:printInfo("Added file path: " + fileName);
            stream<byte[] & readonly, io:Error?>|error fileStream = sftpClient->get(fileName);
            if fileStream is stream<byte[] & readonly, io:Error?> {
                checkpanic io:fileWriteBlocksFromStream(fileName, fileStream, option = io:APPEND);
                checkpanic fileStream.close();
                newFiles[newFiles.length()] = fileName;
            } else {
                io:println(fileStream);
            }
            
        }
    }
}

ftp:ClientConfiguration sftpClientConfig = {
    protocol: ftp:SFTP,
    host: "localhost",
    port: 21213,
    auth: {
        credentials: {username: "wso2", password: "wso2123"},

        privateKey: {
            path: "../resources/keys/sftp.private.key",
            password: "changeit"
        }
    }
};

ftp:Client sftpClient = check new(sftpClientConfig);

public string remainingRow = "";

class PublishJob {

    *task:Job;
    private rabbitmq:Client rabbitmqClient;

    public function execute() {
        while newFiles.length() > 0 {
            string newFileName = newFiles[0];
            log:printInfo("Going to process new file: " + newFileName);
            stream<string[], io:Error?> csvStream = checkpanic io:fileReadCsvAsStream(newFileName);
            checkpanic csvStream.forEach(
                function(string[] values) {
                    string country = values[2];
                    string date = values[3];
                    string totalCases = values[4];
                    string message = country + "," + date + "," + totalCases;
                    log:printInfo("Going to publish message: " + message);
                    error? result = self.rabbitmqClient->publishMessage({content: message.toBytes(),
                        routingKey: "InfectionQueue"});
                    if result is error {
                        log:printError("Error while trying to publish to the queue.");
                    }
                }
            );
            checkpanic csvStream.close();
            _ = newFiles.remove(0);
            checkpanic file:remove(newFileName);
        }
    }

    isolated function init() returns error? {
        self.rabbitmqClient = check new(rabbitmq:DEFAULT_HOST, rabbitmq:DEFAULT_PORT);
        check self.rabbitmqClient->queueDeclare("InfectionQueue", {durable: true, autoDelete: false});
        log:printInfo("Initialized the process job.");
    }

}

public function main() returns error? {
    task:JobId jobId = check task:scheduleJobRecurByFrequency(check new PublishJob(), 10);
    io:println("Started PublishJob: ", jobId.id);
}
