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

import ballerina/io;
import ballerina/jballerina.java;

# FTP caller for interacting with FTP/SFTP servers from within file handlers.
# Provides the same file operations as the Client class.
public isolated client class Caller {
    private final Client 'client;

    # Gets invoked during object initialization.
    #
    # + 'client - The `ftp:Client` which is used to interact with the Ftp server
    isolated function init(Client 'client) {
        self.'client = 'client;
    }

    # Retrieves the file content from a remote resource.
    # ```ballerina
    # stream<byte[] & readonly, io:Error?>|ftp:Error channel = caller->get(path);
    # ```
    #
    # + path - The resource path
    # + return - A byte stream from which the file can be read or `ftp:Error` in case of errors
    # # Deprecated: Use the format specific get methods(`getJson`, `getXml`, `getCsv`, `getBytes`, `getText`) instead.
    @deprecated
    remote isolated function get(string path) returns stream<byte[] & readonly, io:Error?>|Error {
        return self.'client->get(path);
    }

    # Retrieves the file content as bytes from a remote resource.
    # ```ballerina
    # byte[] content = check caller->getBytes(path);
    # ```
    #
    # + path - The resource path
    # + return - Content as a byte array or `ftp:Error` in case of errors
    remote isolated function getBytes(string path) returns byte[]|Error {
        return self.'client->getBytes(path);
    }

    # Retrieves the file content as text from a remote resource.
    # ```ballerina
    # string content = check caller->getText(path);
    # ```
    #
    # + path - The resource path
    # + return - Content as a string or `ftp:Error` in case of errors
    remote isolated function getText(string path) returns string|Error {
        return self.'client->getText(path);
    }

    # Retrieves the file content as JSON from a remote resource.
    # ```ballerina
    # json content = check caller->getJson(path);
    # ```
    #
    # + path - The resource path
    # + targetType - Expected return type (to be used for automatic data binding).
    #                Supported types:
    #                - Built-in `json` type
    #                - Custom types (e.g., `User`, `Student?`, `Person[]`, etc.)
    # + return - Content as JSON or `ftp:Error` in case of errors
    remote isolated function getJson(string path, typedesc<json|record {}> targetType = <>) returns targetType|Error = @java:Method {
        name: "getJson",
        'class: "io.ballerina.stdlib.ftp.server.FtpCaller"
    } external;

    # Retrieves the file content as XML from a remote resource.
    # ```ballerina
    # xml content = check caller->getXml(path);
    # ```
    #
    # + path - The resource path
    # + targetType - Expected return type (to be used for automatic data binding).
    #                Supported types:
    #                - Built-in `xml` type
    #                - Custom types (e.g., `User`, `Student?`, `Person[]`, etc.)
    # + return - Content as XML or `ftp:Error` in case of errors
    remote isolated function getXml(string path, typedesc<xml|record {}> targetType = <>) returns targetType|Error = @java:Method {
        name: "getXml",
        'class: "io.ballerina.stdlib.ftp.server.FtpCaller"
    } external;

    # Retrieves the file content as CSV from a remote resource.
    # When the expected data type is a custom type, the first entry of the CSV file should contain matching headers.
    # ```ballerina
    # string[][] content = check caller->getCsv(path);
    # ```
    #
    # + path - The path to the file on the FTP server
    # + targetType - Expected return type (to be used for automatic data binding).
    #                Supported types:
    #                - Built-in types: `string[][]` (array of string arrays)
    #                - Custom types: (e.g., `User[]`, `Student[]`, `Person[]`, etc.)
    # + return - Content as CSV or `ftp:Error` in case of errors
    remote isolated function getCsv(string path, typedesc<string[][]|record {}[]> targetType = <>) returns targetType|Error = @java:Method {
        name: "getCsv",
        'class: "io.ballerina.stdlib.ftp.server.FtpCaller"
    } external;

    # Retrieves the file content as a byte stream from a remote resource.
    # ```ballerina
    # stream<byte[], error?> response = check caller->getBytesAsStream(path);
    # ```
    #
    # + path - The path to the file on the FTP server
    # + return - A byte stream from which the file can be read or `ftp:Error` in case of errors
    remote isolated function getBytesAsStream(string path) returns stream<byte[], error?>|Error = @java:Method {
        name: "getBytesAsStream",
        'class: "io.ballerina.stdlib.ftp.server.FtpCaller"
    } external;

    # Retrieves the file content as a CSV stream from a remote resource.
    # ```ballerina
    # stream<string[], error?> response = check caller->getCsvAsStream(path);
    # ```
    #
    # + path - The path to the file on the FTP server
    # + targetType - Expected element type (to be used for automatic data binding).
    #                Supported types:
    #                - Built-in types: `string[]` - Array of strings representing CSV columns
    #                - Custom types: (e.g., `User`, `Student?`, `Person[]`, etc.)
    # + return - A stream from which the file can be read or `ftp:Error` in case of errors
    remote isolated function getCsvAsStream(string path, typedesc<string[]|record {}> targetType = <>) returns stream<targetType, error?>|Error = @java:Method {
        name: "getCsvAsStream",
        'class: "io.ballerina.stdlib.ftp.server.FtpCaller"
    } external;

    # Appends the content to an existing file in an FTP server.
    # ```ballerina
    # ftp:Error? response = caller->append(path, channel);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + return - `()` or else an `ftp:Error` if failed to establish the communication with the FTP server
    # # Deprecated: Use the format specific put methods(`putJson`, `putXml`, `putCsv`, `putBytes`, `putText`) instead.
    @deprecated
    remote isolated function append(string path, stream<byte[] & readonly, io:Error?>|string|xml|json content)
            returns Error? {
        return self.'client->append(path, content);
    }

    # Adds a file to an FTP server.
    # ```ballerina
    # ftp:Error? response = caller->put(path, channel);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + compressionType - Type of the compression to be used if the file should be compressed before uploading
    # + return - `()` or else an `ftp:Error` if failed to establish the communication with the FTP server
    # # Deprecated: Use the format specific put methods(`putJson`, `putXml`, `putCsv`, `putBytes`, `putText`) instead.
    @deprecated
    remote isolated function put(string path, stream<byte[] & readonly, io:Error?>
            |string|xml|json content, Compression compressionType = NONE) returns Error? {
        return self.'client->put(path, content, compressionType);
    }

    # Adds a byte array as a file to an FTP server with the specified write option.
    # ```ballerina
    # ftp:Error? response = caller->putBytes(path, content, option);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + option - Write option: overwrite or append the given content
    # + return - `()` or else an `ftp:Error` if failed to write
    remote isolated function putBytes(string path, byte[] content, FileWriteOption option = OVERWRITE) returns Error? {
        return self.'client->putBytes(path, content, option);
    }

    # Adds text content to a file on an FTP server with the specified write option.
    # ```ballerina
    # ftp:Error? response = caller->putText(path, content, option);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + option - Write option: overwrite or append the given content
    # + return - `()` or else an `ftp:Error` if failed to write
    remote isolated function putText(string path, string content, FileWriteOption option = OVERWRITE) returns Error? {
        return self.'client->putText(path, content, option);
    }

    # Adds JSON content to a file on an FTP server with the specified write option.
    # ```ballerina
    # ftp:Error? response = caller->putJson(path, content, option);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + option - Write option: overwrite or append the given content
    # + return - `()` or else an `ftp:Error` if failed to write
    remote isolated function putJson(string path, json|record {} content, FileWriteOption option = OVERWRITE) returns Error? {
        return self.'client->putJson(path, content, option);
    }

    # Adds XML content to a file on an FTP server with the specified write option.
    # ```ballerina
    # ftp:Error? response = caller->putXml(path, content, option);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + option - Write option: overwrite or append the given content
    # + return - `()` or else an `ftp:Error` if failed to write
    remote isolated function putXml(string path, xml|record {} content, FileWriteOption option = OVERWRITE) returns Error? {
        return self.'client->putXml(path, content, option);
    }

    # Adds CSV content to a file on an FTP server with the specified write option.
    # ```ballerina
    # ftp:Error? response = caller->putCsv(path, content, option);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + option - Write option: overwrite or append the given content
    # + return - `()` or else an `ftp:Error` if failed to write
    remote isolated function putCsv(string path, string[][]|record {}[] content, FileWriteOption option = OVERWRITE) returns Error? {
        return self.'client->putCsv(path, content, option);
    }

    # Adds a byte stream as a file to an FTP server with the specified write option.
    # ```ballerina
    # ftp:Error? response = caller->putBytesAsStream(path, content, option);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + option - Write option: overwrite or append the given content
    # + return - `()` or else an `ftp:Error` if failed to write
    remote isolated function putBytesAsStream(string path, stream<byte[], error?> content, FileWriteOption option = OVERWRITE) returns Error? {
        return self.'client->putBytesAsStream(path, content, option);
    }

    # Adds CSV content from a stream of elements to a file on an FTP server with the specified write option.
    # ```ballerina
    # ftp:Error? response = caller->putCsvAsStream(path, content, option);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + option - Write option: overwrite or append the given content
    # + return - `()` or else an `ftp:Error` if failed to write
    remote isolated function putCsvAsStream(string path, stream<string[]|record {}, error?> content, FileWriteOption option = OVERWRITE) returns Error? {
        return self.'client->putCsvAsStream(path, content, option);
    }

    # Creates a new directory in an FTP server.
    # ```ballerina
    # ftp:Error? response = caller->mkdir(path);
    # ```
    #
    # + path - The directory path
    # + return - `()` or else an `ftp:Error` if failed to establish the communication with the FTP server
    remote isolated function mkdir(string path) returns Error? {
        return self.'client->mkdir(path);
    }

    # Deletes an empty directory in an FTP server.
    # ```ballerina
    # ftp:Error? response = caller->rmdir(path);
    # ```
    #
    # + path - The directory path
    # + return - `()` or else an `ftp:Error` if failed to establish the communication with the FTP server
    remote isolated function rmdir(string path) returns Error? {
        return self.'client->rmdir(path);
    }

    # Renames a file or directory on an FTP server or moves it to a new location.
    # ```ballerina
    # ftp:Error? response = caller->rename(origin, destination);
    # ```
    #
    # + origin - The source file location
    # + destination - The destination file location
    # + return - `()` or else an `ftp:Error` if failed to establish the communication with the FTP server
    remote isolated function rename(string origin, string destination) returns Error? {
        return self.'client->rename(origin, destination);
    }

    # Moves a file from one location to another on an FTP server.
    # ```ballerina
    # ftp:Error? response = caller->move(sourcePath, destinationPath);
    # ```
    #
    # + sourcePath - The source file location
    # + destinationPath - The destination file location
    # + return - `()` or else an `ftp:Error` if failed to establish the communication with the FTP server
    remote isolated function move(string sourcePath, string destinationPath) returns Error? {
        return self.'client->move(sourcePath, destinationPath);
    }

    # Copies a file from one location to another on an FTP server.
    # ```ballerina
    # ftp:Error? response = caller->copy(sourcePath, destinationPath);
    # ```
    #
    # + sourcePath - The source file location
    # + destinationPath - The destination file location
    # + return - `()` or else an `ftp:Error` if failed to establish the communication with the FTP server
    remote isolated function copy(string sourcePath, string destinationPath) returns Error? {
        return self.'client->copy(sourcePath, destinationPath);
    }

    # Checks if a file or directory exists on an FTP server.
    # ```ballerina
    # boolean|ftp:Error response = caller->exists(path);
    # ```
    #
    # + path - The resource path
    # + return - `true` if the file or directory exists, `false` otherwise, or an `ftp:Error` if failed to establish the communication with the FTP server
    remote isolated function exists(string path) returns boolean|Error {
        return self.'client->exists(path);
    }

    # Gets the size of a file on an FTP server.
    # ```ballerina
    # int|ftp:Error response = caller->size(path);
    # ```
    #
    # + path - The resource path
    # + return - The file size in bytes or an `ftp:Error` if failed to establish the communication with the FTP server
    remote isolated function size(string path) returns int|Error {
        return self.'client->size(path);
    }

    # Lists files and directories in a folder on an FTP server.
    # ```ballerina
    # ftp:FileInfo[]|ftp:Error response = caller->list(path);
    # ```
    #
    # + path - The directory path
    # + return - An array of FileInfo records or an `ftp:Error` if failed to establish the communication with the FTP server
    remote isolated function list(string path) returns FileInfo[]|Error {
        return self.'client->list(path);
    }

    # Checks if a given resource is a directory on an FTP server.
    # ```ballerina
    # boolean|ftp:Error response = caller->isDirectory(path);
    # ```
    #
    # + path - The resource path
    # + return - `true` if the resource is a directory, `false` otherwise, or an `ftp:Error` if the check fails
    remote isolated function isDirectory(string path) returns boolean|Error {
        return self.'client->isDirectory(path);
    }

    # Deletes a file from an FTP server.
    # ```ballerina
    # ftp:Error? response = caller->delete(path);
    # ```
    #
    # + path - The resource path
    # + return - `()` or else an `ftp:Error` if failed to establish the communication with the FTP server
    remote isolated function delete(string path) returns Error? {
        return self.'client->delete(path);
    }
}
