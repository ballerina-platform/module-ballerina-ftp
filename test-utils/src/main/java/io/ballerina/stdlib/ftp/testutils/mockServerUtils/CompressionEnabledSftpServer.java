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
import org.apache.sshd.server.SshServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

/**
 * Creates an SFTP server with compression enabled for testing compression configurations.
 */
@SuppressWarnings({"java:S2068", "java:S6437"})  // Hardcoded credentials acceptable for test utilities
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
        compressionSftpServer = SshServer.setUpDefaultServer();

        SftpServerUtil.setupBasicServerConfig(compressionSftpServer, resources, port);

        // Enable compression support
        compressionSftpServer.setCompressionFactories(Arrays.asList(
                BuiltinCompressions.zlib,
                BuiltinCompressions.none
        ));

        try {
            SftpServerUtil.setupAuthentication(compressionSftpServer, resources, sftpAuthStatusHolder,
                    username, password);
            compressionSftpServer.start();
        } catch (Exception e) {
            throw new Exception("Error while starting compression-enabled SFTP server: " + e.getMessage());
        }

        SftpServerUtil.waitForServerStart(compressionSftpServer);
        logger.info("Started Compression-enabled SFTP server on port " + port);
        return null;
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
