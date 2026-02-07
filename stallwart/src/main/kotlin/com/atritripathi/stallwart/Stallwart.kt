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
package com.atritripathi.stallwart

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import com.atritripathi.stallwart.config.StallwartConfig
import com.atritripathi.stallwart.internal.WatchdogThread
import com.atritripathi.stallwart.lifecycle.StallwartLifecycleObserver
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Stallwart: A highly resilient ANR detection library for Android.
 *
 * Stallwart detects Main Thread freezes using a heartbeat mechanism:
 * - Posts a ping Runnable to the Main Thread every [StallwartConfig.pollingIntervalMs]
 * - If the ping isn't processed within the threshold, the Main Thread is frozen
 * - Captures the Main Thread's stack trace showing exactly what's blocking
 *
 * ## Quick Start
 *
 * Initialize in your Application class:
 *
 * ```kotlin
 * class MyApplication : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *
 *         Stallwart.init(this) {
 *             anrThresholdMs = 5000L
 *             enableJankDetection = true
 *
 *             listener { event ->
 *                 when (event.type) {
 *                     FreezeType.ANR -> Crashlytics.recordException(StallwartException.from(event))
 *                     FreezeType.JANK -> analytics.track("jank", mapOf("duration" to event.durationMs))
 *                 }
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ## Features
 *
 * - **Reliable Detection**: Works for all Main Thread blocking (input, rendering, etc.)
 * - **Stack Trace Capture**: Reports the actual code causing the freeze
 * - **Lifecycle Aware**: Automatically pauses when app is backgrounded
 * - **Debugger Safe**: Ignores freezes when debugger is attached
 *
 * @see StallwartConfig for configuration options
 * @see com.atritripathi.stallwart.core.FreezeEvent for event details
 * @see com.atritripathi.stallwart.exception.StallwartException for crash reporting
 */
public object Stallwart {

    private val isInitialized = AtomicBoolean(false)
    private val watchdogThread = AtomicReference<WatchdogThread?>(null)
    private val lifecycleObserver = AtomicReference<StallwartLifecycleObserver?>(null)

    /**
     * Initializes Stallwart with the given configuration.
     *
     * Must be called from the Main Thread, typically in [android.app.Application.onCreate].
     * Calling multiple times is safe - subsequent calls are ignored.
     *
     * @param context Application context
     * @param block Configuration DSL block
     */
    @MainThread
    @JvmStatic
    public fun init(context: Context, block: StallwartConfig.Builder.() -> Unit) {
        init(context, StallwartConfig(block))
    }

    /**
     * Initializes Stallwart with a pre-built configuration.
     *
     * @param context Application context
     * @param config The configuration to use
     */
    @MainThread
    @JvmStatic
    public fun init(context: Context, config: StallwartConfig) {
        checkMainThread("init() must be called from the Main Thread")

        if (!isInitialized.compareAndSet(false, true)) {
            return
        }

        // Create and start watchdog
        val watchdog = WatchdogThread(config)
        watchdogThread.set(watchdog)
        watchdog.startWatching()

        // Register lifecycle observer
        val observer = StallwartLifecycleObserver.register(watchdog)
        lifecycleObserver.set(observer)
    }

    /**
     * Returns whether Stallwart has been initialized.
     */
    @JvmStatic
    public fun isInitialized(): Boolean = isInitialized.get()

    /**
     * Manually pauses monitoring.
     *
     * Typically not needed - Stallwart automatically pauses when backgrounded.
     */
    @JvmStatic
    public fun pause() {
        watchdogThread.get()?.pauseWatching()
    }

    /**
     * Resumes monitoring after a manual pause.
     */
    @JvmStatic
    public fun resume() {
        watchdogThread.get()?.resumeWatching()
    }

    /**
     * Shuts down Stallwart completely.
     *
     * After calling this, Stallwart must be re-initialized.
     */
    @MainThread
    @JvmStatic
    public fun shutdown() {
        checkMainThread("shutdown() must be called from the Main Thread")

        if (!isInitialized.compareAndSet(true, false)) {
            return
        }

        lifecycleObserver.getAndSet(null)?.let { observer ->
            StallwartLifecycleObserver.unregister(observer)
        }

        watchdogThread.getAndSet(null)?.stopWatching()
    }

    /**
     * Initializes Stallwart on the Main Thread via Handler.
     *
     * Use this if initializing from a background thread.
     */
    @JvmStatic
    public fun initOnMainThread(context: Context, block: StallwartConfig.Builder.() -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            init(context, block)
        } else {
            Handler(Looper.getMainLooper()).post {
                init(context, block)
            }
        }
    }

    private fun checkMainThread(message: String) {
        check(Looper.myLooper() == Looper.getMainLooper()) { message }
    }
}
