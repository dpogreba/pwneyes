# PwnEyes v10.15 Release Notes

## Native Controls Overlay and Layout Improvements

- **Native Bottom Controls Added**:
  - Added always-visible native Android buttons for critical controls (Shutdown, Reboot, Restart MANU)
  - Native buttons directly interact with web elements through JavaScript
  - Semi-transparent overlay with red background ensures visibility
  - No more missing controls regardless of web content display issues

- **Improved WebView Layout Structure**:
  - Added 150px bottom padding to WebView content area to push content up
  - Set `clipToPadding=false` to ensure content scrolls properly into padding area
  - Constrained WebView properly in layout for more reliable sizing
  - Visual spacing ensures no critical content is hidden

- **JavaScript Command Injection**:
  - Implemented reliable DOM search for finding web control elements
  - Created intelligent button click handlers that find the right elements
  - Added fallback strategies for when exact button matches aren't found
  - Toast notifications confirm when commands are sent

- **Combined Approach Benefits**:
  - Native controls are always visible regardless of web content layout
  - Original web controls remain accessible if needed
  - No reliance on DOM manipulation to make controls visible
  - Consistent experience across different device sizes and orientations

## Technical Details

- Version Code: 24
- Version Name: 10.15
- Required Android Version: 6.0 or higher
