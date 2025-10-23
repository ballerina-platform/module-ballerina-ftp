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

import ballerina/data.csv as _;
import ballerina/data.jsondata as _;
import ballerina/data.xmldata;
import ballerina/io;
import ballerina/jballerina.java;

# FTP client for connecting to and managing FTP/SFTP servers.
# Supports reading files in various formats (text, JSON, XML, CSV, bytes) and writing files with overwrite or append options.
public isolated client class Client {
    private final readonly & ClientConfiguration config;

    # Gets invoked during object initialization.
    #
    # + clientConfig - Configurations for FTP client
    # + return - `ftp:Error` in case of errors or `()` otherwise
    public isolated function init(ClientConfiguration clientConfig) returns Error? {
        self.config = clientConfig.cloneReadOnly();
        Error? response = initEndpoint(self, self.config);
        if response is Error {
            return response;
        }
    }

    # Retrieves the file content from a remote resource.
    # Deprecated: Use the format specific get methods(`getJson`, `getXml`, `getCsv`, `getBytes`, `getText`) instead.
    # ```ballerina
    # stream<byte[] & readonly, io:Error?>|ftp:Error channel = client->get(path);
    # ```
    #
    # + path - The resource path
    # + return - A byte stream from which the file can be read or `ftp:Error` in case of errors
    @deprecated
    remote isolated function get(string path) returns stream<byte[] & readonly, io:Error?>|Error {
        ByteStream|Error byteStream = new (self, path);
        if byteStream is ByteStream {
            return new stream<byte[] & readonly, io:Error?>(byteStream);
        } else {
            return byteStream;
        }

    }

    # Retrieves the file content as bytes from a remote resource.
    # ```ballerina
    # byte[] content = check client->getBytes(path);
    # ```
    #
    # + path - The resource path
    # + return - Content as a byte array or `ftp:Error` in case of errors
    remote isolated function getBytes(string path) returns byte[]|Error {
        return getBytes(self, path);
    }

    # Retrieves the file content as text from a remote resource.
    # ```ballerina
    # string content = check client->getText(path);
    # ```
    #
    # + path - The resource path
    # + return - Content as a string or `ftp:Error` in case of errors
    remote isolated function getText(string path) returns string|Error {
        return getText(self, path);
    }

    # Retrieves the file content as JSON from a remote resource.
    # ```ballerina
    # json content = check client->getJson(path);
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
        'class: "io.ballerina.stdlib.ftp.client.FtpClient"
    } external;

    # Retrieves the file content as XML from a remote resource.
    # ```ballerina
    # xml content = check client->getXml(path);
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
        'class: "io.ballerina.stdlib.ftp.client.FtpClient"
    } external;

    # Retrieves the file content as CSV from a remote resource.
    # When the expected data type is a custom type, the first entry of the CSV file should contain matching headers.
    # ```ballerina
    # string[][] content = check client->getCsv(path);
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
        'class: "io.ballerina.stdlib.ftp.client.FtpClient"
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
        'class: "io.ballerina.stdlib.ftp.client.FtpClient"
    } external;

    # Retrieves the file content as a CSV stream from a remote resource.
    # ```ballerina
    # stream<string[], error?> response = check client->getCsvAsStream(path);
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
        'class: "io.ballerina.stdlib.ftp.client.FtpClient"
    } external;

    # Appends the content to an existing file in an FTP server.
    # Deprecated: Use the format specific put methods(`putJson`, `putXml`, `putCsv`, `putBytes`, `putText`) instead.
    # ```ballerina
    # ftp:Error? response = client->append(path, channel);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + return - `()` or else an `ftp:Error` if failed to establish the communication with the FTP server
    @deprecated
    remote isolated function append(string path, stream<byte[] & readonly, io:Error?>|string|xml|json content)
            returns Error? {
        return append(self, getInputContent(path, content));
    }

    # Adds a file to an FTP server.
    # Deprecated: Use the format specific put methods(`putJson`, `putXml`, `putCsv`, `putBytes`, `putText`) instead.
    # ```ballerina
    # ftp:Error? response = client->put(path, channel);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + compressionType - Type of the compression to be used if the file should be compressed before uploading
    # + return - `()` or else an `ftp:Error` if failed to establish the communication with the FTP server
    @deprecated
    remote isolated function put(string path, stream<byte[] & readonly, io:Error?>
            |string|xml|json content, Compression compressionType = NONE) returns Error? {
        boolean compress = false;
        if compressionType != NONE {
            compress = true;
        }
        return put(self, getInputContent(path, content, compress));
    }

    # Adds a byte array as a file to an FTP server with the specified write option.
    # ```ballerina
    # ftp:Error? response = client->putBytes(path, content, option);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + option - Write option: overwrite or append the given content
    # + return - `()` or else an `ftp:Error` if failed to write
    remote isolated function putBytes(string path, byte[] content, FileWriteOption option = OVERWRITE) returns Error? = @java:Method {
        name: "putBytes",
        'class: "io.ballerina.stdlib.ftp.client.FtpClient"
    } external;

    # Adds text content to a file on an FTP server with the specified write option.
    # ```ballerina
    # ftp:Error? response = client->putText(path, content, option);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + option - Write option: overwrite or append the given content
    # + return - `()` or else an `ftp:Error` if failed to write
    remote isolated function putText(string path, string content, FileWriteOption option = OVERWRITE) returns Error? = @java:Method {
        name: "putText",
        'class: "io.ballerina.stdlib.ftp.client.FtpClient"
    } external;

    # Adds JSON content to a file on an FTP server with the specified write option.
    # ```ballerina
    # ftp:Error? response = client->putJson(path, content, option);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + option - Write option: overwrite or append the given content
    # + return - `()` or else an `ftp:Error` if failed to write
    remote isolated function putJson(string path, json|record {} content, FileWriteOption option = OVERWRITE) returns Error? {
        return putJson(self, path, content.toJsonString(), option);
    }

    # Adds XML content to a file on an FTP server with the specified write option.
    # ```ballerina
    # ftp:Error? response = client->putXml(path, content, option);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + option - Write option: overwrite or append the given content
    # + return - `()` or else an `ftp:Error` if failed to write
    remote isolated function putXml(string path, xml|record {} content, FileWriteOption option = OVERWRITE) returns Error? {
        xml|error xmldata = content is xml ? content : xmldata:toXml(content);
        if xmldata is error {
            return error Error("Failed to convert record to XML: " + xmldata.message());
        }
        return putXml(self, path, xmldata, option);
    }

    # Adds CSV content to a file on an FTP server with the specified write option.
    # ```ballerina
    # ftp:Error? response = client->putCsv(path, content, option);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + option - Write option: overwrite or append the given content
    # + return - `()` or else an `ftp:Error` if failed to write
    remote isolated function putCsv(string path, string[][]|record {}[] content, FileWriteOption option = OVERWRITE) returns Error? {
        return putCsv(self, path, content, option);
    }

    # Adds a byte stream as a file to an FTP server with the specified write option.
    # ```ballerina
    # ftp:Error? response = client->putBytesAsStream(path, content, option);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + option - Write option: overwrite or append the given content
    # + return - `()` or else an `ftp:Error` if failed to write
    remote isolated function putBytesAsStream(string path, stream<byte[], error?> content, FileWriteOption option = OVERWRITE) returns Error? = @java:Method {
        name: "putBytesAsStream",
        'class: "io.ballerina.stdlib.ftp.client.FtpClient"
    } external;

    # Adds CSV content from a stream of elements to a file on an FTP server with the specified write option.
    # ```ballerina
    # ftp:Error? response = client->putCsvAsStream(path, content, option);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + option - Write option: overwrite or append the given content
    # + return - `()` or else an `ftp:Error` if failed to write
    remote isolated function putCsvAsStream(string path, stream<string[]|record {}, error?> content, FileWriteOption option = OVERWRITE) returns Error? = @java:Method {
        name: "putCsvAsStream",
        'class: "io.ballerina.stdlib.ftp.client.FtpClient"
    } external;

    # Creates a new directory in an FTP server.
    # ```ballerina
    # ftp:Error? response = client->mkdir(path);
    # ```
    #
    # + path - The directory path
    # + return - `()` or else an `ftp:Error` if failed to establish the communication with the FTP server
    remote isolated function mkdir(string path) returns Error? {
        return mkdir(self, path);
    }

    # Deletes an empty directory in an FTP server.
    # ```ballerina
    # ftp:Error? response = client->rmdir(path);
    # ```
    #
    # + path - The directory path
    # + return - `()` or else an `ftp:Error` if failed to establish the communication with the FTP server
    remote isolated function rmdir(string path) returns Error? {
        return rmdir(self, path);
    }

    # Renames a file or directory on an FTP server or moves it to a new location.
    # ```ballerina
    # ftp:Error? response = client->rename(origin, destination);
    # ```
    #
    # + origin - The source file location
    # + destination - The destination file location
    # + return - `()` or else an `ftp:Error` if failed to establish the communication with the FTP server
    remote isolated function rename(string origin, string destination) returns Error? {
        return rename(self, origin, destination);
    }

    # Moves a file from one location to another on an FTP server.
    # ```ballerina
    # ftp:Error? response = client->move(sourcePath, destinationPath);
    # ```
    #
    # + sourcePath - The source file location
    # + destinationPath - The destination file location
    # + return - `()` or else an `ftp:Error` if failed to establish the communication with the FTP server
    remote isolated function move(string sourcePath, string destinationPath) returns Error? {
        return move(self, sourcePath, destinationPath);
    }

    # Copies a file from one location to another on an FTP server.
    # ```ballerina
    # ftp:Error? response = client->copy(sourcePath, destinationPath);
    # ```
    #
    # + sourcePath - The source file location
    # + destinationPath - The destination file location
    # + return - `()` or else an `ftp:Error` if failed to establish the communication with the FTP server
    remote isolated function copy(string sourcePath, string destinationPath) returns Error? {
        return copy(self, sourcePath, destinationPath);
    }

    # Checks if a file or directory exists on an FTP server.
    # ```ballerina
    # boolean|ftp:Error response = client->exists(path);
    # ```
    #
    # + path - The resource path
    # + return - `true` if the file or directory exists, `false` otherwise, or an `ftp:Error` if failed to establish the communication with the FTP server
    remote isolated function exists(string path) returns boolean|Error {
        return exists(self, path);
    }

    # Gets the size of a file on an FTP server.
    # ```ballerina
    # int|ftp:Error response = client->size(path);
    # ```
    #
    # + path - The resource path
    # + return - The file size in bytes or an `ftp:Error` if failed to establish the communication with the FTP server
    remote isolated function size(string path) returns int|Error {
        return size(self, path);
    }

    # Lists files and directories in a folder on an FTP server.
    # ```ballerina
    # ftp:FileInfo[]|ftp:Error response = client->list(path);
    # ```
    #
    # + path - The directory path
    # + return - An array of FileInfo records or an `ftp:Error` if failed to establish the communication with the FTP server
    remote isolated function list(string path) returns FileInfo[]|Error {
        return list(self, path);
    }

    # Checks if a given resource is a directory on an FTP server.
    # ```ballerina
    # boolean|ftp:Error response = client->isDirectory(path);
    # ```
    #
    # + path - The resource path
    # + return - `true` if the resource is a directory, `false` otherwise, or an `ftp:Error` if the check fails
    remote isolated function isDirectory(string path) returns boolean|Error {
        return isDirectory(self, path);
    }

    # Deletes a file from an FTP server.
    # ```ballerina
    # ftp:Error? response = client->delete(path);
    # ```
    #
    # + path - The resource path
    # + return - `()` or else an `ftp:Error` if failed to establish the communication with the FTP server
    remote isolated function delete(string path) returns Error? {
        return delete(self, path);
    }
}

# File write options for write operations.
# OVERWRITE - Overwrite the existing file content
# APPEND - Append to the existing file content
public enum FileWriteOption {
    OVERWRITE,
    APPEND
}

# Compression type for file uploads.
# ZIP - Zip compression
# NONE - No compression
public enum Compression {
    ZIP,
    NONE
}

# Configuration for FTP client.
#
# + protocol - Protocol to use for the connection: FTP (unsecure) or SFTP (over SSH)
# + host - Target server hostname or IP address
# + port - Port number of the remote service
# + auth - Authentication options for connecting to the server
# + userDirIsRoot - If set to `true`, treats the login home directory as the root (`/`) and
#                   prevents the underlying VFS from attempting to change to the actual server root.
#                   If `false`, treats the actual server root as `/`, which may cause a `CWD /` command
#                   that can fail on servers restricting root access (e.g., chrooted environments).
# + laxDataBinding - If set to `true`, enables relaxed data binding for XML and JSON responses.
#                    null values in JSON/XML are allowed to be mapped to optional fields
#                    missing fields in JSON/XML are allowed to be mapped as null values
# + connectTimeout - Connection timeout in seconds (default: 30.0 for FTP, 10.0 for SFTP)
# + socketConfig - Socket timeout configurations (optional)
# + ftpFileTransfer - File transfer type: BINARY or ASCII (FTP only, default: BINARY)
# + sftpCompression - Compression algorithms (SFTP only, default: "none")
# + sftpSshKnownHosts - Path to SSH known_hosts file (SFTP only)
# + proxy - Proxy configuration for SFTP connections (SFTP only)
public type ClientConfiguration record {|
    Protocol protocol = FTP;
    string host = "127.0.0.1";
    int port = 21;
    AuthConfiguration auth?;
    boolean userDirIsRoot = false;
    boolean laxDataBinding = false;
    decimal connectTimeout = 30.0;
    SocketConfig socketConfig?;
    ProxyConfiguration proxy?;
    FtpFileTransfer ftpFileTransfer = BINARY;
    string sftpCompression = "none";
    string sftpSshKnownHosts?;
|};

isolated function getInputContent(string path, stream<byte[] & readonly, io:Error?>|string|xml|json content,
        boolean compressInput = false) returns InputContent {
    InputContent inputContent = {
        filePath: path,
        compressInput: compressInput
    };

    if content is stream<byte[] & readonly, io:Error?> {
        inputContent.isFile = true;
        inputContent.fileContent = content;
    } else if content is string {
        inputContent.textContent = content;
    } else if content is json {
        inputContent.textContent = content.toJsonString();
    } else {
        inputContent.textContent = content.toString();
    }
    return inputContent;
}
