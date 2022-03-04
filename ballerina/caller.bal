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

// FTP caller.

# Represents an FTP caller that is passed to the onFileChange function
public isolated client class Caller {
    private final Client 'client;

    # Gets invoked during object initialization.
    #
    # + 'client - Configurations for FTP caller
    # + return - `ftp:Error` in case of errors or `()` otherwise
    isolated function init(Client 'client) returns Error? {
        self.'client = 'client;
    }

    # Retrieves the file content from a remote resource.
    # ```ballerina
    # stream<byte[] & readonly, io:Error?>|ftp:Error channel = caller->get(path);
    # ```
    #
    # + path - The resource path
    # + return - A byte stream from which the file can be read or `ftp:Error` in case of errors
    remote isolated function get(string path) returns stream<byte[] & readonly, io:Error?>|Error {
        return self.'client->get(path);
    }

    # Appends the content to an existing file in an FTP server.
    # ```ballerina
    # ftp:Error? response = caller->append(path, channel);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + return - `()` or else an `ftp:Error` if failed to establish
    #            the communication with the FTP server
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
    # + compressionType - Type of the compression to be used, if
    #                     the file should be compressed before
    #                     uploading
    # + return - `()` or else an `ftp:Error` if failed to establish
    #            the communication with the FTP server
    remote isolated function put(string path, stream<byte[] & readonly, io:Error?>
            |string|xml|json content, Compression compressionType = NONE) returns Error? {
        return self.'client->put(path, content, compressionType);
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
