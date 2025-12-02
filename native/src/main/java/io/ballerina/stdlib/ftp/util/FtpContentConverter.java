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

import io.ballerina.lib.data.ModuleUtils;
import io.ballerina.lib.data.xmldata.xml.Native;
import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.utils.XmlUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BTypedesc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

import static io.ballerina.lib.data.csvdata.csv.Native.parseBytes;
import static io.ballerina.stdlib.ftp.util.FtpConstants.FTP_ERROR;
import static io.ballerina.stdlib.ftp.util.FtpUtil.ErrorType.Error;

/**
 * Utility class for converting file content to various Ballerina types using data binding modules.
 * Uses io.ballerina.lib.data.jsondata, xmldata, and csvdata for proper data binding.
 */
public final class FtpContentConverter {

    private static final Logger log = LoggerFactory.getLogger(FtpContentConverter.class);
    private static final BString ALLOW_DATA_PROJECTION = StringUtils.fromString("allowDataProjection");
    public static final BString FILE_PATH = StringUtils.fromString("filePath");
    public static final String CURRENT_DIRECTORY_PATH = System.getProperty("user.dir");
    public static final String ERROR_LOG_FILE_NAME = "/error.log";
    public static final BString APPEND = StringUtils.fromString("APPEND");
    public static final BString FILE_WRITE_OPTION = StringUtils.fromString("fileWriteOption");
    public static final BString CONTENT_TYPE = StringUtils.fromString("contentType");
    public static final BString RAW_TYPE = StringUtils.fromString("RAW");
    public static final BString FILE_OUTPUT_MODE = StringUtils.fromString("fileOutputMode");
    public static final BString FAIL_SAFE = StringUtils.fromString("failSafe");
    public static final String FAIL_SAFE_OPTIONS = "FailSafeOptions";
    public static final String FILE_OUTPUT_MODE_TYPE = "FileOutputMode";

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
     * @param content    The byte array content
     * @param targetType The target Ballerina type for data binding
     * @return Ballerina JSON object or BError
     */
    public static Object convertBytesToJson(byte[] content, Type targetType, boolean laxDataBinding) {
        try {
            BArray byteArray = ValueCreator.createArrayValue(content);
            BMap<BString, Object> options = createJsonParseOptions(laxDataBinding);
            BTypedesc typedesc = ValueCreator.createTypedescValue(targetType);

            Object result = io.ballerina.lib.data.jsondata.json.Native.parseBytes(byteArray, options, typedesc);
            if (result instanceof BError) {
                log.error("Failed to parse JSON content: {}", ((BError) result).getMessage());
                return FtpUtil.createError(((BError) result).getErrorMessage().getValue(), FTP_ERROR);
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
     * @param content    The byte array content
     * @param targetType The target Ballerina type for data binding
     * @return Ballerina XML object or BError
     */
    public static Object convertBytesToXml(byte[] content, Type targetType, boolean laxDataBinding) {
        try {
            if (targetType.getQualifiedName().equals("xml")) {
                return XmlUtils.parse(StringUtils.fromString(new String(content, StandardCharsets.UTF_8)));
            }

            BMap<BString, Object> mapValue = createXmlParseOptions(laxDataBinding);
            Object bXml = Native.parseBytes(
                    ValueCreator.createArrayValue(content), mapValue, ValueCreator.createTypedescValue(targetType));
            if (bXml instanceof BError) {
                return FtpUtil.createError(((BError) bXml).getErrorMessage().getValue(), FTP_ERROR);
            }
            return bXml;
        } catch (BError e) {
            return FtpUtil.createError(e.getErrorMessage().getValue(), FTP_ERROR);
        }
    }

    /**
     * Converts byte array to CSV using data.csvdata module.
     *
     * @param content    The byte array content
     * @param targetType The target Ballerina type for data binding
     * @return Ballerina CSV data (string[][], record[][], or custom type) or BError
     */
    public static Object convertBytesToCsv(Environment env, byte[] content, Type targetType, boolean laxDataBinding,
                                           boolean csvFailSafeConfigs) {
        try {
            BArray byteArray = ValueCreator.createArrayValue(content);
            BMap<BString, Object> options = createCsvParseOptions(laxDataBinding, csvFailSafeConfigs);

            Type referredType = TypeUtils.getReferredType(targetType);
            BTypedesc typedesc = ValueCreator.createTypedescValue(referredType);

            Object result = parseBytes(env, byteArray, options, typedesc);

            if (result instanceof BError) {
                log.error("Failed to parse CSV content: {}", ((BError) result).getMessage());
                return FtpUtil.createError("Failed to parse CSV content: " + ((BError) result).getErrorMessage(),
                        Error.errorType());
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
    private static BMap<BString, Object> createJsonParseOptions(boolean laxDataBinding) {
        BMap<BString, Object> mapValue = ValueCreator.createRecordValue(ModuleUtils.getModule(), "Options");
        if (laxDataBinding) {
            BMap allowDataProjection = mapValue.getMapValue(StringUtils.fromString("allowDataProjection"));
            allowDataProjection.put(StringUtils.fromString("nilAsOptionalField"), Boolean.TRUE);
            allowDataProjection.put(StringUtils.fromString("absentAsNilableType"), Boolean.TRUE);
            mapValue.put(StringUtils.fromString("allowDataProjection"), allowDataProjection);
        } else {
            mapValue.put(StringUtils.fromString("allowDataProjection"), Boolean.FALSE);
        }
        return mapValue;
    }

    /**
     * Creates parse options for XML data binding.
     * Enables lax data projection for flexible type matching.
     *
     * @return BMap containing parse options
     */
    private static BMap<BString, Object> createXmlParseOptions(boolean laxDataBinding) {
        BMap<BString, Object> mapValue = ValueCreator.createRecordValue(
                new Module("ballerina", "data.xmldata", "1"),
                "SourceOptions");
        mapValue.put(StringUtils.fromString("allowDataProjection"), laxDataBinding);
        return mapValue;
    }

    /**
     * Creates parse options for CSV data binding.
     * Enables lax data projection for flexible type matching.
     *
     * @return BMap containing parse options
     */
    private static BMap<BString, Object> createCsvParseOptions(boolean laxDataBinding, boolean enableCsvFailSafe) {
        BMap<BString, Object> mapValue = ValueCreator.createRecordValue(
                io.ballerina.lib.data.csvdata.utils.ModuleUtils.getModule(), "ParseOptions");
        if (enableCsvFailSafe) {
            BMap<BString, Object> failSafe =
                    ValueCreator.createRecordValue(io.ballerina.lib.data.csvdata.utils.ModuleUtils.getModule(),
                            FAIL_SAFE_OPTIONS);
            BMap<BString, Object> fileOutputMode =
                    ValueCreator.createRecordValue(io.ballerina.lib.data.csvdata.utils.ModuleUtils.getModule(),
                            FILE_OUTPUT_MODE_TYPE);
            fileOutputMode.put(FILE_PATH, StringUtils.fromString(CURRENT_DIRECTORY_PATH + ERROR_LOG_FILE_NAME));
            fileOutputMode.put(FILE_WRITE_OPTION, APPEND);
            fileOutputMode.put(CONTENT_TYPE, RAW_TYPE);
            failSafe.put(FILE_OUTPUT_MODE, fileOutputMode);
            mapValue.put(FAIL_SAFE, failSafe);
        }
        if (laxDataBinding) {
            BMap allowDataProjection = mapValue.getMapValue(StringUtils.fromString("allowDataProjection"));
            allowDataProjection.put(StringUtils.fromString("nilAsOptionalField"), Boolean.TRUE);
            allowDataProjection.put(StringUtils.fromString("absentAsNilableType"), Boolean.TRUE);
            mapValue.put(StringUtils.fromString("allowDataProjection"), allowDataProjection);
        } else {
            mapValue.put(StringUtils.fromString("allowDataProjection"), Boolean.FALSE);
        }
        return mapValue;
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
