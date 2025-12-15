// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/io;
import ballerina/lang.'string as strings;
import ballerina/lang.runtime as runtime;
import ballerina/log;
import ballerina/test;

string filePath = "/home/in/test1.txt";
string nonFittingFilePath = "/home/in/test4.txt";
string newFilePath = "/home/in/test2.txt";
string appendFilePath = "tests/resources/datafiles/file1.txt";
string putFilePath = "tests/resources/datafiles/file2.txt";
string relativePath = "rel-put.txt";
string relativePathWithSlash = "/rel-path-slash-put.txt";
string absPath = "//home/in/double-abs.txt";

// Create the config to access anonymous mock FTP server
ClientConfiguration anonConfig = {
    protocol: FTP,
    host: "127.0.0.1",
    port: 21210
};

// Create the config to access mock FTP server
ClientConfiguration config = {
    protocol: FTP,
    host: "127.0.0.1",
    port: 21212,
    auth: {credentials: {username: "wso2", password: "wso2123"}},
    userDirIsRoot: false
};

// Create the config to access mock FTP server
ClientConfiguration configLaxDataBinding = {
    protocol: FTP,
    host: "127.0.0.1",
    port: 21212,
    auth: {credentials: {username: "wso2", password: "wso2123"}},
    userDirIsRoot: false,
    laxDataBinding: true
};

ClientConfiguration ftpUserHomeRootConfig = {
    protocol: FTP,
    host: "127.0.0.1",
    port: 21212,
    auth: {credentials: {username: "wso2", password: "wso2123"}},
    userDirIsRoot: true
};

// Create the config to access mock SFTP server
ClientConfiguration sftpConfig = {
    protocol: SFTP,
    host: "127.0.0.1",
    port: 21213,
    auth: {
        credentials: {username: "wso2", password: "wso2123"},
        privateKey: {
            path: "tests/resources/sftp.private.key",
            password: "changeit"
        },
        preferredMethods: [GSSAPI_WITH_MIC, PUBLICKEY, KEYBOARD_INTERACTIVE, PASSWORD]
    }
};

// Create the config to access mock SFTP server with jailed home
ClientConfiguration sftpConfigUserDirRoot = {
    protocol: SFTP,
    host: "127.0.0.1",
    port: 21213,
    auth: {
        credentials: {username: "wso2", password: "wso2123"},
        privateKey: {
            path: "tests/resources/sftp.private.key",
            password: "changeit"
        },
        preferredMethods: [GSSAPI_WITH_MIC, PUBLICKEY, KEYBOARD_INTERACTIVE, PASSWORD]
    },
    userDirIsRoot: true
};

Client? anonClientEp = ();
Client? clientEp = ();
Client? clientEpLaxDataBinding = ();
Client? sftpClientEp = ();
Client? sftpClientUserDirRootEp = ();
Client? ftpUserHomeRootClientEp = ();

Listener? callerListener = ();
Listener? remoteServerListener = ();
Listener? anonymousRemoteServerListener = ();
Listener? secureRemoteServerListener = ();

@test:BeforeSuite
function initTestEnvironment() returns error?  {
    io:println("Starting servers");
    anonClientEp = check new (anonConfig);
    clientEp = check new (config);
    clientEpLaxDataBinding = check new (configLaxDataBinding);
    sftpClientEp = check new (sftpConfig);
    sftpClientUserDirRootEp = check new (sftpConfigUserDirRoot);
    ftpUserHomeRootClientEp = check new (ftpUserHomeRootConfig);

    callerListener = check new (callerListenerConfig);
    check (<Listener>callerListener).attach(callerService);
    check (<Listener>callerListener).'start();

    remoteServerListener = check new (remoteServerConfiguration);
    check (<Listener>remoteServerListener).attach(remoteServerService);
    check (<Listener>remoteServerListener).'start();

    anonymousRemoteServerListener = check new (anonymousRemoteServerConfig);
    check (<Listener>anonymousRemoteServerListener).attach(anonymousRemoteServerService);
    check (<Listener>anonymousRemoteServerListener).'start();

    secureRemoteServerListener = check new (secureRemoteServerConfig);
    check (<Listener>secureRemoteServerListener).attach(secureRemoteServerService);
    check (<Listener>secureRemoteServerListener).'start();
}

@test:Config {}
public function testReadFromAnonServer() returns error? {
    stream<byte[] & readonly, io:Error?>|Error str = (<Client>anonClientEp)->get(filePath);
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchStreamContent(str, "File content"), msg = "Found unexpected content from `get` operation");
        io:Error? closeResult = str.close();
        if closeResult is io:Error {
            test:assertFail(msg = "Error while closing stream in `get` operation." + closeResult.message());
        }
    } else {
        test:assertFail(msg = "Found unexpected response type" + str.message());
    }
}

@test:Config {
    dependsOn: [testReadFromAnonServer, testAddedFileCount, testSecureAddedFileCount]
}
public function testReadBlockFittingContent() returns error? {
    stream<byte[] & readonly, io:Error?>|Error str = (<Client>clientEp)->get(filePath);
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchStreamContent(str, "File content"), msg = "Found unexpected content from `get` operation");
        io:Error? closeResult = str.close();
        if closeResult is io:Error {
            test:assertFail(msg = "Error while closing stream in `get` operation." + closeResult.message());
        }
    } else {
        test:assertFail(msg = "Found unexpected response type" + str.message());
    }
}

@test:Config {
    dependsOn: [testReadBlockFittingContent]
}
public function testReadBlockNonFittingContent() returns error? {
    stream<byte[] & readonly, io:Error?>|Error str = (<Client>clientEp)->get(nonFittingFilePath);
    int i = 0;
    string nonFittingContent = "";
    while i < 1000 {
        nonFittingContent += "123456789";
        i += 1;
    }
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchStreamContent(str, nonFittingContent), msg = "Found unexpected content from `get` operation");
        io:Error? closeResult = str.close();
        if closeResult is io:Error {
            test:assertFail(msg = "Error while closing stream in `get` operation." + closeResult.message());
        }
    } else {
        test:assertFail(msg = "Found unexpected response type" + str.message());
    }
}

@test:Config {
    dependsOn: [testReadBlockNonFittingContent]
}
public function testAppendContent() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(appendFilePath, 7);

    Error? response = (<Client>clientEp)->append(filePath, bStream);
    if response is Error {
        test:assertFail(msg = "Error while appending a file: " + response.message());
    } else {
        log:printInfo("Executed `append` operation");
    }

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>clientEp)->get(filePath);
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchStreamContent(str, "File contentAppend content"),
            msg = "Found unexpected content from `get` operation after `append` operation");
        io:Error? closeResult = str.close();
        if closeResult is io:Error {
            test:assertFail(msg = "Error while closing stream in `append` operation." + closeResult.message());
        }
    } else {
        test:assertFail(msg = "Found unexpected response type" + str.message());
    }
}

@test:Config {
    dependsOn: [testAppendContent]
}
public function testPutFileContent() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);

    Error? response = (<Client>clientEp)->put(newFilePath, bStream);
    if response is Error {
        test:assertFail(msg = "Found unexpected response type from `put` operation" + response.message());
    }
    log:printInfo("Executed `put` operation");

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>clientEp)->get(newFilePath);
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchStreamContent(str, "Put content"),
            msg = "Found unexpected content from `get` operation after `put` operation");
        io:Error? closeResult = str.close();
        if closeResult is io:Error {
            test:assertFail(msg = "Error while closing stream in `get` operation." + closeResult.message());
        }
    } else {
        test:assertFail(msg = "Found unexpected response type" + str.message());
    }
}

@test:Config {
    dependsOn: [testPutFileContent]
}
public function testPutCompressedFileContent() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);

    Error? response = (<Client>clientEp)->put("/home/in/test3.txt", bStream, compressionType = ZIP);
    if response is Error {
        test:assertFail(msg = "Found unexpected response type from compressed `put` operation" + response.message());
    }
    log:printInfo("Executed compressed `put` operation");

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>clientEp)->get("/home/in/test3.zip");
    if str is Error {
        test:assertFail(msg = "Error occurred during compressed `put` operation" + str.message());
    }
}

@test:Config {dependsOn: [testPutFileContent]}
function testPutBytes() returns error? {
    byte[] content = "hello-bytes".toBytes();
    string path = "/home/in/put-bytes.txt";

    check (<Client>clientEp)->putBytes(path, content);
    byte[] got = check (<Client>clientEp)->getBytes(path);

    test:assertEquals(got.length(), content.length(), msg = "Byte length mismatch");
    foreach int i in 0 ..< content.length() {
        test:assertEquals(got[i], content[i], msg = "Byte content mismatch at index " + i.toString());
    }
}

@test:Config {dependsOn: [testPutFileContent]}
function testGetBytesAsStream() returns error? {
    byte[] content = "hello-bytes".toBytes();
    string path = "/home/in/put-bytes.txt";

    check (<Client>clientEp)->putBytes(path, content);
    stream<byte[], error?> got = check (<Client>clientEp)->getBytesAsStream(path);
    byte[] accumulatedBytes = [];
    check from byte[] byteChunk in got
        do {
            accumulatedBytes.push(...byteChunk);
        };

    test:assertEquals(accumulatedBytes.length(), content.length(), msg = "Byte length mismatch");
    foreach int i in 0 ..< content.length() {
        test:assertEquals(accumulatedBytes[i], content[i], msg = "Byte content mismatch at index " + i.toString());
    }
}

@test:Config {dependsOn: [testPutFileContent]}
function testPutJson() returns error? {
    json j = {name: "wso2", count: 2, ok: true};
    string path = "/home/in/put.json";

    check (<Client>clientEp)->putJson(path, j);
    json got = check (<Client>clientEp)->getJson(path);

    test:assertEquals(got, j, msg = "JSON content mismatch");
}

@test:Config {dependsOn: [testPutFileContent]}
function testPutXml() returns error? {
    xml x = xml `<root><item k="v">42</item></root>`;
    string path = "/home/in/put.xml";

    check (<Client>clientEp)->putXml(path, x);
    xml got = check (<Client>clientEp)->getXml(path);

    // Compare string representations for stability
    test:assertEquals(got.toString(), x.toString(), msg = "XML content mismatch");
}

@test:Config {dependsOn: [testPutFileContent]}
function testPutXmlFailure() returns error? {
    string txt = "hello text content";
    string path = "/home/in/putXml.txt";

    check (<Client>clientEp)->putText(path, txt);
    xml|Error got = (<Client>clientEp)->getXml(path);

    // Compare string representations for stability
    test:assertTrue(got is Error, msg = "XML content binding should have failed for non-XML content");
}

@test:Config {dependsOn: [testPutFileContent]}
function testPutText() returns error? {
    string txt = "hello text content";
    string path = "/home/in/put.txt";

    check (<Client>clientEp)->putText(path, txt);
    string got = check (<Client>clientEp)->getText(path);

    test:assertEquals(got, txt, msg = "Text content mismatch");
}

@test:Config {dependsOn: [testPutFileContent]}
public function testPutCsvStringAndReadAllAndStream() returns error? {
    string csvPath = "/home/in/test-csv-string.csv";
    string[][] csvData = [
        ["id", "name", "email", "age", "registered_date", "active"],
        ["1", "Alice Smith", "alice@example.com", "25", "2024-01-15", "true"],
        ["2", "Bob Johnson", "bob@example.com", "30", "2024-02-20", "false"],
        ["3", "Carol White", "carol@example.com", "28", "2024-03-10", "true"]
    ];

    Error? resp = (<Client>clientEp)->putCsv(csvPath, csvData);
    if resp is Error {
        test:assertFail(msg = "Error while putting CSV string: " + resp.message());
    } else {
        log:printInfo("Executed `put` operation for CSV string");
    }

    string[][] gotAll = check (<Client>clientEp)->getCsv(csvPath);
    test:assertEquals(gotAll, csvData.slice(1), msg = "CSV text mismatch when reading all at once");

    stream<string[], error?> str = check (<Client>clientEp)->getCsvAsStream(csvPath);
    string[][] actual = [];
    check from string[] row in str
        do {
            actual.push(row);
        };
    test:assertEquals(actual, csvData.slice(1), msg = "CSV stream content mismatch when reading as stream");
}

@test:Config {
    dependsOn: [testPutCsvStringAndReadAllAndStream]
}
public function testPutCsvFromRecordsAndReadAllAndStream() returns error? {
    string csvPath = "/home/in/test-csv-records.csv";

    record {|string name; int age;|}[] records = [
        {name: "Charlie", age: 22},
        {name: "Dana", age: 28},
        {name: "Eve", age: 35}
    ];

    Error? resp = (<Client>clientEp)->putCsv(csvPath, records);
    if resp is Error {
        test:assertFail(msg = "Error while putting CSV string: " + resp.message());
    } else {
        log:printInfo("Executed `put` operation for CSV string");
    }

    record {|string name; int age;|}[] gotAll = check (<Client>clientEp)->getCsv(csvPath);
    test:assertEquals(gotAll, records, msg = "CSV text mismatch when reading all at once");

    stream<record {|string name; int age;|}, error?> str = check (<Client>clientEp)->getCsvAsStream(csvPath);
    record {|string name; int age;|}[] actual = [];
    check from record {|string name; int age;|} row in str
        do {
            actual.push(row);
        };
    test:assertEquals(actual, records, msg = "CSV stream content mismatch when reading as stream");
}

@test:Config {
    dependsOn: [testPutCsvFromRecordsAndReadAllAndStream]
}
public function testStreamClose() returns error? {
    string csvPath = "/home/in/test-csv-records.csv";
    stream<record {|string name; int age;|}, error?> str = check (<Client>clientEp)->getCsvAsStream(csvPath);
    check str.close();

    stream<string[], error?> str1 = check (<Client>clientEp)->getCsvAsStream(csvPath);
    check str1.close();

    string path = "/home/in/put-bytes.txt";
    stream<byte[], error?> got = check (<Client>clientEp)->getBytesAsStream(path);
    check got.close();
}

@test:Config {}
function testFtpUserDirIsRootTrue() returns error? {
    stream<byte[] & readonly, io:Error?>|Error res = (<Client>ftpUserHomeRootClientEp)->get("test1.txt");
    if res is Error {
        test:assertFail("FTP get failed with userDirIsRoot=true: " + res.message());
    }
    
    stream<byte[] & readonly, io:Error?> str = res;
    test:assertTrue(check matchStreamContent(str, "File content"), "Expected file content not found when userDirIsRoot=true");

    io:Error? closeErr = str.close();
    if closeErr is io:Error {
        test:assertFail("Error closing stream: " + closeErr.message());
    }
}

@test:Config {
    dependsOn: [testListFiles]
}
public function testPutRelativePath_userDirIsRootTrue() returns error? {
    Error? putRes = (<Client>ftpUserHomeRootClientEp)->put(relativePath, "hello-jailed-rel");
    if putRes is Error {
        test:assertFail("PUT(relative, no slash) failed on userDirIsRoot=true: " + putRes.message());
    }

    stream<byte[] & readonly, io:Error?>|Error getRes = (<Client>ftpUserHomeRootClientEp)->get(relativePath);
    if getRes is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchStreamContent(getRes, "hello-jailed-rel"),
            msg = "Unexpected content from GET(relative) after PUT on userDirIsRoot=true");
        io:Error? closeErr = getRes.close();
        if closeErr is io:Error {
            test:assertFail("Error closing relative GET stream: " + closeErr.message());
        }
    } else {
        test:assertFail("GET(relative) failed on userDirIsRoot=true: " + getRes.message());
    }

    Error? delRes = (<Client>ftpUserHomeRootClientEp)->delete(relativePath);
    if delRes is Error {
        log:printWarn("Cleanup delete failed for " + relativePath + ": " + delRes.message());
    }
}

@test:Config {
    dependsOn: [testListFiles]
}
public function testPutRelativePathWithSlash_userDirIsRootTrue() returns error? {
    Error? putRes = (<Client>ftpUserHomeRootClientEp)->put(relativePathWithSlash, "hello-jailed-rel-with-slash");
    if putRes is Error {
        test:assertFail("PUT(relative, with slash) failed on userDirIsRoot=true: " + putRes.message());
    }

    stream<byte[] & readonly, io:Error?>|Error getRes = (<Client>ftpUserHomeRootClientEp)->get(relativePathWithSlash);
    if getRes is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchStreamContent(getRes, "hello-jailed-rel-with-slash"),
            msg = "Unexpected content from GET(relative) after PUT on userDirIsRoot=true");
        io:Error? closeErr = getRes.close();
        if closeErr is io:Error {
            test:assertFail("Error closing relative GET stream: " + closeErr.message());
        }
    } else {
        test:assertFail("GET(relative) failed on userDirIsRoot=true: " + getRes.message());
    }

    Error? delRes = (<Client>ftpUserHomeRootClientEp)->delete(relativePathWithSlash);
    if delRes is Error {
        log:printWarn("Cleanup delete failed for " + relativePathWithSlash + ": " + delRes.message());
    }
}

@test:Config { dependsOn: [testListFiles] }
public function testPutAbsoluteDoubleSlash_userDirIsRootFalse() returns error? {
    Error? putRes = (<Client>clientEp)->put(absPath, "hello-abs-double-slash");
    if putRes is Error { test:assertFail("PUT(//absolute) failed: " + putRes.message()); }
    stream<byte[] & readonly, io:Error?>|Error getRes = (<Client>clientEp)->get(absPath);
    if getRes is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchStreamContent(getRes, "hello-abs-double-slash"));
        check getRes.close();
    } else { test:assertFail("GET(//absolute) failed: " + getRes.message()); }
    check (<Client>clientEp)->delete(absPath);
}

@test:Config { dependsOn: [testListFiles] }
public function testSftpUserDirIsRootTrue_RelativePutGet() returns error? {
    stream<io:Block, io:Error?> bStream = ["hello-sftp-rel".toBytes().cloneReadOnly()].toStream();
    Error? putRes = (<Client>sftpClientUserDirRootEp)->put("sftp-rel.txt", bStream);
    if putRes is Error { test:assertFail("SFTP relative PUT failed: " + putRes.message()); }
    stream<byte[] & readonly, io:Error?>|Error getRes = (<Client>sftpClientUserDirRootEp)->get("sftp-rel.txt");
    if getRes is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchStreamContent(getRes, "hello-sftp-rel"));
        check getRes.close();
    } else { test:assertFail("SFTP relative GET failed: " + getRes.message()); }
    check (<Client>sftpClientUserDirRootEp)->delete("sftp-rel.txt");
}

@test:Config {
    dependsOn: [testPutCompressedFileContent]
}
public function testPutLargeFileContent() returns error? {

    byte[] firstByteArray = [];
    int i = 0;
    while i < 16390 {
        firstByteArray[i] = 65;
        i = i + 1;
    }
    string sendString1 = check string:fromBytes(firstByteArray);

    (byte[])[] & readonly bList = [
        firstByteArray.cloneReadOnly(),
        "123456".toBytes().cloneReadOnly(),
        "end.".toBytes().cloneReadOnly()
    ];
    stream<byte[] & readonly, io:Error?> bStream = bList.toStream();
    Error? response = (<Client>clientEp)->put(newFilePath, bStream);
    if response is Error {
        test:assertFail(msg = "Found unexpected response type from `put` operation");
    }
    log:printInfo("Executed `put` operation for large files");

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>clientEp)->get(newFilePath);
    if str is stream<byte[] & readonly, io:Error?> {
        string expectedString = sendString1 + "123456" + "end.";
        test:assertTrue(check matchStreamContent(str, expectedString),
            msg = "Found unexpected content from `get` operation after `put` operation with large chunks");
        io:Error? closeResult = str.close();
        if closeResult is io:Error {
            test:assertFail(msg = "Error while closing stream in `get` operation." + closeResult.message());
        }
    } else {
        test:assertFail(msg = "Found unexpected response type" + str.message());
    }
}

@test:Config { dependsOn: [testPutLargeFileContent] }
function testPutBytesAsStreamNew() returns error? {
    string path = "/home/in/put-bytes-stream.txt";
    (byte[])[] & readonly chunks = [
        "hello-".toBytes().cloneReadOnly(),
        "world".toBytes().cloneReadOnly()
    ];
    stream<byte[] & readonly, io:Error?> s = chunks.toStream();

    Error? resp = (<Client>clientEp)->putBytesAsStream(path, s);
    if resp is Error {
        test:assertFail("putBytesAsStream failed: " + resp.message());
    }

    string got = check (<Client>clientEp)->getText(path);
    test:assertEquals(got, "hello-world", msg = "putBytesAsStream content mismatch");
}

@test:Config {dependsOn: [testPutBytesAsStreamNew]}
function testPutCsvAsStreamWithStringRows() returns error? {
    string csvPath = "/home/in/csv-stream-rows.csv";
    string[][] rows = [
        ["id", "name"],
        ["1", "A"],
        ["2", "B"]
    ];
    stream<string[], error?> s = rows.toStream();

    Error? resp = (<Client>clientEp)->putCsvAsStream(csvPath, s);
    if resp is Error {
        test:assertFail("putCsvAsStream (string[]) failed: " + resp.message());
    }

    // getCsv should return only data rows when targetType is string[][]
    string[][] gotAll = check (<Client>clientEp)->getCsv(csvPath);
    test:assertEquals(gotAll, rows.slice(1), msg = "CSV stream rows mismatch (get all)");

    stream<string[], error?> str = check (<Client>clientEp)->getCsvAsStream(csvPath);
    string[][] actual = [];
    check from string[] row in str
        do {
            actual.push(row);
        };
    test:assertEquals(actual, rows.slice(1), msg = "CSV stream rows mismatch (get stream)");
}

type Rec record {|
    string name;
    int age;
|};

type PersonStrict record {|
    string name;
    int age;
|};

type PersonLax record {|
    string name;
    int? age;
|};

@test:Config {dependsOn: [testPutCsvAsStreamWithStringRows]}
function testPutCsvAsStreamWithRecords() returns error? {
    string csvPath = "/home/in/csv-stream-records.csv";
    Rec[] records = [
        {name: "Charlie", age: 22},
        {name: "Dana", age: 28}
    ];

    stream<Rec, error?> s = records.toStream();
    Error? resp = (<Client>clientEp)->putCsvAsStream(csvPath, s);
    if resp is Error {
        test:assertFail("putCsvAsStream (records) failed: " + resp.message());
    }

    Rec[] gotAll = check (<Client>clientEp)->getCsv(csvPath);
    test:assertEquals(gotAll, records, msg = "CSV record stream mismatch (get all)");

    stream<Rec, error?> str = check (<Client>clientEp)->getCsvAsStream(csvPath);
    Rec[] actual = [];
    check from Rec row in str
        do {
            actual.push(row);
        };
    test:assertEquals(actual, records, msg = "CSV record stream mismatch (get stream)");
}

@test:Config {dependsOn: [testPutCsvAsStreamWithRecords]}
function testGetBytesAndTextNonExistent() returns error? {
    string missing = "/home/in/does-not-exist.txt";
    var r1 = (<Client>clientEp)->getBytes(missing);
    test:assertTrue(r1 is Error, msg = "getBytes should error for missing file");

    var r2 = (<Client>clientEp)->getText(missing);
    test:assertTrue(r2 is Error, msg = "getText should error for missing file");
}

// Strict vs lax data binding for JSON
@test:Config {dependsOn: [testGetBytesAndTextNonExistent]}
function testJsonTypedBinding_strict_and_lax() returns error? {
    string path = "/home/in/json-typed-projection.json";
    json content = {name: "Alice"}; // missing age
    check (<Client>clientEp)->putJson(path, content);

    PersonStrict|Error strictRes = (<Client>clientEp)->getJson(path);
    test:assertTrue(strictRes is Error, msg = "Strict JSON binding should fail when required field absent");

    PersonLax laxVal = check (<Client>clientEpLaxDataBinding)->getJson(path);
    test:assertEquals(laxVal.name, "Alice");
    test:assertEquals(laxVal.age is (), true, msg = "Lax binding should map absent field to nil");
}

type XPersonStrict record {|
    string name;
    int age;
|};

type XPersonLax record {|
    string name;
    int age;
|};

// Strict vs lax data binding for XML
@test:Config {dependsOn: [testJsonTypedBinding_strict_and_lax]}
function testXmlTypedBinding_strict_and_lax() returns error? {
    string path = "/home/in/xml-typed-projection.xml";
    xml x = xml `<person><name>Alice</name><age>32</age><address>12132131</address></person>`;
    check (<Client>clientEp)->putXml(path, x);

    var strictRes = (<Client>clientEp)->getXml(path, XPersonStrict);
    test:assertTrue(strictRes is Error, msg = "Strict XML binding should fail when required field absent");

    XPersonLax laxVal = check (<Client>clientEpLaxDataBinding)->getXml(path, XPersonLax);
    test:assertEquals(laxVal.name, "Alice");
}

type CsvPersonStrict record {|
    string name;
    int age;
|};

type CsvPersonLax record {|
    string name;
|};

// Strict vs lax data binding for CSV
@test:Config {dependsOn: [testXmlTypedBinding_strict_and_lax]}
function testCsvTypedBinding_strict_and_lax() returns error? {
    string csvPath = "/home/in/csv-typed-projection.csv";
    // Write CSV with missing age field for second row
    string[][] csvData = [
        ["name", "age"],
        ["Alice", "25"],
        ["Bob", ""]  // Missing age value
    ];
    check (<Client>clientEp)->putCsv(csvPath, csvData, OVERWRITE);

    // Strict binding should fail when required field is absent
    CsvPersonStrict[]|Error strictRes = (<Client>clientEp)->getCsv(csvPath);
    test:assertTrue(strictRes is Error, msg = "Strict CSV binding should fail when required field absent");

    // Lax binding should succeed with nil for missing field
    CsvPersonLax[] laxVal = check (<Client>clientEpLaxDataBinding)->getCsv(csvPath);
    test:assertEquals(laxVal.length(), 2, msg = "Should have 2 records");
    test:assertEquals(laxVal[0].name, "Alice");
    test:assertEquals(laxVal[1].name, "Bob");
}

// Strict vs lax data binding for CSV with streaming
@test:Config {dependsOn: [testCsvTypedBinding_strict_and_lax]}
function testCsvStreamTypedBinding_strict_and_lax() returns error? {
    string csvPath = "/home/in/csv-stream-typed-projection.csv";
    // Write CSV with missing age field
    string[][] csvData = [
        ["name", "age"],
        ["Charlie", "30"],
        ["Diana", ""]  // Missing age
    ];
    check (<Client>clientEp)->putCsv(csvPath, csvData, OVERWRITE);

    // Strict streaming should fail
    stream<CsvPersonStrict, error?>|Error strictStreamRes = (<Client>clientEp)->getCsvAsStream(csvPath);
    if strictStreamRes is stream<CsvPersonStrict, error?> {
        // Try to consume the stream - should error when hitting the row with missing age
        CsvPersonStrict[]|error consumed = from CsvPersonStrict row in strictStreamRes
            select row;
        test:assertTrue(consumed is error, msg = "Strict CSV stream should error on missing required field");
    } else {
        // Also acceptable if the stream creation itself fails
        test:assertTrue(true, msg = "Strict CSV stream failed at creation, which is acceptable");
    }

    // Lax streaming should succeed
    stream<CsvPersonLax, error?> laxStream = check (<Client>clientEpLaxDataBinding)->getCsvAsStream(csvPath);
    CsvPersonLax[] laxRecords = [];
    check from CsvPersonLax row in laxStream
        do {
            laxRecords.push(row);
        };
    
    test:assertEquals(laxRecords.length(), 2, msg = "Should have 2 records in lax stream");
    test:assertEquals(laxRecords[0].name, "Charlie");
    test:assertEquals(laxRecords[1].name, "Diana");
}

@test:Config {dependsOn: [testCsvStreamTypedBinding_strict_and_lax]}
function testPutTextWithAppendOption() returns error? {
    string path = "/home/in/append-option.txt";
    check (<Client>clientEp)->putText(path, "Hello", OVERWRITE);
    check (<Client>clientEp)->putText(path, " + world", APPEND);
    string got = check (<Client>clientEp)->getText(path);
    test:assertEquals(got, "Hello + world", msg = "APPEND option should append content");
}

@test:Config {dependsOn: [testPutTextWithAppendOption]}
function testPutCsvWithAppendOption() returns error? {
    string path = "/home/in/csv-append.csv";
    string[][] first = [
        ["id", "name"],
        ["1", "Alpha"]
    ];
    Error? r1 = (<Client>clientEp)->putCsv(path, first, OVERWRITE);
    if r1 is Error {
        test:assertFail("putCsv overwrite failed: " + r1.message());
    }

    // Append data rows (no header)
    string[][] more = [["2", "Beta"], ["3", "Gamma"]];
    Error? r2 = (<Client>clientEp)->putCsv(path, more, APPEND);
    if r2 is Error {
        test:assertFail("putCsv append failed: " + r2.message());
    }

    string[][] gotAll = check (<Client>clientEp)->getCsv(path);
    test:assertEquals(gotAll, [["1", "Alpha"], ["2", "Beta"], ["3", "Gamma"]],
            msg = "CSV append should not duplicate header and should add rows");
}

isolated function matchStreamContent(stream<byte[] & readonly, io:Error?> binaryStream, string matchedString) returns boolean|error {
    string fullContent = "";
    string tempContent = "";
    int maxLoopCount = 100000;
    while maxLoopCount > 0 {
        record {|byte[] value;|}|io:Error? binaryArray = binaryStream.next();
        if binaryArray is io:Error {
            break;
        } else if binaryArray is () {
            break;
        } else {
            tempContent = check strings:fromBytes(binaryArray.value);
            fullContent = fullContent + tempContent;
            maxLoopCount -= 1;
        }
    }
    return matchedString == fullContent;
}

@test:Config {
    dependsOn: [testPutLargeFileContent]
}
public function testPutTextContent() returns error? {
    string textToPut = "Sample text content";
    Error? response = (<Client>clientEp)->put(filePath, textToPut);
    if response is Error {
        test:assertFail(msg = "Found unexpected response type from `put` operation on text content" + response.message());
    } else {
        log:printInfo("Executed `put` operation on text");
    }

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>clientEp)->get(filePath);
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchStreamContent(str, "Sample text content"),
            msg = "Found unexpected content from `get` operation after `put` operation on text");
        io:Error? closeResult = str.close();
        if closeResult is io:Error {
            test:assertFail(msg = "Error while closing stream in `get` operation." + closeResult.message());
        }
    } else {
        test:assertFail(msg = "Found unexpected response type" + str.message());
    }
}

@test:Config {
    dependsOn: [testPutTextContent]
}
public function testPutJsonContent() returns error? {
    json jsonToPut = {name: "Anne", age: 20};
    Error? response = (<Client>clientEp)->put(filePath, jsonToPut);
    if response is Error {
        test:assertFail(msg = "Found unexpected response type from `put` operation on JSON content" + response.message());
    } else {
        log:printInfo("Executed `put` operation on JSON");
    }

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>clientEp)->get(filePath);
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchStreamContent(str, "{\"name\":\"Anne\", \"age\":20}"),
            msg = "Found unexpected content from `get` operation after `put` operation on JSON");
        io:Error? closeResult = str.close();
        if closeResult is io:Error {
            test:assertFail(msg = "Error while closing stream in `get` operation." + closeResult.message());
        }
    } else {
        test:assertFail(msg = "Found unexpected response type" + str.message());
    }
}

@test:Config {
    dependsOn: [testPutJsonContent]
}
public function testPutXMLContent() returns error? {
    xml xmlToPut = xml `<note><heading>Memo</heading><body>Memo content</body></note>`;
    Error? response = (<Client>clientEp)->put(filePath, xmlToPut);
    if response is Error {
        test:assertFail(msg = "Found unexpected response type from `put` operation on XML content" + response.message());
    } else {
        log:printInfo("Executed `put` operation on XML");
    }

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>clientEp)->get(filePath);
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertTrue(check matchStreamContent(str, "<note><heading>Memo</heading><body>Memo content</body></note>"),
            msg = "Found unexpected content from `get` operation after `put` operation on XML");
        io:Error? closeResult = str.close();
        if closeResult is io:Error {
            test:assertFail(msg = "Error while closing stream in `get` operation." + closeResult.message());
        }
    } else {
        test:assertFail(msg = "Found unexpected response type" + str.message());
    }
}

@test:Config {
    dependsOn: [testPutXMLContent]
}
public function testIsDirectory() {
    boolean|Error response1 = (<Client>clientEp)->isDirectory("/home/in");
    log:printInfo("Executed `isDirectory` operation on a directory");
    if response1 is boolean {
        log:printInfo("Is directory: " + response1.toString());
        test:assertEquals(response1, true,
            msg = "A directory is not correctly recognized with `isDirectory` operation");
    } else {
        test:assertFail(msg = "Error while invoking `isDirectory` operation" + response1.message());
    }

    boolean|Error response2 = (<Client>clientEp)->isDirectory(filePath);
    log:printInfo("Executed `isDirectory` operation on a file");
    if response2 is boolean {
        log:printInfo("Is directory: " + response2.toString());
        test:assertEquals(response2, false,
            msg = "A file is not correctly recognized with `isDirectory` operation");
    } else {
        test:assertFail(msg = "Error while invoking `isDirectory` operation" + response2.message());
    }
}

@test:Config {
    dependsOn: [testIsDirectory]
}
public function testCreateDirectory() {
    Error? response1 = (<Client>clientEp)->mkdir("/home/in/out");
    if response1 is Error {
        test:assertFail(msg = "Error while creating a directory" + response1.message());
    } else {
        log:printInfo("Executed `mkdir` operation");
    }

    boolean|Error response2 = (<Client>clientEp)->isDirectory("/home/in/out");
    log:printInfo("Executed `isDirectory` operation after creating a directory");
    if response2 is boolean {
        log:printInfo("Is directory: " + response2.toString());
        test:assertEquals(response2, true, msg = "Directory was not created");
    } else {
        test:assertFail(msg = "Error while invoking `isDirectory` operation" + response2.message());
    }
}

@test:Config {
    dependsOn: [testCreateDirectory]
}
public function testRenameDirectory() {
    string existingName = "/home/in/out";
    string newName = "/home/in/test";
    Error? response1 = (<Client>clientEp)->rename(existingName, newName);
    if response1 is Error {
        test:assertFail(msg = "Error while invoking `rename` operation" + response1.message());
    } else {
        log:printInfo("Executed `rename` operation");
    }

    boolean|Error response2 = (<Client>clientEp)->isDirectory(existingName);
    log:printInfo("Executed `isDirectory` operation on original directory after renaming a directory");
    if response2 is Error {
        test:assertEquals(response2.message(), "/home/in/out does not exists to check if it is a directory.",
            msg = "Incorrect error message for non-existing file/directory at `isDirectory` operation");
    } else {
        test:assertFail("Error not created while invoking `isDirectory` operation after `rename` operation");
    }

    boolean|Error response3 = (<Client>clientEp)->isDirectory(newName);
    log:printInfo("Executed `isDirectory` operation on renamed directory after renaming a directory");
    if response3 is boolean {
        log:printInfo("Existance of renamed directory: " + response3.toString());
        test:assertEquals(response3, true, msg = "New directory name was not created during `rename` operation");
    } else {
        test:assertFail(msg = "Error while invoking `isDirectory` operation after `rename` operation" + response3.message());
    }

}

@test:Config {
    dependsOn: [testRenameDirectory]
}
public function testMoveFile() {
    string sourcePath = "/home/in/test_move_source.txt";
    string destinationPath = "/home/in/test_move_dest.txt";
    
    // First, create a test file
    string testContent = "Test content for move operation";
    Error? putResponse = (<Client>clientEp)->put(sourcePath, testContent);
    if putResponse is Error {
        test:assertFail(msg = "Error while creating test file for `move` operation: " + putResponse.message());
    }
    
    // Now move the file
    Error? moveResponse = (<Client>clientEp)->move(sourcePath, destinationPath);
    if moveResponse is Error {
        test:assertFail(msg = "Error while invoking `move` operation: " + moveResponse.message());
    } else {
        log:printInfo("Executed `move` operation");
    }

    // Verify source file no longer exists
    stream<byte[] & readonly, io:Error?>|Error sourceCheck = (<Client>clientEp)->get(sourcePath);
    if sourceCheck is Error {
        log:printInfo("Source file correctly removed after move: " + sourcePath);
    } else {
        test:assertFail(msg = "Source file still exists after `move` operation");
    }

    // Verify destination file exists and has correct content
    stream<byte[] & readonly, io:Error?>|Error destCheck = (<Client>clientEp)->get(destinationPath);
    if destCheck is stream<byte[] & readonly, io:Error?> {
        record {|byte[] & readonly value;|}|io:Error? nextResult = destCheck.next();
        if nextResult is record {|byte[] & readonly value;|} {
            string|error readContent = strings:fromBytes(nextResult.value);
            if readContent is string {
                test:assertEquals(readContent, testContent, msg = "Content mismatch after `move` operation");
                log:printInfo("Destination file correctly created with expected content");
            } else {
                test:assertFail(msg = "Error reading moved file content");
            }
        } else {
            test:assertFail(msg = "Error reading moved file stream");
        }
    } else {
        test:assertFail(msg = "Destination file not found after `move` operation: " + destCheck.message());
    }
    
    // Cleanup
    Error? deleteResponse = (<Client>clientEp)->delete(destinationPath);
    if deleteResponse is Error {
        log:printWarn("Failed to cleanup moved file: " + deleteResponse.message());
    }
}

@test:Config {
    dependsOn: [testMoveFile]
}
public function testCopyFile() {
    string sourcePath = "/home/in/test1.txt";
    string destinationPath = "/home/in/copied.txt";
    
    // Verify source file exists first
    boolean|Error sourceExists = (<Client>clientEp)->exists(sourcePath);
    if sourceExists is boolean {
        test:assertTrue(sourceExists, msg = "Source file should exist before copy");
    } else {
        test:assertFail(msg = "Error checking source file existence: " + sourceExists.message());
    }
    
    // Copy the file
    Error? copyResponse = (<Client>clientEp)->copy(sourcePath, destinationPath);
    if copyResponse is Error {
        test:assertFail(msg = "Error while invoking `copy` operation: " + copyResponse.message());
    } else {
        log:printInfo("Executed `copy` operation");
    }

    // Verify source file still exists
    boolean|Error sourceStillExists = (<Client>clientEp)->exists(sourcePath);
    if sourceStillExists is boolean {
        test:assertTrue(sourceStillExists, msg = "Source file should still exist after copy");
    } else {
        test:assertFail(msg = "Error checking source file existence after copy: " + sourceStillExists.message());
    }

    // Verify destination file exists
    boolean|Error destExists = (<Client>clientEp)->exists(destinationPath);
    if destExists is boolean {
        test:assertTrue(destExists, msg = "Destination file should exist after copy");
        log:printInfo("Destination file correctly created after copy");
    } else {
        test:assertFail(msg = "Error checking destination file existence: " + destExists.message());
    }
    
    // Verify destination file has content
    stream<byte[] & readonly, io:Error?>|Error destCheck = (<Client>clientEp)->get(destinationPath);
    if destCheck is stream<byte[] & readonly, io:Error?> {
        record {|byte[] & readonly value;|}|io:Error? nextResult = destCheck.next();
        if nextResult is record {|byte[] & readonly value;|} {
            string|error readContent = strings:fromBytes(nextResult.value);
            if readContent is string {
                test:assertTrue(readContent.length() > 0, msg = "Copied file should have content");
                log:printInfo("Destination file has content after copy");
            } else {
                test:assertFail(msg = "Error reading copied file content");
            }
        } else {
            test:assertFail(msg = "Error reading copied file stream");
        }
    } else {
        test:assertFail(msg = "Destination file not found after `copy` operation: " + destCheck.message());
    }
    
    // Cleanup
    Error? deleteResponse = (<Client>clientEp)->delete(destinationPath);
    if deleteResponse is Error {
        log:printWarn("Failed to cleanup copied file: " + deleteResponse.message());
    }
}

@test:Config {
    dependsOn: [testCopyFile]
}
public function testExistsFile() {
    string existingPath = "/home/in/test1.txt";
    string nonExistingPath = "/home/in/nonexistent.txt";
    
    // Test with existing file
    boolean|Error existsResult = (<Client>clientEp)->exists(existingPath);
    if existsResult is boolean {
        test:assertTrue(existsResult, msg = "Exists should return true for existing file");
        log:printInfo("Executed `exists` operation for existing file - returned true");
    } else {
        test:assertFail(msg = "Error while invoking `exists` operation for existing file: " + existsResult.message());
    }
    
    // Test with non-existing file
    boolean|Error notExistsResult = (<Client>clientEp)->exists(nonExistingPath);
    if notExistsResult is boolean {
        test:assertFalse(notExistsResult, msg = "Exists should return false for non-existing file");
        log:printInfo("Executed `exists` operation for non-existing file - returned false");
    } else {
        test:assertFail(msg = "Error while invoking `exists` operation for non-existing file: " + notExistsResult.message());
    }
}

@test:Config {
    dependsOn: [testExistsFile]
}
public function testGetFileSize() {
    int|Error response = (<Client>clientEp)->size(filePath);
    log:printInfo("Executed `size` operation.");
    if response is int {
        log:printInfo("Size: " + response.toString());
        test:assertEquals(response, 61, msg = "File size is not given with `size` operation");
    } else {
        test:assertFail(msg = "Error while invoking the `size` operation" + response.message());
    }
}

@test:Config {
    dependsOn: [testGetFileSize]
}
public function testListFiles() {
    string[] resourceNames = [
        "cron",
        "test1.txt",
        "complexDirectory",
        "test",
        "advanced",
        "dependency",
        "folder1",
        "test3.zip",
        "childDirectory",
        "delete",
        "test2.txt",
        "test4.txt",
        "child_directory",
        "content-methods",
        "age",
        "test3.txt"
    ];
    int[] fileSizes = [0, 61, 0, 0, 0, 0, 0, 145, 0, 0, 16400, 9000, 0, 0, 0, 12];
    FileInfo[]|Error response = (<Client>clientEp)->list("/home/in");
    if response is FileInfo[] {
        log:printInfo("List of files/directories: ");
        int i = 0;
        foreach var fileInfo in response {
            log:printInfo(fileInfo.toString());
            test:assertEquals(fileInfo.path, "/home/in/" + resourceNames[i],
                msg = "File path is not matched during the `list` operation");
            test:assertTrue(fileInfo.lastModifiedTimestamp > 0,
                msg = "Last Modified Timestamp of the file is not correct during the `list` operation");
            test:assertEquals(fileInfo.size, fileSizes[i],
                msg = "File size is not matched during the `list` operation");
            i = i + 1;
        }
        log:printInfo("Executed `list` operation");
    } else {
        test:assertFail(msg = "Error while invoking the `list` operation" + response.message());
    }
}

@test:Config {
    dependsOn: [testListFiles]
}
public function testDeleteFile() returns error? {
    Error? response = (<Client>clientEp)->delete(filePath);
    if response is Error {
        test:assertFail(msg = "Error while invoking the `delete` operation" + response.message());
    } else {
        log:printInfo("Executed `delete` operation");
    }

    stream<byte[] & readonly, io:Error?>|Error str = (<Client>clientEp)->get(filePath);
    if str is stream<byte[] & readonly, io:Error?> {
        test:assertFalse(check matchStreamContent(str, "<note><heading>Memo</heading><body>Memo content</body></note>"),
            msg = "File was not deleted with `delete` operation");
        io:Error? closeResult = str.close();
        if closeResult is io:Error {
            test:assertFail(msg = "Error while closing stream in `get` operation." + closeResult.message());
        }
    } else {
        test:assertEquals(str.message(),
            "Failed to read file: ftp://wso2:***@127.0.0.1:21212/home/in/test1.txt not found",
            msg = "Correct error is not given when the file is deleted." + str.message());
    }
}

@test:Config {
    dependsOn: [testDeleteFile]
}
public function testRemoveDirectory() returns error? {
    return testGenericRmdir("/home/in/test");
}

@test:Config {
    dependsOn: [testRemoveDirectory]
}
public function testRemoveDirectoryWithSubdirectory() returns error? {
    return testGenericRmdir("/home/in/folder1");
}

@test:Config {
    dependsOn: [testRemoveDirectoryWithSubdirectory]
}
public function testRemoveDirectoryWithFiles() returns error? {
    return testGenericRmdir("/home/in/child_directory");
}

@test:Config {
    dependsOn: [testRemoveDirectoryWithFiles]
}
public function testRemoveComplexDirectory() returns error? {
    return testGenericRmdir("/home/in/complexDirectory");
}

function testGenericRmdir(string path) returns error? {
    boolean|Error response0 = (<Client>clientEp)->isDirectory(path);
    log:printInfo("Executed `isDirectory` operation before deleting a directory " + path);
    int retryCount = 0;
    while (response0 is Error || !response0) && retryCount < 10 {
        log:printInfo("Executed `isDirectory` operation before deleting a directory " + path);
        runtime:sleep(1);
        response0 = (<Client>clientEp)->isDirectory(path);
        retryCount += 1;
    }

    if retryCount >= 10 {
        test:assertFail(msg = "Error while invoking the `isDirectory` operation before invoking the `rmdir` operation");
    } else {
        Error? response1 = (<Client>clientEp)->rmdir(path);
        if response1 is Error {
            test:assertFail(msg = "Error while invoking the `rmdir` operation on " + path + ": " + response1.message());
        } else {
            log:printInfo("Executed `rmdir` operation on " + path);
        }
    }

    boolean|Error response2 = (<Client>clientEp)->isDirectory(path);
    log:printInfo("Executed `isDirectory` operation after deleting a directory " + path);

    int i = 0;
    while response2 is boolean && i < 10 {
        runtime:sleep(1);
        response2 = (<Client>clientEp)->isDirectory(path);
        log:printInfo("Executed `isDirectory` operation after deleting a directory " + path);
        i += 1;
    }
    if response2 is Error {
        test:assertEquals(response2.message(), path + " does not exists to check if it is a directory.",
            msg = "Incorrect error message for non-existing file/directory at `isDirectory` operation after `rmdir` operation");
    } else {
        // test:assertFail(msg = "Error not created while invoking `isDirectory` operation after `rmdir` operation on " + path );
    }
}

@test:Config {
    dependsOn: [testRemoveComplexDirectory]
}
public function testClientClose() returns error? {
    Client ftpClient = check new (config);
    Error? closeResult = ftpClient->close();
    test:assertEquals(closeResult, ());

    closeResult = ftpClient->close();
    test:assertEquals(closeResult, ());

    boolean|Error existsResult = ftpClient->exists(filePath);
    test:assertTrue(existsResult is Error);
    if existsResult is Error {
        test:assertEquals(existsResult.message(), "FTP client is closed");
    }
    stream<io:Block, io:Error?> byteStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    Error? putResult = ftpClient->put(newFilePath, byteStream);
    test:assertTrue(putResult is Error);
    if putResult is Error {
        test:assertEquals(putResult.message(), "FTP client is closed");
    }
}

@test:Config {
    dependsOn: [testClientClose]
}
public function testCloseThenPutApis() returns error? {
    Client ftpClient = check new (config);
    Error? closeResult = ftpClient->close();
    test:assertEquals(closeResult, ());

    Error? putBytes = ftpClient->putBytes("/home/in/after-close-putBytes.txt", "abc".toBytes());
    test:assertTrue(putBytes is Error);
    if putBytes is Error {
        test:assertEquals(putBytes.message(), "FTP client is closed");
    }

    Error? putText = ftpClient->putText("/home/in/after-close-putText.txt", "abc");
    test:assertTrue(putText is Error);
    if putText is Error {
        test:assertEquals(putText.message(), "FTP client is closed");
    }

    json jsonPayload = {"a": 1};
    Error? putJson = ftpClient->putJson("/home/in/after-close-putJson.json", jsonPayload);
    test:assertTrue(putJson is Error);
    if putJson is Error {
        test:assertEquals(putJson.message(), "FTP client is closed");
    }

    Error? putXml = ftpClient->putXml("/home/in/after-close-putXml.xml", xml `<a>1</a>`);
    test:assertTrue(putXml is Error);
    if putXml is Error {
        test:assertEquals(putXml.message(), "FTP client is closed");
    }

    Error? putCsv = ftpClient->putCsv("/home/in/after-close-putCsv.csv", [["id", "name"], ["1", "A"]]);
    test:assertTrue(putCsv is Error);
    if putCsv is Error {
        test:assertEquals(putCsv.message(), "FTP client is closed");
    }

    stream<byte[] & readonly, io:Error?> bytesStream = [
        "hello-".toBytes().cloneReadOnly(),
        "world".toBytes().cloneReadOnly()
    ].toStream();
    Error? putBytesAsStream = ftpClient->putBytesAsStream("/home/in/after-close-putBytesAsStream.txt", bytesStream);
    test:assertTrue(putBytesAsStream is Error);
    if putBytesAsStream is Error {
        test:assertEquals(putBytesAsStream.message(), "FTP client is closed");
    }

    stream<string[], error?> csvStream = [
        ["id", "name"],
        ["1", "A"]
    ].toStream();
    Error? putCsvAsStream = ftpClient->putCsvAsStream("/home/in/after-close-putCsvAsStream.csv", csvStream);
    test:assertTrue(putCsvAsStream is Error, msg = "Expected putCsvAsStream() to fail after close()");
}

@test:Config {
    dependsOn: [testCloseThenPutApis]
}
public function testCloseThenGetApis() returns error? {
    Client ftpClient = check new (config);
    Error? closeResult = ftpClient->close();
    test:assertEquals(closeResult, ());

    stream<byte[] & readonly, io:Error?>|Error getResult = ftpClient->get("/home/in/test1.txt");
    test:assertTrue(getResult is Error);
    if getResult is Error {
        test:assertEquals(getResult.message(), "FTP client is closed");
    }

    byte[]|Error getBytesResult = ftpClient->getBytes("/home/in/test1.txt");
    test:assertTrue(getBytesResult is Error);
    if getBytesResult is Error {
        test:assertEquals(getBytesResult.message(), "FTP client is closed");
    }

    string|Error getTextResult = ftpClient->getText("/home/in/test1.txt");
    test:assertTrue(getTextResult is Error);
    if getTextResult is Error {
        test:assertEquals(getTextResult.message(), "FTP client is closed");
    }

    json|Error getJsonResult = ftpClient->getJson("/home/in/test1.txt");
    test:assertTrue(getJsonResult is Error);
    if getJsonResult is Error {
        test:assertEquals(getJsonResult.message(), "FTP client is closed");
    }

    xml|Error getXmlResult = ftpClient->getXml("/home/in/test1.txt");
    test:assertTrue(getXmlResult is Error);
    if getXmlResult is Error {
        test:assertEquals(getXmlResult.message(), "FTP client is closed");
    }

    string[][]|Error getCsvResult = ftpClient->getCsv("/home/in/test1.txt");
    test:assertTrue(getCsvResult is Error);
    if getCsvResult is Error {
        test:assertEquals(getCsvResult.message(), "FTP client is closed");
    }

    stream<byte[], error?>|Error getBytesAsStreamResult = ftpClient->getBytesAsStream("/home/in/test1.txt");
    test:assertTrue(getBytesAsStreamResult is Error);
    if getBytesAsStreamResult is Error {
        test:assertEquals(getBytesAsStreamResult.message(), "FTP client is closed");
    }

    stream<string[], error?>|Error getCsvAsStreamResult = ftpClient->getCsvAsStream("/home/in/test1.txt");
    test:assertTrue(getCsvAsStreamResult is Error);
    if getCsvAsStreamResult is Error {
        test:assertEquals(getCsvAsStreamResult.message(), "FTP client is closed");
    }
}

@test:Config {
    dependsOn: [testCloseThenGetApis]
}
public function testCloseThenOtherApis() returns error? {
    Client ftpClient = check new (config);
    Error? closeResult = ftpClient->close();
    test:assertEquals(closeResult, ());

    Error? moveResult = ftpClient->move("/home/in/test1.txt", "/home/in/after-close-move.txt");
    test:assertTrue(moveResult is Error);
    if moveResult is Error {
        test:assertEquals(moveResult.message(), "FTP client is closed");
    }

    Error? copyResult = ftpClient->copy("/home/in/test1.txt", "/home/in/after-close-copy.txt");
    test:assertTrue(copyResult is Error);
    if copyResult is Error {
        test:assertEquals(copyResult.message(), "FTP client is closed");
    }

    Error? appendResult = ftpClient->append("/home/in/after-close-append.txt", "abc");
    test:assertTrue(appendResult is Error);
    if appendResult is Error {
        test:assertEquals(appendResult.message(), "FTP client is closed");
    }

    Error? mkdirResult = ftpClient->mkdir("/home/in/after-close-dir");
    test:assertTrue(mkdirResult is Error);
    if mkdirResult is Error {
        test:assertEquals(mkdirResult.message(), "FTP client is closed");
    }

    Error? rmdirResult = ftpClient->rmdir("/home/in/after-close-dir");
    test:assertTrue(rmdirResult is Error);
    if rmdirResult is Error {
        test:assertEquals(rmdirResult.message(), "FTP client is closed");
    }

    Error? renameResult = ftpClient->rename("/home/in/test1.txt", "/home/in/after-close-rename.txt");
    test:assertTrue(renameResult is Error);
    if renameResult is Error {
        test:assertEquals(renameResult.message(), "FTP client is closed");
    }

    boolean|Error isDirResult = ftpClient->isDirectory("/home/in");
    test:assertTrue(isDirResult is Error);
    if isDirResult is Error {
        test:assertEquals(isDirResult.message(), "FTP client is closed");
    }

    int|Error sizeResult = ftpClient->size("/home/in/test1.txt");
    test:assertTrue(sizeResult is Error);
    if sizeResult is Error {
        test:assertEquals(sizeResult.message(), "FTP client is closed");
    }

    FileInfo[]|Error listResult = ftpClient->list("/home/in");
    test:assertTrue(listResult is Error);
    if listResult is Error {
        test:assertEquals(listResult.message(), "FTP client is closed");
    }

    Error? deleteResult = ftpClient->delete("/home/in/test1.txt");
    test:assertTrue(deleteResult is Error);
    if deleteResult is Error {
        test:assertEquals(deleteResult.message(), "FTP client is closed");
    }
}

@test:Config {
    dependsOn: [testCloseThenOtherApis]
}
public function testCallerCloseClosesUnderlyingClient() returns error? {
    Client ftpClient = check new (config);
    Caller caller = new (ftpClient);

    Error? closeResult = caller->close();
    test:assertEquals(closeResult, ());

    boolean|Error existsResult = caller->exists("/home/in/test1.txt");
    test:assertTrue(existsResult is Error);
    if existsResult is Error {
        test:assertEquals(existsResult.message(), "FTP client is closed");
    }

    string|Error getTextResult = ftpClient->getText("/home/in/test1.txt");
    test:assertTrue(getTextResult is Error);
    if getTextResult is Error {
        test:assertEquals(getTextResult.message(), "FTP client is closed");
    }
}

@test:AfterSuite {}
public function cleanTestEnvironment() returns error? {
    check (<Listener>callerListener).gracefulStop();
    check (<Listener>remoteServerListener).gracefulStop();
    check (<Listener>anonymousRemoteServerListener).gracefulStop();
    check (<Listener>secureRemoteServerListener).gracefulStop();
}
