# PwnEyes Testing Checklist

## Overview
This checklist covers the key functionality that should be tested in Android Studio after the Kotlin 2.0.0 and AGP 8.3.0 updates. Focus on verifying that all essential features work correctly.

## Build Verification
- [ ] Project successfully syncs in Android Studio
- [ ] Gradle build completes without errors
- [ ] APK can be generated for debug variant
- [ ] APK can be installed on emulator/device

## Core Functionality

### Application Startup
- [ ] App launches without crashes
- [ ] Splash screen displays correctly
- [ ] Main activity loads properly
- [ ] Navigation drawer works

### Connection Management
- [ ] Can create new connections
- [ ] Connection list displays correctly
- [ ] Can edit existing connections
- [ ] Can delete connections
- [ ] Connection status indicators work properly

### Web Viewing
- [ ] Web content loads in the viewer
- [ ] Navigation controls work (back, forward, reload)
- [ ] Touch events work correctly
- [ ] Scrolling functions properly
- [ ] Zoom controls work

### Tab Management
- [ ] Can create new tabs
- [ ] Can switch between tabs
- [ ] Can close tabs
- [ ] Tab state is preserved when switching

### Settings
- [ ] Settings screen loads correctly
- [ ] All preference options are accessible
- [ ] Changes to settings are saved
- [ ] Settings changes are applied correctly

### Flavor-Specific Features
- [ ] Free version shows ads correctly (if applicable)
- [ ] Paid version does not show ads (if applicable)
- [ ] In-app purchases work correctly (if applicable)

## Runtime Verification
- [ ] Check logcat for unexpected warnings or errors
- [ ] Monitor memory usage for any leaks
- [ ] Verify background processes work correctly
- [ ] Check for ANRs (Application Not Responding) events

## Database Functionality
- [ ] Data persistence works between app restarts
- [ ] Room database operations execute correctly
- [ ] Database migrations work (if applicable)

## Areas to Watch Closely
These areas might be particularly sensitive to the Kotlin version downgrade:

1. **KAPT-Generated Code**
   - Room database implementations
   - Any other annotation processors

2. **Coroutine Usage**
   - Scope management
   - Exception handling
   - Flow implementations

3. **Kotlin Features Compatibility**
   - Any usage of experimental or Kotlin 2.1-specific features
   - Extension functions
   - Higher-order functions

4. **Third-Party Libraries**
   - Libraries that might depend on Kotlin 2.1 features
   - Libraries with Room or other annotation processor dependencies

## Performance Testing
- [ ] UI responsiveness
- [ ] Startup time
- [ ] Memory usage
- [ ] Battery consumption

## Device Testing (if possible)
- [ ] Test on at least one physical device
- [ ] Test on different screen sizes (phone and tablet layouts)
- [ ] Test with different Android versions

## Final Verification
- [ ] All critical features work correctly
- [ ] No regression in existing functionality
- [ ] Application stability is maintained
