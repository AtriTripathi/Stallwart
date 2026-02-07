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
 * Represents the severity classification of a detected UI freeze.
 *
 * Stallwart distinguishes between different levels of UI responsiveness issues,
 * allowing consumers to handle each type appropriately.
 */
public enum class FreezeType {
    /**
     * A minor lag that may be perceptible to users but doesn't trigger
     * Android's ANR dialog. Typically in the 500ms-5s range.
     *
     * Use this for performance monitoring and optimization insights.
     */
    JANK,

    /**
     * A critical freeze that would trigger Android's ANR (Application Not Responding)
     * dialog if the system watchdog were active. Typically 5s or more.
     *
     * These events indicate severe responsiveness issues that must be addressed.
     */
    ANR
}
