package com.antbear.pwneyes

import android.app.Application
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.antbear.pwneyes.data.AppDatabase
import com.antbear.pwneyes.data.ConnectionRepository

class PwnEyesApplication : Application() {
    private val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { ConnectionRepository(database.connectionDao()) }
    
    override fun onCreate() {
        super.onCreate()
        applyTheme()
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
