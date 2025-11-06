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
package io.ballerina.stdlib.ftp.util;

import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

public class CSVUtils {

    private CSVUtils() {
        // private constructor
    }

    public static String convertToCsv(BArray inputContent, boolean addHeader) {
        if (inputContent.isEmpty()) {
            return "";
        }

        String lineSeparator = System.lineSeparator();
        StringBuilder csvBuilder = new StringBuilder();

        // Check the type of the first element
        Object firstElement = inputContent.get(0);

        if (firstElement instanceof BArray) {
            // Handle string[][] or any[][] - array of arrays
            for (int i = 0; i < inputContent.size(); i++) {
                BArray row = (BArray) inputContent.get(i);
                csvBuilder.append(convertArrayToCsvRow(row));
                csvBuilder.append(lineSeparator);
            }
        } else {
            // Handle record[] - array of records
            BMap<BString, Object> firstRecord = (BMap<BString, Object>) firstElement;
            BString[] keys = firstRecord.getKeys();

            // Add header row with field names
            if (addHeader) {
                csvBuilder.append(keysToRow(keys));
                csvBuilder.append(lineSeparator);
            }

            // Add data rows
            for (int i = 0; i < inputContent.size(); i++) {
                BMap<BString, Object> recordVal = (BMap<BString, Object>) inputContent.get(i);
                csvBuilder.append(recordToCsvRow(recordVal, keys));
                csvBuilder.append(lineSeparator);
            }
        }

        return csvBuilder.toString();
    }

    /**
     * Converts a BMap (record) to a CSV row with optional header (public method for streaming).
     *
     * @param balRecord        The record to convert
     * @param includeHeader If true and this is the first row, includes header row
     * @return CSV formatted string
     */
    public static String convertRecordToCsvRow(BMap<BString, Object> balRecord, boolean includeHeader) {
        BString[] keys = balRecord.getKeys();
        StringBuilder result = new StringBuilder();

        if (includeHeader) {
            result.append(keysToRow(keys));
            result.append(System.lineSeparator());
        }

        result.append(recordToCsvRow(balRecord, keys));
        return result.toString();
    }

    /**
     * Converts an array of keys to a CSV row.
     */
    private static String keysToRow(BString[] keys) {
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < keys.length; i++) {
            row.append(escapeCsvValue(keys[i].getValue()));
            if (i < keys.length - 1) {
                row.append(",");
            }
        }
        return row.toString();
    }

    /**
     * Converts a BArray to a CSV row.
     */
    public static String convertArrayToCsvRow(BArray array) {
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < array.size(); i++) {
            Object value = array.get(i);
            row.append(escapeCsvValue(valueToString(value)));
            if (i < array.size() - 1) {
                row.append(",");
            }
        }
        return row.toString();
    }

    /**
     * Converts a BMap (record) to a CSV row using the specified key order.
     */
    private static String recordToCsvRow(BMap<BString, Object> balRecord, BString[] keys) {
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < keys.length; i++) {
            Object value = balRecord.get(keys[i]);
            row.append(escapeCsvValue(valueToString(value)));
            if (i < keys.length - 1) {
                row.append(",");
            }
        }
        return row.toString();
    }

    /**
     * Converts a value to its string representation.
     */
    private static String valueToString(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BString) {
            return ((BString) value).getValue();
        }
        return value.toString();
    }

    /**
     * Escapes a CSV value according to RFC 4180.
     * - Wrap in quotes if contains comma, quote, or newline
     * - Escape internal quotes by doubling them
     */
    private static String escapeCsvValue(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        boolean needsQuotes = value.contains(",") || value.contains("\"") ||
                value.contains("\n") || value.contains("\r");

        if (needsQuotes) {
            // Escape internal quotes by doubling them
            String escaped = value.replace("\"", "\"\"");
            return "\"" + escaped + "\"";
        }

        return value;
    }

}
