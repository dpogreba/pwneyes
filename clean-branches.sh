#!/bin/bash

echo "==== Branch Cleanup Script ===="
echo "This script will delete local and remote branches that have been fully merged into master."
echo ""

# List of branches that will be preserved no matter what
PRESERVED_BRANCHES="master"

# Get list of merged branches
MERGED_BRANCHES=$(git branch --merged master | grep -v "^\*" | grep -v -E "^\s*($PRESERVED_BRANCHES)\s*$")

echo "The following local branches are fully merged into master and can be safely deleted:"
echo "$MERGED_BRANCHES"
echo ""

# Confirm before proceeding
read -p "Do you want to delete these local branches? (y/n): " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Yy]$ ]]; then
    for branch in $MERGED_BRANCHES; do
        echo "Deleting local branch: $branch"
        git branch -d $branch
    done
    echo "Local branches deleted."
else
    echo "Local branch deletion skipped."
fi

# Confirm before proceeding with remote branches
echo ""
echo "The same branches may exist on the remote server."
read -p "Do you want to delete these branches from the remote server too? (y/n): " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Yy]$ ]]; then
    for branch in $MERGED_BRANCHES; do
        echo "Deleting remote branch: $branch"
        git push origin --delete $branch
    done
    echo "Remote branches deleted."
else
    echo "Remote branch deletion skipped."
fi

echo ""
echo "==== Special Case: tab-detail-view ===="
echo "The tab-detail-view branch functionality has been implemented directly in master."
read -p "Do you want to delete the tab-detail-view branch? (y/n): " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Deleting local branch: tab-detail-view"
    git branch -d tab-detail-view
    
    echo "Deleting remote branch: tab-detail-view"
    git push origin --delete tab-detail-view
    echo "tab-detail-view branch deleted."
else
    echo "tab-detail-view branch deletion skipped."
fi

echo ""
echo "Branch cleanup completed."
