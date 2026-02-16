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

isolated function createCsvParseOptions(boolean laxDataBinding) returns csv:ParseOptions {
    csv:ParseOptions options = {};
    if laxDataBinding {
        options.allowDataProjection = {
            nilAsOptionalField: true,
            absentAsNilableType: true
        };
    } else {
        options.allowDataProjection = false;
    }
    return options;
}

isolated function createContentBindingError(string message, error? cause, string? filePath)
        returns ContentBindingError {
    if filePath is string {
        if cause is error {
            return error ContentBindingError(message, cause, filePath = filePath);
        }
        return error ContentBindingError(message, filePath = filePath);
    }

    if cause is error {
        return error ContentBindingError(message, cause);
    }
    return error ContentBindingError(message);
}

isolated function toCloseError(error err) returns Error {
    if err is Error {
        return err;
    }
    return error Error("Unable to clean input stream: " + err.message());
}

isolated function ignoreCloseError(error? closeResult) {
    if closeResult is error {
        // best-effort cleanup
    }
}
