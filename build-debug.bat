@echo off
REM Script to build a debug version of the app without signing
REM This bypasses the keystore password issue

echo Building debug version of PwnEyes...
echo This will create an APK that can be installed on a device for testing

REM Navigate to the project directory (in case the script is run from elsewhere)
cd %~dp0

REM Clean the project first
call gradlew clean

REM Build the debug APK for the free flavor
call gradlew assembleFreeDebug

echo.
echo Debug build completed!
echo Look for the APK file in app\build\outputs\apk\free\debug\ directory
echo.

REM List the APK files
echo APK files available:
dir /s /b app\build\outputs\apk\free\debug\*.apk

echo.
echo To install on a connected device, run:
echo adb install -r [path-to-apk]

pause
