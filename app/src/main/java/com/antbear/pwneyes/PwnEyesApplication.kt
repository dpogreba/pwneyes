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
import com.antbear.pwneyes.util.AdsManager

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
    
    val billingManager by lazy { 
        try {
            Log.d(TAG, "Attempting to initialize BillingManager...")
            val manager = BillingManager(this)
            Log.d(TAG, "BillingManager initialized successfully")
            manager
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing billing manager", e)
            Toast.makeText(
                this,
                "Failed to initialize billing. In-app purchases may not be available.",
                Toast.LENGTH_LONG
            ).show()
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
            // Only initialize if billing manager was successfully created
            Log.d(TAG, "About to initialize AdsManager with BillingManager")
            if (billingManager != null) {
                Log.d(TAG, "BillingManager is available, initializing AdsManager")
                AdsManager.initialize(this, billingManager!!)
            } else {
                Log.e(TAG, "BillingManager is null, skipping AdsManager initialization")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AdsManager", e)
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
