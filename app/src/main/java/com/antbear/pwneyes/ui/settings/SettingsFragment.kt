package com.antbear.pwneyes.ui.settings

import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import com.antbear.pwneyes.PwnEyesApplication
import com.antbear.pwneyes.R
import com.antbear.pwneyes.billing.BillingManager
import com.antbear.pwneyes.ui.home.SharedViewModel

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val TAG = "SettingsFragment"
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var sharedPreferences: SharedPreferences
    private var billingManager: BillingManager? = null
    private var isPremium = false

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        
        // Initialize SharedPreferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        
        // Initialize BillingManager
        try {
            billingManager = (requireActivity().application as PwnEyesApplication).billingManager
            
            // Observe premium status changes if billing manager is available
            billingManager?.let { manager ->
                manager.premiumStatus.observe(this, Observer { premium ->
                    isPremium = premium
                    updatePremiumPreferencesVisibility()
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error accessing billing manager", e)
            isPremium = false
        }
        
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
        
        // Set up remove ads preference
        findPreference<Preference>("remove_ads")?.setOnPreferenceClickListener {
            if (billingManager != null) {
                billingManager?.launchPurchaseFlow(requireActivity())
            } else {
                Toast.makeText(requireContext(), "Billing service is not available", Toast.LENGTH_SHORT).show()
            }
            true
        }
        
        // Set up restore purchases preference
        findPreference<Preference>("restore_purchases")?.setOnPreferenceClickListener {
            if (billingManager != null) {
                billingManager?.restorePurchases()
                Toast.makeText(requireContext(), "Restoring purchases...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Billing service is not available", Toast.LENGTH_SHORT).show()
            }
            true
        }
        
        // Initial update of premium preferences visibility
        updatePremiumPreferencesVisibility()
    }
    
    private fun updatePremiumPreferencesVisibility() {
        try {
            val removeAdsPreference = findPreference<Preference>("remove_ads")
            val restorePurchasesPreference = findPreference<Preference>("restore_purchases")
            val premiumCategory = findPreference<PreferenceScreen>("premium_category")
            
            // Hide "Remove Ads" option if user is already premium
            removeAdsPreference?.isVisible = !isPremium && billingManager != null
            
            // Show "Restore Purchases" only if billing is available
            restorePurchasesPreference?.isVisible = billingManager != null
            
            // Update summary if user is premium
            if (isPremium) {
                removeAdsPreference?.summary = "Premium status active"
            }
            
            Log.d(TAG, "Updated premium preferences. isPremium: $isPremium, billingAvailable: ${billingManager != null}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating premium preferences visibility", e)
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
