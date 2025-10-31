// Copyright (c) 2025 WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 LLC. licenses this file to you under the Apache License,
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

import ballerina/lang.runtime as runtime;
import ballerina/lang.'string as strings;
import ballerina/test;

Client? ftpClientCache = ();

isolated boolean cronEventReceived = false;
isolated string[] cronAddedFilePaths = [];

isolated boolean ageEventReceived = false;

isolated boolean dependencyEventReceived = false;
isolated string dependencyMatchedFile = "";

@test:BeforeSuite
function initAdvancedSelectionTests() returns error? {
    ftpClientCache = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        port: 21212,
        auth: {
            credentials: {
                username: "wso2",
                password: "wso2123"
            }
        }
    });

    Client? ftpClient = getClient();
    check ensureDirectory((<Client>ftpClient), "/home/in/advanced");
    check ensureDirectory((<Client>ftpClient), "/home/in/advanced/cron");
    check ensureDirectory((<Client>ftpClient), "/home/in/advanced/age");
    check ensureDirectory((<Client>ftpClient), "/home/in/advanced/dependency");
}

@test:AfterSuite
function cleanupAdvancedSelectionTests() returns error? {
    Client? ftpClient = getClient();
    check removeIfExists((<Client>ftpClient), "/home/in/advanced/cron/cron-schedule.txt");
    check removeIfExists((<Client>ftpClient), "/home/in/advanced/age/age-filter.txt");
    check removeIfExists((<Client>ftpClient), "/home/in/advanced/dependency/invoice_pending.xml");
    check removeIfExists((<Client>ftpClient), "/home/in/advanced/dependency/invoice_pending.csv");
    check removeIfExists((<Client>ftpClient), "/home/in/advanced/dependency/invoice_pending.checksum");
}

@test:Config {
    enable: false
}
function testCronScheduledPolling() returns error? {
    Client? ftpClient = getClient();
    string cronFilePath = "/home/in/advanced/cron/cron-schedule.txt";
    lock {
	    cronEventReceived = false;
    }
    lock {
	    cronAddedFilePaths = [];
    }

    check removeIfExists((<Client>ftpClient), cronFilePath);
    check (<Client>ftpClient)->put(cronFilePath, "cron-data");

    Service cronService = service object {
        remote function onFileChange(WatchEvent & readonly event) {
            if event.addedFiles.length() == 0 {
                return;
            }
            lock {
                cronEventReceived = true;
            }
            lock {
                cronAddedFilePaths = [];
                foreach FileInfo file in event.addedFiles {
                    cronAddedFilePaths.push(file.pathDecoded);
                }
            }
        }
    };

    Listener cronListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        port: 21212,
        auth: {
            credentials: {
                username: "wso2",
                password: "wso2123"
            }
        },
        path: "/home/in/advanced/cron",
        pollingInterval: "*/1 * * * * *",
        fileNamePattern: ".*\\.txt"
    });
    check cronListener.attach(cronService);
    check cronListener.'start();
    runtime:registerListener(cronListener);

    int waitCount = 0;
    while waitCount < 15 {
        boolean eventSeen;
        lock {
            eventSeen = cronEventReceived;
        }
        if eventSeen {
            break;
        }
        runtime:sleep(1);
        waitCount += 1;
    }

    runtime:deregisterListener(cronListener);
    check cronListener.gracefulStop();
    check cronListener.detach(cronService);
    check removeIfExists(<Client>ftpClient, cronFilePath);

    boolean cronSeen;
    string[] addedPaths;
    lock {
        cronSeen = cronEventReceived;
    }
    lock {
        addedPaths = cronAddedFilePaths.clone();
    }

    test:assertTrue(cronSeen, msg = "Cron scheduled polling did not detect the target file");
    if cronSeen {
        test:assertTrue(arrayContains(addedPaths, "/home/in/advanced/cron/cron-schedule.txt"),
            msg = "Unexpected files were reported by cron polling");
    }
}

@test:Config {
    enable: false
}
function testFileAgeFilterRespectsMinAge() returns error? {
    Client? ftpClient = getClient();
    string ageFilePath = "/home/in/advanced/age/age-filter.txt";
    lock {
	    ageEventReceived = false;
    }

    check removeIfExists(<Client>ftpClient, ageFilePath);

    Service ageService = service object {
        remote function onFileChange(WatchEvent & readonly event) {
            if event.addedFiles.length() == 0 {
                return;
            }
            foreach FileInfo file in event.addedFiles {
                if file.pathDecoded == "/home/in/advanced/age/age-filter.txt" {
                    lock {
                        ageEventReceived = true;
                    }
                }
            }
        }
    };

    Listener ageListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        port: 21212,
        auth: {
            credentials: {
                username: "wso2",
                password: "wso2123"
            }
        },
        path: "/home/in/advanced/age",
        pollingInterval: 1,
        fileAgeFilter: {
            minAge: 3
        }
    });
    check ageListener.attach(ageService);
    check ageListener.'start();
    runtime:registerListener(ageListener);

    check (<Client>ftpClient)->put(ageFilePath, "age-filter");

    runtime:sleep(2);
    boolean ageSeenEarly;
    lock {
        ageSeenEarly = ageEventReceived;
    }
    test:assertFalse(ageSeenEarly, msg = "File was processed before the minimum age elapsed");

    int waitCount = 0;
    while waitCount < 10 {
        boolean ageSeen;
        lock {
            ageSeen = ageEventReceived;
        }
        if ageSeen {
            break;
        }
        runtime:sleep(1);
        waitCount += 1;
    }

    runtime:deregisterListener(ageListener);
    check ageListener.gracefulStop();
    check ageListener.detach(ageService);
    check removeIfExists(<Client>ftpClient, ageFilePath);

    boolean ageDelivered;
    lock {
        ageDelivered = ageEventReceived;
    }
    test:assertTrue(ageDelivered, msg = "File that satisfied the age filter was not delivered");
}

@test:Config {
    enable: false
}
function testFileDependencyFiltering() returns error? {
    Client? ftpClient = getClient();
    string xmlPath = "/home/in/advanced/dependency/invoice_pending.xml";
    string csvPath = "/home/in/advanced/dependency/invoice_pending.csv";
    string checksumPath = "/home/in/advanced/dependency/invoice_pending.checksum";
    lock {
	    dependencyEventReceived = false;
    }
    lock {
	    dependencyMatchedFile = "";
    }

    check removeIfExists(<Client>ftpClient, xmlPath);
    check removeIfExists(<Client>ftpClient, csvPath);
    check removeIfExists(<Client>ftpClient, checksumPath);

    Service dependencyService = service object {
        remote function onFileChange(WatchEvent & readonly event) {
            if event.addedFiles.length() == 0 {
                return;
            }
            foreach FileInfo file in event.addedFiles {
                if file.pathDecoded == "/home/in/advanced/dependency/invoice_pending.xml" {
                    lock {
                        dependencyEventReceived = true;
                    }
                    lock {
                        dependencyMatchedFile = file.name;
                    }
                }
            }
        }
    };

    Listener dependencyListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        port: 21212,
        auth: {
            credentials: {
                username: "wso2",
                password: "wso2123"
            }
        },
        path: "/home/in/advanced/dependency",
        pollingInterval: 1,
        fileNamePattern: ".*\\.xml",
        fileDependencyConditions: [
            {
                targetPattern: "(.*)\\.xml",
                requiredFiles: ["$1.csv", "$1.checksum"],
                matchingMode: ALL
            }
        ]
    });
    check dependencyListener.attach(dependencyService);
    check dependencyListener.'start();
    runtime:registerListener(dependencyListener);

    // Create the target file without the dependencies first.
    check (<Client>ftpClient)->put(xmlPath, "invoice");

    runtime:sleep(2);
    boolean dependencySeenEarly;
    lock {
        dependencySeenEarly = dependencyEventReceived;
    }
    test:assertFalse(dependencySeenEarly, msg = "File was processed without required dependencies");

    // Add the dependent files so the next poll should deliver the XML file.
    check (<Client>ftpClient)->put(csvPath, "invoice-csv");
    check (<Client>ftpClient)->put(checksumPath, "checksum");

    int waitCount = 0;
    while waitCount < 10 {
        boolean dependencySeen;
        lock {
            dependencySeen = dependencyEventReceived;
        }
        if dependencySeen {
            break;
        }
        runtime:sleep(1);
        waitCount += 1;
    }

    runtime:deregisterListener(dependencyListener);
    check dependencyListener.gracefulStop();
    check dependencyListener.detach(dependencyService);
    check removeIfExists(<Client>ftpClient, xmlPath);
    check removeIfExists(<Client>ftpClient, csvPath);
    check removeIfExists(<Client>ftpClient, checksumPath);

    boolean dependencyDelivered;
    string matchedFileName;
    lock {
        dependencyDelivered = dependencyEventReceived;
    }
    lock {
        matchedFileName = dependencyMatchedFile;
    }

    test:assertTrue(dependencyDelivered,
        msg = "File was not delivered after dependency requirements were met");
    if dependencyDelivered {
        test:assertEquals(matchedFileName, "invoice_pending.xml");
    }
}

function getClient() returns Client? {
    if ftpClientCache is Client {
        return ftpClientCache;
    }
    panic error("FTP client is not initialized");
}

function ensureDirectory(Client ftpClient, string path) returns error? {
    var mkdirResult = trap ftpClient->mkdir(path);
    if mkdirResult is error {
        // Ignore errors that indicate the directory already exists.
        string message = strings:toLowerAscii(mkdirResult.message());
        if !message.includes("exist") {
            return mkdirResult;
        }
    }
}

function removeIfExists(Client ftpClient, string path) returns error? {
    var deleteResult = trap ftpClient->delete(path);
    if deleteResult is error {
        string message = strings:toLowerAscii(deleteResult.message());
        if !(message.includes("no such file") || message.includes("not found")
                || message.includes("cannot find") || message.includes("does not exist")) {
            return deleteResult;
        }
    }
}

function arrayContains(string[] paths, string expected) returns boolean {
    foreach string path in paths {
        if path == expected {
            return true;
        }
    }
    return false;
}
