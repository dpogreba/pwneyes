# PwnEyes v10.22 Release Notes

## WebView Improvements for Bottom Navigation

### Visibility Enhancements
- **Improved Content Scaling**: Reduced initial scale from 50% to 40% to show more content
- **Changed Transform Origin**: Modified from top-center to center-center for better balance
- **Increased Bottom Padding**: Added more vertical space at bottom (180px instead of 80px)
- **Added Automatic Navigation Bar Detection**: The app now detects off-screen navigation bars and fixes their position

### Navigation Improvements
- **Navigation Bar Detection**: Automatically identifies and makes visible navigation elements that may be off-screen
- **Enhanced Scrolling**: Added automatic scrolling to page bottom when executing commands
- **Command Button Access**: Improved access to bottom-positioned control buttons

### Technical Changes
- **Modified Viewport Settings**: Changed viewport meta tag settings for better content fitting
- **Adjusted Content Scale**: Reduced body transform scale from 0.9 to 0.85 for better visibility
- **Improved Document Padding**: Added additional document padding to ensure bottom elements are visible

## Notes for App Users
This update significantly improves the visibility of bottom navigation bars and controls in connected interfaces. If you previously had issues seeing navigation bars at the bottom of web interfaces, this update should resolve those problems by automatically detecting and repositioning them as needed.
