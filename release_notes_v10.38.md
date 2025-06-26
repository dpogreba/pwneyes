# PwnEyes v10.38 Release Notes

## Bluetooth Tethering Check Feature

In this release, we've added a feature to check if Bluetooth tethering is enabled when the app starts. This helps ensure that the app can properly connect to devices using Bluetooth tethering.

### New Features

- **Bluetooth Tethering Check**: Automatically checks if Bluetooth tethering is enabled when the app starts
- **User-Friendly Dialog**: Shows a helpful dialog when Bluetooth tethering is disabled, explaining why it's needed
- **Quick Access to Settings**: Provides a direct button to open the device's Bluetooth tethering settings
- **User Preference**: Option to disable the check in the Settings screen under "Connection Settings"

### Technical Implementation

- Added detection of Bluetooth tethering status using multiple approaches for compatibility across devices
- Integrated seamlessly with app startup flow
- Added proper error handling and fallbacks for device compatibility
- Ensured backward compatibility with older Android versions
- Implemented memory-efficient dialog management

### User Experience Improvements

- Non-intrusive notification that only appears when needed
- "Don't show again" option directly in the dialog
- Preserves user preferences between app sessions
- Quick resolution path via Settings button

This feature helps ensure that users have a smooth connection experience with their devices and provides guidance when settings adjustments are needed.
