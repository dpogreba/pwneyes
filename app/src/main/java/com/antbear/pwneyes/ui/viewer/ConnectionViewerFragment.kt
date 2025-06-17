package com.antbear.pwneyes.ui.viewer

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.antbear.pwneyes.databinding.FragmentConnectionViewerBinding
import java.util.Base64

class ConnectionViewerFragment : Fragment() {
    private var _binding: FragmentConnectionViewerBinding? = null
    private val binding get() = _binding!!
    private val args: ConnectionViewerFragmentArgs by navArgs()
    
    // Variables to preserve WebView state across orientation changes
    private var webViewState: Bundle? = null
    private var lastUrl: String? = null
    private var lastScrollX: Int = 0
    private var lastScrollY: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Retain this fragment across configuration changes
        retainInstance = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConnectionViewerBinding.inflate(inflater, container, false)
        setupWebView()
        
        // Restore WebView state if it exists
        webViewState?.let { state ->
            binding.webView.restoreState(state)
        }
        
        return binding.root
    }

    private fun setupWebView() {
        // Configure WebView to handle orientation changes better
        binding.webView.apply {
            // Set an ID to help with state restoration
            id = View.generateViewId()
            
            // Set hardware acceleration for better performance
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            settings.apply {
                // Enable JavaScript and DOM storage
                javaScriptEnabled = true
                domStorageEnabled = true
                
                // Enable zooming capabilities
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                
                // Critical settings for proper viewport rendering and scrolling
                loadWithOverviewMode = true
                useWideViewPort = true
                
                // Set initial scale to show more content (including bottom bar)
                setInitialScale(90) // 90% of original size to ensure bottom bar is visible
                
                // Enable zoom controls to allow user to adjust view as needed
                builtInZoomControls = true
                displayZoomControls = false
                setSupportZoom(true)
                
                // Enable caching for better performance (modern approach)
                cacheMode = WebSettings.LOAD_DEFAULT
                
                // Additional settings for better web experience
                setGeolocationEnabled(false)
                
                // Allow cross-domain AJAX requests if needed for some APIs
                allowContentAccess = true
                allowFileAccess = true
                
                // Enable JavaScript dialogs
                javaScriptCanOpenWindowsAutomatically = true
                setSupportMultipleWindows(true)
                
                // Set default text encoding
                defaultTextEncodingName = "UTF-8"
                
                // Allow mixed content - needed for some older web interfaces
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
            
            // Enable scroll bars and ensure scrolling works
            isVerticalScrollBarEnabled = true
            isHorizontalScrollBarEnabled = true
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            overScrollMode = View.OVER_SCROLL_ALWAYS
            
            // Ensure layout is handled properly for scrolling
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            
            // Set initial scale to show more content
            setInitialScale(90)
            
            // Add pinch-to-zoom and double-tap-to-zoom gesture handling
            setOnTouchListener { v, event ->
                v.parent.requestDisallowInterceptTouchEvent(true)
                false
            }

            // Set WebChromeClient to handle JavaScript dialogs
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (newProgress < 100) {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.progressBar.progress = newProgress
                    } else {
                        binding.progressBar.visibility = View.GONE
                    }
                }
                
                override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult): Boolean {
                    try {
                        val context = view?.context ?: return false
                        val alertDialog = android.app.AlertDialog.Builder(context)
                            .setTitle("Alert")
                            .setMessage(message)
                            .setPositiveButton("OK") { _, _ -> 
                                result.confirm()
                            }
                            .setCancelable(true)
                            .setOnCancelListener {
                                result.cancel()
                            }
                            .create()
                        
                        alertDialog.show()
                    } catch (e: Exception) {
                        result.cancel()
                    }
                    return true
                }
                
                override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult): Boolean {
                    try {
                        val context = view?.context ?: return false
                        
                        // Check if this is the shutdown confirmation
                        val isShutdown = message?.contains("shutdown", ignoreCase = true) ?: false
                        
                        val title = if (isShutdown) "Shutdown Confirmation" else "Confirmation"
                        val positiveButton = if (isShutdown) "Shutdown" else "OK"
                        
                        val confirmDialog = android.app.AlertDialog.Builder(context)
                            .setTitle(title)
                            .setMessage(message)
                            .setPositiveButton(positiveButton) { _, _ -> 
                                result.confirm()
                            }
                            .setNegativeButton("Cancel") { _, _ -> 
                                result.cancel()
                            }
                            .setCancelable(true)
                            .setOnCancelListener {
                                result.cancel()
                            }
                            .create()
                        
                        confirmDialog.show()
                    } catch (e: Exception) {
                        result.cancel()
                    }
                    return true
                }
                
                override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult): Boolean {
                    try {
                        val context = view?.context ?: return false
                        val input = android.widget.EditText(context)
                        input.setText(defaultValue)
                        
                        val promptDialog = android.app.AlertDialog.Builder(context)
                            .setTitle("Prompt")
                            .setMessage(message)
                            .setView(input)
                            .setPositiveButton("OK") { _, _ -> 
                                result.confirm(input.text.toString())
                            }
                            .setNegativeButton("Cancel") { _, _ -> 
                                result.cancel()
                            }
                            .setCancelable(true)
                            .setOnCancelListener {
                                result.cancel()
                            }
                            .create()
                        
                        promptDialog.show()
                    } catch (e: Exception) {
                        result.cancel()
                    }
                    return true
                }
                
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    consoleMessage?.let {
                        android.util.Log.d("WebConsole", "${it.message()} -- From line ${it.lineNumber()} of ${it.sourceId()}")
                    }
                    return true
                }
            }
            
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    // Keep all navigation within the WebView
                    return false
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    binding.progressBar.visibility = View.VISIBLE
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    binding.progressBar.visibility = View.GONE
                    
                        // Save last URL for state restoration
                    lastUrl = url
                    
                    // Inject enhanced JavaScript to ensure content is scrollable, including nested areas
                    view?.evaluateJavascript("""
                        (function() {
                            // Store current scroll position for orientation changes
                            window.addEventListener('scroll', function() {
                                window.scrollXPos = window.scrollX;
                                window.scrollYPos = window.scrollY;
                            });
                            
                            // More aggressive scrolling function with touch handling
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
                            
                            // Apply scrolling to document and body
                            document.documentElement.style.height = 'auto';
                            document.documentElement.style.overflow = 'auto';
                            document.body.style.height = 'auto';
                            document.body.style.overflow = 'auto';
                            document.body.style.webkitOverflowScrolling = 'touch';
                            
                            // Force all containers to be scrollable - target everything
                            var allElements = document.querySelectorAll('*');
                            for (var i = 0; i < allElements.length; i++) {
                                // Skip certain elements that shouldn't be scrollable
                                var tagName = allElements[i].tagName.toLowerCase();
                                if (tagName === 'script' || tagName === 'style' || tagName === 'meta' || tagName === 'link') {
                                    continue;
                                }
                                
                                // Check if this might be a content container
                                var style = window.getComputedStyle(allElements[i]);
                                if (style.display !== 'none' && style.visibility !== 'hidden' && 
                                    (style.overflow === 'hidden' || allElements[i].scrollHeight > allElements[i].clientHeight)) {
                                    makeScrollable(allElements[i]);
                                }
                            }
                            
                            // Specifically target elements that might be in the Plugin tab
                            var specialSelectors = [
                                '.plugin', '.plugin-content', '.tab-content', '.main-content', 
                                '[id*="plugin"]', '[id*="tab"]', '[class*="plugin"]', '[class*="tab"]',
                                'iframe', 'frame', '.scrollable', '[role="main"]'
                            ];
                            
                            specialSelectors.forEach(function(selector) {
                                try {
                                    var elements = document.querySelectorAll(selector);
                                    for (var i = 0; i < elements.length; i++) {
                                        makeScrollable(elements[i]);
                                        
                                        // Also make all children scrollable
                                        var children = elements[i].querySelectorAll('*');
                                        for (var j = 0; j < children.length; j++) {
                                            makeScrollable(children[j]);
                                        }
                                    }
                                } catch (e) {
                                    console.error('Error applying scrollable to ' + selector, e);
                                }
                            });
                            
                            // Add touch event listeners to handle custom scrolling on problematic elements
                            var touchStartY = 0;
                            var scrollingElement = null;
                            
                            document.addEventListener('touchstart', function(e) {
                                touchStartY = e.touches[0].clientY;
                                var target = e.target;
                                
                                // Find scrollable parent
                                while (target && !isScrollable(target)) {
                                    target = target.parentElement;
                                }
                                
                                scrollingElement = target;
                            }, { passive: false });
                            
                            document.addEventListener('touchmove', function(e) {
                                if (!scrollingElement) return;
                                
                                var touchY = e.touches[0].clientY;
                                var deltaY = touchStartY - touchY;
                                
                                scrollingElement.scrollTop += deltaY;
                                touchStartY = touchY;
                                
                                // Prevent default only if we're handling the scroll
                                if (Math.abs(deltaY) > 5) {
                                    e.preventDefault();
                                }
                            }, { passive: false });
                            
                            function isScrollable(element) {
                                if (!element) return false;
                                var style = window.getComputedStyle(element);
                                return style.overflowY === 'auto' || style.overflowY === 'scroll' || 
                                       element.scrollHeight > element.clientHeight;
                            }
                            
                            // Log information about content dimensions for debugging
                            console.log('Document height: ' + document.documentElement.scrollHeight);
                            console.log('Viewport height: ' + window.innerHeight);
                            
                            // Force a small delay then reflow to ensure scrollbars appear if needed
                            setTimeout(function() {
                                window.dispatchEvent(new Event('resize'));
                                
                                // Restore scroll position if it exists
                                if (typeof window.scrollXPos !== 'undefined' && typeof window.scrollYPos !== 'undefined') {
                                    window.scrollTo(window.scrollXPos, window.scrollYPos);
                                }
                                
                                // Scale viewport to ensure bottom control bar is visible
                                var meta = document.querySelector('meta[name="viewport"]');
                                if (!meta) {
                                    meta = document.createElement('meta');
                                    meta.name = 'viewport';
                                    document.head.appendChild(meta);
                                }
                                meta.content = 'width=device-width, initial-scale=0.9, maximum-scale=3.0, user-scalable=yes';
                                
                                console.log('Enhanced scrolling applied to all elements');
                            }, 500);
                        })();
                    """.trimIndent(), null)
                    
                    // Save scroll position after page has fully loaded
                    view?.postDelayed({
                        lastScrollX = view.scrollX
                        lastScrollY = view.scrollY
                    }, 1000)
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    binding.progressBar.visibility = View.GONE
                    showError("Failed to load: ${error?.description}")
                }

                override fun onReceivedHttpAuthRequest(
                    view: WebView?,
                    handler: HttpAuthHandler?,
                    host: String?,
                    realm: String?
                ) {
                    if (args.username.isNotEmpty()) {
                        handler?.proceed(args.username, args.password)
                    } else {
                        super.onReceivedHttpAuthRequest(view, handler, host, realm)
                    }
                }
            }

            // Handle basic auth in URL if credentials are provided
            val urlWithAuth = if (args.username.isNotEmpty()) {
                val credentials = "${args.username}:${args.password}"
                val base64Credentials = Base64.getEncoder().encodeToString(credentials.toByteArray())
                args.url.replace("://", "://$base64Credentials@")
            } else {
                args.url
            }

            loadUrl(urlWithAuth)
        }
    }

    private fun showError(message: String) {
        binding.errorLayout.visibility = View.VISIBLE
        binding.webView.visibility = View.GONE
        binding.errorMessage.text = message
        binding.retryButton.setOnClickListener {
            binding.errorLayout.visibility = View.GONE
            binding.webView.visibility = View.VISIBLE
            binding.webView.reload()
        }
    }

    override fun onPause() {
        super.onPause()
        // Save WebView state when fragment is paused (e.g., during orientation change)
        val newState = Bundle()
        binding.webView.saveState(newState)
        webViewState = newState
    }
    
    override fun onResume() {
        super.onResume()
        // Restore scroll position after resuming
        if (lastScrollX != 0 || lastScrollY != 0) {
            binding.webView.postDelayed({
                binding.webView.scrollTo(lastScrollX, lastScrollY)
            }, 300)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Don't nullify _binding if we're just changing orientation
        if (!requireActivity().isChangingConfigurations) {
            _binding = null
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up WebView resources only if the fragment is truly being destroyed
        if (!requireActivity().isChangingConfigurations) {
            binding.webView.destroy()
            webViewState = null
        }
    }
}
