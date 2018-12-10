/*
 * Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.ballerinalang.ftp.client;

import org.ballerinalang.launcher.util.BCompileUtil;
import org.ballerinalang.launcher.util.BRunUtil;
import org.ballerinalang.launcher.util.CompileResult;
import org.ballerinalang.model.values.BBoolean;
import org.ballerinalang.model.values.BInteger;
import org.ballerinalang.model.values.BString;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.model.values.BValueArray;
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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
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
    public void testIsDirectory() {
        BValue[] inputArg = { new BString(buildConnectionURL()), new BString(newFolder) };
        final BValue[] results = BRunUtil.invoke(result, "isDirectory", inputArg);
        Assert.assertTrue(((BBoolean) results[0]).booleanValue(), "Not identified as a directory.");
    }

    @Test(dependsOnMethods = "testIsDirectory")
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
        BValueArray list = (BValueArray) readContents[0];
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
    public void testWriteFile() throws URISyntaxException {
        String source = "/write.txt";
        final URL url = this.getClass().getClassLoader().getResource("datafiles/file1.txt");
        final String resourcePath = Paths.get(url.toURI()).toAbsolutePath().toString();
        BValue[] inputArg = { new BString(buildConnectionURL()), new BString(source), new BString(resourcePath) };
        BRunUtil.invoke(result, "write", inputArg);
        Assert.assertTrue(fileSystem.exists(rootFolder + "/write.txt"), "file not created.");
        final FileEntry entry = (FileEntry) fileSystem.getEntry(rootFolder + "/write.txt");
        InputStream inputStream = entry.createInputStream();
        String fileContent = new BufferedReader(new InputStreamReader(inputStream)).lines().
                collect(Collectors.joining("\n"));
        Assert.assertEquals(fileContent, content, "File content not identical.");
    }

    @Test(dependsOnMethods = "testWriteFile")
    public void testAppend() throws URISyntaxException {
        String source = "/write.txt";
        String appendContent = "New content";
        final URL url = this.getClass().getClassLoader().getResource("datafiles/file2.txt");
        final String resourcePath = Paths.get(url.toURI()).toAbsolutePath().toString();
        BValue[] inputArg = { new BString(buildConnectionURL()), new BString(source), new BString(resourcePath) };
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
