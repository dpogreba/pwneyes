package com.antbear.pwneyes.ui.settings

import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.activityViewModels
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.antbear.pwneyes.R
import com.antbear.pwneyes.ui.home.SharedViewModel

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        
        // Initialize SharedPreferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        
        // Set up the erase all preference
        findPreference<Preference>("erase_all")?.setOnPreferenceClickListener {
            showEraseAllConfirmationDialog()
            true
        }
        
        // Set up theme preference
        val themePreference = findPreference<ListPreference>("theme_preference")
        themePreference?.summaryProvider = Preference.SummaryProvider<ListPreference> { preference ->
            "Selected: ${preference.entry}"
        }
    }
    
    private fun showEraseAllConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Erase All Connections")
            .setMessage("Are you sure you want to erase all connections? This action cannot be undone.")
            .setPositiveButton("Erase All") { _, _ -> 
                sharedViewModel.deleteAllConnections()
                Toast.makeText(requireContext(), "All connections deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onResume() {
        super.onResume()
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }
    
    override fun onPause() {
        super.onPause()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }
    
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "theme_preference" && sharedPreferences != null) {
            applyTheme(sharedPreferences)
        }
    }
    
    private fun applyTheme(sharedPreferences: SharedPreferences) {
        val themeValue = sharedPreferences.getString("theme_preference", "system") ?: "system"
        
        val mode = when (themeValue) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
