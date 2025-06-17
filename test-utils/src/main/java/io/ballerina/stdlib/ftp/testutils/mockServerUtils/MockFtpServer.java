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
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.keyprovider.AbstractKeyPairProvider;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Creates a Mock FTP Servers
 */
public final class MockFtpServer {

    private MockFtpServer() {}

    private static final String username = "wso2";
    private static final String password = "wso2123";

    private static final Logger logger = LoggerFactory.getLogger("ballerina");
    private static FakeFtpServer anonFtpServer;
    private static FakeFtpServer ftpServer;
    private static SshServer sftpServer;
    private static FtpServer ftpsServer;
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
        FileSystem fileSystem = new UnixFakeFileSystem();
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
        String content2 = "";
        for (int i = 0; i < 1000; i++) {
            content2 += "123456789";
        }

        ftpServer.addUserAccount(new UserAccount(username, password, rootFolder));
        FileSystem fileSystem = new UnixFakeFileSystem();
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

    public static void startFtpsServer(String resources) throws Exception {
        final FtpServerFactory serverFactory = new FtpServerFactory();
        int port = 21214;
        final ListenerFactory factory = new ListenerFactory();
        SslConfigurationFactory ssl = new SslConfigurationFactory();
        ssl.setKeystoreFile(new File(resources + "/keystore.jks"));
        ssl.setKeystorePassword("changeit");
        factory.setSslConfiguration(ssl.createSslConfiguration());
        factory.setImplicitSsl(true);

        final PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
        final UserManager userManager = userManagerFactory.createUserManager();
        BaseUser user = new BaseUser();
        user.setName(username);
        user.setPassword(password);
        File dataDirectory = new File(resources + "/datafiles");
        user.setHomeDirectory(dataDirectory.getAbsolutePath());
        List<Authority> authorities = new ArrayList<>();
        authorities.add(new WritePermission());
        user.setAuthorities(authorities);
        userManager.save(user);
        serverFactory.setUserManager(userManager);
        factory.setPort(port);
        serverFactory.addListener("default", factory.createListener());
        ftpsServer = serverFactory.createServer();
        ftpsServer.start();

        int i = 0;
        while ((ftpsServer.isStopped() || ftpsServer.isSuspended()) && i < 10) {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
                i++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new Exception("Error in starting Apache FTPS server");
            }
        }
        if (i < 10) {
            logger.info("Started Apache FTPS server");
        } else {
            logger.info("Could not start Apache FTPS server");
        }
    }

    public static Object initSftpServer(String resources) throws Exception {
        final int port = 21213;
        File homeFolder = new File(resources + "/datafiles");
        sftpServer = SshServer.setUpDefaultServer();
        VirtualFileSystemFactory virtualFileSystemFactory
                = new VirtualFileSystemFactory(homeFolder.getAbsoluteFile().toPath());
        virtualFileSystemFactory.getDefaultHomeDir().resolve("in").toFile().mkdirs();
        virtualFileSystemFactory.getDefaultHomeDir().resolve("out").toFile().mkdirs();
        sftpServer.setFileSystemFactory(virtualFileSystemFactory);
        sftpServer.setHost("localhost");
        sftpServer.setPort(port);
        sftpServer.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
        sftpServer.setCommandFactory(new ScpCommandFactory());
        try {
            sftpServer.setKeyPairProvider(getKeyPairProvider(resources));
            sftpServer.setPublickeyAuthenticator(new TwoFactorAuthorizedKeysAuthenticator(
                    new File(resources + "/authorized_keys"), sftpAuthStatusHolder));
            sftpServer.setPasswordAuthenticator(
                    (authUsername, authPassword, session) -> sftpAuthStatusHolder.isPublicKeyAuthenticated()
                            && username.equals(authUsername) && password.equals(authPassword));
            sftpServer.setShellFactory(new ProcessShellFactory("/bin/sh", "-i", "-l"));
            sftpServer.start();
        } catch (Exception e) {
            throw new Exception("Error while starting SFTP server: " + e.getMessage());
        }

        int i = 0;
        while (!sftpServer.isOpen() && i < 10) {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
                i++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new Exception("Error in starting mock FTP server: " + e.getMessage());
            }
        }
        logger.info("Started Mock SFTP server");
        return null;
    }

    private static KeyPairProvider getKeyPairProvider(String resources) throws IOException, CertificateException,
            NoSuchAlgorithmException,
            KeyStoreException,
            UnrecoverableKeyException {
        String keystorePath = resources + "/keystore.jks";
        char[] keystorePassword = "changeit".toCharArray();
        char[] keyPassword = "changeit".toCharArray();
        KeyStore ks  = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(new FileInputStream(keystorePath), keystorePassword);

        List<KeyPair> keyPairs = new ArrayList<>();
        for (Enumeration<String> it = ks.aliases(); it.hasMoreElements(); ) {
            String alias = it.nextElement();
            Key key = ks.getKey(alias, keyPassword);
            if (key instanceof PrivateKey) {
                Certificate cert = ks.getCertificate(alias);
                PublicKey publicKey = cert.getPublicKey();
                keyPairs.add(new KeyPair(publicKey, (PrivateKey) key));
            }
        }

        return new AbstractKeyPairProvider() {
            @Override
            public Iterable<KeyPair> loadKeys() {
                return keyPairs;
            }
        };

    }

    public static void stopAnonymousFtpServer() {
        if (anonFtpServer.isStarted()) {
            anonFtpServer.stop();
        }
        logger.info("Stopped Anonymous Mock FTP server");
    }

    public static void stopFtpServer() {
        if (ftpServer.isStarted()) {
            ftpServer.stop();
        }
        logger.info("Stopped Mock FTP server");
    }

    public static void stopSftpServer() throws IOException {
        if (sftpServer.isOpen()) {
            sftpServer.stop();
        }
        logger.info("Stopped Mock SFTP server");
    }

    public static void stopFtpsServer() throws IOException {
        if (!ftpsServer.isSuspended() && !ftpsServer.isStopped()) {
            sftpServer.stop();
        }
        logger.info("Stopped FTPS server");
    }
}
