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

package io.ballerina.stdlib.ftp.transport.server;

import io.ballerina.stdlib.ftp.exception.RemoteFileSystemConnectorException;
import io.ballerina.stdlib.ftp.server.FtpListener;
import io.ballerina.stdlib.ftp.transport.listener.RemoteFileSystemListener;
import io.ballerina.stdlib.ftp.transport.message.FileInfo;
import io.ballerina.stdlib.ftp.transport.message.RemoteFileSystemEvent;
import io.ballerina.stdlib.ftp.transport.server.util.FileTransportUtils;
import io.ballerina.stdlib.ftp.util.ExcludeCoverageFromGeneratedReport;
import io.ballerina.stdlib.ftp.util.FtpConstants;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.VFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Provides the capability to process a file and move/delete it afterwards.
 */
public class RemoteFileSystemConsumer {

    private static final Logger log = LoggerFactory.getLogger(
            io.ballerina.stdlib.ftp.transport.server.RemoteFileSystemConsumer.class);

    private RemoteFileSystemListener remoteFileSystemListener;
    private String listeningDirURI;
    private FileObject listeningDir;
    private String fileNamePattern = null;
    private FileSystemManager fileSystemManager;
    private FileSystemOptions fileSystemOptions;

    private List<String> processed = new ArrayList<>();
    private List<String> current;
    private List<FileInfo> addedFileInfo;

    /**
     * Constructor for the RemoteFileSystemConsumer.
     *
     * @param fileProperties Map of property values
     * @param listener       RemoteFileSystemListener instance to send callback
     * @throws RemoteFileSystemConnectorException if unable to start the connect to the remote server
     */
    public RemoteFileSystemConsumer(Map<String, String> fileProperties, RemoteFileSystemListener listener)
            throws RemoteFileSystemConnectorException {
        this.remoteFileSystemListener = listener;
        listeningDirURI = fileProperties.get(FtpConstants.URI);
        try {
            this.fileSystemManager = VFS.getManager();
            this.fileSystemOptions = FileTransportUtils.attachFileSystemOptions(fileProperties);
            listeningDir = fileSystemManager.resolveFile(listeningDirURI, fileSystemOptions);
            FileType fileType = listeningDir.getType();
            if (fileType != FileType.FOLDER) {
                String errorMsg = "File system server connector is used to "
                        + "listen to a folder. But the given path does not refer to a folder.";
                final RemoteFileSystemConnectorException e = new RemoteFileSystemConnectorException(errorMsg);
                remoteFileSystemListener.onError(e);
                throw e;
            }
        } catch (FileSystemException e) {
            remoteFileSystemListener.onError(e);
            String rootCauseMessage = (e.getCause() != null && e.getCause().getMessage() != null) 
                    ? e.getCause().getMessage() : e.getMessage();
            throw new RemoteFileSystemConnectorException(
                    "Unable to initialize the connection with the server. " + rootCauseMessage, e);
        }
        if (fileProperties.get(FtpConstants.FILE_NAME_PATTERN) != null) {
            fileNamePattern = fileProperties.get(FtpConstants.FILE_NAME_PATTERN);
        }
    }

    /**
     * Do the file processing operation for the given set of properties. Do the
     * checks and pass the control to file system processor thread/threads.
     *
     * @throws RemoteFileSystemConnectorException for all the error situation.
     */
    public void consume() throws RemoteFileSystemConnectorException {
        logDebugConsumeStarted();
        try {
            boolean isFileExists; // Initially assume that the file doesn't exist
            boolean isFileReadable; // Initially assume that the file is not readable
            listeningDir.refresh();
            isFileExists = listeningDir.exists();
            isFileReadable = listeningDir.isReadable();
            if (isFileExists && isFileReadable) {
                current = new ArrayList<>();
                addedFileInfo = new ArrayList<>();
                FileObject[] children = null;
                try {
                    children = listeningDir.getChildren();
                } catch (FileSystemException ignored) {
                    logDebugErrorWhileGetChildrenFromDirListener(ignored);
                }
                if (children == null || children.length == 0) {
                    logDebugNoChildrenFromDirWhileConsuming();
                } else {
                    handleDirectory(children);
                    List<String> deleted = new ArrayList<>();
                    if (processed.size() != current.size()) {
                        final Iterator<String> it = processed.iterator();
                        while (it.hasNext()) {
                            String fileName = it.next();
                            if (!current.contains(fileName)) {
                                // File got delete between previous and this scan.
                                deleted.add(fileName);
                                // Remove from processed list.
                                it.remove();
                            }
                        }
                    }
                    try {
                        if (addedFileInfo.size() > 0 || deleted.size() > 0) {
                            RemoteFileSystemEvent message = new RemoteFileSystemEvent(addedFileInfo, deleted);
                            remoteFileSystemListener.onMessage(message);
                        }
                    } catch (Exception e) {
                        remoteFileSystemListener.onError(e);
                    }

                }
            } else {
                String errorMsg = String.format("Unable to access or read file or directory :  %s. Reason: %s",
                                FileTransportUtils.maskUrlPassword(listeningDirURI),
                                (isFileExists ? "The file can not be read!" : "The file does not exist!"));
                remoteFileSystemListener.onError(new RemoteFileSystemConnectorException(errorMsg));
            }
        } catch (FileSystemException e) {
            remoteFileSystemListener.onError(e);
            throw new RemoteFileSystemConnectorException(
                    "Unable to get details from remote server.", e);
        } finally {
            closeDirectories();
        }
        logDebugConsumeStopped();
    }

    /**
     * Closes the connections and stops the listener.
     *
     * @throws RemoteFileSystemConnectorException for all the error situation.
     */
    public Object close() {
        closeDirectories();
        return remoteFileSystemListener.done();
    }

    /**
     * Get the FTP Listener of the FTP consumer.
     *
     */
    public FtpListener getFtpListener() {
        return (FtpListener) remoteFileSystemListener;
    }

    /**
     * Get the FileSystemManager instance.
     *
     * @return The FileSystemManager
     */
    public FileSystemManager getFileSystemManager() {
        return fileSystemManager;
    }

    /**
     * Get the FileSystemOptions instance.
     *
     * @return The FileSystemOptions
     */
    public FileSystemOptions getFileSystemOptions() {
        return fileSystemOptions;
    }

    private void closeDirectories() {
        try {
            if (listeningDir != null) {
                listeningDir.close();
            }
        } catch (FileSystemException e) {
            log.warn("Could not close file at URI: " + FileTransportUtils
                    .maskUrlPassword(listeningDirURI), e);
        }
    }

    /**
     * Handle directory with child elements.
     *
     * @param children The array containing child elements of a folder
     */
    private void handleDirectory(FileObject[] children) throws FileSystemException {
        for (FileObject child : children) {
            if (!(fileNamePattern == null || child.getName().getBaseName().matches(fileNamePattern))) {
                logDebugDirFileNamePatternNotMatchedInDirHandler();
            } else {
                FileType childType = child.getType();
                if (childType == FileType.FOLDER) {
                    FileObject[] childFileObject = null;
                    try {
                        childFileObject = child.getChildren();
                    } catch (FileSystemException ignored) {
                        logDebugErrorWhileGetChildrenInDirHandler(ignored);
                    }
                    if (childFileObject == null || childFileObject.length == 0) {
                        logDebugNoChildrenFromDirInDirHandler(child);
                    } else {
                        handleDirectory(childFileObject);
                    }
                } else {
                    current.add(child.getName().getURI());
                    handleFile(child);
                }
            }
        }
    }

    /**
     * Process a single file.
     *
     * @param file A single file to be processed
     */
    private void handleFile(FileObject file) throws FileSystemException {
        String path = file.getName().getURI();
        FileInfo info = new FileInfo(path);
        info.setFileSize(file.getContent().getSize());
        info.setLastModifiedTime(file.getContent().getLastModifiedTime());
        info.setFileName(file.getName());
        info.setFolder(file.isFolder());
        info.setFile(file.isFile());
        info.setPublicURIString(file.getPublicURIString());
        info.setFileType(file.getType());
        info.setAttached(file.isAttached());
        info.setContentOpen(file.isContentOpen());
        info.setExecutable(file.isExecutable());
        info.setHidden(file.isHidden());
        info.setReadable(file.isReadable());
        info.setWritable(file.isWriteable());
        info.setUrl(file.getURL());
        addedFileInfo.add(info);
        processed.add(path);
    }

    @ExcludeCoverageFromGeneratedReport
    private void logDebugErrorWhileGetChildrenFromDirListener(FileSystemException ignored) {
        if (log.isDebugEnabled()) {
            log.debug("The file does not exist, or is not a folder, or an error "
                    + "has occurred when trying to list the children. File URI : " + FileTransportUtils
                    .maskUrlPassword(listeningDirURI), ignored);
        }
    }

    @ExcludeCoverageFromGeneratedReport
    private void logDebugNoChildrenFromDirWhileConsuming() {
        if (log.isDebugEnabled()) {
            log.debug(
                    "Folder at " + FileTransportUtils.maskUrlPassword(listeningDirURI)
                            + " is empty.");
        }
    }

    @ExcludeCoverageFromGeneratedReport
    private void logDebugDirFileNamePatternNotMatchedInDirHandler() {
        if (log.isDebugEnabled()) {
            log.debug("File " + listeningDir.getName().getFriendlyURI()
                    + " is not processed because it did not match the specified pattern.");
        }
    }

    @ExcludeCoverageFromGeneratedReport
    private void logDebugNoChildrenFromDirInDirHandler(FileObject child) {
        if (log.isDebugEnabled()) {
            log.debug("Folder at " + child.getName().getFriendlyURI() + " is empty.");
        }
    }

    @ExcludeCoverageFromGeneratedReport
    private void logDebugErrorWhileGetChildrenInDirHandler(FileSystemException ignored) {
        if (log.isDebugEnabled()) {
            log.debug("The file does not exist, or is not a folder, or an error "
                    + "has occurred when trying to list the children. File URI : " + FileTransportUtils
                    .maskUrlPassword(listeningDirURI), ignored);
        }
    }

    @ExcludeCoverageFromGeneratedReport
    private void logDebugConsumeStarted() {
        if (log.isDebugEnabled()) {
            log.debug("Thread name: " + Thread.currentThread().getName());
            log.debug("File System Consumer hashcode: " + this.hashCode());
            log.debug("Polling for directory or file: " + FileTransportUtils.maskUrlPassword(listeningDirURI));
        }
    }

    @ExcludeCoverageFromGeneratedReport
    private void logDebugConsumeStopped() {
        if (log.isDebugEnabled()) {
            log.debug("End : Scanning directory or file : " + FileTransportUtils
                    .maskUrlPassword(listeningDirURI));
        }
    }
}
