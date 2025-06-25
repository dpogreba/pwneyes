package com.antbear.pwneyes.ui.viewer

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.antbear.pwneyes.databinding.FragmentTabDetailBinding
import java.util.Base64

class TabDetailFragment : Fragment() {
    private var _binding: FragmentTabDetailBinding? = null
    private val binding get() = _binding!!
    private val args: TabDetailFragmentArgs by navArgs()
    
    // Variables to preserve WebView state
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
        _binding = FragmentTabDetailBinding.inflate(inflater, container, false)
        
        Log.d("TabDetailFragment", "Creating TabDetailFragment view with args: ${args.tabName}, URL: ${args.url}")
        
        // Set up toolbar with back navigation
        setupToolbar()
        
        // Set up WebView
        setupWebView()
        
        // Set up control buttons
        setupControlButtons()
        
        // Add a visual indicator that we're in TabDetailFragment for troubleshooting
        binding.tabDetailIndicator.apply {
            text = "${args.tabName} (${args.url})"
            setBackgroundColor(android.graphics.Color.parseColor("#4CAF50")) // Green background
            setTextColor(android.graphics.Color.WHITE)
        }
        
        // Restore WebView state if it exists
        webViewState?.let { state ->
            binding.webView.restoreState(state)
        }
        
        return binding.root
    }
    
    private fun setupToolbar() {
        binding.toolbar.apply {
            // Clear the title since we're using a custom title layout
            title = ""
            setNavigationOnClickListener {
                findNavController().navigateUp()
            }
        }

        // Setup the back arrow click listener
        binding.backArrow.setOnClickListener {
            findNavController().navigateUp()
        }

        // Format the URL properly for the indicator
        val formattedUrl = try {
            val url = java.net.URL(args.url)
            val host = url.host
            val port = if (url.port == -1) "" else ":${url.port}"
            val path = if (url.path.isEmpty() || url.path == "/") "/plugins" else url.path
            
            "$host$port$path"
        } catch (e: Exception) {
            "${args.url}/plugins"
        }
        
        // Set the URL in the indicator
        binding.tabDetailIndicator.text = formattedUrl
    }
    
    private fun setupControlButtons() {
        // Set up click listeners for control buttons
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
                
                // Configure viewport settings
                loadWithOverviewMode = true
                useWideViewPort = true
                
                // Set a more reasonable scale for detail view
                setInitialScale(100) // Use normal scale for detail view
                
                // Enable caching for better performance
                cacheMode = WebSettings.LOAD_DEFAULT
                
                // Additional settings for better web experience
                setGeolocationEnabled(false)
                
                // Allow cross-domain AJAX requests if needed
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
            
            // Disable scrolling in WebView since ScrollView will handle it
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = true
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            overScrollMode = View.OVER_SCROLL_NEVER
            
            // Use wrap_content for height to allow proper measurement
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

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
                    
                    // Inject JavaScript to navigate to the specified tab
                    if (args.tabSelector.isNotEmpty()) {
                        view?.evaluateJavascript("""
                            (function() {
                                try {
                                    // Try to find and activate the specified tab
                                    var tabElements = document.querySelectorAll('a[href*="' + "${args.tabSelector}" + '"], [id*="' + "${args.tabSelector}" + '"], [class*="' + "${args.tabSelector}" + '"], [data-tab*="' + "${args.tabSelector}" + '"]');
                                    
                                    if (tabElements.length > 0) {
                                        console.log('Found tab element, clicking...');
                                        tabElements[0].click();
                                        return 'Tab activated';
                                    }
                                    
                                    // Try to find links with tab name text
                                    var tabLinks = Array.from(document.querySelectorAll('a')).filter(function(el) {
                                        var text = el.textContent || el.innerText || '';
                                        return text.toLowerCase ? text.toLowerCase().includes('${args.tabName.lowercase()}') : text.includes('${args.tabName.lowercase()}');
                                    });
                                    
                                    if (tabLinks.length > 0) {
                                        console.log('Found tab link by name, clicking...');
                                        tabLinks[0].click();
                                        return 'Tab activated by name';
                                    }
                                    
                                    console.log('Could not find specified tab: ${args.tabSelector}');
                                    return 'Tab not found';
                                } catch (e) {
                                    console.error('Error activating tab:', e);
                                    return 'Error: ' + e.message;
                                }
                            })();
                        """.trimIndent(), null)
                    }
                    
                    // Inject JavaScript to properly size content for external ScrollView
                    view?.evaluateJavascript("""
                        (function() {
                            // Disable internal scrolling - outer ScrollView will handle it
                            document.documentElement.style.overflow = 'visible';
                            document.body.style.overflow = 'visible';
                            document.documentElement.style.height = 'auto';
                            document.body.style.height = 'auto';
                            
                            // Fix size of all content containers to expand properly
                            function makeContentExpandable(element) {
                                if (!element) return;
                                element.style.height = 'auto';
                                element.style.minHeight = 'auto';
                                element.style.maxHeight = 'none';
                                element.style.overflow = 'visible';
                                element.style.position = element.style.position === 'fixed' ? 'absolute' : element.style.position;
                            }
                            
                            // Apply to all major content containers
                            ['div', 'section', 'article', 'main', 'aside'].forEach(function(tag) {
                                var elements = document.getElementsByTagName(tag);
                                for (var i = 0; i < elements.length; i++) {
                                    makeContentExpandable(elements[i]);
                                }
                            });
                            
                            // Fix tables, grids and other common layouts
                            document.querySelectorAll('table, .grid, [class*="container"], [class*="content"]').forEach(function(el) {
                                makeContentExpandable(el);
                            });
                            
                            // Ensure iframes expand to their content
                            document.querySelectorAll('iframe').forEach(function(iframe) {
                                iframe.style.height = 'auto';
                                iframe.style.minHeight = '500px'; // Reasonable default
                            });
                            
                            // Resize WebView to content height for native scroll behavior
                            const resizeObserver = new ResizeObserver(entries => {
                                // Notify Android of content size change
                                window.LayoutCallbacks.onContentSizeChanged(
                                    document.documentElement.scrollWidth,
                                    document.documentElement.scrollHeight
                                );
                            });
                            
                            // Observe size changes on the document body
                            resizeObserver.observe(document.body);
                            
                            // Calculate initial size
                            setTimeout(function() {
                                var height = Math.max(
                                    document.body.scrollHeight,
                                    document.documentElement.scrollHeight,
                                    document.body.offsetHeight,
                                    document.documentElement.offsetHeight
                                );
                                
                                console.log('Content height calculated: ' + height + 'px');
                                
                                // Notify Android of initial content size
                                try {
                                    window.LayoutCallbacks.onContentSizeChanged(
                                        document.documentElement.scrollWidth,
                                        height
                                    );
                                } catch (e) {
                                    console.error('Error notifying content size:', e);
                                }
                            }, 300);
                            
                            return 'Content sizing configured for external scrolling';
                        })();
                    """.trimIndent(), null)
                    
                    // Add JavaScript interface for layout callbacks
                    view?.addJavascriptInterface(LayoutCallbacks(), "LayoutCallbacks")
                    
                    // Initialize with reasonable height in case calculation fails
                    binding.webView.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
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
                    android.util.Log.e("TabDetailViewer", "Error parsing URL: ${e.message}")
                    val credentials = "${args.username}:${args.password}"
                    val base64Credentials = Base64.getEncoder().encodeToString(credentials.toByteArray())
                    args.url.replace("://", "://$base64Credentials@")
                }
            } else {
                args.url
            }

            android.util.Log.d("TabDetailViewer", "Loading URL: $urlWithAuth")
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
        // Save WebView state when fragment is paused
        val newState = Bundle()
        binding.webView.saveState(newState)
        webViewState = newState
        
        // Save ScrollView position instead of WebView position
        lastScrollY = binding.scrollView.scrollY
    }
    
    override fun onResume() {
        super.onResume()
        // Restore scroll position of the ScrollView after resuming
        if (lastScrollY != 0) {
            binding.scrollView.postDelayed({
                binding.scrollView.scrollTo(0, lastScrollY)
            }, 300)
        }
    }
    
    /**
     * JavaScript interface for content layout callbacks
     */
    inner class LayoutCallbacks {
        @JavascriptInterface
        fun onContentSizeChanged(width: Int, height: Int) {
            android.util.Log.d("TabDetailViewer", "Content size changed: $width x $height")
            
            // Update WebView height on UI thread to match content
            activity?.runOnUiThread {
                val newHeight = height.coerceAtLeast(1000) // Ensure minimum reasonable height
                android.util.Log.d("TabDetailViewer", "Setting WebView height to: $newHeight")
                
                val params = binding.webView.layoutParams
                params.height = newHeight
                binding.webView.layoutParams = params
            }
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
