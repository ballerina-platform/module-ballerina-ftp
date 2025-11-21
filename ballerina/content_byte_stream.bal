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

# Record returned from the `ContentByteStream.next()` method.
#
# + value - The array of bytes
public type ContentStreamEntry record {|
    byte[] value;
|};

# Record returned from the `ContentCsvStream.next()` method.
#
# + value - The array of strings representing a CSV row
public type ContentCsvStreamEntry record {|
    string[] value;
|};

# Stream for reading file content in byte chunks from content callbacks.
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

# Stream for reading CSV content row by row from content callbacks.
# This stream wraps CSV content and provides it row by row as string arrays.
public class ContentCsvStream {

    private boolean isClosed = false;
    private Error? err;

    public isolated function init(Error? err = ()) {
        self.err = err;
    }

    # Reads and return the next `string[]` row of the CSV stream.
    #
    # + return - A `record` of `string[]` representing a CSV row when the stream is available,
    #            `()` if the stream has reached the end or else an `error`
    public isolated function next() returns record {|string[] value;|}|error? {
        return externGetCsvStreamEntry(self);
    }

    # Closes the stream. The primary usage of this function is to close the stream without reaching the end.
    # If the stream reaches the end, the `contentCsvStream.next` will automatically close the stream.
    #
    # + return - Returns `()` when the closing was successful or an `error`
    public isolated function close() returns error? {
        if !self.isClosed {
            var closeResult = externCloseCsvStream(self);
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

isolated function externGetCsvStreamEntry(ContentCsvStream iterator)
        returns record {|string[] value;|}|error? = @java:Method {
    'class: "io.ballerina.stdlib.ftp.ContentCsvStreamIteratorUtils",
    name: "next"
} external;

isolated function externCloseCsvStream(ContentCsvStream iterator) returns error? = @java:Method {
    'class: "io.ballerina.stdlib.ftp.ContentCsvStreamIteratorUtils",
    name: "close"
} external;
