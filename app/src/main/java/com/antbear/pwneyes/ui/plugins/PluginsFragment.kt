package com.antbear.pwneyes.ui.plugins

import android.content.Context
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
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.antbear.pwneyes.R
import com.antbear.pwneyes.databinding.FragmentPluginsBinding
import com.antbear.pwneyes.ui.viewer.ViewerViewModel
import com.antbear.pwneyes.ui.viewer.WebViewManager
import java.util.Base64

class PluginsFragment : Fragment() {
    private val TAG = "PluginsFragment"
    
    init {
        Log.d(TAG, "🔵 PluginsFragment instance created")
    }
    
    private var _binding: FragmentPluginsBinding? = null
    private val binding get() = _binding!!
    
    // Use ViewModel for state preservation
    private val viewModel: ViewerViewModel by viewModels()
    
    // Safe Args to get connection details
    private val args: PluginsFragmentArgs by navArgs()
    
    // WebView manager - manually instantiated
    private lateinit var webViewManager: WebViewManager
    
    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(TAG, "🔵 onAttach called")
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "🔵 onCreate called")
        
        try {
            val args = args // This will throw if args don't exist
            Log.d(TAG, "🔵 Args successfully retrieved: connectionName=${args.connectionName}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error retrieving args: ${e.message}")
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "🔵 onCreateView called")
        _binding = FragmentPluginsBinding.inflate(inflater, container, false)
        Log.d(TAG, "🔵 Binding inflated successfully")
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "🔵 onViewCreated called")

        try {
            // Initialize WebViewManager
            webViewManager = WebViewManager(requireContext())
            
            setupToolbar()
            setupTabs()
            setupWebView()
            Log.d(TAG, "🔵 All UI setup methods completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during UI setup: ${e.message}")
            Log.e(TAG, "❌ Stack trace: ${e.stackTraceToString()}")
        }
    }
    
    private fun setupToolbar() {
        Log.d(TAG, "🔵 Setting up toolbar")
        try {
            // Set connection name in toolbar
            val title = "${args.connectionName} - Plugins"
            Log.d(TAG, "🔵 Setting toolbar title to: $title")
            binding.connectionTitle.text = title
            
            // Set URL in indicator
            val urlText = "${args.connectionBaseUrl}/plugins"
            Log.d(TAG, "🔵 Setting URL indicator to: $urlText")
            binding.urlIndicator.text = urlText
            
            // Handle back button clicks
            binding.backArrow.setOnClickListener {
                Log.d(TAG, "🔵 Back arrow clicked - navigating up")
                findNavController().navigateUp()
            }
            binding.toolbar.setNavigationOnClickListener {
                Log.d(TAG, "🔵 Toolbar navigation clicked - navigating up")
                findNavController().navigateUp()
            }
            Log.d(TAG, "🔵 Toolbar setup completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setting up toolbar: ${e.message}")
            Log.e(TAG, "❌ Stack trace: ${e.stackTraceToString()}")
        }
    }
    
    private fun setupTabs() {
        // Set click listeners for all tabs
        binding.homeTab.setOnClickListener { navigateToTab("home") }
        binding.inboxTab.setOnClickListener { navigateToTab("inbox") }
        binding.newTab.setOnClickListener { navigateToTab("inbox/new") }
        binding.profileTab.setOnClickListener { navigateToTab("inbox/profile") }
        binding.peersTab.setOnClickListener { navigateToTab("inbox/peers") }
        
        // Note: No need to handle plugins tab click as we're already on it
        // Just highlight it as selected
        binding.pluginsTab.isSelected = true
    }
    
    private fun navigateToTab(tabPath: String) {
        try {
            // For now, since we only have the plugins tab implemented as a native view,
            // we'll fall back to the WebView approach for other tabs
            val action = PluginsFragmentDirections.actionPluginsToTabDetail(
                url = "${args.connectionBaseUrl}/$tabPath",
                tabName = tabPath.split("/").last().capitalize(),
                tabSelector = tabPath.replace("/", "_"),
                connectionName = args.connectionName,
                connectionBaseUrl = args.connectionBaseUrl,
                username = args.username,
                password = args.password
            )
            
            findNavController().navigate(action)
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to tab: $tabPath", e)
            Toast.makeText(requireContext(), "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupWebView() {
        Log.d(TAG, "🔵 Setting up WebView")
        
        binding.pluginsWebView.apply {
            // Set an ID to help with state restoration
            id = View.generateViewId()
            
            // Configure WebView using WebViewManager
            webViewManager.configureWebView(this)
            
            // Set WebChromeClient for JavaScript dialogs and progress tracking
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (newProgress < 100) {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.progressBar.progress = newProgress
                    } else {
                        binding.progressBar.visibility = View.GONE
                    }
                }
                
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    consoleMessage?.let {
                        Log.d("WebConsole", "${it.message()} -- From line ${it.lineNumber()} of ${it.sourceId()}")
                    }
                    return true
                }
            }
            
            // Set WebViewClient for URL loading and error handling
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    binding.progressBar.visibility = View.VISIBLE
                    
                    // Update URL indicator
                    url?.let {
                        binding.urlIndicator.text = it
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    binding.progressBar.visibility = View.GONE
                    
                    // Apply WebView enhancements
                    webViewManager.enhanceRendering(view ?: return)
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    binding.progressBar.visibility = View.GONE
                    
                    // Show error toast
                    val errorMessage = "Failed to load: ${error?.description}"
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "❌ WebView error: $errorMessage")
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

            // Load the plugins URL
            val pluginsUrl = getPluginsUrl()
            Log.d(TAG, "🔵 Loading plugins URL: $pluginsUrl")
            
            // Handle basic auth in URL if credentials are provided
            val urlWithAuth = if (args.username.isNotEmpty()) {
                try {
                    // Parse the URL to extract its components
                    val url = java.net.URL(pluginsUrl)
                    val protocol = url.protocol
                    val host = url.host
                    val port = if (url.port == -1) "" else ":${url.port}"
                    val path = if (url.path.isEmpty()) "/" else url.path
                    val query = if (url.query == null) "" else "?${url.query}"
                    val ref = if (url.ref == null) "" else "#${url.ref}"
                    
                    // Create credentials
                    val credentials = "${args.username}:${args.password}"
                    val base64Credentials = Base64.getEncoder().encodeToString(credentials.toByteArray())
                    
                    // Reconstruct URL with credentials
                    "$protocol://$base64Credentials@$host$port$path$query$ref"
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parsing URL: ${e.message}")
                    pluginsUrl
                }
            } else {
                pluginsUrl
            }
            
            loadUrl(urlWithAuth)
        }
    }
    
    private fun getPluginsUrl(): String {
        // Ensure the base URL has the correct format
        var baseUrl = args.connectionBaseUrl
        
        // Make sure base URL doesn't end with a slash
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length - 1)
        }
        
        // Make sure URL has port if needed
        if (!baseUrl.contains(":8080") && !baseUrl.contains(":443") && !baseUrl.contains(":80")) {
            baseUrl += ":8080"
        }
        
        // Append /plugins to the base URL
        return "$baseUrl/plugins"
    }
    
    override fun onStart() {
        super.onStart()
        Log.d(TAG, "🔵 onStart called")
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "🔵 onResume called - fragment is now visible to user")
    }
    
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "🔵 onPause called")
    }
    
    override fun onStop() {
        super.onStop()
        Log.d(TAG, "🔵 onStop called")
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "🔵 onDestroyView called")
        
        try {
            // Clean up WebView resources
            binding.pluginsWebView.stopLoading()
            binding.pluginsWebView.clearCache(true)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cleaning up WebView: ${e.message}")
        }
        
        _binding = null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "🔵 onDestroy called")
        
        try {
            // Destroy WebView to prevent memory leaks
            if (!requireActivity().isChangingConfigurations) {
                binding.pluginsWebView.destroy()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error destroying WebView: ${e.message}")
        }
    }
    
    override fun onDetach() {
        super.onDetach()
        Log.d(TAG, "🔵 onDetach called")
    }
}
