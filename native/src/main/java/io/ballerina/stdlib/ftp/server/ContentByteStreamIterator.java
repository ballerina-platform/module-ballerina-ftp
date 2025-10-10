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

package io.ballerina.stdlib.ftp.server;

import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.ftp.util.FtpUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Iterator implementation for byte stream content callbacks.
 * Wraps a byte array and provides it as a stream of byte chunks.
 */
public class ContentByteStreamIterator {

    private static final int DEFAULT_CHUNK_SIZE = 8192; // 8KB chunks
    private final byte[] content;
    private final int chunkSize;
    private int currentPosition;
    private boolean closed;

    /**
     * Creates a new ContentByteStreamIterator.
     *
     * @param content The byte array content to stream
     */
    public ContentByteStreamIterator(byte[] content) {
        this(content, DEFAULT_CHUNK_SIZE);
    }

    /**
     * Creates a new ContentByteStreamIterator with custom chunk size.
     *
     * @param content The byte array content to stream
     * @param chunkSize The size of each chunk
     */
    public ContentByteStreamIterator(byte[] content, int chunkSize) {
        this.content = content;
        this.chunkSize = chunkSize;
        this.currentPosition = 0;
        this.closed = false;
    }

    /**
     * Gets the next chunk of bytes from the stream.
     * This method is called by Ballerina runtime when iterating the stream.
     *
     * @param iterator The ContentByteStreamIterator handle passed from Ballerina
     * @return The next record with byte array value, or null if stream is exhausted
     */
    public static Object next(ContentByteStreamIterator iterator) {
        if (iterator.closed || iterator.currentPosition >= iterator.content.length) {
            return null;
        }

        int remainingBytes = iterator.content.length - iterator.currentPosition;
        int bytesToRead = Math.min(iterator.chunkSize, remainingBytes);

        byte[] chunk = Arrays.copyOfRange(iterator.content, iterator.currentPosition,
                iterator.currentPosition + bytesToRead);
        iterator.currentPosition += bytesToRead;

        // Create Ballerina byte array with proper readonly constraint
        byte[] readonlyChunk = new byte[chunk.length];
        System.arraycopy(chunk, 0, readonlyChunk, 0, chunk.length);
        BArray ballerinaByteArray = ValueCreator.createArrayValue(readonlyChunk);

        // Create the record {| byte[] & readonly value; |}
        Map<String, Object> recordValues = new HashMap<>();
        recordValues.put("value", ballerinaByteArray);

        BMap<BString, Object> streamEntry = ValueCreator.createRecordValue(
                FtpUtil.getFtpPackage(),
                "ContentStreamEntry",
                recordValues
        );

        return streamEntry;
    }

    /**
     * Closes the stream iterator.
     *
     * @param iterator The ContentByteStreamIterator handle passed from Ballerina
     * @return null (no error)
     */
    public static Object close(ContentByteStreamIterator iterator) {
        iterator.closed = true;
        return null;
    }

    /**
     * Checks if there are more elements in the stream.
     *
     * @return true if there are more elements, false otherwise
     */
    public boolean hasNext() {
        return !closed && currentPosition < content.length;
    }
}
