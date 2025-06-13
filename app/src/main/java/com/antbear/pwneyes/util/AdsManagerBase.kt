package com.antbear.pwneyes.util

import android.content.Context
import android.view.ViewGroup
import com.antbear.pwneyes.billing.BillingManager

/**
 * Base interface for AdsManager implementations.
 * This allows different implementations for free and paid flavors.
 */
interface AdsManagerBase {
    /**
     * Enable or disable premium features for debugging purposes
     */
    fun setDebugPremiumMode(enabled: Boolean)

    /**
     * Load banner ad into the specified container
     */
    fun loadBannerAd(adContainer: ViewGroup)
    
    /**
     * Clean up resources when the app is destroyed
     */
    fun cleanup()
    
    companion object {
        private const val TAG = "AdsManagerBase"
        
        @Volatile
        internal var INSTANCE: AdsManagerBase? = null
        
        fun initialize(context: Context, billingManager: BillingManager?) {
            try {
                // Directly try both implementations with explicit try/catch blocks for better error handling
                try {
                    // Try FreeAdsManager first
                    val freeManagerClass = Class.forName("com.antbear.pwneyes.util.FreeAdsManager")
                    android.util.Log.d(TAG, "Found FreeAdsManager class")
                    val initMethod = freeManagerClass.getDeclaredMethod("initialize", Context::class.java, BillingManager::class.java)
                    android.util.Log.d(TAG, "Found initialize method in FreeAdsManager")
                    initMethod.invoke(null, context, billingManager)
                    android.util.Log.d(TAG, "Successfully initialized FreeAdsManager")
                    return
                } catch (e: ClassNotFoundException) {
                    android.util.Log.d(TAG, "FreeAdsManager class not found, trying PaidAdsManager")
                } catch (e: NoSuchMethodException) {
                    android.util.Log.e(TAG, "initialize method not found in FreeAdsManager", e)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Error initializing FreeAdsManager", e)
                }
                
                try {
                    // Try PaidAdsManager if FreeAdsManager failed
                    val paidManagerClass = Class.forName("com.antbear.pwneyes.util.PaidAdsManager")
                    android.util.Log.d(TAG, "Found PaidAdsManager class")
                    val initMethod = paidManagerClass.getDeclaredMethod("initialize", Context::class.java, BillingManager::class.java)
                    android.util.Log.d(TAG, "Found initialize method in PaidAdsManager")
                    initMethod.invoke(null, context, billingManager)
                    android.util.Log.d(TAG, "Successfully initialized PaidAdsManager")
                } catch (e: ClassNotFoundException) {
                    android.util.Log.e(TAG, "PaidAdsManager class not found", e)
                } catch (e: NoSuchMethodException) {
                    android.util.Log.e(TAG, "initialize method not found in PaidAdsManager", e)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Error initializing PaidAdsManager", e)
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error initializing AdsManager", e)
            }
        }
        
        fun getInstance(): AdsManagerBase? {
            return INSTANCE
        }
    }
}
