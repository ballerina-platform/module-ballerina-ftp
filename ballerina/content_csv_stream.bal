// Copyright (c) 2026 WSO2 LLC. (https://www.wso2.com).
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

import ballerina/data.csv;

# Record returned from the `ContentCsvStream.next()` method.
#
# + value - The record or array deserialized from a CSV row
type ContentCsvStreamEntry record {|
    record {}|anydata[] value;
|};

# Stream for reading CSV content as structured records or string arrays from content callbacks.
# This stream deserializes CSV rows into Ballerina record types or arrays.
class ContentCsvStream {

    private boolean isClosed = false;
    private string? filePath = ();
    private stream<record {}|anydata[], csv:Error?> csvStream;

    public isolated function init(typedesc<record {}|anydata[]> targetType,
            stream<byte[], error?>? byteStream = (), boolean laxDataBinding = false,
            string? filePath = ()) returns error? {
        self.filePath = filePath;
        if byteStream is () {
            self.isClosed = true;
            return error Error("Input stream is not available for file " + (filePath ?: ""));
        }
        csv:ParseOptions options = createCsvParseOptions(laxDataBinding);
        stream<record {}|anydata[], error?>|csv:Error parsed = csv:parseToStream(byteStream, options, targetType);
        if parsed is csv:Error {
            self.isClosed = true;
            ignoreCloseError(byteStream.close());
            return error Error("CSV parsing failed for file " + (filePath ?: ""), parsed);
        }
        self.csvStream = parsed;
    }

    # Reads and return the next CSV record as `record{}` or `string[]`.
    #
    # + return - A record containing a `record{}` or array value when the stream is available,
    #            `()` if the stream has reached the end or else an `error`
    public isolated function next() returns record {|record {}|anydata[] value;|}|error? {
        if self.isClosed {
            return;
        }

        record {|record {}|anydata[] value;|}|error? nextEntry = self.csvStream.next();
        if nextEntry is () {
            self.isClosed = true;
            ignoreCloseError(self.csvStream.close());
            return;
        }

        if nextEntry is error {
            self.isClosed = true;
            ignoreCloseError(self.csvStream.close());
            return createContentBindingError("Error reading CSV stream: " + nextEntry.message(), nextEntry,
                self.filePath);
        }

        return nextEntry;
    }

    # Closes the stream. The primary usage of this function is to close the stream without reaching the end.
    # If the stream reaches the end, the `contentCsvStream.next` will automatically close the stream.
    #
    # + return - Returns `()` when the closing was successful or an `error`
    public isolated function close() returns error? {
        if self.isClosed {
            return;
        }

        self.isClosed = true;
        error? closeResult = self.csvStream.close();

        if closeResult is error {
            return toCloseError(closeResult);
        }
    }
}
