/*
 * Copyright (c) 2025 WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
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

package io.ballerina.stdlib.ftp.server;

import io.ballerina.runtime.api.types.AnnotatableType;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.types.TypeTags;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.ftp.exception.FtpInvalidConfigException;
import io.ballerina.stdlib.ftp.transport.message.FileInfo;
import io.ballerina.stdlib.ftp.util.FtpConstants;
import io.ballerina.stdlib.ftp.util.FtpFileExtensionMapper;
import io.ballerina.stdlib.ftp.util.FtpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static io.ballerina.stdlib.ftp.util.FtpConstants.ANNOTATION_AFTER_ERROR;
import static io.ballerina.stdlib.ftp.util.FtpConstants.ANNOTATION_AFTER_PROCESS;
import static io.ballerina.stdlib.ftp.util.FtpConstants.ANNOTATION_MOVE_TO;
import static io.ballerina.stdlib.ftp.util.FtpConstants.ANNOTATION_PRESERVE_SUB_DIRS;

/**
 * Routes files to appropriate content handler methods based on file extension and annotations.
 */
public class FormatMethodsHolder {

    private static final Logger log = LoggerFactory.getLogger(FormatMethodsHolder.class);
    private static final String FUNCTION_CONFIG_ANNOTATION = "FunctionConfig";
    private static final String ANNOTATION_PATTERN_FIELD = "fileNamePattern";

    private final BObject service;
    private final Map<String, MethodType> annotationPatternToMethod;
    private final Map<String, MethodType> availableContentMethods;
    private final MethodType onErrorMethod;
    private final Map<String, PostProcessAction> methodAfterProcessAction;
    private final Map<String, PostProcessAction> methodAfterErrorAction;

    public FormatMethodsHolder(BObject service) throws FtpInvalidConfigException {
        this.service = service;
        this.annotationPatternToMethod = new HashMap<>();
        this.availableContentMethods = new HashMap<>();
        this.onErrorMethod = FtpUtil.getOnErrorMethod(service).orElse(null);
        this.methodAfterProcessAction = new HashMap<>();
        this.methodAfterErrorAction = new HashMap<>();
        initializeMethodMappings();
    }

    private void initializeMethodMappings() throws FtpInvalidConfigException {
        MethodType[] contentMethods = FtpUtil.getAllContentHandlerMethods(service);

        for (MethodType method : contentMethods) {
            String methodName = method.getName();
            availableContentMethods.put(methodName, method);

            // Check for FileConfig annotation
            Optional<BMap<BString, Object>> annotationOpt = getFileConfigAnnotation(method);
            if (annotationOpt.isPresent()) {
                BMap<BString, Object> annotation = annotationOpt.get();

                // Parse fileNamePattern
                Object patternObj = annotation.get(StringUtils.fromString(ANNOTATION_PATTERN_FIELD));
                if (patternObj != null) {
                    String pattern = patternObj.toString();
                    annotationPatternToMethod.put(pattern, method);
                    log.debug("Registered annotation pattern '{}' for method '{}'", pattern, methodName);
                }

                // Parse afterProcess action
                PostProcessAction afterProcess = parsePostProcessAction(annotation, ANNOTATION_AFTER_PROCESS,
                        methodName);
                if (afterProcess != null) {
                    methodAfterProcessAction.put(methodName, afterProcess);
                    log.debug("Registered afterProcess action '{}' for method '{}'", afterProcess, methodName);
                }

                // Parse afterError action
                PostProcessAction afterError = parsePostProcessAction(annotation, ANNOTATION_AFTER_ERROR,
                        methodName);
                if (afterError != null) {
                    methodAfterErrorAction.put(methodName, afterError);
                    log.debug("Registered afterError action '{}' for method '{}'", afterError, methodName);
                }
            }
        }

        // Parse post-processing actions for onError method
        if (onErrorMethod != null) {
            Optional<BMap<BString, Object>> annotationOpt = getFileConfigAnnotation(onErrorMethod);
            if (annotationOpt.isPresent()) {
                BMap<BString, Object> annotation = annotationOpt.get();
                PostProcessAction afterProcess = parsePostProcessAction(annotation, ANNOTATION_AFTER_PROCESS,
                        onErrorMethod.getName());
                if (afterProcess != null) {
                    methodAfterProcessAction.put(onErrorMethod.getName(), afterProcess);
                    log.debug("Registered afterProcess action '{}' for onError method", afterProcess);
                }
                PostProcessAction afterError = parsePostProcessAction(annotation, ANNOTATION_AFTER_ERROR,
                        onErrorMethod.getName());
                if (afterError != null) {
                    methodAfterErrorAction.put(onErrorMethod.getName(), afterError);
                    log.debug("Registered afterError action '{}' for onError method", afterError);
                }
            }
        }
    }

    private PostProcessAction parsePostProcessAction(BMap<BString, Object> annotation, String fieldName,
                                                     String methodName) throws FtpInvalidConfigException {
        Object actionObj = annotation.get(StringUtils.fromString(fieldName));
        if (actionObj == null) {
            return null;
        }

        // Check if it's the DELETE constant (string)
        if (TypeUtils.getType(actionObj).getTag() == TypeTags.STRING_TAG) {
            return PostProcessAction.delete();
        }

        @SuppressWarnings("unchecked")
        BMap<BString, Object> moveRecord = (BMap<BString, Object>) actionObj;

        String moveTo = moveRecord.getStringValue(StringUtils.fromString(ANNOTATION_MOVE_TO)).getValue();
        if (moveTo.trim().isEmpty()) {
            throw new FtpInvalidConfigException(String.format(
                    "Move action in '%s' for method '%s' has an empty 'moveTo' path.",
                    fieldName, methodName));
        }

        // preserveSubDirs defaults to true
        boolean preserveSubDirs = moveRecord.getBooleanValue(StringUtils.fromString(ANNOTATION_PRESERVE_SUB_DIRS));

        return PostProcessAction.move(moveTo, preserveSubDirs);
    }

    /**
     * Routes a file to the appropriate content handler method.
     * Priority: Annotation override > Extension mapping > Generic onFile > onFileChange fallback
     *
     * @param fileInfo The file information
     * @return Optional containing the MethodType to invoke, or empty if no suitable method found
     */
    public Optional<MethodType> getMethod(FileInfo fileInfo) {
        String fileName = fileInfo.getFileName().getBaseName();
        String extension = fileInfo.getFileName().getExtension();

        // Priority 1: Check for annotation-based override
        Optional<MethodType> annotatedMethod = findMethodByAnnotationPattern(fileName);
        if (annotatedMethod.isPresent()) {
            log.debug("Routing file '{}' to method '{}' via annotation pattern",
                    fileName, annotatedMethod.get().getName());
            return annotatedMethod;
        }

        // Priority 2: Extension-based routing
        String recommendedMethod = FtpFileExtensionMapper.getMethodForExtension(extension);
        Optional<MethodType> extensionMethod = Optional.ofNullable(availableContentMethods.get(recommendedMethod));
        if (extensionMethod.isPresent()) {
            log.debug("Routing file '{}' with extension '{}' to method '{}'",
                    fileName, extension, recommendedMethod);
            return extensionMethod;
        }

        // Priority 3: Fallback to generic onFile
        Optional<MethodType> genericMethod = Optional.ofNullable(
                availableContentMethods.get(FtpConstants.ON_FILE_REMOTE_FUNCTION));
        if (genericMethod.isPresent()) {
            log.debug("Routing file '{}' to generic onFile method (fallback)", fileName);
            return genericMethod;
        }

        // Priority 4: No content method available - will fall back to onFileChange in caller
        log.debug("No content handler found for file '{}', will fall back to onFileChange", fileName);
        return Optional.empty();
    }

    /**
     * Finds a method by matching the file name against annotation patterns.
     *
     * @param fileName The file name to match
     * @return Optional containing the matching MethodType
     */
    private Optional<MethodType> findMethodByAnnotationPattern(String fileName) {
        for (Map.Entry<String, MethodType> entry : annotationPatternToMethod.entrySet()) {
            String pattern = entry.getKey();
            try {
                if (Pattern.matches(pattern, fileName)) {
                    return Optional.of(entry.getValue());
                }
            } catch (Exception e) {
                log.warn("Invalid regex pattern '{}' in FileConfig annotation: {}", pattern, e.getMessage());
            }
        }
        return Optional.empty();
    }

    /**
     * Gets the FileConfig annotation from a method if it exists.
     *
     * @param method The method to check
     * @return Optional containing the annotation map
     */
    private Optional<BMap<BString, Object>> getFileConfigAnnotation(MethodType method) {
        if (method instanceof AnnotatableType) {
            AnnotatableType annotatableMethod = (AnnotatableType) method;
            BMap<BString, Object> annotations = annotatableMethod.getAnnotations();

            if (annotations != null) {
                for (BString key : annotations.getKeys()) {
                    String keyStr = key.getValue();
                    if (keyStr.endsWith(FUNCTION_CONFIG_ANNOTATION)) {
                        Object annotationValue = annotations.get(key);
                        if (annotationValue instanceof BMap) {
                            return Optional.of((BMap<BString, Object>) annotationValue);
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Checks if any content handler methods are available.
     *
     * @return true if at least one content method is available
     */
    public boolean hasContentMethods() {
        return !availableContentMethods.isEmpty();
    }

    /**
     * Gets the onError method if it exists in the service.
     *
     * @return Optional containing the onError MethodType
     */
    public Optional<MethodType> getOnErrorMethod() {
        return Optional.ofNullable(onErrorMethod);
    }

    /**
     * Checks if an onError handler is available.
     *
     * @return true if onError method is available
     */
    public boolean hasOnErrorMethod() {
        return onErrorMethod != null;
    }

    /**
     * Gets the afterProcess action for a method.
     *
     * @param methodName The method name
     * @return Optional containing the PostProcessAction if configured
     */
    public Optional<PostProcessAction> getAfterProcessAction(String methodName) {
        return Optional.ofNullable(methodAfterProcessAction.get(methodName));
    }

    /**
     * Gets the afterError action for a method.
     *
     * @param methodName The method name
     * @return Optional containing the PostProcessAction if configured
     */
    public Optional<PostProcessAction> getAfterErrorAction(String methodName) {
        return Optional.ofNullable(methodAfterErrorAction.get(methodName));
    }

    /**
     * Checks if any content method has post-processing actions configured.
     *
     * @return true if any method has afterProcess or afterError actions
     */
    public boolean hasPostProcessingActions() {
        return !methodAfterProcessAction.isEmpty() || !methodAfterErrorAction.isEmpty();
    }
}
