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
                // Dynamically determine which implementation to use based on the flavor
                val flavorAdsManagerClass = try {
                    // Try to load the FreeAdsManager class first
                    Class.forName("com.antbear.pwneyes.util.FreeAdsManager")
                } catch (e: ClassNotFoundException) {
                    try {
                        // If that fails, try the PaidAdsManager
                        Class.forName("com.antbear.pwneyes.util.PaidAdsManager")
                    } catch (e: ClassNotFoundException) {
                        null
                    }
                }
                
                // If we found a valid implementation class, call its initialize method
                flavorAdsManagerClass?.let { clazz ->
                    val initializeMethod = clazz.getDeclaredMethod("initialize", Context::class.java, BillingManager::class.java)
                    initializeMethod.invoke(null, context, billingManager)
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
