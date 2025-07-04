package com.antbear.pwneyes.ui.viewer

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

/**
 * ViewModel to handle state persistence for viewer fragments
 * This replaces the deprecated 'retainInstance = true' approach
 */
class ViewerViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    
    companion object {
        private const val KEY_WEB_VIEW_STATE = "web_view_state"
        private const val KEY_LAST_URL = "last_url"
        private const val KEY_SCROLL_X = "scroll_x"
        private const val KEY_SCROLL_Y = "scroll_y"
        private const val KEY_WEB_VIEW_SCALE = "web_view_scale"
        private const val KEY_PLUGIN_STATES = "plugin_states"
    }

    // WebView state bundle
    var webViewState: Bundle?
        get() = savedStateHandle.get<Bundle>(KEY_WEB_VIEW_STATE)
        set(value) {
            savedStateHandle[KEY_WEB_VIEW_STATE] = value
        }

    // Last URL for restoration
    var lastUrl: String?
        get() = savedStateHandle.get<String>(KEY_LAST_URL)
        set(value) {
            savedStateHandle[KEY_LAST_URL] = value
        }

    // Scroll positions
    var lastScrollX: Int
        get() = savedStateHandle.get<Int>(KEY_SCROLL_X) ?: 0
        set(value) {
            savedStateHandle[KEY_SCROLL_X] = value
        }

    var lastScrollY: Int
        get() = savedStateHandle.get<Int>(KEY_SCROLL_Y) ?: 0
        set(value) {
            savedStateHandle[KEY_SCROLL_Y] = value
        }
    
    // WebView scale
    var webViewScale: Float
        get() = savedStateHandle.get<Float>(KEY_WEB_VIEW_SCALE) ?: 1.0f
        set(value) {
            savedStateHandle[KEY_WEB_VIEW_SCALE] = value
        }
    
    /**
     * Get the stored plugin states
     * @return Map of plugin names to their enabled/disabled state
     */
    @Suppress("UNCHECKED_CAST")
    fun getPluginStates(): Map<String, Boolean>? {
        return savedStateHandle.get<Map<String, Boolean>>(KEY_PLUGIN_STATES)
    }
    
    /**
     * Update a single plugin's state
     * @param pluginName Name of the plugin
     * @param enabled Whether the plugin is enabled
     */
    fun updatePluginState(pluginName: String, enabled: Boolean) {
        val currentStates = getPluginStates()?.toMutableMap() ?: mutableMapOf()
        currentStates[pluginName] = enabled
        savedStateHandle[KEY_PLUGIN_STATES] = currentStates
    }
    
    /**
     * Update all plugin states at once
     * @param states Map of plugin names to their enabled/disabled state
     */
    fun updatePluginStates(states: Map<String, Boolean>) {
        savedStateHandle[KEY_PLUGIN_STATES] = states
    }
}
