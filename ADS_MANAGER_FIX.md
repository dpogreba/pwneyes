# AdsManager Fix for PwnEyes

This document provides instructions to fix the "Redeclaration: AdsManager" errors in the PwnEyes project.

## The Problem

The build is failing with the following errors:
- `Redeclaration: AdsManager` 
- `Cannot access '<init>': it is private in 'AdsManager'`
- `Cannot access 'initializeMobileAds': it is private in 'AdsManager'`

This happens because there are multiple `AdsManager.kt` files in different source sets (main, free, paid) that conflict with each other.

## The Solution

We've restructured the ads management to use:
- `AdsManagerBase.kt` - Interface in the main source set
- `FreeAdsManager.kt` - Implementation for the free flavor
- `PaidAdsManager.kt` - Implementation for the paid flavor

## Fix Instructions for Windows

Follow these steps to fix the issue on your Windows system:

1. **Clean the project**
   ```
   gradlew clean --refresh-dependencies
   ```

2. **Delete any build directories**
   ```
   rd /s /q build
   rd /s /q app\build
   rd /s /q .gradle
   ```

3. **Delete all AdsManager.kt files**
   - Delete `app\src\free\java\com\antbear\pwneyes\util\AdsManager.kt`
   - Delete `app\src\paid\java\com\antbear\pwneyes\util\AdsManager.kt`
   - Delete `app\src\main\java\com\antbear\pwneyes\util\AdsManager.kt`
   
   You can use this command in PowerShell:
   ```powershell
   Get-ChildItem -Path app\src -Recurse -Filter "AdsManager.kt" | Remove-Item -Force
   ```

4. **Verify our replacement files exist**
   - `app\src\free\java\com\antbear\pwneyes\util\FreeAdsManager.kt`
   - `app\src\paid\java\com\antbear\pwneyes\util\PaidAdsManager.kt`
   - `app\src\main\java\com\antbear\pwneyes\util\AdsManagerBase.kt`

5. **Create an empty .gitignore file in each util directory**
   ```
   echo "" > app\src\free\java\com\antbear\pwneyes\util\.gitignore
   echo "" > app\src\paid\java\com\antbear\pwneyes\util\.gitignore
   echo "" > app\src\main\java\com\antbear\pwneyes\util\.gitignore
   ```

6. **Commit these changes**
   ```
   git add app\src\free\java\com\antbear\pwneyes\util\FreeAdsManager.kt
   git add app\src\paid\java\com\antbear\pwneyes\util\PaidAdsManager.kt
   git add app\src\main\java\com\antbear\pwneyes\util\AdsManagerBase.kt
   git add app\src\free\java\com\antbear\pwneyes\util\.gitignore
   git add app\src\paid\java\com\antbear\pwneyes\util\.gitignore
   git add app\src\main\java\com\antbear\pwneyes\util\.gitignore
   git commit -m "Final fix: Ensure only FreeAdsManager, PaidAdsManager, and AdsManagerBase exist"
   ```

7. **Run a test build**
   ```
   gradlew assembleFreeDebug --rerun-tasks
   ```

8. **Push changes to GitHub**
   ```
   git push origin master
   ```

## Fix Instructions for Mac/Linux

On Mac or Linux, you can run the `final_fix.sh` script:

```bash
chmod +x final_fix.sh
./final_fix.sh
```

## Verification

The build should complete successfully without any AdsManager redeclaration errors. Both free and paid variants should build correctly.
