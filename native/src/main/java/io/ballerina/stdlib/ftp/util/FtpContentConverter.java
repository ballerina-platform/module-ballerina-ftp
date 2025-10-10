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

import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.types.Field;
import io.ballerina.runtime.api.types.PredefinedTypes;
import io.ballerina.runtime.api.types.RecordType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.types.TypeTags;
import io.ballerina.runtime.api.utils.JsonUtils;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.utils.XmlUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.ballerina.runtime.api.creators.ValueCreator.createRecordValue;

/**
 * Utility class for converting file content to various Ballerina types.
 */
public final class FtpContentConverter {

    private static final Logger log = LoggerFactory.getLogger(FtpContentConverter.class);
    private static final String CSV_DELIMITER = ",";
    private static final String CSV_QUOTE = "\"";

    private FtpContentConverter() {
        // private constructor
    }

    /**
     * Converts byte array to Ballerina string (UTF-8).
     *
     * @param content The byte array content
     * @return Ballerina string
     */
    public static BString convertBytesToString(byte[] content) {
        String textContent = new String(content, StandardCharsets.UTF_8);
        return StringUtils.fromString(textContent);
    }

    /**
     * Converts byte array to Ballerina JSON.
     *
     * @param content The byte array content
     * @return Ballerina JSON object
     * @throws Exception if parsing fails
     */
    public static Object convertBytesToJson(byte[] content) throws Exception {
        String jsonString = new String(content, StandardCharsets.UTF_8);
        return JsonUtils.parse(jsonString);
    }

    /**
     * Converts byte array to Ballerina XML.
     *
     * @param content The byte array content
     * @return Ballerina XML object
     * @throws Exception if parsing fails
     */
    public static Object convertBytesToXml(byte[] content) throws Exception {
        String xmlString = new String(content, StandardCharsets.UTF_8);
        return XmlUtils.parse(xmlString);
    }

    /**
     * Converts byte array to Ballerina string[][] (CSV as array of arrays).
     *
     * @param content The byte array content
     * @return Ballerina string[][]
     */
    public static BArray convertBytesToCsvStringArray(byte[] content) {
        List<List<String>> csvData = parseCsvContent(content);

        // Create 2D string array
        ArrayType innerArrayType = TypeCreator.createArrayType(PredefinedTypes.TYPE_STRING);
        ArrayType outerArrayType = TypeCreator.createArrayType(innerArrayType);

        BArray outerArray = ValueCreator.createArrayValue(outerArrayType);

        for (List<String> row : csvData) {
            BString[] rowArray = new BString[row.size()];
            for (int i = 0; i < row.size(); i++) {
                rowArray[i] = StringUtils.fromString(row.get(i));
            }
            BArray innerArray = ValueCreator.createArrayValue(rowArray, innerArrayType);
            outerArray.append(innerArray);
        }

        return outerArray;
    }

    /**
     * Converts byte array to Ballerina record{}[] (CSV as array of records).
     * First row is treated as header row containing field names.
     *
     * @param content The byte array content
     * @param recordType The record type for each row (contains field definitions)
     * @return Ballerina record{}[]
     */
    public static BArray convertBytesToCsvRecordArray(byte[] content, Type recordType) {
        List<List<String>> csvData = parseCsvContent(content);

        if (csvData.isEmpty()) {
            // Return empty array if no data
            ArrayType arrayType = TypeCreator.createArrayType(recordType);
            return ValueCreator.createArrayValue(arrayType);
        }

        // First row is headers
        List<String> headers = csvData.get(0);

        // Get record type information
        RecordType recType = (RecordType) TypeUtils.getReferredType(recordType);
        Map<String, Field> fields = recType.getFields();

        // Create array to hold records
        ArrayType arrayType = TypeCreator.createArrayType(recordType);
        BMap<BString, Object>[] records = new BMap[csvData.size() - 1];

        // Process each data row (skip header row)
        for (int i = 1; i < csvData.size(); i++) {
            List<String> row = csvData.get(i);
            Map<String, Object> recordValues = new HashMap<>();

            // Map each column to record field
            for (int j = 0; j < headers.size() && j < row.size(); j++) {
                String fieldName = headers.get(j).trim();
                String fieldValue = row.get(j);

                // Check if this field exists in the record type
                Field field = fields.get(fieldName);
                if (field != null) {
                    // Convert value to appropriate type
                    Object convertedValue = convertCsvValueToType(fieldValue, field.getFieldType());
                    recordValues.put(fieldName, convertedValue);
                }
            }

            // Create record with values
            records[i - 1] = createRecordValue((BMap<BString, Object>) recType, recordValues);
        }

        return ValueCreator.createArrayValue(records, arrayType);
    }

    /**
     * Converts a CSV string value to the appropriate Ballerina type based on the field type.
     *
     * @param value The string value from CSV
     * @param targetType The target Ballerina type
     * @return Converted value
     */
    private static Object convertCsvValueToType(String value, Type targetType) {
        Type referredType = TypeUtils.getReferredType(targetType);

        try {
            switch (referredType.getTag()) {
                case TypeTags.STRING_TAG:
                    return StringUtils.fromString(value);

                case TypeTags.INT_TAG:
                    return Long.parseLong(value.trim());

                case TypeTags.FLOAT_TAG:
                    return Double.parseDouble(value.trim());

                case TypeTags.BOOLEAN_TAG:
                    return Boolean.parseBoolean(value.trim());

                case TypeTags.DECIMAL_TAG:
                    return ValueCreator.createDecimalValue(value.trim());

                default:
                    // For other types, return as string
                    return StringUtils.fromString(value);
            }
        } catch (NumberFormatException | ArithmeticException e) {
            log.warn("Failed to convert CSV value '{}' to type {}. Using default value.",
                    value, referredType.getName());
            // Return default value for the type
            return getDefaultValueForType(referredType);
        }
    }

    /**
     * Gets the default value for a given Ballerina type.
     *
     * @param type The Ballerina type
     * @return Default value for the type
     */
    private static Object getDefaultValueForType(Type type) {
        switch (type.getTag()) {
            case TypeTags.STRING_TAG:
                return StringUtils.fromString("");
            case TypeTags.INT_TAG:
                return 0L;
            case TypeTags.FLOAT_TAG:
                return 0.0;
            case TypeTags.BOOLEAN_TAG:
                return false;
            case TypeTags.DECIMAL_TAG:
                return ValueCreator.createDecimalValue("0");
            default:
                return null;
        }
    }

    /**
     * Parses CSV content from byte array to list of lists.
     *
     * @param content The byte array content
     * @return List of CSV rows, where each row is a list of values
     */
    private static List<List<String>> parseCsvContent(byte[] content) {
        List<List<String>> csvData = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                List<String> row = parseCsvLine(line);
                csvData.add(row);
            }
        } catch (Exception e) {
            log.error("Error parsing CSV content", e);
        }

        return csvData;
    }

    /**
     * Parses a single CSV line following RFC 4180 rules.
     *
     * @param line The CSV line to parse
     * @return List of field values
     */
    private static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char currentChar = line.charAt(i);

            if (currentChar == '\"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    // Escaped quote (two consecutive quotes)
                    currentField.append('\"');
                    i++; // Skip next quote
                } else {
                    // Toggle quote state
                    inQuotes = !inQuotes;
                }
            } else if (currentChar == ',' && !inQuotes) {
                // Field delimiter outside quotes
                fields.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(currentChar);
            }
        }

        // Add last field
        fields.add(currentField.toString());

        return fields;
    }

    /**
     * Creates a byte array from input stream.
     *
     * @param inputStream The input stream
     * @return byte array
     * @throws Exception if reading fails
     */
    public static byte[] convertInputStreamToByteArray(java.io.InputStream inputStream) throws Exception {
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[FtpConstants.ARRAY_SIZE];
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        return outputStream.toByteArray();
    }

    /**
     * Converts byte array to Ballerina byte array.
     *
     * @param content The byte array content
     * @return Ballerina byte array
     */
    public static BArray convertToBallerinaByteArray(byte[] content) {
        return ValueCreator.createArrayValue(content);
    }
}
