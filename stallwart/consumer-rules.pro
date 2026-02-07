# Stallwart ProGuard/R8 consumer rules
# These rules are automatically applied to apps that use Stallwart

# Keep the public API
-keep class com.atritripathi.stallwart.Stallwart { *; }
-keep class com.atritripathi.stallwart.StallwartConfig { *; }
-keep class com.atritripathi.stallwart.StallwartConfig$Builder { *; }

# Keep callback interfaces
-keep interface com.atritripathi.stallwart.core.StallwartListener { *; }
-keep class com.atritripathi.stallwart.core.FreezeEvent { *; }
-keep class com.atritripathi.stallwart.core.FreezeType { *; }

# Keep exceptions for proper stack traces
-keep class com.atritripathi.stallwart.exception.** { *; }
