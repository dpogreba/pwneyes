#!/bin/bash

echo "Running comprehensive AdsManager fix..."

# Step 1: Clean all build artifacts
echo "Cleaning build artifacts..."
./gradlew clean --refresh-dependencies
rm -rf build
rm -rf app/build
rm -rf .gradle

# Step 2: Remove any AdsManager.kt files
echo "Removing all AdsManager.kt files..."
find app/src -name "AdsManager.kt" -type f -exec rm -f {} \;

# Step 3: Verify our replacements exist
echo "Verifying replacement files exist..."
if [ ! -f "app/src/free/java/com/antbear/pwneyes/util/FreeAdsManager.kt" ]; then
  echo "ERROR: FreeAdsManager.kt is missing!"
  exit 1
else
  echo "✓ FreeAdsManager.kt exists"
fi

if [ ! -f "app/src/paid/java/com/antbear/pwneyes/util/PaidAdsManager.kt" ]; then
  echo "ERROR: PaidAdsManager.kt is missing!"
  exit 1
else
  echo "✓ PaidAdsManager.kt exists"
fi

if [ ! -f "app/src/main/java/com/antbear/pwneyes/util/AdsManagerBase.kt" ]; then
  echo "ERROR: AdsManagerBase.kt is missing!"
  exit 1
else
  echo "✓ AdsManagerBase.kt exists"
fi

# Step 4: Create an empty .gitignore file in each util directory to track it
# This ensures git will manage these directories
touch app/src/free/java/com/antbear/pwneyes/util/.gitignore
touch app/src/paid/java/com/antbear/pwneyes/util/.gitignore
touch app/src/main/java/com/antbear/pwneyes/util/.gitignore

# Step 5: Commit these changes to ensure the clean state
echo "Committing changes to the repository..."
git add app/src/free/java/com/antbear/pwneyes/util/FreeAdsManager.kt
git add app/src/paid/java/com/antbear/pwneyes/util/PaidAdsManager.kt
git add app/src/main/java/com/antbear/pwneyes/util/AdsManagerBase.kt
git add app/src/free/java/com/antbear/pwneyes/util/.gitignore
git add app/src/paid/java/com/antbear/pwneyes/util/.gitignore
git add app/src/main/java/com/antbear/pwneyes/util/.gitignore
git commit -m "Final fix: Ensure only FreeAdsManager, PaidAdsManager, and AdsManagerBase exist"

echo "Running a test build..."
./gradlew assembleFreeDebug --rerun-tasks

echo "Fix complete! If the build succeeded, please push these changes to your repository."
