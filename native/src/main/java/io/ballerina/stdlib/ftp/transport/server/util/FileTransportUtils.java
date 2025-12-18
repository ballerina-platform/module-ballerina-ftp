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
import io.ballerina.stdlib.ftp.util.FtpUtil;
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
import java.security.KeyStore;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

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
    public static FileSystemOptions attachFileSystemOptions(Map<String, Object> options)
            throws RemoteFileSystemConnectorException {
        if (options == null) {
            return null;
        }
        FileSystemOptions opts = new FileSystemOptions();
        String listeningDirURI = (String) options.get(FtpConstants.URI);
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

    private static void setFtpOptions(Map<String, Object> options, FileSystemOptions opts)
            throws RemoteFileSystemConnectorException {
        final FtpFileSystemConfigBuilder configBuilder = FtpFileSystemConfigBuilder.getInstance();

        Object passiveModeObj = options.get(FtpConstants.PASSIVE_MODE);
        if (passiveModeObj != null) {
            configBuilder.setPassiveMode(opts, Boolean.parseBoolean(passiveModeObj.toString()));
        }
        Object userDirIsRootObj = options.get(FtpConstants.USER_DIR_IS_ROOT);
        if (userDirIsRootObj != null) {
            configBuilder.setUserDirIsRoot(opts, Boolean.parseBoolean(userDirIsRootObj.toString()));
        }


        Object connectTimeoutObj = options.get(FtpConstants.CONNECT_TIMEOUT);
        if (connectTimeoutObj != null) {
            double connectTimeoutSeconds = Double.parseDouble(connectTimeoutObj.toString());
            Duration connectTimeout = Duration.ofMillis((long) (connectTimeoutSeconds * 1000));
            configBuilder.setConnectTimeout(opts, connectTimeout);
            log.debug("FTP connectTimeout set to {} seconds", connectTimeoutSeconds);
        }

        Object dataTimeoutObj = options.get(FtpConstants.FTP_DATA_TIMEOUT);
        if (dataTimeoutObj != null) {
            double dataTimeoutSeconds = Double.parseDouble(dataTimeoutObj.toString());
            Duration dataTimeout = Duration.ofMillis((long) (dataTimeoutSeconds * 1000));
            configBuilder.setDataTimeout(opts, dataTimeout);
            log.debug("FTP dataTimeout set to {} seconds", dataTimeoutSeconds);
        }

        Object socketTimeoutObj = options.get(FtpConstants.FTP_SOCKET_TIMEOUT);
        if (socketTimeoutObj != null) {
            double socketTimeoutSeconds = Double.parseDouble(socketTimeoutObj.toString());
            Duration socketTimeout = Duration.ofMillis((long) (socketTimeoutSeconds * 1000));
            configBuilder.setSoTimeout(opts, socketTimeout);
            log.debug("FTP socketTimeout set to {} seconds", socketTimeoutSeconds);
        }

        Object fileTypeObj = options.get(FtpConstants.FTP_FILE_TYPE);
        if (fileTypeObj != null) {
            String fileTypeStr = fileTypeObj.toString();
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

    private static void setFtpsOptions(Map<String, Object> options, FileSystemOptions opts)
            throws RemoteFileSystemConnectorException {
        // Use FTPS-specific config builder for proper FTPS configuration
        final FtpsFileSystemConfigBuilder ftpsConfigBuilder = FtpsFileSystemConfigBuilder.getInstance();
        
        // Set common FTP options (passive mode, user dir as root) using FTPS builder
        // These methods are inherited from FtpFileSystemConfigBuilder 
        // but must be called on the FTPS builder to ensure they are applied to the ftps. namespace
        Object passiveModeObj = options.get(FtpConstants.PASSIVE_MODE);
        if (passiveModeObj != null) {
            ftpsConfigBuilder.setPassiveMode(opts, Boolean.parseBoolean(passiveModeObj.toString()));
        }
        Object userDirIsRootObj = options.get(FtpConstants.USER_DIR_IS_ROOT);
        if (userDirIsRootObj != null) {
            ftpsConfigBuilder.setUserDirIsRoot(opts, Boolean.parseBoolean(userDirIsRootObj.toString()));
        }

        Object connectTimeoutObj = options.get(FtpConstants.CONNECT_TIMEOUT);
        if (connectTimeoutObj != null) {
            double connectTimeoutSeconds = Double.parseDouble(connectTimeoutObj.toString());
            Duration connectTimeout = Duration.ofMillis((long) (connectTimeoutSeconds * 1000));
            ftpsConfigBuilder.setConnectTimeout(opts, connectTimeout);
            log.debug("FTPS connectTimeout set to {} seconds", connectTimeoutSeconds);
        }

        Object dataTimeoutObj = options.get(FtpConstants.FTP_DATA_TIMEOUT);
        if (dataTimeoutObj != null) {
            double dataTimeoutSeconds = Double.parseDouble(dataTimeoutObj.toString());
            Duration dataTimeout = Duration.ofMillis((long) (dataTimeoutSeconds * 1000));
            ftpsConfigBuilder.setDataTimeout(opts, dataTimeout);
            log.debug("FTPS dataTimeout set to {} seconds", dataTimeoutSeconds);
        }

        Object socketTimeoutObj = options.get(FtpConstants.FTP_SOCKET_TIMEOUT);
        if (socketTimeoutObj != null) {
            double socketTimeoutSeconds = Double.parseDouble(socketTimeoutObj.toString());
            Duration socketTimeout = Duration.ofMillis((long) (socketTimeoutSeconds * 1000));
            ftpsConfigBuilder.setSoTimeout(opts, socketTimeout);
            log.debug("FTPS socketTimeout set to {} seconds", socketTimeoutSeconds);
        }

        Object fileTypeObj = options.get(FtpConstants.FTP_FILE_TYPE);
        if (fileTypeObj != null) {
            String fileTypeStr = fileTypeObj.toString();
            if (FtpConstants.FILE_TYPE_ASCII.equalsIgnoreCase(fileTypeStr)) {
                ftpsConfigBuilder.setFileType(opts, FtpFileType.ASCII);
                log.debug("FTPS file type set to ASCII");
            } else if (FtpConstants.FILE_TYPE_BINARY.equalsIgnoreCase(fileTypeStr)) {
                ftpsConfigBuilder.setFileType(opts, FtpFileType.BINARY);
                log.debug("FTPS file type set to BINARY");
            } else {
                log.warn("Unknown FTPS file type: {}, defaulting to BINARY", fileTypeStr);
                ftpsConfigBuilder.setFileType(opts, FtpFileType.BINARY);
            }
        } else {
            // Default to BINARY when file type is not specified
            // This is required for VFS to determine file type, especially with CLEAR data channel protection
            ftpsConfigBuilder.setFileType(opts, FtpFileType.BINARY);
            log.debug("FTPS file type defaulting to BINARY");
        }
        
        // Handle implicit vs explicit FTPS mode using the recommended VFS2 API
        Object ftpsModeObj = options.get(FtpConstants.ENDPOINT_CONFIG_FTPS_MODE);
        if (ftpsModeObj != null && FtpConstants.FTPS_MODE_IMPLICIT.equalsIgnoreCase(ftpsModeObj.toString())) {
            // For implicit FTPS, set implicit SSL mode
            ftpsConfigBuilder.setFtpsMode(opts, FtpsMode.IMPLICIT);
        } else {
            // For explicit FTPS (default), set explicit mode
            ftpsConfigBuilder.setFtpsMode(opts, FtpsMode.EXPLICIT);
        }
        
        // Configure data channel protection
        configureFtpsSecurityOptions(ftpsConfigBuilder, opts, options);
        
        // Configure SSL/TLS certificates (KeyStore/TrustStore) for FTPS
        configureFtpsSslCertificates(ftpsConfigBuilder, opts, options);
    }
    
    /**
     * Configures SSL/TLS certificates for FTPS by loading KeyStore and TrustStore
     * from paths
     * and setting KeyManager and TrustManager in VFS2.
     *
     * @param ftpsConfigBuilder The FTPS config builder
     * @param opts              The file system options
     * @param options           The configuration options map
     * @throws RemoteFileSystemConnectorException If configuration fails
     */
    private static void configureFtpsSslCertificates(FtpsFileSystemConfigBuilder ftpsConfigBuilder,
            FileSystemOptions opts,
            Map<String, Object> options)
            throws RemoteFileSystemConnectorException {
        try {
            // 1. Configure KeyStore (Client Certificate)
            Object keystorePathObj = options.get(FtpConstants.ENDPOINT_CONFIG_KEYSTORE_PATH);
            if (keystorePathObj != null) {
                String keyStorePath = (String) keystorePathObj;
                Object passwordObj = options.get(FtpConstants.ENDPOINT_CONFIG_KEYSTORE_PASSWORD);
                String password = (passwordObj != null) ? passwordObj.toString() : null;

                // Load KeyStore here
                KeyStore keyStore = FtpUtil.loadKeyStore(keyStorePath, password);

                // Init KeyManagerFactory
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                char[] passChars = (password != null) ? password.toCharArray() : null;

                kmf.init(keyStore, passChars);
                KeyManager[] keyManagers = kmf.getKeyManagers();

                if (keyManagers != null && keyManagers.length > 0) {
                    ftpsConfigBuilder.setKeyManager(opts, keyManagers[0]);
                } else {
                    log.warn("FTPS configured with Keystore path {} but no KeyManagers were found.", keyStorePath);
                }
            }

            // 2. Configure TrustStore (Server Validation)
            Object truststorePathObj = options.get(FtpConstants.ENDPOINT_CONFIG_TRUSTSTORE_PATH);
            if (truststorePathObj != null) {
                String trustStorePath = (String) truststorePathObj;
                Object passwordObj = options.get(FtpConstants.ENDPOINT_CONFIG_TRUSTSTORE_PASSWORD);
                String password = (passwordObj != null) ? passwordObj.toString() : null;

                // Load TrustStore here
                KeyStore trustStore = FtpUtil.loadKeyStore(trustStorePath, password);

                // Init TrustManagerFactory
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(trustStore);
                TrustManager[] trustManagers = tmf.getTrustManagers();

                if (trustManagers != null && trustManagers.length > 0) {
                    ftpsConfigBuilder.setTrustManager(opts, trustManagers[0]);
                } else {
                    log.warn("FTPS configured with TrustStore path {} but no TrustManagers were found.",
                            trustStorePath);
                }
            }
        } catch (Exception e) {
            // Wrap FtpUtil.loadKeyStore exceptions (BallerinaFtpException) and others
            throw new RemoteFileSystemConnectorException(
                    "Failed to configure SSL/TLS certificates for FTPS: " + e.getMessage(), e);
        }
    }
    
    /**
     * Configures FTPS security options including data channel protection.
     *
     * @param ftpsConfigBuilder The FTPS config builder
     * @param opts The file system options
     * @param options The configuration options map
     */
    private static void configureFtpsSecurityOptions(FtpsFileSystemConfigBuilder ftpsConfigBuilder,
                                                     FileSystemOptions opts,
                                                     Map<String, Object> options) {
        // Configure data channel protection level
        Object protectionLevelObj = options.get(FtpConstants.ENDPOINT_CONFIG_FTPS_DATA_CHANNEL_PROTECTION);
        if (protectionLevelObj != null) {
            String protectionLevel = protectionLevelObj.toString();
            FtpsDataChannelProtectionLevel level = mapToVfs2ProtectionLevel(protectionLevel);
            ftpsConfigBuilder.setDataChannelProtectionLevel(opts, level);
            log.debug("FTPS data channel protection set to: {}", protectionLevel);
        } else {
            // Default to PRIVATE (secure)
            ftpsConfigBuilder.setDataChannelProtectionLevel(opts, FtpsDataChannelProtectionLevel.P);
            log.debug("FTPS data channel protection defaulting to PRIVATE");
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
    

    private static void setSftpOptions(Map<String, Object> options, FileSystemOptions opts)
            throws RemoteFileSystemConnectorException {
        final SftpFileSystemConfigBuilder configBuilder = SftpFileSystemConfigBuilder.getInstance();
        Object preferredMethodsObj = options.get(ENDPOINT_CONFIG_PREFERRED_METHODS);
        if (preferredMethodsObj != null) {
            configBuilder.setPreferredAuthentications(opts, preferredMethodsObj.toString());
        }
        Object userDirIsRootObj = options.get(FtpConstants.USER_DIR_IS_ROOT);
        boolean userDirIsRoot = userDirIsRootObj != null && Boolean.parseBoolean(userDirIsRootObj.toString());
        configBuilder.setUserDirIsRoot(opts, userDirIsRoot);
        Object identityObj = options.get(FtpConstants.IDENTITY);
        if (identityObj != null) {
            IdentityInfo identityInfo;
            Object passPhraseObj = options.get(IDENTITY_PASS_PHRASE);
            if (passPhraseObj != null) {
                identityInfo = new IdentityInfo(new File(identityObj.toString()),
                        passPhraseObj.toString().getBytes());
            } else {
                identityInfo = new IdentityInfo(new File(identityObj.toString()));
            }
            configBuilder.setIdentityInfo(opts, identityInfo);
        }
        Object avoidPermissionCheckObj = options.get(FtpConstants.AVOID_PERMISSION_CHECK);
        if (avoidPermissionCheckObj != null) {
            try {
                configBuilder.setStrictHostKeyChecking(opts, "no");
            } catch (FileSystemException e) {
                throw new RemoteFileSystemConnectorException(e.getMessage(), e);
            }
        }

        Object connectTimeoutObj = options.get(FtpConstants.CONNECT_TIMEOUT);
        if (connectTimeoutObj != null) {
            double connectTimeoutSeconds = Double.parseDouble(connectTimeoutObj.toString());
            Duration connectTimeout = Duration.ofMillis((long) (connectTimeoutSeconds * 1000));
            configBuilder.setConnectTimeout(opts, connectTimeout);
            log.debug("SFTP connectTimeout set to {} seconds", connectTimeoutSeconds);
        }

        Object sessionTimeoutObj = options.get(FtpConstants.SFTP_SESSION_TIMEOUT);
        if (sessionTimeoutObj != null) {
            double sessionTimeoutSeconds = Double.parseDouble(sessionTimeoutObj.toString());
            Duration sessionTimeoutMillis = Duration.ofMillis((long) (sessionTimeoutSeconds * 1000));
            configBuilder.setSessionTimeout(opts, sessionTimeoutMillis);
            log.debug("SFTP sessionTimeout set to {} seconds", sessionTimeoutSeconds);
        }

        // Compression configuration
        Object compressionObj = options.get(FtpConstants.SFTP_COMPRESSION);
        if (compressionObj != null) {
            String compression = compressionObj.toString();
            configBuilder.setCompression(opts, compression);
            log.debug("SFTP compression set to: {}", compression);
        }

        // Known hosts configuration
        Object knownHostsObj = options.get(FtpConstants.SFTP_KNOWN_HOSTS);
        if (knownHostsObj != null) {
            String knownHostsPath = knownHostsObj.toString();
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
        Object proxyHostObj = options.get(FtpConstants.PROXY_HOST);
        if (proxyHostObj != null) {
            String proxyHost = proxyHostObj.toString();
            Object proxyPortObj = options.get(FtpConstants.PROXY_PORT);
            String proxyPortStr = proxyPortObj != null ? proxyPortObj.toString() : null;
            Object proxyTypeObj = options.get(FtpConstants.PROXY_TYPE);
            String proxyType = proxyTypeObj != null ? proxyTypeObj.toString() : FtpConstants.PROXY_TYPE_HTTP;

            if (!proxyHost.isEmpty()) {
                int proxyPort = proxyPortStr != null ? Integer.parseInt(proxyPortStr) : 8080;
                configBuilder.setProxyHost(opts, proxyHost);
                configBuilder.setProxyPort(opts, proxyPort);

                // Set proxy type if supported (HTTP/SOCKS)
                configBuilder.setProxyType(opts, SftpFileSystemConfigBuilder.PROXY_HTTP);

                // Set proxy authentication if provided
                Object proxyUsernameObj = options.get(FtpConstants.PROXY_USERNAME);
                Object proxyPasswordObj = options.get(FtpConstants.PROXY_PASSWORD);
                if (proxyUsernameObj != null && !proxyUsernameObj.toString().isEmpty()) {
                    String proxyUsername = proxyUsernameObj.toString();
                    configBuilder.setProxyUser(opts, proxyUsername);
                    if (proxyPasswordObj != null) {
                        configBuilder.setProxyPassword(opts, proxyPasswordObj.toString());
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
