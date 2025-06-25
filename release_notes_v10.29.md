# PwnEyes v10.29 Release Notes

## Native UI Implementation

### New Native Plugins Screen
- Implemented a fully native Android UI for the Plugins screen
- Replaced WebView-based interface with native Android components
- Added direct state management of plugin switches in the UI
- Ensured plugin state persistence across configuration changes
- Created intuitive card-based layout for easy plugin management

### Enhanced Navigation System
- Added special case handling for Plugins tab in navigation flow
- Created seamless transition between connection view and plugins screen
- Implemented fallback to WebView when native UI navigation fails
- Preserved connection context (name, URL) throughout navigation

### User Experience Improvements
- Added visual feedback when enabling/disabling plugins
- Implemented more responsive UI with native Android controls
- Maintained tab navigation bar for consistency across views
- Provided clear back navigation to connection screen
- Enhanced connection name display in toolbar title

### Technical Implementation
- Created dedicated PluginsFragment and layout resources
- Extended ViewerViewModel to handle plugin state persistence
- Updated navigation graph to include the new fragment
- Implemented proper argument passing between fragments
- Maintained backward compatibility with WebView-based tabs

This update provides a smoother, more native experience when managing plugins while ensuring compatibility with existing functionality. The native UI implementation offers better performance and a more integrated feel compared to the previous WebView-based approach.
