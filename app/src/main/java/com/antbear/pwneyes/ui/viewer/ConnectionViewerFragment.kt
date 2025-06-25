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
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.antbear.pwneyes.R
import com.antbear.pwneyes.databinding.FragmentConnectionViewerBinding
import kotlinx.coroutines.launch
import java.util.Base64

class ConnectionViewerFragment : Fragment() {
    private val TAG = "ConnectionViewerFragment"
    
    private var _binding: FragmentConnectionViewerBinding? = null
    private val binding get() = _binding!!
    private val args: ConnectionViewerFragmentArgs by navArgs()
    
    // Web view manager - manually instantiated
    private lateinit var webViewManager: WebViewManager
    
    // Using ViewModel to preserve state across configuration changes
    private val viewModel: ViewerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No longer using deprecated retainInstance
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConnectionViewerBinding.inflate(inflater, container, false)

        // Initialize WebViewManager
        webViewManager = WebViewManager(requireContext())

        // Set the connection name in the toolbar
        binding.connectionTitle.text = args.name

        setupWebView()
        setupControlButtons()
        observeWebViewLoadingState()

        // Restore WebView state from ViewModel if it exists
        viewModel.webViewState?.let { state ->
            binding.webView.restoreState(state)
        }

        return binding.root
    }
    
    private fun observeWebViewLoadingState() {
        webViewManager.loadingState.observe(viewLifecycleOwner) { state ->
            when (state) {
                WebViewManager.LoadingState.LOADING -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                WebViewManager.LoadingState.ENHANCING -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                WebViewManager.LoadingState.ENHANCED -> {
                    binding.progressBar.visibility = View.GONE
                }
                WebViewManager.LoadingState.ERROR -> {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }
    
    private fun setupControlButtons() {
        // Set up direct tab navigation buttons
        binding.pluginsTabButton.setOnClickListener {
            navigateToTab("plugins", "Plugins")
        }
        
        binding.inboxNewTabButton.setOnClickListener {
            navigateToTab("inbox/new", "Inbox New")
        }
        
        binding.inboxProfileTabButton.setOnClickListener {
            navigateToTab("inbox/profile", "Profile")
        }
        
        binding.inboxPeersTabButton.setOnClickListener {
            navigateToTab("inbox/peers", "Peers")
        }
        
        // Set up click listeners for our overlay buttons
        binding.btnShutdown.setOnClickListener {
            executeCommand("shutdown")
        }
        
        binding.btnReboot.setOnClickListener {
            executeCommand("reboot")
        }
        
        binding.btnRestart.setOnClickListener {
            executeCommand("restart_manu")
        }
    }
    
    /**
     * Execute a command via the WebViewManager
     */
    private fun executeCommand(command: String) {
        lifecycleScope.launch {
            try {
                webViewManager.executeCommand(binding.webView, command)
                
                // Show toast confirmation
                val message = "Command sent: $command"
                context?.let {
                    Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing command: $command", e)
                context?.let {
                    Toast.makeText(it, "Command execution failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * Direct method to navigate to any tab
     * This bypasses all the JavaScript detection complexity
     */
    private fun navigateToTab(tabPath: String, tabDisplayName: String) {
        try {
            // Ensure URL has port 8080 if needed
            var url = args.url
            if (!url.contains(":8080") && !url.contains(":443") && !url.contains(":80")) {
                url = if (url.endsWith("/")) {
                    url.substring(0, url.length - 1) + ":8080/"
                } else {
                    url + ":8080"
                }
                Log.d(TAG, "Added port 8080 to URL: $url")
            }

            // Ensure URL ends with the correct tab path
            val cleanTabPath = if (tabPath.startsWith("/")) tabPath.substring(1) else tabPath
            
            url = if (url.endsWith("/")) {
                url + cleanTabPath
            } else {
                url + "/" + cleanTabPath
            }
            
            Log.d(TAG, "Final URL: $url")

            Log.i(TAG, "Navigating directly to $tabDisplayName tab")
            Log.i(TAG, "URL: $url")

            // Extract the base URL (without the path part)
            val baseUrl = try {
                val parsedUrl = java.net.URL(args.url)
                val protocol = parsedUrl.protocol
                val host = parsedUrl.host
                val port = if (parsedUrl.port == -1) "" else ":${parsedUrl.port}"
                "$protocol://$host$port"
            } catch (e: Exception) {
                // Fallback to original URL if parsing fails
                args.url
            }
            
            // Special case for plugins tab - use native UI fragment
            if (cleanTabPath == "plugins") {
                try {
                    // Navigate to our new native Plugins fragment
                    findNavController().navigate(
                        R.id.nav_plugins,
                        Bundle().apply {
                            putString("connectionName", args.name)
                            putString("connectionBaseUrl", baseUrl)
                            putString("username", args.username)
                            putString("password", args.password)
                        },
                        androidx.navigation.navOptions {
                            anim {
                                enter = android.R.anim.fade_in
                                exit = android.R.anim.fade_out
                            }
                            launchSingleTop = true
                        }
                    )
                    Log.i(TAG, "Navigated to native Plugins UI")
                    return
                } catch (e: Exception) {
                    Log.e(TAG, "Error navigating to native Plugins UI, falling back to WebView", e)
                    // Continue with WebView fallback
                }
            }
            
            // For other tabs, use the regular TabDetailFragment with WebView
            val action = ConnectionViewerFragmentDirections.actionConnectionViewerToTabDetail(
                url = url,
                tabName = tabDisplayName,
                tabSelector = cleanTabPath.replace("/", "_"),
                connectionName = args.name,
                connectionBaseUrl = baseUrl,
                username = args.username,
                password = args.password
            )

            try {
                // Force destroy WebView to prevent memory leaks
                binding.webView.stopLoading()
                binding.webView.clearCache(true)
                
                // Forcibly show a toast to confirm navigation is happening
                context?.let {
                    Toast.makeText(
                        it,
                        "Opening $tabDisplayName tab...",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
                // Add small delay to ensure UI updates before navigation
                view?.postDelayed({
                    // Use safe navigation pattern with try/catch
                    try {
                        // Navigate to the tab detail fragment with explicit flags
                        findNavController().navigate(
                            action.actionId,
                            action.arguments,
                            androidx.navigation.navOptions {
                                anim {
                                    enter = android.R.anim.fade_in
                                    exit = android.R.anim.fade_out
                                }
                                launchSingleTop = true
                            }
                        )
                        Log.i(TAG, "Navigation to $tabDisplayName successfully triggered")
                    } catch (e: Exception) {
                        Log.e(TAG, "Navigation failed in delayed execution: ${e.message}", e)
                        showFallbackNavigation(url, tabDisplayName)
                    }
                }, 100)
            } catch (e: Exception) {
                Log.e(TAG, "Error during navigation preparation: ${e.message}", e)
                showFallbackNavigation(url, tabDisplayName)
            }

        } catch (e: Exception) {
            // Log any exceptions
            Log.e(TAG, "Error navigating to $tabDisplayName tab: ${e.message}")
            Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")

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
    
    /**
     * Fallback navigation method if regular navigation fails
     */
    private fun showFallbackNavigation(url: String, tabDisplayName: String) {
        Log.w(TAG, "Using fallback navigation for $tabDisplayName")
        context?.let { ctx ->
            // Show a dialog to inform the user
            android.app.AlertDialog.Builder(ctx)
                .setTitle("Navigation Issue")
                .setMessage("There was an issue navigating to the $tabDisplayName tab. Would you like to try an alternative method?")
                .setPositiveButton("Yes") { _, _ ->
                    // Manually construct the bundle
                    // Extract the base URL for fallback navigation too
                    val baseUrl = try {
                        val parsedUrl = java.net.URL(args.url)
                        val protocol = parsedUrl.protocol
                        val host = parsedUrl.host
                        val port = if (parsedUrl.port == -1) "" else ":${parsedUrl.port}"
                        "$protocol://$host$port"
                    } catch (e: Exception) {
                        args.url
                    }
                    
                    val bundle = Bundle().apply {
                        putString("url", url)
                        putString("tabName", tabDisplayName)
                        putString("tabSelector", tabDisplayName.lowercase().replace(" ", "_"))
                        putString("connectionName", args.name)
                        putString("connectionBaseUrl", baseUrl)
                        putString("username", args.username)
                        putString("password", args.password)
                    }
                    
                    // Direct navigation to fragment
                    try {
                        findNavController().navigate(R.id.nav_tab_detail, bundle)
                        Log.i(TAG, "Fallback navigation completed")
                    } catch (e: Exception) {
                        Log.e(TAG, "Fallback navigation failed: ${e.message}", e)
                        Toast.makeText(ctx, "Navigation failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun setupWebView() {
        binding.webView.apply {
            // Set an ID to help with state restoration
            id = View.generateViewId()
            
            // Configure WebView using WebViewManager
            webViewManager.configureWebView(this)
            
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
                        Log.d("WebConsole", "${it.message()} -- From line ${it.lineNumber()} of ${it.sourceId()}")
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
                    
                    // Save last URL in ViewModel for state restoration
                    viewModel.lastUrl = url
                    
                    // Apply WebView enhancements
                    webViewManager.enhanceRendering(view ?: return)
                    
                    // Save scroll position in ViewModel after page has fully loaded
                    view.postDelayed({
                        viewModel.lastScrollX = view.scrollX
                        viewModel.lastScrollY = view.scrollY
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
                    Log.e(TAG, "Error parsing URL: ${e.message}")
                    val credentials = "${args.username}:${args.password}"
                    val base64Credentials = Base64.getEncoder().encodeToString(credentials.toByteArray())
                    args.url.replace("://", "://$base64Credentials@")
                }
            } else {
                args.url
            }

            Log.d(TAG, "Loading URL: $urlWithAuth")
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
        // Save WebView state to ViewModel when fragment is paused
        val newState = Bundle()
        binding.webView.saveState(newState)
        viewModel.webViewState = newState
    }
    
    override fun onResume() {
        super.onResume()
        // Restore scroll position from ViewModel after resuming
        if (viewModel.lastScrollX != 0 || viewModel.lastScrollY != 0) {
            binding.webView.postDelayed({
                binding.webView.scrollTo(viewModel.lastScrollX, viewModel.lastScrollY)
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
            // ViewModel will be automatically cleared when the fragment is destroyed
        }
    }
}
