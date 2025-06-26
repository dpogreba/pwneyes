package com.antbear.pwneyes.util

import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import androidx.preference.PreferenceManager
import com.antbear.pwneyes.R
import java.lang.reflect.Method

/**
 * Utility class for Bluetooth tethering operations.
 * Handles checking if tethering is enabled and provides methods to show dialogs
 * and open system settings.
 */
class BluetoothUtils(private val context: Context) {
    private val TAG = "BluetoothUtils"
    
    // Shared preferences for storing user's "don't show again" choice
    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    
    companion object {
        // Preference key for the "show Bluetooth tethering check" setting
        const val PREF_SHOW_BLUETOOTH_CHECK = "show_bluetooth_tethering_check"
        
        // Default value - show the check by default
        const val DEFAULT_SHOW_BLUETOOTH_CHECK = true
    }
    
    /**
     * Check if Bluetooth is enabled on the device
     */
    fun isBluetoothEnabled(): Boolean {
        return try {
            val bluetoothManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.getSystemService(BluetoothManager::class.java)
            } else {
                context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            }
            
            val bluetoothAdapter = bluetoothManager?.adapter
            bluetoothAdapter?.isEnabled == true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Bluetooth status", e)
            false
        }
    }
    
    /**
     * Check if Bluetooth tethering is enabled
     * This uses a combination of methods as there's no direct API to check tethering status
     */
    fun isBluetoothTetheringEnabled(): Boolean {
        // First check if Bluetooth is enabled at all
        if (!isBluetoothEnabled()) {
            return false
        }
        
        try {
            // Method 1: Check if there's an active Bluetooth tethering network interface
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            
            if (connectivityManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // For newer Android versions, check active network capabilities
                    val network = connectivityManager.activeNetwork
                    val capabilities = connectivityManager.getNetworkCapabilities(network)
                    
                    if (capabilities != null) {
                        // Check if we have a Bluetooth network
                        if (capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_BLUETOOTH)) {
                            return true
                        }
                    }
                } else {
                    // For older Android versions
                    @Suppress("DEPRECATION")
                    val networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_BLUETOOTH)
                    @Suppress("DEPRECATION")
                    if (networkInfo?.isConnected == true) {
                        return true
                    }
                }
            }
            
            // Method 2: Try to use reflection to access tethering state
            // This is a fallback and may not work on all devices
            return getBluetoothTetheringState()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Bluetooth tethering status", e)
            // If we can't determine, assume it's enabled to avoid false alarms
            return true
        }
    }
    
    /**
     * Try to get Bluetooth tethering state using reflection.
     * This is a fallback method that might not work on all devices or Android versions.
     */
    private fun getBluetoothTetheringState(): Boolean {
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() ?: return false
            
            // Try to invoke the getTetheringState method using reflection
            val method: Method = bluetoothAdapter.javaClass.getDeclaredMethod("isTetheringOn")
            method.isAccessible = true
            return method.invoke(bluetoothAdapter) as Boolean
        } catch (e: Exception) {
            Log.e(TAG, "Error accessing Bluetooth tethering state via reflection", e)
            return false
        }
    }
    
    /**
     * Check if we should show the Bluetooth tethering warning
     * based on user preference
     */
    fun shouldShowBluetoothTetheringWarning(): Boolean {
        return preferences.getBoolean(PREF_SHOW_BLUETOOTH_CHECK, DEFAULT_SHOW_BLUETOOTH_CHECK)
    }
    
    /**
     * Set whether to show the Bluetooth tethering warning
     */
    fun setShowBluetoothTetheringWarning(show: Boolean) {
        preferences.edit().putBoolean(PREF_SHOW_BLUETOOTH_CHECK, show).apply()
    }
    
    /**
     * Show the Bluetooth tethering dialog if needed
     * Returns true if the dialog was shown
     */
    fun showBluetoothTetheringDialogIfNeeded(): Boolean {
        // Check if we should show the dialog based on user preference
        if (!shouldShowBluetoothTetheringWarning()) {
            return false
        }
        
        // Check if Bluetooth tethering is already enabled
        if (isBluetoothTetheringEnabled()) {
            return false
        }
        
        // Show the dialog
        showBluetoothTetheringDialog()
        return true
    }
    
    /**
     * Show a dialog to inform the user that Bluetooth tethering is required
     * and offer to open system settings
     */
    private fun showBluetoothTetheringDialog() {
        try {
            // Create a custom dialog
            val dialog = Dialog(context)
            val inflater = LayoutInflater.from(context)
            val dialogView = inflater.inflate(R.layout.dialog_bluetooth_tethering, null)
            dialog.setContentView(dialogView)
            
            // Get references to views
            val btnCancel = dialogView.findViewById<Button>(R.id.button_cancel)
            val btnSettings = dialogView.findViewById<Button>(R.id.button_settings)
            val cbDontShow = dialogView.findViewById<CheckBox>(R.id.checkbox_do_not_show)
            
            // Set up click listeners
            btnCancel.setOnClickListener {
                // If the checkbox is checked, update the preference
                if (cbDontShow.isChecked) {
                    setShowBluetoothTetheringWarning(false)
                }
                dialog.dismiss()
            }
            
            btnSettings.setOnClickListener {
                // If the checkbox is checked, update the preference
                if (cbDontShow.isChecked) {
                    setShowBluetoothTetheringWarning(false)
                }
                
                // Open Bluetooth tethering settings
                openBluetoothTetheringSettings()
                dialog.dismiss()
            }
            
            // Show the dialog
            dialog.show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing Bluetooth tethering dialog", e)
        }
    }
    
    /**
     * Open system settings to enable Bluetooth tethering
     */
    fun openBluetoothTetheringSettings() {
        try {
            // First try to open the tethering settings directly
            val tetheringIntent = Intent()
            tetheringIntent.setAction(Settings.ACTION_WIRELESS_SETTINGS)
            
            // Add flags to start in a new task
            tetheringIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            // Start the activity
            context.startActivity(tetheringIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening Bluetooth tethering settings, trying fallback", e)
            
            try {
                // Fallback to general Bluetooth settings
                val fallbackIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                Log.e(TAG, "Error opening Bluetooth settings fallback", e2)
            }
        }
    }
}
