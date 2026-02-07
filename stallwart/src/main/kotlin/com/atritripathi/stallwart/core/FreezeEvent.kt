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
 * Represents a detected UI freeze event.
 *
 * @property type The severity classification ([FreezeType.ANR] or [FreezeType.JANK])
 * @property durationMs How long the Main Thread was blocked (in milliseconds)
 * @property taskDescription Human-readable location of the blocking code
 * @property mainThreadStackTrace Full stack trace showing what was blocking the Main Thread
 * @property timestamp When the freeze was detected (System.currentTimeMillis)
 */
public data class FreezeEvent(
    public val type: FreezeType,
    public val durationMs: Long,
    public val taskDescription: String,
    public val mainThreadStackTrace: Array<StackTraceElement>,
    public val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Returns a formatted, human-readable representation of the main thread stack trace.
     */
    public fun formatStackTrace(): String = buildString {
        appendLine("Main Thread Stack Trace:")
        mainThreadStackTrace.forEach { element ->
            appendLine("    at $element")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FreezeEvent

        if (type != other.type) return false
        if (durationMs != other.durationMs) return false
        if (taskDescription != other.taskDescription) return false
        if (!mainThreadStackTrace.contentEquals(other.mainThreadStackTrace)) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + durationMs.hashCode()
        result = 31 * result + taskDescription.hashCode()
        result = 31 * result + mainThreadStackTrace.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }

    override fun toString(): String = buildString {
        appendLine("FreezeEvent(")
        appendLine("  type=$type,")
        appendLine("  durationMs=$durationMs,")
        appendLine("  task=$taskDescription,")
        appendLine("  timestamp=$timestamp")
        appendLine(")")
    }
}
