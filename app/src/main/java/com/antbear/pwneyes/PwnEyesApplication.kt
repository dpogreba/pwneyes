package com.antbear.pwneyes

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.antbear.pwneyes.billing.BillingManager
import com.antbear.pwneyes.data.AppDatabase
import com.antbear.pwneyes.data.ConnectionRepository
import com.antbear.pwneyes.health.ConnectionHealthService
import com.antbear.pwneyes.util.AdsManagerBase

class PwnEyesApplication : Application() {
    private val TAG = "PwnEyesApplication"
    
    // Use lazy initialization to handle potential exceptions
    private val database by lazy { 
        try {
            AppDatabase.getDatabase(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing database", e)
            null
        }
    }
    
    val repository by lazy { 
        ConnectionRepository(database?.connectionDao() ?: throw IllegalStateException("Database not initialized"))
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
    
    override fun onCreate() {
        super.onCreate()
        
        // Set default uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception in thread ${thread.name}", throwable)
            // Let the default handler deal with it after logging
            Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(thread, throwable)
        }
        
        try {
            applyTheme()
        } catch (e: Exception) {
            Log.e(TAG, "Error applying theme", e)
        }
        
        try {
            // Initialize the AdsManager with the BillingManager
            Log.d(TAG, "About to initialize AdsManager with BillingManager")
            // Initialize AdsManager even if billingManager is null 
            // (it will handle the null case internally)
            // Flavor-specific AdsManager implementation will be used
            AdsManagerBase.initialize(this, billingManager)
            Log.d(TAG, "AdsManager initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AdsManager", e)
        }
        
        // Start connection health monitoring
        try {
            Log.d(TAG, "Starting connection health monitoring")
            connectionHealthService?.startMonitoring()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting connection health monitoring", e)
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
