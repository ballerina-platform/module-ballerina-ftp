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
import ballerina/test;

import ballerina_tests/ftp_test_commons as commons;

// ─── Type definitions ─────────────────────────────────────────────────────────

// Strict record: both fields required, no extras allowed.
type Person record {|
    string name;
    int age;
|};

// Lax record: age is optional.
type PersonLax record {|
    string name;
    int? age;
|};

// Strict CSV record.
type CsvPerson record {|
    string name;
    int age;
|};

// Lax CSV record: only name required.
type CsvPersonLax record {|
    string name;
|};

// General-purpose row record for CSV round-trip tests.
type Row record {|
    string name;
    int age;
|};

// ─── Well-known server paths ──────────────────────────────────────────────────

// Pre-seeded by the mock server; do not delete in tests.
const string SEED_FILE = "/home/in/test1.txt"; // "File content"
const string SEED_LARGE_FILE = "/home/in/test4.txt"; // 9000-byte repeating file
const string SEED_DIR = "/home/in";

// Isolated working paths — each test uses its own path to stay independent.
const string P_PUT_BYTES = "/home/in/api-put-bytes.txt";
const string P_PUT_TEXT = "/home/in/api-put-text.txt";
const string P_PUT_JSON = "/home/in/api-put-json.json";
const string P_PUT_XML = "/home/in/api-put-xml.xml";
const string P_PUT_CSV_STR = "/home/in/api-put-csv-str.csv";
const string P_PUT_CSV_REC = "/home/in/api-put-csv-rec.csv";
const string P_PUT_COMPRESSED_SRC = "/home/in/api-compressed.txt";
const string P_PUT_COMPRESSED_ZIP = "/home/in/api-compressed.zip";
const string P_PUT_CSV_STREAM_STR = "/home/in/api-csv-stream-str.csv";
const string P_PUT_CSV_STREAM_REC = "/home/in/api-csv-stream-rec.csv";
const string P_PUT_LARGE = "/home/in/api-put-large.txt";
const string P_TEXT_APPEND = "/home/in/api-text-append.txt";
const string P_CSV_APPEND = "/home/in/api-csv-append.csv";
const string P_JAILED_REL = "api-jailed-rel.txt";
const string P_JAILED_SLASH = "/api-jailed-slash.txt";
const string P_STREAM_CLOSE_CSV = "/home/in/api-stream-close.csv";
const string P_BINDING_JSON = "/home/in/api-bind-json.txt";
const string P_BINDING_XML = "/home/in/api-bind-xml.txt";
const string P_BINDING_CSV = "/home/in/api-bind-csv.csv";
const string P_BIND_TYPED_JSON = "/home/in/api-typed-json.json";
const string P_BIND_TYPED_XML = "/home/in/api-typed-xml.xml";
const string P_BIND_TYPED_CSV = "/home/in/api-typed-csv.csv";
const string P_BIND_TYPED_CSV_STREAM = "/home/in/api-typed-csv-stream.csv";
const string P_BYTES_AS_STREAM = "/home/in/api-bytes-stream.txt";
const string P_SIZE_TEST = "/home/in/api-size-test.txt";
const string P_MOVE_SRC = "/home/in/api-move-src.txt";
const string P_MOVE_DST = "/home/in/api-move-dst.txt";
const string P_COPY_DST = "/home/in/api-copy-dst.txt";
const string P_DELETE = "/home/in/api-delete.txt";

// ─── Shared clients (initialised once per suite) ──────────────────────────────

ftp:Client anonClient = check new ({
    protocol: ftp:FTP,
    host: commons:FTP_HOST,
    port: commons:ANON_FTP_PORT
});

ftp:Client ftpClient = check new ({
    protocol: ftp:FTP,
    host: commons:FTP_HOST,
    port: commons:AUTH_FTP_PORT,
    auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
    userDirIsRoot: false
});

ftp:Client ftpLaxClient = check new ({
    protocol: ftp:FTP,
    host: commons:FTP_HOST,
    port: commons:AUTH_FTP_PORT,
    auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
    laxDataBinding: true
});

ftp:Client ftpJailedClient = check new ({
    protocol: ftp:FTP,
    host: commons:FTP_HOST,
    port: commons:AUTH_FTP_PORT,
    auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}},
    userDirIsRoot: true
});

// ─── Suite teardown ───────────────────────────────────────────────────────────

@test:AfterSuite {}
function cleanupClientApiTestFiles() {
    string[] paths = [
        P_PUT_BYTES,
        P_PUT_TEXT,
        P_PUT_JSON,
        P_PUT_XML,
        P_PUT_CSV_STR,
        P_PUT_CSV_REC,
        P_PUT_CSV_STREAM_STR,
        P_PUT_CSV_STREAM_REC,
        P_PUT_LARGE,
        P_TEXT_APPEND,
        P_CSV_APPEND,
        P_STREAM_CLOSE_CSV,
        P_BINDING_JSON,
        P_BINDING_XML,
        P_BINDING_CSV,
        P_BIND_TYPED_JSON,
        P_BIND_TYPED_XML,
        P_BIND_TYPED_CSV,
        P_BIND_TYPED_CSV_STREAM,
        P_BYTES_AS_STREAM,
        P_SIZE_TEST,
        P_MOVE_DST,
        P_COPY_DST,
        P_DELETE
    ];
    foreach string p in paths {
        do {
            check ftpClient->delete(p);
        } on fail {
        }
    }
    do {
        check ftpJailedClient->delete(P_JAILED_REL);
    } on fail {
    }
    do {
        check ftpJailedClient->delete(P_JAILED_SLASH);
    } on fail {
    }
}

// =============================================================================
// getText / getBytes — read from the server
// =============================================================================

// Anonymous server: read a pre-seeded file as UTF-8 text.
@test:Config {
    groups: ["ftp-client-api", "get"]
}
function testGetText_fromAnonServer() returns error? {
    string content = check anonClient->getText(SEED_FILE);
    test:assertEquals(content, "File content", "Unexpected content from anon getText");
}

// Authenticated server: getText on the same seed file.
@test:Config {
    groups: ["ftp-client-api", "get"]
}
function testGetText_authenticated() returns error? {
    string content = check ftpClient->getText(SEED_FILE);
    test:assertEquals(content, "File content", "Unexpected content from authenticated getText");
}

// Large file: getBytes returns the full byte array.
@test:Config {
    groups: ["ftp-client-api", "get"]
}
function testGetBytes_largeFile() returns error? {
    byte[] content = check ftpClient->getBytes(SEED_LARGE_FILE);
    test:assertEquals(content.length(), 9000, "Large file should be 9000 bytes");
}

// getBytesAsStream returns all bytes when consumed as a stream.
@test:Config {
    groups: ["ftp-client-api", "get"]
}
function testGetBytesAsStream() returns error? {
    byte[] expected = check ftpClient->getBytes(SEED_FILE);
    stream<byte[], error?> s = check ftpClient->getBytesAsStream(SEED_FILE);
    byte[] accumulated = [];
    check from byte[] chunk in s
        do {
            accumulated.push(...chunk);
        };
    test:assertEquals(accumulated, expected, "getBytesAsStream should return the same bytes as getBytes");
}

// =============================================================================
// PUT / GET — typed API round-trips
// =============================================================================

@test:Config {
    groups: ["ftp-client-api", "put", "typed"]
}
function testPutGetText() returns error? {
    string payload = "hello text content";
    check ftpClient->putText(P_PUT_TEXT, payload);
    string got = check ftpClient->getText(P_PUT_TEXT);
    test:assertEquals(got, payload, "putText/getText round-trip mismatch");
}

@test:Config {
    groups: ["ftp-client-api", "put", "typed"]
}
function testPutGetBytes() returns error? {
    byte[] payload = "hello-bytes".toBytes();
    check ftpClient->putBytes(P_PUT_BYTES, payload);
    byte[] got = check ftpClient->getBytes(P_PUT_BYTES);
    test:assertEquals(got, payload, "putBytes/getBytes round-trip mismatch");
}

@test:Config {
    groups: ["ftp-client-api", "put", "typed"]
}
function testPutGetBytesAsStream() returns error? {
    byte[] payload = "hello-stream-bytes".toBytes();
    check ftpClient->putBytes(P_BYTES_AS_STREAM, payload);

    stream<byte[], error?> s = check ftpClient->getBytesAsStream(P_BYTES_AS_STREAM);
    byte[] accumulated = [];
    check from byte[] chunk in s
        do {
            accumulated.push(...chunk);
        };
    test:assertEquals(accumulated, payload, "getBytesAsStream content mismatch");
}

@test:Config {
    groups: ["ftp-client-api", "put", "typed"]
}
function testPutGetJson() returns error? {
    json payload = {name: "wso2", count: 2, ok: true};
    check ftpClient->putJson(P_PUT_JSON, payload);
    json got = check ftpClient->getJson(P_PUT_JSON);
    test:assertEquals(got, payload, "putJson/getJson round-trip mismatch");
}

@test:Config {
    groups: ["ftp-client-api", "put", "typed"]
}
function testPutGetXml() returns error? {
    xml payload = xml `<root><item k="v">42</item></root>`;
    check ftpClient->putXml(P_PUT_XML, payload);
    xml got = check ftpClient->getXml(P_PUT_XML);
    test:assertEquals(got.toString(), payload.toString(), "putXml/getXml round-trip mismatch");
}

@test:Config {
    groups: ["ftp-client-api", "put", "typed"]
}
function testPutGetCsv_stringRows() returns error? {
    string[][] payload = [
        ["id", "name", "age"],
        ["1", "Alice", "25"],
        ["2", "Bob", "30"]
    ];
    check ftpClient->putCsv(P_PUT_CSV_STR, payload);

    // getCsv returns data rows only (header excluded when target is string[][]).
    string[][] got = check ftpClient->getCsv(P_PUT_CSV_STR);
    test:assertEquals(got, payload.slice(1), "putCsv/getCsv string rows mismatch");

    // Same rows via stream.
    stream<string[], error?> s = check ftpClient->getCsvAsStream(P_PUT_CSV_STR);
    string[][] fromStream = [];
    check from string[] row in s
        do {
            fromStream.push(row);
        };
    test:assertEquals(fromStream, payload.slice(1), "getCsvAsStream string rows mismatch");
}

@test:Config {
    groups: ["ftp-client-api", "put", "typed"]
}
function testPutGetCsv_records() returns error? {
    Row[] payload = [{name: "Charlie", age: 22}, {name: "Dana", age: 28}];
    check ftpClient->putCsv(P_PUT_CSV_REC, payload);

    Row[] got = check ftpClient->getCsv(P_PUT_CSV_REC);
    test:assertEquals(got, payload, "putCsv/getCsv record round-trip mismatch");

    stream<Row, error?> s = check ftpClient->getCsvAsStream(P_PUT_CSV_REC);
    Row[] fromStream = [];
    check from Row row in s
        do {
            fromStream.push(row);
        };
    test:assertEquals(fromStream, payload, "getCsvAsStream record round-trip mismatch");
}

// =============================================================================
// putBytesAsStream / putCsvAsStream
// =============================================================================

@test:Config {
    groups: ["ftp-client-api", "put", "typed"]
}
function testPutBytesAsStream() returns error? {
    byte[][] chunks = ["hello-".toBytes(), "world".toBytes()];
    check ftpClient->putBytesAsStream(P_PUT_BYTES, chunks.toStream());
    string got = check ftpClient->getText(P_PUT_BYTES);
    test:assertEquals(got, "hello-world", "putBytesAsStream content mismatch");
}

@test:Config {
    groups: ["ftp-client-api", "put", "typed"]
}
function testPutCsvAsStream_stringRows() returns error? {
    string[][] rows = [["id", "name"], ["1", "A"], ["2", "B"]];
    check ftpClient->putCsvAsStream(P_PUT_CSV_STREAM_STR, rows.toStream());

    string[][] got = check ftpClient->getCsv(P_PUT_CSV_STREAM_STR);
    test:assertEquals(got, rows.slice(1), "putCsvAsStream string rows mismatch");
}

@test:Config {
    groups: ["ftp-client-api", "put", "typed"]
}
function testPutCsvAsStream_records() returns error? {
    Row[] records = [{name: "Eve", age: 35}, {name: "Frank", age: 40}];
    check ftpClient->putCsvAsStream(P_PUT_CSV_STREAM_REC, records.toStream());

    Row[] got = check ftpClient->getCsv(P_PUT_CSV_STREAM_REC);
    test:assertEquals(got, records, "putCsvAsStream record mismatch");
}

// =============================================================================
// putBytesAsStream — large content spanning multiple internal buffers
// =============================================================================

@test:Config {
    groups: ["ftp-client-api", "put"]
}
function testPutBytesAsStream_largeContent() returns error? {
    byte[] block = [];
    foreach int i in 0 ..< 16390 {
        block[i] = 65; // 'A'
    }
    byte[][] chunks = [block, "123456".toBytes(), "end.".toBytes()];
    check ftpClient->putBytesAsStream(P_PUT_LARGE, chunks.toStream());

    byte[] got = check ftpClient->getBytes(P_PUT_LARGE);
    test:assertEquals(got.length(), 16400, "Large putBytesAsStream size mismatch");
    // Verify boundary content.
    test:assertEquals(got[0], 65, "First byte should be 'A'");
    test:assertEquals(got[16390], 49, "Byte 16390 should be '1'");
}

// =============================================================================
// PUT — write modes (OVERWRITE / APPEND)
// =============================================================================

@test:Config {
    groups: ["ftp-client-api", "put", "mode"]
}
function testPutText_appendMode() returns error? {
    check ftpClient->putText(P_TEXT_APPEND, "Hello", ftp:OVERWRITE);
    check ftpClient->putText(P_TEXT_APPEND, " + world", ftp:APPEND);
    string got = check ftpClient->getText(P_TEXT_APPEND);
    test:assertEquals(got, "Hello + world", "putText APPEND mode should concatenate content");
}

@test:Config {
    groups: ["ftp-client-api", "put", "mode"]
}
function testPutCsv_appendMode() returns error? {
    string[][] first = [["id", "name"], ["1", "Alpha"]];
    check ftpClient->putCsv(P_CSV_APPEND, first, ftp:OVERWRITE);

    string[][] more = [["2", "Beta"], ["3", "Gamma"]];
    check ftpClient->putCsv(P_CSV_APPEND, more, ftp:APPEND);

    string[][] got = check ftpClient->getCsv(P_CSV_APPEND);
    test:assertEquals(got, [["1", "Alpha"], ["2", "Beta"], ["3", "Gamma"]],
            "putCsv APPEND should add rows without duplicating header");
}

// =============================================================================
// Stream control
// =============================================================================

@test:Config {
    groups: ["ftp-client-api", "stream"]
}
function testStreamClose_typedStreams() returns error? {
    Row[] records = [{name: "G", age: 1}, {name: "H", age: 2}];
    check ftpClient->putCsv(P_STREAM_CLOSE_CSV, records);

    stream<Row, error?> s1 = check ftpClient->getCsvAsStream(P_STREAM_CLOSE_CSV);
    check s1.close();

    stream<string[], error?> s2 = check ftpClient->getCsvAsStream(P_STREAM_CLOSE_CSV);
    check s2.close();

    stream<byte[], error?> s3 = check ftpClient->getBytesAsStream(P_STREAM_CLOSE_CSV);
    check s3.close();
}

// =============================================================================
// Path normalisation — userDirIsRoot
// =============================================================================

// With userDirIsRoot=true the user's FTP home becomes "/". A relative path
// "test1.txt" maps to home + "/test1.txt".
@test:Config {
    groups: ["ftp-client-api", "path"]
}
function testUserDirIsRootTrue_getText() returns error? {
    string content = check ftpJailedClient->getText("test1.txt");
    test:assertEquals(content, "File content",
            "userDirIsRoot=true: relative getText should resolve from user home");
}

// Relative path (no leading slash): resolved from the jailed root.
@test:Config {
    groups: ["ftp-client-api", "path"]
}
function testUserDirIsRootTrue_relativePut() returns error? {
    check ftpJailedClient->putText(P_JAILED_REL, "hello-jailed");
    string got = check ftpJailedClient->getText(P_JAILED_REL);
    test:assertEquals(got, "hello-jailed",
            "userDirIsRoot=true: relative put/get round-trip failed");
    do {
        check ftpJailedClient->delete(P_JAILED_REL);
    } on fail {
    }
}

// Relative path with a leading slash: still resolved from the jailed root.
@test:Config {
    groups: ["ftp-client-api", "path"]
}
function testUserDirIsRootTrue_relativeSlashPut() returns error? {
    check ftpJailedClient->putText(P_JAILED_SLASH, "hello-jailed-slash");
    string got = check ftpJailedClient->getText(P_JAILED_SLASH);
    test:assertEquals(got, "hello-jailed-slash",
            "userDirIsRoot=true: leading-slash path put/get round-trip failed");
    do {
        check ftpJailedClient->delete(P_JAILED_SLASH);
    } on fail {
    }
}

// =============================================================================
// Content binding errors
// =============================================================================

// getJson on non-JSON content must yield ContentBindingError.
@test:Config {
    groups: ["ftp-client-api", "binding"]
}
function testGetJson_invalidContent_bindsError() returns error? {
    check ftpClient->putText(P_BINDING_JSON, "not valid json {{{");
    json|ftp:Error result = ftpClient->getJson(P_BINDING_JSON);
    test:assertTrue(result is ftp:ContentBindingError,
            "getJson on non-JSON content should return ContentBindingError");
    if result is ftp:ContentBindingError {
        test:assertTrue(result.detail().filePath is string,
                "ContentBindingError should carry filePath detail");
        test:assertTrue(result.detail().content is byte[],
                "ContentBindingError should carry content bytes");
    }
    do {
        check ftpClient->delete(P_BINDING_JSON);
    } on fail {
    }
}

// getXml on non-XML content must yield ContentBindingError.
@test:Config {
    groups: ["ftp-client-api", "binding"]
}
function testGetXml_invalidContent_bindsError() returns error? {
    check ftpClient->putText(P_BINDING_XML, "not valid xml <<<>>>");
    xml|ftp:Error result = ftpClient->getXml(P_BINDING_XML);
    test:assertTrue(result is ftp:ContentBindingError,
            "getXml on non-XML content should return ContentBindingError");
    if result is ftp:ContentBindingError {
        test:assertTrue(result.detail().filePath is string,
                "ContentBindingError should carry filePath detail");
    }
    do {
        check ftpClient->delete(P_BINDING_XML);
    } on fail {
    }
}

// getJson with a strict target type that is missing a required field.
@test:Config {
    groups: ["ftp-client-api", "binding"]
}
function testGetJson_missingField_bindsError() returns error? {
    check ftpClient->putJson(P_BIND_TYPED_JSON, <json>{name: "Alice"}); // missing 'age'
    Person|ftp:Error result = ftpClient->getJson(P_BIND_TYPED_JSON);
    test:assertTrue(result is ftp:ContentBindingError,
            "getJson should return ContentBindingError when required field is absent");
    do {
        check ftpClient->delete(P_BIND_TYPED_JSON);
    } on fail {
    }
}

// getCsv with a strict record type when a field has the wrong data type.
@test:Config {
    groups: ["ftp-client-api", "binding"]
}
function testGetCsv_wrongType_bindsError() returns error? {
    string[][] data = [["name", "age"], ["Alice", "not-a-number"]];
    check ftpClient->putCsv(P_BINDING_CSV, data);
    CsvPerson[]|ftp:Error result = ftpClient->getCsv(P_BINDING_CSV);
    test:assertTrue(result is ftp:ContentBindingError,
            "getCsv should return ContentBindingError when field type mismatches");
    do {
        check ftpClient->delete(P_BINDING_CSV);
    } on fail {
    }
}

// getBytes / getText on a non-existent file must return an error.
@test:Config {
    groups: ["ftp-client-api", "binding"]
}
function testGetTyped_nonExistentFile() {
    string missing = "/home/in/does-not-exist-api.txt";
    byte[]|ftp:Error r1 = ftpClient->getBytes(missing);
    test:assertTrue(r1 is ftp:Error, "getBytes should error for non-existent file");
    string|ftp:Error r2 = ftpClient->getText(missing);
    test:assertTrue(r2 is ftp:Error, "getText should error for non-existent file");
}

// =============================================================================
// Strict vs lax data binding
// =============================================================================

// JSON: strict binding fails on missing field; lax binding maps it to nil.
@test:Config {
    groups: ["ftp-client-api", "binding", "lax"]
}
function testJsonBinding_strictAndLax() returns error? {
    check ftpClient->putJson(P_BIND_TYPED_JSON, <json>{name: "Alice"}); // missing 'age'

    Person|ftp:Error strictResult = ftpClient->getJson(P_BIND_TYPED_JSON, Person);
    test:assertTrue(strictResult is ftp:Error,
            "Strict JSON binding should fail when required field is absent");

    PersonLax laxResult = check ftpLaxClient->getJson(P_BIND_TYPED_JSON, PersonLax);
    test:assertEquals(laxResult.name, "Alice");
    test:assertTrue(laxResult.age is (),
            "Lax JSON binding should map absent field to nil");

    do {
        check ftpClient->delete(P_BIND_TYPED_JSON);
    } on fail {
    }
}

// XML: strict binding fails on extra fields; lax succeeds.
@test:Config {
    groups: ["ftp-client-api", "binding", "lax"]
}
function testXmlBinding_strictAndLax() returns error? {
    xml x = xml `<person><name>Alice</name><age>32</age><address>street</address></person>`;
    check ftpClient->putXml(P_BIND_TYPED_XML, x);

    Person|ftp:Error strictResult = ftpClient->getXml(P_BIND_TYPED_XML, Person);
    test:assertTrue(strictResult is ftp:Error,
            "Strict XML binding should fail when XML has extra fields");

    Person laxResult = check ftpLaxClient->getXml(P_BIND_TYPED_XML, Person);
    test:assertEquals(laxResult.name, "Alice");

    do {
        check ftpClient->delete(P_BIND_TYPED_XML);
    } on fail {
    }
}

// CSV: strict binding fails on empty/missing field; lax succeeds.
@test:Config {
    groups: ["ftp-client-api", "binding", "lax"]
}
function testCsvBinding_strictAndLax() returns error? {
    string[][] data = [["name", "age"], ["Alice", "25"], ["Bob", ""]];
    check ftpClient->putCsv(P_BIND_TYPED_CSV, data, ftp:OVERWRITE);

    CsvPerson[]|ftp:Error strictResult = ftpClient->getCsv(P_BIND_TYPED_CSV);
    test:assertTrue(strictResult is ftp:Error,
            "Strict CSV binding should fail when required field is empty");

    CsvPersonLax[] laxResult = check ftpLaxClient->getCsv(P_BIND_TYPED_CSV);
    test:assertEquals(laxResult.length(), 2, "Lax CSV should return 2 records");
    test:assertEquals(laxResult[0].name, "Alice");
    test:assertEquals(laxResult[1].name, "Bob");

    do {
        check ftpClient->delete(P_BIND_TYPED_CSV);
    } on fail {
    }
}

// CSV stream: strict stream errors on bad row; lax stream succeeds.
@test:Config {
    groups: ["ftp-client-api", "binding", "lax"]
}
function testCsvStreamBinding_strictAndLax() returns error? {
    string[][] data = [["name", "age"], ["Charlie", "30"], ["Diana", ""]];
    check ftpClient->putCsv(P_BIND_TYPED_CSV_STREAM, data, ftp:OVERWRITE);

    stream<CsvPerson, error?> strictStream = check ftpClient->getCsvAsStream(P_BIND_TYPED_CSV_STREAM, CsvPerson);
    error? streamError = ();
    while true {
        record {|CsvPerson value;|}|error? next = strictStream.next();
        if next is error {
            streamError = next;
            break;
        } else if next is () {
            break;
        }
    }
    test:assertTrue(streamError is ftp:ContentBindingError,
            "Strict CSV stream should error on row with missing required field");

    stream<CsvPersonLax, error?> laxStream = check ftpLaxClient->getCsvAsStream(P_BIND_TYPED_CSV_STREAM, CsvPersonLax);
    CsvPersonLax[] laxRecords = [];
    check from CsvPersonLax row in laxStream
        do {
            laxRecords.push(row);
        };
    test:assertEquals(laxRecords.length(), 2, "Lax CSV stream should return 2 records");

    do {
        check ftpClient->delete(P_BIND_TYPED_CSV_STREAM);
    } on fail {
    }
}

// =============================================================================
// isDirectory / mkdir / rename
// =============================================================================

@test:Config {
    groups: ["ftp-client-api", "directory"]
}
function testIsDirectory_dirAndFile() returns error? {
    boolean dirResult = check ftpClient->isDirectory(SEED_DIR);
    test:assertTrue(dirResult, "isDirectory should return true for a directory");

    boolean fileResult = check ftpClient->isDirectory(SEED_FILE);
    test:assertFalse(fileResult, "isDirectory should return false for a file");
}

@test:Config {
    groups: ["ftp-client-api", "directory"]
}
function testMkdir_createAndVerify() returns error? {
    string dir = "/home/in/api-mkdir-test";
    check ftpClient->mkdir(dir);
    boolean exists = check ftpClient->isDirectory(dir);
    test:assertTrue(exists, "mkdir should create the directory");
    do {
        check ftpClient->rmdir(dir);
    } on fail {
    }
}

@test:Config {
    groups: ["ftp-client-api", "directory"]
}
function testRename_directory() returns error? {
    string oldPath = "/home/in/api-rename-old";
    string newPath = "/home/in/api-rename-new";
    check ftpClient->mkdir(oldPath);
    check ftpClient->rename(oldPath, newPath);

    // Old path should no longer be a directory.
    boolean|ftp:Error oldExists = ftpClient->isDirectory(oldPath);
    test:assertTrue(oldExists is ftp:Error || oldExists == false,
            "Original path should not exist after rename");

    // New path should be a directory.
    boolean newExists = check ftpClient->isDirectory(newPath);
    test:assertTrue(newExists, "Renamed directory should exist at the new path");

    do {
        check ftpClient->rmdir(newPath);
    } on fail {
    }
}

// =============================================================================
// move / copy / exists / size
// =============================================================================

@test:Config {
    groups: ["ftp-client-api", "file-ops"]
}
function testMove() returns error? {
    string content = "move-test-content";
    check ftpClient->putText(P_MOVE_SRC, content);
    check ftpClient->move(P_MOVE_SRC, P_MOVE_DST);

    boolean srcGone = check ftpClient->exists(P_MOVE_SRC);
    test:assertFalse(srcGone, "Source file should not exist after move");

    string dstContent = check ftpClient->getText(P_MOVE_DST);
    test:assertEquals(dstContent, content, "Destination file should have original content after move");

    do {
        check ftpClient->delete(P_MOVE_DST);
    } on fail {
    }
}

@test:Config {
    groups: ["ftp-client-api", "file-ops"]
}
function testCopy() returns error? {
    check ftpClient->copy(SEED_FILE, P_COPY_DST);

    boolean srcExists = check ftpClient->exists(SEED_FILE);
    test:assertTrue(srcExists, "Source file must still exist after copy");

    boolean dstExists = check ftpClient->exists(P_COPY_DST);
    test:assertTrue(dstExists, "Destination file must exist after copy");

    string src = check ftpClient->getText(SEED_FILE);
    string dst = check ftpClient->getText(P_COPY_DST);
    test:assertEquals(src, dst, "Copied file should have identical content");

    do {
        check ftpClient->delete(P_COPY_DST);
    } on fail {
    }
}

@test:Config {
    groups: ["ftp-client-api", "file-ops"]
}
function testExists_existingAndMissing() returns error? {
    boolean exists = check ftpClient->exists(SEED_FILE);
    test:assertTrue(exists, "exists() should return true for a known file");

    boolean notExists = check ftpClient->exists("/home/in/definitely-not-there.txt");
    test:assertFalse(notExists, "exists() should return false for a non-existent path");
}

@test:Config {
    groups: ["ftp-client-api", "file-ops"]
}
function testSize() returns error? {
    string payload = "twelve chars"; // exactly 12 bytes in UTF-8
    check ftpClient->putText(P_SIZE_TEST, payload);
    int sz = check ftpClient->size(P_SIZE_TEST);
    test:assertEquals(sz, 12, "size() should return 12 for a 12-byte file");
    do {
        check ftpClient->delete(P_SIZE_TEST);
    } on fail {
    }
}

// =============================================================================
// list
// =============================================================================

@test:Config {
    groups: ["ftp-client-api", "list"]
}
function testList_returnsFileInfo() returns error? {
    ftp:FileInfo[] files = check ftpClient->list(SEED_DIR);
    test:assertTrue(files.length() > 0, "list() should return at least one entry");

    // Spot-check: the seed file must be present.
    boolean found = false;
    foreach ftp:FileInfo fi in files {
        if fi.path == SEED_FILE {
            found = true;
        }
        test:assertTrue(fi.lastModifiedTimestamp > 0,
                "lastModifiedTimestamp must be positive for " + fi.path);
    }
    test:assertTrue(found, "list() result must include the seed file " + SEED_FILE);
}

// =============================================================================
// delete / rmdir
// =============================================================================

@test:Config {
    groups: ["ftp-client-api", "delete"]
}
function testDelete() returns error? {
    check ftpClient->putText(P_DELETE, "to-be-deleted");
    boolean before = check ftpClient->exists(P_DELETE);
    test:assertTrue(before, "File should exist before delete");

    check ftpClient->delete(P_DELETE);
    boolean after = check ftpClient->exists(P_DELETE);
    test:assertFalse(after, "File should not exist after delete");
}

@test:Config {
    groups: ["ftp-client-api", "delete"]
}
function testRmdir_emptyDirectory() returns error? {
    string dir = "/home/in/api-rmdir-empty";
    check ftpClient->mkdir(dir);
    boolean created = check ftpClient->isDirectory(dir);
    test:assertTrue(created, "Directory should exist after mkdir");

    check ftpClient->rmdir(dir);
    boolean|ftp:Error gone = ftpClient->isDirectory(dir);
    test:assertTrue(gone is ftp:Error || gone == false,
            "Directory should not exist after rmdir");
}

@test:Config {
    groups: ["ftp-client-api", "delete"]
}
function testRmdir_withSubdirectory() returns error? {
    string parent = "/home/in/api-rmdir-parent";
    string child = parent + "/child";
    check ftpClient->mkdir(parent);
    check ftpClient->mkdir(child);

    check ftpClient->rmdir(parent);
    boolean|ftp:Error gone = ftpClient->isDirectory(parent);
    test:assertTrue(gone is ftp:Error || gone == false,
            "Parent directory (with subdirectory) should not exist after rmdir");
}

@test:Config {
    groups: ["ftp-client-api", "delete"]
}
function testRmdir_withFiles() returns error? {
    string dir = "/home/in/api-rmdir-withfiles";
    check ftpClient->mkdir(dir);
    check ftpClient->putText(dir + "/f.txt", "data");

    check ftpClient->rmdir(dir);
    boolean|ftp:Error gone = ftpClient->isDirectory(dir);
    test:assertTrue(gone is ftp:Error || gone == false,
            "Directory containing files should not exist after rmdir");
}

// =============================================================================
// Close behaviour
// =============================================================================

@test:Config {
    groups: ["ftp-client-api", "close"]
}
function testClose_idempotent() returns error? {
    ftp:Client c = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}}
    });
    ftp:Error? r1 = c->close();
    test:assertEquals(r1, (), "First close should return ()");
    ftp:Error? r2 = c->close();
    test:assertEquals(r2, (), "Second close should be idempotent");
}

@test:Config {
    groups: ["ftp-client-api", "close"]
}
function testClose_thenPutApis() returns error? {
    ftp:Client c = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}}
    });
    check c->close();
    string msg = commons:CLIENT_ALREADY_CLOSED_MSG;

    ftp:Error? r1 = c->putBytes("/x.txt", []);
    test:assertTrue(isClosedError(r1, msg), "putBytes after close should return CLIENT_ALREADY_CLOSED_MSG");

    ftp:Error? r2 = c->putText("/x.txt", "v");
    test:assertTrue(isClosedError(r2, msg), "putText after close should return CLIENT_ALREADY_CLOSED_MSG");

    ftp:Error? r3 = c->putJson("/x.json", <json>{});
    test:assertTrue(isClosedError(r3, msg), "putJson after close should return CLIENT_ALREADY_CLOSED_MSG");

    ftp:Error? r4 = c->putXml("/x.xml", xml `<a/>`);
    test:assertTrue(isClosedError(r4, msg), "putXml after close should return CLIENT_ALREADY_CLOSED_MSG");

    ftp:Error? r5 = c->putCsv("/x.csv", [["a"]]);
    test:assertTrue(isClosedError(r5, msg), "putCsv after close should return CLIENT_ALREADY_CLOSED_MSG");

    byte[][] emptyChunks = [];
    ftp:Error? r6 = c->putBytesAsStream("/x.txt", emptyChunks.toStream());
    test:assertTrue(isClosedError(r6, msg), "putBytesAsStream after close should return CLIENT_ALREADY_CLOSED_MSG");

    string[][] emptyRows = [];
    ftp:Error? r7 = c->putCsvAsStream("/x.csv", emptyRows.toStream());
    test:assertTrue(isClosedError(r7, msg), "putCsvAsStream after close should return CLIENT_ALREADY_CLOSED_MSG");
}

@test:Config {
    groups: ["ftp-client-api", "close"]
}
function testClose_thenGetApis() returns error? {
    ftp:Client c = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}}
    });
    check c->close();
    string msg = commons:CLIENT_ALREADY_CLOSED_MSG;
    string p = SEED_FILE;

    byte[]|ftp:Error r1 = c->getBytes(p);
    test:assertTrue(isClosedError(r1, msg), "getBytes after close");

    string|ftp:Error r2 = c->getText(p);
    test:assertTrue(isClosedError(r2, msg), "getText after close");

    json|ftp:Error r3 = c->getJson(p);
    test:assertTrue(isClosedError(r3, msg), "getJson after close");

    xml|ftp:Error r4 = c->getXml(p);
    test:assertTrue(isClosedError(r4, msg), "getXml after close");

    string[][]|ftp:Error r5 = c->getCsv(p);
    test:assertTrue(isClosedError(r5, msg), "getCsv after close");

    stream<byte[], error?>|ftp:Error r6 = c->getBytesAsStream(p);
    test:assertTrue(isClosedError(r6, msg), "getBytesAsStream after close");

    stream<string[], error?>|ftp:Error r7 = c->getCsvAsStream(p);
    test:assertTrue(isClosedError(r7, msg), "getCsvAsStream after close");
}

@test:Config {
    groups: ["ftp-client-api", "close"]
}
function testClose_thenOtherApis() returns error? {
    ftp:Client c = check new ({
        protocol: ftp:FTP,
        host: commons:FTP_HOST,
        port: commons:AUTH_FTP_PORT,
        auth: {credentials: {username: commons:FTP_USERNAME, password: commons:FTP_PASSWORD}}
    });
    check c->close();
    string msg = commons:CLIENT_ALREADY_CLOSED_MSG;
    string p = SEED_FILE;

    ftp:Error? r1 = c->move(p, "/x.txt");
    test:assertTrue(isClosedError(r1, msg), "move after close");

    ftp:Error? r2 = c->copy(p, "/x.txt");
    test:assertTrue(isClosedError(r2, msg), "copy after close");

    ftp:Error? r3 = c->mkdir("/d");
    test:assertTrue(isClosedError(r3, msg), "mkdir after close");

    ftp:Error? r4 = c->rmdir("/d");
    test:assertTrue(isClosedError(r4, msg), "rmdir after close");

    ftp:Error? r5 = c->rename(p, "/x.txt");
    test:assertTrue(isClosedError(r5, msg), "rename after close");

    boolean|ftp:Error r6 = c->isDirectory(p);
    test:assertTrue(isClosedError(r6, msg), "isDirectory after close");

    int|ftp:Error r7 = c->size(p);
    test:assertTrue(isClosedError(r7, msg), "size after close");

    ftp:FileInfo[]|ftp:Error r8 = c->list(SEED_DIR);
    test:assertTrue(isClosedError(r8, msg), "list after close");

    ftp:Error? r9 = c->delete(p);
    test:assertTrue(isClosedError(r9, msg), "delete after close");

    boolean|ftp:Error r10 = c->exists(p);
    test:assertTrue(isClosedError(r10, msg), "exists after close");
}

// ─── Deprecated API ───────────────────────────────────────────────────────────

// append() writes content to an existing file without truncating it.
// The deprecated append() method is the only public Ballerina API for the Java
// append operation (APPEND file write mode), so it must remain tested.
const string P_APPEND = "/home/in/api-deprecated-append.txt";

@test:Config {
    groups: ["ftp-client-api", "put"]
}
function testDeprecated_append_AppendsContentToExistingFile() returns error? {
    check ftpClient->putText(P_APPEND, "hello");
    check ftpClient->append(P_APPEND, " world");
    string result = check ftpClient->getText(P_APPEND);
    test:assertEquals(result, "hello world", "append() should concatenate without truncation");
    check ftpClient->delete(P_APPEND);
}

// ─── PUT — compression ────────────────────────────────────────────────────────

@test:Config {
    groups: ["ftp-client-api", "put"]
}
function testPut_CompressedUpload_StoresZipFile() returns error? {
    // put() with compressionType=ZIP passes compressInput=true to the Java layer,
    // which calls FtpClientHelper.getCompressedMessage() and FtpUtil.compress().
    // The file is stored with a .zip extension (FtpUtil.getCompressedFileName).
    check ftpClient->put(P_PUT_COMPRESSED_SRC, "compress me", compressionType = ftp:ZIP);

    boolean|ftp:Error exists = ftpClient->exists(P_PUT_COMPRESSED_ZIP);
    test:assertTrue(exists is boolean && <boolean>exists,
            "Compressed file should be stored at the .zip path");

    check ftpClient->delete(P_PUT_COMPRESSED_ZIP);
}

// Returns true when 'result' is an ftp:Error whose message equals 'expected'.
isolated function isClosedError(any|error result, string expected) returns boolean {
    if result is ftp:Error {
        return result.message() == expected;
    }
    return false;
}
