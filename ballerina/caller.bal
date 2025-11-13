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

# Represents an FTP caller that is passed to the onFileChange function
public isolated client class Caller {
    private final Client 'client;

    # Gets invoked during object initialization.
    #
    # + 'client - The `ftp:Client` which is used to interact with the Ftp server
    isolated function init(Client 'client) {
        self.'client = 'client;
    }

    # Retrieves the file content from a remote resource.
    # Deprecated: Use `get` function of `ftp:Client` instead.
    # ```ballerina
    # stream<byte[] & readonly, io:Error?>|ftp:Error channel = caller->get(path);
    # ```
    #
    # + path - The resource path
    # + return - A byte stream from which the file can be read or `ftp:Error` in case of errors
    @deprecated
    remote isolated function get(string path) returns stream<byte[] & readonly, io:Error?>|Error {
        return self.'client->get(path);
    }

    # Retrieves the file content as bytes from a remote resource.
    # ```ballerina
    # byte[] content = check client->getBytes(path);
    # ```
    #
    # + path - The resource path
    # + return - content as a byte array or `ftp:Error` in case of errors
    remote isolated function getBytes(string path) returns byte[]|Error {
        return self.'client->getBytes(path);
    }

    # Retrieves the file content as text from a remote resource.
    # ```ballerina
    # string content = check client->getText(path);
    # ```
    #
    # + path - The resource path
    # + return - content as a string or `ftp:Error` in case of errors
    remote isolated function getText(string path) returns string|Error {
        return self.'client->getText(path);
    }

    # Retrieves the file content as json from a remote resource.
    # ```ballerina
    # json content = check client->getJson(path);
    # ```
    #
    # + path - The resource path
    # + targetType - The target type of the json content
    #                 (e.g., `json`, `record {}`, or a user-defined record type)
    # + return - content as a json or `ftp:Error` in case of errors
    remote isolated function getJson(string path, typedesc<json|record {}> targetType = <>) returns targetType|Error = @java:Method {
        name: "getJson",
        'class: "io.ballerina.stdlib.ftp.server.FtpCaller"
    } external;

    # Retrieves the file content as xml from a remote resource.
    # ```ballerina
    # xml content = check client->getXml(path);
    # ```
    #
    # + path - The resource path
    # + targetType - The target type of the xml content
    #                 (e.g., `xml`, `record {}`, or a user-defined record type)
    # + return - content as a xml or `ftp:Error` in case of errors
    remote isolated function getXml(string path, typedesc<xml|record {}> targetType = <>) returns targetType|Error = @java:Method {
        name: "getXml",
        'class: "io.ballerina.stdlib.ftp.server.FtpCaller"
    } external;

    # Fetches file content from the FTP server as CSV.
    # When the expected data type is record[], the first entry of the csv file should contain matching headers.
    # ```ballerina
    # string[][] content = check client->getCsv(path);
    # ```
    #
    # + path - The path to the file on the FTP server
    # + targetType - The target type of the CSV content
    #                 (e.g., `string[][]`, `record {}[]`, or an array of user-defined record type)
    # + return - content as a string[][] or `ftp:Error` in case of errors
    remote isolated function getCsv(string path, typedesc<string[][]|record {}[]> targetType = <>) returns targetType|Error = @java:Method {
        name: "getCsv",
        'class: "io.ballerina.stdlib.ftp.server.FtpCaller"
    } external;

    # Retrieves the file content as a byte stream from a remote resource.
    # ```ballerina
    # stream<byte[], error?> response = check client->getBytesAsStream(path);
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
    # stream<string[], error?> response = check client->getCsvAsStream(path);
    # ```
    #
    # + path - The path to the file on the FTP server
    # + targetType - The target element type of the stream (e.g., `string[]` or `record {}`)
    # + return - A stream from which the file can be read or `ftp:Error` in case of errors
    remote isolated function getCsvAsStream(string path, typedesc<string[]|record {}> targetType = <>) returns stream<targetType, error?>|Error = @java:Method {
        name: "getCsvAsStream",
        'class: "io.ballerina.stdlib.ftp.server.FtpCaller"
    } external;

    # Appends the content to an existing file in an FTP server.
    # Deprecated: Use `putText`, `putJson`, `putXml`, `putCsv`, `putBytes` or `putCsvAsStream`.
    # ```ballerina
    # ftp:Error? response = caller->append(path, channel);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + return - `()` or else an `ftp:Error` if failed to establish
    #            the communication with the FTP server
    @deprecated
    remote isolated function append(string path, stream<byte[] & readonly, io:Error?>|string|xml|json content)
            returns Error? {
        return self.'client->append(path, content);
    }

    # Adds a file to an FTP server.
    # Deprecated: Use `putText`, `putJson`, `putXml`, `putCsv`, `putBytes` or `putCsvAsStream`.
    # ```ballerina
    # ftp:Error? response = caller->put(path, channel);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + compressionType - Type of the compression to be used, if
    #                     the file should be compressed before
    #                     uploading
    # + return - `()` or else an `ftp:Error` if failed to establish
    #            the communication with the FTP server
    @deprecated
    remote isolated function put(string path, stream<byte[] & readonly, io:Error?>
            |string|xml|json content, Compression compressionType = NONE) returns Error? {
        return self.'client->put(path, content, compressionType);
    }

    # Adds a byte array as a file to an FTP server with the specified write option.
    # ```ballerina
    # ftp:Error? response = client->putBytes(path, content, option);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + option - To indicate whether to overwrite or append the given content
    # + return - `()` or else an `ftp:Error` if failed to write
    remote isolated function putBytes(string path, byte[] content, FileWriteOption option = OVERWRITE) returns Error? {
        return self.'client->putBytes(path, content, option);
    }

    # Adds a file to an FTP server with the specified write option.
    # ```ballerina
    # ftp:Error? response = client->putText(path, content, option);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + option - To indicate whether to overwrite or append the given content
    # + return - `()` or else an `ftp:Error` if failed to write
    remote isolated function putText(string path, string content, FileWriteOption option = OVERWRITE) returns Error? {
        return self.'client->putText(path, content, option);
    }

    # Adds a file to an FTP server with the specified write option.
    # ```ballerina
    # ftp:Error? response = client->putJson(path, content, option);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + option - To indicate whether to overwrite or append the given content
    # + return - `()` or else an `ftp:Error` if failed to write
    remote isolated function putJson(string path, json|record {} content, FileWriteOption option = OVERWRITE) returns Error? {
        return self.'client->putJson(path, content, option);
    }

    # Adds a file to an FTP server with the specified write option.
    # ```ballerina
    # ftp:Error? response = client->putXml(path, content, option);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + option - To indicate whether to overwrite or append the given content
    # + return - `()` or else an `ftp:Error` if failed to write
    remote isolated function putXml(string path, xml|record {} content, FileWriteOption option = OVERWRITE) returns Error? {
        return self.'client->putXml(path, content, option);
    }

    # Adds a CSV file to an FTP server with the specified write option.
    # ```ballerina
    # ftp:Error? response = client->putCsv(path, content, option);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + option - To indicate whether to overwrite or append the given content
    # + return - `()` or else an `ftp:Error` if failed to write
    remote isolated function putCsv(string path, string[][]|record {}[] content, FileWriteOption option = OVERWRITE) returns Error? {
        return self.'client->putCsv(path, content, option);
    }

    # Adds a byte[] stream as a file to an FTP server with the specified write option.
    # ```ballerina
    # ftp:Error? response = client->putBytesAsStream(path, content, option);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + option - To indicate whether to overwrite or append the given content
    # + return - `()` or else an `ftp:Error` if failed to write
    remote isolated function putBytesAsStream(string path, stream<byte[], error?> content, FileWriteOption option = OVERWRITE) returns Error? {
        return self.'client->putBytesAsStream(path, content, option);
    }

    # Adds a CSV file from string[][] or record{}[] elements as a file to an FTP server with the specified write option.
    # ```ballerina
    # ftp:Error? response = client->putCsvAsStream(path, content, option);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + option - To indicate whether to overwrite or append the given content
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
    # + return - `()` or else an `ftp:Error` if failed to establish
    #            the communication with the FTP server
    remote isolated function mkdir(string path) returns Error? {
        return self.'client->mkdir(path);
    }

    # Deletes an empty directory in an FTP server.
    # ```ballerina
    # ftp:Error? response = caller->rmdir(path);
    # ```
    #
    # + path - The directory path
    # + return - `()` or else an `ftp:Error` if failed to establish
    #            the communication with the FTP server
    remote isolated function rmdir(string path) returns Error? {
        return self.'client->rmdir(path);
    }

    # Renames a file or moves it to a new location within
    # the same FTP server.
    # ```ballerina
    # ftp:Error? response = caller->rename(origin, destination);
    # ```
    #
    # + origin - The source file location
    # + destination - The destination file location
    # + return - `()` or else an `ftp:Error` if failed to establish
    #            the communication with the FTP server
    remote isolated function rename(string origin, string destination) returns Error? {
        return self.'client->rename(origin, destination);
    }

    # Gets the size of a file resource.
    # ```ballerina
    # int|ftp:Error response = caller->size(path);
    # ```
    #
    # + path - The resource path
    # + return - The file size in bytes or an `ftp:Error` if
    #            failed to establish the communication with the FTP server
    remote isolated function size(string path) returns int|Error {
        return self.'client->size(path);
    }

    # Gets the file name list in a given folder.
    # ```ballerina
    # ftp:FileInfo[]|ftp:Error response = caller->list(path);
    # ```
    #
    # + path - The directory path
    # + return - An array of file names or an `ftp:Error` if failed to
    #            establish the communication with the FTP server
    remote isolated function list(string path) returns FileInfo[]|Error {
        return self.'client->list(path);
    }

    # Checks if a given resource is a directory.
    # ```ballerina
    # boolean|ftp:Error response = caller->isDirectory(path);
    # ```
    #
    # + path - The resource path
    # + return - `true` if given resource is a directory or an `ftp:Error` if
    #            an error occurred while checking the path
    remote isolated function isDirectory(string path) returns boolean|Error {
        return self.'client->isDirectory(path);
    }

    # Deletes a file from an FTP server.
    # ```ballerina
    # ftp:Error? response = caller->delete(path);
    # ```
    #
    # + path - The resource path
    # + return - `()` or else an `ftp:Error` if failed to establish
    #             the communication with the FTP server
    remote isolated function delete(string path) returns Error? {
        return self.'client->delete(path);
    }
}
