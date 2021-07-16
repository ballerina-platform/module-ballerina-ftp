/*
 * Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package io.ballerina.stdlib.ftp.transport.client.connector.contractimpl;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.util.MonitorInputStream;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class will wrapped the original InputStream along with the FileObject. This will allow to close both stream
 * properly once it usage is done.
 */
public class FileObjectInputStream extends MonitorInputStream {

    private FileObject path;

    FileObjectInputStream(InputStream originalInputStream, FileObject path) {
        super(originalInputStream);
        this.path = path;
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (path != null) {
            path.close();
        }
    }

    @Override
    protected void onClose() throws IOException {
        super.onClose();
        if (path != null) {
            path.close();
        }
    }
}
