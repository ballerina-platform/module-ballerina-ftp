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
    private volatile FileSystemManager fileSystemManager;
    private volatile FileSystemOptions fileSystemOptions;

    /**
     * Creates a connector that can manage multiple path-specific consumers using the provided base configuration and listener.
     *
     * @param baseProperties base configuration properties used as a template for per-path consumers; a shallow copy is made
     * @param ftpListener    listener used to route FTP events to registered consumers
     */
    public MultiPathServerConnector(Map<String, Object> baseProperties, FtpListener ftpListener) {
        this.baseProperties = new HashMap<>(baseProperties);
        this.ftpListener = ftpListener;
    }

    /**
     * Register a per-path consumer that monitors the given directory and routes events to the FTP listener.
     *
     * @param path the directory path to monitor; used to create a path-specific listener and to replace the path portion of the base URI
     * @param fileNamePattern a regex pattern to filter file names; omit or pass null/empty to disable name filtering
     * @param minAge minimum file age in seconds to include (-1 to disable)
     * @param maxAge maximum file age in seconds to include (-1 to disable)
     * @param ageCalculationMode the file age calculation mode, e.g. "LAST_MODIFIED" or "CREATION_TIME"; omit or pass null/empty to use defaults
     * @param dependencyConditions optional list of file dependency conditions; when present the consumer enforces these before emitting events
     * @throws RemoteFileSystemConnectorException if the consumer cannot be created or initialized
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

    /**
     * Builds a path-specific properties map by cloning the connector's base properties and applying
     * overrides for the target path, file name pattern, and file age filters.
     *
     * The returned map will contain the base properties with:
     * - the URI updated to use the provided path (if a base URI exists),
     * - the FILE_NAME_PATTERN set when a non-empty pattern is provided,
     * - the FILE_AGE_FILTER_* entries set when the corresponding values are provided (min/max as
     *   non-negative numbers and a non-empty ageCalculationMode).
     *
     * @param path the path to apply to the base URI; may be null or empty to leave the URI path unchanged
     * @param fileNamePattern a glob/regex pattern to filter files by name; ignored if null or empty
     * @param minAge minimum file age (in the same units used by the connector) to include; ignored if negative
     * @param maxAge maximum file age to include; ignored if negative
     * @param ageCalculationMode mode used to calculate file age; ignored if null or empty
     * @return a new Map containing the path-specific properties derived from the connector's base properties
     */
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

    /**
     * Replace the path portion of a URI with the provided newPath.
     *
     * The newPath is normalized to start with a leading '/' before substitution. If newPath is null or empty,
     * the original uri is returned. If the uri has no scheme (no "://") or has no existing path component,
     * the normalized newPath is appended to the uri.
     *
     * @param uri the original URI to modify
     * @param newPath the path to set in the URI
     * @return the URI with its path replaced by newPath (or the original uri when newPath is null/empty)
     */
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

    /**
     * Triggers a single polling cycle across all registered path consumers.
     *
     * <p>If no consumers are registered, the method returns immediately. Only one poll may run at a time; if a poll
     * is already in progress the invocation is skipped. Exceptions thrown by individual consumers are caught and logged
     * so that polling continues for other consumers.</p>
     */
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

    /**
     * Stops and closes all path-specific consumers and clears the internal consumer registry.
     *
     * Iterates over each registered consumer, attempts to close it, and records the most recent non-null
     * error returned by any consumer close operation. Exceptions thrown while closing individual consumers
     * are logged but do not interrupt stopping other consumers.
     *
     * @return the last non-null error object returned by a consumer's close method, or {@code null} if none
     */
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

    /**
     * Retrieve the FtpListener associated with this connector.
     *
     * @return the associated {@link FtpListener} instance
     */
    @Override
    public FtpListener getFtpListener() {
        return ftpListener;
    }

    /**
     * Returns the cached FileSystemManager instance initialized from the first added path consumer.
     *
     * @return the cached FileSystemManager, or {@code null} if no consumer has initialized it
     */
    public FileSystemManager getFileSystemManager() {
        return fileSystemManager;
    }

    /**
     * Retrieve the cached FileSystemOptions initialized from the first added consumer.
     *
     * @return the cached FileSystemOptions used by path consumers, or `null` if not yet initialized.
     */
    public FileSystemOptions getFileSystemOptions() {
        return fileSystemOptions;
    }

    /**
     * Determine whether a consumer is registered for the specified path.
     *
     * @return true if a consumer is registered for the path, false otherwise.
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