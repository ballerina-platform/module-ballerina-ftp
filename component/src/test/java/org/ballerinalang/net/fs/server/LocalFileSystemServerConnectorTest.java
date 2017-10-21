package org.ballerinalang.net.fs.server;

import org.ballerinalang.connector.api.BallerinaConnectorException;
import org.ballerinalang.connector.api.BallerinaServerConnector;
import org.ballerinalang.connector.api.ConnectorUtils;
import org.ballerinalang.net.utils.CompileResult;
import org.ballerinalang.net.utils.EnvironmentInitializer;
import org.testng.annotations.Test;
import org.wso2.carbon.transport.localfilesystem.server.connector.contract.LocalFileSystemEvent;

/**
 * Test class for {@link LocalFileSystemServerConnector}.
 */
@Test(sequential = true)
public class LocalFileSystemServerConnectorTest {

    @Test
    public void testValidLocalFileSystemServerConnectorSyntax() {
        CompileResult compileResult = EnvironmentInitializer.setupProgramFile("test-src/fs/file-system.bal");
        BallerinaServerConnector ballerinaServerConnector =
                ConnectorUtils.getBallerinaServerConnector(Constants.FILE_SYSTEM_PACKAGE_NAME);
        LocalFileSystemServerConnector connector = (LocalFileSystemServerConnector) ballerinaServerConnector;
        BallerinaLocalFileSystemListener systemListener = new BallerinaLocalFileSystemListener(connector);
        LocalFileSystemEvent event = new LocalFileSystemEvent("/home/ballerina/bal/file.txt", "create");
        event.setProperty(Constants.TRANSPORT_PROPERTY_SERVICE_NAME, "._fileSystem");
        systemListener.onMessage(event);
        EnvironmentInitializer.cleanup(compileResult);
    }

    @Test(dependsOnMethods = "testValidLocalFileSystemServerConnectorSyntax",
            expectedExceptions = BallerinaConnectorException.class,
            expectedExceptionsMessageRegExp = "Unable to find the associated configuration " +
                    "annotation for given service: .*")
    public void testMissingConfig() {
        execute("test-src/fs/missing-config.bal");
    }

    @Test(dependsOnMethods = "testMissingConfig",
            expectedExceptions = BallerinaConnectorException.class,
            expectedExceptionsMessageRegExp = "More than one resource define for given service: .*")
    public void testMoreResources() {
        execute("test-src/fs/more-resources.bal");
    }

    @Test(dependsOnMethods = "testMoreResources",
            expectedExceptions = BallerinaConnectorException.class,
            expectedExceptionsMessageRegExp = "No Service found to handle the service request.")
    public void testDispatchToNonExistService() {
        CompileResult compileResult = EnvironmentInitializer.setupProgramFile("test-src/fs/file-system.bal");
        BallerinaServerConnector ballerinaServerConnector =
                ConnectorUtils.getBallerinaServerConnector(Constants.FILE_SYSTEM_PACKAGE_NAME);
        LocalFileSystemServerConnector connector = (LocalFileSystemServerConnector) ballerinaServerConnector;
        BallerinaLocalFileSystemListener systemListener = new BallerinaLocalFileSystemListener(connector);
        LocalFileSystemEvent event = new LocalFileSystemEvent("/home/ballerina/bal/file.txt", "create");
        event.setProperty(Constants.TRANSPORT_PROPERTY_SERVICE_NAME, "._NonExistService");
        systemListener.onMessage(event);
        EnvironmentInitializer.cleanup(compileResult);
    }

    @Test(dependsOnMethods = "testDispatchToNonExistService",
            expectedExceptions = BallerinaConnectorException.class,
            expectedExceptionsMessageRegExp = "Could not find a service to dispatch. " +
                    "TRANSPORT_FILE_SERVICE_NAME property not set or empty.")
    public void testServiceNameMissing() {
        CompileResult compileResult = EnvironmentInitializer.setupProgramFile("test-src/fs/file-system.bal");
        BallerinaServerConnector ballerinaServerConnector =
                ConnectorUtils.getBallerinaServerConnector(Constants.FILE_SYSTEM_PACKAGE_NAME);
        LocalFileSystemServerConnector connector = (LocalFileSystemServerConnector) ballerinaServerConnector;
        BallerinaLocalFileSystemListener systemListener = new BallerinaLocalFileSystemListener(connector);
        LocalFileSystemEvent event = new LocalFileSystemEvent("/home/ballerina/bal/file.txt", "create");
        systemListener.onMessage(event);
        EnvironmentInitializer.cleanup(compileResult);
    }

    /*@Test(dependsOnMethods = "testServiceNameMissing",
            expectedExceptions = BallerinaConnectorException.class,
            expectedExceptionsMessageRegExp = "Could not find a service to dispatch. " +
                    "TRANSPORT_FILE_SERVICE_NAME property not set or empty.")
    public void testInvalidSignature() {
        execute("test-src/fs/invalid-signature.bal");
    }*/

    private void execute(String file) {
        CompileResult compileResult = EnvironmentInitializer.setupProgramFile(file);
        EnvironmentInitializer.cleanup(compileResult);
    }
}
