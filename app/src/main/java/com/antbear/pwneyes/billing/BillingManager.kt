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
                    Log.d(TAG, "Billing client connected")
                    // Check if user already has premium status
                    queryPurchases()
                } else {
                    Log.e(TAG, "Billing client connection failed: ${billingResult.responseCode}")
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
        coroutineScope.launch {
            try {
                val params = QueryPurchasesParams.newBuilder()
                    .setProductType(ProductType.INAPP)
                    .build()

                val purchasesResult = billingClient?.queryPurchasesAsync(params)
                purchasesResult?.purchasesList?.let { purchases ->
                    Log.d(TAG, "Found ${purchases.size} existing purchases")
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                }
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
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
            
        coroutineScope.launch {
            try {
                val result = billingClient?.acknowledgePurchase(params)
                if (result?.responseCode == BillingResponseCode.OK) {
                    Log.d(TAG, "Purchase acknowledged successfully")
                } else {
                    Log.e(TAG, "Failed to acknowledge purchase: ${result?.responseCode}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error acknowledging purchase", e)
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        Log.d(TAG, "Launching purchase flow for: $REMOVE_ADS_PRODUCT_ID")
        
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

                val productDetailsResult = billingClient?.queryProductDetailsAsync(params)
                
                productDetailsResult?.productDetailsList?.let { productDetails ->
                    if (productDetails.isNotEmpty()) {
                        Log.d(TAG, "Product details retrieved, launching billing flow")
                        
                        val selectedProductDetails = productDetails.first()
                        
                        val offerToken = selectedProductDetails.subscriptionOfferDetails?.get(0)?.offerToken
                        
                        val productDetailsParamsList = listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(selectedProductDetails)
                                .apply {
                                    if (offerToken != null) {
                                        setOfferToken(offerToken)
                                    }
                                }
                                .build()
                        )

                        val billingFlowParams = BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(productDetailsParamsList)
                            .build()
                            
                        billingClient?.launchBillingFlow(activity, billingFlowParams)
                    } else {
                        Log.e(TAG, "No product details found for $REMOVE_ADS_PRODUCT_ID")
                        Toast.makeText(
                            context,
                            "Product not available. Please try again later.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } ?: run {
                    Log.e(TAG, "Failed to retrieve product details")
                    Toast.makeText(
                        context,
                        "Failed to retrieve product details. Please try again later.",
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
