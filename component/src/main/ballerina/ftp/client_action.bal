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
import ballerina/io;

@Description { value: "FTP client connector for outbound FTP file requests" }
public type ClientActions object {
    public {
        ClientEndpointConfiguration config;
    }

    @Description { value: "Retrieves ByteChannel" }
    @Param { value: "path: The file path to be read" }
    @Return { value: "channel: A ByteChannel that represent the data source" }
    @Return { value: "Error occured during FTP client invocation" }
    public native function get(string path) returns io:ByteChannel|error;

    @Description { value: "Deletes a file from a given location" }
    @Param { value: "path: File path that should be deleted" }
    @Return { value: "Error occured during FTP client invocation" }
    public native function delete(string path) returns error?;

    @Description { value: "Put a file using the given blob" }
    @Param { value: "path: Destination path of the file" }
    @Param { value: "channel: A ByteChannel that represent the data source" }
    @Return { value: "Error occured during FTP client invocation" }
    public native function put(string path, io:ByteChannel channel) returns error?;

    @Description { value: "Append to a file using the given blob" }
    @Param { value: "path: Destination path of the file" }
    @Param { value: "channel: A ByteChannel that represent the data source" }
    @Return { value: "Error occured during FTP client invocation" }
    public native function append(string path, io:ByteChannel channel) returns error?;

    @Description { value: "Create a directory in a given location" }
    @Param { value: "path: Path that directory need to create" }
    @Return { value: "Error occured during FTP client invocation" }
    public native function mkdir(string path) returns error?;

    @Description { value: "Remove directory in a given location" }
    @Param { value: "path: Path that directory need to remove" }
    @Return { value: "Error occured during FTP client invocation" }
    public native function rmdir(string path) returns error?;

    @Description { value: "Rename the existing file. This can also use to move file to a different location" }
    @Param { value: "origin: Origin file path" }
    @Param { value: "destination: Destination file path" }
    @Return { value: "Error occured during FTP client invocation" }
    public native function rename(string origin, string destination) returns error?;

    @Description { value: "Get the size of the given file" }
    @Param { value: "path: File location" }
    @Return { value: "Returns size of the given file" }
    @Return { value: "Error occured during FTP client invocation" }
    public native function size(string path) returns int|error;

    @Description { value: "Get the list of folder/file names in the given location" }
    @Param { value: "path: Directory location" }
    @Return { value: "Returns size of the given file" }
    @Return { value: "Error occured during FTP client invocation" }
    public native function list(string path) returns string[]|error;

    // isDirectory(string path)
};
