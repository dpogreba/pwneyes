# PwnEyes v10.27 Release Notes

## Enhanced Tab Navigation Experience

### Improved Visual Interface
- Added connection name to tab detail view header for better context
- Enhanced URL indicator with the connection's IP address and port
- Improved styling of navigation components for better visibility
- Added dynamic title display combining connection name and tab name

### URL Format Improvements
- Each tab now properly displays URL in format: `[CONNECTION_IP]:PORT/[TAB_PATH]`
- Connection base URL is now preserved and used consistently
- Enhanced URL parsing with robust error handling
- Fixed port display in navigation paths

### Navigation Architecture Enhancements
- Completely redesigned navigation between fragments
- Improved back navigation experience
- Added proper parent-child relationship between connection viewer and tab details
- Enhanced tab selection with more reliable tab activation

### Data Persistence
- Connection context is now properly maintained throughout navigation
- Added proper argument passing between fragments
- Ensured connection details are preserved during rotation and configuration changes

## Technical Improvements

- Added connection name and base URL parameters to TabDetailFragment arguments
- Enhanced toolbar configuration for better UI consistency
- Improved error handling during URL parsing
- Fixed visual indicators to clearly show which tab is active

These improvements ensure that users have a clearer understanding of which connection they're viewing and provide better visual distinction between different sections of the application.
