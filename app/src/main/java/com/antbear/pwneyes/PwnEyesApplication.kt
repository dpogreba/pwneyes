package com.antbear.pwneyes

import android.app.Application
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.antbear.pwneyes.billing.BillingManager
import com.antbear.pwneyes.data.AppDatabase
import com.antbear.pwneyes.data.ConnectionRepository
import com.antbear.pwneyes.util.AdsManager

class PwnEyesApplication : Application() {
    private val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { ConnectionRepository(database.connectionDao()) }
    val billingManager by lazy { BillingManager(this) }
    
    override fun onCreate() {
        super.onCreate()
        applyTheme()
        
        // Initialize the AdsManager with the BillingManager
        AdsManager.initialize(this, billingManager)
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
