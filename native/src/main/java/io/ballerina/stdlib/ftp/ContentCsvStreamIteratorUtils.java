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

import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.PredefinedTypes;
import io.ballerina.runtime.api.types.StreamType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BStream;
import io.ballerina.runtime.api.values.BString;
import org.apache.commons.vfs2.FileObject;

import java.io.InputStream;

import static io.ballerina.stdlib.ftp.transport.server.util.FileTransportUtils.maskUrlPassword;

/**
 * Iterator utilities for streaming CSV content over an InputStream.
 * Creates Ballerina CSV stream iterators backed by byte streams.
 */
public class ContentCsvStreamIteratorUtils {

    private ContentCsvStreamIteratorUtils() {
        // private constructor
    }

    public static Object createRecordStream(InputStream content, Type streamValueType, boolean laxDataBinding,
                                            FileObject fileObject) {
        BStream byteStream = content != null ? createByteStream(content, fileObject) : null;
        BString filePath = resolveFilePath(fileObject);
        BObject contentCsvStreamObject;
        try {
            contentCsvStreamObject = ValueCreator.createObjectValue(
                    io.ballerina.stdlib.ftp.util.ModuleUtils.getModule(), "ContentCsvStream",
                    ValueCreator.createTypedescValue(streamValueType), byteStream, laxDataBinding, filePath
            );
        } catch (BError bError) {
            return bError;
        }
        StreamType streamType = TypeCreator.createStreamType(streamValueType,
                TypeCreator.createUnionType(PredefinedTypes.TYPE_ERROR, PredefinedTypes.TYPE_NULL));
        return ValueCreator.createStreamValue(streamType, contentCsvStreamObject);
    }

    private static BStream createByteStream(InputStream inputStream, FileObject fileObject) {
        Type byteArrayType = TypeCreator.createArrayType(PredefinedTypes.TYPE_BYTE);
        return (BStream) ContentByteStreamIteratorUtils.createStream(inputStream, byteArrayType, false, fileObject);
    }

    private static BString resolveFilePath(FileObject fileObject) {
        if (fileObject == null) {
            return null;
        }
        String filePath = maskUrlPassword(fileObject.getName().getURI());
        return filePath != null ? StringUtils.fromString(filePath) : null;
    }

}
