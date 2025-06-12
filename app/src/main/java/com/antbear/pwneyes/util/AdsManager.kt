package com.antbear.pwneyes.util

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.antbear.pwneyes.BuildConfig
import com.antbear.pwneyes.billing.BillingManager
import com.google.android.gms.ads.*

/**
 * Manages ad loading and display, with premium status awareness.
 * This unified implementation replaces the separate free/paid flavor implementations.
 */
class AdsManager private constructor(
    private val context: Context,
    private val billingManager: BillingManager?
) {
    companion object {
        // Test ad unit ID for development
        private const val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
        // Your real ad unit ID for production
        private const val REAL_AD_UNIT_ID = "ca-app-pub-7195221690948786/7139615263"
        private const val TAG = "AdsManager"
        
        @Volatile
        private var INSTANCE: AdsManager? = null
        
        fun initialize(context: Context, billingManager: BillingManager?) {
            Log.d(TAG, "Initializing AdsManager")
            try {
                if (INSTANCE == null) {
                    synchronized(this) {
                        if (INSTANCE == null) {
                            INSTANCE = AdsManager(context.applicationContext, billingManager)
                            INSTANCE?.initializeMobileAds()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing AdsManager", e)
            }
        }
        
        fun getInstance(): AdsManager? {
            return INSTANCE
        }
    }
    
    private var isPremium = false
    private var isDebugPremium = false
    private var premiumObserver: Observer<Boolean>? = null
    
    init {
        try {
            // Observe premium status changes
            billingManager?.let { manager ->
                premiumObserver = Observer<Boolean> { premium ->
                    this.isPremium = premium
                    Log.d(TAG, "Premium status updated: $isPremium")
                }
                
                premiumObserver?.let { observer ->
                    manager.premiumStatus.observeForever(observer)
                }
            } ?: run {
                Log.d(TAG, "BillingManager is null, defaulting to non-premium")
                isPremium = false
            }
            
            // Check for debug premium mode
            val sharedPrefs = context.getSharedPreferences("com.antbear.pwneyes_preferences", Context.MODE_PRIVATE)
            isDebugPremium = sharedPrefs.getBoolean("debug_premium", false)
            if (isDebugPremium) {
                Log.d(TAG, "Debug premium mode is enabled")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up premium status observer", e)
            isPremium = false
            isDebugPremium = false
        }
    }
    
    /**
     * Enable or disable premium features for debugging purposes
     * This allows testing premium features without an actual purchase
     */
    fun setDebugPremiumMode(enabled: Boolean) {
        try {
            isDebugPremium = enabled
            Log.d(TAG, "Debug premium mode set to: $enabled")
            
            // Save the setting to preferences
            val sharedPrefs = context.getSharedPreferences("com.antbear.pwneyes_preferences", Context.MODE_PRIVATE)
            sharedPrefs.edit().putBoolean("debug_premium", enabled).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting debug premium mode", e)
        }
    }
    
    private fun initializeMobileAds() {
        try {
            // If premium (either purchased or debug), don't initialize ads
            if (isPremium || isDebugPremium) {
                Log.d(TAG, "User has premium status (purchased or debug), skipping ad initialization")
                return
            }
            
            Log.d(TAG, "Initializing MobileAds...")
            
            // Set up test devices configuration
            val configuration = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                .build()
            MobileAds.setRequestConfiguration(configuration)
            Log.d(TAG, "Test device configuration set")

            MobileAds.initialize(context) { initializationStatus ->
                Log.d(TAG, "MobileAds initialization callback received")
                // Log initialization status
                val statusMap = initializationStatus.adapterStatusMap
                statusMap.forEach { (adapter, status) ->
                    Log.d(TAG, String.format(
                        "Adapter: %s, Status: %s, Latency: %d",
                        adapter, status.initializationState, status.latency
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MobileAds: ${e.message}", e)
        }
    }

    fun loadBannerAd(adContainer: ViewGroup) {
        // Skip loading ad if user has premium status (either purchased or debug mode)
        if (isPremium || isDebugPremium) {
            val source = if (isPremium) "premium status" else "debug mode"
            Log.d(TAG, "User has $source, hiding ad container")
            try {
                adContainer.visibility = ViewGroup.GONE
                adContainer.removeAllViews()
            } catch (e: Exception) {
                Log.e(TAG, "Error hiding ad container", e)
            }
            return
        }
        
        try {
            Log.d(TAG, "Starting to load banner ad...")
            
            val adView = AdView(adContainer.context)
            // Use test ad unit ID for development, real one for production
            val adUnitId = if (BuildConfig.DEBUG) TEST_AD_UNIT_ID else REAL_AD_UNIT_ID
            adView.adUnitId = adUnitId
            Log.d(TAG, "Using ad unit ID: $adUnitId")
            
            // Set the banner size
            val adSize = AdSize.LARGE_BANNER
            adView.setAdSize(adSize)
            Log.d(TAG, "Ad size set to: ${adSize.width}x${adSize.height} (LARGE_BANNER)")
            
            val adRequest = AdRequest.Builder().build()
            
            // Add ad listener for better debugging
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d(TAG, "Ad loaded successfully")
                    try {
                        adContainer.visibility = ViewGroup.VISIBLE
                        Log.d(TAG, "Ad container made visible")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error making ad container visible", e)
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Ad failed to load. Error code: ${error.code}")
                    Log.e(TAG, "Error message: ${error.message}")
                    Log.e(TAG, "Error domain: ${error.domain}")
                    try {
                        adContainer.visibility = ViewGroup.GONE
                    } catch (e: Exception) {
                        Log.e(TAG, "Error hiding ad container after load failure", e)
                    }
                }

                override fun onAdOpened() {
                    Log.d(TAG, "Ad opened")
                }

                override fun onAdClosed() {
                    Log.d(TAG, "Ad closed")
                }

                override fun onAdImpression() {
                    Log.d(TAG, "Ad impression recorded")
                }
            }
            
            try {
                // Clear any existing views and add the new ad view
                adContainer.removeAllViews()
                adContainer.addView(adView)
                Log.d(TAG, "Ad view added to container")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding ad view to container", e)
                return
            }
            
            // Load the ad
            try {
                adView.loadAd(adRequest)
                Log.d(TAG, "Ad load request sent")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading ad", e)
                adContainer.visibility = ViewGroup.GONE
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in loadBannerAd: ${e.message}", e)
            try {
                adContainer.visibility = ViewGroup.GONE
            } catch (ex: Exception) {
                Log.e(TAG, "Error hiding ad container after exception", ex)
            }
        }
    }
    
    // Clean up when the app is destroyed
    fun cleanup() {
        try {
            billingManager?.let { manager ->
                premiumObserver?.let { observer ->
                    manager.premiumStatus.removeObserver(observer)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}
