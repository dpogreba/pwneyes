@echo off
REM Script to build an unsigned release version of the app
REM This bypasses the keystore password issue

echo Building unsigned release version of PwnEyes...
echo This will create an APK that can be installed on a device for testing

REM Clean the project first
call gradlew clean

REM Build the release APK without signing
call gradlew assembleRelease -x signReleaseBundle -x signReleaseApk

echo.
echo Unsigned release build completed!
echo Look for the APK file in app\build\outputs\apk\release\ directory
echo.

REM List the APK files (Windows version)
echo APK files available:
dir /s /b app\build\outputs\apk\*.apk | findstr /v "signed"

echo.
echo To install on a connected device, run:
echo adb install -r [path-to-apk]

pause
