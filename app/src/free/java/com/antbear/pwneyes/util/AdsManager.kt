package com.antbear.pwneyes.util

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.antbear.pwneyes.billing.BillingManager
import com.google.android.gms.ads.*

/**
 * Manages ad loading and display, with premium status awareness
 */
class AdsManager private constructor(
    private val context: Context,
    private val billingManager: BillingManager
) {
    companion object {
        // Test ad unit ID for development
        private const val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
        // Your real ad unit ID for production
        private const val REAL_AD_UNIT_ID = "ca-app-pub-7195221690948786/7139615263"
        private const val TAG = "AdsManager"
        
        @Volatile
        private var INSTANCE: AdsManager? = null
        
        fun initialize(context: Context, billingManager: BillingManager) {
            Log.d(TAG, "Initializing AdsManager")
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = AdsManager(context.applicationContext, billingManager)
                        INSTANCE?.initializeMobileAds()
                    }
                }
            }
        }
        
        fun getInstance(): AdsManager {
            return INSTANCE ?: throw IllegalStateException("AdsManager must be initialized first")
        }
    }
    
    private var isPremium = false
    
    init {
        // Observe premium status changes
        billingManager.premiumStatus.observeForever { isPremium ->
            this.isPremium = isPremium
            Log.d(TAG, "Premium status updated: $isPremium")
        }
    }
    
    private fun initializeMobileAds() {
        try {
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
        // Skip loading ad if user has premium status
        if (isPremium) {
            Log.d(TAG, "User has premium status, hiding ad container")
            adContainer.visibility = ViewGroup.GONE
            adContainer.removeAllViews()
            return
        }
        
        try {
            Log.d(TAG, "Starting to load banner ad...")
            
            val adView = AdView(adContainer.context)
            // Use test ad unit ID for development
            adView.adUnitId = TEST_AD_UNIT_ID
            Log.d(TAG, "Using test ad unit ID: $TEST_AD_UNIT_ID")
            
            // Set the banner size
            val adSize = AdSize.LARGE_BANNER
            adView.setAdSize(adSize)
            Log.d(TAG, "Ad size set to: ${adSize.width}x${adSize.height} (LARGE_BANNER)")
            
            val adRequest = AdRequest.Builder().build()
            
            // Add ad listener for better debugging
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d(TAG, "Ad loaded successfully")
                    adContainer.visibility = ViewGroup.VISIBLE
                    Log.d(TAG, "Ad container made visible")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Ad failed to load. Error code: ${error.code}")
                    Log.e(TAG, "Error message: ${error.message}")
                    Log.e(TAG, "Error domain: ${error.domain}")
                    adContainer.visibility = ViewGroup.GONE
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
            
            // Clear any existing views and add the new ad view
            adContainer.removeAllViews()
            adContainer.addView(adView)
            Log.d(TAG, "Ad view added to container")
            
            // Load the ad
            adView.loadAd(adRequest)
            Log.d(TAG, "Ad load request sent")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading banner ad: ${e.message}", e)
        }
    }
}
