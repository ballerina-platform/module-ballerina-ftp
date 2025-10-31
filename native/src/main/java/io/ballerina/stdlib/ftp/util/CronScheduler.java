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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler for executing tasks based on cron expressions.
 */
public class CronScheduler {

    private static final Logger log = LoggerFactory.getLogger(CronScheduler.class);
    private final CronExpression cronExpression;
    private final Runnable task;
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> currentSchedule;
    private volatile boolean isRunning;

    /**
     * Creates a new cron scheduler.
     *
     * @param cronExpression The cron expression to use for scheduling
     * @param task The task to execute
     */
    public CronScheduler(CronExpression cronExpression, Runnable task) {
        this.cronExpression = cronExpression;
        this.task = task;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "ftp-cron-scheduler");
            thread.setDaemon(true);
            return thread;
        });
        this.isRunning = false;
    }

    /**
     * Starts the scheduler.
     */
    public synchronized void start() {
        if (isRunning) {
            log.warn("Cron scheduler is already running");
            return;
        }

        isRunning = true;
        scheduleNext();
        log.info("Cron scheduler started with expression: {}", cronExpression);
    }

    /**
     * Stops the scheduler.
     */
    public synchronized void stop() {
        if (!isRunning) {
            return;
        }

        isRunning = false;
        if (currentSchedule != null && !currentSchedule.isDone()) {
            currentSchedule.cancel(false);
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Cron scheduler stopped");
    }

    /**
     * Schedules the next execution based on the cron expression.
     */
    private void scheduleNext() {
        if (!isRunning) {
            return;
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextExecution = cronExpression.getNextExecutionTime(now);
            long delaySeconds = cronExpression.getSecondsUntilNextExecution(now);

            log.debug("Next execution scheduled at {} (in {} seconds)", nextExecution, delaySeconds);

            currentSchedule = executor.schedule(() -> {
                try {
                    if (isRunning) {
                        task.run();
                    }
                } catch (Exception e) {
                    log.error("Error executing scheduled task", e);
                } finally {
                    // Schedule the next execution
                    scheduleNext();
                }
            }, delaySeconds, TimeUnit.SECONDS);

        } catch (Exception e) {
            log.error("Error scheduling next execution", e);
            if (isRunning) {
                // Retry after 60 seconds in case of error
                currentSchedule = executor.schedule(this::scheduleNext, 60, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * Checks if the scheduler is currently running.
     *
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return isRunning;
    }
}
