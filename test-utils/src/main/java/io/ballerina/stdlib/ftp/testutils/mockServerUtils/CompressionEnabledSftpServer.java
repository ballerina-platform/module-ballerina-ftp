/*
 * Copyright (c) 2025 WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.sshd.common.compression.BuiltinCompressions;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.keyprovider.AbstractKeyPairProvider;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Creates an SFTP server with compression enabled for testing compression configurations.
 */
public final class CompressionEnabledSftpServer {

    private CompressionEnabledSftpServer() {}

    private static final String username = "wso2";
    private static final String password = "wso2123";
    private static final Logger logger = LoggerFactory.getLogger("ballerina");
    private static SshServer compressionSftpServer;
    private static SftpAuthStatusHolder sftpAuthStatusHolder = new SftpAuthStatusHolder();

    /**
     * Starts an SFTP server with compression enabled.
     *
     * @param resources The resources directory path
     * @param port The port to bind to
     * @return null
     * @throws Exception if server fails to start
     */
    public static Object startCompressionSftpServer(String resources, int port) throws Exception {
        File homeFolder = new File(resources + "/datafiles");
        compressionSftpServer = SshServer.setUpDefaultServer();

        VirtualFileSystemFactory virtualFileSystemFactory
                = new VirtualFileSystemFactory(homeFolder.getAbsoluteFile().toPath());
        virtualFileSystemFactory.getDefaultHomeDir().resolve("in").toFile().mkdirs();
        virtualFileSystemFactory.getDefaultHomeDir().resolve("out").toFile().mkdirs();

        compressionSftpServer.setFileSystemFactory(virtualFileSystemFactory);
        compressionSftpServer.setHost("localhost");
        compressionSftpServer.setPort(port);
        compressionSftpServer.setSubsystemFactories(Arrays.asList(new SftpSubsystemFactory()));
        compressionSftpServer.setCommandFactory(new ScpCommandFactory());

        // Enable compression support
        compressionSftpServer.setCompressionFactories(Arrays.asList(
                BuiltinCompressions.zlib,
                BuiltinCompressions.none
        ));

        try {
            compressionSftpServer.setKeyPairProvider(getKeyPairProvider(resources));
            compressionSftpServer.setPublickeyAuthenticator(new TwoFactorAuthorizedKeysAuthenticator(
                    new File(resources + "/authorized_keys"), sftpAuthStatusHolder));
            compressionSftpServer.setPasswordAuthenticator(
                    (authUsername, authPassword, session) -> sftpAuthStatusHolder.isPublicKeyAuthenticated()
                            && username.equals(authUsername) && password.equals(authPassword));
            compressionSftpServer.setShellFactory(new ProcessShellFactory("/bin/sh", "-i", "-l"));
            compressionSftpServer.start();
        } catch (Exception e) {
            throw new Exception("Error while starting compression-enabled SFTP server: " + e.getMessage());
        }

        int i = 0;
        while (!compressionSftpServer.isOpen() && i < 10) {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
                i++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new Exception("Error in starting compression SFTP server: " + e.getMessage());
            }
        }
        logger.info("Started Compression-enabled SFTP server on port " + port);
        return null;
    }

    private static KeyPairProvider getKeyPairProvider(String resources) throws IOException, CertificateException,
            NoSuchAlgorithmException,
            KeyStoreException,
            UnrecoverableKeyException {
        String keystorePath = resources + "/keystore.jks";
        char[] keystorePassword = "changeit".toCharArray();
        char[] keyPassword = "changeit".toCharArray();
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
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

    /**
     * Stops the compression-enabled SFTP server.
     */
    public static void stopCompressionSftpServer() throws IOException {
        if (compressionSftpServer != null && compressionSftpServer.isOpen()) {
            compressionSftpServer.stop();
            logger.info("Stopped Compression-enabled SFTP server");
        }
    }
}
