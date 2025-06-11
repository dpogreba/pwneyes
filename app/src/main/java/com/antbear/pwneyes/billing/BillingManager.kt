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
import kotlinx.coroutines.*

/**
 * Handles all in-app purchase functionality including connecting to Google Play Billing service,
 * querying products, making purchases, and verifying purchase state.
 */
class BillingManager(private val context: Context) {
    companion object {
        private const val TAG = "BillingManager"
        const val REMOVE_ADS_PRODUCT_ID = "remove_ads"
    }

    private val _premiumStatus = MutableLiveData<Boolean>()
    val premiumStatus: LiveData<Boolean> = _premiumStatus

    private var billingClient: BillingClient? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    init {
        Log.d(TAG, "BillingManager initialization started")
        _premiumStatus.value = false
        setupBillingClient()
    }

    private fun setupBillingClient() {
        Log.d(TAG, "Setting up billing client")
        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

        connectToPlayBilling()
    }

    private fun connectToPlayBilling() {
        Log.d(TAG, "Connecting to Google Play Billing")
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    Log.d(TAG, "Billing client connected successfully")
                    // Check if user already has premium status
                    queryPurchases()
                } else {
                    Log.e(TAG, "Billing client connection failed: ${billingResult.responseCode}")
                    Log.e(TAG, "Debug message: ${billingResult.debugMessage}")
                    
                    // Show a toast to the user
                    coroutineScope.launch(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "In-app purchase initialization failed: ${billingResult.responseCode}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "Billing service disconnected")
                // Try to reconnect
                connectToPlayBilling()
            }
        })
    }

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
