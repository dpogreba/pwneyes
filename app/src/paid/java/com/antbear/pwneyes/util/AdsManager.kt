package com.antbear.pwneyes.util

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import com.antbear.pwneyes.billing.BillingManager

/**
 * Paid flavor implementation of AdsManagerBase that doesn't show ads
 */
class AdsManager private constructor(
    private val context: Context,
    private val billingManager: BillingManager?
) : AdsManagerBase {
    companion object {
        private const val TAG = "AdsManager"
        
        @Volatile
        private var INSTANCE: AdsManagerBase? = null
        
        fun initialize(context: Context, billingManager: BillingManager?) {
            Log.d(TAG, "Initializing AdsManager (Paid Version)")
            try {
                if (INSTANCE == null) {
                    synchronized(this) {
                        if (INSTANCE == null) {
                            val manager = AdsManager(context.applicationContext, billingManager)
                            INSTANCE = manager
                            
                            // Also set in the base class for access through AdsManagerBase.getInstance()
                            AdsManagerBase.Companion.INSTANCE = manager
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing AdsManager", e)
            }
        }
        
        fun getInstance(): AdsManagerBase? {
            return INSTANCE
        }
    }
    
    /**
     * Enable or disable premium features for debugging purposes
     * This is a no-op in the paid version
     */
    override fun setDebugPremiumMode(enabled: Boolean) {
        // No-op in paid version - paid version is always premium
    }

    /**
     * Load banner ad
     * This is a no-op in paid version, we just hide the container
     */
    override fun loadBannerAd(adContainer: ViewGroup) {
        try {
            // Always hide ad containers in paid version
            adContainer.visibility = ViewGroup.GONE
            adContainer.removeAllViews()
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding ad container", e)
        }
    }
    
    // Clean up when the app is destroyed
    override fun cleanup() {
        // No-op in paid version
    }
}
