# PwnEyes v10.34 Release Notes

## "See What's New!" Feature

### New User Experience
- Added "See What's New!" button in the navigation drawer that appears after app updates
- Implemented an automatic version tracking system to detect when the app has been updated
- Created a release notes dialog system that displays changes in the current version
- Button automatically disappears after the user views the changes

### Technical Improvements
- Created a robust VersionManager to track app version changes
- Implemented ReleaseNotesManager to display formatted release notes
- Added proper integration with the navigation system
- Enhanced error handling and logging throughout the codebase
- Improved UI organization with separate menu groups in the navigation drawer

This update provides a clean way for users to discover new features after updating the app. When a user updates PwnEyes to a newer version, they'll see a "🎉 See What's New!" option at the bottom of the navigation drawer. Tapping this option shows a dialog with all the latest changes and improvements. After viewing the changes, the menu item disappears until the next update.
