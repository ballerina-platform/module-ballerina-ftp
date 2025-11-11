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

# Represents an FTP client that interacts with an FTP server
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
    # Deprecated: Use `getText`, `getJson`, `getXml`, `getCsv`, `getBytes`, or the streaming variants `getBytesAsStream`/`getCsvAsStream` instead.
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
    # + return - content as a byte array or `ftp:Error` in case of errors
    remote isolated function getBytes(string path) returns byte[]|Error {
        return getBytes(self, path);
    }

    # Retrieves the file content as text from a remote resource.
    # ```ballerina
    # string content = check client->getText(path);
    # ```
    #
    # + path - The resource path
    # + return - content as a string or `ftp:Error` in case of errors
    remote isolated function getText(string path) returns string|Error {
        return getText(self, path);
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
        'class: "io.ballerina.stdlib.ftp.client.FtpClient"
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
        'class: "io.ballerina.stdlib.ftp.client.FtpClient"
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
    # + targetType - The target element type of the stream (e.g., `string[]` or `record {}`)
    # + return - A stream from which the file can be read or `ftp:Error` in case of errors
    remote isolated function getCsvAsStream(string path, typedesc<string[]|record {}> targetType = <>) returns stream<targetType, error?>|Error = @java:Method {
        name: "getCsvAsStream",
        'class: "io.ballerina.stdlib.ftp.client.FtpClient"
    } external;

    # Appends the content to an existing file in an FTP server.
    # Deprecated: Use `putText`, `putJson`, `putXml`, `putCsv`, `putBytes` or `putCsvAsStream` with option `APPEND`.
    # ```ballerina
    # ftp:Error? response = client->append(path, channel);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + return - `()` or else an `ftp:Error` if failed to establish
    #            the communication with the FTP server
    @deprecated
    remote isolated function append(string path, stream<byte[] & readonly, io:Error?>|string|xml|json content)
            returns Error? {
        return append(self, getInputContent(path, content));
    }

    # Adds a file to an FTP server.
    # Deprecated: Use `putText`, `putJson`, `putXml`, `putCsv`, `putBytes` or `putCsvAsStream`.
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
    # + option - To indicate whether to overwrite or append the given content
    # + return - `()` or else an `ftp:Error` if failed to write
    remote isolated function putBytes(string path, byte[] content, FileWriteOption option = OVERWRITE) returns Error? = @java:Method {
        name: "putBytes",
        'class: "io.ballerina.stdlib.ftp.client.FtpClient"
    } external;

    # Adds a file to an FTP server with the specified write option.
    # ```ballerina
    # ftp:Error? response = client->putText(path, content, option);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + option - To indicate whether to overwrite or append the given content
    # + return - `()` or else an `ftp:Error` if failed to write
    remote isolated function putText(string path, string content, FileWriteOption option = OVERWRITE) returns Error? = @java:Method {
        name: "putText",
        'class: "io.ballerina.stdlib.ftp.client.FtpClient"
    } external;

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
        return putJson(self, path, content.toJsonString(), option);
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
        xml|error xmldata = content is xml ? content : xmldata:toXml(content);
        if xmldata is error {
            return error Error("Failed to convert record to XML: " + xmldata.message());
        }
        return putXml(self, path, xmldata, option);
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
        return putCsv(self, path, content, option);
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
    remote isolated function putBytesAsStream(string path, stream<byte[], error?> content, FileWriteOption option = OVERWRITE) returns Error? = @java:Method {
        name: "putBytesAsStream",
        'class: "io.ballerina.stdlib.ftp.client.FtpClient"
    } external;

    # Adds a CSV file from string[][] or record{}[] elements as a file to an FTP server with the specified write option.
    # ```ballerina
    # ftp:Error? response = client->putCsvAsStream(path, content, option);
    # ```
    #
    # + path - The resource path
    # + content - Content to be written to the file in server
    # + option - To indicate whether to overwrite or append the given content
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

# Represents a file opening options for writing.
#
# + OVERWRITE - Overwrite(truncate the existing content)
# + APPEND - Append to the existing content
public enum FileWriteOption {
    OVERWRITE,
    APPEND
}

# Compression type.
#
# + ZIP - Zip compression
# + NONE - No compression used
public enum Compression {
    ZIP,
    NONE
}

# Configuration for FTP client.
#
# + protocol - Supported FTP protocols
# + host - Target service URL
# + port - Port number of the remote service
# + auth - Authentication options
# + userDirIsRoot - If set to `true`, treats the login home directory as the root (`/`) and 
#                   prevents the underlying VFS from attempting to change to the actual server root. 
#                   If `false`, treats the actual server root as `/`, which may cause a `CWD /` command 
#                   that can fail on servers restricting root access (e.g., chrooted environments).
# + laxDataBinding - If set to `true`, enables relaxed  data binding for XML and JSON responses.
#                    null values in JSON/XML are allowed to be mapped to optional fields
#                    missing fields in JSON/XML are allowed to be mapped as null values
public type ClientConfiguration record {|
    Protocol protocol = FTP;
    string host = "127.0.0.1";
    int port = 21;
    AuthConfiguration auth?;
    boolean userDirIsRoot = false;
    boolean laxDataBinding = false;
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
