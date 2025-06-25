# PwnEyes v10.28 Release Notes

## User Experience Improvements

### Automatic Port Configuration
- Added automatic port 8080 assignment when creating or editing connections
- Users no longer need to manually type ":8080" for each connection
- The application now automatically adds port 8080 if no port is specified
- Pre-existing connections with explicit ports (80, 443, etc.) are preserved as-is

### Technical Implementation
- Enhanced URL processing in AddConnectionFragment
- Added intelligent port insertion that correctly handles different URL formats
- Improved URL parsing to identify the correct location to insert the port
- Implemented proper protocol detection to ensure correct URL structure

## User Interface Feedback
- Added contextual logging to help debug connection issues
- Improved URL handling to maintain consistent format

This update makes the connection process more streamlined, particularly for new users who may not be aware that port 8080 is required for proper communication with devices. The application now handles this technical detail automatically, making the overall experience more user-friendly while maintaining compatibility with connections that use other ports.
