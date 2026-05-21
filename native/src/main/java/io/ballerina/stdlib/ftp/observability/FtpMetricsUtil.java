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

import java.net.InetAddress;

/**
 * Utility class for recording FTP connector metrics.
 *
 * <p>Metrics published:
 * <ul>
 *   <li>{@code ftp_active_connections} (gauge) — number of active FTP/FTPS/SFTP connections</li>
 * </ul>
 */
public class FtpMetricsUtil {
    private static final String CONNECTOR_NAME = "ftp";
    private static final String[] METRIC_ACTIVE_CONNECTIONS = {
            "active_connections", "Number of active FTP/FTPS/SFTP connections"};

    /** Sentinel used when a URL or protocol value is unavailable. */
    public static final String UNKNOWN = "unknown";

    /** Context tag value for FTP client operations. */
    public static final String CONTEXT_CLIENT = "client";

    /** Context tag value for FTP listener operations. */
    public static final String CONTEXT_LISTENER = "listener";

    /** Event-type tag value: a file was added or created. */
    public static final String EVENT_TYPE_CHANGE = "create";

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

    /** Action-type tag value for FTP client operations (get, put, delete, etc.). */
    public static final String ACTION_TYPE_OPERATION = "operation";

    /** Action-type tag value for FTP listener event dispatches (create, delete, error). */
    public static final String ACTION_TYPE_EVENT = "event";

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

    private static String instanceUrl;
    private static boolean instanceUrlResolved;

    /** Returns the hostname of the current instance, resolved lazily on first use, or {@code null} if unavailable. */
    public static String getInstanceUrl() {
        if (!instanceUrlResolved) {
            try {
                instanceUrl = InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                instanceUrl = null;
            }
            instanceUrlResolved = true;
        }
        return instanceUrl;
    }

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
     * Maps a raw {@link Throwable} to an observability error-type constant.
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
            case "CloseError" -> ERROR_TYPE_CLOSE;
            default -> UNKNOWN;
        };
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
