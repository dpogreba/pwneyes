# PwnEyes v10.20 Release Notes

## Bug Fixes

- **Critical fix**: Resolved app crash when checking network connectivity. The app was previously crashing with error "Can't toast on a thread that has not called Looper.prepare()" when network checks were performed and UI updates were attempted from a background thread.

- Improved error handling in network connectivity checks to ensure the app gracefully handles connectivity issues.

## Technical Improvements

- Updated network utilities to use proper threading patterns by ensuring callbacks run on the main thread.
- Added additional error logging for network operations to improve diagnostics.
- Enhanced network error recovery to provide better user experience when connectivity is limited.

## Stability Improvements

- Fixed thread-related crash in background network operations.
- Added error handling to ensure network connectivity checks don't disrupt app operation even when they fail.

---

Release Date: June 20, 2025
