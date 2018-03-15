package org.ballerinalang.net.fs.navtiveimpl;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.CallableUnitCallback;
import org.ballerinalang.model.NativeCallableUnit;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.natives.annotations.ReturnType;
import org.ballerinalang.net.fs.FSUtil;
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
        packageName = "ballerina.net.fs", functionName = "delete",
        receiver = @Receiver(type = TypeKind.STRUCT, structType = "FileSystemEvent",
                                        structPackage = "ballerina.net.fs"),
        returnType = {
                @ReturnType(type = TypeKind.STRUCT, structType = "FSError",
                                       structPackage = "ballerina.net.fs")
        },
        isPublic = true)
public class Delete implements NativeCallableUnit {
    private static final Logger log = LoggerFactory.getLogger(Delete.class);

    @Override
    public void execute(Context context, CallableUnitCallback callableUnitCallback) {
        BStruct fileEventStruct = (BStruct) context.getRefArgument(0);
        String source = fileEventStruct.getStringField(0);
        BStruct fsError = null;
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
            fsError = FSUtil.getFSError(context, e);
        }
        context.setReturnValues(fsError);
        callableUnitCallback.notifySuccess();
    }

    @Override
    public boolean isBlocking() {
        return false;
    }


}
