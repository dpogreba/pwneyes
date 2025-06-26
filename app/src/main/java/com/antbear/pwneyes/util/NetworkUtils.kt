package com.antbear.pwneyes.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Enhanced utility class for network-related operations and status monitoring.
 * Provides:
 * 1. Network connectivity status monitoring with LiveData and StateFlow
 * 2. Internet availability checking with coroutines and cached results
 * 3. Helper methods for network error handling and status information
 * 4. Thread-safe operations and proper error handling
 */
class NetworkUtils(private val context: Context) {
    private val TAG = "NetworkUtils"

    // Coroutine scope for network operations
    private val networkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Handler for main thread operations
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Thread pool for network operations
    private val networkExecutor = Executors.newFixedThreadPool(3)
    
    // LiveData to track network connectivity status (for backward compatibility)
    private val _networkConnected = MutableLiveData<Boolean>()
    val networkConnected: LiveData<Boolean> = _networkConnected
    
    // StateFlow to track network connectivity status (modern approach)
    private val _networkState = MutableStateFlow<NetworkState>(NetworkState.Unknown)
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()
    
    // Cache for internet connectivity status
    private var lastInternetCheck = 0L
    private var cachedInternetStatus = false
    
    // Internal callback for network status
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    // ConnectivityManager instance
    private val connectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }

    companion object {
        // Connection timeout in ms
        private const val CONNECTION_TIMEOUT = 5000
        
        // Cache validity period in ms (5 seconds)
        private const val CACHE_VALIDITY_PERIOD = 5000
        
        // Used to test internet connectivity with fallbacks
        private val INTERNET_TEST_URLS = listOf(
            "https://www.google.com",
            "https://www.amazon.com",
            "https://www.microsoft.com"
        )
        
        // Used to convert bytes to KB
        private const val BYTES_TO_KB = 1024
    }
    
    /**
     * Represents the current state of network connectivity
     */
    sealed class NetworkState {
        object Connected : NetworkState()
        object Disconnected : NetworkState()
        object Unknown : NetworkState()
    }
    
    init {
        try {
            // Initialize with current network state
            val isAvailable = isNetworkAvailable()
            _networkConnected.value = isAvailable
            _networkState.value = if (isAvailable) NetworkState.Connected else NetworkState.Disconnected

            // Start monitoring network changes
            startNetworkMonitoring()
            
            // Perform initial internet check in background
            networkScope.launch {
                try {
                    val hasInternet = hasInternetAccessSuspend()
                    cachedInternetStatus = hasInternet
                    lastInternetCheck = System.currentTimeMillis()
                } catch (e: Exception) {
                    Log.e(TAG, "Error during initial internet check", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing NetworkUtils", e)
        }
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
                    updateNetworkStatus(true)
                }

                override fun onLost(network: Network) {
                    Log.d(TAG, "Network lost")
                    updateNetworkStatus(false)
                }

                override fun onUnavailable() {
                    Log.d(TAG, "Network unavailable")
                    updateNetworkStatus(false)
                }
            }

            // Register the callback
            networkCallback?.let { callback ->
                connectivityManager?.registerNetworkCallback(networkRequest, callback)
                Log.d(TAG, "Network monitoring started")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting network monitoring", e)
            // Fallback to default state
            updateNetworkStatus(false)
        }
    }
    
    /**
     * Update network status in both LiveData and StateFlow
     */
    private fun updateNetworkStatus(isConnected: Boolean) {
        try {
            mainHandler.post {
                _networkConnected.value = isConnected
            }
            
            _networkState.value = if (isConnected) NetworkState.Connected else NetworkState.Disconnected
            
            // Invalidate cache when network status changes
            lastInternetCheck = 0
        } catch (e: Exception) {
            Log.e(TAG, "Error updating network status", e)
        }
    }
    
    /**
     * Stop network monitoring and cleanup resources
     */
    fun stopNetworkMonitoring() {
        try {
            // Unregister network callback
            if (connectivityManager != null && networkCallback != null) {
                connectivityManager?.unregisterNetworkCallback(networkCallback!!)
                Log.d(TAG, "Network monitoring stopped")
            }
            
            // Shutdown the executor
            try {
                networkExecutor.shutdown()
                if (!networkExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    networkExecutor.shutdownNow()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error shutting down network executor", e)
                networkExecutor.shutdownNow()
            }
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
                @Suppress("DEPRECATION")
                networkInfo?.isConnected ?: false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking network availability", e)
            return false
        }
    }
    
    /**
     * Check if the device has actual internet connectivity by making a lightweight HTTP request
     * This method is thread-safe and can be called from any thread
     * It uses a cached result if a recent check was performed
     */
    fun hasInternetAccess(): Boolean {
        // Check if network is available first
        if (!isNetworkAvailable()) {
            return false
        }

        // Use cached result if it's still valid
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastInternetCheck < CACHE_VALIDITY_PERIOD) {
            Log.d(TAG, "Using cached internet status: $cachedInternetStatus")
            return cachedInternetStatus
        }

        try {
            // Try each URL until one succeeds or all fail
            for (urlString in INTERNET_TEST_URLS) {
                try {
                    val url = URL(urlString)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = CONNECTION_TIMEOUT
                    connection.readTimeout = CONNECTION_TIMEOUT
                    connection.instanceFollowRedirects = false
                    connection.useCaches = false
                    connection.connect()

                    val responseCode = connection.responseCode
                    connection.disconnect()

                    // Consider 2xx and 3xx response codes as successful
                    if (responseCode in 200..399) {
                        // Update cache
                        cachedInternetStatus = true
                        lastInternetCheck = currentTime
                        return true
                    }
                } catch (e: IOException) {
                    // Try the next URL
                    Log.w(TAG, "Failed to connect to $urlString, trying next URL")
                }
            }

            // All URLs failed
            cachedInternetStatus = false
            lastInternetCheck = currentTime
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking internet access", e)
            return false
        }
    }
    
    /**
     * Check internet connectivity asynchronously and call the provided callback on the main thread
     * Uses a thread pool instead of creating a new thread for each call
     */
    fun checkInternetAsync(callback: (Boolean) -> Unit) {
        networkExecutor.execute {
            try {
                val hasInternet = hasInternetAccess()
                // Use main thread for the callback to ensure UI operations like Toast are safe
                mainHandler.post {
                    callback(hasInternet)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking internet access", e)
                // Ensure callback is still called even on error
                mainHandler.post {
                    callback(false)
                }
            }
        }
    }
    
    /**
     * Check internet connectivity using coroutines
     * This is the preferred method for Kotlin code using coroutines
     */
    fun checkInternetWithCoroutines(scope: CoroutineScope, callback: (Boolean) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val hasInternet = hasInternetAccessSuspend()
                withContext(Dispatchers.Main) {
                    callback(hasInternet)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking internet with coroutines", e)
                withContext(Dispatchers.Main) {
                    callback(false)
                }
            }
        }
    }
    
    /**
     * Coroutine-friendly method to check internet connectivity
     */
    suspend fun hasInternetAccessSuspend(): Boolean = withContext(Dispatchers.IO) {
        try {
            hasInternetAccess()
        } catch (e: Exception) {
            Log.e(TAG, "Error in suspended internet check", e)
            false
        }
    }
    
    /**
     * Try to restore internet connectivity by performing common fixes
     * Returns true if any action was taken
     */
    fun attemptConnectivityRecovery(): Boolean {
        if (isNetworkAvailable()) {
            // No recovery needed if network is available
            return false
        }
        
        try {
            // Attempt to force network re-evaluation on newer Android versions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager?.reportNetworkConnectivity(null, true)
                Log.d(TAG, "Forced network connectivity re-evaluation")
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error attempting connectivity recovery", e)
        }
        
        return false
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
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "Bluetooth"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN) -> "LowPAN"
                    else -> "Unknown"
                }
            } else {
                // Fallback for older Android versions
                // Using deprecated APIs for backward compatibility on older Android versions
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager?.activeNetworkInfo

                @Suppress("DEPRECATION")
                return when (networkInfo?.type) {
                    @Suppress("DEPRECATION") ConnectivityManager.TYPE_WIFI -> "WiFi"
                    @Suppress("DEPRECATION") ConnectivityManager.TYPE_MOBILE -> "Mobile Data"
                    @Suppress("DEPRECATION") ConnectivityManager.TYPE_ETHERNET -> "Ethernet"
                    @Suppress("DEPRECATION") ConnectivityManager.TYPE_BLUETOOTH -> "Bluetooth"
                    @Suppress("DEPRECATION") ConnectivityManager.TYPE_VPN -> "VPN"
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
     * and also return the bandwidth in Mbps for more detailed information
     */
    data class NetworkQuality(val quality: String, val bandwidthMbps: Double)
    
    /**
     * Get network quality information
     */
    fun getNetworkQuality(): NetworkQuality {
        try {
            if (connectivityManager == null) {
                return NetworkQuality("Unknown", 0.0)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager?.activeNetwork ?: return NetworkQuality("None", 0.0)
                val capabilities = connectivityManager?.getNetworkCapabilities(network) 
                    ?: return NetworkQuality("Unknown", 0.0)

                val downstreamBandwidthKbps = capabilities.linkDownstreamBandwidthKbps
                val bandwidthMbps = downstreamBandwidthKbps / (BYTES_TO_KB * 1.0)

                val quality = when {
                    bandwidthMbps > 10.0 -> "High" // > 10 Mbps
                    bandwidthMbps > 1.0 -> "Medium" // > 1 Mbps
                    bandwidthMbps > 0.0 -> "Low" // > 0 Mbps
                    else -> "Unknown"
                }
                
                return NetworkQuality(quality, bandwidthMbps)
            } else {
                return NetworkQuality("Unknown", 0.0) // Can't determine on older Android versions
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting network quality", e)
            return NetworkQuality("Error", 0.0)
        }
    }
    
    /**
     * Get network quality as a simple string for backward compatibility
     */
    fun getNetworkQualityString(): String {
        return getNetworkQuality().quality
    }
    
    /**
     * Get helpful error message for network failures that users can understand
     * with more detailed diagnostic information
     */
    fun getNetworkErrorMessage(): String {
        if (!isNetworkAvailable()) {
            return "No network connection. Please check your WiFi or mobile data."
        }
        
        // Check if there might be internet but it's not responding
        val networkType = getNetworkType()
        return when (networkType) {
            "WiFi" -> "Network error. Your WiFi is connected but the internet may not be working. Please check your router or try mobile data."
            "Mobile Data" -> "Network error. Your mobile data is connected but the internet may not be working. Please check your cellular signal or try WiFi."
            "VPN" -> "Network error. Your VPN connection may be preventing proper internet access. Try disconnecting from VPN."
            else -> "Network error. Please check your internet connection and try again."
        }
    }
    
    /**
     * Get more detailed diagnostic information about current network status
     */
    fun getNetworkDiagnosticInfo(): String {
        val isNetworkAvailable = isNetworkAvailable()
        val networkType = getNetworkType()
        val networkQuality = getNetworkQuality()
        
        return """
            Network Available: $isNetworkAvailable
            Network Type: $networkType
            Network Quality: ${networkQuality.quality}
            Bandwidth: ${String.format("%.2f", networkQuality.bandwidthMbps)} Mbps
            Internet Access: ${cachedInternetStatus}
            Last Checked: ${if (lastInternetCheck > 0) "${(System.currentTimeMillis() - lastInternetCheck) / 1000} seconds ago" else "Never"}
        """.trimIndent()
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
               "Quality: ${networkQuality.quality}, " +
               "Bandwidth: ${String.format("%.2f", networkQuality.bandwidthMbps)} Mbps"
    }
}
