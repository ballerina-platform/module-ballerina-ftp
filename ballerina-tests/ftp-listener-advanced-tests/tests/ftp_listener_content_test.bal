// Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/ftp;
import ballerina/io;
import ballerina/lang.runtime;
import ballerina/test;
import ballerina_tests/ftp_test_commons as commons;

// ─── Record types used for typed-binding tests ────────────────────────────────

type Employee record {
    string name;
    int age;
};

type PersonRecord record {
    string name;
    int age;
    string city;
    boolean? isActive;
};

// ─── Shared client for file setup/teardown ────────────────────────────────────

ftp:Client contentFtpClient = check new ({
    protocol: ftp:FTP,
    host: commons:FTP_HOST,
    port: commons:AUTH_FTP_PORT,
    auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
    userDirIsRoot: false
});

// ─── Isolated directories ─────────────────────────────────────────────────────

const CONTENT_DIR = "/home/in/adv-content";
const ROUTING_DIR = "/home/in/adv-routing";
const POSTPROC_DIR = "/home/in/adv-postproc";

// ─── Helper ───────────────────────────────────────────────────────────────────

function waitUntil(function() returns boolean cond, int timeoutSec) returns boolean {
    int remaining = timeoutSec;
    while remaining > 0 {
        if cond() {
            return true;
        }
        runtime:sleep(1);
        remaining -= 1;
    }
    return false;
}

function contentListenerConfig(string dir, string pattern, decimal poll = 2)
        returns ftp:ListenerConfiguration {
    return {
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
        path: dir,
        pollingInterval: poll,
        fileNamePattern: pattern
    };
}

// =============================================================================
// onFileText
// =============================================================================

string textContent = "";
ftp:FileInfo? textFileInfo = ();

@test:Config {
    groups: ["ftp-listener-advanced", "content-methods"]
}
function testOnFileText_Basic() returns error? {
    textContent = "";
    textFileInfo = ();

    ftp:Service svc = service object {
        remote function onFileText(string content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
            textContent = content;
            textFileInfo = fileInfo;
        }
    };

    ftp:Listener l = check new (contentListenerConfig(CONTENT_DIR, "cnt-text.*\\.txt"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    string text = check io:fileReadString(commons:TEXT_FILE_PATH);
    check contentFtpClient->putText(CONTENT_DIR + "/cnt-text-test.txt", text);

    boolean received = waitUntil(function() returns boolean { return textContent.length() > 0; }, 30);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(received, "onFileText should be invoked");
    test:assertTrue(textContent.length() > 0, "Text content should be non-empty");
    ftp:FileInfo fi = check (textFileInfo).ensureType();
    test:assertTrue(fi.name.endsWith(".txt"), "FileInfo name should end with .txt");

    check contentFtpClient->delete(CONTENT_DIR + "/cnt-text-test.txt");
}

// onFileText without Caller parameter (optional Caller)
string textNocallerContent = "";

@test:Config {
    groups: ["ftp-listener-advanced", "content-methods"],
    dependsOn: [testOnFileText_Basic]
}
function testOnFileText_NoCaller() returns error? {
    textNocallerContent = "";

    ftp:Service svc = service object {
        @ftp:FunctionConfig {fileNamePattern: "cnt-nocaller.*\\.txt"}
        remote function onFileText(string content, ftp:FileInfo fileInfo) returns error? {
            textNocallerContent = content;
        }
    };

    ftp:Listener l = check new (contentListenerConfig(CONTENT_DIR, "cnt-nocaller.*\\.txt"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    string text = check io:fileReadString(commons:TEXT_FILE_PATH);
    check contentFtpClient->putText(CONTENT_DIR + "/cnt-nocaller-test.txt", text);

    boolean received = waitUntil(function() returns boolean { return textNocallerContent.length() > 0; }, 30);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(received, "onFileText without Caller should be invoked");

    check contentFtpClient->delete(CONTENT_DIR + "/cnt-nocaller-test.txt");
}

// =============================================================================
// onFileJson
// =============================================================================

json jsonContent = ();
ftp:FileInfo? jsonFileInfo = ();

@test:Config {
    groups: ["ftp-listener-advanced", "content-methods"],
    dependsOn: [testOnFileText_NoCaller]
}
function testOnFileJson_Basic() returns error? {
    jsonContent = ();
    jsonFileInfo = ();

    ftp:Service svc = service object {
        remote function onFileJson(json content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
            jsonContent = content;
            jsonFileInfo = fileInfo;
        }
    };

    ftp:Listener l = check new (contentListenerConfig(CONTENT_DIR, "cnt-json.*\\.json"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    stream<io:Block, io:Error?> s = check io:fileReadBlocksAsStream(commons:JSON_FILE_PATH);
    check contentFtpClient->put(CONTENT_DIR + "/cnt-json-test.json", s);

    boolean received = waitUntil(function() returns boolean { return jsonContent != (); }, 30);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(received, "onFileJson should be invoked");

    map<json> j = check (jsonContent).ensureType();
    test:assertEquals(j["name"], "John Doe", "name field should match");
    test:assertEquals(check j["age"].ensureType(), 30, "age field should match");
    test:assertEquals(j["city"], "New York", "city field should match");

    check contentFtpClient->delete(CONTENT_DIR + "/cnt-json-test.json");
}

// onFileJson with record type binding
PersonRecord? jsonRecordContent = ();

@test:Config {
    groups: ["ftp-listener-advanced", "content-methods"],
    dependsOn: [testOnFileJson_Basic]
}
function testOnFileJson_RecordType() returns error? {
    jsonRecordContent = ();

    ftp:Service svc = service object {
        remote function onFileJson(PersonRecord content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
            jsonRecordContent = content;
        }
    };

    ftp:Listener l = check new (contentListenerConfig(CONTENT_DIR, "cnt-jsonrec.*\\.json"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    stream<io:Block, io:Error?> s = check io:fileReadBlocksAsStream(commons:JSON_FILE_PATH);
    check contentFtpClient->put(CONTENT_DIR + "/cnt-jsonrec-test.json", s);

    boolean received = waitUntil(function() returns boolean { return jsonRecordContent != (); }, 30);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(received, "onFileJson with record type should be invoked");
    PersonRecord person = check (jsonRecordContent).ensureType();
    test:assertEquals(person.name, "John Doe");
    test:assertEquals(person.age, 30);
    test:assertEquals(person.city, "New York");
    test:assertEquals(person.isActive, true);

    check contentFtpClient->delete(CONTENT_DIR + "/cnt-jsonrec-test.json");
}

// =============================================================================
// onFileXml
// =============================================================================

xml? xmlContent = ();

@test:Config {
    groups: ["ftp-listener-advanced", "content-methods"],
    dependsOn: [testOnFileJson_RecordType]
}
function testOnFileXml_Basic() returns error? {
    xmlContent = ();

    ftp:Service svc = service object {
        remote function onFileXml(xml content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
            xmlContent = content;
        }
    };

    ftp:Listener l = check new (contentListenerConfig(CONTENT_DIR, "cnt-xml.*\\.xml"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    stream<io:Block, io:Error?> s = check io:fileReadBlocksAsStream(commons:XML_FILE_PATH);
    check contentFtpClient->put(CONTENT_DIR + "/cnt-xml-test.xml", s);

    boolean received = waitUntil(function() returns boolean { return xmlContent != (); }, 30);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(received, "onFileXml should be invoked");
    xml x = check (xmlContent).ensureType();
    string xmlStr = x.toString();
    test:assertTrue(xmlStr.includes("Jane Smith"), "XML should contain person name");
    test:assertTrue(xmlStr.includes("Los Angeles"), "XML should contain city");

    check contentFtpClient->delete(CONTENT_DIR + "/cnt-xml-test.xml");
}

// =============================================================================
// onFileCsv — string[][] variant
// =============================================================================

string[][] csvContent = [];
ftp:FileInfo? csvFileInfo = ();

@test:Config {
    groups: ["ftp-listener-advanced", "content-methods"],
    dependsOn: [testOnFileXml_Basic]
}
function testOnFileCsv_StringArray() returns error? {
    csvContent = [];
    csvFileInfo = ();

    ftp:Service svc = service object {
        remote function onFileCsv(string[][] content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
            csvContent = content;
            csvFileInfo = fileInfo;
        }
    };

    ftp:Listener l = check new (contentListenerConfig(CONTENT_DIR, "cnt-csv.*\\.csv"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    stream<io:Block, io:Error?> s = check io:fileReadBlocksAsStream(commons:CSV_FILE_PATH, 5);
    check contentFtpClient->put(CONTENT_DIR + "/cnt-csv-test.csv", s);

    boolean received = waitUntil(function() returns boolean { return csvContent.length() > 0; }, 30);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(received, "onFileCsv (string[][]) should be invoked");
    test:assertTrue(csvContent.length() >= 3, "Should have at least 3 data rows");
    test:assertEquals(csvContent[0][0], "Alice Johnson", "First row name should match");
    test:assertEquals(csvContent[0][1], "25", "First row age should match");

    check contentFtpClient->delete(CONTENT_DIR + "/cnt-csv-test.csv");
}

// onFileCsv — record array variant
Employee[] csvRecordContent = [];

@test:Config {
    groups: ["ftp-listener-advanced", "content-methods"],
    dependsOn: [testOnFileCsv_StringArray]
}
function testOnFileCsv_RecordArray() returns error? {
    csvRecordContent = [];

    ftp:Service svc = service object {
        remote function onFileCsv(Employee[] content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
            csvRecordContent = content;
        }
    };

    ftp:Listener l = check new (contentListenerConfig(CONTENT_DIR, "cnt-csvrec.*\\.csv"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    stream<io:Block, io:Error?> s = check io:fileReadBlocksAsStream(commons:CSV_FILE_PATH);
    check contentFtpClient->put(CONTENT_DIR + "/cnt-csvrec-test.csv", s);

    boolean received = waitUntil(function() returns boolean { return csvRecordContent.length() > 0; }, 30);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(received, "onFileCsv (Employee[]) should be invoked");
    test:assertTrue(csvRecordContent.length() >= 3, "Should have at least 3 Employee records");
    test:assertEquals(csvRecordContent[0].name, "Alice Johnson");
    test:assertEquals(csvRecordContent[0].age, 25);

    check contentFtpClient->delete(CONTENT_DIR + "/cnt-csvrec-test.csv");
}

// onFileCsv — stream variant
int csvStreamRowCount = 0;

@test:Config {
    groups: ["ftp-listener-advanced", "content-methods"],
    dependsOn: [testOnFileCsv_RecordArray]
}
function testOnFileCsv_Stream() returns error? {
    csvStreamRowCount = 0;

    ftp:Service svc = service object {
        remote function onFileCsv(stream<string[], error?> content, ftp:FileInfo fileInfo,
                                  ftp:Caller caller) returns error? {
            check content.forEach(function(string[] row) {
                csvStreamRowCount += 1;
            });
        }
    };

    ftp:Listener l = check new (contentListenerConfig(CONTENT_DIR, "cnt-csvstream.*\\.csv"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    stream<io:Block, io:Error?> s = check io:fileReadBlocksAsStream(commons:CSV_FILE_PATH, 5);
    check contentFtpClient->put(CONTENT_DIR + "/cnt-csvstream-test.csv", s);

    boolean received = waitUntil(function() returns boolean { return csvStreamRowCount > 0; }, 30);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(received, "onFileCsv (stream) should be invoked");
    test:assertTrue(csvStreamRowCount > 0, "Should have processed CSV rows");

    check contentFtpClient->delete(CONTENT_DIR + "/cnt-csvstream-test.csv");
}

// =============================================================================
// onFile — generic byte[] and stream variants
// =============================================================================

byte[] byteContent = [];

@test:Config {
    groups: ["ftp-listener-advanced", "content-methods"],
    dependsOn: [testOnFileCsv_Stream]
}
function testOnFile_ByteArray() returns error? {
    byteContent = [];

    ftp:Service svc = service object {
        remote function onFile(byte[] content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
            byteContent = content;
        }
    };

    ftp:Listener l = check new (contentListenerConfig(CONTENT_DIR, "cnt-bin.*\\.bin"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    stream<io:Block, io:Error?> s = check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH);
    check contentFtpClient->put(CONTENT_DIR + "/cnt-bin-test.bin", s);

    boolean received = waitUntil(function() returns boolean { return byteContent.length() > 0; }, 30);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(received, "onFile (byte[]) should be invoked");
    test:assertTrue(byteContent.length() > 0, "Byte content should be non-empty");

    check contentFtpClient->delete(CONTENT_DIR + "/cnt-bin-test.bin");
}

int streamByteCount = 0;

@test:Config {
    groups: ["ftp-listener-advanced", "content-methods"],
    dependsOn: [testOnFile_ByteArray]
}
function testOnFile_Stream() returns error? {
    streamByteCount = 0;

    ftp:Service svc = service object {
        remote function onFile(stream<byte[], error?> content, ftp:FileInfo fileInfo,
                               ftp:Caller caller) returns error? {
            check content.forEach(function(byte[] chunk) {
                streamByteCount += chunk.length();
            });
        }
    };

    ftp:Listener l = check new (contentListenerConfig(CONTENT_DIR, "cnt-stream.*\\.stream1"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    stream<io:Block, io:Error?> s = check io:fileReadBlocksAsStream(commons:TEXT_FILE_PATH);
    check contentFtpClient->put(CONTENT_DIR + "/cnt-stream-test.stream", s);
    check contentFtpClient->rename(CONTENT_DIR + "/cnt-stream-test.stream",
                                   CONTENT_DIR + "/cnt-stream-test.stream1");

    boolean received = waitUntil(function() returns boolean { return streamByteCount > 0; }, 30);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(received, "onFile (stream) should be invoked");
    test:assertTrue(streamByteCount > 0, "Should have processed bytes from stream");

    check contentFtpClient->delete(CONTENT_DIR + "/cnt-stream-test.stream1");
}

// =============================================================================
// Extension-based routing — multiple content methods on one service
// =============================================================================

int routingTxtCount = 0;
int routingJsonCount = 0;
int routingXmlCount = 0;

@test:Config {
    groups: ["ftp-listener-advanced", "content-methods"],
    dependsOn: [testOnFile_Stream]
}
function testExtensionBasedRouting() returns error? {
    routingTxtCount = 0;
    routingJsonCount = 0;
    routingXmlCount = 0;

    ftp:Service svc = service object {
        remote function onFileText(string content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
            routingTxtCount += 1;
        }
        remote function onFileJson(json content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
            routingJsonCount += 1;
        }
        remote function onFileXml(xml content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
            routingXmlCount += 1;
        }
    };

    ftp:Listener l = check new (contentListenerConfig(ROUTING_DIR, "routing.*\\.(txt|json|xml)"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    string text = check io:fileReadString(commons:TEXT_FILE_PATH);
    check contentFtpClient->putText(ROUTING_DIR + "/routing1.txt", text);

    stream<io:Block, io:Error?> js = check io:fileReadBlocksAsStream(commons:JSON_FILE_PATH);
    check contentFtpClient->put(ROUTING_DIR + "/routing2.json", js);

    stream<io:Block, io:Error?> xs = check io:fileReadBlocksAsStream(commons:XML_FILE_PATH);
    check contentFtpClient->put(ROUTING_DIR + "/routing3.xml", xs);

    boolean allReceived = waitUntil(function() returns boolean {
        return routingTxtCount >= 1 && routingJsonCount >= 1 && routingXmlCount >= 1;
    }, 30);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(allReceived, "All three content methods should fire for their respective file types");

    check contentFtpClient->delete(ROUTING_DIR + "/routing1.txt");
    check contentFtpClient->delete(ROUTING_DIR + "/routing2.json");
    check contentFtpClient->delete(ROUTING_DIR + "/routing3.xml");
}

// Fallback: JSON gets onFileJson, unknown type falls back to onFile
int fallbackJsonCount = 0;
int fallbackGenericCount = 0;

@test:Config {
    groups: ["ftp-listener-advanced", "content-methods"],
    dependsOn: [testExtensionBasedRouting]
}
function testFallbackToGenericOnFile() returns error? {
    fallbackJsonCount = 0;
    fallbackGenericCount = 0;

    ftp:Service svc = service object {
        remote function onFileJson(json content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
            fallbackJsonCount += 1;
        }
        remote function onFile(byte[] content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
            fallbackGenericCount += 1;
        }
    };

    ftp:Listener l = check new (contentListenerConfig(ROUTING_DIR, "fallback.*\\.(json|txt)"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    stream<io:Block, io:Error?> js = check io:fileReadBlocksAsStream(commons:JSON_FILE_PATH, 5);
    check contentFtpClient->put(ROUTING_DIR + "/fallback1.json", js);

    string text = check io:fileReadString(commons:TEXT_FILE_PATH);
    check contentFtpClient->putText(ROUTING_DIR + "/fallback2.txt", text);

    boolean received = waitUntil(function() returns boolean {
        return fallbackJsonCount >= 1 && fallbackGenericCount >= 1;
    }, 30);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(received, "JSON file should use onFileJson, TXT should fall back to onFile");

    check contentFtpClient->delete(ROUTING_DIR + "/fallback1.json");
    check contentFtpClient->delete(ROUTING_DIR + "/fallback2.txt");
}

// =============================================================================
// @FunctionConfig — pattern override and post-processing
// =============================================================================

// @FunctionConfig.fileNamePattern overrides extension routing for non-standard extensions.
int specialFileCount = 0;

@test:Config {
    groups: ["ftp-listener-advanced", "function-config"]
}
function testFunctionConfig_PatternOverride() returns error? {
    specialFileCount = 0;

    ftp:Service svc = service object {
        @ftp:FunctionConfig {fileNamePattern: "(.*)\\.special"}
        remote function onFileJson(json content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
            map<json> data = check content.ensureType();
            if data.hasKey("name") {
                specialFileCount += 1;
            }
        }
    };

    ftp:Listener l = check new (contentListenerConfig(POSTPROC_DIR, ".*\\.special"));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    stream<io:Block, io:Error?> s = check io:fileReadBlocksAsStream(commons:JSON_FILE_PATH);
    check contentFtpClient->put(POSTPROC_DIR + "/override.special", s);

    boolean received = waitUntil(function() returns boolean { return specialFileCount >= 1; }, 30);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(received, "@FunctionConfig should route .special files to onFileJson");

    check contentFtpClient->delete(POSTPROC_DIR + "/override.special");
}

// @FunctionConfig.afterProcess = DELETE: file is removed from server after successful processing.
int afterDeleteCount = 0;

@test:Config {
    groups: ["ftp-listener-advanced", "function-config"],
    dependsOn: [testFunctionConfig_PatternOverride]
}
function testFunctionConfig_AfterProcessDelete() returns error? {
    afterDeleteCount = 0;

    ftp:Service svc = service object {
        @ftp:FunctionConfig {afterProcess: ftp:DELETE}
        remote function onFileText(string content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
            afterDeleteCount += 1;
        }
    };

    ftp:Listener l = check new (contentListenerConfig(POSTPROC_DIR, "afterdel.*\\.txt", 2));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    string remotePath = POSTPROC_DIR + "/afterdel-test.txt";
    string text = check io:fileReadString(commons:TEXT_FILE_PATH);
    check contentFtpClient->putText(remotePath, text);

    boolean processed = waitUntil(function() returns boolean { return afterDeleteCount >= 1; }, 30);

    // Give the post-process action time to run
    runtime:sleep(3);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(processed, "onFileText with afterProcess=DELETE should be invoked");

    // Verify file was deleted by the post-process action
    ftp:Error|boolean exists = contentFtpClient->exists(remotePath);
    if exists is boolean {
        test:assertFalse(exists, "File should have been deleted by afterProcess=DELETE");
    }
    // File already deleted — no cleanup needed
}

// @FunctionConfig.afterProcess = MOVE: file is moved to a target directory after processing.
int afterMoveCount = 0;

@test:Config {
    groups: ["ftp-listener-advanced", "function-config"],
    dependsOn: [testFunctionConfig_AfterProcessDelete]
}
function testFunctionConfig_AfterProcessMove() returns error? {
    afterMoveCount = 0;

    string moveTarget = "/home/in/adv-postproc-done";

    ftp:Service svc = service object {
        @ftp:FunctionConfig {afterProcess: {moveTo: "/home/in/adv-postproc-done"}}
        remote function onFileText(string content, ftp:FileInfo fileInfo, ftp:Caller caller) returns error? {
            afterMoveCount += 1;
        }
    };

    ftp:Listener l = check new (contentListenerConfig(POSTPROC_DIR, "aftermove.*\\.txt", 2));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    string remotePath = POSTPROC_DIR + "/aftermove-test.txt";
    string text = check io:fileReadString(commons:TEXT_FILE_PATH);
    check contentFtpClient->putText(remotePath, text);

    boolean processed = waitUntil(function() returns boolean { return afterMoveCount >= 1; }, 30);

    runtime:sleep(3);

    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(processed, "onFileText with afterProcess=MOVE should be invoked");

    // Verify original file no longer exists in source dir
    ftp:Error|boolean exists = contentFtpClient->exists(remotePath);
    if exists is boolean {
        test:assertFalse(exists, "File should have been moved out of the source directory");
    }

    // Cleanup the moved file (best effort)
    check contentFtpClient->delete(moveTarget + "/aftermove-test.txt");
}

// @FunctionConfig.afterError = DELETE: file is removed when the content handler returns an error.
// This exercises the afterError.ifPresent(...) paths in FtpContentCallbackHandler (lines that
// were unreachable since no previous test used afterError).
const string AFTER_ERR_DIR = "/home/in/adv-postproc-after-error";
isolated boolean afterErrInvoked = false;

@test:Config {
    groups: ["ftp-listener-advanced", "function-config"],
    dependsOn: [testFunctionConfig_AfterProcessMove]
}
function testFunctionConfig_AfterErrorDelete() returns error? {
    lock { afterErrInvoked = false; }

    // Handler that always returns an error to trigger afterError.
    ftp:Service svc = service object {
        @ftp:FunctionConfig {afterError: ftp:DELETE}
        remote function onFileText(string content, ftp:FileInfo fileInfo) returns error? {
            lock { afterErrInvoked = true; }
            return error("Intentional processing error to trigger afterError");
        }
    };

    ftp:Listener l = check new (contentListenerConfig(AFTER_ERR_DIR, "aftererr.*\\.txt", 2));
    check l.attach(svc);
    check l.'start();
    runtime:registerListener(l);

    string remotePath = AFTER_ERR_DIR + "/aftererr-test.txt";
    check contentFtpClient->putText(remotePath, "trigger error");

    // Wait for handler to be invoked and afterError to execute.
    boolean invoked = false;
    int elapsed = 0;
    while (!invoked && elapsed < 15) {
        runtime:sleep(1);
        elapsed += 1;
        lock { invoked = afterErrInvoked; }
    }

    runtime:sleep(2); // allow afterError delete to complete
    runtime:deregisterListener(l);
    check l.gracefulStop();

    test:assertTrue(invoked, "onFileText should have been invoked");

    // File must have been deleted by afterError=DELETE.
    ftp:Error|boolean exists = contentFtpClient->exists(remotePath);
    test:assertTrue(exists is boolean && !<boolean>exists,
        "afterError=DELETE should remove the file when the handler returns an error");
}
