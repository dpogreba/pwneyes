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
            // Implementation will be provided by flavor-specific code
        }
        
        fun getInstance(): AdsManagerBase? {
            return INSTANCE
        }
    }
}
