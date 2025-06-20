#!/bin/bash

echo "Cleaning up AdsManager files..."

# Remove any stray AdsManager.kt files
find app/src -name "AdsManager.kt" -type f -exec rm -f {} \;

# Make sure our renamed files are in place
if [ ! -f "app/src/free/java/com/antbear/pwneyes/util/FreeAdsManager.kt" ]; then
  echo "FreeAdsManager.kt is missing!"
fi

if [ ! -f "app/src/paid/java/com/antbear/pwneyes/util/PaidAdsManager.kt" ]; then
  echo "PaidAdsManager.kt is missing!"
fi

echo "Cleanup complete!"
