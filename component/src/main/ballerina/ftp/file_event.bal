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

documentation {
    This give newly added file's meta information.

    F{{path}} Relative file path for newly added file.
    F{{size}} Size of the file.
    F{{lastModifiedTimestamp}} Last modified timestamp of the file in UNIX Epoch time.
}
public type FileInfo {
    string path,
    int size,
    int lastModifiedTimestamp,
};

documentation {
    This represents the latest status change of the server from the last status change.

    F{{addedFiles}} Array of FileInfo which represents newly added files.
    F{{deletedFiles}} Array of string which contains deleted file names.
}
public type WatchEvent {
    FileInfo[] addedFiles,
    string[] deletedFiles,
};