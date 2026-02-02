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

import ballerina/lang.runtime as runtime;
import ballerina/io;
import ballerina/test;

FileInfo[] fileList = [];
boolean fileGetContentCorrect = false;
int fileSize = 0;
boolean isDir = false;
string filename = "mutableWatchEvent.caller";
string addedFile = "";

// Flags and expected content placeholders for new typed Caller method tests
boolean putTextContentOk = false;
boolean putBytesContentOk = false;
boolean putJsonContentOk = false;
boolean putXmlContentOk = false;
boolean putCsvContentOk = false;
boolean putBytesAsStreamContentOk = false;
boolean putCsvAsStreamContentOk = false;
boolean getCsvContentOk = false;
boolean getBytesAsStreamContentOk = false;
boolean getCsvAsStreamContentOk = false;
boolean getBytesContentOk = false;
boolean getTextContentOk = false;
boolean getJsonContentOk = false;
boolean getXmlContentOk = false;
string[][] retrievedCsvData = [];
byte[] retrievedBytesStreamData = [];
string[][] retrievedCsvStreamData = [];
byte[] retrievedBytesData = [];
string retrievedTextData = "";
anydata retrievedJsonData = {};
xml retrievedXmlData = xml ``;

ListenerConfiguration callerListenerConfig = {
    protocol: SFTP,
    host: "127.0.0.1",
    auth: {
        credentials: {
           username: "wso2",
           password: "wso2123"
        },
        privateKey: {
           path: "tests/resources/sftp.private.key",
           password: "changeit"
        }
    },
    port: 21213,
    pollingInterval: 1,
    path: "/in",
    fileNamePattern: "(.*).caller"
};

Service callerService = service object {

    remote function onFileChange(WatchEvent & readonly event, Caller caller) returns error? {
        foreach FileInfo fileInfo in event.addedFiles {
            string addedFilepath = fileInfo.path;
            if addedFilepath.endsWith("/put.caller") {
                stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
                check caller->put("/out/put2.caller", bStream);
                check caller->rename("/in/put.caller", "/out/put.caller");
            } else if addedFilepath.endsWith("/append.caller") {
                stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(appendFilePath, 7);
                check caller->append("/in/append.caller", bStream);
                check caller->rename("/in/append.caller", "/out/append.caller");
            } else if addedFilepath.endsWith("/rename.caller") {
                check caller->rename("/in/rename.caller", "/out/rename.caller");
            } else if addedFilepath.endsWith("/delete.caller") {
                check caller->delete("/in/delete.caller");
            } else if addedFilepath.endsWith("/list.caller") {
                fileList = check caller->list("/in");
                check caller->rename("/in/list.caller", "/out/list.caller");
            } else if addedFilepath.endsWith("/get.caller") {
                stream<io:Block, io:Error?> fileStream = check caller->get("/in/get.caller");
                fileGetContentCorrect = check matchStreamContent(fileStream, "Put content");
                check caller->rename("/in/get.caller", "/out/get.caller");
            } else if addedFilepath.endsWith("/mkdir.caller") {
                check caller->mkdir("/out/callerDir");
                check caller->rename("/in/mkdir.caller", "/out/mkdir.caller");
            } else if addedFilepath.endsWith("/rmdir.caller") {
                check caller->rmdir("/out/callerDir");
                check caller->rename("/in/rmdir.caller", "/out/rmdir.caller");
            } else if addedFilepath.endsWith("/size.caller") {
                fileSize = check caller->size("/in/size.caller");
                check caller->rename("/in/size.caller", "/out/size.caller");
            } else if addedFilepath.endsWith("/isdirectory.caller") {
                isDir = check caller->isDirectory("/out/callerDir");
                check caller->rename("/in/isdirectory.caller", "/out/isdirectory.caller");
            } else if addedFilepath.endsWith("/puttext.caller") {
                // Write a text file using typed API
                check caller->putText("/out/puttext.result.caller", "Caller Text Content", OVERWRITE);
                putTextContentOk = true;
                check caller->rename("/in/puttext.caller", "/out/puttext.caller");
            } else if addedFilepath.endsWith("/puttext.append.caller") {
                // Append extra text content
                check caller->putText("/out/puttext.result.caller", " + Appended", APPEND);
                check caller->rename("/in/puttext.append.caller", "/out/puttext.append.caller");
            } else if addedFilepath.endsWith("/putbytes.caller") {
                // Write a bytes file using typed API
                byte[] b = "Caller Bytes Content".toBytes();
                check caller->putBytes("/out/putbytes.result.caller", b, OVERWRITE);
                putBytesContentOk = true;
                check caller->rename("/in/putbytes.caller", "/out/putbytes.caller");
            } else if addedFilepath.endsWith("/putbytes.append.caller") {
                // Append bytes content
                byte[] b2 = " + Appended".toBytes();
                check caller->putBytes("/out/putbytes.result.caller", b2, APPEND);
                check caller->rename("/in/putbytes.append.caller", "/out/putbytes.append.caller");
            } else if addedFilepath.endsWith("/putjson.caller") {
                // Write a json file using typed API
                json j = { name: "Alex", active: true, count: 42 };
                check caller->putJson("/out/putjson.result.caller", j, OVERWRITE);
                putJsonContentOk = true;
                check caller->rename("/in/putjson.caller", "/out/putjson.caller");
            } else if addedFilepath.endsWith("/putjson.append.caller") {
                // Append a second JSON object (will make raw file a concatenation; size growth used for validation)
                json j2 = { appended: true, version: 1 };
                check caller->putJson("/out/putjson.result.caller", j2, APPEND);
                check caller->rename("/in/putjson.append.caller", "/out/putjson.append.caller");
            } else if addedFilepath.endsWith("/putxml.caller") {
                // Write an xml file using typed API
                xml x = xml `<root><val>42</val><name>Alex</name></root>`;
                check caller->putXml("/out/putxml.result.caller", x, OVERWRITE);
                putXmlContentOk = true;
                check caller->rename("/in/putxml.caller", "/out/putxml.caller");
            } else if addedFilepath.endsWith("/putxml.append.caller") {
                // Append an xml fragment (raw concatenation; size growth used for validation)
                xml x2 = xml `<append><ok>true</ok></append>`;
                check caller->putXml("/out/putxml.result.caller", x2, APPEND);
                check caller->rename("/in/putxml.append.caller", "/out/putxml.append.caller");
            } else if addedFilepath.endsWith("/putcsv.caller") {
                // Write CSV using putCsv
                string[][] csvData = [["Name", "Age", "City"], ["Alice", "30", "NYC"], ["Bob", "25", "LA"]];
                check caller->putCsv("/out/putcsv.result.caller", csvData, OVERWRITE);
                putCsvContentOk = true;
                check caller->rename("/in/putcsv.caller", "/out/putcsv.caller");
            } else if addedFilepath.endsWith("/putcsv.append.caller") {
                // Append CSV rows
                string[][] csvAppend = [["Charlie", "35", "SF"]];
                check caller->putCsv("/out/putcsv.result.caller", csvAppend, APPEND);
                check caller->rename("/in/putcsv.append.caller", "/out/putcsv.append.caller");
            } else if addedFilepath.endsWith("/putbytesasstream.caller") {
                // Write bytes as stream
                byte[] data = "Streamed Bytes Content".toBytes();
                stream<byte[], error?> byteStream = [data].toStream();
                check caller->putBytesAsStream("/out/putbytesasstream.result.caller", byteStream, OVERWRITE);
                putBytesAsStreamContentOk = true;
                check caller->rename("/in/putbytesasstream.caller", "/out/putbytesasstream.caller");
            } else if addedFilepath.endsWith("/putbytesasstream.append.caller") {
                // Append bytes as stream
                byte[] data2 = " + Stream Appended".toBytes();
                stream<byte[], error?> byteStream2 = [data2].toStream();
                check caller->putBytesAsStream("/out/putbytesasstream.result.caller", byteStream2, APPEND);
                check caller->rename("/in/putbytesasstream.append.caller", "/out/putbytesasstream.append.caller");
            } else if addedFilepath.endsWith("/putcsvasstream.caller") {
                // Write CSV as stream
                string[][] csvRows = [["Product", "Price"], ["Widget", "10.50"], ["Gadget", "25.00"]];
                stream<string[], error?> csvStream = csvRows.toStream();
                check caller->putCsvAsStream("/out/putcsvasstream.result.caller", csvStream, OVERWRITE);
                putCsvAsStreamContentOk = true;
                check caller->rename("/in/putcsvasstream.caller", "/out/putcsvasstream.caller");
            } else if addedFilepath.endsWith("/putcsvasstream.append.caller") {
                // Append CSV rows as stream
                string[][] csvAppendRows = [["Doohickey", "15.75"]];
                stream<string[], error?> csvAppendStream = csvAppendRows.toStream();
                check caller->putCsvAsStream("/out/putcsvasstream.result.caller", csvAppendStream, APPEND);
                check caller->rename("/in/putcsvasstream.append.caller", "/out/putcsvasstream.append.caller");
            } else if addedFilepath.endsWith("/getcsv.caller") {
                // Read CSV using getCsv and store result
                string[][]|Error csvData = caller->getCsv("/in/getcsv.source.caller");
                if csvData is string[][] {
                    retrievedCsvData = csvData;
                    getCsvContentOk = true;
                }
                check caller->rename("/in/getcsv.caller", "/out/getcsv.caller");
            } else if addedFilepath.endsWith("/getbytesasstream.caller") {
                // Read bytes as stream using getBytesAsStream
                stream<byte[], error?>|Error byteStream = caller->getBytesAsStream("/in/getbytesasstream.source.caller");
                if byteStream is stream<byte[], error?> {
                    byte[] accumulated = [];
                    check from byte[] chunk in byteStream
                        do {
                            accumulated.push(...chunk);
                        };
                    retrievedBytesStreamData = accumulated;
                    getBytesAsStreamContentOk = true;
                }
                check caller->rename("/in/getbytesasstream.caller", "/out/getbytesasstream.caller");
            } else if addedFilepath.endsWith("/getcsvasstream.caller") {
                // Read CSV as stream using getCsvAsStream
                stream<string[], error?>|Error csvStream = caller->getCsvAsStream("/in/getcsvasstream.source.caller");
                if csvStream is stream<string[], error?> {
                    string[][] rows = check from string[] row in csvStream select row;
                    retrievedCsvStreamData = rows;
                    getCsvAsStreamContentOk = true;
                }
                check caller->rename("/in/getcsvasstream.caller", "/out/getcsvasstream.caller");
            } else if addedFilepath.endsWith("/getbytes.caller") {
                // Read bytes using getBytes
                byte[]|Error bytesData = caller->getBytes("/in/getbytes.source.caller");
                if bytesData is byte[] {
                    retrievedBytesData = bytesData;
                    getBytesContentOk = true;
                }
                check caller->rename("/in/getbytes.caller", "/out/getbytes.caller");
            } else if addedFilepath.endsWith("/gettext.caller") {
                // Read text using getText
                string|Error textData = caller->getText("/in/gettext.source.caller");
                if textData is string {
                    retrievedTextData = textData;
                    getTextContentOk = true;
                }
                check caller->rename("/in/gettext.caller", "/out/gettext.caller");
            } else if addedFilepath.endsWith("/getjson.caller") {
                // Read JSON using getJson
                json|record {}|Error jsonData = caller->getJson("/in/getjson.source.caller");
                if jsonData is json {
                    retrievedJsonData = jsonData;
                    getJsonContentOk = true;
                } else if jsonData is record {} {
                    retrievedJsonData = jsonData;
                    getJsonContentOk = true;
                }
                check caller->rename("/in/getjson.caller", "/out/getjson.caller");
            } else if addedFilepath.endsWith("/getxml.caller") {
                // Read XML using getXml
                xml|Error xmlData = caller->getXml("/in/getxml.source.caller");
                if xmlData is xml {
                    retrievedXmlData = xmlData;
                    getXmlContentOk = true;
                }
                check caller->rename("/in/getxml.caller", "/out/getxml.caller");
            }
        }

    }
};

@test:Config {}
public function testFilePutWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/in/put.caller", bStream);
    runtime:sleep(3);
    stream<byte[] & readonly, io:Error?> str = check (<Client>sftpClientEp)->get("/out/put2.caller");
    test:assertTrue(check matchStreamContent(str, "Put content"));
    check str.close();
    check (<Client>sftpClientEp)->delete("/out/put2.caller");
    check (<Client>sftpClientEp)->delete("/out/put.caller");
}

@test:Config {}
public function testFileAppendWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/in/append.caller", bStream);
    runtime:sleep(3);
    stream<byte[] & readonly, io:Error?> str = check (<Client>sftpClientEp)->get("/out/append.caller");
    test:assertTrue(check matchStreamContent(str, "Put contentAppend content"));
    check str.close();
    check (<Client>sftpClientEp)->delete("/out/append.caller");
}

@test:Config {}
public function testFileRenameWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/in/rename.caller", bStream);
    runtime:sleep(3);
    stream<byte[] & readonly, io:Error?> str = check (<Client>sftpClientEp)->get("/out/rename.caller");
    test:assertTrue(check matchStreamContent(str, "Put content"));
    check str.close();
    check (<Client>sftpClientEp)->delete("/out/rename.caller");
}

@test:Config {}
public function testFileDeleteWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/in/delete.caller", bStream);
    runtime:sleep(3);
    stream<byte[] & readonly, io:Error?>|Error result = (<Client>sftpClientEp)->get("/in/delete.caller");
    if result is Error {
        test:assertTrue(result.message().endsWith("delete.caller not found"));
    } else {
        test:assertFail("Caller delete operation has failed");
    }
}

@test:Config {}
public function testFileListWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/in/list.caller", bStream);
    runtime:sleep(3);
    test:assertEquals(fileList.length(), 1);
    test:assertEquals(fileList[0].path, "/in/list.caller");
    check (<Client>sftpClientEp)->delete("/out/list.caller");
}

@test:Config {}
public function testFileGetWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/in/get.caller", bStream);
    runtime:sleep(3);
    test:assertTrue(fileGetContentCorrect);
    check (<Client>sftpClientEp)->delete("/out/get.caller");
}

@test:Config {}
public function testMkDirWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/in/mkdir.caller", bStream);
    runtime:sleep(3);
    boolean isDir = check (<Client>sftpClientEp)->isDirectory("/out/callerDir");
    test:assertTrue(isDir);
    check (<Client>sftpClientEp)->delete("/out/mkdir.caller");
}

@test:Config {
    dependsOn: [testMkDirWithCaller]
}
public function testIsDirectoryWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/in/isdirectory.caller", bStream);
    runtime:sleep(3);
    test:assertTrue(isDir);
    check (<Client>sftpClientEp)->delete("/out/isdirectory.caller");
}

@test:Config {
    dependsOn: [testIsDirectoryWithCaller]
}
public function testRmDirWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/in/rmdir.caller", bStream);
    runtime:sleep(3);
    boolean|Error result = (<Client>sftpClientEp)->isDirectory("/out/callerDir");
    if result is Error {
        test:assertEquals(result.message(), "/out/callerDir does not exist to check if it is a directory.");
    } else {
        test:assertFail("Expected an error");
    }
    check (<Client>sftpClientEp)->delete("/out/rmdir.caller");
}

@test:Config {}
public function testFileSizeWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/in/size.caller", bStream);
    runtime:sleep(3);
    test:assertEquals(fileSize, 11);
    check (<Client>sftpClientEp)->delete("/out/size.caller");
}

@test:Config {}
public function testPutTextTypedWithCaller() returns error? {
    // Trigger the listener service
    putTextContentOk = false;
    check (<Client>sftpClientEp)->putText("/in/puttext.caller", "trigger", OVERWRITE);
    runtime:sleep(5);
    string|Error content = (<Client>sftpClientEp)->getText("/out/puttext.result.caller");
    if content is string {
        test:assertTrue(putTextContentOk, msg = "Listener did not mark putText operation executed");
        test:assertEquals(content, "Caller Text Content");
        // Append scenario trigger
        check (<Client>sftpClientEp)->putText("/in/puttext.append.caller", "trigger2", OVERWRITE);
        runtime:sleep(5);
        string|Error appended = (<Client>sftpClientEp)->getText("/out/puttext.result.caller");
        if appended is string {
            test:assertEquals(appended, "Caller Text Content + Appended", msg = "Appended text content mismatch");
        } else {
            test:assertFail("Failed to read appended text content: " + appended.message());
        }
        // Cleanup
        check (<Client>sftpClientEp)->delete("/out/puttext.result.caller");
        check (<Client>sftpClientEp)->delete("/out/puttext.caller");
        check (<Client>sftpClientEp)->delete("/out/puttext.append.caller");
    } else {
        test:assertFail("Failed to read text content: " + content.message());
    }
}

@test:Config {}
public function testPutBytesTypedWithCaller() returns error? {
    putBytesContentOk = false;
    byte[] trigger = "trigger".toBytes();
    check (<Client>sftpClientEp)->putBytes("/in/putbytes.caller", trigger, OVERWRITE);
    runtime:sleep(5);
    byte[]|Error bytesContent = (<Client>sftpClientEp)->getBytes("/out/putbytes.result.caller");
    if bytesContent is byte[] {
        test:assertTrue(putBytesContentOk, msg = "Listener did not mark putBytes operation executed");
        test:assertEquals(string:fromBytes(bytesContent), "Caller Bytes Content");
        // Append scenario trigger
        byte[] trigger2 = "trigger2".toBytes();
        check (<Client>sftpClientEp)->putBytes("/in/putbytes.append.caller", trigger2, OVERWRITE);
        runtime:sleep(5);
        byte[]|Error appendedBytes = (<Client>sftpClientEp)->getBytes("/out/putbytes.result.caller");
        if appendedBytes is byte[] {
            test:assertEquals(string:fromBytes(appendedBytes), "Caller Bytes Content + Appended", msg = "Appended bytes content mismatch");
        } else {
            test:assertFail("Failed to read appended bytes content: " + appendedBytes.message());
        }
        // Cleanup
        check (<Client>sftpClientEp)->delete("/out/putbytes.result.caller");
        check (<Client>sftpClientEp)->delete("/out/putbytes.caller");
        check (<Client>sftpClientEp)->delete("/out/putbytes.append.caller");
    } else {
        test:assertFail("Failed to read bytes content: " + bytesContent.message());
    }
}

@test:Config {}
public function testPutJsonTypedWithCaller() returns error? {
    putJsonContentOk = false;
    json trigger = { action: "trigger" };
    check (<Client>sftpClientEp)->putJson("/in/putjson.caller", trigger, OVERWRITE);
    runtime:sleep(5);
    json|record {}|Error jsonContent = (<Client>sftpClientEp)->getJson("/out/putjson.result.caller");
    if jsonContent is json|record {} {
        test:assertTrue(putJsonContentOk, msg = "Listener did not mark putJson operation executed");
        json expected = { name: "Alex", active: true, count: 42 };
        test:assertEquals(jsonContent.toJsonString(), expected.toJsonString());
        // Capture original size then trigger append
        int originalSize = check (<Client>sftpClientEp)->size("/out/putjson.result.caller");
        json trigger2 = { action: "append" };
        check (<Client>sftpClientEp)->putJson("/in/putjson.append.caller", trigger2, OVERWRITE);
        runtime:sleep(5);
        int newSize = check (<Client>sftpClientEp)->size("/out/putjson.result.caller");
        test:assertTrue(newSize > originalSize, msg = "Expected appended JSON file size to increase");
    // Raw concatenated JSON can be read via getText if needed
        // Cleanup
        check (<Client>sftpClientEp)->delete("/out/putjson.result.caller");
        check (<Client>sftpClientEp)->delete("/out/putjson.caller");
        check (<Client>sftpClientEp)->delete("/out/putjson.append.caller");
    } else {
        test:assertFail("Failed to read json content: " + jsonContent.message());
    }
}

@test:Config {}
public function testPutXmlTypedWithCaller() returns error? {
    putXmlContentOk = false;
    xml trigger = xml `<trigger/>`;
    check (<Client>sftpClientEp)->putXml("/in/putxml.caller", trigger, OVERWRITE);
    runtime:sleep(5);
    xml|Error xmlContent = (<Client>sftpClientEp)->getXml("/out/putxml.result.caller");
    if xmlContent is xml|record {} {
        test:assertTrue(putXmlContentOk, msg = "Listener did not mark putXml operation executed");
        xml expected = xml `<root><val>42</val><name>Alex</name></root>`;
        test:assertEquals(xmlContent.toString(), expected.toString());
        // Capture original size then trigger append
        int originalSize = check (<Client>sftpClientEp)->size("/out/putxml.result.caller");
        xml trigger2 = xml `<trigger2/>`;
        check (<Client>sftpClientEp)->putXml("/in/putxml.append.caller", trigger2, OVERWRITE);
        runtime:sleep(5);
        int newSize = check (<Client>sftpClientEp)->size("/out/putxml.result.caller");
        test:assertTrue(newSize > originalSize, msg = "Expected appended XML file size to increase");
    // Raw concatenated XML can be read via getText if needed
        // Cleanup
        check (<Client>sftpClientEp)->delete("/out/putxml.result.caller");
        check (<Client>sftpClientEp)->delete("/out/putxml.caller");
        check (<Client>sftpClientEp)->delete("/out/putxml.append.caller");
    } else {
        test:assertFail("Failed to read xml content: " + xmlContent.message());
    }
}

@test:Config {}
public function testPutCsvTypedWithCaller() returns error? {
    putCsvContentOk = false;
    check (<Client>sftpClientEp)->putText("/in/putcsv.caller", "trigger", OVERWRITE);
    runtime:sleep(5);
    string[][]|Error csvContent = (<Client>sftpClientEp)->getCsv("/out/putcsv.result.caller");
    if csvContent is string[][] {
        test:assertTrue(putCsvContentOk, msg = "Listener did not mark putCsv operation executed");
        test:assertEquals(csvContent.length(), 2, msg = "Expected 2 rows in CSV");
        test:assertEquals(csvContent[0], ["Alice", "30", "NYC"]);
        test:assertEquals(csvContent[1], ["Bob", "25", "LA"]);
        // Trigger append
        check (<Client>sftpClientEp)->putText("/in/putcsv.append.caller", "trigger", OVERWRITE);
        runtime:sleep(5);
        string[][]|Error appendedCsv = (<Client>sftpClientEp)->getCsv("/out/putcsv.result.caller");
        if appendedCsv is string[][] {
            test:assertEquals(appendedCsv.length(), 3, msg = "Expected 3 rows after append");
            test:assertEquals(appendedCsv[2], ["Charlie", "35", "SF"]);
        } else {
            test:assertFail("Failed to read appended CSV: " + appendedCsv.message());
        }
        // Cleanup
        check (<Client>sftpClientEp)->delete("/out/putcsv.result.caller");
        check (<Client>sftpClientEp)->delete("/out/putcsv.caller");
        check (<Client>sftpClientEp)->delete("/out/putcsv.append.caller");
    } else {
        test:assertFail("Failed to read CSV content: " + csvContent.message());
    }
}

@test:Config {}
public function testPutBytesAsStreamTypedWithCaller() returns error? {
    putBytesAsStreamContentOk = false;
    check (<Client>sftpClientEp)->putText("/in/putbytesasstream.caller", "trigger", OVERWRITE);
    runtime:sleep(5);
    byte[]|Error bytesContent = (<Client>sftpClientEp)->getBytes("/out/putbytesasstream.result.caller");
    if bytesContent is byte[] {
        test:assertTrue(putBytesAsStreamContentOk, msg = "Listener did not mark putBytesAsStream operation executed");
        test:assertEquals(string:fromBytes(bytesContent), "Streamed Bytes Content");
        // Trigger append
        check (<Client>sftpClientEp)->putText("/in/putbytesasstream.append.caller", "trigger", OVERWRITE);
        runtime:sleep(5);
        byte[]|Error appendedBytes = (<Client>sftpClientEp)->getBytes("/out/putbytesasstream.result.caller");
        if appendedBytes is byte[] {
            test:assertEquals(string:fromBytes(appendedBytes), "Streamed Bytes Content + Stream Appended");
        } else {
            test:assertFail("Failed to read appended streamed bytes: " + appendedBytes.message());
        }
        // Cleanup
        check (<Client>sftpClientEp)->delete("/out/putbytesasstream.result.caller");
        check (<Client>sftpClientEp)->delete("/out/putbytesasstream.caller");
        check (<Client>sftpClientEp)->delete("/out/putbytesasstream.append.caller");
    } else {
        test:assertFail("Failed to read bytes stream content: " + bytesContent.message());
    }
}

@test:Config {}
public function testPutCsvAsStreamTypedWithCaller() returns error? {
    putCsvAsStreamContentOk = false;
    check (<Client>sftpClientEp)->putText("/in/putcsvasstream.caller", "trigger", OVERWRITE);
    runtime:sleep(5);
    stream<string[], error?>|Error csvStream = (<Client>sftpClientEp)->getCsvAsStream("/out/putcsvasstream.result.caller");
    if csvStream is stream<string[], error?> {
        test:assertTrue(putCsvAsStreamContentOk, msg = "Listener did not mark putCsvAsStream operation executed");
        string[][] rows = check from string[] row in csvStream select row;
        test:assertEquals(rows.length(), 2, msg = "Expected 2 rows");
        test:assertEquals(rows[0], ["Widget", "10.50"]);
        test:assertEquals(rows[1], ["Gadget", "25.00"]);
        // Trigger append
        check (<Client>sftpClientEp)->putText("/in/putcsvasstream.append.caller", "trigger", OVERWRITE);
        runtime:sleep(5);
        stream<string[], error?>|Error appendStream = (<Client>sftpClientEp)->getCsvAsStream("/out/putcsvasstream.result.caller");
        if appendStream is stream<string[], error?> {
            string[][] allRows = check from string[] row in appendStream select row;
            test:assertEquals(allRows.length(), 3, msg = "Expected 3 rows after append");
            test:assertEquals(allRows[2], ["Doohickey", "15.75"]);
        } else {
            test:assertFail("Failed to read appended CSV stream: " + appendStream.message());
        }
        // Cleanup
        check (<Client>sftpClientEp)->delete("/out/putcsvasstream.result.caller");
        check (<Client>sftpClientEp)->delete("/out/putcsvasstream.caller");
        check (<Client>sftpClientEp)->delete("/out/putcsvasstream.append.caller");
    } else {
        test:assertFail("Failed to read CSV stream content: " + csvStream.message());
    }
}

@test:Config {}
public function testGetCsvTypedWithCaller() returns error? {
    getCsvContentOk = false;
    retrievedCsvData = [];
    // First, create a CSV file that the listener will read
    string[][] sourceData = [["ID", "Name", "Status"], ["1", "Alice", "Active"], ["2", "Bob", "Inactive"]];
    check (<Client>sftpClientEp)->putCsv("/in/getcsv.source.caller", sourceData, OVERWRITE);
    // Trigger listener to read the CSV
    check (<Client>sftpClientEp)->putText("/in/getcsv.caller", "trigger", OVERWRITE);
    runtime:sleep(5);
    test:assertTrue(getCsvContentOk, msg = "Listener did not mark getCsv operation executed");
    test:assertEquals(retrievedCsvData.length(), 2, msg = "Expected 2 rows from getCsv (excluding header row)");
    test:assertEquals(retrievedCsvData[0], ["1", "Alice", "Active"]);
    test:assertEquals(retrievedCsvData[1], ["2", "Bob", "Inactive"]);
    // Cleanup
    check (<Client>sftpClientEp)->delete("/in/getcsv.source.caller");
    check (<Client>sftpClientEp)->delete("/out/getcsv.caller");
}

@test:Config {}
public function testGetBytesAsStreamTypedWithCaller() returns error? {
    getBytesAsStreamContentOk = false;
    retrievedBytesStreamData = [];
    // Create a source file with bytes content
    byte[] sourceBytes = "Hello from bytes stream test!".toBytes();
    check (<Client>sftpClientEp)->putBytes("/in/getbytesasstream.source.caller", sourceBytes, OVERWRITE);
    // Trigger listener to read bytes as stream
    check (<Client>sftpClientEp)->putText("/in/getbytesasstream.caller", "trigger", OVERWRITE);
    runtime:sleep(5);
    test:assertTrue(getBytesAsStreamContentOk, msg = "Listener did not mark getBytesAsStream operation executed");
    test:assertEquals(string:fromBytes(retrievedBytesStreamData), "Hello from bytes stream test!");
    // Cleanup
    check (<Client>sftpClientEp)->delete("/in/getbytesasstream.source.caller");
    check (<Client>sftpClientEp)->delete("/out/getbytesasstream.caller");
}

@test:Config {}
public function testGetCsvAsStreamTypedWithCaller() returns error? {
    getCsvAsStreamContentOk = false;
    retrievedCsvStreamData = [];
    // Create a CSV source file
    string[][] sourceData = [["Code", "Description"], ["A100", "Widget"], ["B200", "Gadget"], ["C300", "Tool"]];
    check (<Client>sftpClientEp)->putCsv("/in/getcsvasstream.source.caller", sourceData, OVERWRITE);
    // Trigger listener to read CSV as stream
    check (<Client>sftpClientEp)->putText("/in/getcsvasstream.caller", "trigger", OVERWRITE);
    runtime:sleep(5);
    test:assertTrue(getCsvAsStreamContentOk, msg = "Listener did not mark getCsvAsStream operation executed");
    test:assertEquals(retrievedCsvStreamData.length(), 3, msg = "Expected 3 data rows from getCsvAsStream (excluding header)");
    test:assertEquals(retrievedCsvStreamData[0], ["A100", "Widget"]);
    test:assertEquals(retrievedCsvStreamData[1], ["B200", "Gadget"]);
    test:assertEquals(retrievedCsvStreamData[2], ["C300", "Tool"]);
    // Cleanup
    check (<Client>sftpClientEp)->delete("/in/getcsvasstream.source.caller");
    check (<Client>sftpClientEp)->delete("/out/getcsvasstream.caller");
}

@test:Config {}
public function testGetBytesTypedWithCaller() returns error? {
    getBytesContentOk = false;
    retrievedBytesData = [];
    // Create a source file with bytes content
    byte[] sourceBytes = "Binary data content for testing".toBytes();
    check (<Client>sftpClientEp)->putBytes("/in/getbytes.source.caller", sourceBytes, OVERWRITE);
    // Trigger listener to read bytes
    check (<Client>sftpClientEp)->putText("/in/getbytes.caller", "trigger", OVERWRITE);
    runtime:sleep(5);
    test:assertTrue(getBytesContentOk, msg = "Listener did not mark getBytes operation executed");
    test:assertEquals(string:fromBytes(retrievedBytesData), "Binary data content for testing");
    // Cleanup
    check (<Client>sftpClientEp)->delete("/in/getbytes.source.caller");
    check (<Client>sftpClientEp)->delete("/out/getbytes.caller");
}

@test:Config {}
public function testGetTextTypedWithCaller() returns error? {
    getTextContentOk = false;
    retrievedTextData = "";
    // Create a source file with text content
    string sourceText = "This is a sample text content for testing getText method.";
    check (<Client>sftpClientEp)->putText("/in/gettext.source.caller", sourceText, OVERWRITE);
    // Trigger listener to read text
    check (<Client>sftpClientEp)->putText("/in/gettext.caller", "trigger", OVERWRITE);
    runtime:sleep(5);
    test:assertTrue(getTextContentOk, msg = "Listener did not mark getText operation executed");
    test:assertEquals(retrievedTextData, "This is a sample text content for testing getText method.");
    // Cleanup
    check (<Client>sftpClientEp)->delete("/in/gettext.source.caller");
    check (<Client>sftpClientEp)->delete("/out/gettext.caller");
}

@test:Config {}
public function testGetJsonTypedWithCaller() returns error? {
    getJsonContentOk = false;
    retrievedJsonData = {};
    // Create a source file with JSON content
    json sourceJson = { id: 123, name: "Test User", active: true, score: 95.5 };
    check (<Client>sftpClientEp)->putJson("/in/getjson.source.caller", sourceJson, OVERWRITE);
    // Trigger listener to read JSON
    check (<Client>sftpClientEp)->putText("/in/getjson.caller", "trigger", OVERWRITE);
    runtime:sleep(5);
    test:assertTrue(getJsonContentOk, msg = "Listener did not mark getJson operation executed");
    json expected = { id: 123, name: "Test User", active: true, score: 95.5 };
    test:assertEquals(retrievedJsonData.toJsonString(), expected.toJsonString());
    // Cleanup
    check (<Client>sftpClientEp)->delete("/in/getjson.source.caller");
    check (<Client>sftpClientEp)->delete("/out/getjson.caller");
}

@test:Config {}
public function testGetXmlTypedWithCaller() returns error? {
    getXmlContentOk = false;
    retrievedXmlData = xml ``;
    // Create a source file with XML content
    xml sourceXml = xml `<book><title>Sample Book</title><author>John Doe</author><year>2024</year></book>`;
    check (<Client>sftpClientEp)->putXml("/in/getxml.source.caller", sourceXml, OVERWRITE);
    // Trigger listener to read XML
    check (<Client>sftpClientEp)->putText("/in/getxml.caller", "trigger", OVERWRITE);
    runtime:sleep(5);
    test:assertTrue(getXmlContentOk, msg = "Listener did not mark getXml operation executed");
    xml expected = xml `<book><title>Sample Book</title><author>John Doe</author><year>2024</year></book>`;
    test:assertEquals(retrievedXmlData.toString(), expected.toString());
    // Cleanup
    check (<Client>sftpClientEp)->delete("/in/getxml.source.caller");
    check (<Client>sftpClientEp)->delete("/out/getxml.caller");
}

@test:Config {
    dependsOn: [testSecureAddedFileCount]
}
public function testMutableWatchEventWithCaller() returns error? {
    Service watchEventService = service object {
        remote function onFileChange(WatchEvent event, Caller caller) returns error? {
            event.addedFiles.forEach(function (FileInfo fileInfo) {
                if fileInfo.name == filename {
                    addedFile = filename;
                }
            });
        }
    };
    Listener 'listener = check new (callerListenerConfig);
    check 'listener.attach(watchEventService);
    check 'listener.start();
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/in/" + filename, bStream);
    runtime:sleep(3);
    test:assertEquals(addedFile, filename);
    check (<Client>sftpClientEp)->delete("/in/" + filename);
}

boolean fileMoved = false;
boolean fileCopied = false;
boolean fileExists = false;

@test:Config {
    dependsOn: [testMutableWatchEventWithCaller]
}
public function testFileMoveWithCaller() returns error? {
    string sourceName = "moveSource.caller";
    string destName = "moveDestination.txt";
    Service moveService = service object {
        remote function onFileChange(WatchEvent event, Caller caller) returns error? {
            foreach FileInfo fileInfo in event.addedFiles {
                if fileInfo.name == sourceName {
                    check caller->move("/in/" + sourceName, "/in/" + destName);
                    fileMoved = true;
                }
            }
        }
    };
    Listener 'listener = check new (callerListenerConfig);
    check 'listener.attach(moveService);
    check 'listener.start();
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/in/" + sourceName, bStream);
    runtime:sleep(3);
    test:assertTrue(fileMoved);
    check (<Client>sftpClientEp)->delete("/in/" + destName);
}

@test:Config {
    dependsOn: [testFileMoveWithCaller]
}
public function testFileCopyWithCaller() returns error? {
    string sourceName = "copySource.caller";
    string destName = "copyDestination.txt";
    Service copyService = service object {
        remote function onFileChange(WatchEvent event, Caller caller) returns error? {
            foreach FileInfo fileInfo in event.addedFiles {
                if fileInfo.name == sourceName {
                    check caller->copy("/in/" + sourceName, "/in/" + destName);
                    fileCopied = true;
                }
            }
        }
    };
    Listener 'listener = check new (callerListenerConfig);
    check 'listener.attach(copyService);
    check 'listener.start();
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/in/" + sourceName, bStream);
    runtime:sleep(3);
    test:assertTrue(fileCopied);
    check (<Client>sftpClientEp)->delete("/in/" + sourceName);
    check (<Client>sftpClientEp)->delete("/in/" + destName);
}

@test:Config {
    dependsOn: [testFileCopyWithCaller]
}
public function testFileExistsWithCaller() returns error? {
    string checkName = "existsCheck.caller";
    Service existsService = service object {
        remote function onFileChange(WatchEvent event, Caller caller) returns error? {
            foreach FileInfo fileInfo in event.addedFiles {
                if fileInfo.name == checkName {
                    fileExists = check caller->exists("/in/" + checkName);
                }
            }
        }
    };
    Listener 'listener = check new (callerListenerConfig);
    check 'listener.attach(existsService);
    check 'listener.start();
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<Client>sftpClientEp)->put("/in/" + checkName, bStream);
    runtime:sleep(3);
    test:assertTrue(fileExists);
    check (<Client>sftpClientEp)->delete("/in/" + checkName);
}
