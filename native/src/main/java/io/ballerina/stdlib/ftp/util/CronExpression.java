/*
 * Copyright (c) 2024 WSO2 LLC. (http://www.wso2.org).
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
 *
 */

package io.ballerina.stdlib.ftp.util;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Cron expression parser and calculator for scheduling file polling.
 * Supports standard Unix cron format with seconds field:
 * second (0-59) minute (0-59) hour (0-23) day-of-month (1-31) month (1-12) day-of-week (0-6 or SUN-SAT)
 */
public class CronExpression {

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
        this.expression = expression.trim();

        // Validate the expression
        CronExpressionUtil.validate(this.expression);

        // Parse all fields
        String[] fields = this.expression.split("\\s+");
        this.seconds = CronExpressionUtil.parseField(fields[0], 0, 59);
        this.minutes = CronExpressionUtil.parseField(fields[1], 0, 59);
        this.hours = CronExpressionUtil.parseField(fields[2], 0, 23);
        this.daysOfMonth = CronExpressionUtil.parseField(fields[3], 1, 31);
        this.months = CronExpressionUtil.parseMonthField(fields[4]);
        this.daysOfWeek = CronExpressionUtil.parseDayOfWeekField(fields[5]);
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

    @Override
    public String toString() {
        return expression;
    }
}
