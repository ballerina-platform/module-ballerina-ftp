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

import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class FtpServer {

    private static final Logger logger = LoggerFactory.getLogger("ballerina");
    private static FakeFtpServer ftpServer;

    public static void main(String args[]) throws InterruptedException {
        ftpServer = new FakeFtpServer();
        ftpServer.setServerControlPort(20210);
        String rootFolder = "/home/in";
        ftpServer.addUserAccount(new UserAccount("wso2", "wso2123", rootFolder));
        FileSystem fileSystem = new UnixFakeFileSystem();
        ftpServer.setFileSystem(fileSystem);
        fileSystem.add(new DirectoryEntry(rootFolder));
        ftpServer.start();
        int i = 0;
        while (!ftpServer.isStarted() && i < 10) {
            TimeUnit.MILLISECONDS.sleep(500);
            i++;
        }
        logger.info("Started Example FTP server");
    }

}
