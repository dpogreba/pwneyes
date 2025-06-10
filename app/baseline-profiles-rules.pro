# Disable baseline profiles
-dontoptimize
-dontpreverify

# Specifically disable baseline profile optimization
-keep class androidx.profileinstaller.** { *; }
-keepclassmembers class androidx.profileinstaller.** { *; }

# Disable profile collection in the application
-keep class * extends android.app.Application {
    <init>();
    void attachBaseContext(android.content.Context);
}
