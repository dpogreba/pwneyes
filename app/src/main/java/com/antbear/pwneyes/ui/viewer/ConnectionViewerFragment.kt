package com.antbear.pwneyes.ui.viewer

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
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
        setupControlButtons()
        
        // Restore WebView state if it exists
        webViewState?.let { state ->
            binding.webView.restoreState(state)
        }
        
        return binding.root
    }
    
    private fun setupControlButtons() {
        // Set up direct Plugins navigation button
        binding.pluginsTabButton.setOnClickListener {
            navigateToPluginsTab()
        }
        
        // Set up click listeners for our overlay buttons
        binding.btnShutdown.setOnClickListener {
            executeJavaScriptCommand("shutdown")
        }
        
        binding.btnReboot.setOnClickListener {
            executeJavaScriptCommand("reboot")
        }
        
        binding.btnRestart.setOnClickListener {
            executeJavaScriptCommand("restart_manu")
        }
    }
    
    /**
     * Direct method to navigate to the Plugins tab
     * This bypasses all the JavaScript detection complexity
     */
    private fun navigateToPluginsTab() {
        try {
            // Ensure URL has port 8080 if needed
            var url = args.url
            if (!url.contains(":8080") && !url.contains(":443") && !url.contains(":80")) {
                url = if (url.endsWith("/")) {
                    url.substring(0, url.length - 1) + ":8080/"
                } else {
                    url + ":8080"
                }
                android.util.Log.d("DirectNavigation", "Added port 8080 to URL: $url")
            }
            
            android.util.Log.i("DirectNavigation", "Navigating directly to Plugins tab")
            android.util.Log.i("DirectNavigation", "URL: $url")
            
            // Create navigation action with explicit details
            val action = ConnectionViewerFragmentDirections.actionConnectionViewerToTabDetail(
                url = url,
                tabName = "Plugins Tab",  // Make tab name very explicit
                tabSelector = "plugins",
                username = args.username,
                password = args.password
            )
            
            // Show a big visible message
            context?.let {
                Toast.makeText(
                    it, 
                    "⚠️ NAVIGATING TO PLUGINS TAB ⚠️", 
                    Toast.LENGTH_LONG
                ).show()
            }
            
            // Add a small delay to ensure toast is visible
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                // Navigate to the tab detail fragment
                findNavController().navigate(action)
                android.util.Log.i("DirectNavigation", "Navigation completed")
            }, 300)
            
        } catch (e: Exception) {
            // Log any exceptions
            android.util.Log.e("DirectNavigation", "Error navigating to Plugins tab: ${e.message}")
            android.util.Log.e("DirectNavigation", "Stack trace: ${e.stackTraceToString()}")
            
            // Show error toast
            context?.let {
                Toast.makeText(
                    it, 
                    "ERROR: ${e.message}", 
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun executeJavaScriptCommand(command: String) {
        val js = when (command) {
            "shutdown" -> """
                (function() {
                    // Find shutdown button and click it
                    var shutdownButtons = Array.from(document.querySelectorAll('*')).filter(function(el) {
                        var text = el.textContent || el.innerText || '';
                        return text.toLowerCase().includes('shutdown');
                    });
                    
                    if (shutdownButtons.length > 0) {
                        console.log('Found shutdown button, clicking...');
                        shutdownButtons[0].click();
                        return 'Shutdown button clicked';
                    } else {
                        console.log('No shutdown button found');
                        // Try to trigger a shutdown via URL/form if applicable
                        return 'No shutdown button found';
                    }
                })();
            """
            "reboot" -> """
                (function() {
                    // Find reboot button and click it
                    var rebootButtons = Array.from(document.querySelectorAll('*')).filter(function(el) {
                        var text = el.textContent || el.innerText || '';
                        return text.toLowerCase().includes('reboot');
                    });
                    
                    if (rebootButtons.length > 0) {
                        console.log('Found reboot button, clicking...');
                        rebootButtons[0].click();
                        return 'Reboot button clicked';
                    } else {
                        console.log('No reboot button found');
                        // Try to trigger a reboot via URL/form if applicable
                        return 'No reboot button found';
                    }
                })();
            """
            "restart_manu" -> """
                (function() {
                    // Find MANU restart button and click it
                    var restartButtons = Array.from(document.querySelectorAll('*')).filter(function(el) {
                        var text = el.textContent || el.innerText || '';
                        return text.toLowerCase().includes('restart') && text.toLowerCase().includes('manu');
                    });
                    
                    if (restartButtons.length > 0) {
                        console.log('Found restart MANU button, clicking...');
                        restartButtons[0].click();
                        return 'Restart MANU button clicked';
                    } else {
                        console.log('No restart MANU button found');
                        // Try to find any restart button
                        var anyRestartButtons = Array.from(document.querySelectorAll('*')).filter(function(el) {
                            var text = el.textContent || el.innerText || '';
                            return text.toLowerCase().includes('restart');
                        });
                        
                        if (anyRestartButtons.length > 0) {
                            console.log('Found generic restart button, clicking...');
                            anyRestartButtons[0].click();
                            return 'Generic restart button clicked';
                        }
                        
                        return 'No restart buttons found';
                    }
                })();
            """
            else -> """
                (function() {
                    console.log('Unknown command: $command');
                    return 'Unknown command';
                })();
            """
        }
        
        binding.webView.evaluateJavascript(js.trimIndent()) { result ->
            android.util.Log.d("WebCommandExecution", "Command: $command, Result: $result")
            
            // Show a toast confirmation
            val message = when {
                result.contains("clicked") -> "Command sent: $command"
                result.contains("No") -> "Could not find button for: $command"
                else -> "Command execution failed"
            }
            
            context?.let {
                Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupWebView() {
        // Configure WebView to handle orientation changes better
        binding.webView.apply {
            // Set an ID to help with state restoration
            id = View.generateViewId()
            
            // Set hardware acceleration for better performance
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
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
                
                // Critical settings for proper viewport rendering and scrolling
                loadWithOverviewMode = true
                useWideViewPort = true
                
                // Take drastic measures - use extremely small scale to ensure all content fits
                setInitialScale(50) // 50% of original size to force everything into view
                
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
            
            // Use extreme scaling to ensure bottom controls are visible
            setInitialScale(50)
            
            // Add explicit bottom padding to push content up
            setPadding(0, 0, 0, 200) // Add 200px padding at bottom
            
            // Implement direct touch handling with custom scrolling
            setOnTouchListener(CustomWebViewTouchListener())

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
                    
                    // Force immediate extreme zoom out to show all content
                    view?.postDelayed({
                        // Use multiple zoom outs to force everything visible
                        for (i in 1..5) {
                            view.zoomOut()
                        }
                        
                        // Try to scroll to extreme bottom to ensure it's loaded
                        val heightGuess = 10000 // Much larger guess to ensure we reach bottom
                        view.scrollTo(0, heightGuess)
                        
                        // Wait longer before scrolling back to top
                        view.postDelayed({
                            // Scroll to show most content but ensure bottom is visible
                            view.scrollTo(0, 100)
                            
                            // Add a direct pixel offset to the rendering to show bottom of page
                            view.evaluateJavascript("""
                                (function() {
                                    // Direct offset of the entire content to show bottom
                                    document.body.style.transform = 'translateY(-150px)';
                                    document.body.style.marginBottom = '300px';
                                    document.documentElement.style.height = 'calc(100% - 200px)';
                                    return 'Applied extreme transform';
                                })();
                            """.trimIndent(), null)
                        }, 500)
                    }, 500)
                    
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
                                
                                // Extreme method to find and move bottom controls
                                var possibleButtons = document.querySelectorAll('*');
                                var foundControls = false;
                                
                                // Check for shutdown/reboot text in any element
                                for (var i = 0; i < possibleButtons.length; i++) {
                                    var el = possibleButtons[i];
                                    var text = el.innerText || el.textContent;
                                    
                                    if (text && (text.indexOf('Shutdown') >= 0 || 
                                                text.indexOf('Reboot') >= 0 || 
                                                text.indexOf('MANU') >= 0)) {
                                        // Found control element - move it into view!
                                        console.log('FOUND CONTROL ELEMENT: ' + text);
                                        foundControls = true;
                                        
                                        // Force it to fixed position at bottom of screen
                                        el.style.position = 'fixed';
                                        el.style.bottom = '50px';
                                        el.style.left = '50%';
                                        el.style.transform = 'translateX(-50%)';
                                        el.style.zIndex = '9999';
                                        el.style.backgroundColor = 'rgba(255,0,0,0.3)';
                                        el.style.padding = '10px';
                                        el.style.border = '2px solid red';
                                    }
                                }
                                
                                // If we found controls, dramatically modify the page to show them
                                if (foundControls) {
                                    // Force entire body to be shorter
                                    document.body.style.height = '70vh';
                                    document.body.style.overflow = 'visible';
                                    document.documentElement.style.height = '70vh';
                                }
                                
                                // As a fallback, add a huge bottom margin anyway
                                document.body.style.paddingBottom = '250px';
                                document.body.style.marginBottom = '250px';
                                
                                // Force bottom margin for any bottom toolbars, navigation, etc.
                                var possibleBottomBars = document.querySelectorAll('.toolbar, .navbar, .navigation, nav, footer, .footer, .controls, .bottom-controls');
                                possibleBottomBars.forEach(function(bar) {
                                    var rect = bar.getBoundingClientRect();
                                    if (rect.bottom > window.innerHeight * 0.8) {
                                        bar.style.marginBottom = '80px';
                                        console.log('Adjusted bottom bar');
                                    }
                                });
                                
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

            // Handle basic auth in URL if credentials are provided, preserving port numbers
            val urlWithAuth = if (args.username.isNotEmpty()) {
                try {
                    // Parse the URL to extract its components
                    val url = java.net.URL(args.url)
                    val protocol = url.protocol
                    val host = url.host
                    val port = if (url.port == -1) "" else ":${url.port}"
                    val path = if (url.path.isEmpty()) "/" else url.path
                    val query = if (url.query == null) "" else "?${url.query}"
                    val ref = if (url.ref == null) "" else "#${url.ref}"
                    
                    // Create credentials
                    val credentials = "${args.username}:${args.password}"
                    val base64Credentials = Base64.getEncoder().encodeToString(credentials.toByteArray())
                    
                    // Reconstruct URL with credentials while preserving port
                    "$protocol://$base64Credentials@$host$port$path$query$ref"
                } catch (e: Exception) {
                    // Fallback to simple replacement if URL parsing fails
                    android.util.Log.e("ConnectionViewer", "Error parsing URL: ${e.message}")
                    val credentials = "${args.username}:${args.password}"
                    val base64Credentials = Base64.getEncoder().encodeToString(credentials.toByteArray())
                    args.url.replace("://", "://$base64Credentials@")
                }
            } else {
                args.url
            }

            android.util.Log.d("ConnectionViewer", "Loading URL: $urlWithAuth")
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
