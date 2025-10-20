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

package io.ballerina.stdlib.ftp.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for mapping file extensions to content handler method names.
 */
public final class FtpFileExtensionMapper {

    private static final Map<String, String> EXTENSION_TO_METHOD_MAP = new HashMap<>();

    static {
        // Default extension to method mappings as per proposal
        EXTENSION_TO_METHOD_MAP.put("json", FtpConstants.ON_FILE_JSON_REMOTE_FUNCTION);
        EXTENSION_TO_METHOD_MAP.put("xml", FtpConstants.ON_FILE_XML_REMOTE_FUNCTION);
        EXTENSION_TO_METHOD_MAP.put("csv", FtpConstants.ON_FILE_CSV_REMOTE_FUNCTION);
        EXTENSION_TO_METHOD_MAP.put("txt", FtpConstants.ON_FILE_TEXT_REMOTE_FUNCTION);
        EXTENSION_TO_METHOD_MAP.put("log", FtpConstants.ON_FILE_TEXT_REMOTE_FUNCTION);
        EXTENSION_TO_METHOD_MAP.put("md", FtpConstants.ON_FILE_TEXT_REMOTE_FUNCTION);
    }

    private FtpFileExtensionMapper() {
        // private constructor
    }

    /**
     * Gets the recommended content handler method name for a given file extension.
     *
     * @param extension The file extension (without dot)
     * @return The method name if a mapping exists, or ON_FILE_REMOTE_FUNCTION as fallback
     */
    public static String getMethodForExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return FtpConstants.ON_FILE_REMOTE_FUNCTION;
        }

        String normalizedExtension = extension.toLowerCase();
        return EXTENSION_TO_METHOD_MAP.getOrDefault(normalizedExtension,
                FtpConstants.ON_FILE_REMOTE_FUNCTION);
    }

    /**
     * Checks if a given file extension has a default mapping.
     *
     * @param extension The file extension (without dot)
     * @return true if a mapping exists
     */
    public static boolean hasMapping(String extension) {
        if (extension == null || extension.isEmpty()) {
            return false;
        }
        return EXTENSION_TO_METHOD_MAP.containsKey(extension.toLowerCase());
    }
}
