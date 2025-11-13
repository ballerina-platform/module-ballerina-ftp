/*
 * Copyright (c) 2025 WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.stdlib.ftp;

import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;

import static io.ballerina.stdlib.ftp.util.FtpConstants.FIELD_VALUE;
import static io.ballerina.stdlib.ftp.util.FtpUtil.getFtpPackage;

/**
 * Utility class for iterating through CSV content as a stream of string arrays.
 * Each iteration returns one CSV row as a string array.
 * The CSV data is pre-parsed using the csvdata module and stored as string[][].
 */
public class ContentCsvStreamIteratorUtils {

    /**
     * Gets the next CSV row from the stream as a string array.
     */
    public static Object next(BObject recordIterator) {
        try {
            BArray csvData = (BArray) recordIterator.getNativeData("CSV_Data");
            Integer currentIndex = (Integer) recordIterator.getNativeData("Current_Index");

            if (currentIndex == null) {
                currentIndex = 0;
            }

            if (currentIndex >= csvData.size()) {
                recordIterator.set(StringUtils.fromString("isClosed"), true);
                return null;
            }

            BArray currentRow = (BArray) csvData.get(currentIndex);
            recordIterator.addNativeData("Current_Index", currentIndex + 1);
            BMap<BString, Object> streamEntry = ValueCreator.createRecordValue(getFtpPackage(),
                    "ContentCsvStreamEntry");
            streamEntry.put(FIELD_VALUE, currentRow);

            return streamEntry;
        } catch (Exception e) {
            return ErrorCreator.createError(StringUtils.fromString("Unable to read CSV row: " + e.getMessage()));
        }
    }

    /**
     * Closes the CSV stream iterator.
     */
    public static Object close(BObject recordIterator) {
        // No resources to close since data is already in memory
        return null;
    }
}
