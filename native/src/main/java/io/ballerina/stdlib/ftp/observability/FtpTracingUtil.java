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

import io.ballerina.runtime.observability.ObservabilityConstants;
import io.ballerina.runtime.observability.ObserveUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for injecting FTP trace spans into Ballerina strands.
 *
 * <p>Usage — call {@code createStrandProperties} to get a properties map that can be
 * passed as the second argument of {@code StrandMetadata}. The runtime will pick up
 * the embedded {@link FtpObserverContext} and create a child span automatically.
 *
 * <pre>{@code
 * Map<String, Object> props = FtpTracingUtil.createStrandProperties(
 *         FtpMetricsUtil.CONTEXT_LISTENER, listenerUrl, listenerProtocol,
 *         FtpMetricsUtil.EVENT_TYPE_CHANGE, filePath);
 * StrandMetadata strandMetadata = new StrandMetadata(isConcurrentSafe, props);
 * runtime.callMethod(service, methodName, strandMetadata, args);
 * }</pre>
 */
public class FtpTracingUtil {

    private FtpTracingUtil() {
    }

    /**
     * Creates strand properties containing an {@link FtpObserverContext} for the given
     * connection coordinates, event type, and file path.
     *
     * <p>Returns {@code null} when tracing is disabled so callers can pass it directly
     * to {@code new StrandMetadata(isConcurrentSafe, props)} without branching.
     *
     * @param context   {@link FtpMetricsUtil#CONTEXT_CLIENT} or {@link FtpMetricsUtil#CONTEXT_LISTENER}
     * @param url       host:port of the remote server
     * @param protocol  "ftp", "ftps", or "sftp"
     * @param eventType event type tag value (e.g. {@link FtpMetricsUtil#EVENT_TYPE_CHANGE}),
     *                  or {@code null} to omit
     * @param filePath  path of the file involved, or {@code null} to omit
     * @return properties map to pass to {@code StrandMetadata}, or {@code null}
     */
    public static Map<String, Object> createStrandProperties(String context, String url, String protocol,
                                                              String eventType, String filePath) {
        if (!ObserveUtils.isTracingEnabled()) {
            return null;
        }
        FtpObserverContext observerContext = new FtpObserverContext(context, url, protocol);
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
     * Creates strand properties for a listener dispatch that has no specific file path
     * (e.g. batch deletions routed to {@code onFileDeleted}).
     *
     * @param context  context tag
     * @param url      host:port
     * @param protocol protocol string
     * @param eventType event type tag value, or {@code null}
     * @return properties map or {@code null} if tracing is disabled
     */
    public static Map<String, Object> createStrandProperties(String context, String url, String protocol,
                                                              String eventType) {
        return createStrandProperties(context, url, protocol, eventType, null);
    }

    /**
     * Builds a tagged {@link FtpObserverContext} for a client-side FTP operation.
     * Returns {@code null} when tracing is disabled.
     *
     * <p>The context carries the {@code remote_url}, {@code protocol}, {@code context},
     * {@code operation.type}, and {@code file.path} tags so they are available for
     * enriching whichever span mechanism the runtime supports.
     * Span lifecycle (start/finish) is handled by Ballerina's auto-instrumentation for
     * {@code client->method()} calls; this context supplements those spans with
     * FTP-specific tags when a native-code span API becomes available.
     *
     * @param url           remote URL (e.g. {@code sftp://host:22})
     * @param protocol      "ftp", "ftps", or "sftp"
     * @param operationType one of the {@code FtpMetricsUtil.OPERATION_TYPE_*} constants
     * @param filePath      source/target file path, or {@code null} to omit
     * @return tagged observer context, or {@code null} if tracing is disabled
     */
    public static FtpObserverContext createClientSpanContext(String url, String protocol,
                                                              String operationType, String filePath) {
        return createClientSpanContext(url, protocol, operationType, filePath, null);
    }

    /**
     * Builds a tagged {@link FtpObserverContext} for a two-path client operation
     * (rename, move, copy), including a {@code destination.path} tag.
     *
     * @param url             remote URL
     * @param protocol        protocol string
     * @param operationType   operation type constant
     * @param filePath        source path, or {@code null}
     * @param destinationPath destination path, or {@code null}
     * @return tagged observer context, or {@code null} if tracing is disabled
     */
    public static FtpObserverContext createClientSpanContext(String url, String protocol,
                                                              String operationType, String filePath,
                                                              String destinationPath) {
        if (!ObserveUtils.isTracingEnabled()) {
            return null;
        }
        FtpObserverContext ctx = new FtpObserverContext(FtpMetricsUtil.CONTEXT_CLIENT, url, protocol);
        if (operationType != null) {
            ctx.addTag(FtpObserverContext.TAG_OPERATION_TYPE, operationType);
        }
        if (filePath != null) {
            ctx.addTag(FtpObserverContext.TAG_FILE_PATH, filePath);
        }
        if (destinationPath != null) {
            ctx.addTag(FtpObserverContext.TAG_DESTINATION_PATH, destinationPath);
        }
        return ctx;
    }
}
