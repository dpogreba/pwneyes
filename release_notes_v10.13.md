# PwnEyes v10.13 Release Notes

## Aggressive WebView Fixes

- **Dramatic Bottom Controls Visibility Improvements**:
  - Reduced content scale to 75% to ensure all UI elements are visible
  - Added active detection and repositioning of bottom control buttons
  - Implemented forced bottom margins to push controls into view
  - Forced zoom out and viewport adjustments on page load
  - Added dynamic DOM analysis to identify and fix hidden controls

- **Complete Scrolling Overhaul**:
  - Implemented custom touch handling system with direct control over scrolling
  - Added JavaScript-based scrolling for nested content containers
  - Implemented content pre-loading with scroll-to-bottom technique
  - Added direct touch event intercepts for scrollable elements
  - Created hybrid native/JavaScript scrolling solution

- **Debugging and Development Improvements**:
  - Enabled WebView debugging for real-time inspection via Chrome DevTools
  - Added detailed console logging for scroll interactions
  - Implemented DOM analysis tools to identify problematic elements
  - Added tag attributes to modified elements for easier identification
  - Improved touch event handling with native Kotlin implementation

## Technical Details

- Version Code: 22
- Version Name: 10.13
- Required Android Version: 6.0 or higher
