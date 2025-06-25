# PwnEyes v10.26 Release Notes

## Build System Improvements

### Gradle and Kotlin Daemon Enhancements
- Increased memory allocation for Gradle and Kotlin daemon processes for better build stability
- Fixed Kotlin daemon connection issues during compilation
- Removed experimental and deprecated Gradle settings
- Configured Room to handle Kotlin 2.0 compatibility correctly
- Removed unstable compiler options

### Build Performance
- Enhanced memory management during compilation
- Added proper cache configuration for faster builds
- Simplified KAPT arguments to only include necessary options
- Resolved warning about experimental KAPT features

## Codebase Modernization

### Modern Architecture Patterns
- Replaced deprecated `retainInstance = true` with proper ViewModel state management
- Added ViewerViewModel for state persistence in WebView fragments
- Implemented SavedStateHandle for configuration change handling
- Updated both ConnectionViewerFragment and TabDetailFragment to use the new approach

### Fragment Lifecycle Improvements
- Fixed memory leaks by properly cleaning up WebView resources
- Enhanced state restoration between fragment navigation
- Added detailed logging during fragment lifecycle events
- Improved scroll position restoration in TabDetailFragment

## Navigation Fixes

### Tab Navigation Enhancement
- Enhanced the tab navigation mechanism with better error handling
- Added visual indicators to confirm navigation success
- Added fallback navigation approach for older Android devices
- Fixed loading state indicators during tab transitions

## Testing and Reliability

- Added comprehensive logging for better debugging
- Enhanced error handling with specific error messages
- Improved WebView cleanup to prevent memory leaks
- Added visual feedback during all user interactions

These improvements ensure better performance and reliability, particularly on physical devices where the tab navigation was previously problematic.
