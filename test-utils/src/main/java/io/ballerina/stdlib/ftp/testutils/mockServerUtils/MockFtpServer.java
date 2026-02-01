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

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.SslConfigurationFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.apache.sshd.server.SshServer;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Creates a Mock FTP Servers
 */
@SuppressWarnings({"java:S2068", "java:S6437"})  // Hardcoded credentials acceptable for test utilities
public final class MockFtpServer {

    private MockFtpServer() {}

    private static final String username = "wso2";
    private static final String password = "wso2123";

    private static final Logger logger = LoggerFactory.getLogger("ballerina");
    private static FakeFtpServer anonFtpServer;
    private static FakeFtpServer ftpServer;
    private static SshServer sftpServer;
    private static FtpServer ftpsServerExplicit;
    private static FtpServer ftpsServerImplicit;
    private static SftpAuthStatusHolder sftpAuthStatusHolder = new SftpAuthStatusHolder();

    public static Object initAnonymousFtpServer() throws Exception {
        int port = 21210;
        anonFtpServer = new FakeFtpServer();
        anonFtpServer.setServerControlPort(port);
        String rootFolder = "/home/in";
        String content = "File content";

        UserAccount anonymousUserAccount = new UserAccount("anonymous", "abc", rootFolder);
        anonymousUserAccount.setPasswordCheckedDuringValidation(false);
        anonFtpServer.addUserAccount(anonymousUserAccount);
        FileSystem fileSystem = new TimestampAwareFakeFileSystem();
        fileSystem.add(new DirectoryEntry(rootFolder));
        fileSystem.add(new FileEntry("/home/in/test1.txt", content));
        anonFtpServer.setFileSystem(fileSystem);
        anonFtpServer.start();
        logger.info("Starting Anonymous FTP server...");

        int i = 0;
        while (!anonFtpServer.isStarted() && i < 10) {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
                i++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new Exception("Error in starting anonymous mock FTP server: " + e.getMessage());
            }
        }
        logger.info("Started Anonymous Mock FTP server");
        return null;
    }

    public static Object initFtpServer() throws Exception {
        final int port = 21212;
        ftpServer = new FakeFtpServer();
        ftpServer.setServerControlPort(port);
        String rootFolder = "/home/in";
        String content1 = "File content";
        StringBuilder content2Builder = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            content2Builder.append("123456789");
        }
        String content2 = content2Builder.toString();

        ftpServer.addUserAccount(new UserAccount(username, password, rootFolder));
        FileSystem fileSystem = new TimestampAwareFakeFileSystem();
        fileSystem.add(new DirectoryEntry(rootFolder));
        fileSystem.add(new FileEntry("/home/in/test1.txt", content1));
        fileSystem.add(new FileEntry("/home/in/test2.txt", content1));
        fileSystem.add(new FileEntry("/home/in/test3.txt", content1));
        fileSystem.add(new FileEntry("/home/in/test4.txt", content2));
        fileSystem.add(new DirectoryEntry("/home/in/folder1"));
        fileSystem.add(new DirectoryEntry("/home/in/folder1/subfolder1"));
        fileSystem.add(new DirectoryEntry("/home/in/childDirectory"));
        fileSystem.add(new DirectoryEntry("/home/in/complexDirectory/subfolder1/subSubFolder1"));
        fileSystem.add(new DirectoryEntry("/home/in/complexDirectory/subfolder1/subSubFolder2"));
        fileSystem.add(new DirectoryEntry("/home/in/complexDirectory/subfolder2"));
        fileSystem.add(new FileEntry("/home/in/complexDirectory/subfolder1/subSubFolder1/content1.txt"));
        fileSystem.add(new FileEntry("/home/in/complexDirectory/subfolder1/subSubFolder3/content1.txt"));
        fileSystem.add(new FileEntry("/home/in/complexDirectory/subfolder1/subSubFolder3/content2.txt"));
        fileSystem.add(new FileEntry("/home/in/child_directory/content1.txt"));
        fileSystem.add(new FileEntry("/home/in/child_directory/content2.txt"));

        fileSystem.add(new DirectoryEntry("/home/in/delete"));
        fileSystem.add(new FileEntry("/home/in/delete/.init", ""));

        fileSystem.add(new DirectoryEntry("/home/in/content-methods"));
        fileSystem.add(new FileEntry("/home/in/content-methods/.init", ""));

        fileSystem.add(new DirectoryEntry("/home/in/retry"));
        fileSystem.add(new FileEntry("/home/in/retry/test1.txt", content1));

        fileSystem.add(new DirectoryEntry("/home/in/advanced/cron"));
        fileSystem.add(new FileEntry("/home/in/cron/.init", ""));

        fileSystem.add(new DirectoryEntry("/home/in/advanced/age"));
        fileSystem.add(new FileEntry("/home/in/age/.init", ""));

        fileSystem.add(new DirectoryEntry("/home/in/advanced/dependency"));
        fileSystem.add(new FileEntry("/home/in/dependency/.init", ""));

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
                throw new Exception("Error in starting mock FTP server: " + e.getMessage());
            }
        }
        logger.info("Started Mock FTP server");
        return null;
    }

    public static void startFtpsServer(String resources, boolean implicitMode, int port) throws Exception {
        final FtpServerFactory serverFactory = new FtpServerFactory();
        final ListenerFactory factory = new ListenerFactory();
        
        SslConfigurationFactory ssl = new SslConfigurationFactory();
        ssl.setKeystoreFile(new File(resources + "/keystore.jks"));
        ssl.setKeystorePassword("changeit");
        factory.setSslConfiguration(ssl.createSslConfiguration());
        factory.setImplicitSsl(implicitMode);

        final PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
        final UserManager userManager = userManagerFactory.createUserManager();
        
        BaseUser user = new BaseUser();
        user.setName(username);
        user.setPassword(password);
        
        File dataDirectory = new File(resources + "/datafiles");
        if (!dataDirectory.exists()) {
            dataDirectory.mkdirs();
        }
        // Ensure folders for listener tests exist to prevent "Not a folder" errors
        new File(dataDirectory, "ftps-client").mkdirs();
        new File(dataDirectory, "ftps-listener").mkdirs();
        
        user.setHomeDirectory(dataDirectory.getAbsolutePath());
        List<Authority> authorities = new ArrayList<>();
        authorities.add(new WritePermission());
        user.setAuthorities(authorities);
        userManager.save(user);
        serverFactory.setUserManager(userManager);
        factory.setPort(port);
        serverFactory.addListener("default", factory.createListener());
        
        FtpServer server = serverFactory.createServer();
        server.start();

        int i = 0;
        while ((server.isStopped() || server.isSuspended()) && i < 10) {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
                i++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new Exception("Error in starting Apache FTPS server");
            }
        }
        
        if (i < 10) {
            if (implicitMode) {
                ftpsServerImplicit = server;
                logger.info("Started Apache FTPS server in IMPLICIT mode on port {}", port);
            } else {
                ftpsServerExplicit = server;
                logger.info("Started Apache FTPS server in EXPLICIT mode on port {}", port);
            }
        } else {
            throw new Exception("Could not start Apache FTPS server on port " + port);
        }
    }

    public static void startFtpsServerExplicit(String resources) throws Exception {
        startFtpsServer(resources, false, 21214);
    }

    public static void startFtpsServerImplicit(String resources) throws Exception {
        startFtpsServer(resources, true, 21217);
    }

    public static Object initSftpServer(String resources) throws Exception {
        final int port = 21213;
        sftpServer = SshServer.setUpDefaultServer();
        SftpServerUtil.setupBasicServerConfig(sftpServer, resources, port);
        try {
            SftpServerUtil.setupAuthentication(sftpServer, resources, sftpAuthStatusHolder,
                    username, password);
            sftpServer.start();
        } catch (Exception e) {
            throw new Exception("Error while starting SFTP server: " + e.getMessage());
        }

        SftpServerUtil.waitForServerStart(sftpServer);
        logger.info("Started Mock SFTP server");
        return null;
    }

    public static void stopAnonymousFtpServer() {
        if (anonFtpServer != null && anonFtpServer.isStarted()) {
            anonFtpServer.stop();
        }
    }

    public static void stopFtpServer() {
        if (ftpServer != null && ftpServer.isStarted()) {
            ftpServer.stop();
        }
    }

    public static void stopSftpServer() throws IOException {
        if (sftpServer != null && sftpServer.isOpen()) {
            sftpServer.stop();
        }
    }

    public static void stopFtpsServer() {
        stopFtpsServerExplicit();
        stopFtpsServerImplicit();
    }

    public static void stopFtpsServerExplicit() {
        if (ftpsServerExplicit != null && !ftpsServerExplicit.isStopped()) {
            ftpsServerExplicit.stop();
            ftpsServerExplicit = null;
        }
    }

    public static void stopFtpsServerImplicit() {
        if (ftpsServerImplicit != null && !ftpsServerImplicit.isStopped()) {
            ftpsServerImplicit.stop();
            ftpsServerImplicit = null;
        }
    }

}
