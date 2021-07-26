// Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import ballerina/jballerina.java;

# Represents the type of the record which returned from the byteStream.next() call.
#
# + value - The array of byte
type StreamEntry record {|
    byte[] & readonly value;
|};

# `ByteStream` used to initialize a stream of type byte[]. This `ByteStream` refers to the stream that embedded to
# the I/O byte channels.
class ByteStream {

    private Client entity;
    private int arraySize;
    private boolean isClosed = false;
    private string resourcePath;
    private boolean initialStreamEntryConsumed = false;
    private StreamEntry? initialStreamEntry;

    # Initialize a `ByteStream` using a `ftp:Client`.
    #
    # + entity - The `ftp:Client` which contains the byte stream
    # + resourcePath - The local path of the file/directory
    # + arraySize - The size of a byte array as an integer
    public isolated function init(Client entity, string resourcePath, int arraySize) returns Error? {
        self.entity = entity;
        self.arraySize = arraySize;
        self.resourcePath = resourcePath;
        record {|byte[] & readonly value;|}|Error? tempInitialStreamEntity
            = externInitialGetStreamEntryRecord(self.entity, self.resourcePath, self.arraySize);
        if (tempInitialStreamEntity is Error) {
            return tempInitialStreamEntity;
        } else {
            self.initialStreamEntry = tempInitialStreamEntity;
        }
    }

    # The next function reads and return the next `byte[]` of the related stream.
    #
    # + return - A `record` of `byte[]`s when the stream is avaliable,
    #            `()` if the stream has reached the end or else an `io:Error`
    public isolated function next() returns record {|byte[] & readonly value;|}|io:Error? {
        if (self.initialStreamEntryConsumed) {
            return externGetStreamEntryRecord(self.entity, self.arraySize);
        } else {
            self.initialStreamEntryConsumed = true;
            return self.initialStreamEntry;
        }
    }

    # Close the stream. The primary usage of this function is to close the stream without reaching the end.
    # If the stream reaches the end, the `byteStream.next` will automatically close the stream.
    #
    # + return - Returns `()` when the closing was successful or a `io:Error`
    public isolated function close() returns io:Error? {
        if (!self.isClosed) {
            var closeResult = externCloseInputStream(self.entity);
            if (closeResult is ()) {
                self.isClosed = true;
            }
            return closeResult;
        }
        return ();
    }
}

isolated function externGetStreamEntryRecord(Client entity, int arraySize)
        returns record {|byte[] & readonly value;|}|io:Error? = @java:Method {
    'class: "io.ballerina.stdlib.ftp.client.FtpClient",
    name: "get"
} external;

isolated function externInitialGetStreamEntryRecord(Client entity, string path, int arraySize)
        returns record {|byte[] & readonly value;|}|Error? = @java:Method {
    'class: "io.ballerina.stdlib.ftp.client.FtpClient",
    name: "getFirst"
} external;

isolated function externCloseInputStream(Client entity) returns io:Error? = @java:Method {
    'class: "io.ballerina.stdlib.ftp.client.FtpClient",
    name: "closeInputByteStream"
} external;
