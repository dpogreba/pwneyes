package com.antbear.pwneyes.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.Purchase.PurchaseState
import com.antbear.pwneyes.util.Constants
import kotlinx.coroutines.*

/**
 * Handles all in-app purchase functionality including connecting to Google Play Billing service,
 * querying products, making purchases, and verifying purchase state.
 */
class BillingManager(private val context: Context) {
    companion object {
        private const val TAG = "BillingManager"
        const val REMOVE_ADS_PRODUCT_ID = Constants.Billing.REMOVE_ADS_PRODUCT_ID
        
        // Billing connection states
        const val STATE_DISCONNECTED = Constants.Billing.STATE_DISCONNECTED
        const val STATE_CONNECTING = Constants.Billing.STATE_CONNECTING
        const val STATE_CONNECTED = Constants.Billing.STATE_CONNECTED
        const val STATE_ERROR = Constants.Billing.STATE_ERROR
    }

    private val _premiumStatus = MutableLiveData<Boolean>()
    val premiumStatus: LiveData<Boolean> = _premiumStatus
    
    // Track billing service connection state
    private val _connectionState = MutableLiveData<Int>()
    val connectionState: LiveData<Int> = _connectionState
    
    // Track the latest error message for diagnostics
    private val _lastErrorMessage = MutableLiveData<String>()
    val lastErrorMessage: LiveData<String> = _lastErrorMessage
    
    private var connectionAttempts = 0
    private var maxRetryAttempts = Constants.Billing.MAX_RETRY_ATTEMPTS
    private var connectingTimeoutJob: Job? = null
    private var periodicReconnectionJob: Job? = null
    private var billingClient: BillingClient? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    // Initialize purchasesUpdatedListener BEFORE it's used in setupBillingClient
    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
            Log.d(TAG, "Purchase updated: ${purchases.size} purchases")
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "Purchase canceled by user")
        } else {
            Log.e(TAG, "Purchase failed: ${billingResult.responseCode}")
            Toast.makeText(
                context,
                "Purchase failed. Please try again later.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    init {
        Log.d(TAG, "BillingManager initialization started")
        _premiumStatus.value = false
        _connectionState.value = STATE_DISCONNECTED
        _lastErrorMessage.value = ""
        
        try {
            setupBillingClient()
        } catch (e: Exception) {
            Log.e(TAG, "Critical error during BillingManager initialization", e)
            _connectionState.value = STATE_ERROR
            _lastErrorMessage.value = "Initialization error: ${e.message}"
            // We'll continue operating with a disabled billing client
        }
    }

    private fun setupBillingClient() {
        Log.d(TAG, "Setting up billing client")
        try {
            billingClient = BillingClient.newBuilder(context)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases(
                    PendingPurchasesParams.newBuilder()
                        .enableOneTimeProducts()
                        .build()
                )
                .build()

            connectToPlayBilling()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up billing client", e)
            coroutineScope.launch(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Failed to set up in-app purchases. Some features may be unavailable.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Force a new connection attempt to Google Play Billing
     * This is called automatically and no longer exposed in the UI
     */
    fun retryConnection() {
        Log.d(TAG, "Auto retry connection initiated")
        
        // Cancel any existing timeout
        connectingTimeoutJob?.cancel()
        connectingTimeoutJob = null
        
        // Reset connection attempts to give full retry quota
        connectionAttempts = 0
        
        // Set to disconnected state first to ensure a clean start
        _connectionState.value = STATE_DISCONNECTED
        _lastErrorMessage.value = "Retrying connection..."
        
        // Start new connection attempt
        connectToPlayBilling()
    }
    
    /**
     * Start periodic reconnection attempts in the background
     * This ensures billing will eventually connect even after max retries
     */
    private fun startPeriodicReconnection() {
        // Cancel any existing periodic job
        periodicReconnectionJob?.cancel()
        
        // Start a new periodic job that attempts reconnection every 2 minutes
        periodicReconnectionJob = coroutineScope.launch {
            while (isActive) {
                delay(Constants.Billing.PERIODIC_RECONNECTION_INTERVAL_MS)
                
                // Only attempt reconnection if we're not already connected or connecting
                if (_connectionState.value != STATE_CONNECTED && 
                    _connectionState.value != STATE_CONNECTING) {
                    
                    Log.d(TAG, "Periodic reconnection attempt")
                    connectionAttempts = 0 // Reset connection attempts for a fresh start
                    connectToPlayBilling()
                }
            }
        }
    }
    
    private fun connectToPlayBilling() {
        Log.d(TAG, "Connecting to Google Play Billing")
        _connectionState.value = STATE_CONNECTING
        connectionAttempts++
        
        // Cancel any existing timeout job
        connectingTimeoutJob?.cancel()
        
        // Create a new timeout job - will change to error state if connection takes too long
        connectingTimeoutJob = coroutineScope.launch {
            delay(Constants.Billing.CONNECTION_TIMEOUT_MS)
            if (_connectionState.value == STATE_CONNECTING) {
                Log.e(TAG, "Billing connection timeout after ${Constants.Billing.CONNECTION_TIMEOUT_MS/1000} seconds")
                _connectionState.value = STATE_ERROR
                _lastErrorMessage.value = "Connection timeout. App may not be published on Google Play yet."
            }
        }
        
        try {
            billingClient?.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingResponseCode.OK) {
                        Log.d(TAG, "Billing client connected successfully")
                        _connectionState.value = STATE_CONNECTED
                        _lastErrorMessage.value = ""
                        connectionAttempts = 0 // Reset the counter on success
                        
                        // Check if user already has premium status
                        queryPurchases()
                    } else {
                        val errorMessage = when (billingResult.responseCode) {
                            BillingResponseCode.BILLING_UNAVAILABLE -> 
                                "Billing unavailable. Google Play Store may need to be updated."
                            BillingResponseCode.DEVELOPER_ERROR -> 
                                "Developer error. Please contact support."
                            BillingResponseCode.FEATURE_NOT_SUPPORTED -> 
                                "Feature not supported on this device."
                            BillingResponseCode.ITEM_UNAVAILABLE -> 
                                "Item unavailable. Product may not be configured properly."
                            BillingResponseCode.SERVICE_DISCONNECTED -> 
                                "Service disconnected. Please check your internet connection."
                            @Suppress("DEPRECATION")
                            BillingResponseCode.SERVICE_TIMEOUT -> 
                                "Service timeout. Please try again later."
                            BillingResponseCode.SERVICE_UNAVAILABLE -> 
                                "Google Play services unavailable."
                            BillingResponseCode.USER_CANCELED -> 
                                "User canceled the operation."
                            else -> "In-app purchase initialization failed: ${billingResult.responseCode}"
                        }
                        
                        _connectionState.value = STATE_ERROR
                        _lastErrorMessage.value = errorMessage
                        
                        Log.e(TAG, "Billing client connection failed: ${billingResult.responseCode}")
                        Log.e(TAG, "Debug message: ${billingResult.debugMessage}")
                        Log.e(TAG, "Friendly error message: $errorMessage")
                        
                        // Show a toast to the user with a more friendly message
                        coroutineScope.launch(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "Failed to initialize billing: $errorMessage",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        
                        // Try to reconnect if we haven't exceeded maximum attempts
                        if (connectionAttempts < maxRetryAttempts) {
                            coroutineScope.launch {
                                // Exponential backoff: wait longer between consecutive retries
                                val delayTime = Constants.Billing.RETRY_DELAY_BASE_MS * (1 shl (connectionAttempts - 1))
                                Log.d(TAG, "Retrying connection attempt $connectionAttempts of $maxRetryAttempts in ${delayTime/1000} seconds")
                                delay(delayTime) // Wait with exponential backoff
                                connectToPlayBilling()
                            }
                        } else {
                            // Start periodic reconnection attempts for long-term recovery
                            Log.d(TAG, "Maximum immediate retries exceeded, switching to periodic reconnection")
                            startPeriodicReconnection()
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Log.d(TAG, "Billing service disconnected")
                    _connectionState.value = STATE_DISCONNECTED
                    
                    // Try to reconnect, but not immediately (could cause an infinite loop)
                    coroutineScope.launch {
                        delay(Constants.Billing.RECONNECT_DELAY_MS)
                        Log.d(TAG, "Attempting to reconnect after disconnect")
                        connectionAttempts = 0 // Reset connection attempts on disconnect
                        connectToPlayBilling()
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to Play Billing", e)
            _connectionState.value = STATE_ERROR
            _lastErrorMessage.value = "Connection error: ${e.message}"
        }
    }

    private fun queryPurchases() {
        Log.d(TAG, "Querying existing purchases")
        
        if (billingClient?.isReady != true) {
            Log.e(TAG, "Billing client is not ready")
            return
        }
        
        coroutineScope.launch {
            try {
                val params = QueryPurchasesParams.newBuilder()
                    .setProductType(ProductType.INAPP)
                    .build()

                billingClient?.let { client ->
                    val purchasesResult = client.queryPurchasesAsync(params)
                    val billingResult = purchasesResult.billingResult
                    val purchasesList = purchasesResult.purchasesList
                    
                    if (billingResult.responseCode == BillingResponseCode.OK) {
                        Log.d(TAG, "Found ${purchasesList.size} existing purchases")
                        for (purchase in purchasesList) {
                            handlePurchase(purchase)
                        }
                    } else {
                        Log.e(TAG, "Failed to query purchases: ${billingResult.responseCode}")
                    }
                } ?: Log.e(TAG, "Billing client is null")
            } catch (e: Exception) {
                Log.e(TAG, "Error querying purchases", e)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        Log.d(TAG, "Handling purchase: ${purchase.products.joinToString()}")
        
        if (purchase.purchaseState == PurchaseState.PURCHASED) {
            // Grant entitlement for the purchased product
            if (purchase.products.contains(REMOVE_ADS_PRODUCT_ID)) {
                Log.d(TAG, "Remove ads purchase detected")
                
                // Acknowledge the purchase if it hasn't been acknowledged yet
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase.purchaseToken)
                }
                
                // Update premium status
                _premiumStatus.value = true
            }
        }
    }

    private fun acknowledgePurchase(purchaseToken: String) {
        Log.d(TAG, "Acknowledging purchase")
        
        if (billingClient?.isReady != true) {
            Log.e(TAG, "Billing client is not ready")
            return
        }
        
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
            
        coroutineScope.launch {
            try {
                billingClient?.let { client ->
                    val acknowledgePurchaseResult = client.acknowledgePurchase(params)
                    
                    if (acknowledgePurchaseResult.responseCode == BillingResponseCode.OK) {
                        Log.d(TAG, "Purchase acknowledged successfully")
                    } else {
                        Log.e(TAG, "Failed to acknowledge purchase: ${acknowledgePurchaseResult.responseCode}")
                    }
                } ?: Log.e(TAG, "Billing client is null")
            } catch (e: Exception) {
                Log.e(TAG, "Error acknowledging purchase", e)
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        Log.d(TAG, "Launching purchase flow for: $REMOVE_ADS_PRODUCT_ID")
        
        if (billingClient?.isReady != true) {
            Log.e(TAG, "Billing client is not ready")
            Toast.makeText(
                context,
                "Billing service is not ready. Please try again later.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        coroutineScope.launch {
            try {
                val productList = listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(REMOVE_ADS_PRODUCT_ID)
                        .setProductType(ProductType.INAPP)
                        .build()
                )

                val params = QueryProductDetailsParams.newBuilder()
                    .setProductList(productList)
                    .build()

                billingClient?.let { client ->
                    val queryProductDetailsResult = client.queryProductDetails(params)
                    
                    val billingResult = queryProductDetailsResult.billingResult
                    val productDetailsList = queryProductDetailsResult.productDetailsList
                    
                    if (billingResult.responseCode == BillingResponseCode.OK && 
                        !productDetailsList.isNullOrEmpty()) {
                        
                        Log.d(TAG, "Product details retrieved, launching billing flow")
                        
                        val selectedProductDetails = productDetailsList.first()
                        
                        // One-time products don't have subscription offer details
                        val productDetailsParamsList = listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(selectedProductDetails)
                                .build()
                        )

                        val billingFlowParams = BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(productDetailsParamsList)
                            .build()
                            
                        val flowResult = client.launchBillingFlow(activity, billingFlowParams)
                        
                        if (flowResult.responseCode != BillingResponseCode.OK) {
                            Log.e(TAG, "Failed to launch billing flow: ${flowResult.responseCode}")
                            Toast.makeText(
                                context,
                                "Failed to start purchase. Please try again later.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Log.e(TAG, "No product details found for $REMOVE_ADS_PRODUCT_ID. Response code: ${billingResult.responseCode}")
                        Toast.makeText(
                            context,
                            "Product not available. Please try again later.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } ?: run {
                    Log.e(TAG, "Billing client is null")
                    Toast.makeText(
                        context,
                        "Billing service is not available. Please try again later.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error launching purchase flow", e)
                Toast.makeText(
                    context,
                    "Error launching purchase flow. Please try again later.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun restorePurchases() {
        Log.d(TAG, "Restoring purchases")
        queryPurchases()
    }

    fun release() {
        Log.d(TAG, "Releasing billing client")
        billingClient?.endConnection()
        coroutineScope.cancel()
    }
}
