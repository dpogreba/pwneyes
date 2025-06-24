# Android Studio Testing Guide for PwnEyes

This document provides guidance on testing the PwnEyes application in Android Studio after the Kotlin version downgrade and AGP update.

## Quick Start

1. Clone or pull the latest version from GitHub
2. Open the project in Android Studio
3. Run the verification script to ensure build integrity:
   ```
   ./verify-build.sh
   ```
4. Follow the testing checklist to verify functionality

## Included Resources

The following resources have been created to help with testing and troubleshooting:

### 1. Verification Script (`verify-build.sh`)

A shell script that performs a series of checks to verify build integrity:
- Cleans the project
- Compiles the project
- Verifies KAPT processing
- Runs lint checks
- Builds a debug APK

Usage:
```
chmod +x verify-build.sh  # Make executable (if needed)
./verify-build.sh
```

### 2. Testing Checklist (`testing-checklist.md`)

A comprehensive checklist covering all key functionality that should be tested:
- Build verification
- Core functionality testing
- Runtime verification
- Database functionality
- Areas to watch closely
- Performance testing
- Device testing

Use this as a systematic guide when testing the application in Android Studio.

### 3. Troubleshooting Guide (`troubleshooting-guide.md`)

Solutions for common issues that might arise:
- Build issues
- Runtime issues
- Android Studio issues
- AGP 8.3.0 specific issues

Refer to this guide if you encounter problems during testing.

## Key Changes Made

1. **Kotlin Downgrade**
   - Changed Kotlin version from 2.1.0 to 2.0.0
   - Updated all Kotlin dependency references
   - Modified forced dependency versions

2. **KAPT Configuration**
   - Added proper KAPT arguments for Room compatibility
   - Configured kotlinx-metadata-jvm version
   - Set up Room schema location

3. **AGP Settings**
   - Maintained AGP 8.3.0
   - Adjusted experimental properties

## Testing Tips

1. **Clean Project First**
   - Always start with a clean project when testing
   - Use `Build > Clean Project` in Android Studio

2. **Monitor LogCat**
   - Keep LogCat open while testing
   - Filter for errors and warnings

3. **Test Both Flavors**
   - Test the free and paid variants (if applicable)

4. **Verify Room Database**
   - Test database operations thoroughly
   - Look for any migration issues

5. **Check for Kotlin Version-Specific Issues**
   - Pay attention to Kotlin features that might be version-sensitive
   - Look for coroutine or Flow-related issues

## Reporting Issues

If you encounter issues:

1. Check the troubleshooting guide first
2. Collect relevant logs (build logs, logcat)
3. Document the steps to reproduce
4. Note any error messages or unexpected behavior

## Next Steps

After successful testing:
1. Update the GitHub repository with any additional fixes
2. Create a release build
3. Distribute for wider testing
