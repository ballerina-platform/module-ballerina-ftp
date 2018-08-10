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

documentation {
    Provides the FTP client actions for interacting with an FTP server.

    F{{config}} The configurations of the client endpoint associated with this ClientActions instance.
}
public type ClientActions object {
    public ClientEndpointConfiguration config;

    documentation {
        The `get()` function can be used to retrieve file content from a remote resource.

        P{{path}} The resource path
        R{{}} A ByteChannel that represents the data source to the resource or
        an `error` if failed to establish communication with the FTP server or read the resource
    }
    public extern function get(string path) returns io:ByteChannel|error;

    documentation {
        The `delete()` function can be used to delete a file from an FTP server.

        P{{path}} The resource path
        R{{}} Returns an `error` if failed to establish communication with the FTP server
    }
    public extern function delete(string path) returns error?;

    documentation {
        The `put()` function can be used to add a file to an FTP server.

        P{{path}} The resource path
        P{{channel}} A ByteChannel that represents the local data source
        R{{}} Returns an `error` if failed to establish communication with the FTP server
    }
    public extern function put(string path, io:ByteChannel channel) returns error?;

    documentation {
        The `append()` function can be used to append content to an existing file in an FTP server.
        A new file is created if the file does not exist.

        P{{path}} The resource path
        P{{channel}} A ByteChannel that represents the local data source
        R{{}} Returns an `error` if failed to establish communication with the FTP server
    }
    public extern function append(string path, io:ByteChannel channel) returns error?;

    documentation {
        The `mkdir()` function can be used to create a new direcotry in an FTP server.

        P{{path}} The directory path
        R{{}} Returns an `error` if failed to establish communication with the FTP server
    }
    public extern function mkdir(string path) returns error?;

    documentation {
        The `rmdir()` function can be used to delete an empty directory in an FTP server.

        P{{path}} The directory path
        R{{}} Returns an `error` if failed to establish communication with the FTP server
    }
    public extern function rmdir(string path) returns error?;

    documentation {
        The `rename()` function can be used to rename a file or move to a new location within the same FTP server.

        P{{origin}} The source file location
        P{{destination}} The destination file location.
        R{{}} Returns an `error` if failed to establish communication with the FTP server
    }
    public extern function rename(string origin, string destination) returns error?;

    documentation {
        The `size()` function can be used to get the size of a file resource.

        P{{path}} The resource path
        R{{}} The file size in bytes or an `error` if failed to establish communication with the FTP server
    }
    public extern function size(string path) returns int|error;

    documentation {
        The `list()` function can be used to get the file name list in a given folder.

        P{{path}} The direcotry path
        R{{}} An array of file names or an `error` if failed to establish communication with the FTP server
    }
    public extern function list(string path) returns string[]|error;

    documentation {
        The `isDirectory()` function can be used to check if a given resource is a direcotry.

        P{{path}} The resource path
        R{{}} Returns true if given resouce is a direcotry or
        an `error` if failed to establish communication with the FTP server
    }
    public extern function isDirectory(string path) returns boolean|error;
};
