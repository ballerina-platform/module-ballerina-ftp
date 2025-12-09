/*
 * Copyright (c) 2017 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package io.ballerina.stdlib.ftp.transport.server.util;

import io.ballerina.stdlib.ftp.exception.RemoteFileSystemConnectorException;
import io.ballerina.stdlib.ftp.util.ExcludeCoverageFromGeneratedReport;
import io.ballerina.stdlib.ftp.util.FtpConstants;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.ftp.FtpFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.ftp.FtpFileType;
import org.apache.commons.vfs2.provider.ftps.FtpsDataChannelProtectionLevel;
import org.apache.commons.vfs2.provider.ftps.FtpsFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.ftps.FtpsMode;
import org.apache.commons.vfs2.provider.sftp.IdentityInfo;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import static io.ballerina.stdlib.ftp.util.FtpConstants.ENDPOINT_CONFIG_PREFERRED_METHODS;
import static io.ballerina.stdlib.ftp.util.FtpConstants.IDENTITY_PASS_PHRASE;
import static io.ballerina.stdlib.ftp.util.FtpConstants.SCHEME_FTP;
import static io.ballerina.stdlib.ftp.util.FtpConstants.SCHEME_FTPS;
import static io.ballerina.stdlib.ftp.util.FtpConstants.SCHEME_SFTP;

/**
 * Utility class for File Transport.
 */
public final class FileTransportUtils {

    private static final Logger log = LoggerFactory.getLogger(FileTransportUtils.class);

    private FileTransportUtils() {}

    private static final Pattern URL_PATTERN = Pattern.compile("^[a-z][a-z0-9+.-]*://", Pattern.CASE_INSENSITIVE);
    private static final Pattern USERINFO_WITH_PASSWORD = Pattern.compile("://([^/@:]+):([^/@]*)@");

    /**
     * A utility method for setting the relevant configurations for the file system in question.
     *
     * @param options Options to be used with the file system manager
     * @return A FileSystemOptions instance
     */
    public static FileSystemOptions attachFileSystemOptions(Map<String, String> options)
            throws RemoteFileSystemConnectorException {
        if (options == null) {
            return null;
        }
        FileSystemOptions opts = new FileSystemOptions();
        String listeningDirURI = options.get(FtpConstants.URI);
        String lowerCaseUri = listeningDirURI.toLowerCase(Locale.getDefault());
        if (lowerCaseUri.startsWith(SCHEME_FTPS)) { //
            setFtpsOptions(options, opts);
        } else if (lowerCaseUri.startsWith(SCHEME_FTP)) {
            setFtpOptions(options, opts);
        } else if (lowerCaseUri.startsWith(SCHEME_SFTP)) { 
            setSftpOptions(options, opts);
        }
        return opts;
    }

    private static void setFtpOptions(Map<String, String> options, FileSystemOptions opts)
            throws RemoteFileSystemConnectorException {
        final FtpFileSystemConfigBuilder configBuilder = FtpFileSystemConfigBuilder.getInstance();

        if (options.get(FtpConstants.PASSIVE_MODE) != null) {
            configBuilder.setPassiveMode(opts, Boolean.parseBoolean(options.get(FtpConstants.PASSIVE_MODE)));
        }
        if (options.get(FtpConstants.USER_DIR_IS_ROOT) != null) {
            configBuilder.setUserDirIsRoot(opts, Boolean.parseBoolean(options.get(FtpConstants.USER_DIR_IS_ROOT)));
        }


        if (options.get(FtpConstants.CONNECT_TIMEOUT) != null) {
            double connectTimeoutSeconds = Double.parseDouble(options.get(FtpConstants.CONNECT_TIMEOUT));
            Duration connectTimeout = Duration.ofMillis((long) (connectTimeoutSeconds * 1000));
            configBuilder.setConnectTimeout(opts, connectTimeout);
            log.debug("FTP connectTimeout set to {} seconds", connectTimeoutSeconds);
        }

        if (options.get(FtpConstants.FTP_DATA_TIMEOUT) != null) {
            double dataTimeoutSeconds = Double.parseDouble(options.get(FtpConstants.FTP_DATA_TIMEOUT));
            Duration dataTimeout = Duration.ofMillis((long) (dataTimeoutSeconds * 1000));
            configBuilder.setDataTimeout(opts, dataTimeout);
            log.debug("FTP dataTimeout set to {} seconds", dataTimeoutSeconds);
        }

        if (options.get(FtpConstants.FTP_SOCKET_TIMEOUT) != null) {
            double socketTimeoutSeconds = Double.parseDouble(options.get(FtpConstants.FTP_SOCKET_TIMEOUT));
            Duration socketTimeout = Duration.ofMillis((long) (socketTimeoutSeconds * 1000));
            configBuilder.setSoTimeout(opts, socketTimeout);
            log.debug("FTP socketTimeout set to {} seconds", socketTimeoutSeconds);
        }

        if (options.get(FtpConstants.FTP_FILE_TYPE) != null) {
            String fileTypeStr = options.get(FtpConstants.FTP_FILE_TYPE);
            if (FtpConstants.FILE_TYPE_ASCII.equalsIgnoreCase(fileTypeStr)) {
                configBuilder.setFileType(opts, FtpFileType.ASCII);
                log.debug("FTP file type set to ASCII");
            } else if (FtpConstants.FILE_TYPE_BINARY.equalsIgnoreCase(fileTypeStr)) {
                configBuilder.setFileType(opts, FtpFileType.BINARY);
                log.debug("FTP file type set to BINARY");
            } else {
                log.warn("Unknown FTP file type: {}, defaulting to BINARY", fileTypeStr);
                configBuilder.setFileType(opts, FtpFileType.BINARY);
            }
        }
    }

    private static void setFtpsOptions(Map<String, String> options, FileSystemOptions opts)
            throws RemoteFileSystemConnectorException {
        // Use FTPS-specific config builder for proper FTPS configuration
        final FtpsFileSystemConfigBuilder ftpsConfigBuilder = FtpsFileSystemConfigBuilder.getInstance();
        final FtpFileSystemConfigBuilder ftpConfigBuilder = FtpFileSystemConfigBuilder.getInstance();
        
        // Set common FTP options (passive mode, user dir as root)
        // These are inherited from FtpFileSystemConfigBuilder
        if (options.get(FtpConstants.PASSIVE_MODE) != null) {
            ftpConfigBuilder.setPassiveMode(opts, Boolean.parseBoolean(options.get(FtpConstants.PASSIVE_MODE)));
        }
        if (options.get(FtpConstants.USER_DIR_IS_ROOT) != null) {
            ftpConfigBuilder.setUserDirIsRoot(opts, Boolean.parseBoolean(options.get(FtpConstants.USER_DIR_IS_ROOT)));
        }
        
        // Handle implicit vs explicit FTPS mode using the recommended VFS2 API
        String ftpsMode = options.get(FtpConstants.ENDPOINT_CONFIG_FTPS_MODE);
        if (ftpsMode != null && ftpsMode.equals(FtpConstants.FTPS_MODE_IMPLICIT)) {
            // For implicit FTPS, set implicit SSL mode
            ftpsConfigBuilder.setFtpsMode(opts, FtpsMode.IMPLICIT);
        } else {
            // For explicit FTPS (default), set explicit mode
            ftpsConfigBuilder.setFtpsMode(opts, FtpsMode.EXPLICIT);
        }
        
        // Configure data channel protection and hostname verification
        configureFtpsSecurityOptions(ftpsConfigBuilder, opts, options);
        
        // Configure SSL/TLS certificates (KeyStore/TrustStore) for FTPS
        configureFtpsSslCertificates(ftpsConfigBuilder, opts, options); //
    }
    
    /**
     * Configures SSL/TLS certificates for FTPS by loading KeyStore and TrustStore
     * and setting KeyManager and TrustManager in VFS2.
     *
     * @param ftpsConfigBuilder The FTPS config builder
     * @param opts The file system options
     * @param options The configuration options map
     * @throws RemoteFileSystemConnectorException If configuration fails
     */
    private static void configureFtpsSslCertificates(FtpsFileSystemConfigBuilder ftpsConfigBuilder,
                                                     FileSystemOptions opts,
                                                     Map<String, String> options) 
            throws RemoteFileSystemConnectorException { //
        try {
            // Configure KeyManager if KeyStore is provided (for client authentication)
            String keystorePath = options.get(FtpConstants.ENDPOINT_CONFIG_KEYSTORE_PATH);
            String keystorePassword = options.get(FtpConstants.ENDPOINT_CONFIG_KEYSTORE_PASSWORD);
            if (keystorePath != null && !keystorePath.isEmpty()) {
                KeyManager[] keyManagers = createKeyManagers(keystorePath, keystorePassword);
                if (keyManagers.length > 0) {
                    ftpsConfigBuilder.setKeyManager(opts, keyManagers[0]);
                }
            }
            
            // Configure TrustManager if TrustStore is provided (for server certificate validation)
            String truststorePath = options.get(FtpConstants.ENDPOINT_CONFIG_TRUSTSTORE_PATH);
            String truststorePassword = options.get(FtpConstants.ENDPOINT_CONFIG_TRUSTSTORE_PASSWORD);
            if (truststorePath != null && !truststorePath.isEmpty()) {
                TrustManager[] trustManagers = createTrustManagers(truststorePath, truststorePassword);
                if (trustManagers.length > 0) {
                    ftpsConfigBuilder.setTrustManager(opts, trustManagers[0]);
                }
            }
        } catch (Exception e) {
            throw new RemoteFileSystemConnectorException(
                    "Failed to configure SSL/TLS certificates for FTPS: " + e.getMessage(), e);
        }
    }
    
    /**
     * Configures FTPS security options including data channel protection and hostname verification.
     *
     * @param ftpsConfigBuilder The FTPS config builder
     * @param opts The file system options
     * @param options The configuration options map
     */
    private static void configureFtpsSecurityOptions(FtpsFileSystemConfigBuilder ftpsConfigBuilder,
                                                     FileSystemOptions opts,
                                                     Map<String, String> options) {
        // Configure data channel protection level
        String protectionLevel = options.get(FtpConstants.ENDPOINT_CONFIG_FTPS_DATA_CHANNEL_PROTECTION);
        if (protectionLevel != null) {
            FtpsDataChannelProtectionLevel level = mapToVfs2ProtectionLevel(protectionLevel);
            ftpsConfigBuilder.setDataChannelProtectionLevel(opts, level);
            log.debug("FTPS data channel protection set to: {}", protectionLevel);
        } else {
            // Default to PRIVATE (secure)
            ftpsConfigBuilder.setDataChannelProtectionLevel(opts, FtpsDataChannelProtectionLevel.P);
            log.debug("FTPS data channel protection defaulting to PRIVATE");
        }
        
        // Configure hostname verification
        String verifyHostnameStr = options.get(FtpConstants.ENDPOINT_CONFIG_FTPS_VERIFY_HOSTNAME);
        boolean verifyHostname = verifyHostnameStr == null || 
                                Boolean.parseBoolean(verifyHostnameStr);
        if (verifyHostname) {
            // Strict hostname verification
            ftpsConfigBuilder.setHostnameVerifier(opts, HttpsURLConnection.getDefaultHostnameVerifier());
            log.debug("FTPS hostname verification enabled");
        } else {
            // To disable hostname verification (not recommended)
            ftpsConfigBuilder.setHostnameVerifier(opts, (hostname, session) -> true);
            log.debug("FTPS hostname verification disabled");
        }
    }
    
    /**
     * Maps Ballerina data channel protection level string to VFS2 enum value.
     *
     * @param level The protection level string (CLEAR, PRIVATE, SAFE, or CONFIDENTIAL)
     * @return The corresponding VFS2 FtpsDataChannelProtectionLevel enum value
     */
    private static FtpsDataChannelProtectionLevel mapToVfs2ProtectionLevel(String level) {
        switch (level.toUpperCase()) {
            case "CLEAR":
                return FtpsDataChannelProtectionLevel.C;
            case "PRIVATE":
                return FtpsDataChannelProtectionLevel.P;
            case "SAFE":
                return FtpsDataChannelProtectionLevel.S;
            case "CONFIDENTIAL":
                return FtpsDataChannelProtectionLevel.E;
            default:
                log.warn("Unknown data channel protection level: {}, defaulting to PRIVATE", level);
                return FtpsDataChannelProtectionLevel.P;
        }
    }
    
    /**
     * Creates KeyManager array from KeyStore file.
     *
     * @param keystorePath Path to the keystore file
     * @param keystorePassword Password for the keystore
     * @return Array of KeyManagers, or empty array if keystorePath is null or empty
     * @throws Exception If KeyManager creation fails
     */
    private static KeyManager[] createKeyManagers(String keystorePath, String keystorePassword)
            throws Exception { //
        if (keystorePath == null || keystorePath.isEmpty()) {
            return new KeyManager[0];
        }
        
        KeyStore keyStore = loadKeyStore(keystorePath, keystorePassword);
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        char[] password = (keystorePassword != null) ? keystorePassword.toCharArray() : null;
        keyManagerFactory.init(keyStore, password);
        return keyManagerFactory.getKeyManagers();
    }
    
    /**
     * Creates TrustManager array from TrustStore file.
     *
     * @param truststorePath Path to the truststore file
     * @param truststorePassword Password for the truststore
     * @return Array of TrustManagers, or empty array if truststorePath is null or empty
     * @throws Exception If TrustManager creation fails
     */
    private static TrustManager[] createTrustManagers(String truststorePath, String truststorePassword)
            throws Exception { //
        if (truststorePath == null || truststorePath.isEmpty()) {
            return new TrustManager[0];
        }
        
        KeyStore trustStore = loadKeyStore(truststorePath, truststorePassword);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        return trustManagerFactory.getTrustManagers();
    }
    
    /**
     * Loads a KeyStore from file.
     *
     * @param keystorePath Path to the keystore file
     * @param keystorePassword Password for the keystore
     * @return Loaded KeyStore
     * @throws Exception If KeyStore loading fails
     */
    private static KeyStore loadKeyStore(String keystorePath, String keystorePassword)
            throws Exception { //
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = (keystorePassword != null) ? keystorePassword.toCharArray() : null;
        try (FileInputStream fis = new FileInputStream(new File(keystorePath))) {
            keyStore.load(fis, password);
        }
        return keyStore;
    }

    private static void setSftpOptions(Map<String, String> options, FileSystemOptions opts)
            throws RemoteFileSystemConnectorException {
        final SftpFileSystemConfigBuilder configBuilder = SftpFileSystemConfigBuilder.getInstance();
        String value = options.get(ENDPOINT_CONFIG_PREFERRED_METHODS);
        configBuilder.setPreferredAuthentications(opts, value);
        boolean userDirIsRoot = Boolean.parseBoolean(options.get(FtpConstants.USER_DIR_IS_ROOT));
        configBuilder.setUserDirIsRoot(opts, userDirIsRoot);
        if (options.get(FtpConstants.IDENTITY) != null) {
            IdentityInfo identityInfo;
            if (options.containsKey(IDENTITY_PASS_PHRASE)) {
                identityInfo = new IdentityInfo(new File(options.get(FtpConstants.IDENTITY)),
                        options.get(IDENTITY_PASS_PHRASE).getBytes());
            } else {
                identityInfo = new IdentityInfo(new File(options.get(FtpConstants.IDENTITY)));
            }
            configBuilder.setIdentityInfo(opts, identityInfo);
        }
        if (options.get(FtpConstants.AVOID_PERMISSION_CHECK) != null) {
            try {
                configBuilder.setStrictHostKeyChecking(opts, "no");
            } catch (FileSystemException e) {
                throw new RemoteFileSystemConnectorException(e.getMessage(), e);
            }
        }

        if (options.get(FtpConstants.CONNECT_TIMEOUT) != null) {
            double connectTimeoutSeconds = Double.parseDouble(options.get(FtpConstants.CONNECT_TIMEOUT));
            Duration connectTimeout = Duration.ofMillis((long) (connectTimeoutSeconds * 1000));
            configBuilder.setConnectTimeout(opts, connectTimeout);
            log.debug("SFTP connectTimeout set to {} seconds", connectTimeoutSeconds);
        }

        if (options.get(FtpConstants.SFTP_SESSION_TIMEOUT) != null) {
            double sessionTimeoutSeconds = Double.parseDouble(options.get(FtpConstants.SFTP_SESSION_TIMEOUT));
            Duration sessionTimeoutMillis = Duration.ofMillis((long) (sessionTimeoutSeconds * 1000));
            configBuilder.setSessionTimeout(opts, sessionTimeoutMillis);
            log.debug("SFTP sessionTimeout set to {} seconds", sessionTimeoutSeconds);
        }

        // Compression configuration
        if (options.get(FtpConstants.SFTP_COMPRESSION) != null) {
            String compression = options.get(FtpConstants.SFTP_COMPRESSION);
            configBuilder.setCompression(opts, compression);
            log.debug("SFTP compression set to: {}", compression);
        }

        // Known hosts configuration
        if (options.get(FtpConstants.SFTP_KNOWN_HOSTS) != null) {
            String knownHostsPath = options.get(FtpConstants.SFTP_KNOWN_HOSTS);
            String expandedPath = expandTildePath(knownHostsPath);
            File knownHostsFile = new File(expandedPath);
            if (knownHostsFile.exists()) {
                try {
                    configBuilder.setKnownHosts(opts, knownHostsFile);
                    configBuilder.setStrictHostKeyChecking(opts, "yes");
                    log.debug("SFTP known_hosts configured from: {}", expandedPath);
                } catch (FileSystemException e) {
                    log.warn("Failed to set known_hosts file '{}': {}", expandedPath, e.getMessage());
                }
            } else {
                log.warn("SFTP known_hosts file not found at: {}", expandedPath);
            }
        }

        // Proxy configuration
        if (options.get(FtpConstants.PROXY_HOST) != null) {
            String proxyHost = options.get(FtpConstants.PROXY_HOST);
            String proxyPortStr = options.get(FtpConstants.PROXY_PORT);
            String proxyType = options.getOrDefault(FtpConstants.PROXY_TYPE, FtpConstants.PROXY_TYPE_HTTP);

            if (!proxyHost.isEmpty()) {
                int proxyPort = proxyPortStr != null ? Integer.parseInt(proxyPortStr) : 8080;
                configBuilder.setProxyHost(opts, proxyHost);
                configBuilder.setProxyPort(opts, proxyPort);

                // Set proxy type if supported (HTTP/SOCKS)
                configBuilder.setProxyType(opts, SftpFileSystemConfigBuilder.PROXY_HTTP);

                // Set proxy authentication if provided
                String proxyUsername = options.get(FtpConstants.PROXY_USERNAME);
                String proxyPassword = options.get(FtpConstants.PROXY_PASSWORD);
                if (proxyUsername != null && !proxyUsername.isEmpty()) {
                    configBuilder.setProxyUser(opts, proxyUsername);
                    if (proxyPassword != null) {
                        configBuilder.setProxyPassword(opts, proxyPassword);
                    }
                    log.debug("SFTP proxy authentication configured for user: {}", proxyUsername);
                }

                log.info("SFTP proxy configured: {} ({}:{})", proxyType, proxyHost, proxyPort);
            }
        }
    }

    private static String expandTildePath(String path) {
        if (path.startsWith("~/")) {
            return System.getProperty("user.home") + path.substring(1);
        } else if (path.equals("~")) {
            return System.getProperty("user.home");
        }
        return path;
    }

    /**
     * A utility method for masking the password in a file URI.
     *
     * @param url URL to be masked
     * @return The masked URL
     */
    @ExcludeCoverageFromGeneratedReport
    public static String maskUrlPassword(String url) {
        if (url == null) {
            return null;
        }
        if (!URL_PATTERN.matcher(url).find()) {
            return url;
        }
        return USERINFO_WITH_PASSWORD.matcher(url).replaceFirst("://$1:***@");
    }
}
