// Copyright (c) 2022 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/file;
import ballerina/ftp;
import ballerina/http;
import ballerina/io;
import ballerina/lang.runtime;
import ballerina/log;
import ballerina/time;

int errorCount = 0;
int sentCount = 0;
int receivedCount = 0;
int deletedCount = 0;
time:Utc startedTime = time:utcNow();
time:Utc endedTime = time:utcNow();
boolean finished = false;

ftp:AuthConfiguration authConfig = {
    credentials: {username: "ballerina", password: "password"}
};

ftp:ClientConfiguration sftpClientConfig = {
    protocol: ftp:SFTP,
    host: "sftp-server",
    port: 23,
    auth: authConfig
};

service /ftp on new http:Listener(9100) {

    resource function get publish() returns boolean {
        error? result = startListener();
        if result is error {
            return false;
        }
        errorCount = 0;
        sentCount = 0;
        receivedCount = 0;
        deletedCount = 0;
        startedTime = time:utcNow();
        endedTime = time:utcNow();
        finished = false;
        _ = start publishMessages();
        return true;
    }

    resource function get getResults() returns boolean|map<string> {
        if finished {
            return {
                errorCount: errorCount.toString(),
                time: time:utcDiffSeconds(endedTime, startedTime).toString(),
                sentCount: sentCount.toString(),
                receivedCount: receivedCount.toString(),
                deletedCount: deletedCount.toString()
            };
        }
        return false;
    }
}

function startListener() returns error? {
    ftp:Listener sftpListener = check new({
        protocol: ftp:SFTP,
        host: "sftp-server",
        auth: authConfig,
        port: 23,
        pollingInterval: 2,
        path: "/upload/",
        fileNamePattern: ".*"
    });
    check sftpListener.attach(ftpSubscriber);
    check sftpListener.start();
    runtime:registerListener(sftpListener);
}

ftp:Service ftpSubscriber = service object {

    private ftp:Client sftpClient;

    public function init() {
        self.sftpClient = checkpanic new(sftpClientConfig);
    }

    remote function onFileChange(ftp:WatchEvent event) {
        foreach ftp:FileInfo addedFile in event.addedFiles {
            int startIndex = (addedFile.path.lastIndexOf("/") ?: 0) + 1;
            int lastIndex = addedFile.path.length();
            string fileName = addedFile.path.substring(startIndex, lastIndex);
            stream<byte[] & readonly, io:Error?>|error fileStream = self.sftpClient->get("/upload/" + fileName);
            if fileStream is stream<byte[] & readonly, io:Error?> {
                error? writeResult = io:fileWriteBlocksFromStream(fileName, fileStream, option = io:APPEND);
                error? fileCloseResult = fileStream.close();
                if writeResult is error || fileCloseResult is error {
                    log:printError("Error while writing/closing a local file: " + fileName);
                    continue;
                }
                log:printDebug("Read the file: " + fileName);
                self.countFile(fileName);
                error? deleteResult = self.sftpClient->delete("/upload/" + fileName);
                if deleteResult is error {
                    logFtpError(deleteResult.message(), "delete");
                } else {
                    lock {
                        deletedCount += 1;
                    }
                }
            } else {
                logFtpError(fileStream.message(), "get");
            }
        }
    }

    private function countFile(string newFileName) {
        lock {
            receivedCount += 1;
        }
        error? fileRemoveResult = file:remove(newFileName);
        if fileRemoveResult is error {
            log:printError("Error while deleting a local file.");
        }
        if newFileName == "file_last" {
            endedTime = time:utcNow();
            finished = true;
        }
    }

};


public function publishMessages() {
    startedTime = time:utcNow();
    // Publishing files for 1 hour
    int endingTimeInSecs = startedTime[0] + 3600;
    ftp:Client|error fileSender = new(sftpClientConfig);
    if fileSender is error {
        log:printError("Error while creating a SFTP client.");
    } else {
        while time:utcNow()[0] <= endingTimeInSecs {
            stream<io:Block, io:Error?>|error fileByteStream = io:fileReadBlocksAsStream("resources/20mb_file", 1024);
            if fileByteStream is stream<io:Block, io:Error?> {
                string fileName = constructFileName();
                ftp:Error? putResponse = fileSender->put("/upload/" + fileName, fileByteStream);
                if putResponse is error {
                    log:printError("Error while publishing. " + putResponse.message());
                    logFtpError(putResponse.message(), "put");
                } else {
                    log:printInfo("Published file: " + fileName);
                    sentCount += 1;
                }
            }
            runtime:sleep(1);
        }
        stream<io:Block, io:Error?>|error fileByteStream = io:fileReadBlocksAsStream("resources/20mb_file", 1024);
        if fileByteStream is stream<io:Block, io:Error?> {
            ftp:Error? putResponse = fileSender->put("/upload/file_last", fileByteStream);
            if putResponse is error {
                logFtpError(putResponse.message(), "put");
            } else {
                sentCount += 1;
            }
        }
    }

}

function logFtpError(string errorMessage, string operationName) {
    log:printError("Error while " + operationName + " operation on the FTP server.");
    lock {
        errorCount += 1;
    }
}

function constructFileName() returns string {
    time:Utc currentTime = time:utcNow(3);
    string timeString = currentTime[0].toString();
    return "file_" + timeString;
}
