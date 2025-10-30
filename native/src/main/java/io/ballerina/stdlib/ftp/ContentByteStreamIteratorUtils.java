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
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static io.ballerina.stdlib.ftp.util.FtpConstants.ARRAY_SIZE;
import static io.ballerina.stdlib.ftp.util.FtpConstants.FIELD_VALUE;
import static io.ballerina.stdlib.ftp.util.FtpUtil.getFtpPackage;

public class ContentByteStreamIteratorUtils {

    /**
     * Gets the next chunk of bytes from the stream.
     * This method is called by Ballerina runtime when iterating the stream.
     *
     * @return The next record with byte array value, or null if stream is exhausted
     */
    public static Object next(BObject recordIterator) {
        InputStream inputStream = (InputStream) recordIterator.getNativeData("Input_Stream");
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
        } catch (IOException e) {
            streamEntry.put(FIELD_VALUE, ErrorCreator.createError(StringUtils.fromString("Unable to parse value")));
        }
        return streamEntry;
    }

    /**
     * Closes the stream iterator.
     *
     * @return null (no error)
     */
    public static Object close(BObject recordIterator) {
        InputStream inputStream = (InputStream) recordIterator.getNativeData("Input_Stream");
        try {
            inputStream.close();
        } catch (IOException e) {
            throw ErrorCreator.createError(StringUtils.fromString("Unable to clean input stream"), e);
        }
        return null;
    }
}
