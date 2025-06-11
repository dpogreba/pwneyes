package com.antbear.pwneyes.ui.home

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.antbear.pwneyes.databinding.ItemConnectionBinding
import com.antbear.pwneyes.data.Connection
import java.net.URLEncoder
import android.util.Log
import android.webkit.CookieManager
import androidx.recyclerview.widget.DiffUtil
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.content.Context
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.*
import java.net.URI
import android.app.AlertDialog
import android.widget.EditText

class ConnectionsAdapter(
    private val onConnectClicked: (Connection) -> Unit,
    private val onEditClicked: (Connection) -> Unit,
    private val onDeleteClicked: (Connection) -> Unit
) : RecyclerView.Adapter<ConnectionsAdapter.ConnectionViewHolder>() {

    private var connections: List<Connection> = emptyList()
    private val handler = Handler(Looper.getMainLooper())
    private val retryDelayMs = 2000L
    private val maxRetries = 3

    fun updateConnections(newConnections: List<Connection>) {
        val oldConnections = connections
        connections = newConnections
        calculateDiff(oldConnections, newConnections)
    }

    private fun calculateDiff(oldList: List<Connection>, newList: List<Connection>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = oldList.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) = 
                oldList[oldPos].id == newList[newPos].id
            override fun areContentsTheSame(oldPos: Int, newPos: Int) = 
                oldList[oldPos] == newList[newPos]
        }
        
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        diffResult.dispatchUpdatesTo(this)
    }

    inner class ConnectionViewHolder(
        private val binding: ItemConnectionBinding,
        private val onConnectClick: (Connection) -> Unit,
        private val onEditClick: (Connection) -> Unit,
        private val onDeleteClick: (Connection) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var webViewClient: WebViewClient? = null
        private var webChromeClient: WebChromeClient? = null
        private var retryCount = 0
        private var currentLoadJob: Job? = null
        private var loadingTimeout: Job? = null
        private val TIMEOUT_DURATION = 30000L // 30 seconds
        private val MAX_RETRIES = 2

        private fun hideDebugInfo() {
            binding.debugInfo.visibility = View.GONE
            binding.debugInfo.text = ""
        }

        private fun startLoadingTimeout() {
            loadingTimeout?.cancel()
            loadingTimeout = CoroutineScope(Dispatchers.Main).launch {
                delay(TIMEOUT_DURATION)
                if (binding.loadingProgress.visibility == View.VISIBLE) {
                    updateDebugInfo("Connection timed out")
                    binding.loadingProgress.visibility = View.GONE
                    binding.webView.stopLoading()
                    // Disconnect on timeout
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val connection = connections[position]
                        onConnectClick(connection)
                    }
                }
            }
        }

        private fun cancelLoadingTimeout() {
            loadingTimeout?.cancel()
            loadingTimeout = null
        }

        private fun setupWebView(connection: Connection) {
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (newProgress < 100) {
                        binding.loadingProgress.visibility = View.VISIBLE
                        binding.loadingProgress.progress = newProgress
                        updateDebugInfo("Loading: $newProgress%")
                    } else {
                        binding.loadingProgress.visibility = View.GONE
                        hideDebugInfo()
                        cancelLoadingTimeout()
                    }
                }

                override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult): Boolean {
                    AlertDialog.Builder(view?.context ?: return false)
                        .setTitle("Alert")
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok) { _, _ -> 
                            result.confirm()
                        }
                        .setCancelable(false)
                        .create()
                        .show()
                    return true
                }

                override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult): Boolean {
                    AlertDialog.Builder(view?.context ?: return false)
                        .setTitle("Confirm")
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok) { _, _ -> 
                            result.confirm()
                        }
                        .setNegativeButton(android.R.string.cancel) { _, _ -> 
                            result.cancel()
                        }
                        .setCancelable(false)
                        .create()
                        .show()
                    return true
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    binding.loadingProgress.visibility = View.VISIBLE
                    updateDebugInfo("Loading started: $url")
                    startLoadingTimeout()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    binding.loadingProgress.visibility = View.GONE
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
                    binding.loadingProgress.visibility = View.GONE
                    
                    if (retryCount < MAX_RETRIES && request?.isForMainFrame == true) {
                        retryCount++
                        updateDebugInfo("Retrying ($retryCount/$MAX_RETRIES)...")
                        handler.postDelayed({
                            view?.reload()
                        }, 2000)
                    } else if (retryCount >= MAX_RETRIES) {
                        updateDebugInfo("Failed after $MAX_RETRIES retries")
                        // Disconnect after max retries
                        val position = adapterPosition
                        if (position != RecyclerView.NO_POSITION) {
                            val connection = connections[position]
                            onConnectClick(connection)
                        }
                    }
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    request?.url?.toString()?.let { url ->
                        view?.loadUrl(url)
                    }
                    return true
                }
            }

            with(binding.webView) {
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
                    databaseEnabled = true
                    setGeolocationEnabled(false)
                    cacheMode = WebSettings.LOAD_NO_CACHE
                    javaScriptCanOpenWindowsAutomatically = true
                }

                isVerticalScrollBarEnabled = true
                isHorizontalScrollBarEnabled = true
                scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
                overScrollMode = View.OVER_SCROLL_ALWAYS

                this.webViewClient = webViewClient
                this.webChromeClient = webChromeClient
            }
        }

        private fun loadUrlWithRetry(connection: Connection) {
            val url = connection.url.trim()
            val finalUrl = when {
                url.startsWith("http://") || url.startsWith("https://") -> url
                else -> "http://$url"
            }

            if (connection.username.isNotEmpty()) {
                val credentials = "${connection.username}:${connection.password}"
                val base64Credentials = android.util.Base64.encodeToString(
                    credentials.toByteArray(),
                    android.util.Base64.NO_WRAP
                )
                binding.webView.loadUrl(
                    finalUrl,
                    mapOf("Authorization" to "Basic $base64Credentials")
                )
            } else {
                binding.webView.loadUrl(finalUrl)
            }
        }

        fun bind(connection: Connection) {
            binding.textViewName.text = connection.name
            binding.buttonConnect.text = if (connection.isConnected) "Disconnect" else "Connect"
            binding.buttonConnect.setOnClickListener { onConnectClick(connection) }
            binding.buttonEdit.setOnClickListener { onEditClick(connection) }
            binding.buttonDelete.setOnClickListener { onDeleteClick(connection) }

            if (binding.webView.tag != connection.id) {
                setupWebView(connection)
                binding.webView.tag = connection.id
            }

            if (connection.isConnected) {
                binding.webViewContainer.visibility = View.VISIBLE
                binding.webView.visibility = View.VISIBLE
                retryCount = 0
                loadUrlWithRetry(connection)
            } else {
                currentLoadJob?.cancel()
                binding.webViewContainer.visibility = View.GONE
                binding.webView.visibility = View.GONE
                binding.loadingProgress.visibility = View.GONE
                hideDebugInfo()
                binding.webView.stopLoading()
                binding.webView.loadUrl("about:blank")
            }
        }

        fun cleanup() {
            currentLoadJob?.cancel()
            loadingTimeout?.cancel()
            binding.webView.stopLoading()
            binding.webView.loadUrl("about:blank")
            binding.webView.clearCache(true)
            binding.webView.clearHistory()
            hideDebugInfo()
            webViewClient = null
            webChromeClient = null
        }

        private fun updateDebugInfo(message: String?) {
            binding.debugInfo.apply {
                text = message
                visibility = if (message.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConnectionViewHolder {
        val binding = ItemConnectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ConnectionViewHolder(binding, onConnectClicked, onEditClicked, onDeleteClicked)
    }

    override fun onBindViewHolder(holder: ConnectionViewHolder, position: Int) {
        holder.bind(connections[position])
    }

    override fun getItemCount() = connections.size

    override fun onViewRecycled(holder: ConnectionViewHolder) {
        super.onViewRecycled(holder)
        holder.cleanup()
    }
}
