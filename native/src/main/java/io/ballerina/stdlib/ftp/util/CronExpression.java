/*
 * Copyright (c) 2025 WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Cron expression parser and calculator for scheduling file polling.
 * Supports standard Unix cron format with seconds field:
 * second (0-59) minute (0-59) hour (0-23) day-of-month (1-31) month (1-12) day-of-week (0-6 or SUN-SAT)
 */
public class CronExpression {

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

    private final String expression;
    private final List<Integer> seconds;
    private final List<Integer> minutes;
    private final List<Integer> hours;
    private final List<Integer> daysOfMonth;
    private final List<Integer> months;
    private final List<Integer> daysOfWeek;

    /**
     * Creates a new cron expression parser.
     *
     * @param expression The cron expression string
     * @throws IllegalArgumentException if the expression is invalid
     */
    public CronExpression(String expression) throws IllegalArgumentException {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("Cron expression cannot be null or empty");
        }

        this.expression = expression.trim();

        if (!CRON_PATTERN.matcher(this.expression).matches()) {
            throw new IllegalArgumentException("Invalid cron expression format. Expected: " +
                    "second minute hour day-of-month month day-of-week");
        }

        String[] fields = this.expression.split("\\s+");
        if (fields.length != 6) {
            throw new IllegalArgumentException("Cron expression must have exactly 6 fields");
        }

        try {
            this.seconds = parseField(fields[0], 0, 59);
            this.minutes = parseField(fields[1], 0, 59);
            this.hours = parseField(fields[2], 0, 23);
            this.daysOfMonth = parseField(fields[3], 1, 31);
            this.months = parseMonthField(fields[4]);
            this.daysOfWeek = parseDayOfWeekField(fields[5]);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cron expression: " + e.getMessage(), e);
        }
    }

    /**
     * Calculates the next execution time from the given base time.
     *
     * @param from The base time to calculate from
     * @return The next execution time
     */
    public LocalDateTime getNextExecutionTime(LocalDateTime from) {
        LocalDateTime current = from.truncatedTo(ChronoUnit.SECONDS).plusSeconds(1);

        // Maximum iterations to prevent infinite loops (search up to 4 years ahead)
        int maxIterations = 4 * 365 * 24 * 60 * 60;
        int iterations = 0;

        while (iterations++ < maxIterations) {
            if (matches(current)) {
                return current;
            }
            current = current.plusSeconds(1);
        }

        throw new IllegalStateException("Could not find next execution time within reasonable timeframe");
    }

    /**
     * Calculates the delay in seconds until the next execution time.
     *
     * @param from The base time to calculate from
     * @return The delay in seconds
     */
    public long getSecondsUntilNextExecution(LocalDateTime from) {
        LocalDateTime next = getNextExecutionTime(from);
        return ChronoUnit.SECONDS.between(from, next);
    }

    /**
     * Checks if the given time matches the cron expression.
     *
     * @param time The time to check
     * @return true if the time matches the expression
     */
    private boolean matches(LocalDateTime time) {
        return seconds.contains(time.getSecond()) &&
               minutes.contains(time.getMinute()) &&
               hours.contains(time.getHour()) &&
               matchesDayOfMonth(time) &&
               months.contains(time.getMonthValue()) &&
               matchesDayOfWeek(time);
    }

    /**
     * Checks if day of month matches, handling special case where day-of-month is "*".
     */
    private boolean matchesDayOfMonth(LocalDateTime time) {
        // If both day-of-month and day-of-week are specified (not *), either can match
        return daysOfMonth.contains(time.getDayOfMonth());
    }

    /**
     * Checks if day of week matches.
     */
    private boolean matchesDayOfWeek(LocalDateTime time) {
        // Convert Java DayOfWeek (MON=1, SUN=7) to cron format (SUN=0, SAT=6)
        int cronDayOfWeek = time.getDayOfWeek() == DayOfWeek.SUNDAY ? 0 : time.getDayOfWeek().getValue();
        return daysOfWeek.contains(cronDayOfWeek);
    }

    /**
     * Parses a cron field into a list of valid values.
     *
     * @param field The field string to parse
     * @param min Minimum valid value
     * @param max Maximum valid value
     * @return List of valid values
     */
    private List<Integer> parseField(String field, int min, int max) {
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
    private List<Integer> parseMonthField(String field) {
        String normalized = field.toUpperCase();
        for (Map.Entry<String, Integer> entry : MONTH_MAP.entrySet()) {
            normalized = normalized.replace(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return parseField(normalized, 1, 12);
    }

    /**
     * Parses day-of-week field, supporting both numeric and text values.
     */
    private List<Integer> parseDayOfWeekField(String field) {
        String normalized = field.toUpperCase();
        for (Map.Entry<String, Integer> entry : DAY_OF_WEEK_MAP.entrySet()) {
            normalized = normalized.replace(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return parseField(normalized, 0, 6);
    }

    @Override
    public String toString() {
        return expression;
    }

    /**
     * Validates a cron expression string.
     *
     * @param expression The expression to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String expression) {
        try {
            new CronExpression(expression);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
