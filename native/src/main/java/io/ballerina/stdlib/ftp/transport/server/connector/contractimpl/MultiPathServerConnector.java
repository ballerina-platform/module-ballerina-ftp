/*
 * Copyright (c) 2026 WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.ftp.transport.server.connector.contractimpl;

import io.ballerina.stdlib.ftp.exception.RemoteFileSystemConnectorException;
import io.ballerina.stdlib.ftp.server.FtpListener;
import io.ballerina.stdlib.ftp.transport.server.FileDependencyCondition;
import io.ballerina.stdlib.ftp.transport.server.RemoteFileSystemConsumer;
import io.ballerina.stdlib.ftp.transport.server.connector.contract.RemoteFileSystemServerConnector;
import io.ballerina.stdlib.ftp.util.FtpConstants;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Server connector that supports monitoring multiple paths.
 * Used when services have @ftp:ServiceConfig annotations with different paths.
 */
public class MultiPathServerConnector implements RemoteFileSystemServerConnector {

    private static final Logger log = LoggerFactory.getLogger(MultiPathServerConnector.class);

    private final FtpListener ftpListener;
    private final Map<String, RemoteFileSystemConsumer> pathConsumers = new ConcurrentHashMap<>();
    private final Map<String, Object> baseProperties;
    private AtomicBoolean isPollOperationOccupied = new AtomicBoolean(false);
    private FileSystemManager fileSystemManager;
    private FileSystemOptions fileSystemOptions;

    public MultiPathServerConnector(Map<String, Object> baseProperties, FtpListener ftpListener) {
        this.baseProperties = new HashMap<>(baseProperties);
        this.ftpListener = ftpListener;
    }

    /**
     * Adds a consumer for a specific path.
     *
     * @param path The directory path to monitor
     * @param fileNamePattern The file name pattern (regex) to match
     * @param minAge Minimum file age in seconds (-1 to disable)
     * @param maxAge Maximum file age in seconds (-1 to disable)
     * @param ageCalculationMode Age calculation mode (LAST_MODIFIED or CREATION_TIME)
     * @param dependencyConditions List of file dependency conditions
     * @throws RemoteFileSystemConnectorException if unable to create the consumer
     */
    public void addPathConsumer(String path, String fileNamePattern,
                                double minAge, double maxAge, String ageCalculationMode,
                                List<FileDependencyCondition> dependencyConditions)
            throws RemoteFileSystemConnectorException {
        if (pathConsumers.containsKey(path)) {
            log.debug("Consumer already exists for path: {}", path);
            return;
        }

        // Create path-specific listener that routes events for this path
        PathSpecificListener pathListener = new PathSpecificListener(ftpListener, path);

        // Build properties for this consumer
        Map<String, Object> pathProperties = buildPathProperties(path, fileNamePattern,
                minAge, maxAge, ageCalculationMode);

        RemoteFileSystemConsumer consumer;
        if (dependencyConditions != null && !dependencyConditions.isEmpty()) {
            consumer = new RemoteFileSystemConsumer(pathProperties, dependencyConditions, pathListener);
        } else {
            consumer = new RemoteFileSystemConsumer(pathProperties, pathListener);
        }

        pathConsumers.put(path, consumer);

        // Store FileSystemManager and options from first consumer
        if (fileSystemManager == null) {
            fileSystemManager = consumer.getFileSystemManager();
            fileSystemOptions = consumer.getFileSystemOptions();
        }

        log.debug("Added consumer for path: {}", path);
    }

    private Map<String, Object> buildPathProperties(String path, String fileNamePattern,
                                                    double minAge, double maxAge,
                                                    String ageCalculationMode) {
        Map<String, Object> props = new HashMap<>(baseProperties);

        // Update URI with the service-specific path
        String baseUri = (String) baseProperties.get(FtpConstants.URI);
        if (baseUri != null) {
            // Replace the path portion of the URI
            String newUri = replacePathInUri(baseUri, path);
            props.put(FtpConstants.URI, newUri);
        }

        // Set file name pattern
        props.remove(FtpConstants.FILE_NAME_PATTERN);
        if (fileNamePattern != null && !fileNamePattern.isEmpty()) {
            props.put(FtpConstants.FILE_NAME_PATTERN, fileNamePattern);
        }

        // Set age filter properties
        props.remove(FtpConstants.FILE_AGE_FILTER_MIN_AGE);
        props.remove(FtpConstants.FILE_AGE_FILTER_MAX_AGE);
        props.remove(FtpConstants.FILE_AGE_FILTER_AGE_CALCULATION_MODE);
        if (minAge >= 0) {
            props.put(FtpConstants.FILE_AGE_FILTER_MIN_AGE, String.valueOf(minAge));
        }
        if (maxAge >= 0) {
            props.put(FtpConstants.FILE_AGE_FILTER_MAX_AGE, String.valueOf(maxAge));
        }
        if (ageCalculationMode != null && !ageCalculationMode.isEmpty()) {
            props.put(FtpConstants.FILE_AGE_FILTER_AGE_CALCULATION_MODE, ageCalculationMode);
        }

        return props;
    }

    private String replacePathInUri(String uri, String newPath) {
        if (newPath == null || newPath.isEmpty()) {
            return uri;
        }
        String normalizedPath = newPath.startsWith("/") ? newPath : "/" + newPath;

        // URI format: protocol://[user:pass@]host[:port]/path
        // We need to replace everything after the host:port with the new path

        int schemeEnd = uri.indexOf("://");
        if (schemeEnd < 0) {
            return uri + normalizedPath;
        }

        int hostStart = schemeEnd + 3;
        int pathStart = uri.indexOf('/', hostStart);

        if (pathStart < 0) {
            // No path in original URI, append new path
            return uri + normalizedPath;
        }

        // Replace path
        return uri.substring(0, pathStart) + normalizedPath;
    }

    @Override
    public void poll() throws RemoteFileSystemConnectorException {
        if (pathConsumers.isEmpty()) {
            log.debug("No consumers registered yet, skipping poll");
            return;
        }

        if (isPollOperationOccupied.compareAndSet(false, true)) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Polling {} path consumers", pathConsumers.size());
                }

                // Poll all consumers
                for (Map.Entry<String, RemoteFileSystemConsumer> entry : pathConsumers.entrySet()) {
                    try {
                        entry.getValue().consume();
                    } catch (Exception e) {
                        log.error("Error polling path: " + entry.getKey(), e);
                    }
                }
            } finally {
                isPollOperationOccupied.set(false);
            }
        } else {
            log.warn("A scheduled polling job was skipped as the previous job was still processing.");
        }
    }

    @Override
    public Object stop() {
        Object lastError = null;
        for (Map.Entry<String, RemoteFileSystemConsumer> entry : pathConsumers.entrySet()) {
            try {
                Object error = entry.getValue().close();
                if (error != null) {
                    lastError = error;
                }
            } catch (Exception e) {
                log.error("Error stopping consumer for path: " + entry.getKey(), e);
            }
        }
        pathConsumers.clear();
        return lastError;
    }

    @Override
    public FtpListener getFtpListener() {
        return ftpListener;
    }

    public FileSystemManager getFileSystemManager() {
        return fileSystemManager;
    }

    public FileSystemOptions getFileSystemOptions() {
        return fileSystemOptions;
    }

    /**
     * Checks if a consumer exists for the given path.
     *
     * @param path The path to check
     * @return true if a consumer exists for the path
     */
    public boolean hasConsumerForPath(String path) {
        return pathConsumers.containsKey(path);
    }

    /**
     * Gets the number of registered path consumers.
     *
     * @return The count of path consumers
     */
    public int getConsumerCount() {
        return pathConsumers.size();
    }
}
