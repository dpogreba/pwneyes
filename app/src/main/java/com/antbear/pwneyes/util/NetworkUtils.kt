package com.antbear.pwneyes.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread

/**
 * Utility class for network-related operations and status monitoring.
 * Provides:
 * 1. Network connectivity status monitoring
 * 2. Internet availability checking
 * 3. Helper methods for network error handling
 */
class NetworkUtils(private val context: Context) {
    private val TAG = "NetworkUtils"
    
    // LiveData to track network connectivity status
    private val _networkConnected = MutableLiveData<Boolean>()
    val networkConnected: LiveData<Boolean> = _networkConnected
    
    // Internal callback for network status
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    
    // ConnectivityManager instance
    private val connectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }
    
    companion object {
        // Connection timeout in ms
        private const val CONNECTION_TIMEOUT = 5000
        
        // Used to test internet connectivity
        private const val INTERNET_TEST_URL = "https://www.google.com"
        
        // Used to convert bytes to KB
        private const val BYTES_TO_KB = 1024
    }
    
    init {
        // Initialize with current network state
        _networkConnected.value = isNetworkAvailable()
        
        // Start monitoring network changes
        startNetworkMonitoring()
    }
    
    /**
     * Register network callback to monitor connectivity changes
     */
    private fun startNetworkMonitoring() {
        try {
            if (connectivityManager == null) {
                Log.e(TAG, "ConnectivityManager is null, cannot monitor network")
                return
            }
            
            // Build network request
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            
            // Create callback
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d(TAG, "Network available")
                    _networkConnected.postValue(true)
                }
                
                override fun onLost(network: Network) {
                    Log.d(TAG, "Network lost")
                    _networkConnected.postValue(false)
                }
                
                override fun onUnavailable() {
                    Log.d(TAG, "Network unavailable")
                    _networkConnected.postValue(false)
                }
            }
            
            // Register the callback
            networkCallback?.let { callback ->
                connectivityManager?.registerNetworkCallback(networkRequest, callback)
                Log.d(TAG, "Network monitoring started")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting network monitoring", e)
        }
    }
    
    /**
     * Stop network monitoring
     */
    fun stopNetworkMonitoring() {
        try {
            if (connectivityManager == null || networkCallback == null) {
                return
            }
            
            connectivityManager?.unregisterNetworkCallback(networkCallback!!)
            Log.d(TAG, "Network monitoring stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping network monitoring", e)
        }
    }
    
    /**
     * Check if the device has an active network connection
     * Note: This does not guarantee internet connectivity, only that a network interface is available
     */
    fun isNetworkAvailable(): Boolean {
        try {
            if (connectivityManager == null) {
                return false
            }
            
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager?.activeNetwork ?: return false
                val capabilities = connectivityManager?.getNetworkCapabilities(network) ?: return false
                
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } else {
                // Fallback for older Android versions
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager?.activeNetworkInfo
                networkInfo?.isConnected ?: false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking network availability", e)
            return false
        }
    }
    
    /**
     * Check if the device has actual internet connectivity by making a lightweight HTTP request
     * This should be called from a background thread
     */
    fun hasInternetAccess(): Boolean {
        if (!isNetworkAvailable()) {
            return false
        }
        
        try {
            val url = URL(INTERNET_TEST_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECTION_TIMEOUT
            connection.readTimeout = CONNECTION_TIMEOUT
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.connect()
            
            val result = connection.responseCode == HttpURLConnection.HTTP_OK
            connection.disconnect()
            
            return result
        } catch (e: IOException) {
            Log.e(TAG, "Error checking internet access", e)
            return false
        }
    }
    
    /**
     * Check internet connectivity asynchronously and call the provided callback
     */
    fun checkInternetAsync(callback: (Boolean) -> Unit) {
        thread {
            val hasInternet = hasInternetAccess()
            callback(hasInternet)
        }
    }
    
    /**
     * Coroutine-friendly method to check internet connectivity
     */
    suspend fun hasInternetAccessSuspend(): Boolean = withContext(Dispatchers.IO) {
        hasInternetAccess()
    }
    
    /**
     * Get the current network type (WiFi, Mobile data, etc.)
     */
    fun getNetworkType(): String {
        try {
            if (connectivityManager == null) {
                return "Unknown"
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager?.activeNetwork ?: return "None"
                val capabilities = connectivityManager?.getNetworkCapabilities(network) ?: return "Unknown"
                
                return when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                    else -> "Unknown"
                }
            } else {
                // Fallback for older Android versions
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager?.activeNetworkInfo
                
                return when (networkInfo?.type) {
                    ConnectivityManager.TYPE_WIFI -> "WiFi"
                    ConnectivityManager.TYPE_MOBILE -> "Mobile Data"
                    ConnectivityManager.TYPE_ETHERNET -> "Ethernet"
                    else -> "Unknown"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting network type", e)
            return "Error"
        }
    }
    
    /**
     * Get network quality as string (High, Medium, Low, Unknown)
     */
    fun getNetworkQuality(): String {
        try {
            if (connectivityManager == null) {
                return "Unknown"
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager?.activeNetwork ?: return "None"
                val capabilities = connectivityManager?.getNetworkCapabilities(network) ?: return "Unknown"
                
                val downstreamBandwidthKbps = capabilities.linkDownstreamBandwidthKbps / BYTES_TO_KB
                
                return when {
                    downstreamBandwidthKbps > 10000 -> "High" // > 10 Mbps
                    downstreamBandwidthKbps > 1000 -> "Medium" // > 1 Mbps
                    downstreamBandwidthKbps > 0 -> "Low" // > 0 Mbps
                    else -> "Unknown"
                }
            } else {
                return "Unknown" // Can't determine on older Android versions
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting network quality", e)
            return "Unknown"
        }
    }
    
    /**
     * Get helpful error message for network failures that users can understand
     */
    fun getNetworkErrorMessage(): String {
        if (!isNetworkAvailable()) {
            return "No network connection. Please check your WiFi or mobile data."
        }
        
        return "Network error. Please check your internet connection and try again."
    }
    
    /**
     * Format network info for logging or debugging
     */
    fun getNetworkInfoForLogging(): String {
        val isConnected = isNetworkAvailable()
        val networkType = getNetworkType()
        val networkQuality = getNetworkQuality()
        
        return "Network: ${if (isConnected) "Connected" else "Disconnected"}, " +
               "Type: $networkType, " +
               "Quality: $networkQuality"
    }
}
