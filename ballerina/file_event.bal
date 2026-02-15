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

# Metadata about a file or directory on the FTP server.
public type FileInfo record {|
    # Relative file path
    string path;
    # Size of the file in bytes
    int size;
    # Last-modified timestamp in UNIX Epoch time
    int lastModifiedTimestamp;
    # File name (without path)
    string name;
    # `true` if the resource is a directory
    boolean isFolder;
    # `true` if the resource is a regular file
    boolean isFile;
    # Normalized absolute path within the file system
    string pathDecoded;
    # File name extension (e.g., `txt`, `pdf`)
    string extension;
    # File URI formatted for public display
    string publicURIString;
    # MIME type or file classification
    string fileType;
    # `true` if the file object is currently attached
    boolean isAttached;
    # `true` if the file is currently being read or written
    boolean isContentOpen;
    # `true` if the file has execute permissions
    boolean isExecutable;
    # `true` if the file is marked as hidden
    boolean isHidden;
    # `true` if the file has read permissions
    boolean isReadable;
    # `true` if the file has write permissions
    boolean isWritable;
    # Directory nesting level within the file system
    int depth;
    # URI scheme (e.g., `ftp`, `sftp`)
    string scheme;
    # Absolute URI of the file
    string uri;
    # Root URI of the file system
    string rootURI;
    # Access path that does not require authentication
    string friendlyURI;
|};

# File system changes detected by the FTP listener in a polling cycle.
public type WatchEvent record {|
    # Array of newly added files
    FileInfo[] addedFiles;
    # Array of deleted file names
    string[] deletedFiles;
|};
