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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    // Advanced file selection fields
    private double minAge = -1; // in seconds, -1 means disabled
    private double maxAge = -1; // in seconds, -1 means disabled
    private String ageCalculationMode = FtpConstants.AGE_CALCULATION_MODE_LAST_MODIFIED;
    private List<FileDependencyCondition> dependencyConditions = new ArrayList<>();

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

        // Parse file age filter configuration
        if (fileProperties.containsKey(FtpConstants.FILE_AGE_FILTER_MIN_AGE)) {
            String minAgeStr = fileProperties.get(FtpConstants.FILE_AGE_FILTER_MIN_AGE);
            if (minAgeStr != null && !minAgeStr.isEmpty()) {
                this.minAge = Double.parseDouble(minAgeStr);
            }
        }
        if (fileProperties.containsKey(FtpConstants.FILE_AGE_FILTER_MAX_AGE)) {
            String maxAgeStr = fileProperties.get(FtpConstants.FILE_AGE_FILTER_MAX_AGE);
            if (maxAgeStr != null && !maxAgeStr.isEmpty()) {
                this.maxAge = Double.parseDouble(maxAgeStr);
            }
        }
        if (fileProperties.containsKey(FtpConstants.FILE_AGE_FILTER_AGE_CALCULATION_MODE)) {
            String mode = fileProperties.get(FtpConstants.FILE_AGE_FILTER_AGE_CALCULATION_MODE);
            if (mode != null && !mode.isEmpty()) {
                this.ageCalculationMode = mode;
            }
        }
    }

    public RemoteFileSystemConsumer(Map<String, String> fileProperties,
                                    List<FileDependencyCondition> conditions,
                                    RemoteFileSystemListener listener)
            throws RemoteFileSystemConnectorException {
        this(fileProperties, listener);
        this.dependencyConditions = conditions;
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

        // Step 1: Check file age filter
        if (!passesAgeFilter(file)) {
            logDebugFileFilteredByAge(file);
            return;
        }

        // Step 2: Check file dependencies
        if (!passesDependencyCheck(file)) {
            logDebugFileFilteredByDependency(file);
            return;
        }

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

    /**
     * Check if a file passes the age filter.
     *
     * @param file The file to check
     * @return true if the file passes the age filter, false otherwise
     */
    private boolean passesAgeFilter(FileObject file) throws FileSystemException {
        // If both minAge and maxAge are disabled, pass all files
        if (minAge < 0 && maxAge < 0) {
            return true;
        }

        long fileTime;
        try {
            if (FtpConstants.AGE_CALCULATION_MODE_CREATION_TIME.equals(ageCalculationMode)) {
                // Get creation time (not all file systems support this)
                if (file.getContent().hasAttribute("creationTime")) {
                    Object creationTimeAttr = file.getContent().getAttribute("creationTime");
                    if (creationTimeAttr instanceof Long) {
                        fileTime = (Long) creationTimeAttr;
                    } else {
                        // Fall back to last modified time
                        fileTime = file.getContent().getLastModifiedTime();
                    }
                } else {
                    // Fall back to last modified time
                    fileTime = file.getContent().getLastModifiedTime();
                }
            } else {
                // Use last modified time (default)
                fileTime = file.getContent().getLastModifiedTime();
            }
        } catch (Exception e) {
            log.warn("Failed to get file time for age filtering, using last modified time", e);
            fileTime = file.getContent().getLastModifiedTime();
        }

        long currentTime = System.currentTimeMillis();
        double ageInSeconds = (currentTime - fileTime) / 1000.0;

        // Check minimum age
        if (minAge >= 0 && ageInSeconds < minAge) {
            return false;
        }

        // Check maximum age
        if (maxAge >= 0 && ageInSeconds > maxAge) {
            return false;
        }
        return true;
    }

    /**
     * Check if a file passes all dependency conditions.
     *
     * @param file The file to check
     * @return true if the file passes all dependency checks, false otherwise
     */
    private boolean passesDependencyCheck(FileObject file) throws FileSystemException {
        if (dependencyConditions.isEmpty()) {
            return true;
        }

        String fileName = file.getName().getBaseName();

        // Check each dependency condition
        for (FileDependencyCondition condition : dependencyConditions) {
            // Check if this file matches the target pattern
            if (!fileName.matches(condition.getTargetPattern())) {
                continue;
            }

            // This file matches the target pattern, check if required files exist
            Pattern targetPatternCompiled = Pattern.compile(condition.getTargetPattern());
            Matcher matcher = targetPatternCompiled.matcher(fileName);

            if (!matcher.matches()) {
                continue;
            }

            // Extract capture groups for substitution
            Map<String, String> captureGroups = new HashMap<>();
            for (int i = 0; i <= matcher.groupCount(); i++) {
                captureGroups.put("$" + i, matcher.group(i));
            }

            // Check required files based on matching mode
            if (!checkRequiredFiles(condition, captureGroups)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if required files exist based on the matching mode.
     *
     * @param condition The dependency condition
     * @param captureGroups Map of capture groups from the target pattern match
     * @return true if required files exist according to matching mode
     */
    private boolean checkRequiredFiles(FileDependencyCondition condition,
                                        Map<String, String> captureGroups) throws FileSystemException {
        List<String> requiredPatterns = condition.getRequiredFiles();
        String matchingMode = condition.getMatchingMode();

        // Substitute capture groups in required file patterns
        List<String> resolvedPatterns = new ArrayList<>();
        for (String pattern : requiredPatterns) {
            String resolved = pattern;
            for (Map.Entry<String, String> entry : captureGroups.entrySet()) {
                resolved = resolved.replace(entry.getKey(), entry.getValue());
            }
            resolvedPatterns.add(resolved);
        }

        // Get all files in the listening directory (shallow, non-recursive)
        FileObject[] siblings = listeningDir.getChildren();
        List<String> siblingNames = new ArrayList<>();
        for (FileObject sibling : siblings) {
            if (sibling.getType() == FileType.FILE) {
                siblingNames.add(sibling.getName().getBaseName());
            }
        }

        // Count matches based on matching mode
        if (FtpConstants.DEPENDENCY_MATCHING_MODE_ALL.equals(matchingMode)) {
            // All patterns must have at least one match
            for (String pattern : resolvedPatterns) {
                boolean found = false;
                for (String siblingName : siblingNames) {
                    if (siblingName.matches(pattern)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            }
            return true;

        } else if (FtpConstants.DEPENDENCY_MATCHING_MODE_ANY.equals(matchingMode)) {
            // At least one pattern must have a match
            for (String pattern : resolvedPatterns) {
                for (String siblingName : siblingNames) {
                    if (siblingName.matches(pattern)) {
                        return true;
                    }
                }
            }
            return false;

        } else {
            // Exact count of files must match (across all patterns)
            int matchCount = 0;
            for (String pattern : resolvedPatterns) {
                for (String siblingName : siblingNames) {
                    if (siblingName.matches(pattern)) {
                        matchCount++;
                    }
                }
            }
            return matchCount == condition.getRequiredFileCount();
        }
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

    @ExcludeCoverageFromGeneratedReport
    private void logDebugFileFilteredByAge(FileObject file) {
        if (log.isDebugEnabled()) {
            try {
                log.debug("File " + file.getName().getFriendlyURI()
                        + " is filtered out by age filter (minAge: " + minAge + ", maxAge: " + maxAge + ")");
            } catch (Exception e) {
                log.debug("File is filtered out by age filter", e);
            }
        }
    }

    @ExcludeCoverageFromGeneratedReport
    private void logDebugFileFilteredByDependency(FileObject file) {
        if (log.isDebugEnabled()) {
            try {
                log.debug("File " + file.getName().getFriendlyURI()
                        + " is filtered out due to missing required dependency files");
            } catch (Exception e) {
                log.debug("File is filtered out by dependency check", e);
            }
        }
    }
}
