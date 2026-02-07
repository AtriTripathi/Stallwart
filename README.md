# Stallwart

[![Maven Central](https://img.shields.io/maven-central/v/com.atritripathi/stallwart.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.atritripathi/stallwart)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)](https://android-arsenal.com/api?level=21)

**Stallwart** is a reliable ANR (Application Not Responding) detection library for Android that captures the exact code causing UI freezes.

## The Problem

When Android detects an ANR, the stack trace often shows the Main Thread idle or in system code—not the actual culprit:

```
// Typical system ANR trace - not helpful
android.os.MessageQueue.nativePollOnce(Native Method)
android.os.Looper.loop(Looper.java:206)
android.app.ActivityThread.main(ActivityThread.java:8668)
```

## The Solution

Stallwart detects freezes **before** the system does and captures a meaningful stack trace:

```
// Stallwart trace - actionable!
at java.lang.Thread.sleep(Native Method)
at com.myapp.MainActivity.simulateAnr(MainActivity.kt:56)
at com.myapp.MainActivity.onCreate$lambda$1(MainActivity.kt:37)
...
```

## Features

- **Reliable Detection** - Works for all Main Thread blocking (input, rendering, any sync work)
- **Accurate Stack Traces** - Captures the actual code causing the freeze
- **Jank Detection** - Optional reporting for shorter freezes (500ms+)
- **Lifecycle Aware** - Automatically pauses when app is backgrounded
- **Debugger Safe** - Ignores freezes when debugger is attached
- **Lightweight** - Single background thread, minimal overhead
- **Modern API** - Clean Kotlin DSL configuration

## Installation

Add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.atritripathi:stallwart:1.0.0")
}
```

> **Note**: The library is published to Maven Central, which is included by default in new Android projects. If you're using an older project, ensure `mavenCentral()` is in your repositories.

## Quick Start

Initialize in your `Application` class:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Stallwart.init(this) {
            // Thresholds
            anrThresholdMs = 5000L      // Report ANR after 5 seconds
            jankThresholdMs = 500L      // Report jank after 500ms
            enableJankDetection = true  // Enable jank reporting

            // Optional: ignore known slow operations
            whitelist { task ->
                task.contains("com.google.firebase")
            }

            // Handle freeze events
            listener { event ->
                when (event.type) {
                    FreezeType.ANR -> {
                        // Send to crash reporter
                        Crashlytics.recordException(StallwartException.from(event))
                    }
                    FreezeType.JANK -> {
                        // Track for performance monitoring
                        analytics.track("jank", mapOf("duration" to event.durationMs))
                    }
                }
            }
        }
    }
}
```

## How It Works

A background watchdog thread continuously posts a small Runnable to the Main Thread. If the Main Thread is responsive, it executes immediately and resets a counter. If frozen, the counter keeps incrementing until it hits the threshold—then we grab the stack trace.

```
┌─────────────────────┐              ┌─────────────────────┐
│   Watchdog Thread   │    post()    │     Main Thread     │
│                     │ ───────────► │                     │
│  Post ticker        │              │  ticker Runnable:   │
│  Sleep 100ms        │              │    tick = 0         │
│  tick += 100        │              │                     │
│  If tick > 5s:      │              │  (If frozen, ticker │
│    → Report ANR     │              │   never executes)   │
└─────────────────────┘              └─────────────────────┘
```

Based on the proven [ANR-WatchDog](https://github.com/SalomonBrys/ANR-WatchDog) pattern.

## Configuration

| Option | Default | Description |
|--------|---------|-------------|
| `anrThresholdMs` | 5000 | Duration (ms) before reporting as ANR |
| `jankThresholdMs` | 500 | Duration (ms) before reporting as Jank |
| `enableJankDetection` | false | Whether to report jank events |
| `pollingIntervalMs` | 100 | How often to check for freezes |
| `whitelist { }` | null | Predicate to filter specific tasks |
| `listener { }` | null | Callback for freeze events |

## Crash Reporter Integration

`StallwartException` carries the Main Thread's stack trace, making it directly usable with crash reporters:

```kotlin
listener { event ->
    if (event.type == FreezeType.ANR) {
        // Firebase Crashlytics
        Firebase.crashlytics.recordException(StallwartException.from(event))

        // Sentry
        Sentry.captureException(StallwartException.from(event))

        // Bugsnag
        Bugsnag.notify(StallwartException.from(event))
    }
}
```

## FreezeEvent Properties

```kotlin
data class FreezeEvent(
    val type: FreezeType,                    // ANR or JANK
    val durationMs: Long,                    // How long the freeze lasted
    val taskDescription: String,             // Human-readable culprit location
    val mainThreadStackTrace: Array<StackTraceElement>,  // Full stack trace
    val timestamp: Long                      // When freeze was detected
)
```

## Lifecycle Awareness

Stallwart automatically:
- **Pauses** when app goes to background (zero CPU usage)
- **Resumes** when app returns to foreground
- **Ignores** freezes when debugger is attached

For manual control:
```kotlin
Stallwart.pause()   // Pause monitoring
Stallwart.resume()  // Resume monitoring
```

## Best Practices

1. **Initialize early** - Call in `Application.onCreate()` to catch startup freezes
2. **Use crash reporters** - Send ANRs to your crash reporting service
3. **Enable jank in debug** - Catch performance issues during development
4. **Whitelist carefully** - Don't over-whitelist or you'll miss real issues

## Requirements

- **Minimum SDK**: 21 (Android 5.0)
- **AndroidX Lifecycle**: 2.6.0+

## License

```
Copyright 2026 Atri Tripathi

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
