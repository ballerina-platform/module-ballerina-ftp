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

import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.SslConfigurationFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class FtpsServer {

    private FtpsServer() {}

    private static final Logger logger = LoggerFactory.getLogger("ballerina");
    private static FtpServer ftpsServer;

    public static void main(String args[]) throws InterruptedException {
        // Force Java to use TLS 1.2 globally. 
        // This fixes a FileZilla "User canceled (90)" / "GnuTLS error" 
        // without needing the newer library methods that caused compilation errors.
        System.setProperty("jdk.tls.server.protocols", "TLSv1.2");

        String mode = args.length > 0 ? args[0] : "EXPLICIT";
        initFtpsServer(mode);
        logger.info("Started Example FTPS server in {} mode", mode);
    }

    public static Object initFtpsServer(String mode) throws InterruptedException {
        final FtpServerFactory serverFactory = new FtpServerFactory();
        int port;
        boolean implicitSsl;
        
        if ("IMPLICIT".equalsIgnoreCase(mode)) {
            port = 990;
            implicitSsl = true;
        } else {
            port = 21214; 
            implicitSsl = false;
        }

        final ListenerFactory factory = new ListenerFactory();
        
        // Configure SSL/TLS
        SslConfigurationFactory ssl = new SslConfigurationFactory();
        String keystorePath = "src/main/resources/keystore.jks";
        File keystoreFile = new File(keystorePath);
        if (!keystoreFile.exists()) {
            logger.error("Keystore file not found at: {}", keystoreFile.getAbsolutePath());
            throw new RuntimeException("Keystore file not found at: " + keystoreFile.getAbsolutePath());
        }
        ssl.setKeystoreFile(keystoreFile);
        char[] keystorePassword = "changeit".toCharArray();
        ssl.setKeystorePassword(new String(keystorePassword));
        
        // Note: We removed the specific setSslProtocol call here because the 
        // System.setProperty in main() handles it more reliably for older libraries.
        
        factory.setSslConfiguration(ssl.createSslConfiguration());
        factory.setImplicitSsl(implicitSsl);

        // Configure Data Connection for Passive Mode (PASV)
        // This ensures directory listings (MLSD) work correctly.
        DataConnectionConfigurationFactory dataConfigFactory = new DataConnectionConfigurationFactory();
        dataConfigFactory.setPassiveExternalAddress("127.0.0.1"); 
        factory.setDataConnectionConfiguration(dataConfigFactory.createDataConnectionConfiguration());

        // Set up user management
        final PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
        final UserManager userManager = userManagerFactory.createUserManager();
        BaseUser user = new BaseUser();
        String finalUsername = "wso2";
        String finalPassword = "wso2123";
        user.setName(finalUsername);
        user.setPassword(finalPassword);
        
        // Set up data directory
        File dataDirectory = new File("src/main/resources/datafiles");
        if (!dataDirectory.exists()) {
            dataDirectory.mkdirs();
        }
        File inputDirectory = new File("src/main/resources/input");
        if (!inputDirectory.exists()) {
            inputDirectory.mkdirs();
        }
        
        user.setHomeDirectory(dataDirectory.getAbsolutePath());
        List<Authority> authorities = new ArrayList<>();
        authorities.add(new WritePermission());
        user.setAuthorities(authorities);
        try {
            userManager.save(user);
        } catch (FtpException e) {
            logger.error("Error saving user", e);
            throw new RuntimeException("Error saving user: " + e.getMessage(), e);
        }
        serverFactory.setUserManager(userManager);
        
        factory.setPort(port);
        serverFactory.addListener("default", factory.createListener());
        ftpsServer = serverFactory.createServer();
        
        try {
            ftpsServer.start();
        } catch (Exception e) {
            logger.error("Error starting FTPS server", e);
            throw new RuntimeException("Error starting FTPS server: " + e.getMessage(), e);
        }

        int i = 0;
        while ((ftpsServer.isStopped() || ftpsServer.isSuspended()) && i < 10) {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
                i++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterruptedException("Error in starting FTPS server");
            }
        }
        
        if (i < 10) {
            logger.info("Started FTPS server in {} mode on port {}", mode, port);
        } else {
            logger.error("Could not start FTPS server");
            throw new RuntimeException("Could not start FTPS server");
        }
        
        // Keep server running
        while (true) {
            Thread.sleep(1000);
        }
    }

    public static void stopFtpsServer() {
        if (ftpsServer != null && !ftpsServer.isSuspended() && !ftpsServer.isStopped()) {
            ftpsServer.stop();
            logger.info("Stopped FTPS server");
        }
    }
}
