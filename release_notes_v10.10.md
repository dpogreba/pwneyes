# PwnEyes v10.10 Release Notes

## Build System and Compatibility Fixes

- **Fixed Compilation Error**: Removed deprecated `setAppCacheEnabled()` method that was causing build failures:
  - Replaced with modern approach using only the standard cache mode
  - Improved compatibility with latest Android WebView implementation

- **Enhanced WebView Configuration**:
  - Simplified caching implementation to use recommended best practices
  - Maintained all scrolling and content display improvements from v10.8

## Technical Details

- Version Code: 19
- Version Name: 10.10
- Required Android Version: 6.0 or higher
