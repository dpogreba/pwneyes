#!/bin/bash
# Script to build a debug version of the app without signing
# This bypasses the keystore password issue

echo "Building debug version of PwnEyes..."
echo "This will create an APK that can be installed on a device for testing"

# Navigate to the project directory (in case the script is run from elsewhere)
cd "$(dirname "$0")"

# Clean the project first
./gradlew clean

# Build the debug APK (no flavor specified since we removed flavors)
./gradlew assembleDebug

echo ""
echo "Debug build completed!"
echo "Look for the APK file in app/build/outputs/apk/debug/ directory"
echo ""

# List the APK files
echo "APK files available:"
find app/build/outputs/apk -name "*.apk" | grep -v "unsigned"

echo ""
echo "To install on a connected device, run:"
echo "adb install -r [path-to-apk]"
