# PwnEyes v10.12 Release Notes

## WebView Visibility & Scrolling Enhancements

- **Fixed Bottom Controls Visibility**: Solved the issue where bottom controls (Shutdown, Reboot, Restart) were not visible:
  - Implemented initial scale adjustment to show more content
  - Added dynamic viewport meta tag injection for proper scaling
  - Improved overall layout visualization to ensure critical controls are visible
  - Added touch gesture handling for easier navigation

- **Enhanced Scrolling for Plugin Tab and Nested Content**:
  - Implemented aggressive touch event handling for all scrollable elements
  - Added specialized selectors to target specific plugin tab content
  - Applied scrolling properties to all potential container elements
  - Implemented custom touch event listeners for manual scrolling control
  - Fixed scrolling issues in deeply nested content areas

- **WebView Optimization**:
  - Added improved touch event handling for better response
  - Enabled proper zoom controls for easier navigation
  - Applied hardware acceleration for smoother performance
  - Prevented parent touch event interference
  - Improved CSS styling for all scrollable containers

## Technical Details

- Version Code: 21
- Version Name: 10.12
- Required Android Version: 6.0 or higher
