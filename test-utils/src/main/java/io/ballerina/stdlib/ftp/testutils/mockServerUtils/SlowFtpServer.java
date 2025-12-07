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

import org.apache.ftpserver.DataConnectionConfiguration;
import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Creates a slow FTP server for timeout testing.
 * This server introduces deliberate delays to test timeout configurations.
 */
@SuppressWarnings("java:S2068")  // Hardcoded password acceptable for test utilities
public final class SlowFtpServer {

    private SlowFtpServer() {}

    private static final String username = "wso2";
    private static final String password = "wso2123";
    private static final Logger logger = LoggerFactory.getLogger("ballerina");
    private static FtpServer slowFtpServer;

    /**
     * Starts a slow FTP server with configurable idle timeout.
     * The server will be slow to respond, allowing timeout testing.
     *
     * @param resources The resources directory path
     * @param port The port to bind to
     * @param idleTimeout Idle timeout in seconds (how long before server times out idle connections)
     * @return null
     * @throws Exception if server fails to start
     */
    public static Object startSlowFtpServer(String resources, int port, int idleTimeout) throws Exception {
        final FtpServerFactory serverFactory = new FtpServerFactory();
        final ListenerFactory factory = new ListenerFactory();

        // Set a very short idle timeout to trigger timeout scenarios
        factory.setIdleTimeout(idleTimeout);
        factory.setPort(port);

        // Configure data connection with short timeouts
        DataConnectionConfigurationFactory dataConnFactory = new DataConnectionConfigurationFactory();
        dataConnFactory.setIdleTime(idleTimeout);
        DataConnectionConfiguration dataConnConfig = dataConnFactory.createDataConnectionConfiguration();
        factory.setDataConnectionConfiguration(dataConnConfig);

        final PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
        final UserManager userManager = userManagerFactory.createUserManager();
        BaseUser user = new BaseUser();
        user.setName(username);
        user.setPassword(password);
        File dataDirectory = new File(resources + "/datafiles");
        if (!dataDirectory.exists()) {
            dataDirectory.mkdirs();
        }
        user.setHomeDirectory(dataDirectory.getAbsolutePath());
        List<Authority> authorities = new ArrayList<>();
        authorities.add(new WritePermission());
        user.setAuthorities(authorities);

        try {
            userManager.save(user);
        } catch (FtpException e) {
            throw new Exception("Error saving user: " + e.getMessage());
        }

        serverFactory.setUserManager(userManager);

        Listener listener = factory.createListener();
        serverFactory.addListener("default", listener);
        slowFtpServer = serverFactory.createServer();
        slowFtpServer.start();

        int i = 0;
        while ((slowFtpServer.isStopped() || slowFtpServer.isSuspended()) && i < 10) {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
                i++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new Exception("Error in starting slow FTP server");
            }
        }
        if (i < 10) {
            logger.info("Started Slow FTP server on port " + port + " with idle timeout " + idleTimeout + "s");
        } else {
            throw new Exception("Could not start slow FTP server");
        }
        return null;
    }

    /**
     * Stops the slow FTP server.
     */
    public static void stopSlowFtpServer() {
        if (slowFtpServer != null && !slowFtpServer.isStopped() && !slowFtpServer.isSuspended()) {
            slowFtpServer.stop();
            logger.info("Stopped Slow FTP server");
        }
    }
}
