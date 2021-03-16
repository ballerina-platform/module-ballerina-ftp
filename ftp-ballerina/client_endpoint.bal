// Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

// FTP Client Endpoint

import ballerina/io;
import ballerina/log;
import ballerina/jballerina.java;

# Represents an FTP client that intracts with an FTP server
public client class Client {
    private ClientEndpointConfig config = {};

    # Gets invoked during object initialization.
    #
    # + clientConfig - Configurations for FTP client endpoint
    public isolated function init(ClientEndpointConfig clientConfig) {
        self.config = clientConfig;
        Error? response = initEndpoint(self, self.config);
        if (response is Error) {
            log:printError("Invalid config provided");
            panic response;
        }
    }

    # The `get()` function can be used to retrieve file content from a remote
    # resource.
    # ```ballerina
    # io:ReadableByteChannel|Error channel = client->get(path);
    # ```
    #
    # + path - The resource path
    # + return - A ReadableByteChannel that represents the data source to the
    #            resource or an `Error` if failed to establish communication
    #            with the FTP server or read the resource
    remote isolated function get(string path) returns io:ReadableByteChannel|Error {
        handle resourcePath = java:fromString(path);
        return get(self, resourcePath);
    }

    # The `append()` function can be used to append content to an existing file
    # in an FTP server.
    # ```ballerina
    # Error? response = client->append(path, channel);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + return - An `Error` if failed to establish communication with the FTP
    #            server
    remote isolated function append(string path, io:ReadableByteChannel|string|xml|json content) returns Error? {
        return append(self, getInputContent(path, content));
    }

    # The `put()` function can be used to add a file to an FTP server.
    # ```ballerina
    # Error? response = client->put(path, channel);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + compressInput - True if file should be compressed before uploading
    # + return - An `Error` if failed to establish communication with the FTP
    #            server
    remote isolated function put(string path, io:ReadableByteChannel|string|xml|json content,
                                                                boolean compressInput=false) returns Error? {
        return put(self, getInputContent(path, content, compressInput));
    }

    # The `mkdir()` function can be used to create a new direcotry in an FTP
    # server.
    # ```ballerina
    # Error? response = client->mkdir(path);
    # ```
    #
    # + path - The directory path
    # + return - An `Error` if failed to establish communication with the FTP
    #            server
    remote isolated function mkdir(string path) returns Error? {
        handle resourcePath = java:fromString(path);
        return mkdir(self, resourcePath);
    }

    # The `rmdir()` function can be used to delete an empty directory in an FTP
    # server.
    # ```ballerina
    # Error? response = client->rmdir(path);
    # ```
    #
    # + path - The directory path
    # + return - An `Error` if failed to establish communication with the FTP
    #            server
    remote isolated function rmdir(string path) returns Error? {
        handle resourcePath = java:fromString(path);
        return rmdir(self, resourcePath);
    }

    # The `rename()` function can be used to rename a file or move to a new
    # location within the same FTP server.
    # ```ballerina
    # Error? response = client->rename(origin, destination);
    # ```
    #
    # + origin - The source file location
    # + destination - The destination file location
    # + return - An `Error` if failed to establish communication with the FTP
    #            server
    remote isolated function rename(string origin, string destination) returns Error? {
        handle originPath = java:fromString(origin);
        handle destinationPath = java:fromString(destination);
        return rename(self, originPath, destinationPath);
    }

    # The `size()` function can be used to get the size of a file resource.
    # ```ballerina
    # int|Error response = client->size(path);
    # ```
    #
    # + path - The resource path
    # + return - The file size in bytes or an `Error` if failed to establish
    #            communication with the FTP server
    remote isolated function size(string path) returns int|Error {
        handle resourcePath = java:fromString(path);
        return size(self, resourcePath);
    }

    # The `list()` function can be used to get the file name list in a given
    # folder.
    # ```ballerina
    # ftp:FileInfo[]|Error response = client->list(path);
    # ```
    #
    # + path - The direcotry path
    # + return - An array of file names or an `Error` if failed to establish
    #            communication with the FTP server
    remote isolated function list(string path) returns FileInfo[]|Error {
        handle resourcePath = java:fromString(path);
        return list(self, resourcePath);
    }

    # The `isDirectory()` function can be used to check if a given resource is a
    # direcotry.
    # ```ballerina
    # boolean|Error response = client->isDirectory(path);
    # ```
    #
    # + path - The resource path
    # + return - true if given resource is a direcotry or an `Error` if failed
    #            to establish communication with the FTP server
    remote isolated function isDirectory(string path) returns boolean|Error {
        handle resourcePath = java:fromString(path);
        return isDirectory(self, resourcePath);
    }

    # The `delete()` function can be used to delete a file from an FTP server.
    # ```ballerina
    # Error? response = client->delete(path);
    # ```
    #
    # + path - The resource path
    # + return -  An `Error` if failed to establish communication with the FTP
    #             server
    remote isolated function delete(string path) returns Error? {
        handle resourcePath = java:fromString(path);
        return delete(self, resourcePath);
    }
}

# Configuration for FTP client endpoint.
#
# + protocol - Supported FTP protocols
# + host - Target service URL
# + port - Port number of the remote service
# + secureSocket - Authenthication options
public type ClientEndpointConfig record {|
    Protocol protocol = FTP;
    string host = "127.0.0.1";
    int? port = 21;
    SecureSocket? secureSocket = ();
|};

isolated function getInputContent(string path, io:ReadableByteChannel|string|xml|json content, boolean compressInput=false) returns InputContent{
    InputContent inputContent = {
        filePath: path,
        compressInput: compressInput
    };

    if(content is io:ReadableByteChannel){
        inputContent.isFile = true;
        inputContent.fileContent = content;
    } else if(content is string){
        inputContent.textContent = content;
    } else if(content is json){
        inputContent.textContent = content.toJsonString();
    } else {
        inputContent.textContent = content.toString();
    }
    return inputContent;
}
