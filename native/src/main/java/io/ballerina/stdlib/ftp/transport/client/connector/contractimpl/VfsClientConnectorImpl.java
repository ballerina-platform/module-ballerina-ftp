/*
 * Copyright (c) 2017 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package io.ballerina.stdlib.ftp.transport.client.connector.contractimpl;

import io.ballerina.stdlib.ftp.exception.BallerinaFtpException;
import io.ballerina.stdlib.ftp.exception.RemoteFileSystemConnectorException;
import io.ballerina.stdlib.ftp.transport.client.connector.contract.FtpAction;
import io.ballerina.stdlib.ftp.transport.client.connector.contract.VfsClientConnector;
import io.ballerina.stdlib.ftp.transport.listener.RemoteFileSystemListener;
import io.ballerina.stdlib.ftp.transport.message.FileInfo;
import io.ballerina.stdlib.ftp.transport.message.RemoteFileSystemMessage;
import io.ballerina.stdlib.ftp.transport.server.util.FileTransportUtils;
import io.ballerina.stdlib.ftp.util.FtpConstants;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.VFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation for {@link VfsClientConnector} interface.
 */
public class VfsClientConnectorImpl implements VfsClientConnector {

    private static final Logger logger = LoggerFactory.getLogger(
            VfsClientConnectorImpl.class);

    private Map<String, String> connectorConfig;
    private RemoteFileSystemListener remoteFileSystemListener;
    private FileSystemOptions opts;
    private FileObject path;
    private FileSystemManager fsManager;

    public VfsClientConnectorImpl(Map<String, String> config)
            throws RemoteFileSystemConnectorException {
        this.connectorConfig = config;
        opts = FileTransportUtils.attachFileSystemOptions(config);
        String fileURI = null;
        try {
            fsManager = VFS.getManager();
            fileURI = connectorConfig.get(FtpConstants.URI);
            path = fsManager.resolveFile(fileURI, opts);
        } catch (FileSystemException e) {
            throw new RemoteFileSystemConnectorException("Error while connecting to the FTP server with URL: "
                    + (fileURI != null ? fileURI : ""));
        }
    }

    public void addListener(RemoteFileSystemListener listener) {
        this.remoteFileSystemListener = listener;
    }

    @Override
    public void send(RemoteFileSystemMessage message, FtpAction action, String filePath, String destination) {
        InputStream inputStream;
        OutputStream outputStream = null;
        ByteBuffer byteBuffer;
        FileObject fileObject = null;
        boolean pathClose = true;
        try {
            try {
                fileObject = path.resolveFile(filePath);
            } catch (FileSystemException e) {
                throw new BallerinaFtpException(e.getMessage());
            }
            switch (action) {
                case MKDIR:
                    if (fileObject.exists()) {
                        throw new RemoteFileSystemConnectorException("Directory exists: "
                                + fileObject.getName().getURI());
                    }
                    fileObject.createFolder();
                    break;
                case PUT:
                case APPEND:
                    if (!fileObject.exists()) {
                        fileObject.createFile();
                        fileObject.refresh();
                    }
                    if (FtpAction.APPEND.equals(action)) {
                        outputStream = fileObject.getContent().getOutputStream(true);
                    } else {
                        outputStream = fileObject.getContent().getOutputStream();
                    }
                    inputStream = message.getInputStream();
                    byteBuffer = message.getBytes();
                    if (byteBuffer != null) {
                        outputStream.write(byteBuffer.array());
                    } else if (inputStream != null) {
                        int n;
                        byte[] buffer = new byte[16384];
                        while ((n = inputStream.read(buffer)) > -1) {
                            outputStream.write(buffer, 0, n);
                        }
                    }
                    outputStream.flush();
                    break;
                case DELETE:
                    if (fileObject.exists()) {
                        int filesDeleted = fileObject.delete(Selectors.SELECT_SELF);
                        if (logger.isDebugEnabled()) {
                            logger.debug(filesDeleted + " files successfully deleted");
                        }
                    } else {
                        throw new RemoteFileSystemConnectorException(
                                "Failed to delete file: " + fileObject.getName().getURI() + " not found");
                    }
                    break;
                case RMDIR:
                    if (fileObject.exists()) {
                        int filesDeleted = fileObject.delete(Selectors.SELECT_ALL);
                        if (logger.isDebugEnabled()) {
                            logger.debug(filesDeleted + " files successfully deleted");
                        }
                    } else {
                        throw new RemoteFileSystemConnectorException(
                                "Failed to delete directory: " + fileObject.getName().getURI() + " not found");
                    }
                    break;
                case RENAME:
                    if (fileObject.exists()) {
                        try (FileObject newPath = fsManager.resolveFile(destination, opts);
                                FileObject parent = newPath.getParent()) {
                            if (parent != null) {
                                if (!parent.exists()) {
                                    parent.createFolder();
                                }
                                try (FileObject finalPath = parent.resolveFile(newPath.getName().getBaseName())) {
                                    if (!finalPath.exists()) {
                                        fileObject.moveTo(finalPath);
                                    } else {
                                        throw new RemoteFileSystemConnectorException(
                                                "The file at " + newPath.getURL().toString()
                                                        + " already exists or it is a directory");
                                    }
                                }
                            }
                        }
                    } else {
                        throw new RemoteFileSystemConnectorException(
                                "Failed to rename file: " + fileObject.getName().getURI() + " not found");
                    }
                    break;
                case GET:
                    if (fileObject.exists()) {
                        inputStream = fileObject.getContent().getInputStream();
                        FileObjectInputStream objectInputStream = new FileObjectInputStream(inputStream, fileObject);
                        RemoteFileSystemMessage fileContent = new RemoteFileSystemMessage(objectInputStream);
                        remoteFileSystemListener.onMessage(fileContent);
                        // We can't close the FileObject or InputStream at the end. This InputStream will pass to upper
                        // layer and stream need to close from there once the usage is done. Along with that need to
                        // close the FileObject. If we close the FileObject now then InputStream will not get any data.
                        pathClose = false;
                    } else {
                        throw new RemoteFileSystemConnectorException(
                                "Failed to read file: " + fileObject.getName().getURI() + " not found");
                    }
                    break;
                case SIZE:
                    remoteFileSystemListener.onMessage(new RemoteFileSystemMessage(fileObject.getContent().getSize()));
                    break;
                case LIST:
                    final FileObject[] fileObjects = fileObject.getChildren();
                    Map<String, FileInfo> childrenInfo = new HashMap<>();
                    for (FileObject tmpFileObject : fileObjects) {
                        FileInfo fileInfo = new FileInfo(tmpFileObject);
                        childrenInfo.put(fileInfo.getBaseName(), fileInfo);
                    }
                    RemoteFileSystemMessage children = new RemoteFileSystemMessage(childrenInfo);
                    remoteFileSystemListener.onMessage(children);
                    break;
                case ISDIR:
                    if (!fileObject.exists()) {
                        throw new BallerinaFtpException(filePath + " does not exists to check if it is a directory.");
                    }
                    remoteFileSystemListener.onMessage(new RemoteFileSystemMessage(fileObject.isFolder()));
                    break;
                default:
                    break;
            }
            remoteFileSystemListener.done();
        } catch (BallerinaFtpException | RemoteFileSystemConnectorException | IOException e) {
            remoteFileSystemListener.onError(e);
        } finally {
            if (fileObject != null && pathClose) {
                try {
                    fileObject.close();
                } catch (FileSystemException e) {
                    logger.error("Error while closing the remote file.");
                }
            }
            closeQuietly(outputStream);
        }
    }

    /**
     * Closes streams quietly.
     *
     * @param closeable The stream that should be closed
     */
    private static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            logger.error("Error while closing the stream.");
        }
    }

}
