# PwnEyes v10.23 - Enhanced Tab Navigation

## New Feature: Multi-Tab Navigation

This update introduces a more intuitive and visually appealing way to access and view various interface tabs for each connected device.

### Key Improvements

1. **Enhanced Navigation Bar** 
   - Added a persistent top navigation bar with device name and multiple tab options
   - Added horizontal scrolling tab bar for easy access to all tabs
   - Tabs are always visible and accessible from any device connection

2. **Dedicated Tab Views**
   - Created separate full-screen views for all interface tabs:
     - Inbox/New: `<IP ADDRESS:8080>/inbox/new`
     - Profile: `<IP ADDRESS:8080>/inbox/profile`
     - Peers: `<IP ADDRESS:8080>/inbox/peers`
     - Plugins: `<IP ADDRESS:8080>/plugins`
   - Added clear visual indication of the current page with URL display

3. **Improved User Experience**
   - Enhanced visual transition between screens with slide animations
   - Added a prominent back button for easy navigation
   - Ensured consistent color scheme and styling across all views
   - Better error handling and URL formatting

4. **Technical Enhancements**
   - Automatic port detection and URL formatting
   - Improved WebView rendering for plugins interface
   - Better state preservation during navigation
   - Consistent toolbar implementation across all fragments

### User Benefits

- Easier access to all interface tabs for connected devices
- Consistent navigation experience across the application
- Better visual indication of current location
- Ability to quickly switch between different interface sections
- Color-coded tabs for easier identification

This update maintains compatibility with all existing connections and requires no user configuration changes.
