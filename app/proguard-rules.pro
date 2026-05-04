# Room - keep entity/dao classes
-keep class com.antbear.pwneyes.data.** { *; }

# Keep Kotlin data classes used with Room
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
