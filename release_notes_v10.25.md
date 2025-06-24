# PwnEyes v10.25 - Build System Update

## Technical Improvement: Android Gradle Plugin Upgrade and Kotlin 2.1 Compatibility Fixes

This update brings PwnEyes up to date with the latest Android Gradle Plugin (AGP) version 8.3.0, providing several improvements to the build system. It also fixes Kotlin 2.1 compatibility issues with annotation processing.

### Key Improvements

1. **Latest Build Tools**
   - Updated Android Gradle Plugin from 8.2.0 to 8.3.0
   - Maintained compatibility with Gradle 8.11.1

2. **Build Performance Improvements**
   - Faster build times, especially for incremental builds
   - Reduced memory consumption during builds
   - Improved parallelization of build tasks

3. **Modern Android Features Support**
   - Better support for modern Android development features
   - Enhanced namespace management
   - Improved handling of Android resources

4. **Better Kotlin Integration**
   - Enhanced compatibility with Kotlin 2.1
   - More reliable Kotlin DSL support in build scripts
   - Improved Kotlin compiler integration
   - Fixed KAPT compatibility issues with Kotlin 2.1

5. **Fixed Build Issues**
   - Resolved Room compiler error with Kotlin 2.1 metadata
   - Updated kotlinx-metadata-jvm to version 0.7.0
   - Added proper KAPT configuration for Kotlin 2.1
   - Adjusted experimental property settings to reduce warnings

### Developer Benefits

- **Faster Development Cycle**: Reduced build times mean quicker iteration
- **Lower Resource Usage**: Better memory management during builds
- **Future-Proofing**: Readiness for upcoming Android features and SDK versions
- **Fewer Build Warnings**: Resolved several deprecated API usages

This update is purely infrastructure-focused and has no user-facing changes. It builds upon the Kotlin 2.1 update (v10.24) to ensure the build system is fully up to date with the latest standards.
