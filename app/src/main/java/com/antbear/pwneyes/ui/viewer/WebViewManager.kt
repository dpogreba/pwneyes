package com.antbear.pwneyes.ui.viewer

import android.content.Context
import android.util.Log
import android.webkit.WebView
import android.webkit.WebSettings
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manager class for WebView configuration and JavaScript injection.
 * Centralizes all WebView enhancement logic and provides a clean API for fragments.
 */
class WebViewManager(private val context: Context) {
    
    private val TAG = "WebViewManager"
    
    // LiveData to track WebView loading state
    private val _loadingState = MutableLiveData<LoadingState>()
    val loadingState: LiveData<LoadingState> = _loadingState
    
    // Coroutine scope for async operations
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    /**
     * Configures a WebView with optimal settings for displaying Raspberry Pi web interfaces
     */
    fun configureWebView(webView: WebView) {
        webView.apply {
            // Set hardware acceleration for better performance
            setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
            
            // Enable WebView debugging (requires Chrome DevTools)
            WebView.setWebContentsDebuggingEnabled(true)
            
            settings.apply {
                // Enable JavaScript and DOM storage
                javaScriptEnabled = true
                domStorageEnabled = true
                
                // Enable zooming capabilities
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                
                // Critical settings for proper viewport rendering
                loadWithOverviewMode = true
                useWideViewPort = true
                
                // Initial scale settings for better content visibility
                setInitialScale(50) // 50% of original size
                
                // Enable caching for better performance
                cacheMode = WebSettings.LOAD_DEFAULT
                
                // Additional settings for better web experience
                setGeolocationEnabled(false)
                
                // Allow cross-domain requests if needed
                allowContentAccess = true
                allowFileAccess = true
                
                // Enable JavaScript dialogs
                javaScriptCanOpenWindowsAutomatically = true
                setSupportMultipleWindows(true)
                
                // Set default text encoding
                defaultTextEncodingName = "UTF-8"
                
                // Allow mixed content - needed for some older interfaces
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
            
            // Set up scrolling properties
            isVerticalScrollBarEnabled = true
            isHorizontalScrollBarEnabled = true
            scrollBarStyle = WebView.SCROLLBARS_INSIDE_OVERLAY
            overScrollMode = WebView.OVER_SCROLL_ALWAYS
            
            // Apply custom touch listener for improved scrolling
            setOnTouchListener(CustomWebViewTouchListener())
        }
    }
    
    /**
     * Enhance the WebView rendering after the page is loaded
     */
    fun enhanceRendering(webView: WebView) {
        _loadingState.value = LoadingState.ENHANCING
        
        coroutineScope.launch {
            // Give the page a moment to render before applying enhancements
            delay(300)
            
            // Apply scrolling enhancements
            applyScrollingEnhancements(webView)
            
            // Apply general UI improvements
            applyUIEnhancements(webView)
            
            _loadingState.value = LoadingState.ENHANCED
        }
    }
    
    /**
     * Apply scrolling enhancements to make content fully scrollable
     */
    private fun applyScrollingEnhancements(webView: WebView) {
        val js = """
            (function() {
                // Store current scroll position for orientation changes
                window.addEventListener('scroll', function() {
                    window.scrollXPos = window.scrollX;
                    window.scrollYPos = window.scrollY;
                });
                
                // Make an element scrollable
                function makeScrollable(element) {
                    if (!element) return;
                    
                    // Force height to ensure scrollability
                    element.style.height = element.scrollHeight > element.clientHeight ? '100%' : 'auto';
                    element.style.maxHeight = 'none';
                    
                    // Force all overflow settings
                    element.style.overflow = 'auto';
                    element.style.overflowX = 'auto';
                    element.style.overflowY = 'auto';
                    element.style.webkitOverflowScrolling = 'touch';
                    
                    // Add specific CSS for touch devices
                    element.style.touchAction = 'pan-y';
                    
                    // Ensure the element is not preventing scroll
                    element.style.position = element.style.position === 'fixed' ? 'absolute' : element.style.position;
                    
                    // Add data attribute for debugging
                    element.setAttribute('data-made-scrollable', 'true');
                }
                
                // Apply to document and body
                document.documentElement.style.height = 'auto';
                document.documentElement.style.overflow = 'auto';
                document.body.style.height = 'auto';
                document.body.style.overflow = 'auto';
                document.body.style.webkitOverflowScrolling = 'touch';
                
                // Target specific elements that are likely to contain scrollable content
                const scrollTargets = [
                    '.plugin', '.plugin-content', '.tab-content', '.main-content', 
                    '[id*="plugin"]', '[id*="tab"]', '[class*="plugin"]', '[class*="tab"]',
                    'iframe', 'frame', '.scrollable', '[role="main"]'
                ];
                
                // Apply to all potential scrollable elements
                const allElements = document.querySelectorAll('*');
                for (const element of allElements) {
                    // Skip certain elements
                    const tagName = element.tagName.toLowerCase();
                    if (tagName === 'script' || tagName === 'style' || tagName === 'meta' || tagName === 'link') {
                        continue;
                    }
                    
                    // Check if this might be a content container
                    const style = window.getComputedStyle(element);
                    if (style.display !== 'none' && style.visibility !== 'hidden' && 
                        (style.overflow === 'hidden' || element.scrollHeight > element.clientHeight)) {
                        makeScrollable(element);
                    }
                }
                
                // Apply to specific targets
                scrollTargets.forEach(selector => {
                    try {
                        const elements = document.querySelectorAll(selector);
                        for (const element of elements) {
                            makeScrollable(element);
                            
                            // Also make children scrollable
                            const children = element.querySelectorAll('*');
                            for (const child of children) {
                                makeScrollable(child);
                            }
                        }
                    } catch (e) {
                        console.error('Error applying scrollable to ' + selector, e);
                    }
                });
                
                // Force a small delay then reflow to ensure scrollbars appear if needed
                setTimeout(function() {
                    window.dispatchEvent(new Event('resize'));
                    
                    // Restore scroll position if it exists
                    if (typeof window.scrollXPos !== 'undefined' && typeof window.scrollYPos !== 'undefined') {
                        window.scrollTo(window.scrollXPos, window.scrollYPos);
                    }
                    
                    console.log('Enhanced scrolling applied to all elements');
                }, 500);
                
                return 'Scrolling enhancements applied';
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(js) { result ->
            Log.d(TAG, "Scrolling enhancements result: $result")
        }
    }
    
    /**
     * Apply UI enhancements to improve content visibility and interaction
     */
    private fun applyUIEnhancements(webView: WebView) {
        val js = """
            (function() {
                // Extreme viewport manipulation to force all content to fit
                var meta = document.querySelector('meta[name="viewport"]');
                if (!meta) {
                    meta = document.createElement('meta');
                    meta.name = 'viewport';
                    document.head.appendChild(meta);
                }
                meta.content = 'width=device-width, initial-scale=0.5, maximum-scale=3.0, user-scalable=yes';

                // Force all content to be visible by manipulating root styles
                document.documentElement.style.height = 'auto';
                document.documentElement.style.overflow = 'visible';
                document.documentElement.style.position = 'relative';
                document.documentElement.style.paddingBottom = '400px';

                // Make body smaller to fit within the viewport
                document.body.style.transform = 'scale(0.9)';
                document.body.style.transformOrigin = 'top center';
                document.body.style.marginBottom = '300px';
                
                // Detect and highlight control elements
                var controlKeywords = ['shutdown', 'reboot', 'restart', 'manu', 'power'];
                var possibleButtons = document.querySelectorAll('*');
                var foundControls = false;
                
                // Check for control text in any element
                for (var i = 0; i < possibleButtons.length; i++) {
                    var el = possibleButtons[i];
                    var text = el.innerText || el.textContent || '';
                    
                    if (text) {
                        var lowerText = text.toLowerCase ? text.toLowerCase() : text.toLowerCase;
                        var isControl = controlKeywords.some(keyword => lowerText.includes(keyword));
                        
                        if (isControl) {
                            // Found control element - improve visibility!
                            console.log('FOUND CONTROL ELEMENT: ' + text);
                            foundControls = true;
                            
                            // Make control more visible
                            el.style.position = 'relative';
                            el.style.zIndex = '999';
                            el.style.backgroundColor = 'rgba(255,245,235,0.1)';
                            el.style.border = '1px solid rgba(255,0,0,0.2)';
                            el.style.borderRadius = '4px';
                            el.style.padding = el.style.padding || '5px';
                            el.style.margin = '5px';
                            el.style.cursor = 'pointer';
                            
                            // Add a data attribute for identification
                            el.setAttribute('data-pwneyes-control', 'true');
                        }
                    }
                }
                
                // Add bottom margin and padding for visibility
                document.body.style.paddingBottom = '80px';
                document.body.style.marginBottom = '80px';
                
                return {
                    enhanced: true, 
                    controlsFound: foundControls
                };
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(js) { result ->
            Log.d(TAG, "UI enhancements result: $result")
        }
    }
    
    /**
     * Execute a command on the device (shutdown, reboot, restart)
     */
    fun executeCommand(webView: WebView, command: String) {
        val js = when (command) {
            "shutdown" -> createCommandScript("shutdown")
            "reboot" -> createCommandScript("reboot")
            "restart_manu" -> createCommandScript("restart", "manu")
            else -> """
                (function() {
                    console.log('Unknown command: $command');
                    return { error: 'Unknown command', command: '$command' };
                })();
            """
        }
        
        webView.evaluateJavascript(js.trimIndent()) { result ->
            Log.d(TAG, "Command execution result ($command): $result")
        }
    }
    
    /**
     * Create a command execution script
     */
    private fun createCommandScript(command: String, additionalKeyword: String? = null): String {
        return """
            (function() {
                // Find button based on text content
                var buttons = Array.from(document.querySelectorAll('*')).filter(function(el) {
                    var text = el.textContent || el.innerText || '';
                    text = text.toLowerCase ? text.toLowerCase() : text;
                    var cmdLower = '${command.lowercase()}';
                    if (text.includes(cmdLower)) {
                        return ${if (additionalKeyword != null) "text.includes('${additionalKeyword.lowercase()}')" else "true"};
                    }
                    return false;
                });
                
                // Also look for attributes that might indicate the right element
                var attrButtons = Array.from(document.querySelectorAll('[data-action="${command}"], [id*="${command}"], [class*="${command}"]'));
                buttons = buttons.concat(attrButtons);
                
                if (buttons.length > 0) {
                    console.log('Found ${command} button, clicking...');
                    // Get the most likely button (first one or the one with matching data attribute)
                    var bestButton = buttons.find(b => b.getAttribute('data-pwneyes-control') === 'true') || buttons[0];
                    bestButton.click();
                    return { 
                        success: true, 
                        action: '${command}', 
                        element: bestButton.tagName,
                        text: bestButton.textContent || bestButton.innerText || ''
                    };
                } else {
                    console.log('No ${command} button found');
                    return { 
                        success: false, 
                        action: '${command}', 
                        reason: 'Button not found' 
                    };
                }
            })();
        """
    }
    
    /**
     * Enum representing the loading state of a WebView
     */
    enum class LoadingState {
        LOADING,
        ENHANCING,
        ENHANCED,
        ERROR
    }
}
