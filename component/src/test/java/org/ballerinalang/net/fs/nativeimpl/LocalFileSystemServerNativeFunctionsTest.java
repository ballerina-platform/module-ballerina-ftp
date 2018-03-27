package org.ballerinalang.net.fs.nativeimpl;

import org.ballerinalang.launcher.util.BCompileUtil;
import org.ballerinalang.launcher.util.BRunUtil;
import org.ballerinalang.launcher.util.CompileResult;
import org.ballerinalang.model.values.BString;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.net.fs.server.Constants;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

/**
 * To verify native functions that related to LocalFileSystemServer connector.
 */
@Test(sequential = true)
public class LocalFileSystemServerNativeFunctionsTest {

    private CompileResult result, resultNegative;
    private final String protocolPackageHttp = Constants.FILE_SYSTEM_PACKAGE_NAME;
    private File rootDirectory;

    @BeforeClass
    public void init() {
        try {
            Path rootListenFolderPath = Files.createTempDirectory(Paths.get("target"), null);
            rootDirectory = rootListenFolderPath.toFile();
            rootDirectory.deleteOnExit();
            String filePath = "test-src/fs/file-system-native-function.bal";
            result = BCompileUtil.compile(filePath);
            String filePathNeg = "test-src/fs/file-system-native-function-negative.bal";
            resultNegative = BCompileUtil.compile(filePathNeg);
        } catch (IOException e) {
            Assert.fail("Unable to create root folder", e);
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
    public void testFileMoveFunction() throws IOException {
        Path tempFile1 = createFile(rootDirectory.getAbsolutePath(), "temp1.txt");
        Path newDirectory = createDirectory("NewDirectory");
        BStruct event = BCompileUtil.createAndGetStruct(result.getProgFile(), protocolPackageHttp,
                Constants.FILE_SYSTEM_EVENT);
        event.setStringField(0, tempFile1.toFile().getAbsolutePath());
        BValue[] inputArg = {event, new BString(newDirectory.toFile().getAbsolutePath())};
        BRunUtil.invoke(result, "testFileMove", inputArg);
        Assert.assertFalse(Files.exists(tempFile1), "File didn't move");
        Path newLocation = Paths.get(newDirectory.toFile().getAbsolutePath(), "temp1.txt");
        Assert.assertTrue(Files.exists(newLocation), "File not in new location");
    }

    @Test(dependsOnMethods = "testFileMoveFunction")
    public void testFileDeleteFunction() throws IOException {
        Path tempFile2 = createFile(rootDirectory.getAbsolutePath(), "temp2.txt");
        BStruct event = BCompileUtil.createAndGetStruct(result.getProgFile(), protocolPackageHttp,
                Constants.FILE_SYSTEM_EVENT);
        event.setStringField(0, tempFile2.toFile().getAbsolutePath());
        BValue[] inputArg = {event};
        BRunUtil.invoke(result, "testFileDelete", inputArg);
        Assert.assertFalse(Files.exists(tempFile2), "File didn't delete");
    }

    @Test()
    public void testFileMoveEmptyDestinationFunction() throws IOException {
        BStruct event = BCompileUtil.createAndGetStruct(result.getProgFile(), protocolPackageHttp,
                Constants.FILE_SYSTEM_EVENT);
        event.setStringField(0, "/newfile");
        BValue[] inputArg = {event, new BString("")};
        String error = null;
        final BValue[] values = BRunUtil.invoke(resultNegative, "testFileMoveInvalidPath", inputArg);
        Assert.assertEquals(values.length, 1);
        final BStruct value = (BStruct) values[0];
        Assert.assertEquals(value.getType().getName(), "FSError", "Not getting expected error");
        Assert.assertEquals(value.getStringField(0),
                "Please provide a local file system destination to move the file." ,
                "Not expected error message.");
    }

    @Test()
    public void testFileMoveNullDestinationFunction() throws IOException {
        BStruct event = BCompileUtil.createAndGetStruct(result.getProgFile(), protocolPackageHttp,
                Constants.FILE_SYSTEM_EVENT);
        event.setStringField(0, "/newfile");
        BValue[] inputArg = {event, new BString(null)};
        final BValue[] values = BRunUtil.invoke(resultNegative, "testFileMoveInvalidPath", inputArg);
        Assert.assertEquals(values.length, 1);
        final BStruct value = (BStruct) values[0];
        Assert.assertEquals(value.getType().getName(), "FSError", "Not getting expected error");
        Assert.assertEquals(value.getStringField(0),
                "Please provide a local file system destination to move the file." ,
                "Not expected error message.");
    }

    @Test(dependsOnMethods = "testFileMoveFunction")
    public void testFileMoveInvalidDestinationFunction() throws IOException {
        Path tempFile1 = createFile(rootDirectory.getAbsolutePath(), "temp1.txt");
        Path newDirectory = createDirectory("NewDirectory1");
        Path tempFile2 = createFile(newDirectory.toFile().toString(), "temp2.txt");
        BStruct event = BCompileUtil.createAndGetStruct(result.getProgFile(), protocolPackageHttp,
                Constants.FILE_SYSTEM_EVENT);
        event.setStringField(0, tempFile1.toFile().getAbsolutePath());
        BValue[] inputArg = {event, new BString(tempFile2.toFile().getAbsolutePath())};
        final BValue[] values = BRunUtil.invoke(resultNegative, "testFileMoveInvalidPath", inputArg);
        Assert.assertEquals(values.length, 1);
        Assert.assertTrue(Files.exists(tempFile1), "File did move");
        final BStruct value = (BStruct) values[0];
        Assert.assertEquals(value.getType().getName(), "FSError", "Not getting expected error");
        Assert.assertEquals(value.getStringField(0), "Destination is not a directory: " + tempFile2.toString(),
                "Not expected error message.");
    }

    @Test(dependsOnMethods = "testFileMoveFunction")
    public void testFileMoveInvalidSourceFunction() throws IOException {
        Path newDirectory = createDirectory("NewDirectory1");
        BStruct event = BCompileUtil.createAndGetStruct(result.getProgFile(), protocolPackageHttp,
                Constants.FILE_SYSTEM_EVENT);
        event.setStringField(0, "/invalid-file");
        BValue[] inputArg = {event, new BString(newDirectory.toFile().getAbsolutePath())};
        final BValue[] values = BRunUtil.invoke(resultNegative, "testFileMoveInvalidPath", inputArg);
        Assert.assertEquals(values.length, 1);
        final BStruct value = (BStruct) values[0];
        Assert.assertEquals(value.getType().getName(), "FSError", "Not getting expected error");
        Assert.assertEquals(value.getStringField(0), "No such a file: invalid-file",
                "Not expected error message.");
    }

    private Path createFile(String directory, String fileName) throws IOException {
        Path file = Paths.get(directory, fileName);
        Files.createFile(file);
        return file;
    }

    private Path createDirectory(String directoryName) throws IOException {
        Path directory = Paths.get(rootDirectory.getAbsolutePath(), directoryName);
        if (Files.notExists(directory)) {
            Files.createDirectory(directory);
        }
        return directory;
    }
}
