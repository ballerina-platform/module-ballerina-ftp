// Copyright (c) 2025 WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/jballerina.java;

# Represents the type of the record which returned from the contentByteStream.next() call.
#
# + value - The array of byte
public type ContentStreamEntry record {|
    byte[] value;
|};

# `ContentByteStream` used to initialize a stream of type byte[] for content callbacks.
# This stream wraps byte array content and provides it as chunks.
public class ContentByteStream {

    private boolean isClosed = false;
    private Error? err;

    public isolated function init(Error? err = ()) {
        self.err = err;
    }

    # Reads and return the next `byte[]` chunk of the stream.
    #
    # + return - A `record` of `byte[]`s when the stream is available,
    #            `()` if the stream has reached the end or else an `error`
    public isolated function next() returns record {|byte[] value;|}|error? {
        return externGetContentStreamEntry(self);
    }

    # Closes the stream. The primary usage of this function is to close the stream without reaching the end.
    # If the stream reaches the end, the `contentByteStream.next` will automatically close the stream.
    #
    # + return - Returns `()` when the closing was successful or an `error`
    public isolated function close() returns error? {
        if !self.isClosed {
            var closeResult = externCloseContentStream(self);
            if closeResult is () {
                self.isClosed = true;
            }
            return closeResult;
        }
        return ();
    }
}

isolated function externGetContentStreamEntry(ContentByteStream iterator)
        returns record {|byte[] value;|}|error? = @java:Method {    
    'class: "io.ballerina.stdlib.ftp.ContentByteStreamIteratorUtils",
    name: "next"
} external;

isolated function externCloseContentStream(ContentByteStream iterator) returns error? = @java:Method {
    'class: "io.ballerina.stdlib.ftp.ContentByteStreamIteratorUtils",
    name: "close"
} external;
