#!/bin/bash

# Extract the ZIP file
unzip -o pwneyes-stability-improvements.zip

# Add the new/modified files
git add app/src/main/java/com/antbear/pwneyes/util/CrashReporter.kt
git add app/src/main/res/xml/file_provider_paths.xml  
git add app/src/main/AndroidManifest.xml
git add app/src/main/java/com/antbear/pwneyes/MainActivity.kt
git add app/src/main/java/com/antbear/pwneyes/ui/settings/SettingsFragment.kt
git add app/src/main/java/com/antbear/pwneyes/util/NetworkUtils.kt
git add app/src/main/java/com/antbear/pwneyes/PwnEyesApplication.kt

# Commit the changes
git commit -m "Implement key stability improvements for app launch

1. Added crash reporting system:
   - Implemented CrashReporter class that captures uncaught exceptions
   - Added email-based crash report sending to pwneyes@proton.me
   - Added FileProvider configuration for sharing crash logs

2. Fixed memory leaks:
   - Properly removing observers in Activity/Fragment onDestroy()
   - Cleaning up resources in MainActivity and SettingsFragment

3. Enhanced network reliability:
   - Added NetworkUtils class for robust network status monitoring
   - Improved error handling for network operations
   - Added user-friendly network error messages

4. General improvements:
   - Better error recovery throughout the app
   - More comprehensive exception handling
   - Cleaner resource management"

echo "Changes committed to stability-improvements branch successfully."
echo "To push to GitHub, run: git push -u origin stability-improvements"
