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
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.ftp.util.FtpConstants;
import io.ballerina.stdlib.ftp.util.FtpUtil;
import io.ballerina.stdlib.ftp.util.ModuleUtils;
import org.apache.commons.vfs2.FileObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static io.ballerina.stdlib.ftp.util.FtpConstants.ARRAY_SIZE;
import static io.ballerina.stdlib.ftp.util.FtpConstants.FIELD_VALUE;
import static io.ballerina.stdlib.ftp.util.FtpConstants.FTP_ERROR;
import static io.ballerina.stdlib.ftp.util.FtpUtil.getFtpPackage;

public class ContentByteStreamIteratorUtils {

    private ContentByteStreamIteratorUtils() {
        // private constructor
    }

    public static Object createStream(InputStream content, Type streamValueType, boolean laxDataBinding,
                                      FileObject fileObject) {
        BObject contentByteStreamObject = ValueCreator.createObjectValue(
                ModuleUtils.getModule(), "ContentByteStream", null, null
        );
        contentByteStreamObject.addNativeData(FtpConstants.NATIVE_INPUT_STREAM, content);
        contentByteStreamObject.addNativeData(FtpConstants.NATIVE_LAX_DATABINDING, laxDataBinding);
        contentByteStreamObject.addNativeData(FtpConstants.NATIVE_STREAM_VALUE_TYPE, streamValueType);
        contentByteStreamObject.addNativeData(FtpConstants.NATIVE_FILE_OBJECT, fileObject);
        StreamType streamType = TypeCreator.createStreamType(streamValueType,
                TypeCreator.createUnionType(PredefinedTypes.TYPE_ERROR, PredefinedTypes.TYPE_NULL));
        return ValueCreator.createStreamValue(streamType, contentByteStreamObject);
    }

    /**
     * Gets the next chunk of bytes from the stream.
     * This method is called by Ballerina runtime when iterating the stream.
     *
     * @return The next record with byte array value, or null if stream is exhausted
     */
    public static Object next(BObject recordIterator) {
        InputStream inputStream = (InputStream) recordIterator.getNativeData(FtpConstants.NATIVE_INPUT_STREAM);
        BMap<BString, Object> streamEntry = ValueCreator.createRecordValue(getFtpPackage(), "ContentStreamEntry");
        try {
            byte[] buffer = new byte[ARRAY_SIZE];
            int readNumber = inputStream.read(buffer);
            if (readNumber == -1) {
                inputStream.close();
                recordIterator.set(StringUtils.fromString("isClosed"), true);
                return null;
            }
            byte[] returnArray;
            if (readNumber < ARRAY_SIZE) {
                returnArray = Arrays.copyOfRange(buffer, 0, readNumber);
            } else {
                returnArray = buffer;
            }
            streamEntry.put(FIELD_VALUE, ValueCreator.createArrayValue(returnArray));
            return streamEntry;
        } catch (IOException e) {
            return FtpUtil.createError("Unable to parse value", e, FTP_ERROR);
        }
    }

    /**
     * Closes the stream iterator.
     *
     * @return null (no error)
     */
    public static Object close(BObject recordIterator) {
        try {
            InputStream inputStream = (InputStream) recordIterator.getNativeData(FtpConstants.NATIVE_INPUT_STREAM);
            inputStream.close();

            Object fileObject = recordIterator.getNativeData(FtpConstants.NATIVE_FILE_OBJECT);
            if (fileObject != null) {
                ((FileObject) fileObject).close();
            }
        } catch (IOException e) {
            return FtpUtil.createError("Unable to clean input stream value", e, FTP_ERROR);
        }
        return null;
    }
}
