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

import io.ballerina.lib.data.csvdata.csv.Native;
import io.ballerina.lib.data.csvdata.utils.ModuleUtils;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.types.TypeTags;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.ftp.util.FtpUtil;

import java.io.IOException;
import java.io.InputStream;

import static io.ballerina.stdlib.ftp.util.FtpConstants.FIELD_VALUE;
import static io.ballerina.stdlib.ftp.util.FtpUtil.getFtpPackage;

/**
 * Iterator utilities for streaming CSV content over an InputStream.
 * Parses the CSV once into a BArray on first access and iterates entries subsequently.
 */
public class ContentCsvStreamIteratorUtils {

    // Native data keys
    private static final String KEY_TYPE = "Type";
    private static final String KEY_INDEX = "index";
    private static final String KEY_INPUT_STREAM = "Input_Stream";
    private static final String KEY_LAX_DATA_BINDING = "Lax_Data_Binding";
    private static final String KEY_DATA = "data";
    private static final String KEY_LENGTH = "length";

    // Record names
    private static final String REC_STRING_ARRAY_ENTRY = "ContentCsvStringArrayStreamEntry";
    private static final String REC_RECORD_ENTRY = "ContentCsvRecordStreamEntry";

    private static final BString PROP_IS_CLOSED = StringUtils.fromString("isClosed");

    public static Object next(BObject recordIterator) {
        final Type elementType = (Type) recordIterator.getNativeData(KEY_TYPE);
        final String recordTypeName = resolveRecordTypeName(elementType);
        final BMap<BString, Object> streamEntry =
                ValueCreator.createRecordValue(getFtpPackage(), recordTypeName);

        Object dataIndex = recordIterator.getNativeData(KEY_INDEX);
        if (dataIndex == null) {
            // First access: parse entire stream into memory (behavior preserved)
            final InputStream inputStream = (InputStream) recordIterator.getNativeData(KEY_INPUT_STREAM);
            if (inputStream == null) {
                recordIterator.set(PROP_IS_CLOSED, true);
                return FtpUtil.createError("Input stream is not available", "Error");
            }

            try {
                byte[] bytes = inputStream.readAllBytes();
                inputStream.close();
                final boolean laxDataBinding = getBoolean(recordIterator.getNativeData(KEY_LAX_DATA_BINDING));

                // Build ParseOptions record
                BMap<BString, Object> parseOptions =
                        ValueCreator.createRecordValue(ModuleUtils.getModule(), "ParseOptions");
                parseOptions.put(StringUtils.fromString("allowDataProjection"), laxDataBinding);

                // Parse
                Object parsed = Native.parseBytes(
                        ValueCreator.createArrayValue(bytes),
                        parseOptions,
                        ValueCreator.createTypedescValue(TypeCreator.createArrayType(elementType)));

                if (parsed instanceof BError) {
                    recordIterator.set(PROP_IS_CLOSED, true);
                    return FtpUtil.createError(((BError) parsed).getErrorMessage().getValue(), "Error");
                }

                if (!(parsed instanceof BArray dataArray)) {
                    recordIterator.set(PROP_IS_CLOSED, true);
                    return FtpUtil.createError("Unexpected parse result type", "Error");
                }

                long length = dataArray.getLength();
                if (length == 0) {
                    recordIterator.set(PROP_IS_CLOSED, true);
                    return null;
                }

                int index = 0;
                // Cache for subsequent iterations
                recordIterator.addNativeData(KEY_DATA, dataArray);
                recordIterator.addNativeData(KEY_INDEX, index + 1);
                recordIterator.addNativeData(KEY_LENGTH, length);

                streamEntry.put(FIELD_VALUE, dataArray.get(index));
                return streamEntry;
            } catch (IOException e) {
                recordIterator.set(PROP_IS_CLOSED, true);
                return FtpUtil.createError("Unable to read input stream: " + e.getMessage(), e,
                        "Error");
            } catch (Throwable t) {
                recordIterator.set(PROP_IS_CLOSED, true);
                return FtpUtil.createError("CSV parsing failed: " + t.getMessage(), t,
                        "Error");
            }
        }

        // Subsequent access: iterate cached array
        int index = (int) dataIndex;
        long count = toLong(recordIterator.getNativeData(KEY_LENGTH));
        if (index >= count) {
            recordIterator.set(PROP_IS_CLOSED, true);
            return null;
        }

        BArray dataArray = (BArray) recordIterator.getNativeData(KEY_DATA);
        if (dataArray == null) {
            recordIterator.set(PROP_IS_CLOSED, true);
            return FtpUtil.createError("Iterator state corrupted: data is missing", "Error");
        }

        recordIterator.addNativeData(KEY_INDEX, index + 1);
        streamEntry.put(FIELD_VALUE, dataArray.get(index));
        return streamEntry;
    }

    public static Object close(BObject recordIterator) {
        InputStream inputStream = (InputStream) recordIterator.getNativeData(KEY_INPUT_STREAM);
        if (inputStream == null) {
            return null;
        }
        try {
            inputStream.close();
        } catch (IOException e) {
            return FtpUtil.createError("Unable to clean input stream", e, "Error");
        } finally {
            recordIterator.set(PROP_IS_CLOSED, true);
        }
        return null;
    }

    // -------- helpers --------

    private static String resolveRecordTypeName(Type type) {
        return (type != null && type.getTag() == TypeTags.ARRAY_TAG)
                ? REC_STRING_ARRAY_ENTRY
                : REC_RECORD_ENTRY;
    }

    private static boolean getBoolean(Object value) {
        return value instanceof Boolean && (Boolean) value;
    }

    private static long toLong(Object value) {
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        return 0L;
    }
}
