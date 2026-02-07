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
package com.atritripathi.stallwart.config

import com.atritripathi.stallwart.core.FreezeEvent
import com.atritripathi.stallwart.core.StallwartListener

/**
 * Configuration for the Stallwart ANR detection library.
 *
 * Use the DSL builder pattern to create configurations:
 * ```kotlin
 * val config = StallwartConfig {
 *     anrThresholdMs = 5000L
 *     jankThresholdMs = 500L
 *     enableJankDetection = true
 *
 *     whitelist { taskDescription ->
 *         taskDescription.contains("FirebaseMessaging")
 *     }
 *
 *     listener { event ->
 *         Log.e("Stallwart", "Freeze detected: $event")
 *     }
 * }
 * ```
 *
 * @property anrThresholdMs Threshold in milliseconds for ANR detection (default: 5000ms)
 * @property jankThresholdMs Threshold in milliseconds for jank detection (default: 500ms)
 * @property enableJankDetection Whether to report jank events in addition to ANRs (default: false)
 * @property pollingIntervalMs How often the watchdog checks for freezes (default: 100ms)
 * @property whitelistPredicate Optional predicate to filter out tasks from monitoring
 * @property listener Callback for freeze events
 */
public class StallwartConfig private constructor(
    public val anrThresholdMs: Long,
    public val jankThresholdMs: Long,
    public val enableJankDetection: Boolean,
    public val pollingIntervalMs: Long,
    public val whitelistPredicate: ((String) -> Boolean)?,
    public val listener: StallwartListener?
) {
    /**
     * DSL builder for [StallwartConfig].
     */
    @StallwartConfigDsl
    public class Builder {
        /**
         * Threshold in milliseconds for ANR detection.
         * Tasks exceeding this duration will trigger [com.atritripathi.stallwart.core.FreezeType.ANR] events.
         *
         * Default: 5000ms (matches Android's system ANR threshold)
         */
        public var anrThresholdMs: Long = DEFAULT_ANR_THRESHOLD_MS

        /**
         * Threshold in milliseconds for jank detection.
         * Tasks exceeding this duration (but under [anrThresholdMs]) will trigger
         * [com.atritripathi.stallwart.core.FreezeType.JANK] events if [enableJankDetection] is true.
         *
         * Default: 500ms
         */
        public var jankThresholdMs: Long = DEFAULT_JANK_THRESHOLD_MS

        /**
         * Whether to report jank (non-fatal lag) events.
         * When false, only ANR events are reported.
         *
         * Default: false
         */
        public var enableJankDetection: Boolean = false

        /**
         * How often the watchdog thread checks for freezes.
         * Lower values increase detection accuracy but use more CPU.
         *
         * Default: 100ms
         */
        public var pollingIntervalMs: Long = DEFAULT_POLLING_INTERVAL_MS

        private var whitelistPredicate: ((String) -> Boolean)? = null
        private var listener: StallwartListener? = null

        /**
         * Adds a whitelist predicate to filter out specific tasks from monitoring.
         *
         * Use this to ignore known slow operations that shouldn't be reported,
         * such as third-party SDK initialization.
         *
         * Example:
         * ```kotlin
         * whitelist { task ->
         *     task.contains("com.google.firebase") ||
         *     task.contains("com.facebook.ads")
         * }
         * ```
         *
         * @param predicate Returns `true` for tasks that should be ignored
         */
        public fun whitelist(predicate: (taskDescription: String) -> Boolean) {
            this.whitelistPredicate = predicate
        }

        /**
         * Sets the listener for freeze events.
         *
         * Example:
         * ```kotlin
         * listener { event ->
         *     when (event.type) {
         *         FreezeType.ANR -> crashlytics.recordException(StallwartException.from(event))
         *         FreezeType.JANK -> analytics.track("jank", mapOf("duration" to event.durationMs))
         *     }
         * }
         * ```
         *
         * @param onFreeze Callback invoked when a freeze is detected
         */
        public fun listener(onFreeze: (FreezeEvent) -> Unit) {
            this.listener = StallwartListener { event -> onFreeze(event) }
        }

        /**
         * Sets the listener using a [StallwartListener] instance.
         *
         * @param listener The listener to receive freeze events
         */
        public fun listener(listener: StallwartListener) {
            this.listener = listener
        }

        internal fun build(): StallwartConfig {
            require(anrThresholdMs > 0) { "anrThresholdMs must be positive" }
            require(jankThresholdMs > 0) { "jankThresholdMs must be positive" }
            require(jankThresholdMs < anrThresholdMs) { "jankThresholdMs must be less than anrThresholdMs" }
            require(pollingIntervalMs > 0) { "pollingIntervalMs must be positive" }

            return StallwartConfig(
                anrThresholdMs = anrThresholdMs,
                jankThresholdMs = jankThresholdMs,
                enableJankDetection = enableJankDetection,
                pollingIntervalMs = pollingIntervalMs,
                whitelistPredicate = whitelistPredicate,
                listener = listener
            )
        }
    }

    public companion object {
        /** Default ANR threshold matching Android's system ANR threshold */
        public const val DEFAULT_ANR_THRESHOLD_MS: Long = 5_000L

        /** Default jank threshold for noticeable lag */
        public const val DEFAULT_JANK_THRESHOLD_MS: Long = 500L

        /** Default polling interval balancing accuracy and efficiency */
        public const val DEFAULT_POLLING_INTERVAL_MS: Long = 100L

        /**
         * Creates a [StallwartConfig] using the DSL builder.
         */
        public operator fun invoke(block: Builder.() -> Unit): StallwartConfig {
            return Builder().apply(block).build()
        }

        /**
         * Creates a default configuration.
         * Useful for quick setup - remember to add a listener.
         */
        public fun default(): StallwartConfig = Builder().build()
    }
}

/**
 * DSL marker to prevent scope leakage in the configuration builder.
 */
@DslMarker
@Target(AnnotationTarget.CLASS)
public annotation class StallwartConfigDsl
