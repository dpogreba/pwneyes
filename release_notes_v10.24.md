# PwnEyes v10.24 - Kotlin 2.1 Update

## Technical Improvement: Kotlin 2.1 Upgrade

This update brings PwnEyes fully in line with Kotlin 2.1, aligning both the compiler and runtime libraries to use the same version.

### Key Improvements

1. **Compiler and Runtime Alignment**
   - Updated Kotlin compiler version from 1.9.22 to 2.1.0
   - Ensured all Kotlin runtime libraries (stdlib, stdlib-jdk8, reflect) use version 2.1.0
   - Added required compiler flags for Kotlin 2.1 compatibility

2. **Enhanced Language Features**
   - Enabled context receivers for more concise code with `-Xcontext-receivers`
   - Added support for expect/actual classes with `-Xexpect-actual-classes`
   - Ensured compatibility with newer dependencies via `-Xallow-unstable-dependencies`

3. **Performance Improvements**
   - Better code generation with the JVM IR backend
   - Improved compilation speed and runtime performance
   - More efficient handling of nullable types and lambdas

4. **Stability Enhancements**
   - Fixed potential mismatch issues between compiler and runtime library versions
   - Addressed warnings related to experimental Kotlin features
   - Ensured proper dependency resolution for Kotlin components

### Developer Benefits

- **Cleaner Code**: Access to more modern Kotlin language features
- **Better Performance**: Improved compilation and runtime efficiency
- **Future Compatibility**: Ready for upcoming Kotlin features and libraries
- **Reduced Warnings**: Fewer build warnings related to Kotlin versioning

This update is entirely under-the-hood and requires no changes to existing code. All existing functionality continues to work as before, but with improved performance and stability.
