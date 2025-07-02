package com.antbear.pwneyes.util

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.util.Log

/**
 * Manages version-related functionality, including tracking app updates
 * and determining when to show "What's New" information.
 */
class VersionManager(private val context: Context) {
    
    private val TAG = "VersionManager"
    
    // SharedPreferences keys
    companion object {
        private const val PREFS_NAME = "pwneyes_version_prefs"
        private const val KEY_PREVIOUS_VERSION_CODE = "previous_version_code"
        private const val KEY_WHATS_NEW_SEEN = "whats_new_seen_for_version"
    }
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Get the current app version code
     */
    fun getCurrentVersionCode(): Long {
        return try {
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current version code", e)
            0L
        }
    }
    
    /**
     * Get the current app version name
     */
    fun getCurrentVersionName(): String {
        return try {
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current version name", e)
            "Unknown"
        }
    }
    
    /**
     * Get the previously recorded version code
     */
    fun getPreviousVersionCode(): Long {
        return prefs.getLong(KEY_PREVIOUS_VERSION_CODE, 0L)
    }
    
    /**
     * Record the current version code as the previous version
     */
    fun updatePreviousVersionCode() {
        prefs.edit().putLong(KEY_PREVIOUS_VERSION_CODE, getCurrentVersionCode()).apply()
    }
    
    /**
     * Check if the app has been updated (not a fresh install)
     */
    fun isAppUpdated(): Boolean {
        val currentVersion = getCurrentVersionCode()
        val previousVersion = getPreviousVersionCode()
        
        // If previous version is 0, it's a fresh install or first run after clearing data
        // If current > previous, it's an update
        return previousVersion > 0 && currentVersion > previousVersion
    }
    
    /**
     * Check if the "What's New" dialog has been seen for the current version
     */
    fun isWhatsNewSeen(): Boolean {
        val currentVersion = getCurrentVersionCode()
        val lastSeenVersion = prefs.getLong(KEY_WHATS_NEW_SEEN, 0L)
        return currentVersion <= lastSeenVersion
    }
    
    /**
     * Mark the "What's New" dialog as seen for the current version
     */
    fun markWhatsNewAsSeen() {
        prefs.edit().putLong(KEY_WHATS_NEW_SEEN, getCurrentVersionCode()).apply()
    }
    
    /**
     * Check if we should show the "What's New" button in the drawer
     */
    fun shouldShowWhatsNewButton(): Boolean {
        // Show if the app has been updated AND the user hasn't seen the what's new dialog yet
        return isAppUpdated() && !isWhatsNewSeen()
    }
}
