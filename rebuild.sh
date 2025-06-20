#!/bin/bash

echo "==== Starting complete rebuild of PwnEyes app ===="

# Step 1: Clean everything
echo "Cleaning project..."
./gradlew clean

# Step 2: Remove any potentially conflicting AdsManager files
echo "Removing conflicting AdsManager.kt files..."
find app/src -name "AdsManager.kt" -type f -exec rm -f {} \;

# Step 3: Make sure we have the right directory structure
echo "Verifying directory structure..."
mkdir -p app/src/main/java/com/antbear/pwneyes/util
mkdir -p app/src/free/java/com/antbear/pwneyes/util
mkdir -p app/src/paid/java/com/antbear/pwneyes/util

# Step 4: Verify our renamed files are in place
echo "Verifying AdsManager implementations..."
if [ ! -f "app/src/free/java/com/antbear/pwneyes/util/FreeAdsManager.kt" ]; then
  echo "ERROR: FreeAdsManager.kt is missing!"
  exit 1
fi

if [ ! -f "app/src/paid/java/com/antbear/pwneyes/util/PaidAdsManager.kt" ]; then
  echo "ERROR: PaidAdsManager.kt is missing!"
  exit 1
fi

if [ ! -f "app/src/main/java/com/antbear/pwneyes/util/AdsManagerBase.kt" ]; then
  echo "ERROR: AdsManagerBase.kt is missing!"
  exit 1
fi

# Step 5: Build the debug variants first
echo "Building debug variants..."
./gradlew assembleFreeDebug --rerun-tasks

if [ $? -ne 0 ]; then
  echo "ERROR: FreeDebug build failed!"
  exit 1
fi

echo "FreeDebug variant built successfully"

# Step 6: Build the release variants
echo "Building release bundle..."
./gradlew bundleFreeRelease --rerun-tasks

if [ $? -ne 0 ]; then
  echo "ERROR: FreeRelease bundle failed!"
  exit 1
fi

echo "FreeRelease bundle built successfully"

# Step 7: Copy release bundles to the release directories
echo "Copying release bundles..."
mkdir -p app/free/release
cp -v app/build/outputs/bundle/freeRelease/app-free-release.aab app/free/release/

echo "==== Rebuild completed successfully! ===="
