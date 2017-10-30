package org.ballerinalang.net.ftp.server;

import org.ballerinalang.connector.api.BallerinaConnectorException;
import org.ballerinalang.connector.api.BallerinaServerConnector;
import org.ballerinalang.connector.api.ConnectorUtils;
import org.ballerinalang.launcher.util.BServiceUtil;
import org.ballerinalang.launcher.util.CompileResult;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.transport.remotefilesystem.message.RemoteFileSystemEvent;

import java.lang.reflect.Field;
import java.util.Map;

import static org.ballerinalang.net.ftp.server.Constants.FTP_PACKAGE_NAME;

/**
 * Test class for the {@link FTPServerConnector}.
 */
@Test(sequential = true)
public class FTPServerConnectorTest {

    private FakeFtpServer ftpServer;

    @BeforeClass
    public void init() {
        ftpServer = new FakeFtpServer();
        ftpServer.setServerControlPort(48123);
        String username = "wso2";
        String password = "wso2123";
        String rootFolder = "/home/wso2";
        ftpServer.addUserAccount(new UserAccount(username, password, rootFolder));
        FileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry(rootFolder));
        fileSystem.add(new FileEntry("/home/wso2/file1.txt"));
        ftpServer.setFileSystem(fileSystem);
        ftpServer.start();
    }

    @Test
    public void testValidFTPServerConnectorSyntax() {
        CompileResult compileResult = BServiceUtil.setupProgramFile(this, "test-src/ftp/remote-system.bal");
        BallerinaServerConnector ballerinaServerConnector =
                ConnectorUtils.getBallerinaServerConnector(FTP_PACKAGE_NAME);
        final Field connectorMapInstance;
        try {
            connectorMapInstance = BallerinaServerConnector.class.getDeclaredField("connectorMap");
            connectorMapInstance.setAccessible(true);
            Map<String, ConnectorInfo> connectorInfoMap =
                    (Map<String, ConnectorInfo>) connectorMapInstance.get(ballerinaServerConnector);

            FTPServerConnector connector = (FTPServerConnector) ballerinaServerConnector;
            BallerinaFTPFileSystemListener systemListener =
                    new BallerinaFTPFileSystemListener(connectorInfoMap.get("._ftpServerConnector").getService());
            RemoteFileSystemEvent event = new RemoteFileSystemEvent("/home/ballerina/bal/file.txt");
            systemListener.onMessage(event);
            BServiceUtil.cleanup(compileResult);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            //Ignore
        }
    }

    @Test(expectedExceptions = BallerinaConnectorException.class,
            expectedExceptionsMessageRegExp = "Unable to find the associated configuration " +
                    "annotation for given service: .*")
    public void testMissingConfig() {
        execute("test-src/ftp/missing-config.bal");
    }

    @Test(dependsOnMethods = "testMissingConfig",
            expectedExceptions = BallerinaConnectorException.class,
            expectedExceptionsMessageRegExp = "More than one resource define for given service: .*")
    public void testMoreResources() {
        execute("test-src/ftp/more-resources.bal");
    }

    private void execute(String file) {
        CompileResult compileResult = BServiceUtil.setupProgramFile(this, file);
        BServiceUtil.cleanup(compileResult);
    }

    @AfterClass
    public void cleanup() {
        if (ftpServer != null && ftpServer.isStarted()) {
            ftpServer.stop();
        }
    }
}
