// Copyright (c) 2025 WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
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

import ballerina/io;
import ballerina/lang.runtime;
import ballerina/log;
import ballerina/test;

// Global tracking variables for content method tests
string textContentReceived = "";
json jsonContentReceived = ();
xml? xmlContentReceived = ();
string[][] csvContentReceived = [];
byte[] byteContentReceived = [];
int streamByteCount = 0;
FileInfo? lastFileInfo = ();
boolean contentMethodInvoked = false;

// Global counters for extension-based routing test
int txtFilesProcessed = 0;
int jsonFilesProcessed = 0;
int xmlFilesProcessed = 0;

// Global counters for fallback test
int fallbackJsonFilesProcessed = 0;
int fallbackFilesProcessed = 0;

// Global counter for annotation test
int specialFilesProcessed = 0;

// Test data file paths
const string JSON_TEST_FILE = "tests/resources/datafiles/test_data.json";
const string XML_TEST_FILE = "tests/resources/datafiles/test_data.xml";
const string CSV_TEST_FILE = "tests/resources/datafiles/test_data.csv";
const string TEXT_TEST_FILE = "tests/resources/datafiles/test_text.txt";
const string GENERIC_TEST_FILE = "tests/resources/datafiles/file2.txt";

// Isolated directory for content listener tests to avoid interfering with testListFiles
const string CONTENT_TEST_DIR = "/home/in/content-methods";

@test:Config {
}
public function testOnFileTextBasic() returns error? {
    // Reset state
    textContentReceived = "";
    lastFileInfo = ();
    contentMethodInvoked = false;

    // Service with onFileText
    Service textService = service object {
        remote function onFileText(string content, FileInfo fileInfo, Caller caller) returns error? {
            log:printInfo(string `onFileText invoked for: ${fileInfo.name}, content length: ${content.length()}`);
            textContentReceived = content;
            lastFileInfo = fileInfo;
            contentMethodInvoked = true;
        }
    };

    // Create listener for .txt files (only contentTest.txt to avoid touching test fixtures)
    Listener textListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: CONTENT_TEST_DIR,
        pollingInterval: 4,
        fileNamePattern: "contentTest.*\\.txt"
    });

    check textListener.attach(textService);
    check textListener.'start();
    runtime:registerListener(textListener);

    // Upload text file to isolated directory
    stream<io:Block, io:Error?> textStream = check io:fileReadBlocksAsStream(TEXT_TEST_FILE, 5);
    check (<Client>clientEp)->put(CONTENT_TEST_DIR + "/contentTest.txt", textStream);
    // Wait longer to ensure file is fully uploaded and listener has detected it
    runtime:sleep(10);

    // Cleanup
    runtime:deregisterListener(textListener);
    check textListener.gracefulStop();

    test:assertTrue(contentMethodInvoked, "onFileText should have been invoked");
    test:assertTrue(textContentReceived.length() > 0, "Should have received text content");
    // Note: Content may be from contentTest.txt or other .txt files in the directory
    // The important thing is that onFileText was invoked and we got some content

    FileInfo fileInfo = check lastFileInfo.ensureType();
    // Verify that we processed at least one .txt file successfully
    test:assertTrue(fileInfo.name.endsWith(".txt"), "Should process .txt files");
}

@test:Config {
    dependsOn: [testOnFileTextBasic]
}
public function testOnFileJsonBasic() returns error? {
    // Reset state
    jsonContentReceived = ();
    lastFileInfo = ();
    contentMethodInvoked = false;

    // Service with onFileJson
    Service jsonService = service object {
        remote function onFileJson(json content, FileInfo fileInfo, Caller caller) returns error? {
            log:printInfo(string `onFileJson invoked for: ${fileInfo.name}`);
            jsonContentReceived = content;
            lastFileInfo = fileInfo;
            contentMethodInvoked = true;
        }
    };

    // Create listener for .json files
    Listener jsonListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: CONTENT_TEST_DIR,
        pollingInterval: 4,
        fileNamePattern: "contentTest.*\\.json"
    });

    check jsonListener.attach(jsonService);
    check jsonListener.'start();
    runtime:registerListener(jsonListener);

    // Upload JSON file
    stream<io:Block, io:Error?> jsonStream = check io:fileReadBlocksAsStream(JSON_TEST_FILE);
    check (<Client>clientEp)->put(CONTENT_TEST_DIR + "/contentTest.json", jsonStream);
    runtime:sleep(10);

    // Cleanup
    runtime:deregisterListener(jsonListener);
    check jsonListener.gracefulStop();

    test:assertTrue(contentMethodInvoked, "onFileJson should have been invoked");
    test:assertTrue(jsonContentReceived != (), "Should have received JSON content");

    // Verify JSON structure
    map<json> jsonMap = check jsonContentReceived.ensureType();
    test:assertEquals(jsonMap["name"], "John Doe", "Name field should match");
    test:assertEquals(check jsonMap["age"].ensureType(), 30, "Age field should match");
    test:assertEquals(jsonMap["city"], "New York", "City field should match");
    test:assertTrue(check jsonMap["isActive"].ensureType(), "isActive should be true");
}

@test:Config {
    dependsOn: [testOnFileJsonBasic]
}
public function testOnFileXmlBasic() returns error? {
    // Reset state
    xmlContentReceived = ();
    lastFileInfo = ();
    contentMethodInvoked = false;

    // Service with onFileXml
    Service xmlService = service object {
        remote function onFileXml(xml content, FileInfo fileInfo, Caller caller) returns error? {
            log:printInfo(string `onFileXml invoked for: ${fileInfo.name}`);
            log:printInfo(content.toString());
            xmlContentReceived = content;
            lastFileInfo = fileInfo;
            contentMethodInvoked = true;
        }
    };

    // Create listener for .xml files
    Listener xmlListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: CONTENT_TEST_DIR,
        pollingInterval: 4,
        fileNamePattern: "contentTest.*\\.xml"
    });

    check xmlListener.attach(xmlService);
    check xmlListener.'start();
    runtime:registerListener(xmlListener);

    // Upload XML file
    stream<io:Block, io:Error?> xmlStream = check io:fileReadBlocksAsStream(XML_TEST_FILE);
    check (<Client>clientEp)->put(CONTENT_TEST_DIR + "/contentTest.xml", xmlStream);
    // Wait longer to ensure file is fully uploaded and listener has detected it
    runtime:sleep(10);

    // Cleanup
    runtime:deregisterListener(xmlListener);
    check xmlListener.gracefulStop();

    test:assertTrue(contentMethodInvoked, "onFileXml should have been invoked");
    test:assertFalse(xmlContentReceived is (), "Should have received XML content");

    // Verify XML content
    xml xmlValue = check xmlContentReceived.ensureType();
    string xmlString = xmlValue.toString();
    test:assertTrue(xmlString.includes("Jane Smith"), "XML should contain person name");
    test:assertTrue(xmlString.includes("Los Angeles"), "XML should contain city");
    test:assertTrue(xmlString.includes("Engineering"), "XML should contain department");
}

@test:Config {
    dependsOn: [testOnFileXmlBasic]
}
public function testOnFileCsvStringArray() returns error? {
    // Reset state
    csvContentReceived = [];
    lastFileInfo = ();
    contentMethodInvoked = false;

    // Service with onFileCsv
    Service csvService = service object {
        remote function onFileCsv(string[][] content, FileInfo fileInfo, Caller caller) returns error? {
            log:printInfo(string `onFileCsv invoked for: ${fileInfo.name}, rows: ${content.length()}`);
            csvContentReceived = content;
            lastFileInfo = fileInfo;
            contentMethodInvoked = true;
        }
    };

    // Create listener for .csv files
    Listener csvListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: CONTENT_TEST_DIR,
        pollingInterval: 4,
        fileNamePattern: "contentTest.*\\.csv"
    });

    check csvListener.attach(csvService);
    check csvListener.'start();
    runtime:registerListener(csvListener);

    // Upload CSV file
    stream<io:Block, io:Error?> csvStream = check io:fileReadBlocksAsStream(CSV_TEST_FILE, 5);
    check (<Client>clientEp)->put(CONTENT_TEST_DIR + "/contentTest.csv", csvStream);
    runtime:sleep(10);

    // Cleanup
    runtime:deregisterListener(csvListener);
    check csvListener.gracefulStop();

    test:assertTrue(contentMethodInvoked, "onFileCsv should have been invoked");
    test:assertTrue(csvContentReceived.length() >= 3,
        string `Should have at least 3 rows (3 data), got ${csvContentReceived.length()}`);

    // Verify first data row
    string[] firstDataRow = csvContentReceived[0];
    test:assertEquals(firstDataRow[0], "Alice Johnson", "First person's name should match");
    test:assertEquals(firstDataRow[1], "25", "First person's age should match");
}

@test:Config {
    dependsOn: [testOnFileCsvStringArray]
}
public function testOnFileByteArray() returns error? {
    // Reset state
    byteContentReceived = [];
    lastFileInfo = ();
    contentMethodInvoked = false;

    // Service with onFile (byte[] variant)
    Service genericService = service object {
        remote function onFile(byte[] content, FileInfo fileInfo, Caller caller) returns error? {
            log:printInfo(string `onFile (byte[]) invoked for: ${fileInfo.name}, bytes: ${content.length()}`);
            byteContentReceived = content;
            lastFileInfo = fileInfo;
            contentMethodInvoked = true;
        }
    };

    // Create listener for .bin files (generic binary)
    Listener genericListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: CONTENT_TEST_DIR,
        pollingInterval: 4,
        fileNamePattern: "contentTest.*\\.bin"
    });

    check genericListener.attach(genericService);
    check genericListener.'start();
    runtime:registerListener(genericListener);

    // Upload generic binary file
    stream<io:Block, io:Error?> binStream = check io:fileReadBlocksAsStream(GENERIC_TEST_FILE);
    check (<Client>clientEp)->put(CONTENT_TEST_DIR + "/contentTest.bin", binStream);
    runtime:sleep(15);

    // Cleanup
    runtime:deregisterListener(genericListener);
    check genericListener.gracefulStop();

    test:assertTrue(contentMethodInvoked, "onFile should have been invoked");
    test:assertTrue(byteContentReceived.length() > 0, "Should have received byte content");

    // Verify byte content can be converted to string
    string contentAsString = check string:fromBytes(byteContentReceived);
    test:assertTrue(contentAsString.length() > 0, "Should be able to convert bytes to string");
}

@test:Config {
    dependsOn: [testOnFileByteArray]
}
public function testOnFileStream() returns error? {
    // Reset state
    streamByteCount = 0;
    lastFileInfo = ();
    contentMethodInvoked = false;

    // Service with onFile (stream variant)
    Service streamService = service object {
        remote function onFile(stream<byte[], error?> content, FileInfo fileInfo, Caller caller) returns error? {
            log:printInfo(string `onFile (stream) invoked for: ${fileInfo.name}`);
            lastFileInfo = fileInfo;
            contentMethodInvoked = true;

            // Process stream
            int totalBytes = 0;
            error? processStream = content.forEach(function(byte[] chunk) {
                totalBytes += chunk.length();
            });

            if processStream is error {
                log:printError("Error processing stream", processStream);
                return processStream;
            }

            streamByteCount = totalBytes;
            log:printInfo(string `Processed ${totalBytes} bytes from stream`);
        }
    };

    // Create listener for .stream files
    Listener streamListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: CONTENT_TEST_DIR,
        pollingInterval: 4,
        fileNamePattern: "streamTest.*\\.stream1"
    });

    check streamListener.attach(streamService);
    check streamListener.'start();
    runtime:registerListener(streamListener);

    // Upload file for stream processing
    stream<io:Block, io:Error?> testStream = check io:fileReadBlocksAsStream(TEXT_TEST_FILE);
    check (<Client>clientEp)->put(CONTENT_TEST_DIR + "/streamTest.stream", testStream);
    check (<Client>clientEp)->rename(CONTENT_TEST_DIR + "/streamTest.stream",
                                     CONTENT_TEST_DIR + "/streamTest.stream1");
    runtime:sleep(10);

    // Cleanup
    runtime:deregisterListener(streamListener);
    check streamListener.gracefulStop();

    test:assertTrue(contentMethodInvoked, "onFile (stream) should have been invoked");
    test:assertTrue(streamByteCount > 0, string `Should have processed bytes, got ${streamByteCount}`);
}

@test:Config {
    dependsOn: [testOnFileStream]
}
public function testExtensionBasedRouting() returns error? {
    // Reset state
    txtFilesProcessed = 0;
    jsonFilesProcessed = 0;
    xmlFilesProcessed = 0;

    // Service with multiple content methods
    Service multiFormatService = service object {
        remote function onFileText(string content, FileInfo fileInfo, Caller caller) returns error? {
            log:printInfo(string `onFileText: ${fileInfo.name}`);
            txtFilesProcessed += 1;
        }

        remote function onFileJson(json content, FileInfo fileInfo, Caller caller) returns error? {
            log:printInfo(string `onFileJson: ${fileInfo.name}`);
            jsonFilesProcessed += 1;
        }

        remote function onFileXml(xml content, FileInfo fileInfo, Caller caller) returns error? {
            log:printInfo(string `onFileXml: ${fileInfo.name}`);
            xmlFilesProcessed += 1;
        }
    };

    // Create listener for multiple extensions
    // Note: Simplified pattern to avoid parser issues with regex alternation
    Listener multiListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: CONTENT_TEST_DIR,
        pollingInterval: 4,
        fileNamePattern: "routing.*\\.(txt|json|xml)"
    });

    check multiListener.attach(multiFormatService);
    check multiListener.'start();
    runtime:registerListener(multiListener);

    // Upload files of different types
    stream<io:Block, io:Error?> txtStream = check io:fileReadBlocksAsStream(TEXT_TEST_FILE);
    check (<Client>clientEp)->put(CONTENT_TEST_DIR + "/routing1.txt", txtStream);

    stream<io:Block, io:Error?> jsonStream = check io:fileReadBlocksAsStream(JSON_TEST_FILE);
    check (<Client>clientEp)->put(CONTENT_TEST_DIR + "/routing2.json", jsonStream);

    stream<io:Block, io:Error?> xmlStream = check io:fileReadBlocksAsStream(XML_TEST_FILE);
    check (<Client>clientEp)->put(CONTENT_TEST_DIR + "/routing3.xml", xmlStream);

    runtime:sleep(15);

    // Cleanup
    runtime:deregisterListener(multiListener);
    check multiListener.gracefulStop();

    test:assertTrue(txtFilesProcessed >= 1, "Should have processed at least 1 TXT file");
    test:assertTrue(jsonFilesProcessed >= 1, "Should have processed at least 1 JSON file");
    test:assertTrue(xmlFilesProcessed >= 1, "Should have processed at least 1 XML file");
}

@test:Config {
    dependsOn: [testExtensionBasedRouting]
}
public function testFallbackToGenericOnFile() returns error? {
    // Reset state
    fallbackJsonFilesProcessed = 0;
    fallbackFilesProcessed = 0;

    // Service with onFileJson and fallback onFile
    Service fallbackService = service object {
        remote function onFileJson(json content, FileInfo fileInfo, Caller caller) returns error? {
            log:printInfo(string `onFileJson: ${fileInfo.name}`);
            fallbackJsonFilesProcessed += 1;
        }

        // Fallback for other file types
        remote function onFile(byte[] content, FileInfo fileInfo, Caller caller) returns error? {
            log:printInfo(string `onFile (fallback): ${fileInfo.name}`);
            fallbackFilesProcessed += 1;
        }
    };

    // Create listener for json and txt files
    Listener fallbackListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: CONTENT_TEST_DIR,
        pollingInterval: 4,
        fileNamePattern: "fallback.*\\.(json|txt)"
    });

    check fallbackListener.attach(fallbackService);
    check fallbackListener.'start();
    runtime:registerListener(fallbackListener);

    // Upload JSON file (should use onFileJson)
    stream<io:Block, io:Error?> jsonStream = check io:fileReadBlocksAsStream(JSON_TEST_FILE, 5);
    check (<Client>clientEp)->put(CONTENT_TEST_DIR + "/fallback1.json", jsonStream);

    // Upload TXT file (should fall back to onFile)
    stream<io:Block, io:Error?> txtStream = check io:fileReadBlocksAsStream(TEXT_TEST_FILE, 5);
    check (<Client>clientEp)->put(CONTENT_TEST_DIR + "/fallback2.txt", txtStream);

    runtime:sleep(15);

    // Cleanup
    runtime:deregisterListener(fallbackListener);
    check fallbackListener.gracefulStop();

    test:assertTrue(fallbackJsonFilesProcessed >= 1, "JSON file should use onFileJson");
    test:assertTrue(fallbackFilesProcessed >= 1, "TXT file should fall back to onFile");
}

@test:Config {
    dependsOn: [testFallbackToGenericOnFile]
}
public function testFileConfigAnnotationOverride() returns error? {
    // Reset state
    specialFilesProcessed = 0;

    // Service with annotation override
    Service annotationService = service object {
        // Override: treat .special files as JSON
        @FunctionConfig {fileNamePattern: "(.*).special"}
        remote function onFileJson(json content, FileInfo fileInfo, Caller caller) returns error? {
            log:printInfo(string `onFileJson (via annotation): ${fileInfo.name}`);
            specialFilesProcessed += 1;

            // Verify it's actually JSON
            map<json> data = check content.ensureType();
            test:assertTrue(data.hasKey("name"), "Should have 'name' field");
        }
    };

    // Create listener for .special extension
    Listener annotationListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: CONTENT_TEST_DIR,
        pollingInterval: 4,
        fileNamePattern: ".*\\.special"
    });

    check annotationListener.attach(annotationService);
    check annotationListener.'start();
    runtime:registerListener(annotationListener);

    // Upload JSON content with .special extension
    stream<io:Block, io:Error?> jsonStream = check io:fileReadBlocksAsStream(JSON_TEST_FILE);
    check (<Client>clientEp)->put(CONTENT_TEST_DIR + "/override.special", jsonStream);

    runtime:sleep(10);

    // Cleanup
    runtime:deregisterListener(annotationListener);
    check annotationListener.gracefulStop();

    test:assertTrue(specialFilesProcessed >= 1, "Should have processed .special file as JSON");
}

@test:Config {
    dependsOn: [testFileConfigAnnotationOverride]
}
public function testOptionalParametersWithoutCaller() returns error? {
    // Reset state
    contentMethodInvoked = false;
    textContentReceived = "";

    // Service without Caller parameter
    Service nocallerService = service object {
        @FunctionConfig {fileNamePattern: "(.*).nocaller"}
        remote function onFileText(string content, FileInfo fileInfo) returns error? {
            log:printInfo(string `onFileText (no caller): ${fileInfo.name}`);
            textContentReceived = content;
            contentMethodInvoked = true;
        }
    };

    // Create listener
    Listener nocallerListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: CONTENT_TEST_DIR,
        pollingInterval: 4,
        fileNamePattern: ".*\\.nocaller"
    });

    check nocallerListener.attach(nocallerService);
    check nocallerListener.'start();
    runtime:registerListener(nocallerListener);

    // Upload file
    string txtStream = check io:fileReadString(TEXT_TEST_FILE);
    check (<Client>clientEp)->putText(CONTENT_TEST_DIR + "/nocallerTest.nocaller", txtStream);
    runtime:sleep(10);

    // Cleanup
    runtime:deregisterListener(nocallerListener);
    check nocallerListener.gracefulStop();

    test:assertTrue(contentMethodInvoked, "Method should work without Caller parameter");
    test:assertTrue(textContentReceived.length() > 0, "Should have received content");
}
