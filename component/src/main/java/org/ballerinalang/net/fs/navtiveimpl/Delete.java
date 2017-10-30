package org.ballerinalang.net.fs.navtiveimpl;

import org.ballerinalang.bre.Context;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.AbstractNativeFunction;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Delete function implementation.
 * Delete triggered file from the file system.
 */
@BallerinaFunction(
        packageName = "ballerina.net.fs",
        functionName = "delete",
        receiver = @Receiver(type = TypeKind.STRUCT, structType = "FileSystemEvent",
                structPackage = "ballerina.net.fs"),
        isPublic = true
)
public class Delete extends AbstractNativeFunction {
    private static final Logger log = LoggerFactory.getLogger(Delete.class);

    @Override
    public BValue[] execute(Context context) {
        BStruct fileEventStruct = (BStruct) getRefArgument(context, 0);
        String source = fileEventStruct.getStringField(0);
        Path sourcePath = Paths.get(source);
        try {
            final boolean result = Files.deleteIfExists(sourcePath);
            if (log.isDebugEnabled()) {
                if (result) {
                    log.debug("File deleted successfully: " + source);
                } else {
                    log.debug("File delete operation failed.");
                }
            }
        } catch (IOException e) {
            throw new BallerinaException("Unable to delete file [" + source + "]", e);
        }
        return AbstractNativeFunction.VOID_RETURN;
    }
}
