package org.ballerinalang.ftp.util;

/**
 * Exception class for FTP
 */
public class BallerinaFTPException extends Exception {

    public BallerinaFTPException(String message) {
        super(message);
    }

    public BallerinaFTPException(String message, Throwable cause) {
        super(message, cause);
    }
}
