/*
 * Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package io.ballerina.stdlib.ftp.testutils.mockServerUtils;

import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystemEntry;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;
import java.util.Date;

/**
 * Custom FileSystem that ensures proper modification times for files.
 * When files are added, if they don't have an explicit modification time,
 * this sets it to the current time.
 */
public class TimestampAwareFakeFileSystem extends UnixFakeFileSystem {

    @Override
    public void add(FileSystemEntry entry) {
        // If this is a file and has no modification time set, use current time
        if (entry instanceof FileEntry) {
            FileEntry fileEntry = (FileEntry) entry;
            Date lastModified = fileEntry.getLastModified();
            if (lastModified == null || lastModified.getTime() <= 0) {
                fileEntry.setLastModified(new Date(System.currentTimeMillis()));
            }
        }
        super.add(entry);
    }
}
