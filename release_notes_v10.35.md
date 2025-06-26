# PwnEyes v10.35 Release Notes

## Fixed WebView Compilation Error and Deprecation Warnings

In this release, we addressed the following issues:

### WebView Compilation Error Fixed
- Fixed a critical compilation error in WebViewManager.kt that was preventing the app from building
- Removed the invalid `setAllowedNetworkImageAccess` method call that doesn't exist in the Android SDK
- Implemented a safer approach to handling WebView security settings

### Deprecation Warnings Addressed
- Added file-level `@Suppress("DEPRECATION")` annotation to WebViewManager.kt to properly handle all deprecation warnings
- Removed redundant per-line suppression annotations
- Updated comments to clarify usage of deprecated properties and potential alternatives
- Mentioned WebViewAssetLoader as a modern alternative for secure file access in newer Android versions

### Additional Improvements
- Fixed NetworkUtils.kt deprecation warnings with proper annotations
- Fixed BillingManager.kt null check and suppressed SERVICE_TIMEOUT deprecation

These changes improve code quality and eliminate build errors while maintaining backward compatibility with older Android versions.
