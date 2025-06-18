# PwnEyes Branch Documentation

This document provides information about the Git branches in the PwnEyes repository before cleanup.

## Master Branch

- **master**: The main production branch containing all stable features.

## Feature Branches (Fully Merged)

These branches have been fully merged into master and can be safely deleted:

- **aggressive-webview-fixes**: Implemented aggressive fixes for WebView rendering issues.
- **extreme-webview-fixes**: Implemented extreme measures for WebView fixes for v10.14.
- **native-controls-overlay**: Added native controls overlay for WebView bottom buttons for v10.15.
- **stability-improvements**: General stability improvements across the app.
- **url-port-fix**: Fixed URL port handling when adding authentication for v10.16.
- **webview-bottom-controls-fix**: Fixed issues with bottom controls visibility in WebView.
- **webview-compilation-fix**: Fixed compilation issues in WebView-related code.
- **webview-orientation-fix**: Fixed orientation change issues in WebView.
- **webview-scrolling-fixes**: Fixed scrolling issues in WebView.
- **webview-scrolling-improvements**: Additional scrolling improvements for WebView.

## Special Case

- **tab-detail-view**: Implemented plugin tab navigation. This branch's functionality has been implemented directly in master (v10.17), but using a different approach.

## Best Practices for Branch Management

1. **Delete merged branches**: Once a branch's changes have been merged into master, it should be deleted to avoid clutter.
2. **Keep branch names descriptive**: Use feature-based or version-based naming for clarity.
3. **Limit the number of active branches**: Too many branches can cause confusion and management overhead.
4. **Document branch purposes**: Maintain documentation about what each branch is for.

## Cleaning Up Branches

Use the provided `clean-branches.sh` script to safely delete branches that have been fully merged into master. The script will:

1. Show you which branches are fully merged and can be safely deleted
2. Ask for confirmation before deleting local branches
3. Ask for confirmation before deleting remote branches
4. Handle the special case of the tab-detail-view branch separately
