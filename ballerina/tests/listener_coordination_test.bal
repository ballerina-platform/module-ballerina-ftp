// Copyright (c) 2026 WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/task;
import ballerina/test;

// Test that ListenerConfiguration works without coordination (backward compatibility)
@test:Config {}
public function testListenerConfigurationWithoutCoordination() {
    ListenerConfiguration config = {
        protocol: FTP,
        host: "ftp.example.com",
        port: 21,
        auth: {
            credentials: {
                username: "user",
                password: "pass"
            }
        },
        path: "/data",
        pollingInterval: 30
    };

    test:assertTrue(config.coordination is ());
}

// Test listener initialization without coordination still works
@test:Config {}
public function testListenerInitWithoutCoordination() returns error? {
    Listener ftpListener = check new ({
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
        pollingInterval: 2
    });

    // If we reach here, initialization succeeded without coordination
    check ftpListener.gracefulStop();
}

// Test listener initialization with coordination config
// Note: This test expects an error since no coordination database is available
@test:Config {}
public function testListenerInitWithCoordinationFailsWithoutDb() {
    Listener|Error result = new ({
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
        pollingInterval: 2,
        coordination: {
            databaseConfig: <task:MysqlConfig>{
                host: "nonexistent-db.local",
                user: "user",
                password: "pass",
                database: "coordination"
            },
            nodeId: "test-node",
            clusterName: "test-cluster"
        }
    });

    // We expect this to fail because the coordination database is not available
    test:assertTrue(result is Error,
        msg = "Expected error when coordination database is unavailable");
}
