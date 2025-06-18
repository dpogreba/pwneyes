#!/bin/bash
# Script to build an unsigned release version of the app
# This bypasses the keystore password issue

echo "Building unsigned release version of PwnEyes..."
echo "This will create an APK that can be installed on a device for testing"

# Navigate to the project directory (in case the script is run from elsewhere)
cd "$(dirname "$0")"

# Clean the project first
./gradlew clean

# Build the release APK without signing - skip the signing completely
./gradlew assembleFreeRelease -PskipSigning=true

echo ""
echo "Unsigned release build completed!"
echo "Look for the APK file in app/build/outputs/apk/free/release/ directory"
echo ""

# List the APK files
echo "APK files available:"
find app/build/outputs/apk -name "*.apk" | grep -v "signed"

echo ""
echo "To install on a connected device, run:"
echo "adb install -r [path-to-apk]"
