package com.antbear.pwneyes

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.antbear.pwneyes.billing.BillingManager
import com.antbear.pwneyes.data.ConnectionRepository
import com.antbear.pwneyes.health.ConnectionHealthService
import com.antbear.pwneyes.util.AdsManagerBase
import com.antbear.pwneyes.util.CrashReporter
import com.antbear.pwneyes.util.NetworkUtils
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PwnEyesApplication : Application() {
    private val TAG = "PwnEyesApplication"
    
    // Injected dependencies
    @Inject lateinit var repository: ConnectionRepository
    @Inject lateinit var connectionHealthService: ConnectionHealthService
    @Inject lateinit var networkUtils: NetworkUtils
    
    // Optional dependencies that might be null
    @Inject lateinit var billingManager: BillingManager
    
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
            connectionHealthService.startMonitoring()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting connection health monitoring", e)
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        
        // Stop connection health monitoring
        try {
            connectionHealthService.stopMonitoring()
            connectionHealthService.cleanup()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping connection health service", e)
        }
        
        // Stop network monitoring
        try {
            networkUtils.stopNetworkMonitoring()
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
