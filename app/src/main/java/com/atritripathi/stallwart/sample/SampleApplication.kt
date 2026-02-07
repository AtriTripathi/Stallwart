package com.atritripathi.stallwart.sample

import android.app.Application
import android.util.Log
import com.atritripathi.stallwart.Stallwart
import com.atritripathi.stallwart.core.FreezeType
import com.atritripathi.stallwart.exception.StallwartException

/**
 * Sample Application demonstrating Stallwart initialization.
 */
class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Stallwart with configuration DSL
        Stallwart.init(this) {
            // ANR threshold - 5 seconds (Android's default)
            anrThresholdMs = 5000L

            // Enable jank detection for performance monitoring
            enableJankDetection = true
            jankThresholdMs = 500L

            // Whitelist known slow operations
            whitelist { task ->
                // Ignore Google Play Services initialization
                task.contains("com.google.android.gms") ||
                // Ignore Firebase initialization
                task.contains("com.google.firebase")
            }

            // Handle freeze events
            listener { event ->
                when (event.type) {
                    FreezeType.ANR -> {
                        Log.e(TAG, "ANR DETECTED!")
                        Log.e(TAG, "Duration: ${event.durationMs}ms")
                        Log.e(TAG, "Task: ${event.taskDescription}")
                        Log.e(TAG, event.formatStackTrace())

                        // In production, send to your crash reporter:
                        // Crashlytics.recordException(StallwartException.from(event))

                        // For demo purposes, create the exception to show it works
                        val exception = StallwartException.from(event)
                        Log.e(TAG, "Exception message: ${exception.message}")
                    }

                    FreezeType.JANK -> {
                        Log.w(TAG, "Jank detected: ${event.durationMs}ms")
                        Log.w(TAG, "Task: ${event.taskDescription.take(80)}")

                        // In production, track for analytics:
                        // analytics.track("jank", mapOf("duration" to event.durationMs))
                    }
                }
            }
        }

        Log.i(TAG, "Stallwart initialized successfully")
    }

    companion object {
        private const val TAG = "Stallwart"
    }
}
