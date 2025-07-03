package com.antbear.pwneyes.ui.webview

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.*
import java.net.URI

/**
 * WebViewManager handles all WebView-related operations
 * This class encapsulates WebView setup, configuration, and lifecycle management
 */
class WebViewManager(
    private val webView: WebView,
    private val progressBar: ProgressBar,
    private val debugInfoView: TextView? = null
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val TAG = "WebViewManager"
    
    private var loadingJob: Job? = null
    private var timeoutJob: Job? = null
    private val TIMEOUT_DURATION = 30000L // 30 seconds
    private val MAX_RETRIES = 2
    private var retryCount = 0
    
    init {
        configureWebView()
    }
    
    /**
     * Configure the WebView with all necessary settings
     */
    private fun configureWebView() {
        webView.apply {
            WebView.setWebContentsDebuggingEnabled(true)
            
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                allowContentAccess = true
                allowFileAccess = true
                setGeolocationEnabled(false)
                cacheMode = WebSettings.LOAD_NO_CACHE
                
                // Critical settings for JavaScript dialogs
                javaScriptCanOpenWindowsAutomatically = true
                setSupportMultipleWindows(true)
                
                // Modern storage approach - DOM storage replaces deprecated database storage
                domStorageEnabled = true
                
                // Modern security approach for Android 15 - restrict cross-origin access
                // Only enable if specifically needed for pwnagotchi functionality
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    @Suppress("DEPRECATION")
                    allowUniversalAccessFromFileURLs = true
                    @Suppress("DEPRECATION") 
                    allowFileAccessFromFileURLs = true
                }
                
                // Set default text encoding
                defaultTextEncodingName = "UTF-8"
            }
            
            isVerticalScrollBarEnabled = true
            isHorizontalScrollBarEnabled = true
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            overScrollMode = View.OVER_SCROLL_ALWAYS
        }

        setupWebViewClient()
        setupWebChromeClient()
    }
    
    /**
     * Set up the WebViewClient to handle page loading, errors, etc.
     */
    private fun setupWebViewClient() {
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
                updateDebugInfo("Loading started: $url")
                startLoadingTimeout()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                hideDebugInfo()
                cancelLoadingTimeout()
                retryCount = 0
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                val errorMessage = "Error: ${error?.description}"
                updateDebugInfo(errorMessage)
                progressBar.visibility = View.GONE
                
                if (retryCount < MAX_RETRIES && request?.isForMainFrame == true) {
                    retryCount++
                    updateDebugInfo("Retrying ($retryCount/$MAX_RETRIES)...")
                    
                    // Use coroutines for retry instead of handler
                    loadingJob?.cancel()
                    loadingJob = coroutineScope.launch {
                        delay(2000) // Wait 2 seconds before retry
                        view?.reload()
                    }
                } else if (retryCount >= MAX_RETRIES) {
                    updateDebugInfo("Failed after $MAX_RETRIES retries")
                    onLoadingFailed?.invoke()
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                request?.url?.toString()?.let { url ->
                    view?.loadUrl(url)
                }
                return true
            }
        }
    }
    
    /**
     * Set up the WebChromeClient to handle JavaScript dialogs, progress updates, etc.
     */
    private fun setupWebChromeClient() {
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress < 100) {
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = newProgress
                    updateDebugInfo("Loading: $newProgress%")
                } else {
                    progressBar.visibility = View.GONE
                    hideDebugInfo()
                    cancelLoadingTimeout()
                }
            }

            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult): Boolean {
                Log.d(TAG, "JavaScript alert: $message")
                try {
                    val context = view?.context ?: return false
                    AlertDialog.Builder(context)
                        .setTitle("Pwnagotchi Alert")
                        .setMessage(message)
                        .setPositiveButton("OK") { _, _ -> 
                            result.confirm()
                        }
                        .setCancelable(true)
                        .setOnCancelListener {
                            result.cancel()
                        }
                        .create()
                        .show()
                } catch (e: Exception) {
                    Log.e(TAG, "Error showing alert dialog", e)
                    result.cancel()
                }
                return true
            }

            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult): Boolean {
                Log.d(TAG, "JavaScript confirm: $message")
                try {
                    val context = view?.context ?: return false
                    
                    // Check if this is the shutdown confirmation
                    val isShutdown = message?.contains("shutdown", ignoreCase = true) ?: false
                    
                    val title = if (isShutdown) "Shutdown Confirmation" else "Confirmation"
                    val positiveButton = if (isShutdown) "Shutdown" else "OK"
                    
                    AlertDialog.Builder(context)
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
                        .show()
                } catch (e: Exception) {
                    Log.e(TAG, "Error showing confirmation dialog", e)
                    result.cancel()
                }
                return true
            }
            
            override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult): Boolean {
                Log.d(TAG, "JavaScript prompt: $message")
                try {
                    val context = view?.context ?: return false
                    val input = EditText(context)
                    input.setText(defaultValue)
                    
                    AlertDialog.Builder(context)
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
                        .show()
                } catch (e: Exception) {
                    Log.e(TAG, "Error showing prompt dialog", e)
                    result.cancel()
                }
                return true
            }
            
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    Log.d("WebConsole", "${it.message()} -- From line ${it.lineNumber()} of ${it.sourceId()}")
                }
                return true
            }
        }
    }
    
    /**
     * Load a URL with automatic retry on failure
     */
    fun loadUrl(url: String) {
        val finalUrl = when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            else -> "http://$url"
        }
        
        // Cancel any existing loading jobs
        cancelAllJobs()
        
        // Reset retry count and start loading
        retryCount = 0
        webView.loadUrl(finalUrl)
    }
    
    /**
     * Stop loading and clear the WebView
     */
    fun stopLoading() {
        cancelAllJobs()
        webView.stopLoading()
        webView.loadUrl("about:blank")
        progressBar.visibility = View.GONE
        hideDebugInfo()
    }
    
    /**
     * Start a timeout for loading
     */
    private fun startLoadingTimeout() {
        timeoutJob?.cancel()
        timeoutJob = coroutineScope.launch {
            delay(TIMEOUT_DURATION)
            if (progressBar.visibility == View.VISIBLE) {
                updateDebugInfo("Connection timed out")
                progressBar.visibility = View.GONE
                webView.stopLoading()
                onLoadingFailed?.invoke()
            }
        }
    }
    
    /**
     * Cancel the loading timeout
     */
    private fun cancelLoadingTimeout() {
        timeoutJob?.cancel()
        timeoutJob = null
    }
    
    /**
     * Cancel all running jobs
     */
    private fun cancelAllJobs() {
        loadingJob?.cancel()
        timeoutJob?.cancel()
        loadingJob = null
        timeoutJob = null
    }
    
    /**
     * Clean up resources when no longer needed
     */
    fun cleanup() {
        cancelAllJobs()
        coroutineScope.cancel()
        webView.stopLoading()
        webView.loadUrl("about:blank")
        webView.clearCache(true)
        webView.clearHistory()
        hideDebugInfo()
    }
    
    /**
     * Update debug info if debug view is available
     */
    private fun updateDebugInfo(message: String?) {
        debugInfoView?.apply {
            text = message
            visibility = if (message.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
    }
    
    /**
     * Hide debug info
     */
    private fun hideDebugInfo() {
        debugInfoView?.apply {
            text = ""
            visibility = View.GONE
        }
    }
    
    /**
     * Callback for when loading fails after max retries
     */
    var onLoadingFailed: (() -> Unit)? = null
}
