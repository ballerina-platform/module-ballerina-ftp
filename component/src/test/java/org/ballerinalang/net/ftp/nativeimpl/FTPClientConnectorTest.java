package org.ballerinalang.net.ftp.nativeimpl;

import org.ballerinalang.launcher.util.BCompileUtil;
import org.ballerinalang.launcher.util.BRunUtil;
import org.ballerinalang.launcher.util.CompileResult;
import org.ballerinalang.model.values.BBoolean;
import org.ballerinalang.model.values.BString;
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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

/**
 * FTP Client connector createFile test class.
 * {@link CreateFile}
 */
@Test(sequential = true)
public class FTPClientConnectorTest {

    private FakeFtpServer ftpServer;
    private FileSystem fileSystem;
    private String username = "wso2";
    private String password = "wso2123";
    private String rootFolder = "/home/wso2";
    private int serverPort = 49567;
    private CompileResult result;
    private final String newFolder = "/newFolder";
    private final String content = "File content";

    @BeforeClass
    public void init() {
        ftpServer = new FakeFtpServer();
        ftpServer.setServerControlPort(serverPort);
        ftpServer.addUserAccount(new UserAccount(username, password, rootFolder));
        fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry(rootFolder));
        fileSystem.add(new FileEntry("/home/wso2/file1.txt", content));
        fileSystem.add(new FileEntry("/home/wso2/file2.txt", content));
        ftpServer.setFileSystem(fileSystem);
        ftpServer.start();
        String filePath = "test-src/ftp/positive-client-connector-actions.bal";
        result = BCompileUtil.compile(filePath);
    }

    @Test
    public void testCreateNewFolder() {
        String url = buildConnectionURL() + newFolder;
        BValue[] inputArg = {new BString(url), new BBoolean(true)};
        BRunUtil.invoke(result, "createFile", inputArg);
        Assert.assertTrue(fileSystem.exists(rootFolder + newFolder), "Folder not created.");
    }

    @Test(dependsOnMethods = "testCreateNewFolder")
    public void testIsExist() {
        String url = buildConnectionURL() + newFolder;
        BValue[] inputArg = {new BString(url)};
        final BValue[] isExists = BRunUtil.invoke(result, "isExist", inputArg);
        Assert.assertTrue(((BBoolean) isExists[0]).booleanValue(), "Folder not exist");
        Assert.assertTrue(fileSystem.exists(rootFolder + newFolder), "Folder not created.");
    }

    @Test
    public void testReadContent() {
        String url = buildConnectionURL() + "/file1.txt";
        BValue[] inputArg = {new BString(url)};
        final BValue[] readContents = BRunUtil.invoke(result, "readContent", inputArg);
        Assert.assertEquals(readContents[0].stringValue(), content, "File content mismatch.");
    }

    @Test
    public void testCopyFiles() {
        String source = buildConnectionURL() + "/file2.txt";
        String destination = buildConnectionURL() + "/file3.txt";
        BValue[] inputArg = {new BString(source), new BString(destination)};
        BRunUtil.invoke(result, "copyFiles", inputArg);
        Assert.assertTrue(fileSystem.exists(rootFolder + "/file3.txt"), "file not created.");
        final FileEntry entry = (FileEntry) fileSystem.getEntry(rootFolder + "/file3.txt");
        InputStream inputStream = entry.createInputStream();
        String fileContent = new BufferedReader(new InputStreamReader(inputStream)).lines().
                collect(Collectors.joining("\n"));
        Assert.assertEquals(fileContent, content, "File content not identical.");
    }

    @Test(dependsOnMethods = "testCopyFiles")
    public void testMoveFile() {
        String source = buildConnectionURL() + "/file2.txt";
        String destination = buildConnectionURL() + "/move.txt";
        BValue[] inputArg = {new BString(source), new BString(destination)};
        BRunUtil.invoke(result, "moveFile", inputArg);
        Assert.assertFalse(fileSystem.exists(rootFolder + "/file2.txt"), "file not moved.");
        Assert.assertTrue(fileSystem.exists(rootFolder + "/move.txt"), "file not created.");
        final FileEntry entry = (FileEntry) fileSystem.getEntry(rootFolder + "/move.txt");
        InputStream inputStream = entry.createInputStream();
        String fileContent = new BufferedReader(new InputStreamReader(inputStream)).lines().
                collect(Collectors.joining("\n"));
        Assert.assertEquals(fileContent, content, "File content not identical.");
    }

    @Test()
    public void testWriteFile() {
        String source = buildConnectionURL() + "/write.txt";
        BValue[] inputArg = {new BString(source), new BString(content)};
        BRunUtil.invoke(result, "write", inputArg);
        Assert.assertTrue(fileSystem.exists(rootFolder + "/write.txt"), "file not created.");
        final FileEntry entry = (FileEntry) fileSystem.getEntry(rootFolder + "/write.txt");
        InputStream inputStream = entry.createInputStream();
        String fileContent = new BufferedReader(new InputStreamReader(inputStream)).lines().
                collect(Collectors.joining("\n"));
        Assert.assertEquals(fileContent, content, "File content not identical.");
    }

    @Test(dependsOnMethods = "testWriteFile")
    public void testDeleteFile() {
        String source = buildConnectionURL() + "/write.txt";
        BValue[] inputArg = {new BString(source)};
        BRunUtil.invoke(result, "fileDelete", inputArg);
        Assert.assertFalse(fileSystem.exists(rootFolder + "/write.txt"), "file not deleted.");
    }

    @AfterClass
    public void cleanup() {
        if (ftpServer != null && ftpServer.isStarted()) {
            ftpServer.stop();
        }
    }

    private String buildConnectionURL() {
        return "ftp://" + username + ":" + password + "@localhost:" + serverPort + rootFolder;
    }
}
