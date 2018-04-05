package org.ballerinalang.ftp.client.nativeimpl;

import org.ballerinalang.launcher.util.BCompileUtil;
import org.ballerinalang.launcher.util.BRunUtil;
import org.ballerinalang.launcher.util.CompileResult;
import org.ballerinalang.model.values.BInteger;
import org.ballerinalang.model.values.BString;
import org.ballerinalang.model.values.BStringArray;
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
 * FTP Client connector test class.
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
        fileSystem.add(new FileEntry("/home/wso2/file3.txt", content));
        fileSystem.add(new DirectoryEntry("/home/wso2/folder1"));
        fileSystem.add(new DirectoryEntry("/home/wso2/folder1/subfolder1"));
        fileSystem.add(new DirectoryEntry("/home/wso2/childDirectory"));
        fileSystem.add(new FileEntry("/home/wso2/child_directory/content1.txt"));
        fileSystem.add(new FileEntry("/home/wso2/child_directory/content2.txt"));
        ftpServer.setFileSystem(fileSystem);
        ftpServer.start();
        String filePath = "test-src/positive-client-connector-actions.bal";
        result = BCompileUtil.compile(filePath);
    }

    @Test
    public void testCreateNewDirectory() {
        BValue[] inputArg = { new BString(buildConnectionURL()), new BString(newFolder) };
        BRunUtil.invoke(result, "createDirectory", inputArg);
        Assert.assertTrue(fileSystem.exists(rootFolder + newFolder), "Folder not created.");
    }

    @Test(dependsOnMethods = "testCreateNewDirectory")
    public void testRemoveDirectory() {
        BValue[] inputArg = { new BString(buildConnectionURL()), new BString(newFolder) };
        BRunUtil.invoke(result, "removeDirectory", inputArg);
        Assert.assertFalse(fileSystem.exists(rootFolder + newFolder), "Folder not deleted.");
    }

    @Test
    public void testReadContent() {
        String url = "/file1.txt";
        BValue[] inputArg = { new BString(buildConnectionURL()), new BString(url) };
        final BValue[] readContents = BRunUtil.invoke(result, "readContent", inputArg);
        Assert.assertEquals(readContents[0].stringValue(), content, "File content mismatch.");
    }

    @Test
    public void testGetSize() {
        String url = "/file1.txt";
        BValue[] inputArg = { new BString(buildConnectionURL()), new BString(url) };
        final BValue[] readContents = BRunUtil.invoke(result, "size", inputArg);
        BInteger size = (BInteger) readContents[0];
        Assert.assertEquals(size.intValue(), 12, "File size mismatch.");
    }

    @Test
    public void testList() {
        String url = "/child_directory";
        BValue[] inputArg = { new BString(buildConnectionURL()), new BString(url) };
        final BValue[] readContents = BRunUtil.invoke(result, "list", inputArg);
        BStringArray list = (BStringArray) readContents[0];
        Assert.assertEquals(list.getStringArray().length, 2, "File list mismatch.");
    }

    @Test()
    public void testRename() {
        String source = "/file3.txt";
        String destination = "/move.txt";
        BValue[] inputArg = { new BString(buildConnectionURL()), new BString(source), new BString(destination) };
        BRunUtil.invoke(result, "rename", inputArg);
        Assert.assertFalse(fileSystem.exists(rootFolder + "/file3.txt"), "file not moved.");
        Assert.assertTrue(fileSystem.exists(rootFolder + "/move.txt"), "file not created.");
        final FileEntry entry = (FileEntry) fileSystem.getEntry(rootFolder + "/move.txt");
        InputStream inputStream = entry.createInputStream();
        String fileContent = new BufferedReader(new InputStreamReader(inputStream)).lines().
                collect(Collectors.joining("\n"));
        Assert.assertEquals(fileContent, content, "File content not identical.");
    }

    @Test()
    public void testWriteFile() {
        String source = "/write.txt";
        BValue[] inputArg = { new BString(buildConnectionURL()), new BString(source), new BString(content) };
        BRunUtil.invoke(result, "write", inputArg);
        Assert.assertTrue(fileSystem.exists(rootFolder + "/write.txt"), "file not created.");
        final FileEntry entry = (FileEntry) fileSystem.getEntry(rootFolder + "/write.txt");
        InputStream inputStream = entry.createInputStream();
        String fileContent = new BufferedReader(new InputStreamReader(inputStream)).lines().
                collect(Collectors.joining("\n"));
        Assert.assertEquals(fileContent, content, "File content not identical.");
    }

    @Test(dependsOnMethods = "testWriteFile")
    public void testAppend() {
        String source = "/write.txt";
        String appendContent = "New content";
        BValue[] inputArg = { new BString(buildConnectionURL()), new BString(source), new BString(appendContent) };
        BRunUtil.invoke(result, "append", inputArg);
        Assert.assertTrue(fileSystem.exists(rootFolder + "/write.txt"), "file not created.");
        final FileEntry entry = (FileEntry) fileSystem.getEntry(rootFolder + "/write.txt");
        InputStream inputStream = entry.createInputStream();
        String fileContent = new BufferedReader(new InputStreamReader(inputStream)).lines().
                collect(Collectors.joining("\n"));
        Assert.assertEquals(fileContent, content + appendContent, "File content not identical.");
    }

    @Test(dependsOnMethods = "testAppend")
    public void testDeleteFile() {
        String source = "/write.txt";
        BValue[] inputArg = { new BString(buildConnectionURL()), new BString(source) };
        BRunUtil.invoke(result, "fileDelete", inputArg);
        Assert.assertFalse(fileSystem.exists(rootFolder + "/write.txt"), "File not deleted.");
    }

    @AfterClass
    public void cleanup() {
        if (ftpServer != null && ftpServer.isStarted()) {
            ftpServer.stop();
        }
    }

    private String buildConnectionURL() {
        return username + ":" + password + "@localhost:" + serverPort + rootFolder;
    }
}
