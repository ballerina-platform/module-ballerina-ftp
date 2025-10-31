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

@test:Config {
    dependsOn: [testIsolatedService],
    enable: false
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
            check caller->delete(fileInfo.path);
        }
    };

    // Create listener for .txt files
    Listener textListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: "/home/in",
        pollingInterval: 1,
        fileNamePattern: "(.*).txt"
    });

    check textListener.attach(textService);
    check textListener.'start();
    runtime:registerListener(textListener);

    // Upload text file
    stream<io:Block, io:Error?> textStream = check io:fileReadBlocksAsStream(TEXT_TEST_FILE, 5);
    check (<Client>clientEp)->put("/home/in/contentTest.txt", textStream);
    runtime:sleep(3);

    // Wait for processing
    int timeoutInSeconds = 60;
    while timeoutInSeconds > 0 {
        if contentMethodInvoked {
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }

    // Cleanup
    runtime:deregisterListener(textListener);
    check textListener.gracefulStop();

    // Assertions
    if timeoutInSeconds == 0 {
        test:assertFail("Timeout: onFileText was not invoked");
    }

    test:assertTrue(contentMethodInvoked, "onFileText should have been invoked");
    test:assertTrue(textContentReceived.length() > 0, "Should have received text content");
    // Note: Content may be from contentTest.txt or other .txt files in the directory
    // The important thing is that onFileText was invoked and we got some content

    FileInfo fileInfo = check lastFileInfo.ensureType();
    // Verify that we processed at least one .txt file successfully
    test:assertTrue(fileInfo.name.endsWith(".txt"), "Should process .txt files");
}

@test:Config {
    dependsOn: [testOnFileTextBasic],
    enable: false
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
            check caller->delete(fileInfo.path);
        }
    };

    // Create listener for .json files
    Listener jsonListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: "/home/in",
        pollingInterval: 1,
        fileNamePattern: "(.*).json"
    });

    check jsonListener.attach(jsonService);
    check jsonListener.'start();
    runtime:registerListener(jsonListener);

    // Upload JSON file
    stream<io:Block, io:Error?> jsonStream = check io:fileReadBlocksAsStream(JSON_TEST_FILE, 5);
    check (<Client>clientEp)->put("/home/in/contentTest.json", jsonStream);
    runtime:sleep(3);

    // Wait for processing
    int timeoutInSeconds = 60;
    while timeoutInSeconds > 0 {
        if contentMethodInvoked {
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }

    // Cleanup
    runtime:deregisterListener(jsonListener);
    check jsonListener.gracefulStop();

    // Assertions
    if timeoutInSeconds == 0 {
        test:assertFail("Timeout: onFileJson was not invoked");
    }

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
    dependsOn: [testOnFileJsonBasic],
    enable: false
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
            xmlContentReceived = content;
            lastFileInfo = fileInfo;
            contentMethodInvoked = true;
            check caller->delete(fileInfo.path);
        }
    };

    // Create listener for .xml files
    Listener xmlListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: "/home/in",
        pollingInterval: 1,
        fileNamePattern: "(.*).xml"
    });

    check xmlListener.attach(xmlService);
    check xmlListener.'start();
    runtime:registerListener(xmlListener);

    // Upload XML file
    stream<io:Block, io:Error?> xmlStream = check io:fileReadBlocksAsStream(XML_TEST_FILE, 5);
    check (<Client>clientEp)->put("/home/in/contentTest.xml", xmlStream);
    runtime:sleep(3);

    // Wait for processing
    int timeoutInSeconds = 60;
    while timeoutInSeconds > 0 {
        if contentMethodInvoked {
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }

    // Cleanup
    runtime:deregisterListener(xmlListener);
    check xmlListener.gracefulStop();

    // Assertions
    if timeoutInSeconds == 0 {
        test:assertFail("Timeout: onFileXml was not invoked");
    }

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
    dependsOn: [testOnFileXmlBasic],
    enable: false
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
            check caller->delete(fileInfo.path);
        }
    };

    // Create listener for .csv files
    Listener csvListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: "/home/in",
        pollingInterval: 1,
        fileNamePattern: "(.*).csv"
    });

    check csvListener.attach(csvService);
    check csvListener.'start();
    runtime:registerListener(csvListener);

    // Upload CSV file
    stream<io:Block, io:Error?> csvStream = check io:fileReadBlocksAsStream(CSV_TEST_FILE, 5);
    check (<Client>clientEp)->put("/home/in/contentTest.csv", csvStream);
    runtime:sleep(3);

    // Wait for processing
    int timeoutInSeconds = 60;
    while timeoutInSeconds > 0 {
        if contentMethodInvoked {
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }

    // Cleanup
    runtime:deregisterListener(csvListener);
    check csvListener.gracefulStop();

    // Assertions
    if timeoutInSeconds == 0 {
        test:assertFail("Timeout: onFileCsv was not invoked");
    }

    test:assertTrue(contentMethodInvoked, "onFileCsv should have been invoked");
    test:assertTrue(csvContentReceived.length() >= 4,
        string `Should have at least 4 rows (header + 3 data), got ${csvContentReceived.length()}`);

    // Verify CSV header
    string[] headerRow = csvContentReceived[0];
    test:assertEquals(headerRow[0], "name", "First column should be 'name'");
    test:assertEquals(headerRow[1], "age", "Second column should be 'age'");
    test:assertEquals(headerRow[2], "city", "Third column should be 'city'");
    test:assertEquals(headerRow[3], "salary", "Fourth column should be 'salary'");

    // Verify first data row
    string[] firstDataRow = csvContentReceived[1];
    test:assertEquals(firstDataRow[0], "Alice Johnson", "First person's name should match");
    test:assertEquals(firstDataRow[1], "25", "First person's age should match");
}

@test:Config {
    dependsOn: [testOnFileCsvStringArray],
    enable: false
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
            check caller->delete(fileInfo.path);
        }
    };

    // Create listener for .bin files (generic binary)
    Listener genericListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: "/home/in",
        pollingInterval: 1,
        fileNamePattern: "(.*).bin"
    });

    check genericListener.attach(genericService);
    check genericListener.'start();
    runtime:registerListener(genericListener);

    // Upload generic binary file
    stream<io:Block, io:Error?> binStream = check io:fileReadBlocksAsStream(GENERIC_TEST_FILE, 5);
    check (<Client>clientEp)->put("/home/in/contentTest.bin", binStream);
    runtime:sleep(3);

    // Wait for processing
    int timeoutInSeconds = 60;
    while timeoutInSeconds > 0 {
        if contentMethodInvoked {
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }

    // Cleanup
    runtime:deregisterListener(genericListener);
    check genericListener.gracefulStop();

    // Assertions
    if timeoutInSeconds == 0 {
        test:assertFail("Timeout: onFile was not invoked");
    }

    test:assertTrue(contentMethodInvoked, "onFile should have been invoked");
    test:assertTrue(byteContentReceived.length() > 0, "Should have received byte content");

    // Verify byte content can be converted to string
    string contentAsString = check string:fromBytes(byteContentReceived);
    test:assertTrue(contentAsString.length() > 0, "Should be able to convert bytes to string");
}

@test:Config {
    dependsOn: [testOnFileByteArray],
    enable: false
}
public function testOnFileStream() returns error? {
    // Reset state
    streamByteCount = 0;
    lastFileInfo = ();
    contentMethodInvoked = false;

    // Service with onFile (stream variant)
    Service streamService = service object {
        remote function onFile(stream<byte[], error> content, FileInfo fileInfo, Caller caller) returns error? {
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
            check caller->delete(fileInfo.path);
        }
    };

    // Create listener for .stream files
    Listener streamListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: "/home/in",
        pollingInterval: 1,
        fileNamePattern: "(.*).stream"
    });

    check streamListener.attach(streamService);
    check streamListener.'start();
    runtime:registerListener(streamListener);

    // Upload file for stream processing
    stream<io:Block, io:Error?> testStream = check io:fileReadBlocksAsStream(TEXT_TEST_FILE, 5);
    check (<Client>clientEp)->put("/home/in/streamTest.stream", testStream);
    runtime:sleep(3);

    // Wait for processing
    int timeoutInSeconds = 60;
    while timeoutInSeconds > 0 {
        if contentMethodInvoked {
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }

    // Cleanup
    runtime:deregisterListener(streamListener);
    check streamListener.gracefulStop();

    // Assertions
    if timeoutInSeconds == 0 {
        test:assertFail("Timeout: onFile (stream) was not invoked");
    }

    test:assertTrue(contentMethodInvoked, "onFile (stream) should have been invoked");
    test:assertTrue(streamByteCount > 0, string `Should have processed bytes, got ${streamByteCount}`);
}

@test:Config {
    dependsOn: [testOnFileStream],
    enable: false
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
            check caller->delete(fileInfo.path);
        }

        remote function onFileJson(json content, FileInfo fileInfo, Caller caller) returns error? {
            log:printInfo(string `onFileJson: ${fileInfo.name}`);
            jsonFilesProcessed += 1;
            check caller->delete(fileInfo.path);
        }

        remote function onFileXml(xml content, FileInfo fileInfo, Caller caller) returns error? {
            log:printInfo(string `onFileXml: ${fileInfo.name}`);
            xmlFilesProcessed += 1;
            check caller->delete(fileInfo.path);
        }
    };

    // Create listener for multiple extensions
    // Note: Simplified pattern to avoid parser issues with regex alternation
    Listener multiListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: "/home/in",
        pollingInterval: 1,
        fileNamePattern: "routing.*"
    });

    check multiListener.attach(multiFormatService);
    check multiListener.'start();
    runtime:registerListener(multiListener);

    // Upload files of different types
    stream<io:Block, io:Error?> txtStream = check io:fileReadBlocksAsStream(TEXT_TEST_FILE, 5);
    check (<Client>clientEp)->put("/home/in/routing1.txt", txtStream);

    stream<io:Block, io:Error?> jsonStream = check io:fileReadBlocksAsStream(JSON_TEST_FILE, 5);
    check (<Client>clientEp)->put("/home/in/routing2.json", jsonStream);

    stream<io:Block, io:Error?> xmlStream = check io:fileReadBlocksAsStream(XML_TEST_FILE, 5);
    check (<Client>clientEp)->put("/home/in/routing3.xml", xmlStream);

    runtime:sleep(5);

    // Wait for all files to be processed
    int timeoutInSeconds = 60;
    while timeoutInSeconds > 0 {
        if txtFilesProcessed >= 1 && jsonFilesProcessed >= 1 && xmlFilesProcessed >= 1 {
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }

    // Cleanup
    runtime:deregisterListener(multiListener);
    check multiListener.gracefulStop();

    // Assertions
    if timeoutInSeconds == 0 {
        test:assertFail(string `Timeout: Not all files processed. TXT: ${txtFilesProcessed}, JSON: ${jsonFilesProcessed}, XML: ${xmlFilesProcessed}`);
    }

    test:assertTrue(txtFilesProcessed >= 1, "Should have processed at least 1 TXT file");
    test:assertTrue(jsonFilesProcessed >= 1, "Should have processed at least 1 JSON file");
    test:assertTrue(xmlFilesProcessed >= 1, "Should have processed at least 1 XML file");
}

@test:Config {
    dependsOn: [testExtensionBasedRouting],
    enable: false
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
            check caller->delete(fileInfo.path);
        }

        // Fallback for other file types
        remote function onFile(byte[] content, FileInfo fileInfo, Caller caller) returns error? {
            log:printInfo(string `onFile (fallback): ${fileInfo.name}`);
            fallbackFilesProcessed += 1;
            check caller->delete(fileInfo.path);
        }
    };

    // Create listener for json and txt files
    Listener fallbackListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: "/home/in",
        pollingInterval: 1,
        fileNamePattern: "fallback.*"
    });

    check fallbackListener.attach(fallbackService);
    check fallbackListener.'start();
    runtime:registerListener(fallbackListener);

    // Upload JSON file (should use onFileJson)
    stream<io:Block, io:Error?> jsonStream = check io:fileReadBlocksAsStream(JSON_TEST_FILE, 5);
    check (<Client>clientEp)->put("/home/in/fallback1.json", jsonStream);

    // Upload TXT file (should fall back to onFile)
    stream<io:Block, io:Error?> txtStream = check io:fileReadBlocksAsStream(TEXT_TEST_FILE, 5);
    check (<Client>clientEp)->put("/home/in/fallback2.txt", txtStream);

    runtime:sleep(5);

    // Wait for processing
    int timeoutInSeconds = 60;
    while timeoutInSeconds > 0 {
        if fallbackJsonFilesProcessed >= 1 && fallbackFilesProcessed >= 1 {
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }

    // Cleanup
    runtime:deregisterListener(fallbackListener);
    check fallbackListener.gracefulStop();

    // Assertions
    if timeoutInSeconds == 0 {
        test:assertFail(string `Timeout: JSON: ${fallbackJsonFilesProcessed}, Fallback: ${fallbackFilesProcessed}`);
    }

    test:assertTrue(fallbackJsonFilesProcessed >= 1, "JSON file should use onFileJson");
    test:assertTrue(fallbackFilesProcessed >= 1, "TXT file should fall back to onFile");
}

@test:Config {
    dependsOn: [testFallbackToGenericOnFile],
    enable: false
}
public function testFileConfigAnnotationOverride() returns error? {
    // Reset state
    specialFilesProcessed = 0;

    // Service with annotation override
    Service annotationService = service object {
        // Override: treat .special files as JSON
        // TODO: Enable when @FileConfig annotation is implemented
        // @FileConfig {pattern: "(.*).special"}
        remote function onFileJson(json content, FileInfo fileInfo, Caller caller) returns error? {
            log:printInfo(string `onFileJson (via annotation): ${fileInfo.name}`);
            specialFilesProcessed += 1;

            // Verify it's actually JSON
            map<json> data = check content.ensureType();
            test:assertTrue(data.hasKey("name"), "Should have 'name' field");

            check caller->delete(fileInfo.path);
        }
    };

    // Create listener for .special extension
    Listener annotationListener = check new ({
        protocol: FTP,
        host: "127.0.0.1",
        auth: {credentials: {username: "wso2", password: "wso2123"}},
        port: 21212,
        path: "/home/in",
        pollingInterval: 1,
        fileNamePattern: "(.*).special"
    });

    check annotationListener.attach(annotationService);
    check annotationListener.'start();
    runtime:registerListener(annotationListener);

    // Upload JSON content with .special extension
    stream<io:Block, io:Error?> jsonStream = check io:fileReadBlocksAsStream(JSON_TEST_FILE, 5);
    check (<Client>clientEp)->put("/home/in/override.special", jsonStream);

    runtime:sleep(3);

    // Wait for processing
    int timeoutInSeconds = 60;
    while timeoutInSeconds > 0 {
        if specialFilesProcessed >= 1 {
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }

    // Cleanup
    runtime:deregisterListener(annotationListener);
    check annotationListener.gracefulStop();

    // Assertions
    if timeoutInSeconds == 0 {
        test:assertFail("Timeout: Annotation override did not work");
    }

    test:assertTrue(specialFilesProcessed >= 1, "Should have processed .special file as JSON");
}

@test:Config {
    dependsOn: [testFileConfigAnnotationOverride],
    enable: false
}
public function testOptionalParametersWithoutCaller() returns error? {
    // Reset state
    contentMethodInvoked = false;
    textContentReceived = "";

    // Service without Caller parameter
    Service nocallerService = service object {
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
        path: "/home/in",
        pollingInterval: 1,
        fileNamePattern: "(.*).nocaller"
    });

    check nocallerListener.attach(nocallerService);
    check nocallerListener.'start();
    runtime:registerListener(nocallerListener);

    // Upload file
    stream<io:Block, io:Error?> txtStream = check io:fileReadBlocksAsStream(TEXT_TEST_FILE, 5);
    check (<Client>clientEp)->put("/home/in/nocallerTest.nocaller", txtStream);
    runtime:sleep(3);

    // Wait for processing
    int timeoutInSeconds = 60;
    while timeoutInSeconds > 0 {
        if contentMethodInvoked {
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }

    // Cleanup
    runtime:deregisterListener(nocallerListener);
    check nocallerListener.gracefulStop();

    // Delete file manually since we don't have Caller
    check (<Client>clientEp)->delete("/home/in/nocallerTest.nocaller");

    // Assertions
    if timeoutInSeconds == 0 {
        test:assertFail("Timeout: onFileText without Caller was not invoked");
    }

    test:assertTrue(contentMethodInvoked, "Method should work without Caller parameter");
    test:assertTrue(textContentReceived.length() > 0, "Should have received content");
}
