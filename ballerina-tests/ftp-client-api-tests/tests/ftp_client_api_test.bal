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
import ballerina/lang.'string as strings;
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
const string SEED_FILE = "/home/in/test1.txt";          // "File content" (61 bytes)
const string SEED_LARGE_FILE = "/home/in/test4.txt";    // 9000-byte file
const string SEED_DIR = "/home/in";

// Isolated working paths — each test uses its own path to stay independent.
const string P_PUT_STREAM = "/home/in/api-put-stream.txt";
const string P_PUT_BYTES = "/home/in/api-put-bytes.txt";
const string P_PUT_TEXT = "/home/in/api-put-text.txt";
const string P_PUT_JSON = "/home/in/api-put-json.json";
const string P_PUT_XML = "/home/in/api-put-xml.xml";
const string P_PUT_CSV_STR = "/home/in/api-put-csv-str.csv";
const string P_PUT_CSV_REC = "/home/in/api-put-csv-rec.csv";
const string P_PUT_CSV_STREAM_STR = "/home/in/api-csv-stream-str.csv";
const string P_PUT_CSV_STREAM_REC = "/home/in/api-csv-stream-rec.csv";
const string P_PUT_LARGE = "/home/in/api-put-large.txt";
const string P_PUT_COMPRESSED_SRC = "/home/in/api-put-compressed.txt";
const string P_PUT_COMPRESSED_ZIP = "/home/in/api-put-compressed.zip";
const string P_APPEND = "/home/in/api-append.txt";
const string P_TEXT_APPEND = "/home/in/api-text-append.txt";
const string P_CSV_APPEND = "/home/in/api-csv-append.csv";
const string P_JAILED_REL = "api-jailed-rel.txt";
const string P_JAILED_SLASH = "/api-jailed-slash.txt";
const string P_DOUBLE_SLASH = "//home/in/api-double-slash.txt";
const string P_STREAM_CLOSE_CSV = "/home/in/api-stream-close.csv";
const string P_BINDING_JSON = "/home/in/api-bind-json.txt";
const string P_BINDING_XML = "/home/in/api-bind-xml.txt";
const string P_BINDING_CSV = "/home/in/api-bind-csv.csv";
const string P_BIND_TYPED_JSON = "/home/in/api-typed-json.json";
const string P_BIND_TYPED_XML = "/home/in/api-typed-xml.xml";
const string P_BIND_TYPED_CSV = "/home/in/api-typed-csv.csv";
const string P_BIND_TYPED_CSV_STREAM = "/home/in/api-typed-csv-stream.csv";
const string P_BYTES_AS_STREAM = "/home/in/api-bytes-stream.txt";
const string P_MOVE_SRC = "/home/in/api-move-src.txt";
const string P_MOVE_DST = "/home/in/api-move-dst.txt";
const string P_COPY_DST = "/home/in/api-copy-dst.txt";
const string P_MKDIR = "/home/in/api-mkdir-test";
const string P_RENAME_OLD = "/home/in/api-rename-old";
const string P_RENAME_NEW = "/home/in/api-rename-new";
const string P_DELETE = "/home/in/api-delete.txt";

// ─── Shared clients (initialized once per suite) ──────────────────────────────

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

// ─── Suite lifecycle ──────────────────────────────────────────────────────────

@test:AfterSuite {}
function cleanupClientApiTestFiles() returns error? {
    // Best-effort cleanup; ignore errors for files that may not exist.
    string[] paths = [
        P_PUT_STREAM, P_PUT_BYTES, P_PUT_TEXT, P_PUT_JSON, P_PUT_XML,
        P_PUT_CSV_STR, P_PUT_CSV_REC, P_PUT_CSV_STREAM_STR, P_PUT_CSV_STREAM_REC,
        P_PUT_LARGE, P_PUT_COMPRESSED_ZIP, P_APPEND, P_TEXT_APPEND, P_CSV_APPEND,
        P_JAILED_SLASH, P_DOUBLE_SLASH, P_STREAM_CLOSE_CSV, P_BINDING_JSON,
        P_BINDING_XML, P_BINDING_CSV, P_BIND_TYPED_JSON, P_BIND_TYPED_XML,
        P_BIND_TYPED_CSV, P_BIND_TYPED_CSV_STREAM, P_BYTES_AS_STREAM,
        P_MOVE_DST, P_COPY_DST, P_DELETE
    ];
    foreach string p in paths {
        _ = ftpClient->delete(p);
    }
    _ = ftpJailedClient->delete(P_JAILED_REL);
    // Directories created by tests.
    foreach string d in [P_MKDIR, P_RENAME_NEW, P_RENAME_OLD] {
        _ = ftpClient->rmdir(d);
    }
}

// ─── Helper ───────────────────────────────────────────────────────────────────

isolated function readStream(stream<byte[] & readonly, io:Error?> s) returns string|error {
    string result = "";
    do {
        record {|byte[] & readonly value;|}|io:Error? chunk = s.next();
        if chunk is () || chunk is io:Error {
            break;
        }
        result += check strings:fromBytes(chunk.value);
    }
    return result;
}

// =============================================================================
// GET — read from server
// =============================================================================

// Anonymous server: read a pre-seeded file as a raw byte stream.
@test:Config {
    groups: ["ftp-client-api", "get"]
}
function testGet_fromAnonServer() returns error? {
    stream<byte[] & readonly, io:Error?> s = check anonClient->get(SEED_FILE);
    string content = check readStream(s);
    test:assertEquals(content, "File content", "Unexpected content from anon get");
    check s.close();
}

// Authenticated server: file content fits in a single read block.
@test:Config {
    groups: ["ftp-client-api", "get"]
}
function testGet_fittingContent() returns error? {
    stream<byte[] & readonly, io:Error?> s = check ftpClient->get(SEED_FILE);
    string content = check readStream(s);
    test:assertEquals(content, "File content", "Unexpected content from authenticated get");
    check s.close();
}

// Content that spans multiple internal read blocks (9000-byte file).
@test:Config {
    groups: ["ftp-client-api", "get"]
}
function testGet_nonFittingContent() returns error? {
    stream<byte[] & readonly, io:Error?> s = check ftpClient->get(SEED_LARGE_FILE);
    string content = check readStream(s);
    string expected = string:'join("", ...from int _ in 0 ..< 1000 select "123456789");
    test:assertEquals(content, expected, "Large file content mismatch");
    check s.close();
}

// =============================================================================
// PUT — write to server (stream input)
// =============================================================================

// Put a file using a local file stream and read it back as bytes.
@test:Config {
    groups: ["ftp-client-api", "put"]
}
function testPut_streamContent() returns error? {
    stream<io:Block, io:Error?> s = check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH, 5);
    check ftpClient->put(P_PUT_STREAM, s);
    stream<byte[] & readonly, io:Error?> got = check ftpClient->get(P_PUT_STREAM);
    test:assertEquals(check readStream(got), "Put content", "Stream put content mismatch");
    check got.close();
}

// Put a file that exceeds internal buffer boundaries.
@test:Config {
    groups: ["ftp-client-api", "put"]
}
function testPut_largeStreamContent() returns error? {
    byte[] block = [];
    foreach int i in 0 ..< 16390 {
        block[i] = 65; // 'A'
    }
    string blockStr = check strings:fromBytes(block);
    string expected = blockStr + "123456" + "end.";

    (byte[])[] & readonly chunks = [
        block.cloneReadOnly(),
        "123456".toBytes().cloneReadOnly(),
        "end.".toBytes().cloneReadOnly()
    ];
    check ftpClient->put(P_PUT_LARGE, chunks.toStream());

    stream<byte[] & readonly, io:Error?> got = check ftpClient->get(P_PUT_LARGE);
    test:assertEquals(check readStream(got), expected, "Large stream content mismatch");
    check got.close();
}

// PUT with ZIP compression: the server stores the compressed form.
@test:Config {
    groups: ["ftp-client-api", "put"]
}
function testPut_withCompression() returns error? {
    stream<io:Block, io:Error?> s = check io:fileReadBlocksAsStream(commons:PUT_FILE_PATH, 5);
    check ftpClient->put(P_PUT_COMPRESSED_SRC, s, compressionType = ftp:ZIP);
    // The server stores the ZIP file; verify the archive exists.
    boolean exists = check ftpClient->exists(P_PUT_COMPRESSED_ZIP);
    test:assertTrue(exists, "Compressed ZIP file should exist after put with compressionType=ZIP");
}

// =============================================================================
// PUT / GET — typed API pairs
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

    stream<byte[], error?> got = check ftpClient->getBytesAsStream(P_BYTES_AS_STREAM);
    byte[] accumulated = [];
    check from byte[] chunk in got
        do { accumulated.push(...chunk); };
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

    // getCsv returns data rows only (header excluded).
    string[][] got = check ftpClient->getCsv(P_PUT_CSV_STR);
    test:assertEquals(got, payload.slice(1), "putCsv/getCsv string rows mismatch");

    // Same rows via stream.
    stream<string[], error?> s = check ftpClient->getCsvAsStream(P_PUT_CSV_STR);
    string[][] fromStream = [];
    check from string[] row in s do { fromStream.push(row); };
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
    check from Row row in s do { fromStream.push(row); };
    test:assertEquals(fromStream, payload, "getCsvAsStream record round-trip mismatch");
}

// =============================================================================
// putBytesAsStream / putCsvAsStream
// =============================================================================

@test:Config {
    groups: ["ftp-client-api", "put", "typed"]
}
function testPutBytesAsStream() returns error? {
    (byte[])[] & readonly chunks = [
        "hello-".toBytes().cloneReadOnly(),
        "world".toBytes().cloneReadOnly()
    ];
    check ftpClient->putBytesAsStream(P_PUT_STREAM, chunks.toStream());
    string got = check ftpClient->getText(P_PUT_STREAM);
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
// PUT — content-type overloads of put()
// =============================================================================

@test:Config {
    groups: ["ftp-client-api", "put"]
}
function testPut_textContent() returns error? {
    check ftpClient->put(P_PUT_TEXT, "Sample text content");
    stream<byte[] & readonly, io:Error?> s = check ftpClient->get(P_PUT_TEXT);
    test:assertEquals(check readStream(s), "Sample text content", "put(string) content mismatch");
    check s.close();
}

@test:Config {
    groups: ["ftp-client-api", "put"]
}
function testPut_jsonContent() returns error? {
    json j = {name: "Anne", age: 20};
    check ftpClient->put(P_PUT_JSON, j);
    stream<byte[] & readonly, io:Error?> s = check ftpClient->get(P_PUT_JSON);
    test:assertEquals(check readStream(s), "{\"name\":\"Anne\", \"age\":20}", "put(json) content mismatch");
    check s.close();
}

@test:Config {
    groups: ["ftp-client-api", "put"]
}
function testPut_xmlContent() returns error? {
    xml x = xml `<note><heading>Memo</heading><body>Body</body></note>`;
    check ftpClient->put(P_PUT_XML, x);
    stream<byte[] & readonly, io:Error?> s = check ftpClient->get(P_PUT_XML);
    test:assertEquals(check readStream(s), "<note><heading>Memo</heading><body>Body</body></note>",
        "put(xml) content mismatch");
    check s.close();
}

// =============================================================================
// APPEND operation
// =============================================================================

@test:Config {
    groups: ["ftp-client-api", "append"]
}
function testAppend_streamContent() returns error? {
    // Seed the file first.
    check ftpClient->putText(P_APPEND, "Hello");

    stream<io:Block, io:Error?> extra = check io:fileReadBlocksAsStream(commons:APPEND_FILE_PATH, 7);
    check ftpClient->append(P_APPEND, extra);

    stream<byte[] & readonly, io:Error?> got = check ftpClient->get(P_APPEND);
    string content = check readStream(got);
    check got.close();
    test:assertTrue(content.startsWith("Hello"), "Appended file should start with original content");
    test:assertTrue(content.includes("Append content"), "Appended file should include new content");
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

    // CSV typed stream.
    stream<Row, error?> s1 = check ftpClient->getCsvAsStream(P_STREAM_CLOSE_CSV);
    check s1.close();

    // CSV string stream.
    stream<string[], error?> s2 = check ftpClient->getCsvAsStream(P_STREAM_CLOSE_CSV);
    check s2.close();

    // Bytes stream.
    stream<byte[], error?> s3 = check ftpClient->getBytesAsStream(P_STREAM_CLOSE_CSV);
    check s3.close();
}

// =============================================================================
// Path normalisation — userDirIsRoot
// =============================================================================

// With userDirIsRoot=true the user's FTP home becomes "/". A relative path
// such as "test1.txt" maps to the user's home + "/test1.txt".
@test:Config {
    groups: ["ftp-client-api", "path"]
}
function testUserDirIsRootTrue_get() returns error? {
    stream<byte[] & readonly, io:Error?> s = check ftpJailedClient->get("test1.txt");
    string content = check readStream(s);
    test:assertEquals(content, "File content",
        "userDirIsRoot=true: relative get should resolve from user home");
    check s.close();
}

// Relative path (no leading slash): resolved from the jailed root.
@test:Config {
    groups: ["ftp-client-api", "path"]
}
function testUserDirIsRootTrue_relativePut() returns error? {
    check ftpJailedClient->putText(P_JAILED_REL, "hello-jailed");
    stream<byte[] & readonly, io:Error?> s = check ftpJailedClient->get(P_JAILED_REL);
    test:assertEquals(check readStream(s), "hello-jailed",
        "userDirIsRoot=true: relative put/get round-trip failed");
    check s.close();
    _ = ftpJailedClient->delete(P_JAILED_REL);
}

// Relative path with a leading slash: still resolved from the jailed root.
@test:Config {
    groups: ["ftp-client-api", "path"]
}
function testUserDirIsRootTrue_relativeSlashPut() returns error? {
    check ftpJailedClient->putText(P_JAILED_SLASH, "hello-jailed-slash");
    stream<byte[] & readonly, io:Error?> s = check ftpJailedClient->get(P_JAILED_SLASH);
    test:assertEquals(check readStream(s), "hello-jailed-slash",
        "userDirIsRoot=true: leading-slash path put/get round-trip failed");
    check s.close();
    _ = ftpJailedClient->delete(P_JAILED_SLASH);
}

// A path starting with "//" is normalised to an absolute path by the library.
@test:Config {
    groups: ["ftp-client-api", "path"]
}
function testAbsoluteDoubleSlash_putGet() returns error? {
    check ftpClient->putText(P_DOUBLE_SLASH, "hello-abs-double-slash");
    stream<byte[] & readonly, io:Error?> s = check ftpClient->get(P_DOUBLE_SLASH);
    test:assertEquals(check readStream(s), "hello-abs-double-slash",
        "Double-slash absolute path put/get round-trip failed");
    check s.close();
    _ = ftpClient->delete(P_DOUBLE_SLASH);
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
    ftp:Error|json result = ftpClient->getJson(P_BINDING_JSON);
    test:assertTrue(result is ftp:ContentBindingError,
        "getJson on non-JSON content should return ContentBindingError");
    if result is ftp:ContentBindingError {
        test:assertTrue(result.detail().filePath is string,
            "ContentBindingError should carry filePath detail");
        test:assertTrue(result.detail().content is byte[],
            "ContentBindingError should carry content bytes");
    }
    _ = ftpClient->delete(P_BINDING_JSON);
}

// getXml on non-XML content must yield ContentBindingError.
@test:Config {
    groups: ["ftp-client-api", "binding"]
}
function testGetXml_invalidContent_bindsError() returns error? {
    check ftpClient->putText(P_BINDING_XML, "not valid xml <<<>>>");
    ftp:Error|xml result = ftpClient->getXml(P_BINDING_XML);
    test:assertTrue(result is ftp:ContentBindingError,
        "getXml on non-XML content should return ContentBindingError");
    if result is ftp:ContentBindingError {
        test:assertTrue(result.detail().filePath is string,
            "ContentBindingError should carry filePath detail");
    }
    _ = ftpClient->delete(P_BINDING_XML);
}

// getXml on a text file (using putText) should also fail binding.
@test:Config {
    groups: ["ftp-client-api", "binding"]
}
function testGetXml_textFile_bindsError() returns error? {
    check ftpClient->putText(P_BINDING_XML, "plain text");
    ftp:Error|xml result = ftpClient->getXml(P_BINDING_XML);
    test:assertTrue(result is ftp:Error,
        "getXml on plain-text file should return an error");
    _ = ftpClient->delete(P_BINDING_XML);
}

// getJson with a strict target type that is missing a required field.
@test:Config {
    groups: ["ftp-client-api", "binding"]
}
function testGetJson_missingField_bindsError() returns error? {
    check ftpClient->putJson(P_BIND_TYPED_JSON, {name: "Alice"}); // missing 'age'
    ftp:Error|Person result = ftpClient->getJson(P_BIND_TYPED_JSON);
    test:assertTrue(result is ftp:ContentBindingError,
        "getJson should return ContentBindingError when required field is absent");
    _ = ftpClient->delete(P_BIND_TYPED_JSON);
}

// getCsv with a strict record type when a field has the wrong data type.
@test:Config {
    groups: ["ftp-client-api", "binding"]
}
function testGetCsv_wrongType_bindsError() returns error? {
    string[][] data = [["name", "age"], ["Alice", "not-a-number"]];
    check ftpClient->putCsv(P_BINDING_CSV, data);
    ftp:Error|CsvPerson[] result = ftpClient->getCsv(P_BINDING_CSV);
    test:assertTrue(result is ftp:ContentBindingError,
        "getCsv should return ContentBindingError when field type mismatches");
    _ = ftpClient->delete(P_BINDING_CSV);
}

// getBytes / getText on a non-existent file must return an error.
@test:Config {
    groups: ["ftp-client-api", "binding"]
}
function testGetTyped_nonExistentFile() {
    string missing = "/home/in/does-not-exist-api.txt";
    test:assertTrue(ftpClient->getBytes(missing) is ftp:Error,
        "getBytes should error for non-existent file");
    test:assertTrue(ftpClient->getText(missing) is ftp:Error,
        "getText should error for non-existent file");
}

// =============================================================================
// Strict vs lax data binding
// =============================================================================

// JSON: strict binding fails on missing field; lax binding maps it to nil.
@test:Config {
    groups: ["ftp-client-api", "binding", "lax"]
}
function testJsonBinding_strictAndLax() returns error? {
    check ftpClient->putJson(P_BIND_TYPED_JSON, {name: "Alice"}); // missing 'age'

    ftp:Error|Person strictResult = ftpClient->getJson(P_BIND_TYPED_JSON);
    test:assertTrue(strictResult is ftp:Error,
        "Strict JSON binding should fail when required field is absent");

    PersonLax laxResult = check ftpLaxClient->getJson(P_BIND_TYPED_JSON);
    test:assertEquals(laxResult.name, "Alice");
    test:assertEquals(laxResult.age is (), true,
        "Lax JSON binding should map absent field to nil");

    _ = ftpClient->delete(P_BIND_TYPED_JSON);
}

// XML: strict binding fails on extra/missing fields; lax succeeds.
@test:Config {
    groups: ["ftp-client-api", "binding", "lax"]
}
function testXmlBinding_strictAndLax() returns error? {
    xml x = xml `<person><name>Alice</name><age>32</age><address>street</address></person>`;
    check ftpClient->putXml(P_BIND_TYPED_XML, x);

    // Strict: record must exactly match — extra field 'address' causes failure.
    var strictResult = ftpClient->getXml(P_BIND_TYPED_XML, Person);
    test:assertTrue(strictResult is ftp:Error,
        "Strict XML binding should fail when XML has extra fields");

    Person laxResult = check ftpLaxClient->getXml(P_BIND_TYPED_XML, Person);
    test:assertEquals(laxResult.name, "Alice");

    _ = ftpClient->delete(P_BIND_TYPED_XML);
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

    _ = ftpClient->delete(P_BIND_TYPED_CSV);
}

// CSV stream: strict stream errors on bad row; lax stream succeeds.
@test:Config {
    groups: ["ftp-client-api", "binding", "lax"]
}
function testCsvStreamBinding_strictAndLax() returns error? {
    string[][] data = [["name", "age"], ["Charlie", "30"], ["Diana", ""]];
    check ftpClient->putCsv(P_BIND_TYPED_CSV_STREAM, data, ftp:OVERWRITE);

    // Strict stream: error when consuming the row with empty 'age'.
    stream<CsvPerson, error?>|ftp:Error strictStream = ftpClient->getCsvAsStream(P_BIND_TYPED_CSV_STREAM);
    if strictStream is stream<CsvPerson, error?> {
        CsvPerson[]|error consumed = from CsvPerson row in strictStream select row;
        test:assertTrue(consumed is ftp:ContentBindingError,
            "Strict CSV stream should error on row with missing required field");
    } else {
        test:assertTrue(strictStream is ftp:ContentBindingError,
            "Strict CSV stream creation should fail with ContentBindingError");
    }

    // Lax stream: succeeds.
    stream<CsvPersonLax, error?> laxStream = check ftpLaxClient->getCsvAsStream(P_BIND_TYPED_CSV_STREAM);
    CsvPersonLax[] laxRecords = [];
    check from CsvPersonLax row in laxStream do { laxRecords.push(row); };
    test:assertEquals(laxRecords.length(), 2, "Lax CSV stream should return 2 records");

    _ = ftpClient->delete(P_BIND_TYPED_CSV_STREAM);
}

// =============================================================================
// isDirectory / mkdir / rename
// =============================================================================

@test:Config {
    groups: ["ftp-client-api", "directory"]
}
function testIsDirectory_dirAndFile() returns error? {
    // Known directory.
    boolean dirResult = check ftpClient->isDirectory(SEED_DIR);
    test:assertTrue(dirResult, "isDirectory should return true for a directory");

    // Known file.
    boolean fileResult = check ftpClient->isDirectory(SEED_FILE);
    test:assertFalse(fileResult, "isDirectory should return false for a file");
}

@test:Config {
    groups: ["ftp-client-api", "directory"]
}
function testMkdir_createAndVerify() returns error? {
    check ftpClient->mkdir(P_MKDIR);
    boolean exists = check ftpClient->isDirectory(P_MKDIR);
    test:assertTrue(exists, "mkdir should create the directory");
    // Cleanup.
    _ = ftpClient->rmdir(P_MKDIR);
}

@test:Config {
    groups: ["ftp-client-api", "directory"]
}
function testRename_directoryAndFile() returns error? {
    check ftpClient->mkdir(P_RENAME_OLD);
    check ftpClient->rename(P_RENAME_OLD, P_RENAME_NEW);

    // Old path should no longer be a directory.
    boolean|ftp:Error oldExists = ftpClient->isDirectory(P_RENAME_OLD);
    test:assertTrue(oldExists is ftp:Error || oldExists == false,
        "Original path should not exist after rename");

    // New path should be a directory.
    boolean newExists = check ftpClient->isDirectory(P_RENAME_NEW);
    test:assertTrue(newExists, "Renamed directory should exist at the new path");

    // Cleanup.
    _ = ftpClient->rmdir(P_RENAME_NEW);
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

    // Source must be gone.
    test:assertFalse(check ftpClient->exists(P_MOVE_SRC),
        "Source file should not exist after move");
    // Destination must have the original content.
    test:assertEquals(check ftpClient->getText(P_MOVE_DST), content,
        "Destination file should have the original content after move");

    _ = ftpClient->delete(P_MOVE_DST);
}

@test:Config {
    groups: ["ftp-client-api", "file-ops"]
}
function testCopy() returns error? {
    check ftpClient->copy(SEED_FILE, P_COPY_DST);

    // Both source and destination must exist.
    test:assertTrue(check ftpClient->exists(SEED_FILE),
        "Source file must still exist after copy");
    test:assertTrue(check ftpClient->exists(P_COPY_DST),
        "Destination file must exist after copy");

    // Content must match.
    string src = check ftpClient->getText(SEED_FILE);
    string dst = check ftpClient->getText(P_COPY_DST);
    test:assertEquals(src, dst, "Copied file should have identical content");

    _ = ftpClient->delete(P_COPY_DST);
}

@test:Config {
    groups: ["ftp-client-api", "file-ops"]
}
function testExists_existingAndMissing() returns error? {
    test:assertTrue(check ftpClient->exists(SEED_FILE),
        "exists() should return true for a known file");
    test:assertFalse(check ftpClient->exists("/home/in/definitely-not-there.txt"),
        "exists() should return false for a non-existent path");
}

@test:Config {
    groups: ["ftp-client-api", "file-ops"]
}
function testSize() returns error? {
    int sz = check ftpClient->size(SEED_FILE);
    test:assertEquals(sz, 61, "Size of seed file should be 61 bytes");
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

    // Spot-check: the seed file must be in the listing.
    boolean found = files.some(f => f.path == SEED_FILE);
    test:assertTrue(found, "list() result must include the seed file " + SEED_FILE);

    // FileInfo fields must be populated.
    foreach ftp:FileInfo fi in files {
        test:assertTrue(fi.lastModifiedTimestamp > 0,
            "lastModifiedTimestamp must be positive for " + fi.path);
    }
}

// =============================================================================
// delete / rmdir
// =============================================================================

@test:Config {
    groups: ["ftp-client-api", "delete"]
}
function testDelete() returns error? {
    check ftpClient->putText(P_DELETE, "to-be-deleted");
    test:assertTrue(check ftpClient->exists(P_DELETE), "File should exist before delete");

    check ftpClient->delete(P_DELETE);
    test:assertFalse(check ftpClient->exists(P_DELETE), "File should not exist after delete");
}

@test:Config {
    groups: ["ftp-client-api", "delete"]
}
function testRmdir_emptyDirectory() returns error? {
    string dir = "/home/in/api-rmdir-empty";
    check ftpClient->mkdir(dir);
    test:assertTrue(check ftpClient->isDirectory(dir), "Directory should exist after mkdir");

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
// Close-then-API: every typed API must return CLIENT_ALREADY_CLOSED_MSG
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
    // First close must succeed.
    test:assertEquals(c->close(), (), "First close should return ()");
    // Second close on an already-closed client should also succeed (idempotent).
    test:assertEquals(c->close(), (), "Second close should be idempotent");
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
    test:assertTrue(assertClosedMsg(c->put("/x.txt", "v"), msg), "put(string)");
    test:assertTrue(assertClosedMsg(c->putBytes("/x.txt", []), msg), "putBytes");
    test:assertTrue(assertClosedMsg(c->putText("/x.txt", "v"), msg), "putText");
    test:assertTrue(assertClosedMsg(c->putJson("/x.json", {}), msg), "putJson");
    test:assertTrue(assertClosedMsg(c->putXml("/x.xml", xml `<a/>`), msg), "putXml");
    test:assertTrue(assertClosedMsg(c->putCsv("/x.csv", [["a"]]), msg), "putCsv");
    stream<byte[] & readonly, io:Error?> bs = ([] : byte[][]&readonly).toStream();
    test:assertTrue(assertClosedMsg(c->putBytesAsStream("/x.txt", bs), msg), "putBytesAsStream");
    stream<string[], error?> cs = ([] : string[][]).toStream();
    test:assertTrue(assertClosedMsg(c->putCsvAsStream("/x.csv", cs), msg), "putCsvAsStream");
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
    test:assertTrue(assertClosedMsg(c->get(p), msg), "get");
    test:assertTrue(assertClosedMsg(c->getBytes(p), msg), "getBytes");
    test:assertTrue(assertClosedMsg(c->getText(p), msg), "getText");
    test:assertTrue(assertClosedMsg(c->getJson(p), msg), "getJson");
    test:assertTrue(assertClosedMsg(c->getXml(p), msg), "getXml");
    test:assertTrue(assertClosedMsg(c->getCsv(p), msg), "getCsv");
    test:assertTrue(assertClosedMsg(c->getBytesAsStream(p), msg), "getBytesAsStream");
    test:assertTrue(assertClosedMsg(c->getCsvAsStream(p), msg), "getCsvAsStream");
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
    test:assertTrue(assertClosedMsg(c->move(p, "/x.txt"), msg), "move");
    test:assertTrue(assertClosedMsg(c->copy(p, "/x.txt"), msg), "copy");
    test:assertTrue(assertClosedMsg(c->append(p, "v"), msg), "append");
    test:assertTrue(assertClosedMsg(c->mkdir("/d"), msg), "mkdir");
    test:assertTrue(assertClosedMsg(c->rmdir("/d"), msg), "rmdir");
    test:assertTrue(assertClosedMsg(c->rename(p, "/x.txt"), msg), "rename");
    test:assertTrue(assertClosedMsg(c->isDirectory(p), msg), "isDirectory");
    test:assertTrue(assertClosedMsg(c->size(p), msg), "size");
    test:assertTrue(assertClosedMsg(c->list(SEED_DIR), msg), "list");
    test:assertTrue(assertClosedMsg(c->delete(p), msg), "delete");
    test:assertTrue(assertClosedMsg(c->exists(p), msg), "exists");
}

// Returns true when the result is an ftp:Error whose message equals msg.
isolated function assertClosedMsg(any|error result, string msg) returns boolean {
    if result is ftp:Error {
        return result.message() == msg;
    }
    return false;
}
