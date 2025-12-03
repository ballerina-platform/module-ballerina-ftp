/*
 * Copyright (c) 2025 WSO2 LLC. (http://www.wso2.org)
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

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.keyprovider.AbstractKeyPairProvider;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;

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
 * Utility class for shared SFTP server setup logic.
 */
@SuppressWarnings({"java:S2068", "java:S6437"})  // Hardcoded credentials acceptable for test utilities
public final class SftpServerUtil {

    private SftpServerUtil() {}

    private static final String DATAFILES_DIR = "/datafiles";
    private static final String IN_DIR = "in";
    private static final String OUT_DIR = "out";
    private static final String LOCALHOST = "localhost";
    private static final String AUTHORIZED_KEYS_FILE = "/authorized_keys";
    private static final String KEYSTORE_FILE = "/keystore.jks";
    private static final String KEYSTORE_PASSWORD = "changeit";
    private static final String KEY_PASSWORD = "changeit";

    /**
     * Sets up basic SFTP server configuration with virtual file system factory.
     *
     * @param server The SSH server instance
     * @param resources The resources directory path
     * @param port The port to bind to
     */
    public static void setupBasicServerConfig(SshServer server, String resources, int port) {
        File homeFolder = new File(resources + DATAFILES_DIR);
        VirtualFileSystemFactory virtualFileSystemFactory
                = new VirtualFileSystemFactory(homeFolder.getAbsoluteFile().toPath());
        virtualFileSystemFactory.getDefaultHomeDir().resolve(IN_DIR).toFile().mkdirs();
        virtualFileSystemFactory.getDefaultHomeDir().resolve(OUT_DIR).toFile().mkdirs();

        server.setFileSystemFactory(virtualFileSystemFactory);
        server.setHost(LOCALHOST);
        server.setPort(port);
        server.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
        server.setCommandFactory(new ScpCommandFactory());
        server.setShellFactory(new ProcessShellFactory("/bin/sh", "-i", "-l"));
    }

    /**
     * Sets up authentication for the SFTP server.
     *
     * @param server The SSH server instance
     * @param resources The resources directory path
     * @param sftpAuthStatusHolder The authentication status holder
     * @param username The username for authentication
     * @param password The password for authentication
     */
    public static void setupAuthentication(SshServer server, String resources,
                                          SftpAuthStatusHolder sftpAuthStatusHolder,
                                          String username, String password) throws IOException, CertificateException,
            NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
        server.setKeyPairProvider(getKeyPairProvider(resources));
        server.setPublickeyAuthenticator(new TwoFactorAuthorizedKeysAuthenticator(
                new File(resources + AUTHORIZED_KEYS_FILE), sftpAuthStatusHolder));
        server.setPasswordAuthenticator(
                (authUsername, authPassword, session) -> sftpAuthStatusHolder.isPublicKeyAuthenticated()
                        && username.equals(authUsername) && password.equals(authPassword));
    }

    /**
     * Waits for the SFTP server to start.
     *
     * @param server The SSH server instance
     * @throws Exception if the server fails to start within the timeout
     */
    public static void waitForServerStart(SshServer server) throws Exception {
        int i = 0;
        while (!server.isOpen() && i < 10) {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
                i++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new Exception("Error in starting SFTP server: " + e.getMessage());
            }
        }
    }

    /**
     * Gets the key pair provider from the keystore file.
     *
     * @param resources The resources directory path
     * @return The key pair provider
     * @throws IOException if an I/O error occurs
     * @throws CertificateException if a certificate error occurs
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws KeyStoreException if the keystore cannot be accessed
     * @throws UnrecoverableKeyException if the key cannot be recovered
     */
    public static KeyPairProvider getKeyPairProvider(String resources) throws IOException, CertificateException,
            NoSuchAlgorithmException,
            KeyStoreException,
            UnrecoverableKeyException {
        String keystorePath = resources + KEYSTORE_FILE;
        char[] keystorePassword = KEYSTORE_PASSWORD.toCharArray();
        char[] keyPassword = KEY_PASSWORD.toCharArray();
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            ks.load(fis, keystorePassword);
        }

        List<KeyPair> keyPairs = new ArrayList<>();
        for (Enumeration<String> it = ks.aliases(); it.hasMoreElements(); ) {
            String alias = it.nextElement();
            Key key = ks.getKey(alias, keyPassword);
            if (key instanceof PrivateKey) {
                Certificate cert = ks.getCertificate(alias);
                if (cert != null) {
                    PublicKey publicKey = cert.getPublicKey();
                    keyPairs.add(new KeyPair(publicKey, (PrivateKey) key));
                }
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
