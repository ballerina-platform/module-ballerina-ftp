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
import org.apache.commons.vfs2.provider.sftp.IdentityInfo;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;

import java.io.File;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.ballerina.stdlib.ftp.util.FtpConstants.ENDPOINT_CONFIG_PREFERRED_METHODS;
import static io.ballerina.stdlib.ftp.util.FtpConstants.IDENTITY_PASS_PHRASE;
import static io.ballerina.stdlib.ftp.util.FtpConstants.SCHEME_FTP;
import static io.ballerina.stdlib.ftp.util.FtpConstants.SCHEME_SFTP;

/**
 * Utility class for File Transport.
 */
public final class FileTransportUtils {

    private FileTransportUtils() {}

    private static final Pattern URL_PATTERN = Pattern.compile("[a-z]+://.*");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(":(?:[^/]+)@");

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
        if (listeningDirURI.toLowerCase(Locale.getDefault()).startsWith(SCHEME_FTP)) {
            setFtpOptions(options, opts);
        } else if (listeningDirURI.toLowerCase(Locale.getDefault()).startsWith(SCHEME_SFTP)) {
            setSftpOptions(options, opts);
        }
        return opts;
    }

    private static void setFtpOptions(Map<String, String> options, FileSystemOptions opts) {
        final FtpFileSystemConfigBuilder configBuilder = FtpFileSystemConfigBuilder.getInstance();
        if (options.get(FtpConstants.PASSIVE_MODE) != null) {
            configBuilder.setPassiveMode(opts, Boolean.parseBoolean(options.get(FtpConstants.PASSIVE_MODE)));
        }
        if (options.get(FtpConstants.USER_DIR_IS_ROOT) != null) {
            configBuilder.setUserDirIsRoot(opts, Boolean.parseBoolean(FtpConstants.USER_DIR_IS_ROOT));
        }
    }

    private static void setSftpOptions(Map<String, String> options, FileSystemOptions opts)
            throws RemoteFileSystemConnectorException {
        final SftpFileSystemConfigBuilder configBuilder = SftpFileSystemConfigBuilder.getInstance();
        String value = options.get(ENDPOINT_CONFIG_PREFERRED_METHODS);
        configBuilder.setPreferredAuthentications(opts, value);
        if (options.get(FtpConstants.USER_DIR_IS_ROOT) != null) {
            configBuilder.setUserDirIsRoot(opts, false);
        }
        if (options.get(FtpConstants.IDENTITY) != null) {
            try {
                IdentityInfo identityInfo;
                if (options.containsKey(IDENTITY_PASS_PHRASE)) {
                    identityInfo = new IdentityInfo(new File(options.get(FtpConstants.IDENTITY)),
                            options.get(IDENTITY_PASS_PHRASE).getBytes());
                } else {
                    identityInfo = new IdentityInfo(new File(options.get(FtpConstants.IDENTITY)));
                }
                configBuilder.setIdentityInfo(opts, identityInfo);
            } catch (FileSystemException e) {
                throw new RemoteFileSystemConnectorException(e.getMessage(), e);
            }
        }
        if (options.get(FtpConstants.AVOID_PERMISSION_CHECK) != null) {
            try {
                configBuilder.setStrictHostKeyChecking(opts, "no");
            } catch (FileSystemException e) {
                throw new RemoteFileSystemConnectorException(e.getMessage(), e);
            }
        }
        configBuilder.setConnectTimeout(opts, Duration.ofSeconds(10));
    }

    /**
     * A utility method for masking the password in a file URI.
     *
     * @param url URL to be masked
     * @return The masked URL
     */
    @ExcludeCoverageFromGeneratedReport
    public static String maskUrlPassword(String url) {
        Matcher urlMatcher = URL_PATTERN.matcher(url);
        if (urlMatcher.find()) {
            Matcher pwdMatcher = PASSWORD_PATTERN.matcher(url);
            return pwdMatcher.replaceFirst("\":***@\"");
        } else {
            return url;
        }
    }
}
