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
#
# + path - Relative file path
# + size - Size of the file in bytes
# + lastModifiedTimestamp - Last-modified timestamp in UNIX Epoch time
# + name - File name (without path)
# + isFolder - `true` if the resource is a directory
# + isFile - `true` if the resource is a regular file
# + pathDecoded - Normalized absolute path within the file system
# + extension - File name extension (e.g., 'txt', 'pdf')
# + publicURIString - File URI formatted for public display
# + fileType - MIME type or file classification
# + isAttached - `true` if the file object is currently attached
# + isContentOpen - `true` if the file is currently being read or written
# + isExecutable - `true` if the file has execute permissions
# + isHidden - `true` if the file is marked as hidden
# + isReadable - `true` if the file has read permissions
# + isWritable - `true` if the file has write permissions
# + depth - Directory nesting level within the file system
# + scheme - URI scheme (e.g., 'ftp', 'sftp')
# + uri - Absolute URI of the file
# + rootURI - Root URI of the file system
# + friendlyURI - Access path that does not require authentication
public type FileInfo record {|
    string path;
    int size;
    int lastModifiedTimestamp;
    string name;
    boolean isFolder;
    boolean isFile;
    string pathDecoded;
    string extension;
    string publicURIString;
    string fileType;
    boolean isAttached;
    boolean isContentOpen;
    boolean isExecutable;
    boolean isHidden;
    boolean isReadable;
    boolean isWritable;
    int depth;
    string scheme;
    string uri;
    string rootURI;
    string friendlyURI;
|};

# File system changes detected by the FTP listener in a polling cycle.
#
# + addedFiles - Array of newly added files
# + deletedFiles - Array of deleted file names
public type WatchEvent record {|
    FileInfo[] addedFiles;
    string[] deletedFiles;
|};
