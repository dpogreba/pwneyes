# PwnEyes v10.17 Release Notes

## Major Improvements

### 1. Direct Plugins Tab Navigation

Added a direct navigation system to access plugin tabs with a single tap:

- A prominent blue "PLUGINS" button is now available in the top-right corner of the connection view
- The button appears when viewing any active connection
- Single tap navigation eliminates the need to find and click small tab elements

### 2. Dedicated Tab Detail View

Created a completely new tab detail viewing experience:

- Custom fragment specifically designed for viewing tab content
- Improved scrolling behavior using native Android ScrollView
- Clear visual indicators show when you're in tab detail view
- Distinctive UI elements make navigation more obvious
- Easy return to main connection view with back button

### 3. Technical Improvements

- Ensured port 8080 is properly included in URLs when needed
- Added extensive error handling with detailed logging
- Improved rendering of complex tab content
- Better handling of scrollable content in tabs
- Enhanced WebView content sizing for proper display

## Instructions

1. To use the new direct navigation:
   - Connect to any device as normal
   - Look for the blue "PLUGINS" button in the top-right corner
   - Tap the button to go directly to the Plugins tab
   - Use the back button to return to the main view

## Bug Fixes

- Fixed an issue where some tab content wasn't scrollable
- Fixed port handling for connections
- Improved error messaging and recovery
