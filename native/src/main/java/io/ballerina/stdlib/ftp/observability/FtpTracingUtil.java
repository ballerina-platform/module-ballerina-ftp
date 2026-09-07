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
import io.ballerina.runtime.observability.tracer.BSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 *   <li><b>Client operations</b> — call {@link #sendMetricsData} from inside a native external
 *       method. Ballerina has already created an auto-instrumented span for the remote-method call;
 *       this helper enriches that span with the FTP-specific tags ({@code action.type},
 *       {@code context}, {@code remote.url}, etc.) so that the resulting
 *       {@code requests_total_value} metric is correctly labelled.</li>
 * </ul>
 */
public class FtpTracingUtil {

    private static final Logger log = LoggerFactory.getLogger(FtpTracingUtil.class);

    private FtpTracingUtil() {
    }

    /**
     * Creates a per-file parent span that covers the entire file lifecycle (found → cleaned_up).
     * The returned context has a {@link BSpan} set on it. When this context is set as the parent
     * of strand properties (via {@link #setParentContext}), the runtime automatically creates
     * child spans for each {@code callMethod} invocation.
     *
     * @param url      host:port of the remote server
     * @param protocol "ftp", "ftps", or "sftp"
     * @param filePath path of the file being processed (added as a trace-only tag)
     * @return context with parent span, or {@code null} if observability is disabled
     */
    public static FtpObserverContext createFileLifecycleContext(String url, String protocol, String filePath) {
        if (!ObserveUtils.isObservabilityEnabled()) {
            return null;
        }
        try {
            FtpObserverContext ctx = new FtpObserverContext(
                    FtpMetricsUtil.CONTEXT_LISTENER, url, protocol);
            ctx.addTag(FtpObserverContext.TAG_ACTION_TYPE, FtpMetricsUtil.ACTION_TYPE_EVENT);
            String instanceUrl = FtpMetricsUtil.getInstanceUrl();
            if (instanceUrl != null) {
                ctx.addTag(FtpObserverContext.TAG_INSTANCE_URL, instanceUrl);
            }
            BSpan span = BSpan.start("ftp", "file-lifecycle", false);
            if (filePath != null) {
                span.addTag(FtpObserverContext.TAG_FILE_PATH, filePath);
            }
            ctx.setSpan(span);
            return ctx;
        } catch (Throwable t) {
            log.debug("Failed to create file lifecycle context", t);
            return null;
        }
    }

    /**
     * Sets a parent context on the observer context inside strand properties, so that the
     * auto-instrumented span created by {@code callMethod} becomes a child of the parent's span.
     *
     * @param strandProperties the strand properties map (may be null)
     * @param parentCtx        the parent context with a span set on it (may be null)
     */
    public static void setParentContext(Map<String, Object> strandProperties,
                                         FtpObserverContext parentCtx) {
        if (strandProperties == null || parentCtx == null) {
            return;
        }
        try {
            Object ctxObj = strandProperties.get(ObservabilityConstants.KEY_OBSERVER_CONTEXT);
            if (ctxObj instanceof ObserverContext ctx) {
                ctx.setParent(parentCtx);
            }
        } catch (Throwable t) {
            log.debug("Failed to set parent context on strand properties", t);
        }
    }

    /**
     * Finishes the per-file parent span. Must be called exactly once per file, after all
     * processing (handler + cleanup) has completed.
     *
     * @param parentCtx the parent context returned by {@link #createFileLifecycleContext}, or null
     */
    public static void finishFileLifecycleSpan(FtpObserverContext parentCtx) {
        if (parentCtx == null) {
            return;
        }
        try {
            BSpan span = parentCtx.getSpan();
            if (span != null) {
                span.finishSpan();
            }
        } catch (Throwable t) {
            log.debug("Failed to finish file lifecycle span", t);
        }
    }

    /**
     * Creates strand properties containing an {@link FtpObserverContext} for a listener event
     * dispatch. Pass the returned map to {@code new StrandMetadata(isConcurrentSafe, props)} when
     * invoking a service method via {@code callMethod}.
     *
     * @param context   {@link FtpMetricsUtil#CONTEXT_LISTENER}
     * @param url       host:port of the remote server
     * @param protocol  "ftp", "ftps", or "sftp"
     * @param eventType event type tag value (e.g. {@link FtpMetricsUtil#EVENT_TYPE_CHANGE})
     * @param filePath  retained for API compatibility; not added to the context — {@code file.path}
     *                  is excluded from metric labels (unbounded cardinality) and cannot be added
     *                  span-only here because the span does not exist until the strand starts
     * @return properties map, or {@code null} if observability is disabled
     */
    public static Map<String, Object> createStrandProperties(String context, String url, String protocol,
                                                              String eventType, String filePath) {
        if (!ObserveUtils.isObservabilityEnabled()) {
            return null;
        }
        try {
            FtpObserverContext observerContext = new FtpObserverContext(context, url, protocol);
            observerContext.addTag(FtpObserverContext.TAG_ACTION_TYPE, FtpMetricsUtil.ACTION_TYPE_EVENT);
            String instanceUrl = FtpMetricsUtil.getInstanceUrl();
            if (instanceUrl != null) {
                observerContext.addTag(FtpObserverContext.TAG_INSTANCE_URL, instanceUrl);
            }
            observerContext.addTag(FtpObserverContext.TAG_EVENT_TYPE, eventType);
            Map<String, Object> properties = new HashMap<>();
            properties.put(ObservabilityConstants.KEY_OBSERVER_CONTEXT, observerContext);
            return properties;
        } catch (Throwable t) {
            log.debug("Failed to create strand properties", t);
            return null;
        }
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
     * Creates strand properties for a listener file lifecycle event with file stage, handler name,
     * and file metadata. This is the primary method for content-based handler dispatch.
     *
     * @param context     context tag
     * @param url         host:port
     * @param protocol    protocol string
     * @param eventType   event type (e.g. "create")
     * @param fileStage   file lifecycle stage (found, dispatched, handled, cleaned_up)
     * @param handlerName handler method name (e.g. "onFileJson"), or {@code null}
     * @param fileSize    file size in bytes, or -1 if unknown
     * @param modifiedTime last-modified timestamp, or -1 if unknown
     * @return properties map, or {@code null} if observability is disabled
     */
    public static Map<String, Object> createFileStageStrandProperties(String context, String url, String protocol,
                                                                       String eventType, String fileStage,
                                                                       String handlerName, long fileSize,
                                                                       long modifiedTime) {
        if (!ObserveUtils.isObservabilityEnabled()) {
            return null;
        }
        try {
            FtpObserverContext observerContext = new FtpObserverContext(context, url, protocol);
            observerContext.addTag(FtpObserverContext.TAG_ACTION_TYPE, FtpMetricsUtil.ACTION_TYPE_EVENT);
            String instanceUrl = FtpMetricsUtil.getInstanceUrl();
            if (instanceUrl != null) {
                observerContext.addTag(FtpObserverContext.TAG_INSTANCE_URL, instanceUrl);
            }
            observerContext.addTag(FtpObserverContext.TAG_EVENT_TYPE, eventType);
            observerContext.addTag(FtpObserverContext.TAG_FILE_STAGE, fileStage);
            if (handlerName != null) {
                observerContext.addTag(FtpObserverContext.TAG_HANDLER_NAME, handlerName);
            }
            if (fileSize >= 0) {
                observerContext.addProperty(FtpObserverContext.TAG_FILE_SIZE, fileSize);
            }
            if (modifiedTime >= 0) {
                observerContext.addProperty(FtpObserverContext.TAG_FILE_MODIFIED_TIME, modifiedTime);
            }
            Map<String, Object> properties = new HashMap<>();
            properties.put(ObservabilityConstants.KEY_OBSERVER_CONTEXT, observerContext);
            return properties;
        } catch (Throwable t) {
            log.debug("Failed to create file stage strand properties", t);
            return null;
        }
    }

    /**
     * Creates strand properties for a cleanup (post-processing) span.
     *
     * @param context       context tag
     * @param url           host:port
     * @param protocol      protocol string
     * @param cleanupAction cleanup action type (move, delete, none)
     * @param handlerName   handler method name that triggered this cleanup
     * @return properties map, or {@code null} if observability is disabled
     */
    public static Map<String, Object> createCleanupStrandProperties(String context, String url, String protocol,
                                                                     String cleanupAction, String handlerName) {
        if (!ObserveUtils.isObservabilityEnabled()) {
            return null;
        }
        try {
            FtpObserverContext observerContext = new FtpObserverContext(context, url, protocol);
            observerContext.addTag(FtpObserverContext.TAG_ACTION_TYPE, FtpMetricsUtil.ACTION_TYPE_EVENT);
            String instanceUrl = FtpMetricsUtil.getInstanceUrl();
            if (instanceUrl != null) {
                observerContext.addTag(FtpObserverContext.TAG_INSTANCE_URL, instanceUrl);
            }
            observerContext.addTag(FtpObserverContext.TAG_FILE_STAGE, FtpMetricsUtil.FILE_STAGE_CLEANED_UP);
            observerContext.addTag(FtpObserverContext.TAG_CLEANUP_ACTION, cleanupAction);
            if (handlerName != null) {
                observerContext.addTag(FtpObserverContext.TAG_HANDLER_NAME, handlerName);
            }
            Map<String, Object> properties = new HashMap<>();
            properties.put(ObservabilityConstants.KEY_OBSERVER_CONTEXT, observerContext);
            return properties;
        } catch (Throwable t) {
            log.debug("Failed to create cleanup strand properties", t);
            return null;
        }
    }

    /**
     * Creates strand properties for a listener error dispatch, adding {@code event.type=error}
     * and an {@code error.type} tag to the embedded observer context.
     *
     * @param context   context tag
     * @param url       host:port
     * @param protocol  protocol string
     * @param filePath  path of the file involved, or {@code null}
     * @param errorType Ballerina error type name (e.g. {@code "ConnectionError"})
     * @return properties map, or {@code null} if observability is disabled
     */
    public static Map<String, Object> createErrorStrandProperties(String context, String url, String protocol,
                                                                   String filePath, String errorType) {
        try {
            Map<String, Object> props = createStrandProperties(context, url, protocol,
                    FtpMetricsUtil.EVENT_TYPE_ERROR, filePath);
            if (props != null) {
                FtpObserverContext ctx = (FtpObserverContext) props.get(
                        ObservabilityConstants.KEY_OBSERVER_CONTEXT);
                ctx.addTag(FtpObserverContext.TAG_ERROR_TYPE, errorType);
                ctx.addTag(FtpObserverContext.TAG_OUTCOME, FtpMetricsUtil.OUTCOME_FAILURE);
            }
            return props;
        } catch (Throwable t) {
            log.debug("Failed to create error strand properties", t);
            return null;
        }
    }

    /**
     * Adds outcome and optional error type tags to strand properties.
     *
     * @param strandProperties the strand properties map (may be null)
     * @param outcome          success or failure
     * @param errorType        error type (Ballerina error name or predefined reason), or null
     */
    public static void addOutcomeToStrandProperties(Map<String, Object> strandProperties, String outcome,
                                                     String errorType) {
        if (strandProperties == null) {
            return;
        }
        try {
            FtpObserverContext ctx = (FtpObserverContext) strandProperties.get(
                    ObservabilityConstants.KEY_OBSERVER_CONTEXT);
            if (ctx == null) {
                return;
            }
            ctx.addTag(FtpObserverContext.TAG_OUTCOME, outcome);
            if (errorType != null) {
                ctx.addTag(FtpObserverContext.TAG_ERROR_TYPE, errorType);
            }
        } catch (Throwable t) {
            log.debug("Failed to add outcome to strand properties", t);
        }
    }

    /**
     * Enriches the auto-instrumented span for the currently executing native external method with
     * FTP client-operation tags. Metric tags ({@code action.type}, {@code context},
     * {@code remote.url}, {@code protocol}, {@code operation.type}) are added to the
     * {@link ObserverContext}; {@code file.path} and {@code destination.path} are span-only and
     * excluded from metrics to prevent unbounded label cardinality.
     *
     * @param env           the current Ballerina environment
     * @param url           remote URL
     * @param protocol      protocol string
     * @param operationType one of the {@code FtpMetricsUtil.OPERATION_TYPE_*} constants
     * @param filePath      source/target file path
     */
    public static void sendMetricsData(Environment env, String url, String protocol,
                                       String operationType, String filePath) {
        sendMetricsData(env, url, protocol, operationType, filePath, null);
    }

    /**
     * Variant for two-path operations ({@code rename}, {@code move}, {@code copy}), adding
     * a span-only {@code destination.path} tag.
     *
     * @param env             the current Ballerina environment
     * @param url             remote URL
     * @param protocol        protocol string
     * @param operationType   operation type constant
     * @param filePath        source path
     * @param destinationPath destination path, or {@code null} for single-path operations
     */
    public static void sendMetricsData(Environment env, String url, String protocol,
                                       String operationType, String filePath,
                                       String destinationPath) {
        try {
            ObserverContext ctx = ObserveUtils.getObserverContextOfCurrentFrame(env);
            if (ctx == null) {
                return;
            }
            ctx.addTag(FtpObserverContext.TAG_MODULE, FtpMetricsUtil.MODULE_FTP);
            ctx.addTag(FtpObserverContext.TAG_ACTION_TYPE, FtpMetricsUtil.ACTION_TYPE_OPERATION);
            ctx.addTag(FtpObserverContext.TAG_CONTEXT, FtpMetricsUtil.CONTEXT_CLIENT);
            ctx.addTag(FtpObserverContext.TAG_REMOTE_URL, url);
            ctx.addTag(FtpObserverContext.TAG_PROTOCOL, protocol);
            ctx.addTag(FtpObserverContext.TAG_OPERATION_TYPE, operationType);
            String instanceUrl = FtpMetricsUtil.getInstanceUrl();
            if (instanceUrl != null) {
                ctx.addTag(FtpObserverContext.TAG_INSTANCE_URL, instanceUrl);
            }
            BSpan span = ctx.getSpan();
            if (span != null) {
                span.addTag(FtpObserverContext.TAG_FILE_PATH, filePath);
                if (destinationPath != null) {
                    span.addTag(FtpObserverContext.TAG_DESTINATION_PATH, destinationPath);
                }
            }
        } catch (Throwable t) {
            log.debug("Failed to send metrics data", t);
        }
    }

    /**
     * Tags the auto-instrumented span of the current frame as a poll cycle span.
     *
     * @param env      the current Ballerina environment
     * @param url      remote URL
     * @param protocol protocol string
     */
    public static void sendPollMetricsData(Environment env, String url, String protocol) {
        try {
            ObserverContext ctx = ObserveUtils.getObserverContextOfCurrentFrame(env);
            if (ctx == null) {
                return;
            }
            ctx.addTag(FtpObserverContext.TAG_MODULE, FtpMetricsUtil.MODULE_FTP);
            ctx.addTag(FtpObserverContext.TAG_ACTION_TYPE, FtpMetricsUtil.ACTION_TYPE_POLL);
            ctx.addTag(FtpObserverContext.TAG_CONTEXT, FtpMetricsUtil.CONTEXT_LISTENER);
            ctx.addTag(FtpObserverContext.TAG_REMOTE_URL, url != null ? url : FtpMetricsUtil.UNKNOWN);
            ctx.addTag(FtpObserverContext.TAG_PROTOCOL, protocol != null ? protocol : FtpMetricsUtil.UNKNOWN);
            String instanceUrl = FtpMetricsUtil.getInstanceUrl();
            if (instanceUrl != null) {
                ctx.addTag(FtpObserverContext.TAG_INSTANCE_URL, instanceUrl);
            }
        } catch (Throwable t) {
            log.debug("Failed to send poll metrics data", t);
        }
    }

    /**
     * Adds the outcome tag to the poll span on the current frame.
     *
     * @param env     the current Ballerina environment
     * @param outcome success or failure
     */
    public static void sendPollOutcome(Environment env, String outcome) {
        try {
            ObserverContext ctx = ObserveUtils.getObserverContextOfCurrentFrame(env);
            if (ctx == null) {
                return;
            }
            ctx.addTag(FtpObserverContext.TAG_OUTCOME, outcome);
        } catch (Throwable t) {
            log.debug("Failed to send poll outcome", t);
        }
    }

    /**
     * Adds {@code error=true} and {@code error.type} tags to the auto-instrumented span for the
     * currently executing native external method. Call this after an operation returns a
     * {@link io.ballerina.runtime.api.values.BError} to record the error on the span.
     *
     * @param env       the current Ballerina environment
     * @param errorType Ballerina error type name (e.g. {@code "ConnectionError"})
     */
    public static void sendErrorMetricsOnCurrentFrame(Environment env, String errorType) {
        try {
            ObserverContext ctx = ObserveUtils.getObserverContextOfCurrentFrame(env);
            if (ctx == null) {
                return;
            }
            ctx.addTag(ObservabilityConstants.TAG_KEY_ERROR, ObservabilityConstants.TAG_TRUE_VALUE);
            ctx.addTag(FtpObserverContext.TAG_ERROR_TYPE, errorType);
            ctx.addTag(FtpObserverContext.TAG_OUTCOME, FtpMetricsUtil.OUTCOME_FAILURE);
        } catch (Throwable t) {
            log.debug("Failed to send error metrics on current frame", t);
        }
    }

    /**
     * Adds file.size and file.modified_time as span-only tags to strand properties.
     * These are trace-only to avoid metric cardinality explosion.
     *
     * @param strandProperties the strand properties map (may be null)
     * @param fileSize         file size in bytes, or -1 if unknown
     * @param modifiedTime     last-modified timestamp, or -1 if unknown
     * @param filePath         file path for span-only tag
     */
    public static void addFileMetadataToStrandProperties(Map<String, Object> strandProperties,
                                                          long fileSize, long modifiedTime, String filePath) {
        if (strandProperties == null) {
            return;
        }
        try {
            FtpObserverContext ctx = (FtpObserverContext) strandProperties.get(
                    ObservabilityConstants.KEY_OBSERVER_CONTEXT);
            if (ctx == null) {
                return;
            }
            // These are stored as properties and will be added as span-only tags
            // when the span is available (after strand starts)
            if (fileSize >= 0) {
                ctx.addProperty(FtpObserverContext.TAG_FILE_SIZE, fileSize);
            }
            if (modifiedTime >= 0) {
                ctx.addProperty(FtpObserverContext.TAG_FILE_MODIFIED_TIME, modifiedTime);
            }
            if (filePath != null) {
                ctx.addProperty(FtpObserverContext.TAG_FILE_PATH, filePath);
            }
        } catch (Throwable t) {
            log.debug("Failed to add file metadata to strand properties", t);
        }
    }
}
