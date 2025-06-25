# PwnEyes v10.33 Release Notes

## Hybrid Native/WebView UI for Plugins Screen

### Architecture Enhancement
- Implemented a hybrid approach with both native UI elements and a WebView in the Plugins screen
- Created a native UI header section with status indicator and control buttons
- WebView is now embedded within the native UI layout, not replacing it
- Maintained the separate activity approach for clean navigation

### Native UI Improvements
- Added "Available Plugins" header that shows the connection name
- Added status text indicator for user feedback
- Added functional Refresh button that reloads the WebView content
- Added Manage button for future plugin management functionality
- Included a visual divider between native controls and WebView content

### WebView Integration
- WebView is now positioned below the native UI elements
- WebView still loads the full plugins page as before
- Added proper Handler implementation for UI updates
- Improved error handling and user feedback

### Technical Improvements
- Properly integrated android.os.Handler and Looper for UI updates
- Enhanced layout constraints for proper view positioning
- Added user feedback when interacting with native controls
- Maintained consistent styling across the application

This update provides the best of both worlds - a native UI experience with access to the web-based plugins content. The app now properly matches the design diagram, with the Home screen's WebView detecting plugins URLs and launching a separate screen that contains both native UI elements and a WebView specific to the plugins content.
