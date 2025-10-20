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
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
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

/**
 * Routes files to appropriate content handler methods based on file extension and annotations.
 */
public class ContentMethodRouter {

    private static final Logger log = LoggerFactory.getLogger(ContentMethodRouter.class);
    private static final String FILE_CONFIG_ANNOTATION = "FileConfig";
    private static final String ANNOTATION_PATTERN_FIELD = "pattern";

    private final BObject service;
    private final Map<String, MethodType> annotationPatternToMethod;
    private final Map<String, MethodType> availableContentMethods;

    public ContentMethodRouter(BObject service) {
        this.service = service;
        this.annotationPatternToMethod = new HashMap<>();
        this.availableContentMethods = new HashMap<>();
        initializeMethodMappings();
    }

    /**
     * Initializes method mappings by scanning all content methods and their annotations.
     */
    private void initializeMethodMappings() {
        MethodType[] contentMethods = FtpUtil.getAllContentHandlerMethods(service);

        for (MethodType method : contentMethods) {
            String methodName = method.getName();
            availableContentMethods.put(methodName, method);

            // Check for FileConfig annotation
            Optional<BMap<BString, Object>> annotationOpt = getFileConfigAnnotation(method);
            if (annotationOpt.isPresent()) {
                BMap<BString, Object> annotation = annotationOpt.get();
                Object patternObj = annotation.get(io.ballerina.runtime.api.utils.StringUtils.
                        fromString(ANNOTATION_PATTERN_FIELD));
                if (patternObj != null) {
                    String pattern = patternObj.toString();
                    annotationPatternToMethod.put(pattern, method);
                    log.debug("Registered annotation pattern '{}' for method '{}'", pattern, methodName);
                }
            }
        }
    }

    /**
     * Routes a file to the appropriate content handler method.
     * Priority: Annotation override > Extension mapping > Generic onFile > onFileChange fallback
     *
     * @param fileInfo The file information
     * @return Optional containing the MethodType to invoke, or empty if no suitable method found
     */
    public Optional<MethodType> routeFile(FileInfo fileInfo) {
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
                    if (keyStr.endsWith(FILE_CONFIG_ANNOTATION)) {
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
}
