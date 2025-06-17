package com.antbear.pwneyes.ui.viewer

import android.view.MotionEvent
import android.view.View
import android.webkit.WebView
import android.util.Log
import kotlin.math.abs

/**
 * CustomWebViewTouchListener provides aggressive touch handling for WebView
 * to ensure proper scrolling in all areas, especially for problematic nested content.
 */
class CustomWebViewTouchListener : View.OnTouchListener {
    private var lastY = 0f
    private var startY = 0f
    private var startX = 0f
    private var isScrolling = false
    private var initialDistance = 0f
    
    // For pinch-to-zoom detection
    private var lastPointerCount = 0
    private var initialPointerDistance = 0f
    
    companion object {
        private const val TAG = "CustomWebViewTouch"
        private const val TOUCH_SLOP = 10f // Minimum distance to consider as scrolling
    }
    
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (v !is WebView) return false
        
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Record starting position
                lastY = event.y
                startY = event.y
                startX = event.x
                isScrolling = false
                lastPointerCount = event.pointerCount
                v.parent.requestDisallowInterceptTouchEvent(true)
                
                Log.d(TAG, "Touch DOWN at y=${event.y}")
            }
            
            MotionEvent.ACTION_MOVE -> {
                val currentY = event.y
                val deltaY = lastY - currentY
                
                if (event.pointerCount > 1) {
                    // This is likely a pinch-to-zoom gesture - let WebView handle it
                    if (lastPointerCount == 1) {
                        // Transition from scroll to zoom
                        initialPointerDistance = getPointerDistance(event)
                    }
                    lastPointerCount = event.pointerCount
                    return false
                }
                
                // Handle scrolling
                if (!isScrolling && abs(startY - currentY) > TOUCH_SLOP) {
                    isScrolling = true
                }
                
                if (isScrolling) {
                    // Scroll the WebView content
                    scrollWebView(v, deltaY)
                    lastY = currentY
                    
                    // Prevent horizontal scrolling
                    val deltaX = abs(event.x - startX)
                    if (deltaX < abs(event.y - startY) * 2) {
                        // If more vertical than horizontal, prevent parent from intercepting
                        v.parent.requestDisallowInterceptTouchEvent(true)
                    }
                    
                    Log.d(TAG, "Scrolling, deltaY=$deltaY")
                    return true
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                v.parent.requestDisallowInterceptTouchEvent(false)
                isScrolling = false
                lastPointerCount = 0
                Log.d(TAG, "Touch UP/CANCEL")
                
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
     * Scroll the WebView content directly
     */
    private fun scrollWebView(webView: WebView, deltaY: Float) {
        // First try native scrolling
        webView.scrollBy(0, deltaY.toInt())
        
        // Also try JavaScript scrolling for nested content
        val js = """
            (function() {
                // Find scrollable parent of where user touched
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
                
                // Get element at center of screen
                const centerX = window.innerWidth / 2;
                const centerY = window.innerHeight / 2;
                const elementAtCenter = document.elementFromPoint(centerX, centerY);
                
                // Find scrollable parent
                const scrollable = findScrollableParent(elementAtCenter);
                
                // Scroll it
                if (scrollable) {
                    scrollable.scrollTop += ${deltaY};
                    return scrollable.tagName + ' scrolled by ${deltaY}';
                }
                
                return 'No scrollable found';
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(js) { result ->
            Log.d(TAG, "JS scroll result: $result")
        }
    }
    
    /**
     * Calculate distance between pointers for pinch detection
     */
    private fun getPointerDistance(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        
        val x1 = event.getX(0)
        val y1 = event.getY(0)
        val x2 = event.getX(1)
        val y2 = event.getY(1)
        
        return kotlin.math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))
    }
}
