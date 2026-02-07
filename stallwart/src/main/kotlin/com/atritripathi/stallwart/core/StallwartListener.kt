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
package com.atritripathi.stallwart.core

/**
 * Callback interface for receiving freeze detection events from Stallwart.
 *
 * Implementations receive notifications when UI freezes are detected, allowing
 * for custom handling such as logging, crash reporting, or user notification.
 *
 * **Thread Safety:** Callbacks are invoked on the Watchdog background thread,
 * NOT the Main Thread (which may be blocked during ANR detection). Implementations
 * must handle their own thread synchronization if needed.
 *
 * **Performance:** Callbacks should execute quickly to avoid impacting detection
 * accuracy. Long-running operations should be dispatched to a separate thread.
 *
 * Example usage:
 * ```kotlin
 * val listener = object : StallwartListener {
 *     override fun onFreezeDetected(event: FreezeEvent) {
 *         when (event.type) {
 *             FreezeType.ANR -> crashReporter.recordAnr(event)
 *             FreezeType.JANK -> analytics.trackJank(event.durationMs)
 *         }
 *     }
 * }
 * ```
 */
public fun interface StallwartListener {
    /**
     * Called when a UI freeze is detected.
     *
     * @param event The freeze event containing diagnostic information including
     *              the actual Main Thread stack trace at detection time.
     */
    public fun onFreezeDetected(event: FreezeEvent)
}
