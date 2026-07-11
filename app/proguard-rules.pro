# Keep Room entities' field names (used by generated code / reflection paths).
-keep class com.avtar.cabbilling.data.local.entity.** { *; }

# Room generated implementations.
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# Kotlin metadata / coroutines are handled by the AndroidX consumer rules,
# but keep the standard safety nets for a release build.
-dontwarn org.jetbrains.annotations.**
