# PwnEyes Android App

## Build Instructions

### Solving Keystore Password Issues

If you're experiencing the following error:

```
com.android.ide.common.signing.KeytoolException: Failed to read key pwneyes from store "C:\Users\derek\AndroidStudioProjects\pwneyes\app\pwneyes.keystore.jks": keystore password was incorrect
Cause: failed to decrypt safe contents entry: javax.crypto.BadPaddingException: Given final block not properly padded
```

This is caused by a keystore password issue when trying to sign release builds. The app's build configuration has been updated to make signing optional for development builds. You can now build and test the app without needing the correct keystore passwords.

### Building Debug Versions

#### For Windows:
1. Double-click `build-debug.bat` to build a debug version of the app
2. The script will create an unsigned debug APK that you can install on your device

#### For macOS/Linux:
1. Make the script executable: `chmod +x build-debug.sh`
2. Run the script: `./build-debug.sh`
3. The script will create an unsigned debug APK that you can install on your device

### Manually Building

To manually build a debug version:

1. Open the project in Android Studio
2. Select the "debug" build variant
3. Select "Build" > "Build Bundle(s) / APK(s)" > "Build APK(s)"

### Setting Up Signing (Optional)

If you want to properly sign release builds, you need to:

1. Create a `local.properties` file in the project root (if it doesn't exist already)
2. Add the following lines with your keystore passwords:
   ```
   KEYSTORE_PASSWORD=your_keystore_password
   KEY_PASSWORD=your_key_password
   ```
3. Make sure the keystore file (`pwneyes.keystore.jks`) is correctly located in the app folder

## Project Structure

The app is organized into the following key directories:

- `app/src/main/java/com/antbear/pwneyes/`: Main application code
- `app/src/main/res/`: Resources, layouts, and UI elements
- `app/src/free/`: Code specific to the free version of the app
- `app/src/paid/`: Code specific to the paid version of the app

## Features

- Connection management for pwnagotchi devices
- WebView-based interface for interacting with pwnagotchi web UI
- Support for JavaScript dialogs and popups from the web interface
- In-app purchase functionality to remove ads
