#!/bin/bash
# Script to stage, commit, and push changes to GitHub

# Set script to exit immediately if a command fails
set -e

echo "=== PwnEyes GitHub Update Script ==="
echo ""
echo "This script will commit and push your changes to GitHub."
echo ""

# Check if git is installed
if ! command -v git &> /dev/null; then
    echo "Error: git is not installed. Please install git first."
    exit 1
fi

# Check if the .git directory exists
if [ ! -d ".git" ]; then
    echo "Error: This directory is not a git repository."
    echo "Please run this script from the root of your PwnEyes project."
    exit 1
fi

# Check current branch
CURRENT_BRANCH=$(git branch --show-current)
echo "Current branch: $CURRENT_BRANCH"
echo ""

# Prompt for confirmation
read -p "Do you want to continue with updating GitHub? (y/n): " CONFIRM
if [[ "$CONFIRM" != "y" && "$CONFIRM" != "Y" ]]; then
    echo "Update cancelled."
    exit 0
fi

echo ""
echo "=== Staging Changes ==="
git add .

echo ""
echo "=== Staged Files ==="
git status --short

echo ""
echo "=== Committing Changes ==="
echo "Enter a commit message that describes your changes:"
read -p "Commit message: " COMMIT_MESSAGE

if [ -z "$COMMIT_MESSAGE" ]; then
    COMMIT_MESSAGE="Improved architecture: Added WebViewManager, NavigationManager, and DI-ready structure

- Enhanced WebView scrolling with improved touch handling and GestureDetector
- Added centralized WebViewManager for better JavaScript injection and WebView configuration
- Created NavigationManager for improved navigation flow
- Refactored code to be more modular and maintainable
- Prepared infrastructure for future Hilt integration
- See release_notes_v10.18.md for complete details"
    
    echo "Using default commit message."
    echo "-----------------------------------"
    echo "$COMMIT_MESSAGE"
    echo "-----------------------------------"
fi

git commit -m "$COMMIT_MESSAGE"

echo ""
echo "=== Pushing to GitHub ==="
echo "This will push your changes to the '$CURRENT_BRANCH' branch on GitHub."
read -p "Continue? (y/n): " PUSH_CONFIRM

if [[ "$PUSH_CONFIRM" != "y" && "$PUSH_CONFIRM" != "Y" ]]; then
    echo "Push cancelled. Your changes are committed locally but not pushed to GitHub."
    exit 0
fi

# Push changes to GitHub
git push origin $CURRENT_BRANCH

echo ""
echo "=== Success ==="
echo "Your changes have been pushed to GitHub successfully."
echo "Branch: $CURRENT_BRANCH"
echo "Commit message: $COMMIT_MESSAGE"
echo ""
echo "You can check your changes at: https://github.com/dpogreba/pwneyes"
