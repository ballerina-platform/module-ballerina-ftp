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

// FTP caller.

import ballerina/io;

# Represents an FTP caller that is passed to the onFileChange function
public isolated client class Caller {
    private final readonly & ClientConfiguration config;

    # Gets invoked during object initialization.
    #
    # + clientConfig - Configurations for FTP client
    # + return - `ftp:Error` in case of errors or `()` otherwise
    isolated function init(ClientConfiguration clientConfig) returns Error? {
        self.config = clientConfig.cloneReadOnly();
        Error? response = initEndpoint(self, self.config);
        if response is Error {
            return response;
        }
    }

    # Retrieves the file content from a remote resource.
    # ```ballerina
    # stream<byte[] & readonly, io:Error?>|ftp:Error channel = client->get(path);
    # ```
    #
    # + path - The resource path
    # + return - A byte stream from which the file can be read or `ftp:Error` in case of errors
    remote isolated function get(string path) returns stream<byte[] & readonly, io:Error?>|Error {
        ByteStream|Error byteStream = new (self, path);
        if byteStream is ByteStream {
            return new stream<byte[] & readonly, io:Error?>(byteStream);
        } else {
            return byteStream;
        }

    }

    # Appends the content to an existing file in an FTP server.
    # ```ballerina
    # ftp:Error? response = client->append(path, channel);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + return - `()` or else an `ftp:Error` if failed to establish
    #            the communication with the FTP server
    remote isolated function append(string path, stream<byte[] & readonly, io:Error?>|string|xml|json content)
            returns Error? {
        return append(self, getInputContent(path, content));
    }

    # Adds a file to an FTP server.
    # ```ballerina
    # ftp:Error? response = client->put(path, channel);
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
        boolean compress = false;
        if compressionType != NONE {
            compress = true;
        }
        return put(self, getInputContent(path, content, compress));
    }

    # Creates a new directory in an FTP server.
    # ```ballerina
    # ftp:Error? response = client->mkdir(path);
    # ```
    #
    # + path - The directory path
    # + return - `()` or else an `ftp:Error` if failed to establish
    #            the communication with the FTP server
    remote isolated function mkdir(string path) returns Error? {
        return mkdir(self, path);
    }

    # Deletes an empty directory in an FTP server.
    # ```ballerina
    # ftp:Error? response = client->rmdir(path);
    # ```
    #
    # + path - The directory path
    # + return - `()` or else an `ftp:Error` if failed to establish
    #            the communication with the FTP server
    remote isolated function rmdir(string path) returns Error? {
        return rmdir(self, path);
    }

    # Renames a file or moves it to a new location within
    # the same FTP server.
    # ```ballerina
    # ftp:Error? response = client->rename(origin, destination);
    # ```
    #
    # + origin - The source file location
    # + destination - The destination file location
    # + return - `()` or else an `ftp:Error` if failed to establish
    #            the communication with the FTP server
    remote isolated function rename(string origin, string destination) returns Error? {
        return rename(self, origin, destination);
    }

    # Gets the size of a file resource.
    # ```ballerina
    # int|ftp:Error response = client->size(path);
    # ```
    #
    # + path - The resource path
    # + return - The file size in bytes or an `ftp:Error` if
    #            failed to establish the communication with the FTP server
    remote isolated function size(string path) returns int|Error {
        return size(self, path);
    }

    # Gets the file name list in a given folder.
    # ```ballerina
    # ftp:FileInfo[]|ftp:Error response = client->list(path);
    # ```
    #
    # + path - The directory path
    # + return - An array of file names or an `ftp:Error` if failed to
    #            establish the communication with the FTP server
    remote isolated function list(string path) returns FileInfo[]|Error {
        return list(self, path);
    }

    # Checks if a given resource is a directory.
    # ```ballerina
    # boolean|ftp:Error response = client->isDirectory(path);
    # ```
    #
    # + path - The resource path
    # + return - `true` if given resource is a directory or an `ftp:Error` if
    #            an error occurred while checking the path
    remote isolated function isDirectory(string path) returns boolean|Error {
        return isDirectory(self, path);
    }

    # Deletes a file from an FTP server.
    # ```ballerina
    # ftp:Error? response = client->delete(path);
    # ```
    #
    # + path - The resource path
    # + return - `()` or else an `ftp:Error` if failed to establish
    #             the communication with the FTP server
    remote isolated function delete(string path) returns Error? {
        return delete(self, path);
    }
}
