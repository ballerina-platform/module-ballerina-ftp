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

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.keyprovider.AbstractKeyPairProvider;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.mockftpserver.fake.FakeFtpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.sshd.server.SshServer;

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

public final class SftpServer {

    private SftpServer() {}

    private static final Logger logger = LoggerFactory.getLogger("ballerina");
    private static FakeFtpServer ftpServer;

    public static void main(String args[]) throws InterruptedException, UnrecoverableKeyException, CertificateException,
            IOException, NoSuchAlgorithmException, KeyStoreException {
        initSftpServer();
        logger.info("Started Example SFTP server");
    }

    public static Object initSftpServer() throws InterruptedException, UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException {

        File homeFolder = new File("src/main/resources/input");
        SftpAuthStatusHolder sftpAuthStatusHolder = new SftpAuthStatusHolder();

        SshServer sftpServer;
        sftpServer = SshServer.setUpDefaultServer();
        VirtualFileSystemFactory virtualFileSystemFactory
                = new VirtualFileSystemFactory(homeFolder.getAbsoluteFile().toPath());
        sftpServer.setFileSystemFactory(virtualFileSystemFactory);
        sftpServer.setHost("localhost");
        sftpServer.setPort(21213);
        sftpServer.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
        sftpServer.setCommandFactory(new ScpCommandFactory());
        try {
            sftpServer.setKeyPairProvider(getKeyPairProvider());
            sftpServer.setPublickeyAuthenticator(new TwoFactorAuthorizedKeysAuthenticator(
                    new File("src/main/resources/authorized_keys"),
                    sftpAuthStatusHolder));
            String finalUsername = "wso2";
            String finalPassword = "wso2123";
            sftpServer.setPasswordAuthenticator(
                    (authUsername, authPassword, session) -> sftpAuthStatusHolder.isPublicKeyAuthenticated()
                            && finalUsername.equals(authUsername) && finalPassword.equals(authPassword));
            sftpServer.setShellFactory(new ProcessShellFactory("/bin/sh", "-i", "-l"));
            sftpServer.start();
        } catch (Exception e) {
            throw e;
        }

        int i = 0;
        while (!sftpServer.isOpen() && i < 10) {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
                i++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            }
        }
        logger.info("Started the SFTP server");
        while (true) {
            Thread.sleep(1000);
        }

    }

    private static KeyPairProvider getKeyPairProvider() throws IOException, CertificateException, NoSuchAlgorithmException,
            KeyStoreException, UnrecoverableKeyException {
        String keystorePath = "src/main/resources/keystore.jks";
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

}
