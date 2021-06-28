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

package org.ballerinalang.stdlib.ftp.transport.message;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;

import java.net.URL;

/**
 * This represent meta details of the remote file.
 */
public class FileInfo {

    /**
     * Relative file path for newly added file.
     */
    private final String path;

    /**
     * Size of the file.
     */
    private long fileSize;

    /**
     * Last modified timestamp of the file in UNIX Epoch time.
     */
    private long lastModifiedTime;

    /**
     * The name of this file.
     */
    private FileName fileName;

    /**
     * The receiver as a URI String for public display.
     */
    private String publicURIString;

    /**
     * File's type.
     */
    private FileType fileType;

    /**
     * Whether the fileObject is attached.
     */
    private boolean isAttached;

    /**
     * The URL representation of this file.
     */
    private URL url;

    /**
     * Whether someone reads/writes to this file.
     */
    private boolean isContentOpen;

    /**
     * Whether this file is executable.
     */
    private boolean isExecutable;

    /**
     * Whether the file is a file or not.
     */
    private boolean isFile;

    /**
     * Whether the file is a folder or not.
     */
    private boolean isFolder;

    /**
     * Whether this file is hidden.
     */
    private boolean isHidden;

    /**
     * Whether this file can be read.
     */
    private boolean isReadable;

    /**
     * Whether this file can be written to.
     */
    private boolean isWritable;

    public FileInfo(String path) {
        this.path = path;
    }

    public FileInfo(FileObject fileObject) throws FileSystemException {
        this.fileName = fileObject.getName();
        this.path = fileName.getPath();
        this.publicURIString = fileObject.getPublicURIString();
        this.isFolder = fileObject.isFolder();
        this.isFile = fileObject.isFile();
        this.fileType = fileObject.getType();
        this.isAttached = fileObject.isAttached();
        this.url = fileObject.getURL();
        this.isContentOpen = fileObject.isContentOpen();
        this.isExecutable = fileObject.isExecutable();
        this.isHidden = fileObject.isHidden();
        this.isReadable = fileObject.isReadable();
        this.isWritable = fileObject.isWriteable();
    }

    /**
     * This will return relative path from the root folder.
     *
     * @return The relative path from root folder.
     */
    public String getPath() {
        return path;
    }

    /**
     * Determines the size of the file, in bytes.
     *
     * @return File size in bytes.
     */
    public long getFileSize() {
        return fileSize;
    }

    /**
     * Determines the last-modified timestamp of the file.
     *
     * @return Last-modified timestamp.
     */
    public long getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public void setLastModifiedTime(long lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    /**
     * Returns the base name of this file. The base name is the last element of the file name. F
     *
     * @return The base name. Never returns null.
     */
    public String getBaseName() {
        return this.fileName.getBaseName();
    }

    /**
     * Returns the name of this file.
     *
     * @return the FileName.
     */
    public FileName getFileName() {
        return fileName;
    }

    /**
     * Returns the receiver as a URI String for public display, like, without a password.
     *
     * @return A URI String without a password.
     */
    public String getPublicURIString() {
        return publicURIString;
    }

    /**
     * Returns this file's type.
     *
     * @return One of the {@link FileType} constants. Never returns null.
     */
    public FileType getFileType() {
        return fileType;
    }

    /**
     * Checks if the fileObject is attached.
     *
     * @return true if the FileObject is attached.
     */
    public boolean isAttached() {
        return isAttached;
    }

    /**
     * Returns a URL representing this file.
     *
     * @return the URL for the file.
     */
    public URL getUrl() {
        return url;
    }

    /**
     * Checks if someone reads/write to this file.
     *
     * @return true if the file content is open.
     */
    public boolean isContentOpen() {
        return isContentOpen;
    }

    /**
     * Determines if this file is executable.
     *
     * @return {@code true} if this file is executable, {@code false} if not.
     */
    public boolean isExecutable() {
        return isExecutable;
    }

    /**
     * Checks if this file is a regular file.
     *
     * @return true if this file is a regular file.
     */
    public boolean isFile() {
        return isFile;
    }

    /**
     * Checks if this file is a folder.
     *
     * @return true if this file is a folder.
     */
    public boolean isFolder() {
        return isFolder;
    }

    /**
     * Determines if this file is hidden.
     *
     * @return {@code true} if this file is hidden, {@code false} if not.
     */
    public boolean isHidden() {
        return isHidden;
    }

    /**
     * Determines if this file can be read.
     *
     * @return {@code true} if this file is readable, {@code false} if not.
     */
    public boolean isReadable() {
        return isReadable;
    }

    /**
     * Determines if this file can be written to.
     *
     * @return {@code true} if this file is writeable, {@code false} if not.
     */
    public boolean isWritable() {
        return isWritable;
    }
}
