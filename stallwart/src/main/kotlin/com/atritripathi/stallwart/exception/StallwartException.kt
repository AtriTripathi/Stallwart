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
package com.atritripathi.stallwart.exception

import com.atritripathi.stallwart.core.FreezeEvent
import com.atritripathi.stallwart.core.FreezeType

/**
 * An exception that represents a detected UI freeze, designed for crash reporting integration.
 *
 * This exception implements **Stack Trace Transplant** - a technique where the exception
 * carries the stack trace of the Main Thread (the actual culprit) rather than the
 * Watchdog thread that detected the freeze. This ensures crash reporters display
 * the meaningful stack trace showing what code was executing during the freeze.
 *
 * The exception is non-fatal by design and should typically be logged rather than thrown.
 *
 * Example integration with crash reporters:
 * ```kotlin
 * Stallwart.init(context) {
 *     listener { event ->
 *         if (event.type == FreezeType.ANR) {
 *             // Send to crash reporter with correct stack trace
 *             Crashlytics.recordException(StallwartException.from(event))
 *         }
 *     }
 * }
 * ```
 *
 * @property event The underlying freeze event with full diagnostic details
 */
public class StallwartException private constructor(
    message: String,
    public val event: FreezeEvent
) : Exception(message) {

    init {
        // Transplant the Main Thread's stack trace onto this exception
        // This is the key technique that makes crash reports actionable
        stackTrace = event.mainThreadStackTrace
    }

    public companion object {
        /**
         * Creates a [StallwartException] from a freeze event.
         *
         * The returned exception will have its stack trace set to the Main Thread's
         * stack trace at the time of freeze detection, making it immediately useful
         * for crash reporting tools.
         *
         * @param event The freeze event to convert
         * @return An exception with transplanted stack trace
         */
        @JvmStatic
        public fun from(event: FreezeEvent): StallwartException {
            val typeLabel = when (event.type) {
                FreezeType.ANR -> "ANR"
                FreezeType.JANK -> "Jank"
            }
            val message = buildString {
                append("[$typeLabel detected] ")
                append("Duration: ${event.durationMs}ms | ")
                append("Task: ${event.taskDescription.take(100)}")
                if (event.taskDescription.length > 100) append("...")
            }
            return StallwartException(message, event)
        }
    }
}
