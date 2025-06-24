# PwnEyes v10.25 - Build System Update

## Technical Improvement: Android Gradle Plugin Upgrade and Kotlin Compatibility Fixes

This update brings PwnEyes up to date with the latest Android Gradle Plugin (AGP) version 8.3.0, providing several improvements to the build system. It also ensures proper compatibility between Kotlin and annotation processing tools.

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
   - Optimized for Kotlin 2.0 compatibility
   - More reliable Kotlin DSL support in build scripts
   - Improved Kotlin compiler integration
   - Fixed KAPT compatibility issues with Room

5. **Fixed Build Issues**
   - Resolved Room compiler errors with Kotlin metadata
   - Configured kotlinx-metadata-jvm version 0.7.0 for improved compatibility
   - Added proper KAPT configuration for annotation processing
   - Adjusted experimental property settings to reduce warnings

### Developer Benefits

- **Faster Development Cycle**: Reduced build times mean quicker iteration
- **Lower Resource Usage**: Better memory management during builds
- **Future-Proofing**: Readiness for upcoming Android features and SDK versions
- **Fewer Build Warnings**: Resolved several deprecated API usages

This update is purely infrastructure-focused and has no user-facing changes. It ensures optimal build system compatibility with current Kotlin and Android Gradle Plugin versions.
