package com.antbear.pwneyes.util

import android.content.Context
import android.view.View
import android.view.ViewGroup

class AdsManager {
    companion object {
        fun initialize(context: Context) {
            // No-op in paid version
        }

        fun loadBannerAd(adContainer: ViewGroup) {
            // Hide the container in paid version
            adContainer.visibility = View.GONE
        }
    }
} 