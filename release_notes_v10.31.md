# PwnEyes v10.31 Release Notes

## WebView URL Monitoring for Navigation

### Enhanced Navigation Detection
- Implemented WebView URL monitoring to detect tab navigation
- Added automatic detection of "/plugins" URLs in WebView navigation
- Now watches for URLs in multiple places:
  1. During WebView navigation (shouldOverrideUrlLoading)
  2. After page loading completes (onPageFinished)
  3. Direct button clicks (unchanged from v10.30)

### Improved Plugins Navigation
- Extracted navigation logic into a dedicated `handlePluginsNavigation()` method
- Added multiple trigger points to ensure the Plugins view is launched regardless of navigation method
- Enhanced error handling with user-visible toast messages
- Implemented consistent URL parsing logic across all navigation paths

### Technical Improvements
- Better URL parsing with java.net.URL for more robust base URL extraction
- Added comprehensive logging for each navigation detection point
- Improved error reporting with more detailed logs and user feedback
- Fixed edge cases in navigation detection

### Architecture Changes
- Maintained the activity-based approach introduced in v10.30
- Added more robust detection layer to ensure navigation intent is always recognized
- Proper error handling to gracefully fall back to WebView when necessary

This update complements v10.30 by ensuring that regardless of how the user navigates to the plugins tab (via button click, URL navigation, or page redirect), the native UI will be displayed. This should resolve cases where the Plugins screen was still showing in the WebView instead of using the native UI.
