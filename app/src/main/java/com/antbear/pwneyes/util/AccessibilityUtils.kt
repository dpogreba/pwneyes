package com.antbear.pwneyes.util

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.antbear.pwneyes.R
import java.util.Locale

/**
 * Utility class for managing accessibility features throughout the app.
 * Handles font size, contrast settings, text-to-speech, and other accessibility features.
 */
class AccessibilityUtils(private val context: Context) {
    private val TAG = "AccessibilityUtils"
    
    // Shared preferences for storing/retrieving accessibility settings
    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    
    // Text-to-speech engine
    private var textToSpeech: TextToSpeech? = null
    private var ttsInitialized = false
    
    companion object {
        // Font scale factors
        private const val FONT_SCALE_SMALL = 0.85f
        private const val FONT_SCALE_NORMAL = 1.0f
        private const val FONT_SCALE_LARGE = 1.25f
        private const val FONT_SCALE_EXTRA_LARGE = 1.5f
        
        // Touch target scaling
        private const val TOUCH_TARGET_NORMAL = 1.0f
        private const val TOUCH_TARGET_LARGE = 1.25f
        
        // Minimum touch target size in dp (as per WCAG guidelines)
        private const val MIN_TOUCH_TARGET_SIZE_DP = 48
    }
    
    init {
        // Initialize TTS if enabled
        if (isTextToSpeechEnabled()) {
            initializeTextToSpeech()
        }
    }
    
    /**
     * Initialize Text-to-Speech engine
     */
    private fun initializeTextToSpeech() {
        try {
            textToSpeech = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = textToSpeech?.setLanguage(Locale.getDefault())
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "Language not supported for TTS")
                        ttsInitialized = false
                    } else {
                        ttsInitialized = true
                        Log.d(TAG, "TTS initialized successfully")
                    }
                } else {
                    Log.e(TAG, "TTS initialization failed with status: $status")
                    ttsInitialized = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TTS", e)
            ttsInitialized = false
        }
    }
    
    /**
     * Shutdown and clean up resources
     */
    fun shutdown() {
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            ttsInitialized = false
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }
    }
    
    /**
     * Check if font size adjustment is enabled and get the selected size
     */
    fun getFontSizeScale(): Float {
        val fontSizePref = preferences.getString("font_size_preference", "normal") ?: "normal"
        return when (fontSizePref) {
            "small" -> FONT_SCALE_SMALL
            "large" -> FONT_SCALE_LARGE
            "extra_large" -> FONT_SCALE_EXTRA_LARGE
            else -> FONT_SCALE_NORMAL
        }
    }
    
    /**
     * Check if high contrast mode is enabled
     */
    fun isHighContrastEnabled(): Boolean {
        return preferences.getBoolean("high_contrast_mode", false)
    }
    
    /**
     * Check if text-to-speech is enabled
     */
    fun isTextToSpeechEnabled(): Boolean {
        return preferences.getBoolean("text_to_speech", false)
    }
    
    /**
     * Check if larger touch targets are enabled
     */
    fun isLargerTouchTargetsEnabled(): Boolean {
        return preferences.getBoolean("larger_touch_targets", false)
    }
    
    /**
     * Check if enhanced screen reader support is enabled
     */
    fun isScreenReaderSupportEnabled(): Boolean {
        return preferences.getBoolean("screen_reader_support", false)
    }
    
    /**
     * Check if reduced animations are enabled
     */
    fun isReduceAnimationsEnabled(): Boolean {
        return preferences.getBoolean("reduce_animations", false)
    }
    
    /**
     * Speak the provided text using text-to-speech
     */
    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (!isTextToSpeechEnabled() || !ttsInitialized || textToSpeech == null) {
            return
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech?.speak(text, queueMode, null, "PwnEyes_${System.currentTimeMillis()}")
            } else {
                @Suppress("DEPRECATION")
                textToSpeech?.speak(text, queueMode, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error using TTS to speak text: $text", e)
        }
    }
    
    /**
     * Apply font size scaling to a TextView
     */
    fun applyFontSize(textView: TextView) {
        try {
            val currentSize = textView.textSize / context.resources.displayMetrics.scaledDensity
            val fontScale = getFontSizeScale()
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentSize * fontScale)
        } catch (e: Exception) {
            Log.e(TAG, "Error applying font size", e)
        }
    }
    
    /**
     * Apply high contrast if enabled
     */
    fun applyHighContrast(view: View) {
        if (!isHighContrastEnabled()) return
        
        try {
            when (view) {
                is TextView -> {
                    // Use highly contrasting colors
                    view.setTextColor(ContextCompat.getColor(context, R.color.black))
                    view.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
                }
                is ViewGroup -> {
                    view.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
                    // Apply to all children
                    for (i in 0 until view.childCount) {
                        applyHighContrast(view.getChildAt(i))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying high contrast", e)
        }
    }
    
    /**
     * Apply larger touch targets to interactive elements
     */
    fun applyLargerTouchTargets(view: View) {
        if (!isLargerTouchTargetsEnabled()) return
        
        try {
            // Check if view has click listener (interactive)
            if (view.hasOnClickListeners()) {
                // Get current dimensions
                val layoutParams = view.layoutParams
                
                // Convert min touch target size to pixels
                val minSizePx = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 
                    MIN_TOUCH_TARGET_SIZE_DP.toFloat(),
                    context.resources.displayMetrics
                ).toInt()
                
                // Ensure width and height are at least the minimum
                if (layoutParams.width > 0 && layoutParams.width < minSizePx) {
                    layoutParams.width = minSizePx
                }
                if (layoutParams.height > 0 && layoutParams.height < minSizePx) {
                    layoutParams.height = minSizePx
                }
                
                view.layoutParams = layoutParams
                
                // If padding is too small, increase it
                val totalHorizontalPadding = view.paddingLeft + view.paddingRight
                val totalVerticalPadding = view.paddingTop + view.paddingBottom
                
                if (totalHorizontalPadding < 16) {
                    view.setPadding(view.paddingLeft + 4, view.paddingTop, 
                                    view.paddingRight + 4, view.paddingBottom)
                }
                
                if (totalVerticalPadding < 16) {
                    view.setPadding(view.paddingLeft, view.paddingTop + 4, 
                                    view.paddingRight, view.paddingBottom + 4)
                }
            }
            
            // Apply to all children if it's a ViewGroup
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    applyLargerTouchTargets(view.getChildAt(i))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying larger touch targets", e)
        }
    }
    
    /**
     * Apply screen reader support enhancements
     */
    fun applyScreenReaderSupport(view: View, contentDescription: String? = null) {
        if (!isScreenReaderSupportEnabled()) return
        
        try {
            // Set content description if provided
            if (!contentDescription.isNullOrEmpty()) {
                view.contentDescription = contentDescription
            }
            
            // Make sure view is accessible to screen readers
            view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            
            // If it's a ViewGroup, apply to all children
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    applyScreenReaderSupport(view.getChildAt(i))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying screen reader support", e)
        }
    }
    
    /**
     * Check if the device has a screen reader enabled
     */
    fun isScreenReaderActive(context: Context): Boolean {
        try {
            val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager
            return accessibilityManager?.isEnabled == true && 
                   accessibilityManager.isTouchExplorationEnabled
        } catch (e: Exception) {
            Log.e(TAG, "Error checking screen reader status", e)
            return false
        }
    }
    
    /**
     * Utility method to get configuration for proper font scaling
     */
    fun getConfigurationWithFontScale(baseConfig: Configuration): Configuration {
        val fontScale = getFontSizeScale()
        val config = Configuration(baseConfig)
        config.fontScale = fontScale
        return config
    }
    
    /**
     * Apply all accessibility settings to a view and its children
     */
    fun applyAccessibilitySettings(view: View, contentDescription: String? = null) {
        try {
            if (view is TextView) {
                applyFontSize(view)
            }
            
            if (isHighContrastEnabled()) {
                applyHighContrast(view)
            }
            
            if (isLargerTouchTargetsEnabled()) {
                applyLargerTouchTargets(view)
            }
            
            if (isScreenReaderSupportEnabled()) {
                applyScreenReaderSupport(view, contentDescription)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying accessibility settings", e)
        }
    }
    
    /**
     * Create resources with adjusted font scaling
     */
    fun getResourcesWithFontScale(context: Context): Resources {
        val configuration = Configuration(context.resources.configuration)
        configuration.fontScale = getFontSizeScale()
        return context.createConfigurationContext(configuration).resources
    }
}
