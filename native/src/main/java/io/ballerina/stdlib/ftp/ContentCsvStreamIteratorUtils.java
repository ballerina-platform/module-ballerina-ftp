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
import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.PredefinedTypes;
import io.ballerina.runtime.api.types.StreamType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.types.TypeTags;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BStream;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.ftp.util.FtpConstants;
import io.ballerina.stdlib.ftp.util.FtpUtil;
import org.apache.commons.vfs2.FileObject;

import java.io.IOException;
import java.io.InputStream;

import static io.ballerina.stdlib.ftp.util.FtpConstants.FIELD_VALUE;
import static io.ballerina.stdlib.ftp.util.FtpConstants.FTP_ERROR;
import static io.ballerina.stdlib.ftp.util.FtpConstants.NATIVE_INPUT_STREAM;
import static io.ballerina.stdlib.ftp.util.FtpConstants.NATIVE_LAX_DATABINDING;
import static io.ballerina.stdlib.ftp.util.FtpConstants.NATIVE_STREAM_VALUE_TYPE;
import static io.ballerina.stdlib.ftp.util.FtpConstants.BYTE_STREAM_NEXT_FUNC;
import static io.ballerina.stdlib.ftp.util.FtpUtil.getFtpPackage;

/**
 * Iterator utilities for streaming CSV content over an InputStream.
 * Parses the CSV using the native CSV streaming API and yields entries incrementally.
 */
public class ContentCsvStreamIteratorUtils {

    // Native data keys
    private static final String KEY_CSV_STREAM = "csvStream";
    private static final String KEY_CSV_ITER = "csvStreamIterator";
    // Record names
    private static final String REC_STRING_ARRAY_ENTRY = "ContentCsvStringArrayStreamEntry";
    private static final String REC_RECORD_ENTRY = "ContentCsvRecordStreamEntry";
    private static final BString PROP_IS_CLOSED = StringUtils.fromString("isClosed");

    private ContentCsvStreamIteratorUtils() {
        // private constructor
    }

    public static Object createStringArrayStream(InputStream content, Type streamValueType, boolean laxDataBinding,
                                                 FileObject fileObject) {
        BObject contentCsvStreamObject = ValueCreator.createObjectValue(
                io.ballerina.stdlib.ftp.util.ModuleUtils.getModule(), "ContentCsvStringArrayStream",
                null, null
        );
        contentCsvStreamObject.addNativeData(NATIVE_INPUT_STREAM, content);
        contentCsvStreamObject.addNativeData(NATIVE_LAX_DATABINDING, laxDataBinding);
        contentCsvStreamObject.addNativeData(NATIVE_STREAM_VALUE_TYPE, streamValueType);
        contentCsvStreamObject.addNativeData(FtpConstants.NATIVE_FILE_OBJECT, fileObject);
        StreamType streamType = TypeCreator.createStreamType(streamValueType,
                TypeCreator.createUnionType(PredefinedTypes.TYPE_ERROR, PredefinedTypes.TYPE_NULL));
        return ValueCreator.createStreamValue(streamType, contentCsvStreamObject);
    }

    public static Object createRecordStream(InputStream content, Type streamValueType, boolean laxDataBinding,
                                            FileObject fileObject) {
        BObject contentCsvStreamObject = ValueCreator.createObjectValue(
                io.ballerina.stdlib.ftp.util.ModuleUtils.getModule(), "ContentCsvRecordStream",
                null, null
        );
        contentCsvStreamObject.addNativeData(NATIVE_INPUT_STREAM, content);
        contentCsvStreamObject.addNativeData(NATIVE_LAX_DATABINDING, laxDataBinding);
        contentCsvStreamObject.addNativeData(NATIVE_STREAM_VALUE_TYPE, streamValueType);
        contentCsvStreamObject.addNativeData(FtpConstants.NATIVE_FILE_OBJECT, fileObject);
        StreamType streamType = TypeCreator.createStreamType(streamValueType,
                TypeCreator.createUnionType(PredefinedTypes.TYPE_ERROR, PredefinedTypes.TYPE_NULL));
        return ValueCreator.createStreamValue(streamType, contentCsvStreamObject);
    }

    public static Object next(Environment environment, BObject recordIterator) {
        final Type elementType = (Type) recordIterator.getNativeData(NATIVE_STREAM_VALUE_TYPE);
        final String recordTypeName = resolveRecordTypeName(elementType);
        final BMap<BString, Object> streamEntry =
                ValueCreator.createRecordValue(getFtpPackage(), recordTypeName);
        final String filePath = resolveFilePath(recordIterator);

        Object csvIteratorObj = recordIterator.getNativeData(KEY_CSV_ITER);
        if (csvIteratorObj instanceof BObject csvIterator) {
            return readNextCsvEntry(environment, recordIterator, csvIterator, streamEntry, filePath,
                    "Error reading CSV stream: ");
        }

        // First access: initialize CSV stream using native parseToStream
        final InputStream inputStream = (InputStream) recordIterator.getNativeData(NATIVE_INPUT_STREAM);
        if (inputStream == null) {
            recordIterator.set(PROP_IS_CLOSED, true);
            closeQuietly(recordIterator);
            return FtpUtil.createContentBindingError("Input stream is not available", null, filePath, null);
        }

        try {
            final boolean laxDataBinding = getBoolean(recordIterator.getNativeData(NATIVE_LAX_DATABINDING));
            BStream byteStream = createByteStream(inputStream, getFileObject(recordIterator));
            // Build ParseOptions record
            BMap<BString, Object> parseOptions =
                    ValueCreator.createRecordValue(ModuleUtils.getModule(), "ParseOptions");
            parseOptions.put(StringUtils.fromString("allowDataProjection"), laxDataBinding);

            Object parsed = Native.parseToStream(
                    environment,
                    byteStream,
                    parseOptions,
                    ValueCreator.createTypedescValue(elementType));

            if (parsed instanceof BError bError) {
                recordIterator.set(PROP_IS_CLOSED, true);
                closeQuietly(recordIterator);
                return FtpUtil.createContentBindingError(bError.getErrorMessage().getValue(), bError,
                        filePath, null);
            }

            if (!(parsed instanceof BStream csvStream)) {
                recordIterator.set(PROP_IS_CLOSED, true);
                closeQuietly(recordIterator);
                return FtpUtil.createContentBindingError("Unexpected CSV stream type", null, filePath, null);
            }

            BObject csvIterator = csvStream.getIteratorObj();
            recordIterator.addNativeData(KEY_CSV_STREAM, csvStream);
            recordIterator.addNativeData(KEY_CSV_ITER, csvIterator);

            return readNextCsvEntry(environment, recordIterator, csvIterator, streamEntry, filePath,
                    "CSV parsing failed: ");
        } catch (Throwable t) {
            recordIterator.set(PROP_IS_CLOSED, true);
            closeQuietly(recordIterator);
            return FtpUtil.createContentBindingError("CSV parsing failed: " + t.getMessage(), t,
                    filePath, null);
        }
    }

    public static Object close(BObject recordIterator) {
        try {
            Object inputStream = recordIterator.getNativeData(NATIVE_INPUT_STREAM);
            if (inputStream != null) {
                ((InputStream) inputStream).close();
            }
            Object fileObject = recordIterator.getNativeData(FtpConstants.NATIVE_FILE_OBJECT);
            if (fileObject != null) {
                ((FileObject) fileObject).close();
            }
        } catch (IOException e) {
            return FtpUtil.createError("Unable to clean input stream", e, FTP_ERROR);
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

    private static Object readNextCsvEntry(Environment environment, BObject recordIterator, BObject csvIterator,
                                           BMap<BString, Object> streamEntry, String filePath, String errorPrefix) {
        try {
            Object next = environment.getRuntime().callMethod(csvIterator, BYTE_STREAM_NEXT_FUNC, null);
            return mapCsvEntryResult(next, recordIterator, streamEntry, filePath);
        } catch (Throwable t) {
            recordIterator.set(PROP_IS_CLOSED, true);
            closeQuietly(recordIterator);
            return FtpUtil.createContentBindingError(errorPrefix + t.getMessage(), t, filePath, null);
        }
    }

    private static Object mapCsvEntryResult(Object next, BObject recordIterator,
                                            BMap<BString, Object> streamEntry, String filePath) {
        if (next == null) {
            recordIterator.set(PROP_IS_CLOSED, true);
            closeQuietly(recordIterator);
            return null;
        }
        if (next instanceof BError bError) {
            recordIterator.set(PROP_IS_CLOSED, true);
            closeQuietly(recordIterator);
            return FtpUtil.createContentBindingError(bError.getErrorMessage().getValue(), bError,
                    filePath, null);
        }
        if (next instanceof BMap<?, ?> entry) {
            Object value = entry.get(FIELD_VALUE);
            streamEntry.put(FIELD_VALUE, value);
            return streamEntry;
        }
        recordIterator.set(PROP_IS_CLOSED, true);
        closeQuietly(recordIterator);
        return FtpUtil.createContentBindingError("Unexpected CSV stream entry type", null, filePath, null);
    }

    private static void closeQuietly(BObject recordIterator) {
        try {
            close(recordIterator);
        } catch (Throwable ignored) {
            // best-effort cleanup
        }
    }

    private static boolean getBoolean(Object value) {
        return value instanceof Boolean && (Boolean) value;
    }

    private static BStream createByteStream(InputStream inputStream, FileObject fileObject) {
        Type byteArrayType = TypeCreator.createArrayType(PredefinedTypes.TYPE_BYTE);
        return (BStream) ContentByteStreamIteratorUtils.createStream(inputStream, byteArrayType, false, fileObject);
    }

    private static String resolveFilePath(BObject recordIterator) {
        FileObject fileObject = getFileObject(recordIterator);
        return fileObject != null ? fileObject.getName().getURI() : null;
    }

    private static FileObject getFileObject(BObject recordIterator) {
        Object fileObject = recordIterator.getNativeData(FtpConstants.NATIVE_FILE_OBJECT);
        if (fileObject instanceof FileObject) {
            return (FileObject) fileObject;
        }
        return null;
    }
}
