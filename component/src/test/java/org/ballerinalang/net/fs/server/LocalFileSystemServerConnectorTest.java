package org.ballerinalang.net.fs.server;

import org.ballerinalang.connector.api.BallerinaConnectorException;
import org.ballerinalang.connector.api.BallerinaServerConnector;
import org.ballerinalang.connector.api.ConnectorUtils;
import org.ballerinalang.launcher.util.BServiceUtil;
import org.ballerinalang.launcher.util.CompileResult;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.transport.localfilesystem.server.connector.contract.LocalFileSystemEvent;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Map;

/**
 * Test class for {@link LocalFileSystemServerConnector}.
 */
@Test(sequential = true)
public class LocalFileSystemServerConnectorTest {

    private File rootDirectory;

    @BeforeClass
    public void init() {
        try {
            Path rootListenFolderPath = Files.createDirectory(Paths.get("target", "fs"));
            rootDirectory = rootListenFolderPath.toFile();
            rootDirectory.deleteOnExit();
        } catch (IOException e) {
            Assert.fail("Unable to create root folder to setup watch.", e);
        }
    }

    @AfterClass
    public void cleanup() {
        if (rootDirectory != null) {
            try {
                Files.walk(rootDirectory.toPath(), FileVisitOption.FOLLOW_LINKS)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException ignore) {
                //Ignore
            }
        }
    }

    @Test
    public void testValidLocalFileSystemServerConnectorSyntax() {
        CompileResult compileResult = BServiceUtil.setupProgramFile(this, "test-src/fs/file-system.bal");
        BallerinaServerConnector ballerinaServerConnector =
                ConnectorUtils.getBallerinaServerConnector(compileResult.getProgFile(), Constants
                        .FILE_SYSTEM_PACKAGE_NAME);
        LocalFileSystemServerConnector connector = (LocalFileSystemServerConnector) ballerinaServerConnector;
        try {
            final Field connectorMapInstance =
                    LocalFileSystemServerConnector.class.getDeclaredField("connectorMap");
            connectorMapInstance.setAccessible(true);
            Map<String, ConnectorInfo> connectorInfoMap =
                    (Map<String, ConnectorInfo>) connectorMapInstance.get(connector);
            BallerinaLocalFileSystemListener systemListener =
                    new BallerinaLocalFileSystemListener(connectorInfoMap.get("._fileSystem").getService());
            LocalFileSystemEvent event =
                    new LocalFileSystemEvent("/home/ballerina/bal/file.txt", "create");
            systemListener.onMessage(event);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            //Do nothing
        }
    }

    @Test(
            expectedExceptions = BallerinaConnectorException.class,
            expectedExceptionsMessageRegExp = "Unable to find the associated configuration " +
                    "annotation for given service: .*")
    public void testMissingConfig() {
        BServiceUtil.setupProgramFile(this, "test-src/fs/missing-config.bal");
    }

    @Test(dependsOnMethods = "testMissingConfig",
            expectedExceptions = BallerinaConnectorException.class,
            expectedExceptionsMessageRegExp = "More than one resource define for given service: .*")
    public void testMoreResources() {
        BServiceUtil.setupProgramFile(this, "test-src/fs/more-resources.bal");
    }

}
