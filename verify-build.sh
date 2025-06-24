#!/bin/bash
# Build verification script for PwnEyes

# Define colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}PwnEyes Build Verification Script${NC}"
echo "======================================"
echo "Running checks to verify build integrity..."
echo

# Step 1: Clean the project
echo -e "Step 1: ${YELLOW}Cleaning project...${NC}"
./gradlew clean
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Clean failed!${NC}"
    exit 1
else
    echo -e "${GREEN}✓ Clean successful${NC}"
fi
echo

# Step 2: Compile the project (free debug variant)
echo -e "Step 2: ${YELLOW}Compiling project (free debug variant)...${NC}"
./gradlew assembleFreeDebug
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Compilation failed!${NC}"
    exit 1
else
    echo -e "${GREEN}✓ Compilation successful${NC}"
fi
echo

# Step 3: Check KAPT processing
echo -e "Step 3: ${YELLOW}Verifying KAPT processing...${NC}"
./gradlew kaptFreeDebugKotlin
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ KAPT processing failed!${NC}"
    exit 1
else
    echo -e "${GREEN}✓ KAPT processing successful${NC}"
fi
echo

# Step 4: Check lint
echo -e "Step 4: ${YELLOW}Running lint checks...${NC}"
./gradlew lintFreeDebug
if [ $? -ne 0 ]; then
    echo -e "${YELLOW}⚠️ Lint found issues, but continuing...${NC}"
else
    echo -e "${GREEN}✓ Lint checks passed${NC}"
fi
echo

# Step 5: Try to build a debug APK
echo -e "Step 5: ${YELLOW}Building debug APK...${NC}"
./gradlew assembleFreeDebug
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Debug APK build failed!${NC}"
    exit 1
else
    echo -e "${GREEN}✓ Debug APK built successfully${NC}"
fi
echo

echo -e "${GREEN}✓ All verification steps completed successfully!${NC}"
echo "The project should be ready for testing in Android Studio."
echo "======================================"
echo -e "${YELLOW}Android Studio Testing Recommendations:${NC}"
echo "1. Open the project in Android Studio"
echo "2. Run on an emulator to verify basic functionality"
echo "3. Check for runtime exceptions in logcat"
echo "4. Test key features as outlined in testing-checklist.md"
echo
