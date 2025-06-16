# PwnEyes v10.6 Release Notes

## Bug Fixes and Improvements

- **Removed "Retry Billing Connection" button from Settings**: The retry functionality now runs automatically in the background, simplifying the user experience.

- **Enhanced billing connection stability**: 
  - Improved automatic reconnection logic with exponential backoff
  - Added periodic reconnection attempts for better long-term recovery
  - Increased maximum retry attempts from 3 to 5 for better reliability

- **Memory management improvements**: Fixed memory leaks in SettingsFragment and MainActivity by properly removing observers when components are destroyed.

- **Added NetworkUtils class**: Provides robust network status monitoring and better error messages for network operations.

- **Crash reporting improvements**: Enhanced error handling and reporting capabilities to help identify and fix issues faster.

## Technical Details

- Version Code: 15
- Version Name: 10.6
- Required Android Version: 6.0 or higher
