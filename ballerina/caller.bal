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

# Access to file operations when a file event is triggered.
# Use this within your actions to read, write, or manage files.
public isolated client class Caller {
    private final Client 'client;

    # Gets invoked during object initialization.
    #
    # + 'client - The `ftp:Client` which is used to interact with the Ftp server
    isolated function init(Client 'client) {
        self.'client = 'client;
    }

    # Read file content as a stream of byte chunks.
    # Deprecated: Use `getBytesAsStream` instead.
    # ```ballerina
    # stream<byte[] & readonly, io:Error?>|ftp:Error channel = caller->get(path);
    # ```
    #
    # + path - File or folder location on the server
    # + return - A continuous stream of byte chunks, or an error if the operation fails
    @deprecated
    remote isolated function get(string path) returns stream<byte[] & readonly, io:Error?>|Error {
        return self.'client->get(path);
    }

    # Read file content as bytes (raw binary data).
    # ```ballerina
    # byte[] content = check client->getBytes(path);
    # ```
    #
    # + path - File or folder location on the server
    # + return - File content as bytes, or an error if the operation fails
    remote isolated function getBytes(string path) returns byte[]|Error {
        return self.'client->getBytes(path);
    }

    # Read file content as text.
    # ```ballerina
    # string content = check client->getText(path);
    # ```
    #
    # + path - File or folder location on the server
    # + return - File content as text, or an error if the operation fails
    remote isolated function getText(string path) returns string|Error {
        return self.'client->getText(path);
    }

    # Read a file as JSON data.
    # ```ballerina
    # json content = check client->getJson(path);
    # ```
    #
    # + path - Location of the file on the server
    # + targetType - Format of the JSON content (JSON, or a custom json schema)
    # + return - The file content as JSON or an error if the operation fails
    remote isolated function getJson(string path, typedesc<json|record {}> targetType = <>) returns targetType|Error = @java:Method {
        name: "getJson",
        'class: "io.ballerina.stdlib.ftp.server.FtpCaller"
    } external;

    # Read a file as XML data.
    # ```ballerina
    # xml content = check client->getXml(path);
    # ```
    #
    # + path - Location of the file on the server
    # + targetType - Format of the XML content (XML, or a custom xml schema)
    # + return - The file content as XML or an error if the operation fails
    remote isolated function getXml(string path, typedesc<xml|record {}> targetType = <>) returns targetType|Error = @java:Method {
        name: "getXml",
        'class: "io.ballerina.stdlib.ftp.server.FtpCaller"
    } external;

    # Read a CSV (comma-separated) file from the server.
    # The first row of the CSV file should contain column names (headers).
    # ```ballerina
    # string[][] content = check client->getCsv(path);
    # ```
    #
    # + path - Location of the CSV file on the server
    # + targetType - Format of the CSV content (row value as string[] or structured records)
    # + return - The CSV file content as a string[][] or records[], or an error if the operation fails
    remote isolated function getCsv(string path, typedesc<string[][]|record {}[]> targetType = <>) returns targetType|Error = @java:Method {
        name: "getCsv",
        'class: "io.ballerina.stdlib.ftp.server.FtpCaller"
    } external;

    # Read file content as a stream of byte chunks.
    # Useful for processing large files without loading the entire file into memory.
    # ```ballerina
    # stream<byte[], error?> response = check client->getBytesAsStream(path);
    # ```
    #
    # + path - File or folder location on the server
    # + return - A continuous stream of byte chunks, or an error if the operation fails
    remote isolated function getBytesAsStream(string path) returns stream<byte[], error?>|Error = @java:Method {
        name: "getBytesAsStream",
        'class: "io.ballerina.stdlib.ftp.server.FtpCaller"
    } external;

    # Read a CSV file as a continuous stream of rows.
    # Useful for processing very large files one row at a time.
    # The first row of the CSV file should contain column names (headers).
    # ```ballerina
    # stream<string[], error?> response = check client->getCsvAsStream(path);
    # ```
    #
    # + path - Location of the CSV file on the server
    # + targetType - Format of the CSV content (Row values as string[] or structured record)
    # + return - A stream of rows from the CSV file, or an error if the operation fails
    remote isolated function getCsvAsStream(string path, typedesc<string[]|record {}> targetType = <>) returns stream<targetType, error?>|Error = @java:Method {
        name: "getCsvAsStream",
        'class: "io.ballerina.stdlib.ftp.server.FtpCaller"
    } external;

    # Add content to the end of an existing file.
    # Deprecated: Use `putText`, `putJson`, `putXml`, `putCsv`, `putBytes` or `putCsvAsStream` with APPEND option instead.
    # ```ballerina
    # ftp:Error? response = caller->append(path, channel);
    # ```
    #
    # + path - File location on the server
    # + content - Data to add to the file
    # + return - An error if the operation fails
    @deprecated
    remote isolated function append(string path, stream<byte[] & readonly, io:Error?>|string|xml|json content)
            returns Error? {
        return self.'client->append(path, content);
    }

    # Write content to a file (with optional compression).
    # Deprecated: Use `putText`, `putJson`, `putXml`, `putCsv`, `putBytes` or `putCsvAsStream` instead.
    # ```ballerina
    # ftp:Error? response = caller->put(path, channel);
    # ```
    #
    # + path - File location on the server
    # + content - Data to write to the file
    # + compressionType - Should the file be compressed before uploading?
    # + return - An error if the operation fails
    @deprecated
    remote isolated function put(string path, stream<byte[] & readonly, io:Error?>
            |string|xml|json content, Compression compressionType = NONE) returns Error? {
        return self.'client->put(path, content, compressionType);
    }

    # Write bytes to a file.
    # ```ballerina
    # ftp:Error? response = client->putBytes(path, content, option);
    # ```
    #
    # + path - File location on the server
    # + content - Binary data to write
    # + option - Replace or add to the file
    # + return - An error if the operation fails
    remote isolated function putBytes(string path, byte[] content, FileWriteOption option = OVERWRITE) returns Error? {
        return self.'client->putBytes(path, content, option);
    }

    # Write text to a file.
    # ```ballerina
    # ftp:Error? response = client->putText(path, content, option);
    # ```
    #
    # + path - File location on the server
    # + content - Text to write
    # + option - Replace or add to the file
    # + return - An error if the operation fails
    remote isolated function putText(string path, string content, FileWriteOption option = OVERWRITE) returns Error? {
        return self.'client->putText(path, content, option);
    }

    # Write JSON data to a file.
    # ```ballerina
    # ftp:Error? response = client->putJson(path, content, option);
    # ```
    #
    # + path - File location on the server
    # + content - JSON data to write
    # + option - Replace or add to the file
    # + return - An error if the operation fails
    remote isolated function putJson(string path, json|record {} content, FileWriteOption option = OVERWRITE) returns Error? {
        return self.'client->putJson(path, content, option);
    }

    # Write XML data to a file.
    # ```ballerina
    # ftp:Error? response = client->putXml(path, content, option);
    # ```
    #
    # + path - File location on the server
    # + content - XML data to write
    # + option - Replace or add to the file?
    # + return - An error if the operation fails
    remote isolated function putXml(string path, xml|record {} content, FileWriteOption option = OVERWRITE) returns Error? {
        return self.'client->putXml(path, content, option);
    }

    # Write CSV data to a file.
    # ```ballerina
    # ftp:Error? response = client->putCsv(path, content, option);
    # ```
    #
    # + path - File location on the server
    # + content - CSV data (string[][] or records) to write
    # + option - Replace or add to the file
    # + return - An error if the operation fails
    remote isolated function putCsv(string path, string[][]|record {}[] content, FileWriteOption option = OVERWRITE) returns Error? {
        return self.'client->putCsv(path, content, option);
    }

    # Write bytes from a stream to a file.
    # Useful for processing and uploading large files without loading everything into memory.
    # ```ballerina
    # ftp:Error? response = client->putBytesAsStream(path, content, option);
    # ```
    #
    # + path - File location on the server
    # + content - Stream of byte chunks to write
    # + option - Replace or add to the file
    # + return - An error if the operation fails
    remote isolated function putBytesAsStream(string path, stream<byte[], error?> content, FileWriteOption option = OVERWRITE) returns Error? {
        return self.'client->putBytesAsStream(path, content, option);
    }

    # Write CSV data from a stream to a file.
    # Useful for processing and uploading large CSV files without loading everything into memory.
    # ```ballerina
    # ftp:Error? response = client->putCsvAsStream(path, content, option);
    # ```
    #
    # + path - File location on the server
    # + content - Stream of CSV rows to write
    # + option - Replace or add to the file?
    # + return - An error if the operation fails
    remote isolated function putCsvAsStream(string path, stream<string[]|record {}, error?> content, FileWriteOption option = OVERWRITE) returns Error? {
        return self.'client->putCsvAsStream(path, content, option);
    }

    # Create a new folder on the file server.
    # ```ballerina
    # ftp:Error? response = caller->mkdir(path);
    # ```
    #
    # + path - The folder location to create
    # + return - An error if the operation fails
    remote isolated function mkdir(string path) returns Error? {
        return self.'client->mkdir(path);
    }

    # Remove an empty folder from the file server.
    # ```ballerina
    # ftp:Error? response = caller->rmdir(path);
    # ```
    #
    # + path - The folder to remove (must be empty)
    # + return - An error if the operation fails
    remote isolated function rmdir(string path) returns Error? {
        return self.'client->rmdir(path);
    }

    # Change the name or location of a file on the file server.
    # ```ballerina
    # ftp:Error? response = caller->rename(origin, destination);
    # ```
    #
    # + origin - The current file location
    # + destination - The new file location
    # + return - An error if the operation fails
    remote isolated function rename(string origin, string destination) returns Error? {
        return self.'client->rename(origin, destination);
    }

    # Move a file to a different location on the file server.
    # ```ballerina
    # ftp:Error? response = caller->move(sourcePath, destinationPath);
    # ```
    #
    # + sourcePath - The current file location
    # + destinationPath - The new file location
    # + return - An error if the operation fails
    remote isolated function move(string sourcePath, string destinationPath) returns Error? {
        return self.'client->move(sourcePath, destinationPath);
    }

    # Copy a file to a different location on the file server.
    # ```ballerina
    # ftp:Error? response = caller->copy(sourcePath, destinationPath);
    # ```
    #
    # + sourcePath - The file to copy
    # + destinationPath - Where to create the copy
    # + return - An error if the operation fails
    remote isolated function copy(string sourcePath, string destinationPath) returns Error? {
        return self.'client->copy(sourcePath, destinationPath);
    }

    # Check if a file or folder exists on the file server.
    # ```ballerina
    # boolean|ftp:Error response = caller->exists(path);
    # ```
    #
    # + path - File or folder location to check
    # + return - True if it exists, false if it doesn't, or an error if the check fails
    remote isolated function exists(string path) returns boolean|Error {
        return self.'client->exists(path);
    }

    # Get the size of a file.
    # ```ballerina
    # int|ftp:Error response = caller->size(path);
    # ```
    #
    # + path - File location on the server
    # + return - The file size in bytes, or an error if the operation fails
    remote isolated function size(string path) returns int|Error {
        return self.'client->size(path);
    }

    # List all files in a folder on the file server.
    # ```ballerina
    # ftp:FileInfo[]|ftp:Error response = caller->list(path);
    # ```
    #
    # + path - The folder to list
    # + return - Information about all files in the folder, or an error if the operation fails
    remote isolated function list(string path) returns FileInfo[]|Error {
        return self.'client->list(path);
    }

    # Check if a path is a folder (directory).
    # ```ballerina
    # boolean|ftp:Error response = caller->isDirectory(path);
    # ```
    #
    # + path - File or folder location to check
    # + return - True if it's a folder, false if it's a file, or an error if the check fails
    remote isolated function isDirectory(string path) returns boolean|Error {
        return self.'client->isDirectory(path);
    }

    # Remove a file from the file server.
    # ```ballerina
    # ftp:Error? response = caller->delete(path);
    # ```
    #
    # + path - The file to remove
    # + return - An error if the operation fails
    remote isolated function delete(string path) returns Error? {
        return self.'client->delete(path);
    }
}
