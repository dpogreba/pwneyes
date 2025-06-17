# PwnEyes v10.8 Release Notes

## WebView Performance and Scrolling Improvements

- **Fixed Scrolling Issues**: Resolved problems with content scrolling in WebView interfaces by:
  - Injecting JavaScript to ensure proper content sizing and overflow settings
  - Adding explicit scrolling attributes to both WebView code and layout XML
  - Implementing proper layout parameters to ensure scrollable content

- **Improved Loading Performance**: Enhanced WebView loading speed by:
  - Enabling content caching (previously disabled)
  - Using default cache mode instead of no-cache mode
  - Implementing AppCache for web content

- **Layout Enhancements**:
  - Added explicit scrollbar visibility in the layout
  - Set focusable attributes to improve touch interaction
  - Enabled smoother layout animations

- **Enhanced JavaScript Support**:
  - Added console logging for better debugging
  - Implemented content dimension detection for proper scrolling
  - Added delayed resize event handling to ensure UI updates properly

## Technical Details

- Version Code: 17
- Version Name: 10.8
- Required Android Version: 6.0 or higher
