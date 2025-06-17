# PwnEyes v10.11 Release Notes

## WebView Orientation & Scrolling Improvements

- **Fixed Orientation Change Issues**: Solved the problem where changing phone orientation would reset the WebView:
  - Implemented WebView state saving and restoration
  - Added configuration in AndroidManifest.xml to handle orientation changes properly
  - Preserved scroll position when rotating the device
  - Maintained current page/location instead of resetting to the home page

- **Enhanced Scrolling for All Content Areas**:
  - Targeted specific scrollable elements that weren't working previously
  - Implemented advanced JavaScript to ensure nested scrollable areas work properly
  - Added support for all types of scrollable containers
  - Fixed issues with the content area highlighted in orange that wasn't scrolling properly

- **Layout and Performance Improvements**:
  - Added hardware acceleration for smoother scrolling
  - Enabled nested scrolling support
  - Made scrollbars always visible when needed
  - Improved touch responsiveness in scrollable areas

## Technical Details

- Version Code: 20
- Version Name: 10.11
- Required Android Version: 6.0 or higher
