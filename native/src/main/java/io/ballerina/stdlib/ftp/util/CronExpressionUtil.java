/*
 * Copyright (c) 2025 WSO2 LLC. (http://www.wso2.org)
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.ftp.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utility class for validating and parsing cron expressions.
 */
public class CronExpressionUtil {

    private static final Map<String, Integer> MONTH_MAP = new HashMap<>();
    private static final Map<String, Integer> DAY_OF_WEEK_MAP = new HashMap<>();
    private static final Pattern CRON_PATTERN =
            Pattern.compile("^\\s*\\S+\\s+\\S+\\s+\\S+\\s+\\S+\\s+\\S+\\s+\\S+\\s*$");

    static {
        MONTH_MAP.put("JAN", 1);
        MONTH_MAP.put("FEB", 2);
        MONTH_MAP.put("MAR", 3);
        MONTH_MAP.put("APR", 4);
        MONTH_MAP.put("MAY", 5);
        MONTH_MAP.put("JUN", 6);
        MONTH_MAP.put("JUL", 7);
        MONTH_MAP.put("AUG", 8);
        MONTH_MAP.put("SEP", 9);
        MONTH_MAP.put("OCT", 10);
        MONTH_MAP.put("NOV", 11);
        MONTH_MAP.put("DEC", 12);

        DAY_OF_WEEK_MAP.put("SUN", 0);
        DAY_OF_WEEK_MAP.put("MON", 1);
        DAY_OF_WEEK_MAP.put("TUE", 2);
        DAY_OF_WEEK_MAP.put("WED", 3);
        DAY_OF_WEEK_MAP.put("THU", 4);
        DAY_OF_WEEK_MAP.put("FRI", 5);
        DAY_OF_WEEK_MAP.put("SAT", 6);
    }

    private CronExpressionUtil() {
        // private constructor
    }

    /**
     * Validates a cron expression string.
     *
     * @param expression The expression to validate
     * @throws IllegalArgumentException if the expression is invalid
     */
    public static void validate(String expression) throws IllegalArgumentException {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("Cron expression cannot be null or empty");
        }

        String trimmed = expression.trim();

        if (!CRON_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Invalid cron expression format. Expected: " +
                    "second minute hour day-of-month month day-of-week");
        }

        String[] fields = trimmed.split("\\s+");
        if (fields.length != 6) {
            throw new IllegalArgumentException("Cron expression must have exactly 6 fields");
        }

        try {
            parseField(fields[0], 0, 59);
            parseField(fields[1], 0, 59);
            parseField(fields[2], 0, 23);
            parseField(fields[3], 1, 31);
            parseMonthField(fields[4]);
            parseDayOfWeekField(fields[5]);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cron expression: " + e.getMessage(), e);
        }
    }

    /**
     * Parses a cron field into a list of valid values.
     *
     * @param field The field string to parse
     * @param min Minimum valid value
     * @param max Maximum valid value
     * @return List of valid values
     */
    public static List<Integer> parseField(String field, int min, int max) {
        List<Integer> values = new ArrayList<>();

        if (field.equals("*")) {
            for (int i = min; i <= max; i++) {
                values.add(i);
            }
            return values;
        }

        String[] parts = field.split(",");
        for (String part : parts) {
            if (part.contains("/")) {
                // Step values: */5 or 10-30/5
                String[] stepParts = part.split("/");
                int step = Integer.parseInt(stepParts[1]);

                if (stepParts[0].equals("*")) {
                    for (int i = min; i <= max; i += step) {
                        values.add(i);
                    }
                } else if (stepParts[0].contains("-")) {
                    String[] range = stepParts[0].split("-");
                    int start = Integer.parseInt(range[0]);
                    int end = Integer.parseInt(range[1]);
                    for (int i = start; i <= end; i += step) {
                        values.add(i);
                    }
                } else {
                    int start = Integer.parseInt(stepParts[0]);
                    for (int i = start; i <= max; i += step) {
                        values.add(i);
                    }
                }
            } else if (part.contains("-")) {
                // Range: 10-20
                String[] range = part.split("-");
                int start = Integer.parseInt(range[0]);
                int end = Integer.parseInt(range[1]);
                for (int i = start; i <= end; i++) {
                    values.add(i);
                }
            } else {
                // Single value
                values.add(Integer.parseInt(part));
            }
        }

        // Validate all values are within range
        for (Integer value : values) {
            if (value < min || value > max) {
                throw new IllegalArgumentException(
                    String.format("Value %d is out of range [%d-%d]", value, min, max));
            }
        }

        return values;
    }

    /**
     * Parses month field, supporting both numeric and text values.
     */
    public static List<Integer> parseMonthField(String field) {
        String normalized = field.toUpperCase();
        for (Map.Entry<String, Integer> entry : MONTH_MAP.entrySet()) {
            normalized = normalized.replace(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return parseField(normalized, 1, 12);
    }

    /**
     * Parses day-of-week field, supporting both numeric and text values.
     */
    public static List<Integer> parseDayOfWeekField(String field) {
        String normalized = field.toUpperCase();
        for (Map.Entry<String, Integer> entry : DAY_OF_WEEK_MAP.entrySet()) {
            normalized = normalized.replace(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return parseField(normalized, 0, 6);
    }
}
