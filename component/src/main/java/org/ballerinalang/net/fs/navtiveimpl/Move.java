package org.ballerinalang.net.fs.navtiveimpl;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.CallableUnitCallback;
import org.ballerinalang.model.NativeCallableUnit;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.natives.annotations.ReturnType;
import org.ballerinalang.net.fs.FSUtil;
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
        args = {
                @Argument(name = "destination", type = TypeKind.STRING)},
        returnType = {
                @ReturnType(type = TypeKind.STRUCT, structType = "FSError", structPackage = "ballerina.net.fs")
        },
        isPublic = true
)
public class Move implements NativeCallableUnit {
    private static final Logger log = LoggerFactory.getLogger(Move.class);

    @Override
    public void execute(Context context, CallableUnitCallback callableUnitCallback) {
        BStruct fileEventStruct = (BStruct) context.getRefArgument(0);
        String destination = context.getNullableStringArgument(0);
        BStruct fsError = null;
        if (destination == null || destination.isEmpty()) {
            throw new BallerinaException("Please provide a local file system destination to move the file.");
        } else if (!Files.isDirectory(Paths.get(destination))) {
            throw new BallerinaException("Destination is not a directory: " + destination);
        }
        String source = fileEventStruct.getStringField(0);
        Path sourcePath = Paths.get(source);
        Path fileName = sourcePath.getFileName();
        if (fileName == null) {
            fsError = FSUtil.getFSError(context,
                    new BallerinaException("Could not find the file name for triggered event: " + source));

        } else {
            Path destinationPath = Paths.get(destination, fileName.toString());
            try {
                Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                if (log.isDebugEnabled()) {
                    log.debug("File moved successfully to " + destination + " from " + source);
                }
            } catch (IOException e) {
                fsError = FSUtil.getFSError(context, e);
            }
        }
        context.setReturnValues(fsError);
        callableUnitCallback.notifySuccess();
    }

    @Override
    public boolean isBlocking() {
        return false;
    }
}
