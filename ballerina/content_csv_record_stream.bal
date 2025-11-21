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

# Record returned from the `ContentCsvRecordStream.next()` method.
#
# + value - The record deserialized from a CSV row
public type ContentCsvRecordStreamEntry record {|
    record {} value;
|};

# Stream for reading CSV content as structured records from content callbacks.
# This stream deserializes CSV rows into Ballerina record types.
public class ContentCsvRecordStream {

    private boolean isClosed = false;
    private Error? err;

    public isolated function init(Error? err = ()) {
        self.err = err;
    }

    # Reads and return the next CSV record as `record{}`.
    #
    # + return - A record containing a `record{}` value when the stream is available,
    #            `()` if the stream has reached the end or else an `error`
    public isolated function next() returns record {|record {} value;|}|error? {
        return externGetContentCsvRecordStreamEntry(self);
    }

    # Closes the stream. The primary usage of this function is to close the stream without reaching the end.
    # If the stream reaches the end, the `contentCsvRecordStream.next` will automatically close the stream.
    #
    # + return - Returns `()` when the closing was successful or an `error`
    public isolated function close() returns error? {
        if !self.isClosed {
            var closeResult = externCloseContentCsvRecordStream(self);
            if closeResult is () {
                self.isClosed = true;
            }
            return closeResult;
        }
        return ();
    }
}

isolated function externGetContentCsvRecordStreamEntry(ContentCsvRecordStream iterator)
        returns record {|record {} value;|}|error? = @java:Method {
    'class: "io.ballerina.stdlib.ftp.ContentCsvStreamIteratorUtils",
    name: "next"
} external;

isolated function externCloseContentCsvRecordStream(ContentCsvRecordStream iterator) returns error? = @java:Method {
    'class: "io.ballerina.stdlib.ftp.ContentCsvStreamIteratorUtils",
    name: "close"
} external;
