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

# Information about a file that was added to or found in the monitored folder.
#
# + path - Relative file path for a newly-added file
# + size - Size of the file
# + lastModifiedTimestamp - Last-modified timestamp of the file in UNIX Epoch time
# + name - File name
# + isFolder - `true` if the file is a folder
# + isFile - `true` if the file is a file
# + pathDecoded - Normalized absolute path of this file within its file system
# + extension - Extension of the file name
# + publicURIString - Receiver as a URI String for public display
# + fileType - Type of the file
# + isAttached - `true` if the `fileObject` is attached
# + isContentOpen - `true` if someone reads/writes from/to this file
# + isExecutable - `true` if this file is executable
# + isHidden - `true` if this file is hidden
# + isReadable - `true` if this file can be read
# + isWritable - `true` if this file can be written
# + depth - Depth of the file name within its file system
# + scheme - URI scheme of the file
# + uri - Absolute URI of the file
# + rootURI - Root URI of the file system in which the file exists
# + friendlyURI - A "friendly path" is a path, which can be accessed without a password
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

# Event information containing files that were added or deleted since the last poll.
#
# + addedFiles - List of files that were added to the monitored directory
# + deletedFiles - List of file names that were deleted from the monitored directory
public type WatchEvent record {|
    FileInfo[] addedFiles;
    string[] deletedFiles;
|};
