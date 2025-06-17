# PwnEyes v10.16 Release Notes

## URL Port Handling Fix

- **Fixed URL Port Handling**:
  - Resolved issue where port numbers (e.g., `:8080`) had to be explicitly included in URLs
  - Implemented proper URL parsing to preserve port information when adding authentication credentials
  - URLs now work correctly with or without explicit port numbers
  - Connection strings like `http://device.local:8080` and `http://device.local` both work as expected

- **Improved URL Parsing**:
  - Added robust URL component parsing using java.net.URL
  - Properly preserves protocol, host, port, path, query parameters, and fragments
  - Full URL reconstruction maintains all original components
  - Added fallback to previous method if URL parsing fails

- **Enhanced Logging**:
  - Added debug logging for URL processing
  - Makes it easier to diagnose connection issues
  - Improved error handling with appropriate exception messages

## Technical Details

- Version Code: 25
- Version Name: 10.16
- Required Android Version: 6.0 or higher

This update maintains all previous improvements to WebView rendering and the native controls overlay while fixing the URL port handling issue.
