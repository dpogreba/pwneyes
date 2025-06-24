# PwnEyes Troubleshooting Guide

This guide provides solutions for common issues that might arise after the Kotlin 2.0.0 downgrade and AGP 8.3.0 upgrade.

## Build Issues

### Gradle Sync Fails

**Symptoms:** 
- Android Studio shows "Gradle sync failed" error
- Build tab shows sync errors

**Solutions:**
1. **Check Gradle version compatibility:**
   ```
   ./gradlew --version
   ```
   Ensure it shows Gradle 8.11.1 or compatible

2. **Clear Gradle caches:**
   ```
   ./gradlew cleanBuildCache
   ```
   Or delete `.gradle` folder in your home directory (backup first)

3. **Check for stale metadata:**
   - Delete `.idea` folder and reimport project
   - Run `File > Invalidate Caches and Restart` in Android Studio

### KAPT Processing Errors

**Symptoms:**
- Errors mentioning "KAPT" or "annotation processing"
- Room database errors during compilation

**Solutions:**
1. **Check KAPT configuration:**
   Ensure `app/build.gradle.kts` has proper KAPT setup:
   ```kotlin
   kapt {
       correctErrorTypes = true
       arguments {
           arg("kapt.kotlin.generated", layout.buildDirectory.dir("generated/kapt/src").get().toString())
           arg("kotlinx.metadata.jvm.version", "0.7.0")
           arg("room.schemaLocation", layout.buildDirectory.dir("schemas").get().toString())
           arg("room.incremental", "true")
           arg("kapt.use.k2", "true")
       }
       useBuildCache = true
   }
   ```

2. **Clean and rebuild:**
   ```
   ./gradlew clean kaptFreeDebugKotlin --refresh-dependencies
   ```

3. **Check for Room version conflicts:**
   Ensure Room version (2.6.1) is consistent across all Room dependencies

### Kotlin Version Conflicts

**Symptoms:**
- Errors about "unresolved reference" in Kotlin files
- Complaints about version mismatches

**Solutions:**
1. **Check dependency versions:**
   ```
   ./gradlew dependencyInsight --dependency org.jetbrains.kotlin:kotlin-stdlib
   ```
   Look for any modules forcing higher Kotlin versions

2. **Enforce consistent Kotlin version:**
   Add to `build.gradle.kts`:
   ```kotlin
   plugins.withType<org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper> {
       kotlinPluginVersion = "2.0.0"
   }
   ```

3. **Update import statements:**
   - Look for imports using Kotlin 2.1-specific features
   - Rewrite code using only Kotlin 2.0 features

## Runtime Issues

### App Crashes on Startup

**Symptoms:**
- App installs but crashes immediately
- LogCat shows runtime exceptions

**Solutions:**
1. **Check Room initialization:**
   - Ensure database creation code doesn't use Kotlin 2.1 features
   - Verify migrations work correctly

2. **Look for reflection issues:**
   - Search codebase for `KClass` usage and check for compatibility
   - Consider removing direct Kotlin reflection if problematic

3. **Check for missing coroutine dispatchers:**
   - Ensure proper dispatcher setup in viewModels
   - Verify proper coroutine scope usage

### UI Rendering Problems

**Symptoms:**
- Blank screens or UI elements
- ViewBinding errors

**Solutions:**
1. **Verify view binding generation:**
   ```
   ./gradlew cleanBuildCache assembleFreeDebug --refresh-dependencies
   ```

2. **Check layout XML files:**
   - Ensure XML files don't use any features incompatible with AGP 8.3.0
   - Verify data binding expressions are correct

3. **Monitor LogCat:**
   - Look for view inflation errors
   - Check for view binding instantiation issues

### Coroutine Issues

**Symptoms:**
- Unhandled exceptions in coroutines
- Operations not completing

**Solutions:**
1. **Check coroutine scope handling:**
   - Ensure proper lifecycle scopes are used
   - Verify cancellation is handled correctly

2. **Update flow operators:**
   - Look for flow operators that changed between Kotlin versions
   - Replace any deprecated operators

3. **Add better exception handling:**
   ```kotlin
   viewModelScope.launch {
       try {
           // Your suspended code
       } catch (e: Exception) {
           Log.e("TAG", "Error in coroutine", e)
       }
   }
   ```

## Android Studio Issues

### Project Structure Problems

**Symptoms:**
- Missing sources in project view
- Unresolved references despite successful builds

**Solutions:**
1. **Re-import project:**
   `File > Close Project` then reopen

2. **Rebuild project indices:**
   `File > Invalidate Caches / Restart...`

3. **Check module setup:**
   - Verify module dependencies
   - Check source set configuration

### AGP 8.3.0 Specific Issues

**Symptoms:**
- New build warnings
- Changes in resource handling

**Solutions:**
1. **Update R8/ProGuard rules:**
   - Check if R8 rules need updating
   - Verify proguard-rules.pro is compatible

2. **Check AndroidManifest merging:**
   - Run manifest merger report
   - Look for conflicts in merged manifest

3. **Update resource handling:**
   - Check for deprecated resource references
   - Update any drawable loading code

## Getting Advanced Help

If issues persist:

1. **Generate detailed build scan:**
   ```
   ./gradlew assembleFreeDebug --scan
   ```
   Share the resulting URL

2. **Collect logs:**
   - Android Studio logs (`Help > Show Log in Explorer/Finder`)
   - Gradle logs (use --info or --debug)
   - Device logs (from logcat)

3. **Create minimal reproduction:**
   - Isolate the problematic module or feature
   - Create a minimal test case demonstrating the issue
