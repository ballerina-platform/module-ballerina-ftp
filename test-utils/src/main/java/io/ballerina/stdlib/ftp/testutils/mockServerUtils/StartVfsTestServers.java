/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.ftp.testutils.mockServerUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts additional test servers specifically for VFS configuration behavioral testing.
 * This class is separate from StartServer to avoid interfering with existing test infrastructure.
 */
public final class StartVfsTestServers {

    private StartVfsTestServers() {}

    private static final Logger logger = LoggerFactory.getLogger(StartVfsTestServers.class);

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                throw new Exception("Please specify the resources directory as an argument");
            }

            logger.info("Starting VFS configuration test servers...");

            // Start SlowFtpServer for timeout testing
            try {
                SlowFtpServer.startSlowFtpServer(args[0], 21215, 1);  // Port 21215, 1s idle timeout
                logger.info("Slow FTP server started on port 21215 for timeout testing");
            } catch (Exception e) {
                logger.error("Could not start slow FTP server: " + e.getMessage(), e);
            }

            // Start CompressionEnabledSftpServer for compression testing
            try {
                CompressionEnabledSftpServer.startCompressionSftpServer(args[0], 21216);  // Port 21216
                logger.info("Compression-enabled SFTP server started on port 21216");
            } catch (Exception e) {
                logger.error("Could not start compression SFTP server: " + e.getMessage(), e);
            }

            logger.info("VFS test servers initialization complete. Servers will run until process is terminated.");

            // Keep the process alive
            Thread.currentThread().join();

        } catch (InterruptedException ex) {
            logger.error("VFS test servers interrupted: " + ex.getMessage(), ex);
            Thread.currentThread().interrupt();
            System.exit(1);
        } catch (Exception ex) {
            logger.error("Error starting VFS test servers: " + ex.getMessage(), ex);
            System.exit(1);
        }
    }
}
