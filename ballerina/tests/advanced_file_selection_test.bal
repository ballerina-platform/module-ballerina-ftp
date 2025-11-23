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

Client ftpClient = check new (config);

isolated boolean cronEventReceived = false;
isolated string[] cronAddedFilePaths = [];

isolated boolean ageEventReceived = false;

isolated boolean dependencyEventReceived = false;
isolated string dependencyMatchedFile = "";

@test:Config {
    groups: ["adFiltering"]
}
function testCronScheduledPolling() returns error? {
    string cronFilePath = "/home/in/advanced/cron/cron-schedule.txt";
    lock {
	    cronEventReceived = false;
    }
    lock {
	    cronAddedFilePaths = [];
    }

    check removeIfExists(ftpClient, cronFilePath);
    check ftpClient->putText(cronFilePath, "cron-data");

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
        pollingInterval: "*/5 * * * * *",
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

    check cronListener.gracefulStop();
    runtime:deregisterListener(cronListener);
    check removeIfExists(ftpClient, cronFilePath);

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
    groups: ["adFiltering"]
}
function testFileAgeFilterRespectsMaxAge() returns error? {
    string ageFilePath = "/home/in/advanced/age/age-filter.txt";
    lock {
	    ageEventReceived = false;
    }

    check removeIfExists(ftpClient, ageFilePath);

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
        pollingInterval: 5,
        fileAgeFilter: {
            maxAge: 60
        },
        fileNamePattern: ".*\\.txt"
    });
    check ageListener.attach(ageService);
    check ageListener.'start();
    runtime:registerListener(ageListener);

    // Test 1: Recent file within maxAge should be picked up
    check ftpClient->putText(ageFilePath, "age-filter");

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

    boolean ageDelivered;
    lock {
        ageDelivered = ageEventReceived;
    }
    test:assertTrue(ageDelivered, msg = "Recent file within max age was not delivered");

    runtime:sleep(130);

    // Test 2: Wait for file to exceed maxAge threshold and verify it's not picked up again
    lock {
        ageEventReceived = false;
    }

    // File is now older than maxAge (120 seconds). Verify it's not picked up again
    waitCount = 0;
    boolean ageSeenAfterMaxAge;
    while waitCount < 10 {
        lock {
            ageSeenAfterMaxAge = ageEventReceived;
        }
        if ageSeenAfterMaxAge {
            break;
        }
        runtime:sleep(1);
        waitCount += 1;
    }

    lock {
        ageSeenAfterMaxAge = ageEventReceived;
    }
    test:assertFalse(ageSeenAfterMaxAge, msg = "File older than max age was incorrectly picked up");

    runtime:deregisterListener(ageListener);
    check ageListener.gracefulStop();

    check removeIfExists(ftpClient, ageFilePath);
}

@test:Config {
    groups: ["adFiltering"]
}
function testFileDependencyFiltering() returns error? {
    string xmlPath = "/home/in/advanced/dependency/invoice_pending.xml";
    string csvPath = "/home/in/advanced/dependency/invoice_pending.csv";
    string checksumPath = "/home/in/advanced/dependency/invoice_pending.checksum";
    lock {
	    dependencyEventReceived = false;
    }
    lock {
	    dependencyMatchedFile = "";
    }

    check removeIfExists(ftpClient, xmlPath);
    check removeIfExists(ftpClient, csvPath);
    check removeIfExists(ftpClient, checksumPath);

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
    check ftpClient->put(xmlPath, "invoice");

    runtime:sleep(2);
    boolean dependencySeenEarly;
    lock {
        dependencySeenEarly = dependencyEventReceived;
    }
    test:assertFalse(dependencySeenEarly, msg = "File was processed without required dependencies");

    // Add the dependent files so the next poll should deliver the XML file.
    check ftpClient->put(csvPath, "invoice-csv");
    check ftpClient->put(checksumPath, "checksum");

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

    check removeIfExists(ftpClient, xmlPath);
    check removeIfExists(ftpClient, csvPath);
    check removeIfExists(ftpClient, checksumPath);

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

@test:Config {
    groups: ["adFiltering"]
}
function testFileAgeFilterRespectsMinAge() returns error? {
    string ageFilePath = "/home/in/advanced/age/age-min-filter.txt";
    lock {
	    ageEventReceived = false;
    }

    check removeIfExists(ftpClient, ageFilePath);

    Service ageService = service object {
        remote function onFileChange(WatchEvent & readonly event) {
            if event.addedFiles.length() == 0 {
                return;
            }
            foreach FileInfo file in event.addedFiles {
                if file.pathDecoded == "/home/in/advanced/age/age-min-filter.txt" {
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
            minAge: 90
        },
        fileNamePattern: ".*\\.txt"
    });
    check ageListener.attach(ageService);
    check ageListener.'start();
    runtime:registerListener(ageListener);

    check ftpClient->putText(ageFilePath, "age-min-filter");

    runtime:sleep(30);
    boolean ageSeenEarly;
    lock {
        ageSeenEarly = ageEventReceived;
    }
    test:assertFalse(ageSeenEarly, msg = "File was processed before the minimum age of 90 seconds elapsed");
    int waitCount = 0;
    while waitCount < 70 {
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

    check ageListener.gracefulStop();
    runtime:deregisterListener(ageListener);
    check removeIfExists(ftpClient, ageFilePath);

    boolean ageDelivered;
    lock {
        ageDelivered = ageEventReceived;
    }
    test:assertTrue(ageDelivered, msg = "File that satisfied the minimum age filter was not delivered");
}

@test:Config {
    groups: ["adFiltering"]
}
function testDependencyFilteringWithCreationTime() returns error? {
    string xmlPath = "/home/in/advanced/dependency/doc_creation.xml";
    string csvPath = "/home/in/advanced/dependency/doc_creation.csv";
    lock {
	    dependencyEventReceived = false;
    }
    lock {
	    dependencyMatchedFile = "";
    }

    check removeIfExists(ftpClient, xmlPath);
    check removeIfExists(ftpClient, csvPath);

    Service dependencyService = service object {
        remote function onFileChange(WatchEvent & readonly event) {
            if event.addedFiles.length() == 0 {
                return;
            }
            foreach FileInfo file in event.addedFiles {
                if file.pathDecoded == "/home/in/advanced/dependency/doc_creation.xml" {
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
        fileNamePattern: "doc_creation\\.xml",
        fileDependencyConditions: [
            {
                targetPattern: "(doc_creation)\\.xml",
                requiredFiles: ["$1.csv"],
                matchingMode: ALL
            }
        ]
    });
    check dependencyListener.attach(dependencyService);
    check dependencyListener.'start();
    runtime:registerListener(dependencyListener);

    check ftpClient->putText(csvPath, "csv-data");
    runtime:sleep(2);
    check ftpClient->putText(xmlPath, "xml-data");

    int waitCount = 0;
    while waitCount < 10 {
        boolean ageSeen;
        lock {
            ageSeen = dependencyEventReceived;
        }
        if ageSeen {
            break;
        }
        runtime:sleep(1);
        waitCount += 1;
    }

    check dependencyListener.gracefulStop();
    runtime:deregisterListener(dependencyListener);
    check removeIfExists(ftpClient, xmlPath);
    check removeIfExists(ftpClient, csvPath);

    boolean dependencyDelivered;
    lock {
        dependencyDelivered = dependencyEventReceived;
    }
    test:assertTrue(dependencyDelivered, msg = "Dependency filtering with creation time did not work");
}

@test:Config {
    groups: ["adFiltering"]
}
function testInvalidCronExpression() returns error? {
    Service invalidCronService = service object {
        remote function onFileChange(WatchEvent & readonly event) {
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
        path: "/home/in",
        pollingInterval: "INVALID CRON"
    });

    check cronListener.attach(invalidCronService);
    error? startErr = cronListener.'start();

    test:assertTrue(startErr is error, msg = "Invalid cron expression should throw an error");
}

@test:Config {
    groups: ["adFiltering"]
}
function testComplexCronExpression() returns error? {
    string complexCronFilePath = "/home/in/advanced/cron/complex-cron.txt";

    check removeIfExists(ftpClient, complexCronFilePath);
    check ftpClient->putText(complexCronFilePath, "complex-cron-data");

    Service complexCronService = service object {
        remote function onFileChange(WatchEvent & readonly event) {
        }
    };

    Listener complexCronListener = check new ({
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
        pollingInterval: "0/30 * * * * *",
        fileNamePattern: ".*\\.txt"
    });
    check complexCronListener.attach(complexCronService);
    error? result = complexCronListener.'start();

    check complexCronListener.gracefulStop();

    test:assertTrue(result is (), "Cron expression should pass");
}

@test:Config {
    groups: ["adFiltering"]
}
function testDependencyMatchingModeAny() returns error? {
    string xmlPath = "/home/in/advanced/dependency/report_any.xml";
    string csvPath = "/home/in/advanced/dependency/report_any.csv";
    string txtPath = "/home/in/advanced/dependency/report_any.txt";
    lock {
	    dependencyEventReceived = false;
    }
    lock {
	    dependencyMatchedFile = "";
    }

    check removeIfExists(ftpClient, xmlPath);
    check removeIfExists(ftpClient, csvPath);
    check removeIfExists(ftpClient, txtPath);

    Service dependencyService = service object {
        remote function onFileChange(WatchEvent & readonly event) {
            if event.addedFiles.length() == 0 {
                return;
            }
            foreach FileInfo file in event.addedFiles {
                if file.pathDecoded == "/home/in/advanced/dependency/report_any.xml" {
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
        fileNamePattern: "report_any\\.xml",
        fileDependencyConditions: [
            {
                targetPattern: "(report_any)\\.xml",
                requiredFiles: ["$1.csv", "$1.txt"],
                matchingMode: ANY
            }
        ]
    });
    check dependencyListener.attach(dependencyService);
    check dependencyListener.'start();
    runtime:registerListener(dependencyListener);

    // Create only CSV file (not all dependencies) - should still trigger with ANY mode
    check ftpClient->putText(csvPath, "csv-data");
    runtime:sleep(2);
    check ftpClient->putText(xmlPath, "xml-data");

    int waitCount = 0;
    while waitCount < 10 {
        boolean ageSeen;
        lock {
            ageSeen = dependencyEventReceived;
        }
        if ageSeen {
            break;
        }
        runtime:sleep(1);
        waitCount += 1;
    }

    check dependencyListener.gracefulStop();
    runtime:deregisterListener(dependencyListener);
    check removeIfExists(ftpClient, xmlPath);
    check removeIfExists(ftpClient, csvPath);
    check removeIfExists(ftpClient, txtPath);

    boolean dependencyDelivered;
    lock {
        dependencyDelivered = dependencyEventReceived;
    }
    test:assertTrue(dependencyDelivered, msg = "Dependency matching with ANY mode did not work");
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
