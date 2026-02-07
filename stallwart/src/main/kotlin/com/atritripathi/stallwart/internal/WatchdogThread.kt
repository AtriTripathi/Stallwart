/*
 * Copyright 2026 Atri Tripathi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.atritripathi.stallwart.internal

import android.os.Debug
import android.os.Handler
import android.os.Looper
import com.atritripathi.stallwart.config.StallwartConfig
import com.atritripathi.stallwart.core.FreezeEvent
import com.atritripathi.stallwart.core.FreezeType
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Heartbeat-based watchdog that detects Main Thread freezes.
 *
 * The algorithm is simple and reliable:
 * 1. Post a Runnable to the Main Thread that resets a tick counter to 0
 * 2. Sleep for an interval
 * 3. If the tick counter is not 0, the Main Thread hasn't processed our Runnable
 * 4. If tick >= threshold, the Main Thread is frozen - capture stack trace and report
 *
 * This approach works for ALL types of Main Thread blocking, including:
 * - Input event processing (touch handling)
 * - Choreographer frame callbacks
 * - Handler message processing
 * - Any synchronous work on the Main Thread
 *
 * Based on the proven pattern from [ANR-WatchDog](https://github.com/SalomonBrys/ANR-WatchDog).
 */
internal class WatchdogThread(
    private val config: StallwartConfig
) : Thread("Stallwart-Watchdog") {

    private val isRunning = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)

    /**
     * Tick counter that accumulates while Main Thread is unresponsive.
     * Reset to 0 by the ticker Runnable when Main Thread processes it.
     */
    @Volatile
    private var tick: Long = 0L

    /**
     * Tracks what level we've already reported for the current freeze.
     * null = nothing reported, JANK = jank reported, ANR = ANR reported
     */
    @Volatile
    private var reportedLevel: FreezeType? = null

    /**
     * Handler for posting to the Main Thread.
     */
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Runnable posted to Main Thread - resets tick to 0 when executed.
     */
    private val ticker = Runnable {
        tick = 0L
        reportedLevel = null
    }

    init {
        isDaemon = true
        priority = MAX_PRIORITY // High priority to ensure accurate detection
    }

    fun startWatching() {
        if (isRunning.compareAndSet(false, true)) {
            isPaused.set(false)
            if (state == State.NEW) {
                start()
            }
        }
    }

    fun pauseWatching() {
        isPaused.set(true)
    }

    fun resumeWatching() {
        isPaused.set(false)
        // Reset state when resuming
        tick = 0L
        reportedLevel = null
    }

    fun stopWatching() {
        isRunning.set(false)
        interrupt()
    }

    override fun run() {
        // Initial tick reset
        tick = 0L

        while (isRunning.get()) {
            // Skip if paused or debugger attached
            if (isPaused.get()) {
                sleepSafely(config.pollingIntervalMs)
                continue
            }

            if (Debug.isDebuggerConnected()) {
                // Don't report ANRs while debugging - breakpoints look like freezes
                tick = 0L
                reportedLevel = null
                sleepSafely(config.pollingIntervalMs)
                continue
            }

            // Post ticker to Main Thread
            mainHandler.post(ticker)

            // Sleep for the polling interval
            sleepSafely(config.pollingIntervalMs)

            // Accumulate tick (this happens if Main Thread hasn't run ticker yet)
            tick += config.pollingIntervalMs

            // Check if frozen (only if we haven't already reported ANR for this freeze)
            if (tick >= config.jankThresholdMs && reportedLevel != FreezeType.ANR) {
                checkForFreeze()
            }
        }
    }

    private fun checkForFreeze() {
        val currentTick = tick

        // Determine freeze type based on duration
        val freezeType = when {
            currentTick >= config.anrThresholdMs -> FreezeType.ANR
            currentTick >= config.jankThresholdMs && config.enableJankDetection -> FreezeType.JANK
            else -> return
        }

        // Skip if we've already reported this level or higher
        // JANK < ANR, so if we reported JANK, we can still escalate to ANR
        when {
            reportedLevel == FreezeType.ANR -> return // Already reported ANR, nothing more to do
            reportedLevel == FreezeType.JANK && freezeType == FreezeType.JANK -> return // Already reported jank
        }

        // Mark the level we're reporting
        reportedLevel = freezeType

        // Capture Main Thread stack trace
        val mainThread = Looper.getMainLooper().thread
        val stackTrace = mainThread.stackTrace

        // Build task description from stack trace
        val taskDescription = buildTaskDescription(stackTrace)

        // Check whitelist
        if (config.whitelistPredicate?.invoke(taskDescription) == true) {
            return
        }

        // Create freeze event
        val event = FreezeEvent(
            type = freezeType,
            durationMs = currentTick,
            taskDescription = taskDescription,
            mainThreadStackTrace = stackTrace,
            timestamp = System.currentTimeMillis()
        )

        // Notify listener
        try {
            config.listener?.onFreezeDetected(event)
        } catch (e: Exception) {
            // Don't let listener exceptions crash the watchdog
        }
    }

    /**
     * Builds a human-readable task description from the stack trace.
     * Finds the first app-related frame or the top of the trace.
     */
    private fun buildTaskDescription(stackTrace: Array<StackTraceElement>): String {
        // Find first non-system frame (likely the culprit)
        val appFrame = stackTrace.firstOrNull { frame ->
            !frame.className.startsWith("android.") &&
            !frame.className.startsWith("java.") &&
            !frame.className.startsWith("kotlin.") &&
            !frame.className.startsWith("dalvik.") &&
            !frame.className.startsWith("com.android.")
        }

        return if (appFrame != null) {
            "${appFrame.className}.${appFrame.methodName}(${appFrame.fileName}:${appFrame.lineNumber})"
        } else if (stackTrace.isNotEmpty()) {
            val top = stackTrace.first()
            "${top.className}.${top.methodName}"
        } else {
            "Unknown task"
        }
    }

    private fun sleepSafely(millis: Long) {
        try {
            sleep(millis)
        } catch (e: InterruptedException) {
            if (!isRunning.get()) {
                return
            }
        }
    }
}
