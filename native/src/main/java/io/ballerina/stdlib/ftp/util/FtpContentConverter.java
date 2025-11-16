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
import io.ballerina.runtime.api.types.PredefinedTypes;
import io.ballerina.runtime.api.types.StreamType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BTypedesc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

import static io.ballerina.stdlib.ftp.util.FtpUtil.ErrorType.Error;

/**
 * Utility class for converting file content to various Ballerina types using data binding modules.
 * Uses io.ballerina.lib.data.jsondata, xmldata, and csvdata for proper data binding.
 */
public final class FtpContentConverter {

    private static final Logger log = LoggerFactory.getLogger(FtpContentConverter.class);
    private static final BString ALLOW_DATA_PROJECTION = StringUtils.fromString("allowDataProjection");

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
     * Converts byte array to Ballerina JSON using data.jsondata module.
     *
     * @param content The byte array content
     * @param targetType The target Ballerina type for data binding
     * @return Ballerina JSON object or BError
     */
    public static Object convertBytesToJson(byte[] content, Type targetType) {
        try {
            BArray byteArray = ValueCreator.createArrayValue(content);
            BMap<BString, Object> options = createJsonParseOptions();
            BTypedesc typedesc = ValueCreator.createTypedescValue(targetType);

            Object result = io.ballerina.lib.data.jsondata.json.Native.parseBytes(byteArray, options, typedesc);

            if (result instanceof BError) {
                log.error("Failed to parse JSON content: {}", ((BError) result).getMessage());
                return result;
            }

            return result;
        } catch (Exception e) {
            log.error("Error converting bytes to JSON", e);
            return FtpUtil.createError("Failed to parse JSON content: " + e.getMessage(), Error.errorType());
        }
    }

    /**
     * Converts byte array to Ballerina XML using data.xmldata module.
     *
     * @param content The byte array content
     * @param targetType The target Ballerina type for data binding
     * @return Ballerina XML object or BError
     */
    public static Object convertBytesToXml(byte[] content, Type targetType) {
        try {
            BArray byteArray = ValueCreator.createArrayValue(content);
            BMap<BString, Object> options = createXmlParseOptions();

            Type referredType = TypeUtils.getReferredType(targetType);
            BTypedesc typedesc = ValueCreator.createTypedescValue(referredType);

            Object result = io.ballerina.lib.data.xmldata.xml.Native.parseBytes(byteArray, options, typedesc);

            if (result instanceof BError) {
                return result;
            }

            return result;
        } catch (Exception e) {
            return FtpUtil.createError("Failed to parse XML content: " + e.getMessage(), Error.errorType());
        }
    }

    /**
     * Converts byte array to CSV using data.csvdata module.
     *
     * @param content The byte array content
     * @param targetType The target Ballerina type for data binding
     * @return Ballerina CSV data (string[][], record[][], or custom type) or BError
     */
    public static Object convertBytesToCsv(byte[] content, Type targetType) {
        try {
            BArray byteArray = ValueCreator.createArrayValue(content);
            BMap<BString, Object> options = createCsvParseOptions();

            Type referredType = TypeUtils.getReferredType(targetType);
            BTypedesc typedesc = ValueCreator.createTypedescValue(referredType);

            Object result = io.ballerina.lib.data.csvdata.csv.Native.parseBytes(byteArray, options, typedesc);

            if (result instanceof BError) {
                log.error("Failed to parse CSV content: {}", ((BError) result).getMessage());
                return result;
            }

            return result;
        } catch (Exception e) {
            log.error("Error converting bytes to CSV", e);
            return FtpUtil.createError("Failed to parse CSV content: " + e.getMessage(), Error.errorType());
        }
    }

    /**
     * Creates parse options for JSON data binding.
     * Enables lax data projection for flexible type matching.
     *
     * @return BMap containing parse options
     */
    private static BMap<BString, Object> createJsonParseOptions() {
        BMap<BString, Object> options = ValueCreator.createRecordValue(
                io.ballerina.lib.data.ModuleUtils.getModule(), "Options");

        Object allowDataProjectionObj = options.get(ALLOW_DATA_PROJECTION);
        if (allowDataProjectionObj instanceof BMap) {
            BMap<BString, Object> allowDataProjection = (BMap<BString, Object>) allowDataProjectionObj;
            allowDataProjection.put(StringUtils.fromString("nilAsOptionalField"), Boolean.TRUE);
            allowDataProjection.put(StringUtils.fromString("absentAsNilableType"), Boolean.TRUE);
            options.put(ALLOW_DATA_PROJECTION, allowDataProjection);
        } else {
            options.put(ALLOW_DATA_PROJECTION, Boolean.FALSE);
        }

        return options;
    }

    /**
     * Creates parse options for XML data binding.
     * Enables lax data projection for flexible type matching.
     *
     * @return BMap containing parse options
     */
    private static BMap<BString, Object> createXmlParseOptions() {
        BMap<BString, Object> options = ValueCreator.createMapValue();
        options.put(ALLOW_DATA_PROJECTION, true);
        return options;
    }

    /**
     * Creates parse options for CSV data binding.
     * Enables lax data projection for flexible type matching.
     *
     * @return BMap containing parse options
     */
    private static BMap<BString, Object> createCsvParseOptions() {
        BMap<BString, Object> options = ValueCreator.createMapValue();
        options.put(ALLOW_DATA_PROJECTION, true);
        return options;
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

    /**
     * Creates a Ballerina CSV stream (stream<string[], error?>) from byte array content.
     * First parses the CSV content to string[][] using csvdata module, then wraps in a stream.
     *
     * @param content The byte array content
     * @return Ballerina stream object of type stream<string[], error?> or BError
     */
    public static Object createCsvStreamFromContent(byte[] content) {
        try {
            BArray byteArray = ValueCreator.createArrayValue(content);
            BMap<BString, Object> options = createCsvParseOptions();

            // Create typedesc for string[][]
            Type stringArrayArrayType = TypeCreator.createArrayType(
                    TypeCreator.createArrayType(PredefinedTypes.TYPE_STRING));
            BTypedesc typedesc = ValueCreator.createTypedescValue(stringArrayArrayType);

            Object result = io.ballerina.lib.data.csvdata.csv.Native.parseBytes(byteArray, options, typedesc);

            if (result instanceof BError) {
                log.error("Failed to parse CSV content for stream: {}", ((BError) result).getMessage());
                return result;
            }

            // Create ContentCsvStream object and initialize with parsed data
            BObject contentCsvStreamObject = ValueCreator.createObjectValue(
                    io.ballerina.stdlib.ftp.util.ModuleUtils.getModule(), "ContentCsvStream", null, null
            );

            contentCsvStreamObject.addNativeData("CSV_Data", result);
            contentCsvStreamObject.addNativeData("Current_Index", 0);

            // Create stream type: stream<string[], error?>
            ArrayType stringArrayType = TypeCreator.createArrayType(PredefinedTypes.TYPE_STRING);
            StreamType streamType = TypeCreator.createStreamType(stringArrayType,
                    PredefinedTypes.TYPE_ERROR);

            return ValueCreator.createStreamValue(streamType, contentCsvStreamObject);
        } catch (Exception e) {
            log.error("Failed to create CSV stream", e);
            return FtpUtil.createError("Failed to create CSV stream: " + e.getMessage(), Error.errorType());
        }
    }
}
