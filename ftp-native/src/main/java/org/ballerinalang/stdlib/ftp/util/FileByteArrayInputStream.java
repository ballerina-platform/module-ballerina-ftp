/*
 * Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.stdlib.ftp.util;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BObject;
import org.ballerinalang.stdlib.ftp.client.FTPClientHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * InputStream carrying the file data stream.
 */
public class FileByteArrayInputStream extends ByteArrayInputStream {

    private static final Logger log = LoggerFactory.getLogger(FileByteArrayInputStream.class);

    private Environment env;
    private BObject clientConnector;
    private BufferHolder bufferHolder;
    private BObject iteratorObj;
    private CountDownLatch latch;

    public FileByteArrayInputStream(byte[] buf) {
        super(buf);
    }

    public FileByteArrayInputStream(byte[] buf, Environment env, BObject clientConnector, BufferHolder bufferHolder,
                                    BObject iteratorObj, CountDownLatch latch) {
        this(buf);
        this.env = env;
        this.clientConnector = clientConnector;
        this.bufferHolder = bufferHolder;
        this.iteratorObj = iteratorObj;
        this.latch = latch;
    }

    @Override
    public int read() {
        if (super.available() == 0) {
            FTPClientHelper.callStreamNext(env, clientConnector, bufferHolder, iteratorObj, latch);
            try {
                int timeout = 120;
                boolean countDownReached = latch.await(timeout, TimeUnit.SECONDS);
                if (!countDownReached) {
                    throw ErrorCreator.createError(StringUtils.fromString(
                            "Could not complete byte stream serialization within " + timeout +
                                    " seconds"));
                }
            } catch (InterruptedException e) {
                log.warn("Interrupted before completing the 'next' method of the stream");
            }
        }
        try {
            return super.read(bufferHolder.getBuffer());
        } catch (IOException e) {
            log.error("Error in reading input from file");
            return -1;
        }
    }
}
