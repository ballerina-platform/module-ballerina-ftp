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

package org.wso2.ei.testutil;

import org.ballerinalang.jvm.values.MapValue;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.ei.ftp.util.BallerinaFTPException;
import org.wso2.ei.ftp.util.FTPUtil;
import org.wso2.ei.ftp.util.FtpConstants;

import java.util.concurrent.TimeUnit;

/**
 * Creates a Mock FTP Server
 */
public class MockFTPServer {

    MockFTPServer() {
        // empty constructor
    }

    private static final Logger logger = LoggerFactory.getLogger("ballerina");
    private static FakeFtpServer ftpServer;

    public static void initServer(MapValue<Object, Object> config) throws BallerinaFTPException {

        int port = FTPUtil.getIntFromConfig(config, FtpConstants.ENDPOINT_CONFIG_PORT, logger);
        final MapValue secureSocket = config.getMapValue(FtpConstants.ENDPOINT_CONFIG_SECURE_SOCKET);
        String username = null;
        String password = null;
        if (secureSocket != null) {
            final MapValue basicAuth = secureSocket.getMapValue(FtpConstants.ENDPOINT_CONFIG_BASIC_AUTH);
            if (basicAuth != null) {
                username = basicAuth.getStringValue(FtpConstants.ENDPOINT_CONFIG_USERNAME);
                password = basicAuth.getStringValue(FtpConstants.ENDPOINT_CONFIG_PASSWORD);
            }
        }

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            throw new BallerinaFTPException("Username and password cannot be empty");
        }

        ftpServer = new FakeFtpServer();
        ftpServer.setServerControlPort(port);
        String rootFolder = "/home/in";
        String content = "File content";

        ftpServer.addUserAccount(new UserAccount(username, password, rootFolder));
        FileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry(rootFolder));
        fileSystem.add(new FileEntry("/home/in/file1.txt", content));
        fileSystem.add(new FileEntry("/home/in/file2.txt", content));
        fileSystem.add(new FileEntry("/home/in/file3.txt", content));
        fileSystem.add(new DirectoryEntry("/home/in/folder1"));
        fileSystem.add(new DirectoryEntry("/home/in/folder1/subfolder1"));
        fileSystem.add(new DirectoryEntry("/home/in/childDirectory"));
        fileSystem.add(new FileEntry("/home/in/child_directory/content1.txt"));
        fileSystem.add(new FileEntry("/home/in/child_directory/content2.txt"));
        ftpServer.setFileSystem(fileSystem);
        ftpServer.start();
        logger.info("Starting FTP server...");

        int i = 0;
        while (!ftpServer.isStarted() && i < 10) {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
                i++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BallerinaFTPException("Error in starting mock FTP server");
            }
        }
        logger.info("Started Mock FTP server");
    }

    public static void stopServer() {

        if (ftpServer.isStarted()) {
            ftpServer.stop();
        }
        logger.info("Stopped Mock FTP server");
    }
}
