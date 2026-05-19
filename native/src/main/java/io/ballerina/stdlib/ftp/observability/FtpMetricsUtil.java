/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.ftp.observability;

import io.ballerina.runtime.observability.ObserveUtils;
import io.ballerina.runtime.observability.metrics.DefaultMetricRegistry;
import io.ballerina.runtime.observability.metrics.MetricId;
import io.ballerina.runtime.observability.metrics.MetricRegistry;
import io.ballerina.stdlib.ftp.util.FtpUtil;

/**
 * Utility class for recording FTP connector metrics.
 *
 * <p>Metrics published:
 * <ul>
 *   <li>{@code ftp_active_connections} (gauge) — active FTP/FTPS/SFTP connections</li>
 *   <li>{@code ftp_file_operations} (counter) — client file operations</li>
 *   <li>{@code ftp_file_events} (counter) — listener file events</li>
 *   <li>{@code ftp_errors} (counter) — errors by type</li>
 * </ul>
 */
public class FtpMetricsUtil {
    private static final String CONNECTOR_NAME = "ftp";
    private static final String[] METRIC_ACTIVE_CONNECTIONS = {
            "active_connections", "Number of active FTP/FTPS/SFTP connections"};
    private static final String[] METRIC_FILE_OPERATIONS = {
            "file_operations", "Number of file operations performed"};
    private static final String[] METRIC_FILE_EVENTS = {
            "file_events", "Number of file events dispatched by the listener"};
    private static final String[] METRIC_ERRORS = {
            "errors", "Number of errors"};

    /** Sentinel used when a URL or protocol value is unavailable. */
    public static final String UNKNOWN = "unknown";

    /** Context tag value for FTP client operations. */
    public static final String CONTEXT_CLIENT = "client";

    /** Context tag value for FTP listener operations. */
    public static final String CONTEXT_LISTENER = "listener";

    /** Event-type tag value: a file was added or changed. */
    public static final String EVENT_TYPE_CHANGE = "change";

    /** Event-type tag value: a file was deleted. */
    public static final String EVENT_TYPE_DELETE = "delete";

    /** Event-type tag value: content-binding error dispatched to {@code onError}. */
    public static final String EVENT_TYPE_ERROR = "error";

    /** Operation-type tag value: get file content ({@code ftp:Client.getBytes()} and typed variants). */
    public static final String OPERATION_TYPE_GET = "get";

    /** Operation-type tag value: put file content ({@code ftp:Client.putBytes()} and typed variants). */
    public static final String OPERATION_TYPE_PUT = "put";

    /** Operation-type tag value: {@code ftp:Client.delete()}. */
    public static final String OPERATION_TYPE_DELETE = "delete";
    public static final String OPERATION_TYPE_RENAME = "rename";
    public static final String OPERATION_TYPE_MOVE = "move";
    public static final String OPERATION_TYPE_COPY = "copy";

    /** Operation-type tag value: {@code ftp:Client.mkdir()}. */
    public static final String OPERATION_TYPE_MKDIR = "mkdir";

    /** Operation-type tag value: {@code ftp:Client.rmdir()}. */
    public static final String OPERATION_TYPE_RMDIR = "rmdir";

    /** Error-type tag value: connection-level failure. */
    public static final String ERROR_TYPE_CONNECTION = "connection";

    /** Error-type tag value: authentication failure. */
    public static final String ERROR_TYPE_AUTHENTICATION = "authentication";

    /** Error-type tag value: file not found. */
    public static final String ERROR_TYPE_FILE_NOT_FOUND = "file_not_found";

    /** Error-type tag value: file already exists. */
    public static final String ERROR_TYPE_FILE_ALREADY_EXISTS = "file_already_exists";

    /** Error-type tag value: remote service unavailable (e.g. circuit-breaker open). */
    public static final String ERROR_TYPE_SERVICE_UNAVAILABLE = "service_unavailable";

    /** Error-type tag value: content binding / deserialization failure. */
    public static final String ERROR_TYPE_CONTENT_BINDING = "content_binding";

    /** Error-type tag value: all retry attempts exhausted. */
    public static final String ERROR_TYPE_RETRY_EXHAUSTED = "retry_exhausted";

    /** Error-type tag value: circuit breaker is open. */
    public static final String ERROR_TYPE_CIRCUIT_BREAKER_OPEN = "circuit_breaker_open";

    /** Error-type tag value: invalid configuration. */
    public static final String ERROR_TYPE_INVALID_CONFIG = "invalid_config";

    /** Error-type tag value: error while closing a connection. */
    public static final String ERROR_TYPE_CLOSE = "close";

    private static final MetricRegistry metricRegistry = DefaultMetricRegistry.getInstance();

    /**
     * Increments the active-connections gauge when a new FTP client or listener is created.
     *
     * @param url      host:port of the remote server
     * @param protocol "ftp", "ftps", or "sftp"
     * @param context  {@link #CONTEXT_CLIENT} or {@link #CONTEXT_LISTENER}
     */
    public static void reportNewConnection(String url, String protocol, String context) {
        if (!ObserveUtils.isMetricsEnabled()) {
            return;
        }
        FtpObserverContext observerContext = new FtpObserverContext(context, url, protocol);
        incrementGauge(observerContext, METRIC_ACTIVE_CONNECTIONS[0], METRIC_ACTIVE_CONNECTIONS[1]);
    }

    /**
     * Decrements the active-connections gauge when an FTP client or listener is closed.
     *
     * @param url      host:port of the remote server
     * @param protocol "ftp", "ftps", or "sftp"
     * @param context  client or listener
     */
    public static void reportConnectionClose(String url, String protocol, String context) {
        if (!ObserveUtils.isMetricsEnabled()) {
            return;
        }
        FtpObserverContext observerContext = new FtpObserverContext(context, url, protocol);
        decrementGauge(observerContext, METRIC_ACTIVE_CONNECTIONS[0], METRIC_ACTIVE_CONNECTIONS[1]);
    }

    /**
     * Increments the file-operations counter for any client operation.
     * File paths are intentionally excluded to avoid unbounded metric cardinality;
     * they are captured instead as tags on the trace span context.
     *
     * @param url           remote URL
     * @param protocol      protocol string
     * @param operationType one of the {@code OPERATION_TYPE_*} constants
     */
    public static void reportFileOperation(String url, String protocol, String operationType) {
        if (!ObserveUtils.isMetricsEnabled()) {
            return;
        }
        FtpObserverContext observerContext = new FtpObserverContext(CONTEXT_CLIENT, url, protocol);
        observerContext.addTag(FtpObserverContext.TAG_OPERATION_TYPE, operationType);
        incrementCounter(observerContext, METRIC_FILE_OPERATIONS[0], METRIC_FILE_OPERATIONS[1]);
    }

    /**
     * Increments the file-events counter when the listener dispatches a resource method.
     * File paths are excluded from the counter to avoid unbounded cardinality;
     * they are captured on the trace span context instead.
     *
     * @param url       remote URL
     * @param protocol  protocol string
     * @param eventType {@link #EVENT_TYPE_CHANGE}, {@link #EVENT_TYPE_DELETE}, or {@link #EVENT_TYPE_ERROR}
     */
    public static void reportFileEvent(String url, String protocol, String eventType) {
        if (!ObserveUtils.isMetricsEnabled()) {
            return;
        }
        FtpObserverContext observerContext = new FtpObserverContext(CONTEXT_LISTENER, url, protocol);
        observerContext.addTag(FtpObserverContext.TAG_EVENT_TYPE, eventType);
        incrementCounter(observerContext, METRIC_FILE_EVENTS[0], METRIC_FILE_EVENTS[1]);
    }

    /**
     * Increments the errors counter.
     *
     * @param url       host:port
     * @param protocol  protocol string
     * @param context   client or listener
     * @param errorType one of the {@code ERROR_TYPE_*} constants
     */
    public static void reportError(String url, String protocol, String context, String errorType) {
        if (!ObserveUtils.isMetricsEnabled()) {
            return;
        }
        FtpObserverContext observerContext = new FtpObserverContext(context, url, protocol);
        observerContext.addTag(FtpObserverContext.TAG_ERROR_TYPE, errorType);
        incrementCounter(observerContext, METRIC_ERRORS[0], METRIC_ERRORS[1]);
    }

    /**
     * Maps a raw {@link Throwable} (e.g. from {@code RemoteFileSystemListener.onError})
     * directly to an observability error-type constant, eliminating the need for callers
     * to pass through {@code FtpUtil.getErrorTypeForException} first.
     *
     * @param throwable the exception to classify
     * @return the matching {@code ERROR_TYPE_*} string
     */
    public static String toObservabilityErrorType(Throwable throwable) {
        return toObservabilityErrorType(FtpUtil.getErrorTypeForException(throwable));
    }

    /**
     * Maps a Ballerina FTP error type name to its observability constant.
     *
     * @param ballerinaErrorType the {@code error.getType().getName()} value
     * @return the matching {@code ERROR_TYPE_*} string
     */
    public static String toObservabilityErrorType(String ballerinaErrorType) {
        if (ballerinaErrorType == null) {
            return UNKNOWN;
        }
        return switch (ballerinaErrorType) {
            case "ConnectionError" -> ERROR_TYPE_CONNECTION;
            case "AuthenticationError" -> ERROR_TYPE_AUTHENTICATION;
            case "FileNotFoundError" -> ERROR_TYPE_FILE_NOT_FOUND;
            case "FileAlreadyExistsError" -> ERROR_TYPE_FILE_ALREADY_EXISTS;
            case "ServiceUnavailableError" -> ERROR_TYPE_SERVICE_UNAVAILABLE;
            case "ContentBindingError" -> ERROR_TYPE_CONTENT_BINDING;
            case "RetryError" -> ERROR_TYPE_RETRY_EXHAUSTED;
            case "CircuitBreakerOpenError" -> ERROR_TYPE_CIRCUIT_BREAKER_OPEN;
            case "InvalidConfigError" -> ERROR_TYPE_INVALID_CONFIG;
            default -> UNKNOWN;
        };
    }

    private static void incrementCounter(FtpObserverContext observerContext, String name, String desc) {
        if (metricRegistry == null) {
            return;
        }
        metricRegistry.counter(new MetricId(CONNECTOR_NAME + "_" + name, desc, observerContext.getAllTags()))
                .increment();
    }

    private static void incrementGauge(FtpObserverContext observerContext, String name, String desc) {
        if (metricRegistry == null) {
            return;
        }
        metricRegistry.gauge(new MetricId(CONNECTOR_NAME + "_" + name, desc, observerContext.getAllTags()))
                .increment();
    }

    private static void decrementGauge(FtpObserverContext observerContext, String name, String desc) {
        if (metricRegistry == null) {
            return;
        }
        metricRegistry.gauge(new MetricId(CONNECTOR_NAME + "_" + name, desc, observerContext.getAllTags()))
                .decrement();
    }

    private FtpMetricsUtil() {
    }
}
