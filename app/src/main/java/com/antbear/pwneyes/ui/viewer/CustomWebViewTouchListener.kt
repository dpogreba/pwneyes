package com.antbear.pwneyes.ui.viewer

import android.content.Context
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.webkit.WebView
import androidx.core.view.GestureDetectorCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Enhanced WebView touch handler with improved gesture support.
 * Provides better scrolling for WebViews, especially for content with nested scrollable areas.
 */
class CustomWebViewTouchListener : View.OnTouchListener {
    private var lastY = 0f
    private var startY = 0f
    private var startX = 0f
    private var isScrolling = false
    
    // Gesture detectors
    private var gestureDetector: GestureDetectorCompat? = null
    private var scaleGestureDetector: ScaleGestureDetector? = null
    
    // Coroutine scope for JavaScript execution
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    companion object {
        private const val TAG = "CustomWebViewTouch"
        private const val TOUCH_SLOP = 10f // Minimum distance to consider as scrolling
    }
    
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (v !is WebView) return false
        
        // Initialize gesture detectors if needed
        if (gestureDetector == null) {
            initGestureDetectors(v.context, v)
        }
        
        // Let the gesture detector handle the event first
        gestureDetector?.onTouchEvent(event)
        scaleGestureDetector?.onTouchEvent(event)
        
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Record starting position
                lastY = event.y
                startY = event.y
                startX = event.x
                isScrolling = false
                v.parent.requestDisallowInterceptTouchEvent(true)
                
                Log.d(TAG, "Touch DOWN at y=${event.y}")
            }
            
            MotionEvent.ACTION_MOVE -> {
                val currentY = event.y
                val deltaY = lastY - currentY
                
                if (event.pointerCount > 1) {
                    // This is likely a pinch-to-zoom gesture
                    return false // Let WebView handle zoom
                }
                
                // Handle scrolling
                if (!isScrolling && abs(startY - currentY) > TOUCH_SLOP) {
                    isScrolling = true
                }
                
                if (isScrolling) {
                    // Calculate if scroll is more vertical than horizontal
                    val deltaX = abs(event.x - startX)
                    val isVerticalScroll = deltaX < abs(event.y - startY) * 1.5
                    
                    if (isVerticalScroll) {
                        // Scroll the WebView content
                        scrollWebView(v, deltaY)
                        lastY = currentY
                        
                        // Prevent parent from intercepting vertical scrolls
                        v.parent.requestDisallowInterceptTouchEvent(true)
                        return true
                    } else {
                        // Allow parent to handle horizontal scrolling
                        v.parent.requestDisallowInterceptTouchEvent(false)
                    }
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                v.parent.requestDisallowInterceptTouchEvent(false)
                isScrolling = false
                
                // If we didn't scroll much, this might be a click
                if (abs(event.y - startY) < TOUCH_SLOP && abs(event.x - startX) < TOUCH_SLOP) {
                    // Let WebView handle the click
                    return false
                }
            }
        }
        
        // If we're handling scrolling, consume the event
        return isScrolling
    }
    
    /**
     * Initialize gesture detectors
     */
    private fun initGestureDetectors(context: Context, webView: WebView) {
        // Create gesture detector for scrolling
        gestureDetector = GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                // Handle scroll logic for nested content
                return if (abs(distanceY) > abs(distanceX) * 1.5) {
                    // Only intercept if clearly vertical scrolling
                    scrollWebView(webView, distanceY)
                    true
                } else {
                    false
                }
            }
            
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                // Handle fling gesture
                if (abs(velocityY) > abs(velocityX) * 1.5) {
                    // Fling in JavaScript for smoother experience in nested content
                    flingWebView(webView, velocityY)
                    return true
                }
                return false
            }
            
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                // Let WebView handle taps normally
                return false
            }
        })
        
        // Create scale gesture detector for pinch-to-zoom
        scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                // Let WebView handle zooming
                return false
            }
        })
    }
    
    /**
     * Scroll the WebView content directly
     */
    private fun scrollWebView(webView: WebView, deltaY: Float) {
        // First try native scrolling
        webView.scrollBy(0, deltaY.toInt())
        
        // Also try JavaScript scrolling for nested content
        coroutineScope.launch {
            val js = """
                (function() {
                    // Try to find the most appropriate element to scroll
                    function findScrollableParent(element) {
                        if (!element) return document.documentElement;
                        
                        // Check if the element itself is scrollable
                        const style = window.getComputedStyle(element);
                        if ((style.overflowY === 'auto' || style.overflowY === 'scroll') && 
                            element.scrollHeight > element.clientHeight) {
                            return element;
                        }
                        
                        // Check parent elements
                        let parent = element.parentElement;
                        while (parent && parent !== document.body && parent !== document.documentElement) {
                            const parentStyle = window.getComputedStyle(parent);
                            if ((parentStyle.overflowY === 'auto' || parentStyle.overflowY === 'scroll') && 
                                parent.scrollHeight > parent.clientHeight) {
                                return parent;
                            }
                            parent = parent.parentElement;
                        }
                        
                        // Default to document element
                        return document.documentElement;
                    }
                    
                    // Try to find the element at the center of the viewport
                    const centerX = window.innerWidth / 2;
                    const centerY = window.innerHeight / 2;
                    const elementAtCenter = document.elementFromPoint(centerX, centerY);
                    
                    // Find the scrollable parent
                    const scrollable = findScrollableParent(elementAtCenter);
                    
                    // Adjust the scroll position
                    if (scrollable) {
                        scrollable.scrollTop += ${deltaY};
                        return {
                            scrolled: true,
                            element: scrollable.tagName || 'unknown',
                            deltaY: ${deltaY}
                        };
                    }
                    
                    return { scrolled: false };
                })();
            """.trimIndent()
            
            webView.evaluateJavascript(js) { result ->
                if (result != "null" && result.contains("scrolled\":true")) {
                    Log.d(TAG, "JS scroll success: $result")
                }
            }
        }
    }
    
    /**
     * Apply a fling gesture to the WebView content
     */
    private fun flingWebView(webView: WebView, velocityY: Float) {
        // Scale down velocity for smoother scrolling
        val scaledVelocity = velocityY / 10
        
        // Use JavaScript to create a smooth fling animation
        val js = """
            (function() {
                // Find scrollable element
                const centerX = window.innerWidth / 2;
                const centerY = window.innerHeight / 2;
                const elementAtCenter = document.elementFromPoint(centerX, centerY);
                
                function findScrollableParent(element) {
                    if (!element) return document.documentElement;
                    
                    while (element && element !== document.body && element !== document.documentElement) {
                        const style = window.getComputedStyle(element);
                        if (style.overflowY === 'auto' || style.overflowY === 'scroll') {
                            return element;
                        }
                        element = element.parentElement;
                    }
                    
                    return document.documentElement;
                }
                
                const scrollable = findScrollableParent(elementAtCenter);
                
                // Apply smooth scroll with velocity
                if (scrollable) {
                    // Use smooth scroll behavior
                    const currentPos = scrollable.scrollTop;
                    const distance = ${-scaledVelocity}; // Negative because scrollTop increases when scrolling down
                    
                    scrollable.scrollBy({
                        top: distance,
                        behavior: 'smooth'
                    });
                    
                    return { flinged: true, element: scrollable.tagName || 'unknown', distance: distance };
                }
                
                return { flinged: false };
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(js) { result ->
            if (result != "null" && result.contains("flinged\":true")) {
                Log.d(TAG, "JS fling applied: $result")
            }
        }
    }
}
