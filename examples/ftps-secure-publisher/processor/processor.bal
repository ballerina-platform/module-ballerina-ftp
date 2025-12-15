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

// FTPS authentication configuration with secure socket
ftp:AuthConfiguration authConfig = {
    credentials: {username: "wso2", password: "wso2123"},
    secureSocket: {
        key: {
            path: "../resources/resources/keys/client-keystore.jks",
            password: "changeit"
        },
        cert: {
            path: "../resources/resources/keys/truststore.jks",
            password: "changeit"
        },
        mode: ftp:EXPLICIT,  // Can be changed to ftp:IMPLICIT for implicit mode (requires port 990)
        dataChannelProtection: ftp:PRIVATE
    }
};

// FTPS listener configuration - EXPLICIT mode (port 21214)
listener ftp:Listener secureRemoteServer = check new(
    protocol = ftp:FTPS,
    host = "localhost",
    auth = authConfig,
    port = 21214,  // EXPLICIT mode port
    pollingInterval = 2,
    fileNamePattern = "(.*).csv",
    laxDataBinding = true
);

service "Covid19UpdateDownloader" on secureRemoteServer {

    public function init() returns error? {
        log:printInfo("Initialized the FTPS processor job.");
    }

    remote function onFileCsv(stream<string[], io:Error?> csvStream, ftp:FileInfo fileInfo) returns error? {
        log:printInfo("Processing new FTPS file: " + fileInfo.name);
        int rowCount = 0;
        _ = check from string[] entry in csvStream
        where entry.length > 4 && entry[2] != "" && entry[3] != "" && entry[4] != ""
        do {
            rowCount += 1;
            if (rowCount % 1000 == 0) {
                log:printInfo("Processed " + rowCount.toString() + " rows from file: " + fileInfo.name);
            }
            json messageJson = {country: entry[2], date: entry[3], totalCases: entry[4]};
            string message = messageJson.toJsonString();
            // In a real scenario, this would publish to RabbitMQ or another message queue
            // For this example, we'll just log the message
            if (rowCount <= 5) {
                log:printInfo("Sample message: " + message);
            }
        };
        log:printInfo("Finished processing file: " + fileInfo.name + ". Total rows processed: " + rowCount.toString());
    }

    remote function onFileChange(ftp:WatchEvent event) returns error? {
        log:printInfo("File change detected. Added files: " + event.addedFiles.length().toString() + 
                     ", Deleted files: " + event.deletedFiles.length().toString());
    }
}
