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

# This provides metadata information for newly added files.
#
# + path - Relative file path for newly added file
# + size - Size of the file
# + lastModifiedTimestamp - Last modified timestamp of the file in UNIX Epoch time
# + name - File name
# + isFolder - Whether the file is a folder or not
# + isFile - Whether the file is a file or not
# + pathDecoded - The normalized absolute path of this file, within its file system
# + extension - The extension of this file name
# + publicURIString - The receiver as a URI String for public display
# + fileType - File's type
# + isAttached - Whether the fileObject is attached
# + isContentOpen - Whether someone reads/writes to this file
# + isExecutable - Whether this file is executable
# + isHidden - Whether this file is hidden
# + isReadable - Whether this file can be read
# + isWritable - Whether this file can be written to
# + depth - The depth of this file name, within its file system
# + scheme - The URI scheme of this file
# + uri - The absolute URI of this file
# + rootURI - The root URI of the file system this file belongs to
# + friendlyURI - A "friendly path", this is a path without a password
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

# This represents the latest status change of the server from the last status change.
#
# + addedFiles - Array of FileInfo that represents newly added files
# + deletedFiles - Array of string that contains deleted file names
public type WatchEvent record {|
    FileInfo[] addedFiles;
    string[] deletedFiles;
|};
