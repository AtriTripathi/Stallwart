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
package com.atritripathi.stallwart.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.atritripathi.stallwart.internal.WatchdogThread

/**
 * Lifecycle-aware component that automatically pauses/resumes the watchdog
 * based on the application's foreground/background state.
 *
 * This ensures Stallwart is a "good citizen" - consuming zero CPU cycles
 * when the app is backgrounded, where UI freezes are irrelevant anyway.
 *
 * Uses [ProcessLifecycleOwner] to observe the entire application lifecycle,
 * not individual Activity lifecycles.
 */
internal class StallwartLifecycleObserver(
    private val watchdogThread: WatchdogThread
) : DefaultLifecycleObserver {

    /**
     * Called when the app enters the foreground (any Activity becomes visible).
     * Resumes watchdog monitoring.
     */
    override fun onStart(owner: LifecycleOwner) {
        watchdogThread.resumeWatching()
    }

    /**
     * Called when the app enters the background (no Activities visible).
     * Pauses watchdog monitoring to conserve battery.
     */
    override fun onStop(owner: LifecycleOwner) {
        watchdogThread.pauseWatching()
    }

    companion object {
        /**
         * Registers the observer with the ProcessLifecycleOwner.
         * Must be called from the Main Thread.
         *
         * @param watchdogThread The watchdog to control
         * @return The registered observer (for later unregistration if needed)
         */
        fun register(watchdogThread: WatchdogThread): StallwartLifecycleObserver {
            val observer = StallwartLifecycleObserver(watchdogThread)
            ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
            return observer
        }

        /**
         * Unregisters the observer from the ProcessLifecycleOwner.
         * Must be called from the Main Thread.
         *
         * @param observer The observer to unregister
         */
        fun unregister(observer: StallwartLifecycleObserver) {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
        }
    }
}
