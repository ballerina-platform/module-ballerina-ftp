// Copyright (c) 2025 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/lang.runtime as runtime;
import ballerina/test;

// Reusing the Explicit Config from your existing tests
ClientConfiguration ftpsAdvClientConfig = {
    protocol: FTPS,
    host: "127.0.0.1",
    port: 21214,
    auth: {
        credentials: {username: "wso2", password: "wso2123"},
        secureSocket: {
            key: {path: "tests/resources/keystore.jks", password: "changeit"},
            cert: {path: "tests/resources/keystore.jks", password: "changeit"},
            mode: EXPLICIT
        }
    }
};

isolated boolean ftpsAgeEventReceived = false;

@test:Config { groups: ["ftpsAdvanced"] }
function testFtpsFileAgeFilter() returns error? {
    string watchDir = "/ftps-listener"; 
    string ageFileName = "ftps-age-filter.txt";
    string ageFilePath = watchDir + "/" + ageFileName;

    Client ftpsClient = check new (ftpsAdvClientConfig);
    
    // Cleanup
    check removeIfExists(ftpsClient, ageFilePath);

    resetFtpsAgeState();

    Service ageService = service object {
        remote function onFileChange(WatchEvent & readonly event) {
            foreach FileInfo file in event.addedFiles {
                if file.pathDecoded.endsWith(ageFileName) {
                    lock { ftpsAgeEventReceived = true; }
                }
            }
        }
    };

    // Initialize Listener with FTPS and Age Filter
    Listener ageListener = check new ({
        protocol: FTPS,
        host: "127.0.0.1",
        port: 21214,
        auth: ftpsAdvClientConfig.auth,
        path: watchDir, 
        pollingInterval: 2,
        fileAgeFilter: { maxAge: 300 }, // Hits RemoteFileSystemConsumer.passesAgeFilter
        fileNamePattern: ".*\\.txt"    // Use standard wildcard pattern, filter in service
    });

    check ageListener.attach(ageService);
    check ageListener.'start();
    runtime:registerListener(ageListener);

    // Trigger
    check ftpsClient->putText(ageFilePath, "data");

    // Wait loop
    int waitCount = 0;
    while waitCount < 15 {
        boolean seen;
        lock { seen = ftpsAgeEventReceived; }
        if seen { break; }
        runtime:sleep(1);
        waitCount += 1;
    }

    check ageListener.gracefulStop();
    runtime:deregisterListener(ageListener);
    
    boolean result;
    lock { result = ftpsAgeEventReceived; }
    test:assertTrue(result, "FTPS Listener failed to respect Age Filter (File not detected)");
    
    // Cleanup
    check removeIfExists(ftpsClient, ageFilePath);
}

function resetFtpsAgeState() {
    lock { ftpsAgeEventReceived = false; }
}