package com.antbear.pwneyes.ui.settings

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
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
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreference
import com.antbear.pwneyes.BuildConfig
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
                // Premium status observer
                manager.premiumStatus.observe(this, Observer { premium ->
                    isPremium = premium
                    updatePremiumPreferencesVisibility()
                })
                
                // Connection state observer
                manager.connectionState.observe(this, Observer { state ->
                    when (state) {
                        BillingManager.STATE_DISCONNECTED -> {
                            Log.d(TAG, "Billing service disconnected")
                            updateBillingState(false, "Billing service disconnected")
                            updateBillingStatusUI(state, manager.lastErrorMessage.value)
                        }
                        BillingManager.STATE_CONNECTING -> {
                            Log.d(TAG, "Connecting to billing service...")
                            updateBillingState(false, "Connecting to billing service...")
                            updateBillingStatusUI(state, null)
                        }
                        BillingManager.STATE_CONNECTED -> {
                            Log.d(TAG, "Billing service connected")
                            updateBillingState(true, null)
                            updateBillingStatusUI(state, null)
                        }
                        BillingManager.STATE_ERROR -> {
                            val errorMsg = manager.lastErrorMessage.value ?: "Unknown error"
                            Log.e(TAG, "Billing service error: $errorMsg")
                            updateBillingState(false, errorMsg)
                            updateBillingStatusUI(state, errorMsg)
                        }
                    }
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error accessing billing manager", e)
            isPremium = false
            updateBillingState(false, "Failed to initialize billing")
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
                Toast.makeText(requireContext(), getString(R.string.toast_billing_unavailable), Toast.LENGTH_SHORT).show()
            }
            true
        }
        
        // Set up restore purchases preference
        findPreference<Preference>("restore_purchases")?.setOnPreferenceClickListener {
            if (billingManager != null) {
                billingManager?.restorePurchases()
                Toast.makeText(requireContext(), getString(R.string.toast_restoring_purchases), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), getString(R.string.toast_billing_unavailable), Toast.LENGTH_SHORT).show()
            }
            true
        }
        
        // Set up Buy Me Coffee preference
        findPreference<Preference>("buy_me_coffee")?.setOnPreferenceClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/ltldrk"))
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error opening Buy Me Coffee link", e)
                Toast.makeText(requireContext(), getString(R.string.toast_browser_error), Toast.LENGTH_SHORT).show()
            }
            true
        }
        
        // Set up Contact Us preference
        findPreference<Preference>("contact_us")?.setOnPreferenceClickListener {
            try {
                val emailAddress = "PwnEyes@proton.me"
                
                // Create a more general intent that more apps can handle
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "message/rfc822" // Standard email MIME type
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
                    putExtra(Intent.EXTRA_SUBJECT, "FROM PWNEYES ANDROID")
                    
                    // Include app version in the email body
                    val appVersion = "App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                    val deviceInfo = "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}, Android ${android.os.Build.VERSION.RELEASE}"
                    putExtra(Intent.EXTRA_TEXT, "----------\n\n\n----------\n$appVersion\n$deviceInfo")
                }
                
                // Create and show chooser dialog with all compatible apps
                val chooser = Intent.createChooser(intent, "Contact Support")
                startActivity(chooser)
            } catch (e: Exception) {
                Log.e(TAG, "Error launching email intent", e)
                Toast.makeText(requireContext(), getString(R.string.toast_email_error), Toast.LENGTH_SHORT).show()
            }
            true
        }
        
        // Initial update of premium preferences visibility
        updatePremiumPreferencesVisibility()
    }
    
    /**
     * Updates the UI based on the billing connection state
     */
    private fun updateBillingState(isConnected: Boolean, errorMessage: String?) {
        try {
            val removeAdsPreference = findPreference<Preference>("remove_ads")
            val restorePurchasesPreference = findPreference<Preference>("restore_purchases")
            
            // Enable/disable based on connection status
            removeAdsPreference?.isEnabled = isConnected
            restorePurchasesPreference?.isEnabled = isConnected
            
            // Update summary to show connection status
            if (!isConnected) {
                removeAdsPreference?.summary = errorMessage ?: "Billing service not available"
                restorePurchasesPreference?.summary = errorMessage ?: "Billing service not available"
            } else {
                // Clear error message if connected
                if (isPremium) {
                    removeAdsPreference?.summary = getString(R.string.premium_status)
                } else {
                    removeAdsPreference?.summary = getString(R.string.pref_remove_ads_summary)
                }
                restorePurchasesPreference?.summary = getString(R.string.pref_restore_purchases_summary)
            }
            
            Log.d(TAG, "Updated billing state UI. isConnected: $isConnected, errorMessage: $errorMessage")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating billing state UI", e)
        }
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
                removeAdsPreference?.summary = getString(R.string.premium_status)
            }
            
            Log.d(TAG, "Updated premium preferences. isPremium: $isPremium, billingAvailable: ${billingManager != null}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating premium preferences visibility", e)
        }
    }
    
    private fun showEraseAllConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_erase_all_title))
            .setMessage(getString(R.string.dialog_erase_all_message))
            .setPositiveButton(getString(R.string.dialog_erase_all_confirm)) { _, _ -> 
                sharedViewModel.deleteAllConnections()
                Toast.makeText(requireContext(), getString(R.string.toast_connections_deleted), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.dialog_cancel), null)
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
    
    override fun onDestroy() {
        try {
            // Remove LiveData observers to prevent memory leaks
            billingManager?.let { manager ->
                manager.premiumStatus.removeObservers(this)
                manager.connectionState.removeObservers(this)
                manager.lastErrorMessage.removeObservers(this)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing observers in onDestroy", e)
        }
        
        super.onDestroy()
    }
    
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "theme_preference" && sharedPreferences != null) {
            applyTheme(sharedPreferences)
        }
    }
    
    /**
     * Updates the billing status UI based on connection state
     * Note: Retry is now handled automatically in the background
     */
    private fun updateBillingStatusUI(state: Int, errorMessage: String?) {
        try {
            val retryBillingPreference = findPreference<Preference>("retry_billing")
            val billingStatusPreference = findPreference<Preference>("billing_status")
            
            // Always hide the retry button as retries are now automatic
            retryBillingPreference?.isVisible = false
            
            // Update status text with current state information
            val statusText = when (state) {
                BillingManager.STATE_CONNECTING -> getString(R.string.billing_connecting)
                BillingManager.STATE_CONNECTED -> "Connected to Google Play Billing"
                BillingManager.STATE_DISCONNECTED -> getString(R.string.billing_disconnected)
                BillingManager.STATE_ERROR -> getString(R.string.billing_error, errorMessage ?: "Unknown error")
                else -> "Unknown billing state"
            }
            
            // Show status preference when needed
            billingStatusPreference?.isVisible = true
            billingStatusPreference?.title = getString(R.string.billing_connection_status, statusText)
            
            // For error states, also show detailed error message as summary
            if (state == BillingManager.STATE_ERROR && !errorMessage.isNullOrEmpty()) {
                billingStatusPreference?.summary = errorMessage
            } else {
                billingStatusPreference?.summary = null
            }
            
            Log.d(TAG, "Updated billing status UI. State: $state, Message: $errorMessage")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating billing status UI", e)
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
