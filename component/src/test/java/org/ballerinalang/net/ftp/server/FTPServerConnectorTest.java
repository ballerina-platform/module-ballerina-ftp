package org.ballerinalang.net.ftp.server;

import org.ballerinalang.launcher.util.BCompileUtil;
import org.ballerinalang.launcher.util.BRunUtil;
import org.ballerinalang.launcher.util.BServiceUtil;
import org.ballerinalang.launcher.util.CompileResult;
import org.ballerinalang.model.values.BBoolean;
import org.ballerinalang.model.values.BValue;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test class for the {@link}.
 */
@Test(sequential = true)
public class FTPServerConnectorTest {

    private FakeFtpServer ftpServer;
    private FileSystem fileSystem;

    @BeforeClass
    public void init() {
        ftpServer = new FakeFtpServer();
        ftpServer.setServerControlPort(48123);
        String username = "wso2";
        String password = "wso2123";
        String rootFolder = "/home/wso2";
        ftpServer.addUserAccount(new UserAccount(username, password, rootFolder));
        fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry(rootFolder));
        fileSystem.add(new FileEntry("/home/wso2/file1.txt"));
        ftpServer.setFileSystem(fileSystem);
        ftpServer.start();
    }

    @Test
    public void testValidFTPServerConnectorSyntax() {
        CompileResult compileResult = BCompileUtil.compileAndSetup("test-src/ftp/remote-system.bal");
        BServiceUtil.runService(compileResult);
        fileSystem.add(new FileEntry("/home/wso2/newFile.txt"));
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // Ignore.
        }
        final BValue[] result = BRunUtil.invokeStateful(compileResult, "isInvoked");
        BBoolean isInvoked = (BBoolean) result[0];
        Assert.assertTrue(isInvoked.booleanValue(), "Resource didn't invoke for the file create.");
    }

    @AfterClass
    public void cleanup() {
        if (ftpServer != null && ftpServer.isStarted()) {
            ftpServer.stop();
        }
    }
}
