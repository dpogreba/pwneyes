# PwnEyes v10.14 Release Notes

## Extreme WebView Fixes for Bottom Controls

- **Drastic Bottom Controls Visibility Enhancements**:
  - Reduced content scale to 50% (extreme zoom out) to ensure all UI elements are visible
  - Added direct DOM element detection for Shutdown/Reboot/MANU controls
  - Implemented forced repositioning of found control elements to fixed positions
  - Added extreme bottom padding (250px) to push all content up
  - Added visual highlights to ensure bottom controls are easily visible
  - Implemented multiple forced zoom outs on page load
  - Added direct content transform with -150px Y-axis offset

- **Complete DOM Manipulation**:
  - Modified document height to 70% viewport height when controls are found
  - Implemented direct body transform with scale(0.9)
  - Added 400px padding to document root
  - Added extreme content preloading with 10,000px scroll depth
  - Implemented direct HTML structure manipulation
  - Added colored border highlighting for control elements

- **Debugging Improvements**:
  - Enhanced console logging with DOM element detection
  - Added data attributes for all modified elements
  - Implemented forced scrolling for better content loading
  - Added direct pixel-level manipulation of content position
  - Added element identification with visual indicators

## Technical Details

- Version Code: 23
- Version Name: 10.14
- Required Android Version: 6.0 or higher
