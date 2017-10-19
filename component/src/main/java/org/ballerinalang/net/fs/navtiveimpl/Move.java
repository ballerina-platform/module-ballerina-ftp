package org.ballerinalang.net.fs.navtiveimpl;

import org.ballerinalang.bre.Context;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.AbstractNativeFunction;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;


/**
 * Move function implementation.
 * Move triggered file to a given location.
 */
@BallerinaFunction(
        packageName = "ballerina.net.fs",
        functionName = "move",
        receiver = @Receiver(type = TypeKind.STRUCT, structType = "FileSystemEvent",
                structPackage = "ballerina.net.fs"),
        args = {@Argument(name = "destination", type = TypeKind.STRING)},
        isPublic = true
)
public class Move extends AbstractNativeFunction {
    private static final Logger log = LoggerFactory.getLogger(Move.class);

    @Override
    public BValue[] execute(Context context) {
        BStruct fileEventStruct = (BStruct) getRefArgument(context, 0);
        String destination = getStringArgument(context, 0);
        if (destination == null || destination.isEmpty()) {
            throw new BallerinaException("Please provide a local file system destination to move the file.");
        } else if (!Files.isDirectory(Paths.get(destination))) {
            throw new BallerinaException("Destination is not a directory: " + destination);
        }
        String source = fileEventStruct.getStringField(0);
        Path sourcePath = Paths.get(source);
        Path fileName = sourcePath.getFileName();
        Path destinationPath = Paths.get(destination, fileName.toString());
        try {
            Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            if (log.isDebugEnabled()) {
                log.debug("File moved successfully to " + destination + " from " + source);
            }
        } catch (IOException e) {
            throw new BallerinaException("Unable to move file [" + source + "] to destination[" + destination + "]", e);
        }
        return AbstractNativeFunction.VOID_RETURN;
    }
}
