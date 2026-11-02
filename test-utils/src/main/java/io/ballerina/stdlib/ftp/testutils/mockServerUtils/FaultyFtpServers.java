/*
 * Copyright (c) 2026 WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.ftp.testutils.mockServerUtils;

import org.mockftpserver.core.command.CommandNames;
import org.mockftpserver.core.command.StaticReplyCommandHandler;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * FTP servers that misbehave in a fixed way, for error classification, retry, and circuit breaker tests.
 */
public final class FaultyFtpServers {

    public static final int BUSY_FTP_PORT = 21221;
    public static final int HUNG_FTP_PORT = 21222;

    private static final Logger logger = LoggerFactory.getLogger("ballerina");
    private static final String ROOT_FOLDER = "/home/in";
    private static FakeFtpServer busyServer;
    private static FakeFtpServer hungServer;

    private FaultyFtpServers() {}

    /**
     * Starts a server that answers every login with 421 Too many connections.
     */
    public static void startBusyServer() throws InterruptedException {
        busyServer = newServer(BUSY_FTP_PORT);
        busyServer.setCommandHandler(CommandNames.USER, new StaticReplyCommandHandler(421, "Too many connections"));
        start(busyServer, "busy");
    }

    /**
     * Starts a server that accepts a login and then never replies to PASV or EPSV.
     */
    public static void startHungServer() throws InterruptedException {
        hungServer = newServer(HUNG_FTP_PORT);
        hungServer.setCommandHandler(CommandNames.PASV, (command, session) -> { });
        hungServer.setCommandHandler(CommandNames.EPSV, (command, session) -> { });
        start(hungServer, "hung");
    }

    public static void stop() {
        if (busyServer != null && busyServer.isStarted()) {
            busyServer.stop();
        }
        if (hungServer != null && hungServer.isStarted()) {
            hungServer.stop();
        }
    }

    private static FakeFtpServer newServer(int port) {
        FakeFtpServer server = new FakeFtpServer();
        server.setServerControlPort(port);
        server.addUserAccount(new UserAccount("wso2", "wso2123", ROOT_FOLDER));
        FileSystem fileSystem = new TimestampAwareFakeFileSystem();
        fileSystem.add(new DirectoryEntry(ROOT_FOLDER));
        server.setFileSystem(fileSystem);
        return server;
    }

    private static void start(FakeFtpServer server, String name) throws InterruptedException {
        server.start();
        int attempts = 0;
        while (!server.isStarted() && attempts < 10) {
            TimeUnit.MILLISECONDS.sleep(500);
            attempts++;
        }
        logger.info("Started {} mock FTP server", name);
    }
}
