package com.antbear.pwneyes

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.antbear.pwneyes.billing.BillingManager
import com.antbear.pwneyes.data.AppDatabase
import com.antbear.pwneyes.data.ConnectionRepository
import com.antbear.pwneyes.health.ConnectionHealthService
import com.antbear.pwneyes.util.AdsManagerBase
import com.antbear.pwneyes.util.CrashReporter
import com.antbear.pwneyes.util.NetworkUtils
import com.antbear.pwneyes.util.ReleaseNotesManager
import com.antbear.pwneyes.util.VersionManager
import java.io.File

// TODO: Uncomment this when Hilt is properly configured
// import dagger.hilt.android.HiltAndroidApp
// @HiltAndroidApp
class PwnEyesApplication : Application() {
    private val TAG = "PwnEyesApplication"
    
    // Use lazy initialization with more robust error handling
    private val database by lazy { 
        try {
            Log.d(TAG, "Initializing Room database")
            val db = AppDatabase.getDatabase(this)
            Log.d(TAG, "Room database initialized successfully")
            db
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing database: ${e.message}", e)
            null
        }
    }
    
    // Create a repository that can handle null database
    val repository by lazy { 
        try {
            val dao = database?.connectionDao()
            Log.d(TAG, "Creating repository with DAO: ${dao != null}")
            ConnectionRepository(dao)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating repository", e)
            // Create a fallback repository that won't crash
            ConnectionRepository(null)
        }
    }
    
    val connectionHealthService by lazy {
        try {
            val connectionDao = database?.connectionDao() 
                ?: throw IllegalStateException("Database not initialized")
            ConnectionHealthService(this, connectionDao)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing connection health service", e)
            null
        }
    }
    
    val billingManager by lazy { 
        try {
            // Check if device has Google Play Services first
            val playServicesAvailable = try {
                val status = com.google.android.gms.common.GoogleApiAvailability.getInstance()
                    .isGooglePlayServicesAvailable(this)
                status == com.google.android.gms.common.ConnectionResult.SUCCESS
            } catch (e: Exception) {
                Log.w(TAG, "Error checking Google Play Services availability", e)
                false
            }
            
            if (playServicesAvailable) {
                Log.d(TAG, "Google Play Services available, initializing BillingManager...")
                val manager = BillingManager(this)
                Log.d(TAG, "BillingManager initialized successfully")
                manager
            } else {
                Log.w(TAG, "Google Play Services unavailable, skipping BillingManager initialization")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing billing manager", e)
            null
        }
    }
    
    // Network utilities for network status monitoring and error handling
    val networkUtils by lazy {
        try {
            Log.d(TAG, "Initializing NetworkUtils")
            // Create a new instance directly since we're not using DI now
            NetworkUtils(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing NetworkUtils", e)
            null
        }
    }
    
    // Version management for tracking app updates
    val versionManager by lazy {
        try {
            Log.d(TAG, "Initializing VersionManager")
            VersionManager(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing VersionManager", e)
            null
        }
    }
    
    // Release notes management for displaying what's new information
    val releaseNotesManager by lazy {
        try {
            Log.d(TAG, "Initializing ReleaseNotesManager")
            val vManager = versionManager ?: throw IllegalStateException("VersionManager not initialized")
            ReleaseNotesManager(this, vManager)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing ReleaseNotesManager", e)
            null
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize crash reporter (must be done before any other initialization)
        try {
            Log.d(TAG, "Initializing crash reporter")
            CrashReporter.initialize(this)
            
            // Check for crash reports from previous sessions
            CrashReporter.checkForCrashReports(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing crash reporter", e)
        }
        
        // Initialize network utils early to detect connectivity
        try {
            Log.d(TAG, "Initializing NetworkUtils")
            if (networkUtils == null) {
                Log.e(TAG, "NetworkUtils could not be initialized, connectivity monitoring won't be available")
            } else {
                Log.d(TAG, "Network status: " + (networkUtils?.getNetworkInfoForLogging() ?: "Unknown"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing network utils", e)
        }
        
        try {
            applyTheme()
        } catch (e: Exception) {
            Log.e(TAG, "Error applying theme", e)
        }
        
        try {
            // Initialize the AdsManager with the BillingManager
            Log.d(TAG, "About to initialize AdsManager with BillingManager")
            
            // Determine which flavor we're running
            val packageName = applicationContext.packageName
            Log.d(TAG, "Package name: $packageName")
            
            // Direct initialization without reflection
            if (packageName.endsWith(".free")) {
                Log.d(TAG, "Using direct FreeAdsManager initialization")
                try {
                    com.antbear.pwneyes.util.FreeAdsManager.initialize(this, billingManager)
                    Log.d(TAG, "FreeAdsManager initialized successfully")
                } catch (ex: Exception) {
                    Log.e(TAG, "Error initializing FreeAdsManager directly: ${ex.message}", ex)
                }
            } else if (packageName.endsWith(".paid")) {
                Log.d(TAG, "Skipping ads initialization for paid version")
                // Paid version doesn't need ads - initializing the base class is enough
                AdsManagerBase.INSTANCE = object : AdsManagerBase {
                    override fun setDebugPremiumMode(enabled: Boolean) {}
                    override fun loadBannerAd(adContainer: ViewGroup) {
                        adContainer.visibility = ViewGroup.GONE
                    }
                    override fun cleanup() {}
                }
            } else {
                Log.d(TAG, "Unknown package suffix, using no-op AdsManager")
                // Create a no-op implementation that won't crash
                AdsManagerBase.INSTANCE = object : AdsManagerBase {
                    override fun setDebugPremiumMode(enabled: Boolean) {}
                    override fun loadBannerAd(adContainer: ViewGroup) {
                        adContainer.visibility = ViewGroup.GONE
                    }
                    override fun cleanup() {}
                }
            }
            
            Log.d(TAG, "AdsManager initialization completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during AdsManager initialization process: ${e.message}", e)
            
            // Create a fallback no-op implementation
            AdsManagerBase.INSTANCE = object : AdsManagerBase {
                override fun setDebugPremiumMode(enabled: Boolean) {}
                override fun loadBannerAd(adContainer: ViewGroup) {
                    adContainer.visibility = ViewGroup.GONE
                }
                override fun cleanup() {}
            }
        }
        
        // Create schema directory if it doesn't exist
        try {
            val schemaDir = File(filesDir, "schemas")
            if (!schemaDir.exists()) {
                schemaDir.mkdirs()
                Log.d(TAG, "Created schemas directory: ${schemaDir.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating schemas directory: ${e.message}", e)
        }
            
        // Force database initialization by accessing it
        try {
            Log.d(TAG, "Ensuring database is initialized")
            if (database != null) {
                Log.d(TAG, "Database accessed successfully")
            } else {
                Log.w(TAG, "Database is null - will use fallback in-memory implementation")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Critical error accessing database: ${e.message}", e)
            // Don't crash the app, but log the error
        }
        
        // Start connection health monitoring only if the database initialization was successful
        try {
            if (database != null) {
                Log.d(TAG, "Starting connection health monitoring")
                connectionHealthService?.startMonitoring()
            } else {
                Log.w(TAG, "Skipping connection health monitoring due to database initialization failure")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting connection health monitoring", e)
        }
        
        // Initialize the version manager to track app updates
        try {
            Log.d(TAG, "Checking app version")
            if (versionManager != null) {
                // If this is the first run after an update, record the new version code
                if (versionManager!!.isAppUpdated()) {
                    Log.d(TAG, "App was updated from version ${versionManager!!.getPreviousVersionCode()} to ${versionManager!!.getCurrentVersionCode()}")
                }
                // Always update the previous version code to the current one
                versionManager!!.updatePreviousVersionCode()
            } else {
                Log.w(TAG, "VersionManager is null, cannot check for app updates")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking app version", e)
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        
        // Stop connection health monitoring
        try {
            connectionHealthService?.stopMonitoring()
            connectionHealthService?.cleanup()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping connection health service", e)
        }
        
        // Stop network monitoring
        try {
            // Call the stopNetworkMonitoring method directly
            networkUtils?.stopNetworkMonitoring()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping network monitoring", e)
        }
    }
    
    private fun applyTheme() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val themeValue = sharedPreferences.getString("theme_preference", "system") ?: "system"
        
        val mode = when (themeValue) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
