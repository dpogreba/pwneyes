package com.antbear.pwneyes.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.HttpAuthHandler
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import com.antbear.pwneyes.R
import com.antbear.pwneyes.databinding.DialogHttpAuthBinding
import com.antbear.pwneyes.databinding.FragmentConnectionWebviewBinding
import com.antbear.pwneyes.viewmodels.HomeViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.net.HttpURLConnection
import java.net.URL

/**
 * Displays a single Pwnagotchi web UI in a full-screen WebView.
 *
 * Features:
 *  - Background HTTP HEAD check on load; red banner + Retry on failure
 *  - Pull-to-refresh via SwipeRefreshLayout
 *  - Auto-refresh on configurable schedule
 *  - "Open in Browser" overflow menu item
 *  - HTTP Basic Auth login dialog (onReceivedHttpAuthRequest)
 *  - JavaScript alert / confirm / prompt dialogs (WebChromeClient)
 *  - window.open() popups rendered in a Material dialog
 */
class ConnectionWebViewFragment : Fragment() {

    private var _binding: FragmentConnectionWebviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by activityViewModels()

    private var connectionId: Long = 0
    private var connectionUrl: String = ""
    private var connectionName: String = ""

    private val autoRefreshHandler = Handler(Looper.getMainLooper())
    private var autoRefreshRunnable: Runnable? = null

    companion object {
        private const val ARG_ID   = "connection_id"
        private const val ARG_URL  = "connection_url"
        private const val ARG_NAME = "connection_name"

        fun newInstance(id: Long, url: String, name: String) =
            ConnectionWebViewFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_ID, id)
                    putString(ARG_URL, url)
                    putString(ARG_NAME, name)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConnectionWebviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        connectionId   = arguments?.getLong(ARG_ID)      ?: return
        connectionUrl  = arguments?.getString(ARG_URL)   ?: return
        connectionName = arguments?.getString(ARG_NAME)  ?: ""

        setupWebView(connectionUrl)
        checkAndLoad(connectionId, connectionUrl)

        binding.swipeRefresh.setColorSchemeColors(
            requireContext().getColor(R.color.blue_500)
        )
        binding.swipeRefresh.setOnRefreshListener {
            binding.progressBar.visibility = View.VISIBLE
            binding.webView.reload()
            checkAndLoad(connectionId, connectionUrl)
        }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_webview, menu)
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_open_browser -> {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(connectionUrl)))
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner)
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------
    override fun onResume() {
        super.onResume()
        scheduleAutoRefresh()
    }

    override fun onPause() {
        super.onPause()
        cancelAutoRefresh()
    }

    override fun onDestroyView() {
        cancelAutoRefresh()
        binding.webView.stopLoading()
        binding.webView.destroy()
        super.onDestroyView()
        _binding = null
    }

    // -------------------------------------------------------------------------
    // Auto-refresh
    // -------------------------------------------------------------------------
    private fun scheduleAutoRefresh() {
        val intervalMs = PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getString("pref_auto_refresh", "0")?.toLongOrNull() ?: 0L
        if (intervalMs <= 0L) return
        autoRefreshRunnable = Runnable {
            if (_binding != null) {
                binding.swipeRefresh.isRefreshing = true
                binding.webView.reload()
                checkAndLoad(connectionId, connectionUrl)
                scheduleAutoRefresh()
            }
        }
        autoRefreshHandler.postDelayed(autoRefreshRunnable!!, intervalMs)
    }

    private fun cancelAutoRefresh() {
        autoRefreshRunnable?.let { autoRefreshHandler.removeCallbacks(it) }
        autoRefreshRunnable = null
    }

    // -------------------------------------------------------------------------
    // WebView setup
    // -------------------------------------------------------------------------
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(url: String) {
        binding.webView.apply {
            settings.apply {
                javaScriptEnabled                = true   // Pwnagotchi's web UI requires JS
                domStorageEnabled                = true
                loadWithOverviewMode             = true
                useWideViewPort                  = true
                builtInZoomControls              = true
                displayZoomControls              = false
                javaScriptCanOpenWindowsAutomatically = true  // honour window.open() calls
                setSupportMultipleWindows(true)               // required for onCreateWindow
            }

            webViewClient = buildWebViewClient()
            webChromeClient = buildWebChromeClient()
        }
        binding.progressBar.visibility = View.VISIBLE
        binding.webView.loadUrl(url)
    }

    // -------------------------------------------------------------------------
    // WebViewClient — page lifecycle + HTTP auth
    // -------------------------------------------------------------------------
    private fun buildWebViewClient() = object : WebViewClient() {

        override fun onPageFinished(view: WebView, url: String) {
            _binding?.progressBar?.visibility = View.GONE
            _binding?.swipeRefresh?.isRefreshing = false
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
            if (request.isForMainFrame) {
                _binding?.progressBar?.visibility = View.GONE
                _binding?.swipeRefresh?.isRefreshing = false
                showStatus(connected = false)
            }
        }

        /**
         * HTTP Basic Authentication prompt — shown whenever the server responds
         * with 401 and a WWW-Authenticate header (e.g. Pwnagotchi's default auth).
         */
        override fun onReceivedHttpAuthRequest(
            view: WebView,
            handler: HttpAuthHandler,
            host: String,
            realm: String
        ) {
            if (_binding == null) { handler.cancel(); return }

            val dialogBinding = DialogHttpAuthBinding.inflate(layoutInflater)

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.auth_dialog_title, host))
                .setView(dialogBinding.root)
                .setPositiveButton(R.string.btn_sign_in) { _, _ ->
                    handler.proceed(
                        dialogBinding.etUsername.text.toString(),
                        dialogBinding.etPassword.text.toString()
                    )
                }
                .setNegativeButton(R.string.btn_cancel) { _, _ -> handler.cancel() }
                .setOnCancelListener { handler.cancel() }
                .show()
        }
    }

    // -------------------------------------------------------------------------
    // WebChromeClient — JS dialogs + window.open() popups
    // -------------------------------------------------------------------------
    private fun buildWebChromeClient() = object : WebChromeClient() {

        /** JavaScript alert() */
        override fun onJsAlert(
            view: WebView, url: String, message: String, result: JsResult
        ): Boolean {
            if (_binding == null) { result.cancel(); return true }
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ -> result.confirm() }
                .setOnCancelListener { result.cancel() }
                .show()
            return true
        }

        /** JavaScript confirm() */
        override fun onJsConfirm(
            view: WebView, url: String, message: String, result: JsResult
        ): Boolean {
            if (_binding == null) { result.cancel(); return true }
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ -> result.confirm() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> result.cancel() }
                .setOnCancelListener { result.cancel() }
                .show()
            return true
        }

        /** JavaScript prompt() — shows an input dialog and returns the typed value */
        override fun onJsPrompt(
            view: WebView, url: String, message: String,
            defaultValue: String?, result: JsPromptResult
        ): Boolean {
            if (_binding == null) { result.cancel(); return true }

            // Wrap an EditText in a FrameLayout so Material dialog padding looks right.
            val input = android.widget.EditText(requireContext()).apply {
                setText(defaultValue)
                inputType = android.text.InputType.TYPE_CLASS_TEXT
            }
            val container = FrameLayout(requireContext()).apply {
                val h = resources.getDimensionPixelSize(R.dimen.card_padding) * 2
                val v = resources.getDimensionPixelSize(R.dimen.card_padding)
                setPadding(h, v, h, 0)
                addView(input)
            }

            MaterialAlertDialogBuilder(requireContext())
                .setMessage(message)
                .setView(container)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    result.confirm(input.text.toString())
                }
                .setNegativeButton(android.R.string.cancel) { _, _ -> result.cancel() }
                .setOnCancelListener { result.cancel() }
                .show()
            return true
        }

        /**
         * window.open() — renders the popup in a full-height Material dialog
         * containing a fresh WebView. Closed automatically when the popup calls
         * window.close() or the user taps the Close button.
         */
        override fun onCreateWindow(
            view: WebView, isDialog: Boolean,
            isUserGesture: Boolean, resultMsg: Message
        ): Boolean {
            if (_binding == null) return false

            val popupWebView = WebView(requireContext()).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    setSupportMultipleWindows(true)
                }
            }

            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setView(popupWebView)
                .setNegativeButton(R.string.btn_close) { _, _ -> popupWebView.destroy() }
                .setOnCancelListener { popupWebView.destroy() }
                .create()

            // Allow the popup to close itself via window.close().
            popupWebView.webChromeClient = object : WebChromeClient() {
                override fun onCloseWindow(window: WebView) {
                    dialog.dismiss()
                    window.destroy()
                }
            }
            popupWebView.webViewClient = object : WebViewClient() {}

            // Connect the new WebView back to the parent via the transport message.
            val transport = resultMsg.obj as WebView.WebViewTransport
            transport.webView = popupWebView
            resultMsg.sendToTarget()

            dialog.show()
            return true
        }
    }

    // -------------------------------------------------------------------------
    // Connectivity check
    // -------------------------------------------------------------------------
    /**
     * Runs an HTTP HEAD probe on a background thread.
     * Updates Room status and refreshes the status banner.
     */
    private fun checkAndLoad(id: Long, url: String) {
        showStatus(checking = true)

        Thread {
            val reachable = try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.requestMethod = "HEAD"
                val timeoutMs = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getString("pref_timeout", "4000")?.toIntOrNull() ?: 4_000
                conn.connectTimeout = timeoutMs
                conn.readTimeout    = timeoutMs
                val code = conn.responseCode
                conn.disconnect()
                code < 400
            } catch (_: Exception) { false }

            viewModel.setConnectionStatus(id, reachable)

            if (isAdded && _binding != null) {
                requireActivity().runOnUiThread {
                    if (_binding != null) showStatus(connected = reachable)
                }
            }
        }.also { it.isDaemon = true }.start()

        binding.btnRetry.setOnClickListener {
            cancelAutoRefresh()
            binding.progressBar.visibility = View.VISIBLE
            binding.webView.reload()
            checkAndLoad(connectionId, connectionUrl)
        }
    }

    // -------------------------------------------------------------------------
    // Status banner
    // -------------------------------------------------------------------------
    private fun showStatus(checking: Boolean = false, connected: Boolean = false) {
        when {
            checking -> {
                binding.statusBar.setBackgroundColor(
                    requireContext().getColor(R.color.status_checking)
                )
                binding.tvStatus.text        = getString(R.string.status_checking)
                binding.btnRetry.visibility  = View.GONE
                binding.statusBar.visibility = View.VISIBLE
            }
            connected -> {
                binding.swipeRefresh.isRefreshing = false
                binding.statusBar.visibility = View.GONE
            }
            else -> {
                binding.swipeRefresh.isRefreshing = false
                binding.statusBar.setBackgroundColor(
                    requireContext().getColor(R.color.status_disconnected)
                )
                binding.tvStatus.text = if (connectionName.isNotBlank())
                    getString(R.string.status_unreachable_named, connectionName)
                else
                    getString(R.string.status_unreachable)
                binding.btnRetry.visibility  = View.VISIBLE
                binding.statusBar.visibility = View.VISIBLE
            }
        }
    }
}
