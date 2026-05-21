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

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.observability.ObservabilityConstants;
import io.ballerina.runtime.observability.ObserveUtils;
import io.ballerina.runtime.observability.ObserverContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for injecting FTP observability context into Ballerina strands and spans.
 *
 * <p>Two usage patterns:
 * <ul>
 *   <li><b>Listener dispatch</b> — call {@link #createStrandProperties} to build a properties map
 *       that is embedded in {@code StrandMetadata} when dispatching a service method via
 *       {@code callMethod}. The runtime picks up the embedded {@link FtpObserverContext} and uses
 *       it as the span context for the entire service-method execution.</li>
 *   <li><b>Caller / client operations</b> — call {@link #sendMetricsData} from
 *       inside a native external method. Ballerina has already created an auto-instrumented span
 *       for the remote-method call; this helper enriches that span with the FTP-specific tags
 *       ({@code action_type}, {@code context}, {@code remote.url}, etc.) so that the resulting
 *       {@code requests_total_value} metric is correctly labelled.</li>
 * </ul>
 */
public class FtpTracingUtil {
    private static final String TAG_SRC_CLIENT_REMOTE = "src.client.remote";

    private FtpTracingUtil() {
    }

    /**
     * Creates strand properties containing an {@link FtpObserverContext} for a listener event
     * dispatch. Pass the returned map to {@code new StrandMetadata(isConcurrentSafe, props)} when
     * invoking a service method via {@code callMethod}.
     *
     * @param context   {@link FtpMetricsUtil#CONTEXT_LISTENER}
     * @param url       host:port of the remote server
     * @param protocol  "ftp", "ftps", or "sftp"
     * @param eventType event type tag value (e.g. {@link FtpMetricsUtil#EVENT_TYPE_CHANGE}),
     *                  or {@code null} to omit
     * @param filePath  path of the file involved, or {@code null} to omit
     * @return properties map, or {@code null} if tracing is disabled
     */
    public static Map<String, Object> createStrandProperties(String context, String url, String protocol,
                                                              String eventType, String filePath) {
        if (!ObserveUtils.isTracingEnabled()) {
            return null;
        }
        FtpObserverContext observerContext = new FtpObserverContext(context, url, protocol);
        observerContext.addTag(FtpObserverContext.TAG_ACTION_TYPE, FtpMetricsUtil.ACTION_TYPE_EVENT);
        String instanceUrl = FtpMetricsUtil.getInstanceUrl();
        if (instanceUrl != null) {
            observerContext.addTag(FtpObserverContext.TAG_INSTANCE_URL, instanceUrl);
        }
        if (eventType != null) {
            observerContext.addTag(FtpObserverContext.TAG_EVENT_TYPE, eventType);
        }
        if (filePath != null) {
            observerContext.addTag(FtpObserverContext.TAG_FILE_PATH, filePath);
        }
        Map<String, Object> properties = new HashMap<>();
        properties.put(ObservabilityConstants.KEY_OBSERVER_CONTEXT, observerContext);
        return properties;
    }

    /**
     * Overload without {@code filePath} for dispatches that have no single source file
     * (e.g. batch deletions routed to {@code onFileDeleted}).
     */
    public static Map<String, Object> createStrandProperties(String context, String url, String protocol,
                                                              String eventType) {
        return createStrandProperties(context, url, protocol, eventType, null);
    }

    /**
     * Creates strand properties for a listener error dispatch, adding {@code event.type=error}
     * and an {@code error.type} tag to the embedded observer context.
     *
     * @param context   context tag
     * @param url       host:port
     * @param protocol  protocol string
     * @param filePath  path of the file involved, or {@code null}
     * @param errorType one of the {@code FtpMetricsUtil.ERROR_TYPE_*} constants, or {@code null}
     * @return properties map, or {@code null} if tracing is disabled
     */
    public static Map<String, Object> createErrorStrandProperties(String context, String url, String protocol,
                                                                   String filePath, String errorType) {
        Map<String, Object> props = createStrandProperties(context, url, protocol,
                FtpMetricsUtil.EVENT_TYPE_ERROR, filePath);
        if (props != null && errorType != null) {
            FtpObserverContext ctx = (FtpObserverContext) props.get(ObservabilityConstants.KEY_OBSERVER_CONTEXT);
            if (ctx != null) {
                ctx.addTag(FtpObserverContext.TAG_ERROR_TYPE, errorType);
            }
        }
        return props;
    }

    /**
     * Enriches the auto-instrumented span for the currently executing native external method with
     * FTP client-operation tags.
     *
     * <p>Ballerina creates an observer context (span) for every remote-method call before
     * invoking the external Java implementation. Calling this method from inside the native
     * implementation adds the FTP-specific tags to that span, so that the corresponding
     * {@code requests_total_value} metric carries {@code action_type="operation"},
     * {@code context="client"}, {@code remote.url}, {@code protocol}, {@code operation.type},
     * {@code file.path}, and {@code host}.
     *
     * @param env           the current Ballerina environment
     * @param url           remote URL, or {@code null} to omit
     * @param protocol      protocol string, or {@code null} to omit
     * @param operationType one of the {@code FtpMetricsUtil.OPERATION_TYPE_*} constants
     * @param filePath      source/target file path, or {@code null} to omit
     */
    public static void sendMetricsData(Environment env, String url, String protocol,
                                                        String operationType, String filePath) {
        sendMetricsData(env, url, protocol, operationType, filePath, null);
    }

    /**
     * Variant for two-path operations ({@code rename}, {@code move}, {@code copy}), adding
     * a {@code destination.path} tag in addition to the standard operation tags.
     *
     * @param env             the current Ballerina environment
     * @param url             remote URL, or {@code null} to omit
     * @param protocol        protocol string, or {@code null} to omit
     * @param operationType   operation type constant
     * @param filePath        source path, or {@code null} to omit
     * @param destinationPath destination path, or {@code null} to omit
     */
    public static void sendMetricsData(Environment env, String url, String protocol,
                                                        String operationType, String filePath,
                                                        String destinationPath) {
        ObserverContext ctx = ObserveUtils.getObserverContextOfCurrentFrame(env);
        if (ctx == null) {
            return;
        }
        ObserverContext target = ctx;
        if (ctx.getTag(TAG_SRC_CLIENT_REMOTE) == null) {
            ObserverContext parent = ctx.getParent();
            if (parent != null) {
                target = parent;
            }
        }
        target.addTag(FtpObserverContext.TAG_ACTION_TYPE, FtpMetricsUtil.ACTION_TYPE_OPERATION);
        target.addTag(FtpObserverContext.TAG_CONTEXT, FtpMetricsUtil.CONTEXT_CLIENT);
        if (url != null) {
            target.addTag(FtpObserverContext.TAG_REMOTE_URL, url);
        }
        if (protocol != null) {
            target.addTag(FtpObserverContext.TAG_PROTOCOL, protocol);
        }
        if (operationType != null) {
            target.addTag(FtpObserverContext.TAG_OPERATION_TYPE, operationType);
        }
        if (filePath != null) {
            target.addTag(FtpObserverContext.TAG_FILE_PATH, filePath);
        }
        if (destinationPath != null) {
            target.addTag(FtpObserverContext.TAG_DESTINATION_PATH, destinationPath);
        }
        String instanceUrl = FtpMetricsUtil.getInstanceUrl();
        if (instanceUrl != null) {
            target.addTag(FtpObserverContext.TAG_INSTANCE_URL, instanceUrl);
        }
    }

    /**
     * Adds an {@code error.type} tag to the auto-instrumented span for the currently executing
     * native external method. Call this after an operation returns a {@link BError} to record
     * the error classification on the span.
     *
     * @param env       the current Ballerina environment; if {@code null} this is a no-op
     * @param errorType one of the {@code FtpMetricsUtil.ERROR_TYPE_*} constants
     */
    public static void sendErrorMetricsOnCurrentFrame(Environment env, String errorType) {
        if (env == null || errorType == null) {
            return;
        }
        ObserverContext ctx = ObserveUtils.getObserverContextOfCurrentFrame(env);
        if (ctx == null) {
            return;
        }
        ObserverContext target = ctx;
        if (ctx.getTag(TAG_SRC_CLIENT_REMOTE) == null) {
            ObserverContext parent = ctx.getParent();
            if (parent != null) {
                target = parent;
            }
        }
        target.addTag(FtpObserverContext.TAG_ERROR_TYPE, errorType);
    }
}
