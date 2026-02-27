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

import ballerina/ftp;
import ballerina/ftp_test_commons;
import ballerina/lang.runtime as runtime;
import ballerina/io;
import ballerina/test;

ftp:FileInfo[] fileList = [];
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

ftp:ListenerConfiguration callerListenerConfig = {
    protocol: ftp:SFTP,
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

ftp:Service callerService = service object {

    remote function onFileChange(ftp:WatchEvent & readonly event, ftp:Caller caller) returns error? {
        foreach ftp:FileInfo fileInfo in event.addedFiles {
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
                fileGetContentCorrect = check ftp_test_commons:matchStreamContent(fileStream, "Put content");
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
                check caller->putText("/out/puttext.result.caller", "Caller Text Content", ftp:OVERWRITE);
                putTextContentOk = true;
                check caller->rename("/in/puttext.caller", "/out/puttext.caller");
            } else if addedFilepath.endsWith("/puttext.append.caller") {
                check caller->putText("/out/puttext.result.caller", " + Appended", ftp:APPEND);
                check caller->rename("/in/puttext.append.caller", "/out/puttext.append.caller");
            } else if addedFilepath.endsWith("/putbytes.caller") {
                byte[] b = "Caller Bytes Content".toBytes();
                check caller->putBytes("/out/putbytes.result.caller", b, ftp:OVERWRITE);
                putBytesContentOk = true;
                check caller->rename("/in/putbytes.caller", "/out/putbytes.caller");
            } else if addedFilepath.endsWith("/putbytes.append.caller") {
                byte[] b2 = " + Appended".toBytes();
                check caller->putBytes("/out/putbytes.result.caller", b2, ftp:APPEND);
                check caller->rename("/in/putbytes.append.caller", "/out/putbytes.append.caller");
            } else if addedFilepath.endsWith("/putjson.caller") {
                json j = { name: "Alex", active: true, count: 42 };
                check caller->putJson("/out/putjson.result.caller", j, ftp:OVERWRITE);
                putJsonContentOk = true;
                check caller->rename("/in/putjson.caller", "/out/putjson.caller");
            } else if addedFilepath.endsWith("/putjson.append.caller") {
                json j2 = { appended: true, version: 1 };
                check caller->putJson("/out/putjson.result.caller", j2, ftp:APPEND);
                check caller->rename("/in/putjson.append.caller", "/out/putjson.append.caller");
            } else if addedFilepath.endsWith("/putxml.caller") {
                xml x = xml `<root><val>42</val><name>Alex</name></root>`;
                check caller->putXml("/out/putxml.result.caller", x, ftp:OVERWRITE);
                putXmlContentOk = true;
                check caller->rename("/in/putxml.caller", "/out/putxml.caller");
            } else if addedFilepath.endsWith("/putxml.append.caller") {
                xml x2 = xml `<append><ok>true</ok></append>`;
                check caller->putXml("/out/putxml.result.caller", x2, ftp:APPEND);
                check caller->rename("/in/putxml.append.caller", "/out/putxml.append.caller");
            } else if addedFilepath.endsWith("/putcsv.caller") {
                string[][] csvData = [["Name", "Age", "City"], ["Alice", "30", "NYC"], ["Bob", "25", "LA"]];
                check caller->putCsv("/out/putcsv.result.caller", csvData, ftp:OVERWRITE);
                putCsvContentOk = true;
                check caller->rename("/in/putcsv.caller", "/out/putcsv.caller");
            } else if addedFilepath.endsWith("/putcsv.append.caller") {
                string[][] csvAppend = [["Charlie", "35", "SF"]];
                check caller->putCsv("/out/putcsv.result.caller", csvAppend, ftp:APPEND);
                check caller->rename("/in/putcsv.append.caller", "/out/putcsv.append.caller");
            } else if addedFilepath.endsWith("/putbytesasstream.caller") {
                byte[] data = "Streamed Bytes Content".toBytes();
                stream<byte[], error?> byteStream = [data].toStream();
                check caller->putBytesAsStream("/out/putbytesasstream.result.caller", byteStream, ftp:OVERWRITE);
                putBytesAsStreamContentOk = true;
                check caller->rename("/in/putbytesasstream.caller", "/out/putbytesasstream.caller");
            } else if addedFilepath.endsWith("/putbytesasstream.append.caller") {
                byte[] data2 = " + Stream Appended".toBytes();
                stream<byte[], error?> byteStream2 = [data2].toStream();
                check caller->putBytesAsStream("/out/putbytesasstream.result.caller", byteStream2, ftp:APPEND);
                check caller->rename("/in/putbytesasstream.append.caller", "/out/putbytesasstream.append.caller");
            } else if addedFilepath.endsWith("/putcsvasstream.caller") {
                string[][] csvRows = [["Product", "Price"], ["Widget", "10.50"], ["Gadget", "25.00"]];
                stream<string[], error?> csvStream = csvRows.toStream();
                check caller->putCsvAsStream("/out/putcsvasstream.result.caller", csvStream, ftp:OVERWRITE);
                putCsvAsStreamContentOk = true;
                check caller->rename("/in/putcsvasstream.caller", "/out/putcsvasstream.caller");
            } else if addedFilepath.endsWith("/putcsvasstream.append.caller") {
                string[][] csvAppendRows = [["Doohickey", "15.75"]];
                stream<string[], error?> csvAppendStream = csvAppendRows.toStream();
                check caller->putCsvAsStream("/out/putcsvasstream.result.caller", csvAppendStream, ftp:APPEND);
                check caller->rename("/in/putcsvasstream.append.caller", "/out/putcsvasstream.append.caller");
            } else if addedFilepath.endsWith("/getcsv.caller") {
                string[][]|ftp:Error csvData = caller->getCsv("/in/getcsv.source.caller");
                if csvData is string[][] {
                    retrievedCsvData = csvData;
                    getCsvContentOk = true;
                }
                check caller->rename("/in/getcsv.caller", "/out/getcsv.caller");
            } else if addedFilepath.endsWith("/getbytesasstream.caller") {
                stream<byte[], error?>|ftp:Error byteStream = caller->getBytesAsStream("/in/getbytesasstream.source.caller");
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
                stream<string[], error?>|ftp:Error csvStream = caller->getCsvAsStream("/in/getcsvasstream.source.caller");
                if csvStream is stream<string[], error?> {
                    string[][] rows = check from string[] row in csvStream select row;
                    retrievedCsvStreamData = rows;
                    getCsvAsStreamContentOk = true;
                }
                check caller->rename("/in/getcsvasstream.caller", "/out/getcsvasstream.caller");
            } else if addedFilepath.endsWith("/getbytes.caller") {
                byte[]|ftp:Error bytesData = caller->getBytes("/in/getbytes.source.caller");
                if bytesData is byte[] {
                    retrievedBytesData = bytesData;
                    getBytesContentOk = true;
                }
                check caller->rename("/in/getbytes.caller", "/out/getbytes.caller");
            } else if addedFilepath.endsWith("/gettext.caller") {
                string|ftp:Error textData = caller->getText("/in/gettext.source.caller");
                if textData is string {
                    retrievedTextData = textData;
                    getTextContentOk = true;
                }
                check caller->rename("/in/gettext.caller", "/out/gettext.caller");
            } else if addedFilepath.endsWith("/getjson.caller") {
                json|record {}|ftp:Error jsonData = caller->getJson("/in/getjson.source.caller");
                if jsonData is json {
                    retrievedJsonData = jsonData;
                    getJsonContentOk = true;
                } else if jsonData is record {} {
                    retrievedJsonData = jsonData;
                    getJsonContentOk = true;
                }
                check caller->rename("/in/getjson.caller", "/out/getjson.caller");
            } else if addedFilepath.endsWith("/getxml.caller") {
                xml|ftp:Error xmlData = caller->getXml("/in/getxml.source.caller");
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
    check (<ftp:Client>sftpClientEp)->put("/in/put.caller", bStream);
    runtime:sleep(3);
    stream<byte[] & readonly, io:Error?> str = check (<ftp:Client>sftpClientEp)->get("/out/put2.caller");
    test:assertTrue(check ftp_test_commons:matchStreamContent(str, "Put content"));
    check str.close();
    check (<ftp:Client>sftpClientEp)->delete("/out/put2.caller");
    check (<ftp:Client>sftpClientEp)->delete("/out/put.caller");
}

@test:Config {}
public function testFileAppendWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<ftp:Client>sftpClientEp)->put("/in/append.caller", bStream);
    runtime:sleep(3);
    stream<byte[] & readonly, io:Error?> str = check (<ftp:Client>sftpClientEp)->get("/out/append.caller");
    test:assertTrue(check ftp_test_commons:matchStreamContent(str, "Put contentAppend content"));
    check str.close();
    check (<ftp:Client>sftpClientEp)->delete("/out/append.caller");
}

@test:Config {}
public function testFileRenameWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<ftp:Client>sftpClientEp)->put("/in/rename.caller", bStream);
    runtime:sleep(3);
    stream<byte[] & readonly, io:Error?> str = check (<ftp:Client>sftpClientEp)->get("/out/rename.caller");
    test:assertTrue(check ftp_test_commons:matchStreamContent(str, "Put content"));
    check str.close();
    check (<ftp:Client>sftpClientEp)->delete("/out/rename.caller");
}

@test:Config {}
public function testFileDeleteWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<ftp:Client>sftpClientEp)->put("/in/delete.caller", bStream);
    runtime:sleep(3);
    stream<byte[] & readonly, io:Error?>|ftp:Error result = (<ftp:Client>sftpClientEp)->get("/in/delete.caller");
    if result is ftp:Error {
        test:assertTrue(result.message().endsWith("delete.caller not found"));
    } else {
        test:assertFail("Caller delete operation has failed");
    }
}

@test:Config {}
public function testFileListWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<ftp:Client>sftpClientEp)->put("/in/list.caller", bStream);
    runtime:sleep(3);
    test:assertEquals(fileList.length(), 1);
    test:assertEquals(fileList[0].path, "/in/list.caller");
    check (<ftp:Client>sftpClientEp)->delete("/out/list.caller");
}

@test:Config {}
public function testFileGetWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<ftp:Client>sftpClientEp)->put("/in/get.caller", bStream);
    runtime:sleep(3);
    test:assertTrue(fileGetContentCorrect);
    check (<ftp:Client>sftpClientEp)->delete("/out/get.caller");
}

@test:Config {}
public function testMkDirWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<ftp:Client>sftpClientEp)->put("/in/mkdir.caller", bStream);
    runtime:sleep(3);
    boolean isDir = check (<ftp:Client>sftpClientEp)->isDirectory("/out/callerDir");
    test:assertTrue(isDir);
    check (<ftp:Client>sftpClientEp)->delete("/out/mkdir.caller");
}

@test:Config {
    dependsOn: [testMkDirWithCaller]
}
public function testIsDirectoryWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<ftp:Client>sftpClientEp)->put("/in/isdirectory.caller", bStream);
    runtime:sleep(3);
    test:assertTrue(isDir);
    check (<ftp:Client>sftpClientEp)->delete("/out/isdirectory.caller");
}

@test:Config {
    dependsOn: [testIsDirectoryWithCaller]
}
public function testRmDirWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<ftp:Client>sftpClientEp)->put("/in/rmdir.caller", bStream);
    runtime:sleep(3);
    boolean|ftp:Error result = (<ftp:Client>sftpClientEp)->isDirectory("/out/callerDir");
    if result is ftp:Error {
        test:assertEquals(result.message(), "/out/callerDir does not exist to check if it is a directory.");
    } else {
        test:assertFail("Expected an error");
    }
    check (<ftp:Client>sftpClientEp)->delete("/out/rmdir.caller");
}

@test:Config {}
public function testFileSizeWithCaller() returns error? {
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<ftp:Client>sftpClientEp)->put("/in/size.caller", bStream);
    runtime:sleep(3);
    test:assertEquals(fileSize, 11);
    check (<ftp:Client>sftpClientEp)->delete("/out/size.caller");
}

@test:Config {}
public function testPutTextTypedWithCaller() returns error? {
    putTextContentOk = false;
    check (<ftp:Client>sftpClientEp)->putText("/in/puttext.caller", "trigger", ftp:OVERWRITE);
    runtime:sleep(5);
    string|ftp:Error content = (<ftp:Client>sftpClientEp)->getText("/out/puttext.result.caller");
    if content is string {
        test:assertTrue(putTextContentOk, msg = "Listener did not mark putText operation executed");
        test:assertEquals(content, "Caller Text Content");
        check (<ftp:Client>sftpClientEp)->putText("/in/puttext.append.caller", "trigger2", ftp:OVERWRITE);
        runtime:sleep(5);
        string|ftp:Error appended = (<ftp:Client>sftpClientEp)->getText("/out/puttext.result.caller");
        if appended is string {
            test:assertEquals(appended, "Caller Text Content + Appended", msg = "Appended text content mismatch");
        } else {
            test:assertFail("Failed to read appended text content: " + appended.message());
        }
        check (<ftp:Client>sftpClientEp)->delete("/out/puttext.result.caller");
        check (<ftp:Client>sftpClientEp)->delete("/out/puttext.caller");
        check (<ftp:Client>sftpClientEp)->delete("/out/puttext.append.caller");
    } else {
        test:assertFail("Failed to read text content: " + content.message());
    }
}

@test:Config {}
public function testPutBytesTypedWithCaller() returns error? {
    putBytesContentOk = false;
    byte[] trigger = "trigger".toBytes();
    check (<ftp:Client>sftpClientEp)->putBytes("/in/putbytes.caller", trigger, ftp:OVERWRITE);
    runtime:sleep(5);
    byte[]|ftp:Error bytesContent = (<ftp:Client>sftpClientEp)->getBytes("/out/putbytes.result.caller");
    if bytesContent is byte[] {
        test:assertTrue(putBytesContentOk, msg = "Listener did not mark putBytes operation executed");
        test:assertEquals(string:fromBytes(bytesContent), "Caller Bytes Content");
        byte[] trigger2 = "trigger2".toBytes();
        check (<ftp:Client>sftpClientEp)->putBytes("/in/putbytes.append.caller", trigger2, ftp:OVERWRITE);
        runtime:sleep(5);
        byte[]|ftp:Error appendedBytes = (<ftp:Client>sftpClientEp)->getBytes("/out/putbytes.result.caller");
        if appendedBytes is byte[] {
            test:assertEquals(string:fromBytes(appendedBytes), "Caller Bytes Content + Appended", msg = "Appended bytes content mismatch");
        } else {
            test:assertFail("Failed to read appended bytes content: " + appendedBytes.message());
        }
        check (<ftp:Client>sftpClientEp)->delete("/out/putbytes.result.caller");
        check (<ftp:Client>sftpClientEp)->delete("/out/putbytes.caller");
        check (<ftp:Client>sftpClientEp)->delete("/out/putbytes.append.caller");
    } else {
        test:assertFail("Failed to read bytes content: " + bytesContent.message());
    }
}

@test:Config {}
public function testPutJsonTypedWithCaller() returns error? {
    putJsonContentOk = false;
    json trigger = { action: "trigger" };
    check (<ftp:Client>sftpClientEp)->putJson("/in/putjson.caller", trigger, ftp:OVERWRITE);
    runtime:sleep(5);
    json|record {}|ftp:Error jsonContent = (<ftp:Client>sftpClientEp)->getJson("/out/putjson.result.caller");
    if jsonContent is json|record {} {
        test:assertTrue(putJsonContentOk, msg = "Listener did not mark putJson operation executed");
        json expected = { name: "Alex", active: true, count: 42 };
        test:assertEquals(jsonContent.toJsonString(), expected.toJsonString());
        int originalSize = check (<ftp:Client>sftpClientEp)->size("/out/putjson.result.caller");
        json trigger2 = { action: "append" };
        check (<ftp:Client>sftpClientEp)->putJson("/in/putjson.append.caller", trigger2, ftp:OVERWRITE);
        runtime:sleep(5);
        int newSize = check (<ftp:Client>sftpClientEp)->size("/out/putjson.result.caller");
        test:assertTrue(newSize > originalSize, msg = "Expected appended JSON file size to increase");
        check (<ftp:Client>sftpClientEp)->delete("/out/putjson.result.caller");
        check (<ftp:Client>sftpClientEp)->delete("/out/putjson.caller");
        check (<ftp:Client>sftpClientEp)->delete("/out/putjson.append.caller");
    } else {
        test:assertFail("Failed to read json content: " + jsonContent.message());
    }
}

@test:Config {}
public function testPutXmlTypedWithCaller() returns error? {
    putXmlContentOk = false;
    xml trigger = xml `<trigger/>`;
    check (<ftp:Client>sftpClientEp)->putXml("/in/putxml.caller", trigger, ftp:OVERWRITE);
    runtime:sleep(5);
    xml|ftp:Error xmlContent = (<ftp:Client>sftpClientEp)->getXml("/out/putxml.result.caller");
    if xmlContent is xml|record {} {
        test:assertTrue(putXmlContentOk, msg = "Listener did not mark putXml operation executed");
        xml expected = xml `<root><val>42</val><name>Alex</name></root>`;
        test:assertEquals(xmlContent.toString(), expected.toString());
        int originalSize = check (<ftp:Client>sftpClientEp)->size("/out/putxml.result.caller");
        xml trigger2 = xml `<trigger2/>`;
        check (<ftp:Client>sftpClientEp)->putXml("/in/putxml.append.caller", trigger2, ftp:OVERWRITE);
        runtime:sleep(5);
        int newSize = check (<ftp:Client>sftpClientEp)->size("/out/putxml.result.caller");
        test:assertTrue(newSize > originalSize, msg = "Expected appended XML file size to increase");
        check (<ftp:Client>sftpClientEp)->delete("/out/putxml.result.caller");
        check (<ftp:Client>sftpClientEp)->delete("/out/putxml.caller");
        check (<ftp:Client>sftpClientEp)->delete("/out/putxml.append.caller");
    } else {
        test:assertFail("Failed to read xml content: " + xmlContent.message());
    }
}

@test:Config {}
public function testPutCsvTypedWithCaller() returns error? {
    putCsvContentOk = false;
    check (<ftp:Client>sftpClientEp)->putText("/in/putcsv.caller", "trigger", ftp:OVERWRITE);
    runtime:sleep(5);
    string[][]|ftp:Error csvContent = (<ftp:Client>sftpClientEp)->getCsv("/out/putcsv.result.caller");
    if csvContent is string[][] {
        test:assertTrue(putCsvContentOk, msg = "Listener did not mark putCsv operation executed");
        test:assertEquals(csvContent.length(), 2, msg = "Expected 2 rows in CSV");
        test:assertEquals(csvContent[0], ["Alice", "30", "NYC"]);
        test:assertEquals(csvContent[1], ["Bob", "25", "LA"]);
        check (<ftp:Client>sftpClientEp)->putText("/in/putcsv.append.caller", "trigger", ftp:OVERWRITE);
        runtime:sleep(5);
        string[][]|ftp:Error appendedCsv = (<ftp:Client>sftpClientEp)->getCsv("/out/putcsv.result.caller");
        if appendedCsv is string[][] {
            test:assertEquals(appendedCsv.length(), 3, msg = "Expected 3 rows after append");
            test:assertEquals(appendedCsv[2], ["Charlie", "35", "SF"]);
        } else {
            test:assertFail("Failed to read appended CSV: " + appendedCsv.message());
        }
        check (<ftp:Client>sftpClientEp)->delete("/out/putcsv.result.caller");
        check (<ftp:Client>sftpClientEp)->delete("/out/putcsv.caller");
        check (<ftp:Client>sftpClientEp)->delete("/out/putcsv.append.caller");
    } else {
        test:assertFail("Failed to read CSV content: " + csvContent.message());
    }
}

@test:Config {}
public function testPutBytesAsStreamTypedWithCaller() returns error? {
    putBytesAsStreamContentOk = false;
    check (<ftp:Client>sftpClientEp)->putText("/in/putbytesasstream.caller", "trigger", ftp:OVERWRITE);
    runtime:sleep(5);
    byte[]|ftp:Error bytesContent = (<ftp:Client>sftpClientEp)->getBytes("/out/putbytesasstream.result.caller");
    if bytesContent is byte[] {
        test:assertTrue(putBytesAsStreamContentOk, msg = "Listener did not mark putBytesAsStream operation executed");
        test:assertEquals(string:fromBytes(bytesContent), "Streamed Bytes Content");
        check (<ftp:Client>sftpClientEp)->putText("/in/putbytesasstream.append.caller", "trigger", ftp:OVERWRITE);
        runtime:sleep(5);
        byte[]|ftp:Error appendedBytes = (<ftp:Client>sftpClientEp)->getBytes("/out/putbytesasstream.result.caller");
        if appendedBytes is byte[] {
            test:assertEquals(string:fromBytes(appendedBytes), "Streamed Bytes Content + Stream Appended");
        } else {
            test:assertFail("Failed to read appended streamed bytes: " + appendedBytes.message());
        }
        check (<ftp:Client>sftpClientEp)->delete("/out/putbytesasstream.result.caller");
        check (<ftp:Client>sftpClientEp)->delete("/out/putbytesasstream.caller");
        check (<ftp:Client>sftpClientEp)->delete("/out/putbytesasstream.append.caller");
    } else {
        test:assertFail("Failed to read bytes stream content: " + bytesContent.message());
    }
}

@test:Config {}
public function testPutCsvAsStreamTypedWithCaller() returns error? {
    putCsvAsStreamContentOk = false;
    check (<ftp:Client>sftpClientEp)->putText("/in/putcsvasstream.caller", "trigger", ftp:OVERWRITE);
    runtime:sleep(5);
    stream<string[], error?>|ftp:Error csvStream = (<ftp:Client>sftpClientEp)->getCsvAsStream("/out/putcsvasstream.result.caller");
    if csvStream is stream<string[], error?> {
        test:assertTrue(putCsvAsStreamContentOk, msg = "Listener did not mark putCsvAsStream operation executed");
        string[][] rows = check from string[] row in csvStream select row;
        test:assertEquals(rows.length(), 2, msg = "Expected 2 rows");
        test:assertEquals(rows[0], ["Widget", "10.50"]);
        test:assertEquals(rows[1], ["Gadget", "25.00"]);
        check (<ftp:Client>sftpClientEp)->putText("/in/putcsvasstream.append.caller", "trigger", ftp:OVERWRITE);
        runtime:sleep(5);
        stream<string[], error?>|ftp:Error appendStream = (<ftp:Client>sftpClientEp)->getCsvAsStream("/out/putcsvasstream.result.caller");
        if appendStream is stream<string[], error?> {
            string[][] allRows = check from string[] row in appendStream select row;
            test:assertEquals(allRows.length(), 3, msg = "Expected 3 rows after append");
            test:assertEquals(allRows[2], ["Doohickey", "15.75"]);
        } else {
            test:assertFail("Failed to read appended CSV stream: " + appendStream.message());
        }
        check (<ftp:Client>sftpClientEp)->delete("/out/putcsvasstream.result.caller");
        check (<ftp:Client>sftpClientEp)->delete("/out/putcsvasstream.caller");
        check (<ftp:Client>sftpClientEp)->delete("/out/putcsvasstream.append.caller");
    } else {
        test:assertFail("Failed to read CSV stream content: " + csvStream.message());
    }
}

@test:Config {}
public function testGetCsvTypedWithCaller() returns error? {
    getCsvContentOk = false;
    retrievedCsvData = [];
    string[][] sourceData = [["ID", "Name", "Status"], ["1", "Alice", "Active"], ["2", "Bob", "Inactive"]];
    check (<ftp:Client>sftpClientEp)->putCsv("/in/getcsv.source.caller", sourceData, ftp:OVERWRITE);
    check (<ftp:Client>sftpClientEp)->putText("/in/getcsv.caller", "trigger", ftp:OVERWRITE);
    runtime:sleep(5);
    test:assertTrue(getCsvContentOk, msg = "Listener did not mark getCsv operation executed");
    test:assertEquals(retrievedCsvData.length(), 2, msg = "Expected 2 rows from getCsv (excluding header row)");
    test:assertEquals(retrievedCsvData[0], ["1", "Alice", "Active"]);
    test:assertEquals(retrievedCsvData[1], ["2", "Bob", "Inactive"]);
    check (<ftp:Client>sftpClientEp)->delete("/in/getcsv.source.caller");
    check (<ftp:Client>sftpClientEp)->delete("/out/getcsv.caller");
}

@test:Config {}
public function testGetBytesAsStreamTypedWithCaller() returns error? {
    getBytesAsStreamContentOk = false;
    retrievedBytesStreamData = [];
    byte[] sourceBytes = "Hello from bytes stream test!".toBytes();
    check (<ftp:Client>sftpClientEp)->putBytes("/in/getbytesasstream.source.caller", sourceBytes, ftp:OVERWRITE);
    check (<ftp:Client>sftpClientEp)->putText("/in/getbytesasstream.caller", "trigger", ftp:OVERWRITE);
    runtime:sleep(5);
    test:assertTrue(getBytesAsStreamContentOk, msg = "Listener did not mark getBytesAsStream operation executed");
    test:assertEquals(string:fromBytes(retrievedBytesStreamData), "Hello from bytes stream test!");
    check (<ftp:Client>sftpClientEp)->delete("/in/getbytesasstream.source.caller");
    check (<ftp:Client>sftpClientEp)->delete("/out/getbytesasstream.caller");
}

@test:Config {}
public function testGetCsvAsStreamTypedWithCaller() returns error? {
    getCsvAsStreamContentOk = false;
    retrievedCsvStreamData = [];
    string[][] sourceData = [["Code", "Description"], ["A100", "Widget"], ["B200", "Gadget"], ["C300", "Tool"]];
    check (<ftp:Client>sftpClientEp)->putCsv("/in/getcsvasstream.source.caller", sourceData, ftp:OVERWRITE);
    check (<ftp:Client>sftpClientEp)->putText("/in/getcsvasstream.caller", "trigger", ftp:OVERWRITE);
    runtime:sleep(5);
    test:assertTrue(getCsvAsStreamContentOk, msg = "Listener did not mark getCsvAsStream operation executed");
    test:assertEquals(retrievedCsvStreamData.length(), 3, msg = "Expected 3 data rows from getCsvAsStream (excluding header)");
    test:assertEquals(retrievedCsvStreamData[0], ["A100", "Widget"]);
    test:assertEquals(retrievedCsvStreamData[1], ["B200", "Gadget"]);
    test:assertEquals(retrievedCsvStreamData[2], ["C300", "Tool"]);
    check (<ftp:Client>sftpClientEp)->delete("/in/getcsvasstream.source.caller");
    check (<ftp:Client>sftpClientEp)->delete("/out/getcsvasstream.caller");
}

@test:Config {}
public function testGetBytesTypedWithCaller() returns error? {
    getBytesContentOk = false;
    retrievedBytesData = [];
    byte[] sourceBytes = "Binary data content for testing".toBytes();
    check (<ftp:Client>sftpClientEp)->putBytes("/in/getbytes.source.caller", sourceBytes, ftp:OVERWRITE);
    check (<ftp:Client>sftpClientEp)->putText("/in/getbytes.caller", "trigger", ftp:OVERWRITE);
    runtime:sleep(5);
    test:assertTrue(getBytesContentOk, msg = "Listener did not mark getBytes operation executed");
    test:assertEquals(string:fromBytes(retrievedBytesData), "Binary data content for testing");
    check (<ftp:Client>sftpClientEp)->delete("/in/getbytes.source.caller");
    check (<ftp:Client>sftpClientEp)->delete("/out/getbytes.caller");
}

@test:Config {}
public function testGetTextTypedWithCaller() returns error? {
    getTextContentOk = false;
    retrievedTextData = "";
    string sourceText = "This is a sample text content for testing getText method.";
    check (<ftp:Client>sftpClientEp)->putText("/in/gettext.source.caller", sourceText, ftp:OVERWRITE);
    check (<ftp:Client>sftpClientEp)->putText("/in/gettext.caller", "trigger", ftp:OVERWRITE);
    runtime:sleep(5);
    test:assertTrue(getTextContentOk, msg = "Listener did not mark getText operation executed");
    test:assertEquals(retrievedTextData, "This is a sample text content for testing getText method.");
    check (<ftp:Client>sftpClientEp)->delete("/in/gettext.source.caller");
    check (<ftp:Client>sftpClientEp)->delete("/out/gettext.caller");
}

@test:Config {}
public function testGetJsonTypedWithCaller() returns error? {
    getJsonContentOk = false;
    retrievedJsonData = {};
    json sourceJson = { id: 123, name: "Test User", active: true, score: 95.5 };
    check (<ftp:Client>sftpClientEp)->putJson("/in/getjson.source.caller", sourceJson, ftp:OVERWRITE);
    check (<ftp:Client>sftpClientEp)->putText("/in/getjson.caller", "trigger", ftp:OVERWRITE);
    runtime:sleep(5);
    test:assertTrue(getJsonContentOk, msg = "Listener did not mark getJson operation executed");
    json expected = { id: 123, name: "Test User", active: true, score: 95.5 };
    test:assertEquals(retrievedJsonData.toJsonString(), expected.toJsonString());
    check (<ftp:Client>sftpClientEp)->delete("/in/getjson.source.caller");
    check (<ftp:Client>sftpClientEp)->delete("/out/getjson.caller");
}

@test:Config {}
public function testGetXmlTypedWithCaller() returns error? {
    getXmlContentOk = false;
    retrievedXmlData = xml ``;
    xml sourceXml = xml `<book><title>Sample Book</title><author>John Doe</author><year>2024</year></book>`;
    check (<ftp:Client>sftpClientEp)->putXml("/in/getxml.source.caller", sourceXml, ftp:OVERWRITE);
    check (<ftp:Client>sftpClientEp)->putText("/in/getxml.caller", "trigger", ftp:OVERWRITE);
    runtime:sleep(5);
    test:assertTrue(getXmlContentOk, msg = "Listener did not mark getXml operation executed");
    xml expected = xml `<book><title>Sample Book</title><author>John Doe</author><year>2024</year></book>`;
    test:assertEquals(retrievedXmlData.toString(), expected.toString());
    check (<ftp:Client>sftpClientEp)->delete("/in/getxml.source.caller");
    check (<ftp:Client>sftpClientEp)->delete("/out/getxml.caller");
}

@test:Config {
    dependsOn: [testSecureAddedFileCount]
}
public function testMutableWatchEventWithCaller() returns error? {
    ftp:Service watchEventService = service object {
        remote function onFileChange(ftp:WatchEvent event, ftp:Caller caller) returns error? {
            event.addedFiles.forEach(function (ftp:FileInfo fileInfo) {
                if fileInfo.name == filename {
                    addedFile = filename;
                }
            });
        }
    };
    ftp:Listener 'listener = check new (callerListenerConfig);
    check 'listener.attach(watchEventService);
    check 'listener.start();
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<ftp:Client>sftpClientEp)->put("/in/" + filename, bStream);
    runtime:sleep(3);
    check 'listener.gracefulStop();
    test:assertEquals(addedFile, filename);
    check (<ftp:Client>sftpClientEp)->delete("/in/" + filename);
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
    ftp:Service moveService = service object {
        remote function onFileChange(ftp:WatchEvent event, ftp:Caller caller) returns error? {
            foreach ftp:FileInfo fileInfo in event.addedFiles {
                if fileInfo.name == sourceName {
                    check caller->move("/in/" + sourceName, "/in/" + destName);
                    fileMoved = true;
                }
            }
        }
    };
    ftp:Listener 'listener = check new (callerListenerConfig);
    check 'listener.attach(moveService);
    check 'listener.start();
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<ftp:Client>sftpClientEp)->put("/in/" + sourceName, bStream);
    runtime:sleep(3);
    check 'listener.gracefulStop();
    test:assertTrue(fileMoved);
    check (<ftp:Client>sftpClientEp)->delete("/in/" + destName);
}

@test:Config {
    dependsOn: [testFileMoveWithCaller]
}
public function testFileCopyWithCaller() returns error? {
    string sourceName = "copySource.caller";
    string destName = "copyDestination.txt";
    ftp:Service copyService = service object {
        remote function onFileChange(ftp:WatchEvent event, ftp:Caller caller) returns error? {
            foreach ftp:FileInfo fileInfo in event.addedFiles {
                if fileInfo.name == sourceName {
                    check caller->copy("/in/" + sourceName, "/in/" + destName);
                    fileCopied = true;
                }
            }
        }
    };
    ftp:Listener 'listener = check new (callerListenerConfig);
    check 'listener.attach(copyService);
    check 'listener.start();
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<ftp:Client>sftpClientEp)->put("/in/" + sourceName, bStream);
    runtime:sleep(3);
    check 'listener.gracefulStop();
    test:assertTrue(fileCopied);
    check (<ftp:Client>sftpClientEp)->delete("/in/" + sourceName);
    check (<ftp:Client>sftpClientEp)->delete("/in/" + destName);
}

@test:Config {
    dependsOn: [testFileCopyWithCaller]
}
public function testFileExistsWithCaller() returns error? {
    string checkName = "existsCheck.caller";
    ftp:Service existsService = service object {
        remote function onFileChange(ftp:WatchEvent event, ftp:Caller caller) returns error? {
            foreach ftp:FileInfo fileInfo in event.addedFiles {
                if fileInfo.name == checkName {
                    fileExists = check caller->exists("/in/" + checkName);
                }
            }
        }
    };
    ftp:Listener 'listener = check new (callerListenerConfig);
    check 'listener.attach(existsService);
    check 'listener.start();
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream(putFilePath, 5);
    check (<ftp:Client>sftpClientEp)->put("/in/" + checkName, bStream);
    runtime:sleep(3);
    check 'listener.gracefulStop();
    test:assertTrue(fileExists);
    check (<ftp:Client>sftpClientEp)->delete("/in/" + checkName);
}
